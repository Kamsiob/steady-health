# HANDOFF.md

The resume document. A session with no memory of any previous conversation should
be able to pick this up and continue without repeating work, reversing a decision
it cannot see the reasoning for, or breaking something it does not understand.

Read this in full. Then DECISIONS.md. Then MASTER_SPEC.md and DESIGN.md. Then
`git log`.

**Last updated:** 2026-09-09, with every phase of ADDENDUM-03 Part 21 built.
**Read TEST-ME.md first if you are the owner.** It says what to try and what I need.

---

## 1. Where the work stands

**Read ADDENDUM-03-experience.md first.** It is the final structural overhaul and it
supersedes every earlier phase plan, onboarding, navigation and session
specification. It has been folded into MASTER_SPEC.md, DESIGN.md, LOGIC.md, AI.md,
ONBOARDING.md, COMPLIANCE.md and CONTENT.md, and the merge is recorded in
DECISIONS.md with the commit hash and every superseded decision. The phases below
are Part 21's, not the old section 9's.

**The project was reset on 2026-09-06.** An earlier weight-tracking version existed,
was built to roughly Phase 3, and was deleted. The repository history was rewritten:
the root commit is `dd4c3bd`. Details in DECISIONS.md under "The reset".

**A whole app to the old plan was built and tested on the Pixel between 2026-09-06
and 2026-09-07**, phases 0 to 5 and most of 6, and a 7.5 MB release APK was
delivered. Most of that code is still here and still works. ADDENDUM-03 then
reorganised what the app is around: the session rather than the daily three.

### Done to the new plan

**Phase 1a is done.** The session engine, the pure state machine, the seven screens,
audio with pacing, haptics, the warm up and the cool down, pause, the three exits,
the pain button, and the accelerometer counting stands and steps. All of it runs on
the phone.

**Phase 3 is done and its gate passes.** The camera works on the phone end to end:
"Scan something" opens a chooser with Part 6's four ways in, the page is photographed,
ML Kit reads the words off it on the device, and the classifier asks one of Part 5's
five questions. A therapist's plan is the session when one exists, with the app's own
suggestions in a separate block below and a one-tap switch to turn them off. The
export PDF, the appointment prompt and the model choice screen all landed.

**Phase 4 is done and its gate passes, and the feature ships off.** The job 9 reading
validator, fifty five adversarial fixtures and forty two documents. Turning it on
waits on the attorney review, which is on the BLOCKED list and is not a code task.

**Phase 5 is done.** All four ways of getting around have a real library: seventy
seven movements, every one explicitly tagged, and `Movement.ways` has no default so
the compiler asks about the next one. A fortnight of planned sessions per way found
three engine faults a single plan could not, including that Steady was never offered
to anybody in any version of the app.

**Phase 6 is done.** Passive measure capture and the monthly confirm, the second
optional rating and the sentence it produces, and the first month card.

**Phase 7 is done.** Beyond exercise: twenty five cards, the floor guide, the places
walkthrough, the day between sessions. The card in all four kinds. Job 7 without the
model.

**Phase 8 is done apart from what only the owner can do.** Numbers off with the line
written down, export, import, delete, backup and restore, hardening, the clinician
handout, the store listing, a signing config that reads a key from outside the
repository, and LAUNCH.md. The three translations and the signing key itself are
BLOCKED, not unbuilt.

**Phase 2 is done.** Today in full: what they said they want, the next thing, the
session card, the week line, what the app noticed, "Got a minute?" and "Do it without
the phone". The four tabs of MASTER_SPEC 5 with Settings promoted out from behind a
gear. Sessions with the library, the history, "Do this again", editing and removing a
past session, and logging one after the fact. Progress with four weeks that stand
alone and the look back card. The one daily prompt that stops itself, and the widget.

**Phase 1b is done, and the gate passed.** Onboarding is the five screens
of Part 3, with the first session in the middle of them. The help system is the three
layers of Part 4: a sand block per screen shown once, a question mark in the same
place on every screen that has a topic, and hand written sheets behind it. Today
leads with the session card and carries one noticed line.

### The Phase 1 acceptance gate

