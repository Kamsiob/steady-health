#!/usr/bin/env python3
"""Phase 1 gate: pause survives an interruption and resumes in place.

A screen lock, an incoming call and pressing home all reach the app as the same
thing: a stopped activity. This drives home, which needs nothing from the owner.

The lock screen itself cannot be driven from here, because coming back through it
means typing the owner's PIN, and this project does not touch anything on the phone
beyond installing and testing the app. Locking the screen mid session is in
TEST-ME.md for the owner to try, and it is the same code path.

What it proves is that the count does not move while the app is away and that the
session comes back on the same movement with the same number.
"""
import re
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from drive import Device, back_to_today, onboard

AWAY_SECONDS = 15
COUNT = re.compile(r"^\d+$")

d = Device()
onboard(d)
back_to_today(d)

d.tap("Do another")
d.tap("Skip this one")
d.tap("I'm ready")
label, ring = d.wait("of ", timeout=20)
time.sleep(3.6)


def live_state():
    """The movement, the ask and the count: everything that must not move."""
    on = d.screen()
    return {
        "ask": next((t for t in on if t.startswith("of ")), None),
        "count": next((t for t in on if COUNT.match(t)), None),
        "movement": next((t for t in on if "seat" in t.lower() or "push" in t.lower()), None),
    }


for _ in range(3):
    d.tap_point(*ring)
    time.sleep(0.5)

before = live_state()
print("before:", before)

d._adb("shell", "input", "keyevent", "KEYCODE_HOME")
time.sleep(AWAY_SECONDS)
d.launch()
time.sleep(2.5)

after = live_state()
print(f"after {AWAY_SECONDS}s at the home screen:", after)

if before == after and before["count"] is not None:
    print("PASS: the count did not move and the set is where it was")
elif before["count"] is None:
    print("INCONCLUSIVE: the count was never read; the ring may not have taken the taps")
else:
    print("FAIL:", before, "became", after)
