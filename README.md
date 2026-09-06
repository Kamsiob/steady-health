# Steady Health by Kamsiob

**What can you do?**

Steady Health is a free, open source Android app that keeps a record of what your
body can do, and what changes it.

It is not a weight loss app. Weight is tracked, and it is shown as one of the
levers that make things easier, never as the score. The things it leads with are
four abilities: **Get up**, **Go**, **Carry**, **Steady**. You say what you would
like to be able to do, in your own words, and the app tracks those things. Once a
month it takes ten minutes, a chair and a wall, and tells you what you can do now
and what it was before.

Staying the same counts as a result. Strength falls between one and a half and
five percent a year from midlife if nothing is done about it, so a number you have
held for a year is a year of that not happening. The app says so, in the same
words and the same weight it uses for an improvement.

Everything stays on your phone. No account, no server, no analytics, no ads, no
subscription.

## Where this is

Early. The repository currently holds the specification, the design, and the
first working scaffolding: the theme, the encrypted database, and the three tab
shell running on a device. The screens are being built phase by phase, and this
README is updated as each one lands rather than describing an app that does not
exist yet.

There is no release to install yet.

## What it does, and what it will not do

It tracks four abilities and reports each as Better, Same, or Quieter, with a
sentence about life rather than about seconds. It keeps a smoothed weight, so one
heavy morning does not move the line. It suggests walks or sets that get longer
only after two that felt easy, and shorter after a day that felt worse. It has
four versions, for being on your feet, using a walker or cane, using a wheelchair,
or being mostly in bed for now, and none of them is presented as better than
another.

It will never count calories or ask what you ate. It has no streaks, no badges,
no scores, and no targets it sets for you. Nothing in it turns red. There is no
combined score, because a single number would rebuild the scoreboard the app
exists to remove. It does not name conditions, track medication, or diagnose
anything, and it says so on the screens where that matters.

## Privacy

Everything is written to an encrypted database on the device, keyed from the
Android Keystore. There is no server to send anything to.

The one thing that uses the internet is an optional model download, which is off
by default, and which the app asks for plainly before doing. After that it runs on
the phone. When the camera counts repetitions during a check, the picture is
looked at as it happens and never saved.

The full policy is in [PRIVACY.md](PRIVACY.md).

## Building it

You need the Android SDK and JDK 21.

    source ./gradle-env.sh
    ./gradlew assembleDebug
    ./gradlew installDebug      # with a device connected

`gradle-env.sh` points `JAVA_HOME` at JDK 21, which is what the Android Gradle
Plugin runs on and what CI uses. Tests:

    ./gradlew testDebugUnitTest         # logic, on the machine
    ./gradlew connectedDebugAndroidTest # database and rendering, on a device

## How this is built

The implementation is written by a coding agent working from the specification
documents in this repository. That is why those documents are authoritative and
why they are kept current with the code rather than written once: `MASTER_SPEC.md`
for what the app is, `DESIGN.md` for how it looks and what it is allowed to say,
`LOGIC.md` for every rule and threshold, `AI.md` for what the on-device model may
and may not do, and `DECISIONS.md` for every judgment call and why.

If you fork this, you get the current complete build path rather than a
specification describing a version that no longer exists.

## Licence

AGPL-3.0. See [LICENSE](LICENSE).

Bundled components and their licences are listed in `licenses/` and on the Made
with screen inside the app: Figtree under the SIL Open Font Licence, Phosphor
icons under MIT, and the rest recorded as they are added.

## Support this work

Built and carried by one person. If software made this way matters to you, there
is a place to stand behind it: <https://buymeacoffee.com/kamsiob>. Either way, it
is yours.