Run on the phone and recorded in DECISIONS.md in full. Where it stands:

| Gate item | Result |
| --- | --- |
| Install to a finished first session under 90 seconds | **Pass**, 48 seconds |
| A full session face down, audio only | **Pass as a rule**; sounding right is for the owner |
| One obvious action per screen, by screenshot | **Pass** |
| The three exits from every point, keeping what was done | **Pass** |
| The pain button in one press, suppressing that area | **Pass** |
| Pause survives an interruption | **Pass for home**; the screen lock is for the owner |
| 200% font scale and TalkBack | **Pass**, run on the phone |
| The next session differs after "hard" | **Pass** |
| No banned word on any screen | **Pass** |

The exits from every point are already proved by `SessionRunnerTest`, which presses
each of the three and the pain button from every stage of the pure machine. The
device run is the same four things once each on the real screen.

**The two settings-dependent gate items are tests rather than runs.** Both 200% font
scale and TalkBack would mean changing a system setting, and the standing rule is that
nothing on the owner's phone is touched beyond installing and testing this one app.
`SessionAccessibilityTest` asserts both in the semantics tree instead, which is what
TalkBack reads, and overrides the font scale inside the test. The emulator does not
run on this machine; see BLOCKED.

All 23 instrumented tests pass on the phone, and Phase 2 was walked end to end on it
after the owner unlocked it. `tools/drive.py` drives the app by the text on the screen
and scrolls to what it taps; `tools/walk-phase2.py` prints every tab.

### What is uncommitted or mid-flight

Check `git status` before assuming. Everything through Phase 2 is committed.

### What would break if somebody assumed it was finished

- **Reading a document does nothing yet.** Part 5's "Explain it" keeps the page and
  returns to Today, because Part 7's reader is Phase 4 and behind a flag that stays off
  until an attorney has reviewed the boundary. The validator is being built first, as
  Part 7 requires.
- **Saying a plan out loud needs Android 13 and an on-device recogniser.** All four of
  Part 6's ways in are built and all four end on the one confirmation screen. The
  spoken one uses `SpeechRecognizer.createOnDeviceSpeechRecognizer` only, and where a
  phone cannot do that the screen says so in a sentence and offers typing. There is no
  fallback on purpose: the recogniser that would work sends the audio to a server.
  Nobody has yet run it on a phone that has the on-device recogniser installed.
- **Start does not run the therapist's plan.** This is the biggest thing Part 6 asks
  for that is not built. Part 6: "when a therapist's plan exists, IT IS the session
  ... and it is what Start runs". Today's card and the Sessions card do show the plan,
  labelled, with the app's own suggestions kept separate below, and a second
  therapist's plan sits beside the first with its own name. But `startTodays` plans
  through `SessionEngine` from the person's own history and never opens the plan
  table, so the button under the plan runs the app's session. What was done against
  the plan, and the export's "what was prescribed, what was done and when", both wait
  on this.
- **The appointment page is the first plan only.** `TherapistPageViewModel.share`
  takes `live().firstOrNull()`, so a second therapist's plan is not on the page taken
  to the appointment. The brief, the pages and the PDF are all written around one
  plan.
- **Nothing downloads.** No INTERNET permission, no model, no download screen wired.
- **Progress is still the old Abilities grid** with the four weeks and the look back
  added on top. MASTER_SPEC 5 wants the months, the numbers, the card and the
  therapist export there too; those are Phases 6 and 7.
- **MoveScreen and MoveUiState still exist and are dead.** The Sessions tab replaced
  them and nothing routes to `MoveScreen` any more. It is left in place rather than
  deleted because the walk ladder logic behind it is still what the walking movements
  read, and untangling that belongs with Phase 5.
- **The library has seventy seven movements**, and the help sentence still says
  "sixty or so". On feet and with a walker see fifty of them, a wheelchair user
  thirty five, somebody in bed thirty one. Adding one is a table entry in
  `Movements.kt`, and `Movement.ways` has no default, so the compiler asks which of
  the four versions it belongs to.
- **Only five screens have a help topic.** A screen with no topic has no dot, which
  is honest but incomplete. Each gets one as its screen is rebuilt.
