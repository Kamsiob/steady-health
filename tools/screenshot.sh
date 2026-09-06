#!/usr/bin/env bash
# Capture one screenshot from the connected device.
#
#   tools/screenshot.sh 01-today          -> docs/screenshots/01-today.png
#   tools/screenshot.sh raw/whatever      -> docs/screenshots/raw/whatever.png
#
# It refuses unless Steady Health is the focused window. That check is the whole
# point of this script existing rather than a bare `adb exec-out screencap`: a
# mistimed capture puts whatever the owner had on screen into a public
# repository, and no amount of being careful about timing is a substitute for the
# machine refusing. Everything under docs/screenshots/raw/ is gitignored, so a
# capture has to be moved deliberately before it can be committed.
set -euo pipefail

PACKAGE="com.kamsiob.steadyhealth"
NAME="${1:-screen}"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT="$ROOT/docs/screenshots/$NAME.png"

if ! adb get-state >/dev/null 2>&1; then
  echo "No device. Connect the phone and try again." >&2
  exit 1
fi

FOCUS="$(adb shell dumpsys window 2>/dev/null | grep -m1 mFocusedWindow || true)"
case "$FOCUS" in
  *"$PACKAGE"*) ;;
  *)
    echo "Refused: $PACKAGE is not the focused window." >&2
    echo "  focused: ${FOCUS:-nothing}" >&2
    exit 2
    ;;
esac

mkdir -p "$(dirname "$OUT")"
adb exec-out screencap -p > "$OUT"

# A screencap of a window that vanished mid-capture is a few hundred bytes.
SIZE=$(wc -c < "$OUT")
if [ "$SIZE" -lt 20000 ]; then
  rm -f "$OUT"
  echo "Refused: the capture came back at ${SIZE} bytes, which is not a screen." >&2
  exit 3
fi

echo "$OUT ($SIZE bytes)"
