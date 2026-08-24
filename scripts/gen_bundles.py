#!/usr/bin/env python3
"""Generate catalog/bundles/*.yaml from the verified androidWTF catalogue.

The schema deliberately mirrors windowsWTF's catalog/bundles/*.yaml so the two
platforms read the same way and can eventually share an engine:

    bundle: <name>
    description: >
      ...
    tools:
      - id / name / desc / tier / profiles / status / upstream / checked
        install:
          - {provider: ..., id: ...}

The Android-specific addition is `tier`, because on Android the question is not
which package manager installs a thing but whether the device can run it at all.

Source of truth is the website's data/tools.json, whose every identifier was
verified against F-Droid, the Play Store, GitHub, PyPI, the Go module proxy and
the Termux package indexes.
"""

import json
import sys
from collections import OrderedDict
from datetime import date
from pathlib import Path

SITE = Path(__file__).resolve().parents[2] / "androidwtf_website" / "data" / "tools.json"
OUT = Path(__file__).resolve().parents[1] / "catalog" / "bundles"

# category slug -> (bundle name, one-line bundle description)
BUNDLES = {
    "core":       ("core",        "Termux itself, its add-on APKs, and the terminal essentials. Installed by every profile."),
    "control":    ("device",      "App and device control: Shizuku, debloat, work-profile isolation, and the root stack."),
    "netrecon":   ("network",     "Capture, scan, map and intercept the network the device is on."),
    "wireless":   ("wireless",    "Wardriving and wireless attack tooling. Most of it needs a NetHunter kernel and an external adapter."),
    "hardware":   ("hardware",    "NFC, RFID, BLE and USB OTG serial — the bench hardware interface."),
    "sdr":        ("radio",       "Software defined radio, GNSS and LoRa."),
    "vpn":        ("egress",      "Firewall, VPN, Tor and proxying: control over where the traffic goes."),
    "web":        ("web",         "Web and API testing, including the on-device intercepting proxies."),
    "mobile":     ("mobile",      "Mobile application assessment: decompile, patch, hook, intercept."),
    "osint":      ("recon",       "OSINT and pre-engagement reconnaissance, including offline mapping."),
    "creds":      ("credaccess",  "Password managers, TOTP and engagement-scoped credential storage."),
    "remote":     ("remote",      "Remote access, file transfer and getting artifacts off the device."),
    "evidence":   ("reporting",   "Evidence capture, notes and the reporting pipeline."),
    "desktop":    ("desktop",     "Linux desktop backends for the phone. Pluggable: KeX today, AVF later."),
    "dev":        ("dev",         "Editors, runtimes and the shell environment."),
    "comms":      ("comms",       "Engagement communications, including self-hostable and offline-capable options."),
    "socialeng":  ("socialeng",   "Social engineering support. Read the legal notes before using any of it."),
    "automation": ("automation",  "Trigger-based field workflows, usually driving Termux scripts."),
    "reference":  ("reference",   "Offline methodology, standards and wordlists."),
    # Held back at the operator's request; generated but not wired into any profile.
    "server":     ("server",      "Servers and listeners run from the phone. PAUSED — not in any profile yet."),
    "labs":       ("labs",        "Deliberately vulnerable targets to practise against. PAUSED — not in any profile yet."),
}

# Which profiles a tool belongs to, by tier. A profile always includes every
# tier at or below its own, so a Tier 0 tool appears in all four.
PROFILE_BY_TIER = {
    0: ["stock", "shizuku", "rooted", "nethunter"],
    1: ["shizuku", "rooted", "nethunter"],
    2: ["rooted", "nethunter"],
    3: ["nethunter"],
}

# Termux packages that do not live in the default repo.
EXTRA_REPO = {"tcpdump": "root", "juice-shop": "tur"}


