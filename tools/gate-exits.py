#!/usr/bin/env python3
"""Phase 1 gate: the three exits and the pain button, on the device.

"From every point in the session" is proved by SessionRunnerTest, which presses each
exit from every stage of the pure state machine. This is the same four things once
each on the real screen, which is what the gate asks to be run on the device.

Each check starts from a fresh first run, because every session changes what the next
one is and a script that assumed yesterday's plan would be testing the wrong screen.
"""
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from drive import Device, back_to_today, onboard

d = Device()


def into_a_main_set():
    """A fresh person, a second session, and the first main movement of it, live.

    The first movement of an ordinary session is a warm up, which is held for a time
    and ends itself, so pressing anything on it is a race with the clock.
    """
    onboard(d)
    back_to_today(d)
    d.tap("Do another")
    d.tap("Skip this one")
    name = [t for t in d.screen() if t.startswith("Aiming")]
    d.tap("I'm ready")
    d.wait("of ", timeout=20)
    time.sleep(3.6)
    return name


def done_screen():
    d.wait("How did that feel", timeout=20)
    return sorted(t for t in d.screen() if t not in ("Easy", "About right", "Hard", "Save"))


print("== that's enough for today")
into_a_main_set()
_, ring = d.wait("of ")
d.tap_point(*ring)
time.sleep(0.6)
d.tap("That's enough for today")
print("   kept:", done_screen())

print("== skip this one")
into_a_main_set()
d.tap("Skip this one")
time.sleep(1.4)
after = [t for t in sorted(d.screen()) if " of " in t or t.startswith("Aiming") or "Rest" in t]
print("   moved on to:", after)
d.tap("That's enough for today")
print("   kept:", done_screen())

print("== make it easier")
asked = into_a_main_set()
before = d.wait("of ")[0]
d.tap("Make it easier")
time.sleep(1.2)
now = sorted(d.screen())
after = [t for t in now if t.startswith("of ")]
print(f"   ask was {before!r}, is now {after}")
print("   eased says:", [t for t in now if "easier" in t.lower() or "Easier" in t])

print("== something hurts, one press")
into_a_main_set()
d.tap("Something hurts")
label, _ = d.wait("Where", timeout=10)
print("   one press reached:", label)
d.tap("Knee")
time.sleep(2.0)
back_to_today(d)
card = sorted(t for t in d.screen() if t not in ("?", "Today", "Move", "Abilities"))
print("   the next session is now:", card)
