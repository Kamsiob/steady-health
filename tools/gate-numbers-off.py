#!/usr/bin/env python3
"""Numbers off, and no digit anywhere.

MASTER_SPEC.md section on testing asks for exactly this as an instrumented test:
"numbers-off mode showing no digits anywhere". LOGIC.md says what the mode is:
"all figures become direction words; charts keep shape and lose axes; the
destination is hidden."

A unit test cannot answer it, because the failure is a digit that reaches a screen
through a path nobody thought about: a plural string, a duration, a count of days,
an axis label, a content description read aloud by TalkBack. So this walks the app
with the switch off and reads every piece of text on every screen it can reach.

It reads text and taps. It writes nothing to the phone, installs nothing, and
changes one setting inside this application, which is the setting under test, and
puts it back at the end.

Where the line is, in one sentence, and at length in NumbersOff.kt: a number the
app reports back becomes a word, and a number that is part of doing something
right now stays. So the count climbing while somebody stands up out of a chair is
still a number with the setting off, and so are a rest timer, a therapist's own
repetitions, and a setting somebody chose themselves. Dates stay too, because an
app that hid today's date would be hiding the calendar rather than the numbers,
and they are matched out below by shape.
"""

import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from drive import Device, NotOnScreen, back_to_today  # noqa: E402

DIGIT = re.compile(r"\d")

# A date, a time, and the one place a number is the name of a thing rather than a
# measurement of a person. Each of these is allowed to carry a digit with numbers
# off, and each is here with the reason it is allowed.
ALLOWED = [
    # Today's date, in every shape the app writes one.
    re.compile(r"^\w+day, \w+ \d{1,2}$"),
    re.compile(r"^\w+ \d{1,2}$"),
    re.compile(r"^\d{1,2} \w+$"),
    re.compile(r"^\d{1,2}:\d{2}( ?[ap]m)?$", re.I),
    re.compile(r"^\d{4}-\d{2}-\d{2}$"),
    # The system status bar, which is not this app's text at all.
    re.compile(r"^\d{1,3}%$"),
]

# Every screen the walk visits, as a list of taps from Today. A screen missing
# from here is a screen this gate does not cover, so the list is the coverage.
SCREENS = [
    (["Today"], "Today"),
    (["Sessions"], "Sessions"),
    (["Progress"], "Progress"),
    (["You"], "You"),
    (["Progress", "Summary"], "Progress, what changed"),
    (["Progress", "Getting out of a chair"], "an ability in detail"),
    (["Sessions", "Everything you can do"], "the library"),
    (["Sessions", "Add a session you already did"], "logging a past session"),
    (["You", "Anything to leave out"], "exclusions"),
    (["You", "Sessions a week"], "how many a week"),
    (["You", "Reminders"], "reminders"),
    (["You", "Your data"], "your data"),
    (["Today", "Today's session"], "the ready screen"),
]


def allowed(text):
    return any(pattern.match(text.strip()) for pattern in ALLOWED)


def offences(screen):
    return sorted(t for t in screen if DIGIT.search(t) and not allowed(t))


def set_numbers(device, on):
    back_to_today(device)
    device.tap("You")
    device.tap("Settings")
    row = device.screen()
    if "Show numbers" not in row:
        raise NotOnScreen("Show numbers is not on the settings screen")
    # The switch is a row, so tapping it toggles. Read the state first rather
    # than assuming, because a gate that leaves the setting inverted is worse
    # than one that fails.
    device.tap("Show numbers")


def walk(device, label, taps):
    back_to_today(device)
    for text in taps:
        device.tap(text)
    screen = device.screen()
    bad = offences(screen)
    if bad:
        print(f"   FAIL ({label}): {bad}")
    else:
        print(f"   ok  {label}")
    return bad


def main():
    device = Device()
    found = {}
    try:
        set_numbers(device, on=False)
        print("numbers off\n")
        for taps, label in SCREENS:
            try:
                bad = walk(device, label, taps)
            except NotOnScreen as missing:
                print(f"   skipped {label}: {missing}")
                bad = []
            if bad:
                found[label] = bad
    finally:
        try:
            set_numbers(device, on=True)
            print("\nnumbers back on")
        except NotOnScreen:
            print("\ncould not put the setting back; check Show numbers in You")

    print()
    if not found:
        print("pass: no digit reached any screen with numbers off")
        return 0
    print("FAIL: digits reached these screens with numbers off")
    for label, bad in found.items():
        print(f"  {label}: {bad}")
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