def install_steps(t):
    """The ordered install chain for one tool. One verified step, plus an
    Obtainium entry where Obtainium can actually track the source."""
    src, pkg = t["source"], t["package"]
    if src == "termux":
        m = t["method"]
        if m == "pkg":
            step = {"provider": "pkg", "id": pkg}
            repo = next((EXTRA_REPO[n] for n in pkg.split() if n in EXTRA_REPO), None)
            if repo:
                step["repo"] = repo
            return [step]
        return [{"provider": m, "id": pkg}]
    if src == "fdroid":
        return [{"provider": "obtainium", "source": "FDroid", "id": pkg},
                {"provider": "fdroid", "id": pkg}]
    if src == "github":
        return [{"provider": "obtainium", "source": "GitHub", "id": pkg},
                {"provider": "github", "id": pkg}]
    if src == "play":
        return [{"provider": "play", "id": pkg}]
    if src == "nethunter":
        return [{"provider": "nethunter", "id": pkg}]
    if src == "own":
        return [{"provider": "androidwtf", "id": pkg}]
    if src == "web":
        return [{"provider": "web", "id": pkg}]
    return [{"provider": "builtin", "id": pkg}]


def upstream_of(t):
    if t["source"] in ("github",):
        return t["package"]
    if t["source"] == "fdroid":
        return f"f-droid:{t['package']}"
    if t["source"] == "play":
        return f"play:{t['package']}"
    if t["source"] == "termux" and t["method"] in ("go", "git"):
        return t["package"].replace("https://github.com/", "").replace("https://gitlab.com/", "gitlab:")
    return ""


def q(s):
    """YAML-quote a scalar only when it needs it."""
    s = str(s)
    if s == "":
        return "''"
    if any(c in s for c in ':#{}[]&*!|>%@`"\'\n') or s[0] in "-?" or s.strip() != s:
        return "'" + s.replace("'", "''") + "'"
    return s


def wrap(text, width, indent):
    out, line = [], ""
    for word in text.split():
        if line and len(line) + 1 + len(word) > width:
            out.append(line)
            line = word
        else:
            line = f"{line} {word}".strip()
    if line:
        out.append(line)
    return ("\n" + " " * indent).join(out)


def main():
    data = json.loads(SITE.read_text())
    today = date.today().isoformat()
    groups = OrderedDict()
    for t in data["tools"]:
        groups.setdefault(t["category"], []).append(t)

    OUT.mkdir(parents=True, exist_ok=True)
    generated = {name for name, _ in BUNDLES.values()}
    for old in OUT.glob("*.yaml"):
        # Hand-authored bundles (smoke) are not derived from the website
        # catalogue and must survive a regenerate.
        if old.stem in generated:
            old.unlink()

    written = 0
    for cat, tools in groups.items():
        if cat not in BUNDLES:
            sys.exit(f"no bundle mapping for category {cat!r}")
        name, desc = BUNDLES[cat]
        L = [f"bundle: {name}", "description: >", f"  {wrap(desc, 76, 2)}", "tools:"]
        for t in sorted(tools, key=lambda x: (x["tier"], x["id"])):
            L.append(f"  - id: {t['id']}")
            L.append(f"    name: {q(t['name'])}")
            L.append(f"    desc: {q(t['description'])}")
            L.append(f"    tier: {t['tier']}")
            L.append(f"    profiles: [{', '.join(PROFILE_BY_TIER[t['tier']])}]")
            L.append("    status: maintained")
            up = upstream_of(t)
            if up:
                L.append(f"    upstream: {q(up)}")
            L.append(f"    checked: {today}")
            if t["flags"]:
                L.append(f"    flags: [{', '.join(t['flags'])}]")
            if t["license"] != "free":
                L.append(f"    license: {t['license']}")
            if t["notes"]:
                L.append(f"    notes: {q(t['notes'])}")
            L.append("    install:")
            for s in install_steps(t):
                inner = ", ".join(f"{k}: {q(v)}" for k, v in s.items())
                L.append(f"      - {{{inner}}}")
            L.append("")
        (OUT / f"{name}.yaml").write_text("\n".join(L).rstrip() + "\n")
        written += 1
        print(f"  {name:12} {len(tools):3} tools")

    print(f"\n{written} bundles -> {OUT}")


if __name__ == "__main__":
    main()
