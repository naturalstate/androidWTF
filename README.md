# androidWTF

Turn an Android phone into a working security platform — and find out what
*your* phone can actually run before you install anything.

Part of the WTF suite, alongside
[macWTF](https://naturalstate.github.io/macwtf_website/) and
[windowsWTF](https://naturalstate.github.io/windowsWTF_website/).
Browse the catalogue at
**<https://naturalstate.github.io/androidWTF_website/>**.

> **Preview — 0.1.0-dev.** The catalogue and the engine work. Nothing has been
> run on a physical device yet.

## Quick start

Install **Termux** by hand first — it is an APK, so it cannot install itself.
Take it from [F-Droid](https://f-droid.org/packages/com.termux) or GitHub
Releases, never the Play Store, whose build was abandoned in 2020 and whose
packages are years stale.

Then, inside Termux:

```bash
pkg install -y curl
curl -fsSL https://raw.githubusercontent.com/naturalstate/androidWTF/main/platforms/android/bootstrap/install.sh | bash
```

```bash
wtf doctor
```

## The tier model

On macOS the organising question is *which package manager installs this*. On
Android it is *can this device run it at all*, so every tool carries a tier and
`wtf install` refuses to run steps above the tier the device actually reports.

| Tier | Needs | Unlocks | Tools |
|---|---|---|---:|
| **T0** | any Android 10+ device | Termux, the app catalogue, unprivileged scanning, VPN-mode capture, NFC, BLE, OTG serial | 170 |
| **T1** | Shizuku, paired over wireless debugging | debloat, AppOps, package control, on-device `adb` | +4 |
| **T2** | Magisk or KernelSU | raw sockets, privileged ports, Frida, real `tcpdump`, LSPosed | +13 |
| **T3** | NetHunter kernel | monitor mode, injection, HID attacks, external adapters | +4 |

`wtf doctor` reads `ro.build.version.sdk` and friends, checks for `su`, Shizuku,
a NetHunter kernel, USB host, NFC and BLE, and reports the tier. It is
read-only: it installs nothing and needs no privileges.

## Commands

```
wtf doctor                                Probe this device and report its tier
wtf list      --profile <name>            Show what a profile would install
wtf install   --profile <name> [--dry-run]
wtf catalogue [--bundle <name>]           Dump the tool catalogue
wtf version
wtf help
```

Nothing is installed until an explicit `install`. `doctor`, `list`, `catalogue`
and `--dry-run` change nothing.

## Profiles

| Profile | Tier | Notes |
|---|---|---|
| `android-minimal` | 0 | Termux and a working shell. Nothing else. |
| `android-stock` | 0 | Everything that runs untouched. Warranty intact, Play Integrity intact. |
| `android-rf` | 0 | Focused radio and hardware bench. Needs adapters, not privileges. |
| `android-shizuku` | 1 | Stock plus ADB-level privileges, reversible. |
| `android-rooted` | 2 | Magisk/KernelSU. Breaks Play Integrity. |
| `android-nethunter` | 3 | NetHunter kernel. Can brick a phone. |

The `labs`, `server` and `socialeng` bundles exist but are wired into no
profile — the first two are on hold, and the third carries legal warnings that
should be an explicit opt-in.

## Install providers

Android has several genuinely different install paths and they are not
interchangeable, so a tool's `install:` is an ordered chain:

| Provider | Meaning |
|---|---|
| `pkg` | Termux apt package (`repo:` names root/x11/tur when it is not in the default repo) |
| `pip` / `go` / `git` | installed inside Termux, but *not* an apt package |
| `obtainium` | trackable by Obtainium, so it self-updates — F-Droid and GitHub Releases |
| `fdroid` / `github` | the direct fallback if Obtainium is not used |
| `play` / `nethunter` | manual: Obtainium cannot track these |
| `builtin` / `web` | a documented workflow or reference, not an install |

Of the 51 shell tools, only 25 are actually `pkg install`-able. The rest are
pip, Go or clone-and-build, and the catalogue says which — that distinction was
wrong in the first draft and is the single most common way these lists mislead.

## Layout

```
catalog/bundles/*.yaml            the tool catalogue, one file per bundle
profiles/android-*.yaml           which bundles and modules a profile applies
platforms/android/bootstrap/
    install.sh                    network bootstrap, target of the one-liner
    preflight.sh                  the capability probe; `wtf doctor`
    wtf                           CLI shim (bash)
    wtf.py                        engine (Python stdlib only)
scripts/gen_bundles.py            regenerate the bundles from the catalogue
```

The engine has no third-party dependencies. The catalogue YAML is
machine-generated into a known subset, and `wtf.py` ships a reader for exactly
that subset — verified to parse identically to PyYAML across all 27 files —
rather than making every user build PyYAML on a phone.

## Verification

Every identifier in the catalogue was checked against its live source: the
F-Droid API, Play Store listings, the GitHub repos API, PyPI, the Go module
proxy, and the Termux main/x11/root/TUR package indexes. See the
[website README](https://github.com/naturalstate/androidWTF_website) for the
per-source breakdown and the corrections that pass turned up.

## Not done yet

Run on a physical device · `modules:` (termux/layout/isolate/shizuku/repos) are
declared in the profiles but not yet applied by the engine · the adb pack
installer · Obtainium config export from the CLI · first-party apps · scheduled
re-verification so the catalogue does not rot.

`catalog/bundles/*.yaml` is currently generated *from* the website's
`data/tools.json`. That is backwards from macWTF and windowsWTF, where the repo
is the source of truth — the site should be generated from these bundles
instead, and that flip is the next structural job.

## License

MIT
