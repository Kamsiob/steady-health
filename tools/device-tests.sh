#!/usr/bin/env bash
# Run the instrumented tests without uninstalling the app.
#
#   tools/device-tests.sh                 -> every device test
#   tools/device-tests.sh AccessibilityTest -> one class
#
# Gradle's connectedDebugAndroidTest uninstalls both APKs when it finishes, which
# takes the app's data with it. On a phone somebody is actually using, that is a
# wipe dressed up as a test run. This installs and runs through adb instead, and
# leaves everything where it was.
#
# The tests themselves use their own database and their own key (see
# DatabaseSmokeTest), so nothing they do touches real rows either.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PACKAGE="com.kamsiob.steadyhealth"
RUNNER="$PACKAGE.test/androidx.test.runner.AndroidJUnitRunner"

# Which device, when more than one is attached. STEADY_SERIAL picks it; without it
# adb refuses rather than choosing, which is the right way round.
ADB=(adb)
if [ -n "${STEADY_SERIAL:-}" ]; then ADB=(adb -s "$STEADY_SERIAL"); fi

cd "$ROOT"
source ./gradle-env.sh
./gradlew assembleDebug assembleDebugAndroidTest -q

"${ADB[@]}" install -r -t "app/build/outputs/apk/debug/app-debug.apk" >/dev/null
"${ADB[@]}" install -r -t "app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk" >/dev/null

if [ $# -gt 0 ]; then
  "${ADB[@]}" shell am instrument -w -e class "$PACKAGE.$1" "$RUNNER"
else
  "${ADB[@]}" shell am instrument -w "$RUNNER"
fi
