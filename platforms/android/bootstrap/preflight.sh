#!/usr/bin/env bash
#
# androidWTF preflight / capability probe.
#
# Answers the only question that matters before installing anything on Android:
# what can THIS device actually do? Everything here is read-only. It installs
# nothing, changes nothing, and needs no privileges to run.
#
# The probe leans on Android's system properties, which are key/value pairs the
# platform exposes through `getprop`. `ro.build.version.sdk` is the API level as
# an integer — 29 is Android 10, 34 is Android 14, 36 is Android 16 — and `ro.`
# means read-only, set at build time. It is the most reliable version signal on
# Android because ro.build.version.release ("15", "12L", vendor strings) is not
# consistently parseable.
#
# Exit status is 0 unless something would make the install meaningless.
#
# Sourcing this file instead of running it defines the probe functions and sets
# the WTF_* variables without printing a report, which is how `wtf` reuses it.

set -uo pipefail

# Phone terminals are narrow and render SGR 2 (faint) as near-invisible grey,
# so this uses bright-black instead and sizes every row to the real width.
BOLD=$'\033[1m'; DIM=$'\033[90m'; GREEN=$'\033[32m'; YELLOW=$'\033[33m'; RED=$'\033[31m'; CYAN=$'\033[36m'; RESET=$'\033[0m'

COLS="${COLUMNS:-}"
[ -z "$COLS" ] && COLS="$(tput cols 2>/dev/null || true)"
[ -z "$COLS" ] && COLS="$(stty size 2>/dev/null | cut -d' ' -f2 || true)"
case "$COLS" in ''|*[!0-9]*) COLS=50 ;; esac
[ "$COLS" -lt 32 ] && COLS=32
[ "$COLS" -gt 80 ] && COLS=80
LBL=$(( COLS < 46 ? 11 : 16 ))

say()  { printf '%s\n' "$*"; }
step() { printf '%s%s%s\n' "$BOLD" "$*" "$RESET"; }
# warn folds too — an unwrapped sentence is the most common thing to overflow a
# phone terminal, and warnings are the lines that most need reading.
warn() {
	printf '%s\n' "$*" | fold -s -w $((COLS - 2)) | while IFS= read -r l; do
		printf '  %s%s%s\n' "$YELLOW" "$l" "$RESET"
	done
}
dim()  { printf '%s%s%s\n' "$DIM" "$*" "$RESET"; }
# The literal box-drawing character rather than printf '\uXXXX', which needs
# bash 4.2+ and macOS still ships 3.2.
rule() { local i=0 out=""; while [ $i -lt "$COLS" ]; do out="$out─"; i=$((i+1)); done; printf '%s%s%s\n' "$DIM" "$out" "$RESET"; }
row()  { printf "  %s%-${LBL}s%s %s\n" "$DIM" "$1" "$RESET" "$2"; }
# wrap folds a paragraph to the terminal and indents every line, continuations
# included — otherwise the second line starts hard against the left margin.
wrap() {
	printf '%s\n' "$*" | fold -s -w $((COLS - 2)) | while IFS= read -r l; do
		printf '  %s%s%s\n' "$DIM" "$l" "$RESET"
	done
}

GETPROP=/system/bin/getprop
prop() { [ -x "$GETPROP" ] && "$GETPROP" "$1" 2>/dev/null || printf ''; }

# has_feature asks the package manager whether the hardware feature is present.
# `pm` is readable from an unprivileged Termux shell on most builds; where it is
# not, we report unknown rather than guessing.
PM_OK=0
pm_features() {
	if [ "$PM_OK" = 0 ]; then
		PM_FEATURES="$(/system/bin/pm list features 2>/dev/null || printf '')"
		PM_OK=1
	fi
	printf '%s' "$PM_FEATURES"
}
has_feature() {
	local f; f="$(pm_features)"
	[ -z "$f" ] && return 2
	printf '%s' "$f" | grep -q "feature:$1" && return 0 || return 1
}
pkg_installed() {
	/system/bin/pm path "$1" >/dev/null 2>&1
}

