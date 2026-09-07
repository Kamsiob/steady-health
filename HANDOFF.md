# HANDOFF.md

The resume document. A session with no memory of any previous conversation should
be able to pick this up and continue without repeating work, reversing a decision
it cannot see the reasoning for, or breaking something it does not understand.

Read this in full. Then DECISIONS.md. Then MASTER_SPEC.md and DESIGN.md. Then
`git log`.

**Last updated:** 2026-09-06, at the point the app was handed over for testing.
Phases 0 to 5 are done, Phase 6 is most of the way, and all of it was driven on
the Pixel 8 rather than asserted. 178 unit tests and 15 device tests, all passing;
CI green; debug APK 41.7 MB.

---

## 1. Where the work stands

**The project was reset on 2026-09-06.** An earlier weight-tracking version of
this app existed, was built to roughly Phase 3, and was deleted. The capability
specification in this folder replaces it entirely and the old tree is not a
starting point. The repository history was rewritten: the root commit is
`dd4c3bd` and nothing before it is reachable. Details in DECISIONS.md under "The
reset".

**Phases 0 to 5 are done and verified on the phone. Phase 6 is most of the way.
Phase 7 has not started.**

What runs on the phone right now: setup; all four versions of the app; the daily
three; walks with the talk test and the offer; the monthly check with the phone
counting chair stands from its motion sensor; the four abilities with measured
life sentences and Better, Same or Quieter; the ability and weight pages; the tag
grid; the Sunday write-up; Ask a question; the visit summary with its validator
and a PDF export; settings with seven working rows; reminders with the two-a-week
ceiling; numbers-off; and export and delete.

What is proven rather than assumed:

- The encrypted database opens, holds a row, and the file on disk is not readable
  as plain SQLite. Instrumented, on device.
- Deleting leaves no database file and no Keystore key. Instrumented, on device.
- Vico renders a line chart. Instrumented, on device.
- Every colour pair in the palette meets its contrast threshold, and nothing in
  the palette is red. Unit test.
- Every shipped string passes the banned word list, the dash rule and the
  no-shouting rule. Unit test, reading the real resource file.

Driven on the Pixel 8, not asserted: setup, twice, including once immediately
after deleting everything; a weigh-in; a check-in with tags that survive a save
and a reopen; two qualifying walks producing the offer, accepting it, and the next
walk reading four minutes; switching to a wheelchair and to the bed version and
back from settings; a bed session with its three parts and "How do you feel" in
place of the talk test; pacing mode on from the pattern question and off from its
own screen; asking a question and reading a card; a whole monthly check including
the thirty second measure finishing on its own; Today afterwards carrying measured
sentences instead of the person's own words; the visit summary and its PDF through
the share sheet; the export zip, opened and read back; delete, with an empty
database directory afterwards; numbers off and on again; and turning a reminder on,
including the permission prompt.

The engine is pure and has about a hundred and sixty unit tests, plus fifteen on
the device. The ones worth knowing about:

- The job 6 validator against 37 deliberately bad paragraphs, every one of which
  it catches, and six honest ones, every one of which survives.
- Forty-eight generated visit briefs whose template summaries all pass that same
  validator.
- A repetition counter that is allowed to be low and never high, across four rep
  counts and three cadences.
- The tag validator against a 37-sentence corpus.
- Every route having a screen, read out of the source, because one did not.
- The accessibility floor in the semantics tree, at normal and at twice the text
  size.

**The very next concrete step:** the months path (grid 20), then import, then the
camera counting wall push-ups. None of them is on the critical path for somebody
testing the app; all of them are named in the specification.

### What is uncommitted or mid-flight

Nothing at the last commit. Check `git status` before assuming.

### What would break if somebody assumed it was finished

- **Try it and see offers one variable**, when the strength set happens. The
  other five in LOGIC.md 9b are in the enum and are not offered. Its patterns are
  also not yet passed into the visit-summary brief, so the co-occurrence section
  of the brief is still empty.
- **The months path does not exist.** Grid 20. Vico is proven and unused.
- **The model is not integrated.** No dependency, no INTERNET permission. The tag
  grid, the Sunday note, the cards and the visit summary all run without it, and
  that is the shipped path; the reader is an optional 3.66 GB download.
