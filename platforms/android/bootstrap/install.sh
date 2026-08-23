#!/usr/bin/env bash
#
# androidWTF network bootstrap. The target of the memorable one-liner.
#
# Downloads (or updates) the repo to ~/.wtf/repo, runs preflight, then puts the
# `wtf` command on PATH. Kept deliberately tiny — it is the first impression and
# the only thing that must work before anything else is on the device.
#
# Run it inside Termux:
#
#     pkg install -y curl
#     curl -fsSL https://raw.githubusercontent.com/naturalstate/androidWTF/main/platforms/android/bootstrap/install.sh | bash
#
# Termux itself is an APK and cannot install itself, so it is the one thing you
# fetch by hand — from F-Droid or GitHub Releases, never the Play Store, whose
# build was abandoned in 2020 and whose packages are years stale.

set -euo pipefail

BOLD=$'\033[1m'; DIM=$'\033[2m'; GREEN=$'\033[32m'; YELLOW=$'\033[33m'; RESET=$'\033[0m'
say()  { printf '%s\n' "$*"; }
step() { printf '%s==>%s %s\n' "$BOLD" "$RESET" "$*"; }
warn() { printf '%s%s%s\n' "$YELLOW" "$*" "$RESET"; }
dim()  { printf '%s%s%s\n' "$DIM" "$*" "$RESET"; }

REF="${REF:-main}"
PROFILE="${PROFILE:-}"
DRY_RUN="${DRY_RUN:-}"
WTF_HOME="${WTF_HOME:-$HOME/.wtf}"
REPO_DIR="$WTF_HOME/repo"
BIN_DIR="$REPO_DIR/platforms/android/bootstrap"
REPO_URL="https://github.com/naturalstate/androidWTF.git"
TARBALL="https://codeload.github.com/naturalstate/androidWTF/tar.gz/refs/heads/$REF"

while [ $# -gt 0 ]; do
	case "$1" in
		--ref)     REF="$2"; shift 2 ;;
		--profile) PROFILE="$2"; shift 2 ;;
		--dry-run) DRY_RUN=1; shift ;;
		-h|--help) say "usage: install.sh [--ref <branch>] [--profile <name>] [--dry-run]"; exit 0 ;;
		*)         warn "unknown option: $1"; exit 1 ;;
	esac
done

# Termux is the only supported host. Checking for the prefix rather than the
# `termux` command, because the prefix is what every path in here depends on.
if [ -z "${PREFIX:-}" ] || [ ! -d "${PREFIX:-}/bin" ]; then
	warn "androidWTF runs inside Termux, and \$PREFIX is not set."
	say  ""
	say  "Install Termux from F-Droid or GitHub Releases, open it, and run this again:"
	say  "    https://f-droid.org/packages/com.termux"
	say  ""
	dim  "Not the Play Store build — it was abandoned in 2020."
	exit 1
fi

step "androidWTF bootstrap ($REF)"
mkdir -p "$WTF_HOME"

# git if present, so `wtf` can self-update later; tarball otherwise.
if command -v git >/dev/null 2>&1; then
	if [ -d "$REPO_DIR/.git" ]; then
		step "Updating $REPO_DIR"
		git -C "$REPO_DIR" fetch --depth 1 origin "$REF" --quiet
		git -C "$REPO_DIR" reset --hard "origin/$REF" --quiet
	else
		step "Cloning to $REPO_DIR"
		rm -rf "$REPO_DIR"
		git clone --depth 1 --branch "$REF" --quiet "$REPO_URL" "$REPO_DIR"
	fi
else
	step "git not found, downloading tarball"
	command -v curl >/dev/null 2>&1 || { warn "Neither git nor curl. Run: pkg install -y curl"; exit 1; }
	tmp="$(mktemp -d)"
	curl -fsSL "$TARBALL" -o "$tmp/repo.tar.gz"
	rm -rf "$REPO_DIR"; mkdir -p "$REPO_DIR"
	tar -xzf "$tmp/repo.tar.gz" -C "$REPO_DIR" --strip-components=1
	rm -rf "$tmp"
fi

# Link `wtf` BEFORE preflight runs. `wtf doctor` is the tool you most need on a
# device preflight is unhappy about, so refusing to install it because the
# diagnosis came back badly is exactly backwards. Earlier versions did that and
# left the user with no command at all.
step "Linking wtf into $PREFIX/bin"
chmod +x "$BIN_DIR/wtf" "$BIN_DIR/preflight.sh" 2>/dev/null || true
ln -sf "$BIN_DIR/wtf" "$PREFIX/bin/wtf"

# Preflight now reports; it does not gate the bootstrap. `wtf install` still
# refuses to run steps above the tier the device actually reaches.
PREFLIGHT="$BIN_DIR/preflight.sh"
if [ -x "$PREFLIGHT" ]; then
	"$PREFLIGHT" || warn "preflight reported issues — see above. 'wtf doctor' re-runs this."
fi

say ""
printf '%s%s✓ androidWTF installed to %s%s\n' "$BOLD" "$GREEN" "$REPO_DIR" "$RESET"
say ""
dim "Start with:  wtf doctor              probe this device and report its tier"
dim "             wtf list --profile android-stock"
dim "             wtf install --profile android-stock --dry-run"

if [ -n "$PROFILE" ]; then
	say ""
	set -- install --profile "$PROFILE"
	[ -n "$DRY_RUN" ] && set -- "$@" --dry-run
	exec "$BIN_DIR/wtf" "$@"
fi
