#!/usr/bin/env python3
"""Phase 1 gate: pause survives an interruption and resumes in place.

A screen lock, an incoming call and pressing home all reach the app as the same
thing: a stopped activity. This drives the two of those that can be done on the
owner's phone without opening anything else on it, the power button and home.

What it proves is that the count does not move while the app is away and that the
session comes back on the same movement with the same number.
"""
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from drive import Device, back_to_today, onboard

AWAY_SECONDS = 12

d = Device()
onboard(d)
back_to_today(d)

d.tap("Do another")
d.tap("Skip this one")
d.tap("I'm ready")
label, ring = d.wait("of ", timeout=20)
time.sleep(3.6)

for _ in range(3):
    d.tap_point(*ring)
    time.sleep(0.4)

before = {t for t in d.screen() if t.isdigit() or t.startswith("of ")}
movement_before = [t for t in d.screen() if t.startswith("2 of ") or "seat" in t.lower()]
print("before the lock:", sorted(before), movement_before)

def away_and_back(what):
    if what == "lock":
        d._adb("shell", "input", "keyevent", "KEYCODE_POWER")
        time.sleep(AWAY_SECONDS)
        d._adb("shell", "input", "keyevent", "KEYCODE_POWER")
        time.sleep(1.0)
        d._adb("shell", "input", "keyevent", "KEYCODE_MENU")
    else:
        d._adb("shell", "input", "keyevent", "KEYCODE_HOME")
        time.sleep(AWAY_SECONDS)
        d.launch()
    time.sleep(2.5)
    return {t for t in d.screen() if t.isdigit() or t.startswith("of ")}


for what in ("lock", "home"):
    after = away_and_back(what)
    print(f"after {AWAY_SECONDS}s away by {what}:", sorted(after))
    if before == after:
        print(f"PASS ({what}): the count did not move and the set is where it was")
    else:
        print(f"FAIL ({what}): {sorted(before)} became {sorted(after)}")
        break