- **Wall push-ups are counted by hand, not by the camera.** Grid 11 says the
  camera counts full reps on the phone. MediaPipe is not integrated, the measure
  works with a tap per repetition, and no permission is requested.
- **Import does not exist.** Export does, and LOGIC.md section 14 asks for both.
- **Health Connect is not integrated.**
- **Step names and instructions are English literals in Ladders.kt**, and the
  week writer's sentences and the measure names are too. They move to resources
  with the translations.
- **strings.xml is English only.** Four locales are declared and three are empty.
  The switching mechanism works and the picker hides itself until there is a
  choice; see BLOCKED in DECISIONS.md for what the translations need.
- **The chair-stand count has never been checked against a human counting.**
  MASTER_SPEC section 10 asks for that as a device test and it cannot be
  automated: somebody has to stand up out of a chair ten times with the phone in
  their pocket and compare. It is the one measurement in the app whose accuracy
  is unverified.

---

## 2. The next concrete steps, in order

1. **The months path**, grid 20, with the milestones renamed. Vico is proven.
2. **Import**, to match the export that exists.
3. **Feeding Try it and see's patterns into the visit-summary brief**, which
   LOGIC.md 13b's co-occurrence section asks for and which is a few lines now that
   both halves exist.
4. **The other five experiment variables**, which are table entries.
5. **The camera counting wall push-ups**, MediaPipe Pose, grid 11.
6. **Health Connect.**
7. **Phase 7**, hardening and release, plus `store-assets/` and `LAUNCH.md`.

The reader itself can be wired at any point: its validator, its fixtures and its
fallbacks are all built, and every feature it touches already works without it. It
is deliberately not on the critical path.

**Ask the owner about the translations before doing anything else on them.** They
are on the BLOCKED list with the reasoning.

---

## 3. Everything tried that did not work

**`gradle wrapper` before the project exists.** It needs `settings.gradle.kts` and
the module directory to exist first. Create them, then generate the wrapper.

**The `room` Gradle extension.** Room 3's extension is `room3 { }`, not `room { }`.
Entities import `androidx.room3.*` and the artifacts are `room3-` prefixed at the
`androidx.room3` group.

**SQLCipher without loading its native library.** Room 3 opens the database
through `SQLCipherDriver`, and nothing loads the native library for you. Without
`System.loadLibrary("sqlcipher")` the first query throws `UnsatisfiedLinkError`.
This cannot be caught by a JVM test, because there is no native library there to
be missing. Found by the Phase 0 smoke test on the device, which is what it is
for.

**Deleting the database by a list of suffixes.** `steady.db-wal`, `-shm` and
`-journal` are not all of it. The device also had `steady.db.lck`. Deleting every
file whose name starts with the database name is the only version that leaves
nothing.

**Vico 3's package names.** Everything moved under
`com.patrykandpatrick.vico.compose.*`; there is no separate `core` artifact.
`lineSeries` is deprecated in favour of `lineModel`, and
`androidx.compose.ui.test.junit4.createComposeRule` is deprecated in favour of the
`v2` one. With `allWarningsAsErrors` on, both are build failures rather than
warnings.

**Bare `mipmap-anydpi`.** Not read by AAPT2, and the launcher icon stops
resolving. Adaptive icons live in unqualified `mipmap/` at minSdk 29.

**Rive.** Measured and dropped for version 1. See DECISIONS.md.

**`Ladders.visibleLadders(exclusions)`.** It now takes the way of getting around
first. A call that passes only a set of exclusions will not compile, which is
deliberate: there is no sensible default for which version of the app somebody is
using.

**Letting `SteadyViewModel` hold everything.** It grew past detekt's size rule
three times. Ask a question, Settings, the check, the ability pages and the
summary are their own view models now, and the split works because Today, Move and
Abilities each reload the profile when they come back into view rather than being
told to.

**Holding the database in a `lazy`.** Deleting everything closes it, and every
view model then threw "Database is closed" on its next write. It crashed on the
first screen of setup, immediately after somebody had deleted everything. The
database is resolved on every use now; the repositories are stateless wrappers so
it costs an allocation.

**Running `connectedDebugAndroidTest` on a phone with data on it.** Gradle
uninstalls both APKs when it finishes and the app's data goes with them. Use
`tools/device-tests.sh`, which installs and runs through adb and leaves everything
alone. Separately, `DatabaseSmokeTest` used to destroy the real database and the
real key; it has its own now.