- **The model is not integrated.** No dependency, no INTERNET permission.
- **MedGemma is required again** by ADDENDUM-03 Part 7, reversing the Phase 0
  decision not to use it. Nothing is built for it and it stays behind a flag until an
  attorney has reviewed the boundary. See BLOCKED.
- **The camera counts nothing.** MediaPipe is not integrated.
- **Import does not exist.** Export does.
- **strings.xml is English only.** Four locales are declared and three are empty.
- **The movement library's copy is English literals in Kotlin**, by design until the
  translations land in Phase 8.
- **The chair-stand count has never been checked against a human counting.** It is
  the one measurement in the app whose accuracy is unverified, and it now matters
  more, because the accelerometer counts reps inside a session.
- **Kotlin is pinned to 2.4.10 and 2.4.20 is stable.** `lintRelease` failed on the
  upgrade and it was left alone rather than chased mid-phase.

---

## 2. The next concrete steps, in order

1. **Once the phone is unlocked**: run `tools/device-tests.sh`, then walk TEST-ME.md
   steps 10 to 20, which are Phase 2 and have never been seen running.
2. **Phase 3**: the camera and the therapist's plan, with its gate.
4. **Phase 4**: report and letter reading with MedGemma, behind the flag.
5. **Phases 5 to 8** as written in ADDENDUM-03 Part 21.
6. **TEST-ME.md**, Part 22, which is the last thing produced.

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

**A gate script of fixed coordinates and sleeps.** The first attempt at timing
install to a finished first session tapped by coordinate every 0.9 seconds. Its
first tap landed before the app had drawn, was swallowed, and the run carried on
pressing whatever happened to be at those coordinates while reporting a time that
meant nothing. `tools/drive.py` waits for the text it is aiming at and taps the
middle of it, and fails loudly when it never appears.

**Two device scripts at once.** Both drive the same phone, and one of them clears
the app's data while the other is halfway through a session. It also makes
`uiautomator dump` come back empty, which reads as "the screen is blank" rather
than "something else is dumping". Kill the previous run before starting another.

**`am start` straight after `am force-stop`.** The activity can land in a process
that is still being torn down, and what comes up is a half-drawn screen: the tab
bar, and every piece of state still at its default. A second between them is
enough. Today now also refreshes on every resume rather than once, so a screen
whose one load did not finish is not stuck that way.

**Piping a long-running script to `tail`.** No output at all until it exits, which
looks exactly like a hang. Write to a file and read the file.

**Reading `./gradlew ... | tail -2`.** It cuts off the line that says BUILD FAILED,
so a broken build reads as a successful one and the next `adb install` quietly
installs the previous APK. Grep for BUILD and FAILED instead.

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
- **The emulator does not run on this machine.** `avdmanager` creates an AVD and
  `emulator` starts, reaches "Emulator is performing a full startup", and the process
  exits without an error in its log. Tried windowed and headless, `swiftshader_indirect`
  and the default GPU, on a fresh `steady-gate` AVD from
  `system-images;android-36;google_apis;x86_64`. `/dev/kvm` is `crw-rw-rw-`, so it is
  not a permissions problem. Everything that needs a device therefore needs the phone.
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

The phases are ADDENDUM-03 Part 21's, repeated in MASTER_SPEC 9. Phase 1a and most
of 1b are done; Phases 2 to 8 are not started. The old screen grid in
`design/screen-grid-v2-capability.html` is still a useful reference for the screens
that carry over, but it is not the plan any more: the addendum's own screen
descriptions win where they disagree.

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
reviewing the cards in CONTENT.md, the two research items MASTER_SPEC 11 marks open
(Springer per-cell norms and the Cooper category bands), the three translations, and
the one ADDENDUM-03 added: **a health tech attorney has to review the HAI-DEF
Clinical Use boundary** before the report reading of Part 7 can be turned on. Phase 4
builds it behind a flag that stays off until that clears.

One thing needs the owner rather than a lawyer: **standing up out of a chair ten
times with the phone in a pocket and comparing the count.** The accelerometer now
counts reps inside a session and nobody has checked it against a person.