probe() {
	WTF_SDK="$(prop ro.build.version.sdk)"
	WTF_RELEASE="$(prop ro.build.version.release)"
	WTF_MODEL="$(prop ro.product.model)"
	WTF_BRAND="$(prop ro.product.manufacturer)"
	WTF_ARCH="$(uname -m)"
	WTF_KERNEL="$(uname -r)"
	WTF_SELINUX="$(command -v getenforce >/dev/null 2>&1 && getenforce 2>/dev/null || prop ro.boot.selinux)"

	# Root: a usable su on PATH, or a Magisk/KernelSU manager installed.
	WTF_ROOT=no
	if command -v su >/dev/null 2>&1 && su -c id 2>/dev/null | grep -q 'uid=0'; then
		WTF_ROOT=yes
	elif [ -x /sbin/su ] || [ -x /system/xbin/su ] || [ -x /system/bin/su ]; then
		WTF_ROOT=maybe
	elif pkg_installed com.topjohnwu.magisk || pkg_installed me.weishu.kernelsu; then
		WTF_ROOT=maybe
	fi

	# Shizuku: installed is not the same as running and granted. We can only see
	# the package from here, so anything more is reported as "installed".
	WTF_SHIZUKU=no
	pkg_installed moe.shizuku.privileged.api && WTF_SHIZUKU=installed

	# NetHunter: the kernel string is the honest signal. The app alone means
	# rootless mode, which is Tier 0, not Tier 3.
	WTF_NETHUNTER=no
	case "$WTF_KERNEL" in *[Nn]et[Hh]unter*|*-nh*) WTF_NETHUNTER=kernel ;; esac
	if [ "$WTF_NETHUNTER" = no ] && pkg_installed com.offsec.nethunter; then
		WTF_NETHUNTER=app
	fi

	# Termux's own health. A stale bootstrap is the single most common reason
	# `pkg install` fails, and apt reports it as "Metadata integrity can't be
	# verified / repository is disabled", which points at nothing useful.
	WTF_TERMUX_VER="${TERMUX_VERSION:-}"
	WTF_SOURCES=""
	WTF_REPO=ok
	if [ -n "${PREFIX:-}" ] && [ -r "$PREFIX/etc/apt/sources.list" ]; then
		WTF_SOURCES="$(grep -hoE 'https?://[^ ]+' "$PREFIX/etc/apt/sources.list" \
			"$PREFIX/etc/apt/sources.list.d/"* 2>/dev/null | sed 's#\(https\?://[^/]*\).*#\1#' | sort -u | tr '\n' ' ')"
		case "$WTF_SOURCES" in
			"")                       WTF_REPO=unknown ;;
			*packages.termux.dev*)    WTF_REPO=ok ;;
			*grimler.se*)             WTF_REPO=ok ;;
			*packages.termux.org*)    WTF_REPO=stale ;;
			*termux.net*)             WTF_REPO=stale ;;
			*)                        WTF_REPO=custom ;;
		esac
	else
		WTF_REPO=unknown
	fi

	has_feature android.hardware.usb.host; WTF_USBHOST=$?
	has_feature android.hardware.nfc;      WTF_NFC=$?
	has_feature android.hardware.bluetooth_le; WTF_BLE=$?

	# Tier is the highest rung the device actually reaches. With no SDK we could
	# not read the device at all, so the honest answer is "unknown", not 0.
	if   [ -z "$WTF_SDK" ];              then WTF_TIER=""
	elif [ "$WTF_NETHUNTER" = kernel ];  then WTF_TIER=3
	elif [ "$WTF_ROOT" = yes ];          then WTF_TIER=2
	elif [ "$WTF_SHIZUKU" = installed ]; then WTF_TIER=1
	else                                      WTF_TIER=0
	fi
}

yesno() {
	case "$1" in
		0) printf '%syes%s' "$GREEN" "$RESET" ;;
		1) printf '%sno%s' "$DIM" "$RESET" ;;
		*) printf '%sunknown%s' "$YELLOW" "$RESET" ;;
	esac
}

