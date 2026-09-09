#!/usr/bin/env bash
# Every gate, in order, on the phone. ADDENDUM-03 Part 21.
#
#   tools/all-gates.sh          -> the whole thing
#   tools/all-gates.sh --quick  -> everything that needs no phone
#
# What it does to the phone: installs this one app as an upgrade, drives it by
# tapping what is on the screen, and changes two settings inside it that two of
# the gates are about, putting both back at the end. It never uninstalls, never
# clears data unless a gate says it does, and touches nothing else on the device.
#
# Every step writes to its own file under the reports directory rather than
# scrolling past, because a gate that fails halfway through a wall of output is a
# gate nobody reads.
set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT="${STEADY_GATE_OUT:-$ROOT/app/build/gates}"
mkdir -p "$OUT"
cd "$ROOT"

pass=0
fail=0

run() {
  local name="$1"; shift
  printf '%-28s ' "$name"
  if "$@" >"$OUT/$name.log" 2>&1; then
    echo "pass"
    pass=$((pass + 1))
  else
    echo "FAIL   see $OUT/$name.log"
    fail=$((fail + 1))
  fi
}

echo "== off the phone"
# shellcheck disable=SC1091
source ./gradle-env.sh
run unit-tests        ./gradlew :app:testDebugUnitTest
run detekt            ./gradlew :app:detekt
run lint              ./gradlew :app:lintDebug
run banned-words      python3 tools/banned-words.py
run release-build     ./gradlew :app:assembleRelease

if [ "${1:-}" = "--quick" ]; then
  echo
  echo "$pass passed, $fail failed. The phone was not touched."
  [ "$fail" -eq 0 ]
  exit $?
fi

echo
echo "== on the phone"
if ! adb get-state >/dev/null 2>&1; then
  echo "no device. Plug the phone in and unlock it."
  exit 1
fi

# The screen has to be on and unlocked, because a Compose test launches its own
# activity and an activity cannot come up over a locked screen. Asked rather than
# forced: waking somebody's phone is not this script's business.
if adb shell dumpsys window 2>/dev/null | grep -q "mDreamingLockscreen=true"; then
  echo "the phone is locked. Unlock it and run this again."
  exit 1
fi

run device-tests      tools/device-tests.sh
run first-run         python3 tools/gate-first-run.py
run screens           python3 tools/gate-screens.py
run exits             python3 tools/gate-exits.py
run interrupted       python3 tools/gate-interrupted.py
run four-ways-around  python3 tools/gate-phase5.py
run numbers-off       python3 tools/gate-numbers-off.py

echo
echo "$pass passed, $fail failed."
echo "logs in $OUT"
[ "$fail" -eq 0 ]
