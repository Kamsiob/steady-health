#!/usr/bin/env python3
"""Drive the app on the device by what is on the screen rather than by coordinates.

Written for the Phase 1 acceptance gate, where "install to a finished first session
in under ninety seconds" has to be a real measurement. A script of fixed taps and
sleeps is not one: a tap that lands a frame early is silently swallowed, and the run
carries on pressing the wrong things while the clock says everything is fine. That
happened, and the timing it produced was meaningless.

So every tap here waits for the text it is aiming at, taps the middle of it, and
fails loudly if it never appears.

The screen is read with `uiautomator dump /dev/tty`, which streams. The version that
writes to /sdcard is deliberately not used: nothing of this project's belongs in the
owner's phone storage.

    from drive import Device
    d = Device()
    d.tap("Show me")
    d.tap("On my feet")
"""
import os
import re
import subprocess
import sys
import time

PACKAGE = "com.kamsiob.steadyhealth"

# Which device to drive. Set STEADY_SERIAL when an emulator is up as well as the
# phone, because a bare adb command with two devices attached fails rather than
# picking one, and the one it would not pick is the one that matters.
SERIAL = os.environ.get("STEADY_SERIAL")
# A node carries a text, or a description, or both. The gates want whichever a
# person would meet, so both are read and the text wins where there is one.
#
# The description half matters more than it looks. Anything wrapped in
# clearAndSetSemantics has no text at all, only a description, and the biggest
# thing in the app is one of them: the count on the live screen is drawn as digits
# and spoken as a sentence. A gate reading only text saw no count there and said
# so, which is how this was found.
NODE = re.compile(
    r'text="([^"]*)"[^>]*?content-desc="([^"]*)"[^>]*?'
    r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"'
)


class NotOnScreen(Exception):
    pass


