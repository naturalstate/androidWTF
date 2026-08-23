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

BOLD=$'\033[1m'; DIM=$'\033[2m'; GREEN=$'\033[32m'; YELLOW=$'\033[33m'; RED=$'\033[31m'; CYAN=$'\033[36m'; RESET=$'\033[0m'
say()  { printf '%s\n' "$*"; }
step() { printf '%s==>%s %s\n' "$BOLD" "$RESET" "$*"; }
warn() { printf '%s%s%s\n' "$YELLOW" "$*" "$RESET"; }
dim()  { printf '%s%s%s\n' "$DIM" "$*" "$RESET"; }

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

	has_feature android.hardware.usb.host; WTF_USBHOST=$?
	has_feature android.hardware.nfc;      WTF_NFC=$?
	has_feature android.hardware.bluetooth_le; WTF_BLE=$?

	# Tier is the highest rung the device actually reaches.
	if [ "$WTF_NETHUNTER" = kernel ]; then WTF_TIER=3
	elif [ "$WTF_ROOT" = yes ];        then WTF_TIER=2
	elif [ "$WTF_SHIZUKU" = installed ]; then WTF_TIER=1
	else                                    WTF_TIER=0
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
	esac

	step "Device"
	printf '    %-22s %s %s\n' "model"        "${WTF_BRAND:-?}" "${WTF_MODEL:-?}"
	printf '    %-22s %s (API %s)\n' "android" "${WTF_RELEASE:-?}" "${WTF_SDK:-?}"
	printf '    %-22s %s\n' "arch"            "${WTF_ARCH:-?}"
	printf '    %-22s %s\n' "kernel"          "${WTF_KERNEL:-?}"
	printf '    %-22s %s\n' "selinux"         "${WTF_SELINUX:-?}"
	say ""
	step "Capability"
	printf '    %-22s %s\n' "root"        "$([ "$WTF_ROOT" = yes ] && printf '%syes%s' "$GREEN" "$RESET" || printf '%s%s%s' "$DIM" "$WTF_ROOT" "$RESET")"
	printf '    %-22s %s\n' "shizuku"     "$([ "$WTF_SHIZUKU" = installed ] && printf '%sinstalled%s' "$GREEN" "$RESET" || printf '%sno%s' "$DIM" "$RESET")"
	printf '    %-22s %s\n' "nethunter"   "$([ "$WTF_NETHUNTER" = kernel ] && printf '%skernel%s' "$GREEN" "$RESET" || printf '%s%s%s' "$DIM" "$WTF_NETHUNTER" "$RESET")"
	printf '    %-22s %s\n' "usb host (otg)" "$(yesno $WTF_USBHOST)"
	printf '    %-22s %s\n' "nfc"            "$(yesno $WTF_NFC)"
	printf '    %-22s %s\n' "bluetooth le"   "$(yesno $WTF_BLE)"
	# Built as a variable rather than inline: a `case` inside $( ) has its
	# pattern `)` read as the end of the substitution.
	local blurb
	case "$WTF_TIER" in
		0) blurb='stock. No root, nothing voided.' ;;
		1) blurb='Shizuku present. ADB-level privileges without root.' ;;
		2) blurb='rooted. Raw sockets, privileged ports, Frida.' ;;
		3) blurb='NetHunter kernel. Monitor mode and injection.' ;;
	esac
	say ""
	printf '%s==>%s Tier %s%s%s — %s\n' "$BOLD" "$RESET" "$tcol" "$WTF_TIER" "$RESET" "$blurb"
}

advise() {
	local n=0
	[ "$WTF_SHIZUKU" = no ] && [ "$WTF_TIER" = 0 ] && {
		n=1; say ""; dim "Shizuku would move this device to Tier 1 for free — pairing over wireless"
		dim "debugging is reversible and voids nothing.  wtf catalogue --bundle device"
	}
	[ "$WTF_USBHOST" = 1 ] && {
		n=1; say ""; dim "No USB host support: the SDR, serial and Proxmark entries cannot work here"
		dim "no matter which tier you reach."
	}
	[ "$WTF_ROOT" = maybe ] && {
		n=1; say ""; warn "A root binary or manager is present but 'su' did not return uid=0."
		dim "Grant Termux root once, then re-run: wtf doctor"
	}
	return $n
}

blocking() {
	local fail=0
	if [ -z "${PREFIX:-}" ]; then
		warn "Not running inside Termux (\$PREFIX unset)."; fail=1
	fi
	if [ -z "$WTF_SDK" ]; then
		warn "Could not read ro.build.version.sdk — this does not look like Android."; fail=1
	elif [ "$WTF_SDK" -lt 29 ] 2>/dev/null; then
		warn "Android API $WTF_SDK is below the catalogue's floor of 29 (Android 10)."; fail=1
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
