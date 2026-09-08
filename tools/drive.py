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
NODE = re.compile(r'text="([^"]*)"[^>]*?bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"')


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
        for text, x1, y1, x2, y2 in NODE.findall(raw):
            if text:
                found.setdefault(text, ((int(x1) + int(x2)) // 2, (int(y1) + int(y2)) // 2))
        return found

    def wait(self, *texts, timeout=None):
        """Wait until one of these is on screen, and say which and where."""
        deadline = time.time() + (timeout or self.timeout)
        seen = {}
        while time.time() < deadline:
            seen = self.screen()
            for text in texts:
                for label, point in seen.items():
                    if text.lower() in label.lower():
                        return label, point
            time.sleep(0.15)
        raise NotOnScreen(f"{texts} never appeared. On screen: {sorted(seen)}")

    def tap(self, text, timeout=None):
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
    device.clear()
    device.launch()
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
    device.tap("Yes")
    device.tap("Done")
    device.wait("Today's session", timeout=20)


def back_to_today(device):
    """Leave whatever is on screen and start again at Today."""
    device._adb("shell", "am", "force-stop", device.package)
    device.launch()
    device.wait("Today's session", timeout=25)


if __name__ == "__main__":
    device = Device()
    print("\n".join(f"{point}  {text}" for text, point in sorted(device.screen().items())))
    sys.exit(0)