class Device:
    def __init__(self, package=PACKAGE, timeout=12.0):
        self.package = package
        self.timeout = timeout

    def _adb(self, *args, binary=False):
        prefix = ["adb"] + (["-s", SERIAL] if SERIAL else [])
        out = subprocess.run([*prefix, *args], capture_output=True, timeout=40)
        return out.stdout if binary else out.stdout.decode("utf-8", "replace")

    def screen(self):
        """Every piece of text on screen, with the middle of the thing it sits in."""
        raw = self._adb("exec-out", "uiautomator", "dump", "/dev/tty")
        found = {}
        for text, described, x1, y1, x2, y2 in NODE.findall(raw):
            middle = ((int(x1) + int(x2)) // 2, (int(y1) + int(y2)) // 2)
            for word in (text, described):
                if word:
                    found.setdefault(word, middle)
        return found

    def scan(self, sweeps=6):
        """Every piece of text on a screen, including what is below the fold.

        `screen` reads what is drawn, which on a long list is the top of it. A gate
        that counted rows from `screen` counted how many fit on a Pixel, which is a
        measurement of the phone. This scrolls to the bottom, gathering as it goes,
        and scrolls back so the screen is where it was found.
        """
        found = {}
        down = 0
        for _ in range(sweeps):
            before = len(found)
            found.update(self.screen())
            if len(found) == before:
                break
            self._adb("shell", "input", "swipe", "540", "1700", "540", "800", "300")
            down += 1
            time.sleep(0.4)
        # Back up by exactly as many as went down, rather than a fixed number of
        # swipes against the top of a list. On a short screen that was six swipes
        # into nothing, twelve times a gate, which is most of why one took a quarter
        # of an hour.
        for _ in range(down):
            self._adb("shell", "input", "swipe", "540", "800", "540", "1700", "300")
        if down:
            time.sleep(0.4)
        return found

    def one_of(self, *texts, timeout=None):
        """Tap whichever of these is on screen. For a button that has two names."""
        label, point = self.wait(*texts, timeout=timeout)
        self.tap_point(*point)
        return label

    def wait(self, *texts, timeout=None):
        """Wait until one of these is on screen, and say which and where.

        An exact match wins over a substring one, always. Matching on substrings alone
        had "You" find "What's changed since you started" and tap the middle of a
        sentence instead of the tab, and the screen that came back looked like a bug in
        the app rather than a bug in this.
        """
        deadline = time.time() + (timeout or self.timeout)
        seen = {}
        while time.time() < deadline:
            seen = self.screen()
            for text in texts:
                for label, point in seen.items():
                    if text.lower() == label.lower():
                        return label, point
            for text in texts:
                for label, point in seen.items():
                    if text.lower() in label.lower():
                        return label, point
            time.sleep(0.15)
        raise NotOnScreen(f"{texts} never appeared. On screen: {sorted(seen)}")

    def tap(self, text, timeout=None, scroll=6):
        """Tap something, scrolling down to find it if it is below the fold.

        uiautomator only reports what is drawn, so a button further down a long screen
        is simply not there as far as this is concerned. Scrolling to it is what a
        person does, so it is what this does.
        """
        try:
            label, (x, y) = self.wait(text, timeout=3.0)
        except NotOnScreen:
            label = None
        for _ in range(scroll):
            if label is not None:
                break
            self._adb("shell", "input", "swipe", "540", "1700", "540", "800", "400")
            time.sleep(0.7)
            try:
                label, (x, y) = self.wait(text, timeout=1.5)
            except NotOnScreen:
                label = None
        if label is None:
            label, (x, y) = self.wait(text, timeout=timeout)
        self._adb("shell", "input", "tap", str(x), str(y))
        return label

    def tap_point(self, x, y):
        self._adb("shell", "input", "tap", str(x), str(y))

    def launch(self):
        self._adb("shell", "am", "start", "-n", f"{self.package}/.MainActivity")

    def clear(self):
        """Wipe this app's own data. Never anything else on the device."""
        self._adb("shell", "pm", "clear", self.package)


def onboard(device, reps=10, pace=0.35):
    """Take a cleared app all the way through the first run to Today.

    Used by the gate scripts that need a person who exists. The first session is
    tapped through at machine speed on purpose: what is being measured here is not
    how long it takes, which gate-first-run.py measures properly.
    """
    # Clear, then wait, then start. `pm clear` returns before the process is gone,
    # and starting into the tail of the old one brings back the screen it was on
    # rather than the first screen of a fresh install. That failed one run in three,
    # which is worse than failing every time, because it looks like a bug in the app.
    device.clear()
    device._adb("shell", "am", "force-stop", device.package)
    time.sleep(1.5)
    device.launch()
    device.wait("Show me", timeout=25)
    device.tap("Show me")
    device.tap("On my feet")
    device.tap("Stairs without stopping")
    device.tap("Continue")
    device.tap("Start")
    device.tap("I'm ready")
    label, ring = device.wait("of ", timeout=20)
    for _ in range(int(label.split()[-1])):
        device.tap_point(*ring)
        time.sleep(pace)
    device.tap("Done with this one")
    device.tap("About right")
    device.tap("Save")
    # O5's two optional questions are on one screen. Answering the chair one is
    # enough; the exclusions question is left alone on purpose, because a person
    # who skips it is the ordinary case and the gates should test that person.
    device.tap("Yes")
    device.tap("Done")
    device.wait("Today's session", timeout=20)


def back_to_today(device):
    """Leave whatever is on screen and start again at Today.

    The pause between stopping and starting is not politeness. Starting straight
    after a force-stop can land the activity in a process that is still being torn
    down, and what comes up is a half-drawn screen that no amount of waiting fixes.
    """
    device._adb("shell", "am", "force-stop", device.package)
    time.sleep(1.0)
    device.launch()
    device.wait("Today's session", timeout=25)


if __name__ == "__main__":
    device = Device()
    print("\n".join(f"{point}  {text}" for text, point in sorted(device.screen().items())))
    sys.exit(0)
