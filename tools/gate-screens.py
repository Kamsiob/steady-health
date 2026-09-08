#!/usr/bin/env python3
"""Phase 1 gate: every screen in the session has one obvious next action.

"Obvious" is one primary button and no second thing competing with it. The exits are
deliberately not counted: they are always there, always secondary, and always the
same four, which is what makes them ignorable until somebody needs one.

Screenshots go to docs/screenshots/gate/, which is gitignored like the rest of the
raw captures.
"""
import subprocess
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from drive import Device, back_to_today, onboard

ROOT = Path(__file__).resolve().parent.parent

# The four exits and the two controls that sit on every session screen.
FURNITURE = {
    "Make it easier", "Skip this one", "That's enough for today", "Something hurts",
    "Sound on", "Sound off", "Pause", "Resume", "?",
}

# The one primary action each session screen offers.
PRIMARY = {
    "ready": "I'm ready",
    "count in": "Skip the count in",
    "live": "Done with this one",
    "rest": "Skip the rest",
    "done": "Save",
}

d = Device()


def shot(name):
    out = ROOT / "docs/screenshots/gate" / f"{name}.png"
    out.parent.mkdir(parents=True, exist_ok=True)
    focus = d._adb("shell", "dumpsys", "window")
    if d.package not in focus:
        raise SystemExit(f"Refused: {d.package} is not the focused window.")
    png = d._adb("exec-out", "screencap", "-p", binary=True)
    if len(png) < 20_000:
        raise SystemExit(f"Refused: the capture came back at {len(png)} bytes.")
    out.write_bytes(png)
    return out


def check(name):
    time.sleep(0.6)
    on = set(d.screen())
    shot(name)
    expected = PRIMARY[name]
    if expected not in on:
        return f"{name}: no primary action; screen has {sorted(on)}"
    others = {t for t in on if t in PRIMARY.values() and t != expected}
    if others:
        return f"{name}: {len(others) + 1} primary actions at once: {sorted(others | {expected})}"
    return f"{name}: one primary action, {expected!r}"


onboard(d)
back_to_today(d)
d.tap("Do another")

print(check("ready"))
d.tap("I'm ready")
print(check("count in"))
d.wait("of ", timeout=20)
time.sleep(3.4)
print(check("live"))
d.tap("Done with this one")
print(check("rest"))
d.tap("Skip the rest")
d.wait("I'm ready", timeout=20)
d.tap("That's enough for today")
print(check("done"))
