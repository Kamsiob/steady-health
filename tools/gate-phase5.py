#!/usr/bin/env python3
"""The Phase 5 gate: a real session for every way of getting around.

ADDENDUM-03 Phase 5 is "the other ways of getting around; exclusions, readiness,
pacing mode; interruptions and context; ceilings and variants". The part only a
device can answer is the first: whether somebody who says they use a wheelchair,
or who is mostly in bed, gets a session that is actually theirs rather than an
empty card or a list of movements they cannot do.

Everything here goes through what is on the screen, using tools/drive.py, which
streams the view hierarchy over adb and never writes to the phone. It changes one
setting inside this app, which is the setting the gate is about, and puts it back
at the end.

It does not uninstall, does not clear app data, and touches nothing outside this
application.
"""

import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from drive import Device, NotOnScreen, back_to_today  # noqa: E402

# The labels as the settings screen writes them. If one of these stops matching,
# the gate fails loudly rather than quietly testing the same way four times.
WAYS = [
    ("On my feet", "on feet"),
    ("With a walker or cane", "walker or cane"),
    ("In a wheelchair", "wheelchair"),
    ("Mostly in bed or a chair", "in bed or a chair"),
]

# A session is a warm up, movements with a target, and a way out. The ready
# screen carries all three, so it is the one screen worth asserting on.
MUST_HAVE = ["I'm ready"]

# A week of sessions without repeating needs more than a handful. Every way has at
# least thirty movements, so this is a floor and not a target.
ENOUGH_MOVEMENTS = 12

# Nothing on a seated or bed session may ask somebody to stand or walk. These
# are the movement names that would mean the library was filtered wrongly.
ON_FEET_ONLY = [
    "A walk", "A brisk walk", "Stairs", "Wall push ups", "Counter push ups",
    "Step ups", "Heel to toe walking", "Marching on the spot", "Chair stands",
    "Mini squats", "Floor to stand", "Half kneel to stand", "One foot",
    "Tandem stand", "Semi-tandem stand", "Feet together",
]


def choose(device, label):
    """Set how somebody gets around, from the You tab."""
    back_to_today(device)
    device.tap("You")
    device.tap("How you get around")
    device.tap(label)


def one_way(device, label, name, seated):
    print(f"\n== {name}")
    choose(device, label)
    back_to_today(device)

    today = device.scan()
    print("   Today:", sorted(t for t in today if len(t) > 3)[:10])

    # The card's action is "Start", or "Do another" once a session has been done
    # today, and the gates run one after another so it is usually the second.
    device.one_of("Start", "Do another")
    ready = device.scan()
    print("   ready:", sorted(t for t in ready if len(t) > 3)[:10])

    ok = True
    missing = [want for want in MUST_HAVE if not any(want in t for t in ready)]
    if missing:
        print(f"   FAIL ({name}): the ready screen is missing {missing}")
        ok = False

    if seated:
        wrong = [m for m in ON_FEET_ONLY if m in ready]
        if wrong:
            print(f"   FAIL ({name}): offered {wrong}, which needs standing")
            ok = False

    # And the library, which is the other place a way of getting around has to be
    # honoured. An empty one is as much a failure as a wrong one. It is below the
    # fold, so this scans rather than reads: counting what fits on the screen would
    # be measuring the phone.
    back_to_today(device)
    device.tap("Sessions")
    device.tap("Everything you can do")
    library = device.scan()
    movements = sorted(t for t in library if t.startswith("For "))
    print(f"   library rows: {len(movements)}")
    if len(movements) < ENOUGH_MOVEMENTS:
        print(f"   FAIL ({name}): only {len(movements)} movements in the library")
        ok = False
    if seated:
        wrong = [m for m in ON_FEET_ONLY if m in library]
        if wrong:
            print(f"   FAIL ({name}): the library still lists {wrong}")
            ok = False

    return ok


def main():
    device = Device()
    passed = []
    try:
        for label, name in WAYS:
            seated = name in ("wheelchair", "in bed or a chair")
            passed.append(one_way(device, label, name, seated))
    except NotOnScreen as missing:
        print(f"   FAIL: {missing}")
        passed.append(False)
    finally:
        try:
            choose(device, "On my feet")
            back_to_today(device)
            print("\nput the setting back to on my feet")
        except NotOnScreen:
            print("\ncould not put the setting back; it is on whatever was last chosen")

    print()
    for (_, name), ok in zip(WAYS, passed):
        print(f"{'pass' if ok else 'FAIL'}: {name}")
    return 0 if len(passed) == len(WAYS) and all(passed) else 1


if __name__ == "__main__":
    raise SystemExit(main())