report() {
	local tcol
	case "$WTF_TIER" in
		0) tcol="$GREEN" ;; 1) tcol="$CYAN" ;; 2) tcol="$YELLOW" ;; 3) tcol="$RED" ;;
		*) tcol="$YELLOW" ;;
	esac

	local kver
	kver="${WTF_KERNEL:-?}"
	[ "$COLS" -lt 46 ] && kver="$(printf '%s' "$kver" | cut -c1-24)"

	rule
	step "DEVICE"
	row "model"   "${WTF_BRAND:-?} ${WTF_MODEL:-?}"
	row "android" "${WTF_RELEASE:-?}  ${DIM}API${RESET} ${WTF_SDK:-?}"
	row "arch"    "${WTF_ARCH:-?}"
	row "kernel"  "$kver"
	row "selinux" "${WTF_SELINUX:-?}"
	say ""
	step "CAPABILITY"
	if [ "$WTF_ROOT" = yes ]; then row "root" "${GREEN}yes${RESET}"; else row "root" "${DIM}${WTF_ROOT}${RESET}"; fi
	if [ "$WTF_SHIZUKU" = installed ]; then row "shizuku" "${GREEN}installed${RESET}"; else row "shizuku" "${DIM}no${RESET}"; fi
	if [ "$WTF_NETHUNTER" = kernel ]; then row "nethunter" "${GREEN}kernel${RESET}"; else row "nethunter" "${DIM}${WTF_NETHUNTER}${RESET}"; fi
	row "usb otg" "$(yesno $WTF_USBHOST)"
	row "nfc"     "$(yesno $WTF_NFC)"
	row "ble"     "$(yesno $WTF_BLE)"
	say ""
	step "TERMUX"
	row "version" "${WTF_TERMUX_VER:-${DIM}unknown${RESET}}"
	case "$WTF_REPO" in
		ok)      row "repo" "${GREEN}reachable${RESET}" ;;
		stale)   row "repo" "${RED}stale mirror${RESET}" ;;
		custom)  row "repo" "${YELLOW}custom${RESET}" ;;
		unknown) row "repo" "${DIM}unreadable${RESET}" ;;
	esac
	# Built as a variable rather than inline: a `case` inside $( ) has its
	# pattern `)` read as the end of the substitution.
	local blurb
	case "$WTF_TIER" in
		"") blurb='unknown — could not read this device.' ;;
		0) blurb='stock. No root, nothing voided.' ;;
		1) blurb='Shizuku present. ADB-level privileges without root.' ;;
		2) blurb='rooted. Raw sockets, privileged ports, Frida.' ;;
		3) blurb='NetHunter kernel. Monitor mode and injection.' ;;
	esac
	say ""
	rule
	printf '  %sTIER %s%s%s\n' "$BOLD" "$tcol" "${WTF_TIER:-?}" "$RESET"
	wrap "$blurb"
	rule
}

advise() {
	local n=0
	if [ "$WTF_REPO" = unknown ]; then
		n=1; say ""
		wrap "Could not read Termux's sources.list, so the package repository state is unknown. If pkg install fails, try: termux-change-repo"
	fi
	if [ "$WTF_REPO" = stale ]; then
		n=1; say ""
		warn "Termux's package repository is stale. Nothing will install."
		wrap "The old packages.termux.org host now redirects, and apt refuses the redirect rather than following it, so it reports 'Metadata integrity can't be verified. Repository is disabled now.'"
		say ""
		dim  "  Try first:   termux-change-repo"
		dim  "  If that fails, the Termux app itself is too old to fix from"
		dim  "  inside. Uninstall it and install the current build from"
		dim  "  F-Droid or GitHub Releases, then re-run the bootstrap."
		wrap "The Play Store build was abandoned in 2020 and its repositories are dead; it cannot be repaired."
	fi
	[ "$WTF_SHIZUKU" = no ] && [ "$WTF_TIER" = 0 ] && [ -n "$WTF_SDK" ] && {
		n=1; say ""
		wrap "Shizuku would move this device to Tier 1 for free. Pairing over wireless debugging is reversible and voids nothing."
		dim "  wtf catalogue --bundle device"
	}
	[ "$WTF_USBHOST" = 1 ] && {
		n=1; say ""
		wrap "No USB host support, so the SDR, serial and Proxmark entries cannot work here no matter which tier you reach."
	}
	[ "$WTF_ROOT" = maybe ] && {
		n=1; say ""
		warn "A root binary is present but 'su' did not return uid=0."
		wrap "Grant Termux root once, then re-run: wtf doctor"
	}
	return $n
}

blocking() {
	local fail=0
	if [ -z "${PREFIX:-}" ]; then
		warn "Not running inside Termux."
		wrap "\$PREFIX is unset, so this is not a Termux shell."
		fail=1
	fi
	if [ -z "$WTF_SDK" ]; then
		warn "Could not read ro.build.version.sdk — this does not look like Android."; fail=1
	elif [ "$WTF_SDK" -lt 24 ] 2>/dev/null; then
		warn "Android API $WTF_SDK is below Termux's own floor of 24 (Android 7)."; fail=1
	elif [ "$WTF_SDK" -lt 29 ] 2>/dev/null; then
		# Not fatal. Termux supports API 24+, and nearly all of the shell
		# catalogue runs there. What degrades is the APK half: Android 10 is
		# where scoped storage, the modern permission model and most of the
		# catalogue's minSdk values land.
		say ""
		warn "Android API $WTF_SDK is below 29 (Android 10)."
		wrap "Termux and the shell tooling are fine; Termux itself supports API 24+. Some catalogue APKs will refuse to install."
		dim  "  wtf list --profile android-minimal"
	fi
	return $fail
}

# Only run the report when executed, not when sourced.
if [ "${BASH_SOURCE[0]}" = "${0}" ]; then
	probe
	report
	advise || true
	blocking || exit 1
	exit 0
fi
