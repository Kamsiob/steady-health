#!/usr/bin/env python3
"""Walk the Sessions, Progress and You tabs and report what is on each.

Phase 2 was built while the phone was locked, so none of it had been seen running.
This drives it once and prints what each screen actually shows, which is the cheapest
way to find a screen that is blank, a list that is empty when it should not be, or a
button that leads nowhere.
"""
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from drive import Device, back_to_today, onboard

d = Device()
onboard(d)
back_to_today(d)


def show(what, scroll=0):
    """Print what is on the screen, scrolling first if the screen is longer than one.

    uiautomator reports what is drawn, so anything below the fold is simply absent. A
    list that looks empty here is usually a list that starts lower down.
    """
    time.sleep(1.4)
    seen = dict(d.screen())
    for _ in range(scroll):
        d._adb("shell", "input", "swipe", "540", "1700", "540", "700", "400")
        time.sleep(0.9)
        seen.update(d.screen())
    print(f"\n== {what}")
    for text in sorted(seen):
        print(f"   {text}")


show("Today")

d.tap("Sessions")
show("Sessions", scroll=5)

d.tap("Progress")
show("Progress", scroll=4)

d.tap("You")
show("You", scroll=4)

d.tap("Today")
time.sleep(1.0)
d.tap("Got a minute")
show("the ninety second session")
d.tap("That's enough for today")
time.sleep(1.2)
d.tap("Save")
time.sleep(1.5)

back_to_today(d)
d.tap("Do it without the phone")
show("without the phone", scroll=3)
d.tap("I have done it")
show("logging what was managed")
d.tap("Save it")
time.sleep(1.5)

back_to_today(d)
d.tap("Sessions")
time.sleep(1.2)
d.tap("Add a session you already did")
show("adding a past session", scroll=3)
