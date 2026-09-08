#!/usr/bin/env python3
"""Phase 1 gate, item one: launch to a finished first session, timed.

Every tap waits for the thing it is pressing, so the number at the end is the app's
time and not the script's. The ten reps are tapped at a human rate rather than as
fast as adb will go, because a person doing ten heel raises cannot go faster than
their heels.
"""
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from drive import Device

REP_SECONDS = 1.6

device = Device()
device.clear()

started = time.time()
device.launch()
device.tap("Show me")
device.tap("On my feet")
device.tap("Stairs without stopping")
device.tap("Continue")
name, _ = device.wait("Start")
first = [t for t in device.screen() if t not in ("Start",)]
device.tap("Start")
device.tap("I'm ready")

# The count in is three seconds and then the ring is live.
label, ring = device.wait("of 10", "of 3", "of 8", timeout=20)
target = int(label.split()[-1])
for _ in range(target):
    device.tap_point(*ring)
    time.sleep(REP_SECONDS)

device.tap("Done with this one")
device.wait("How did that feel", timeout=20)
elapsed = time.time() - started

print(f"first movement offered: {first}")
print(f"reps asked for: {target}")
print(f"launch to the done screen: {elapsed:.1f} seconds")
print(f"of which {target * REP_SECONDS:.1f} was doing the movement")
print("PASS" if elapsed < 90 else "FAIL", "(gate is 90 seconds)")