**Assuming the accessibility tree from a uiautomator dump.** It shows Compose's
merged label on one node and the click action on another, which reads as "every
tile is unlabelled" and is wrong. Use a Compose test and `assertHasClickAction`.

**A dynamic locale change with the default bundle configuration.** Play splits
language resources and fetches the rest on demand, which needs Play Core and a
network. `bundle { language { enableSplit = false } }` is in the build file and
has to stay.

---

## 4. Measurements and facts about this environment

- Device: Pixel 8, Android 17, SDK 37, `39151FDJH00506`. 7.75 GB of memory.
- Local JDK is 26, which AGP will not run on. `source ./gradle-env.sh` sets
  `JAVA_HOME` to JDK 21 at
  `/home/linuxbrew/.linuxbrew/opt/openjdk@21/libexec`. CI uses Temurin 21, so the
  two agree.
- SDK platforms installed: 36, 36.1, 37.0, 37.1. Build tools 36 and 37. NDK 28.2.
- Versions verified against live Maven metadata on 2026-09-06: AGP 9.4.0 (9.5.0 is
  alpha), Kotlin 2.4.10 (2.4.20 is RC), KSP 2.3.11, Compose BOM 2026.08.00,
  Room 3.0.2, SQLCipher 4.18.0, Vico 3.3.1, LiteRT-LM 0.17.0, Health Connect 1.1.0.
- The reader, verified 2026-09-06: `gemma-4-E4B-it-litert-lm.litertlm`, 3.66 GB,
  Apache-2.0, via `com.google.ai.edge.litertlm:litertlm-android:0.17.0`. Entry
  points `Engine`, `EngineConfig`, `Conversation`; `initialize()` can take ten
  seconds. MediaPipe's `.task` format is the legacy path. Constrained decoding is
  not documented, which is why the tag validator is a validator.
- Debug APK with SQLCipher and Vico: **39 MB**. Adding Rive took it to **54 MB**.
- Figtree ships as one variable font, 62 KB, covering weights 400 to 900.

### Contrast, measured

On white and on the ground colour, the approved palette gives: ink 15.3 and 14.4,
ink2 6.0 and 5.7, navy 13.7 and 12.9, and **ink3 2.68 and 2.53**, **orange-d 3.22
and 3.04**, **green 4.21 and 3.98**, all of which fail 4.5:1 as text. White on the
approved orange is **2.54**, which fails even the 3.0 that Android allows at 16 sp
bold. The derived text colours are ink3-text `#6C708F`, orange-text `#BD5114` and
green-text `#287855`, and the primary button is filled orange-d.

---

## 5. Remaining work, by phase

What is left of MASTER_SPEC.md section 9 is the tail of Phase 5, some of Phase 6,
and all of Phase 7. The screens are numbered 1 to 22 in
`design/screen-grid-v2-capability.html`. Of those, 14, 15 and 20 are not built,
and 11 is built with the person tapping rather than the camera counting.

Not yet created, and required by the template before release: `store-assets/` and
`LAUNCH.md`.

---

## 6. Decisions a future session must not reverse without reading

All in DECISIONS.md, with reasoning:

- The palette gives fills and a small set of darker variants give words. Do not
  "fix" the contrast test by relaxing it.
- The primary button is orange-d, not orange.
- Rive is not in version 1.
- The whole schema is at version 1 rather than grown per phase.
- The job 6 validator is built before the model, not after.
- A measure with no published detectable change never moves an ability off Same.
- The repetition counter is allowed to under-count and never to over-count.
- Numbers-off replaces the weight figure and leaves the capability counts alone.
- The language picker shows only languages that have strings behind them.
- Device tests use their own database and their own key.
- The repository history was rewritten deliberately.

---

## 7. Open questions and things waiting on the owner

The BLOCKED list is at the end of DECISIONS.md and is the authoritative version.
In short: publishing PRIVACY.md at kamsiob.com, the Play Console manual steps,
reviewing the twelve cards in CONTENT.md, and the two research items MASTER_SPEC
section 11 marks open (Springer per-cell norms and the Cooper category bands),
both of which ship without the unverified part until somebody checks a source.
