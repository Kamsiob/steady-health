# HANDOFF.md

The resume document. A session with no memory of any previous conversation should
be able to pick this up and continue without repeating work, reversing a decision
it cannot see the reasoning for, or breaking something it does not understand.

Read this in full. Then DECISIONS.md. Then MASTER_SPEC.md and DESIGN.md. Then
`git log`.

**Last updated:** 2026-09-06, after Phase 3. Phases 0 to 3 are done and were
driven on the Pixel 8 rather than asserted.

---

## 1. Where the work stands

**The project was reset on 2026-09-06.** An earlier weight-tracking version of
this app existed, was built to roughly Phase 3, and was deleted. The capability
specification in this folder replaces it entirely and the old tree is not a
starting point. The repository history was rewritten: the root commit is
`dd4c3bd` and nothing before it is reachable. Details in DECISIONS.md under "The
reset".

**Phases 0 to 3 are done and verified on the phone. Phases 4 to 7 have not
started.**

What runs on the phone right now: setup, all four versions of the app, the daily
three, walks with the talk test and the offer, settings, the tag grid, the Sunday
write-up and Ask a question.

What is proven rather than assumed:

- The encrypted database opens, holds a row, and the file on disk is not readable
  as plain SQLite. Instrumented, on device.
- Deleting leaves no database file and no Keystore key. Instrumented, on device.
- Vico renders a line chart. Instrumented, on device.
- Every colour pair in the palette meets its contrast threshold, and nothing in
  the palette is red. Unit test.
- Every shipped string passes the banned word list, the dash rule and the
  no-shouting rule. Unit test, reading the real resource file.

Driven on the Pixel 8, not asserted: setup; a weigh-in; a check-in with tags that
survive a save and a reopen; two qualifying walks producing the offer, accepting
it, and the next walk reading four minutes; switching to a wheelchair and to the
bed version and back from settings; a bed session with its three parts and "How do
you feel" in place of the talk test; turning pacing mode on from the pattern
question and off from its own screen; asking a question and reading a card.

Built and tested: the theme, the encrypted database, the component library from
DESIGN.md section 3, the eleven setup screens, Today, Move, Abilities, the daily
three, the walk and the bed set, the offer, settings and its four sub-screens,
the tag grid, the Sunday write-up, and Ask a question with the thirteen cards.

The engine is pure and has about a hundred tests: weight smoothing, the eight
ladders as a table, progression with every threshold a named constant, the four
ways of getting around, pacing mode, gap decay, the tag validator against a
thirty-seven case corpus, the week writer against every shape of week, and the
card search against twenty-six questions somebody would actually ask.

**The very next concrete step:** Phase 4, the monthly check. It is why every
ability still reads Same: Better, Same and Quieter come from measures, and there
are no measures yet. It is also the largest unknown left in the build, because it
is the accelerometer and MediaPipe rather than more Compose.

### What is uncommitted or mid-flight

Nothing at the last commit. Check `git status` before assuming.

### What would break if somebody assumed it was finished

- **The abilities never change.** Every one reads Same, because Better, Same and
  Quieter come from the monthly check, which is Phase 4. There is no check yet.
- **The ability tiles carry the person's own words, not a measured sentence.**
  The life sentence from LOGIC.md 3b needs measures behind it.
- **Tapping an ability only switches tabs.** The detail page (grid 9), the weight
  page (grid 19) and the months path (grid 20) are not built.
- **The model is not integrated.** No dependency, no INTERNET permission. The tag
  grid, the Sunday note and the cards all run without it, and that is the shipped
  path; the reader is an optional 3.66 GB download that has not been wired.
- **The visit summary does not exist.** Neither does its validator, which
  ADDENDUM-01 requires to be built first.
- **Step names and instructions are English literals in Ladders.kt**, and the
  week writer's sentences are English literals too. Both move to resources in
  Phase 6, which is where the four languages are.
- **strings.xml is English only.** Four locales are declared and three are empty.
- **Reminders, Health Connect, export, import and delete do not exist.**

---

## 2. The next concrete steps, in order

1. **Phase 4**, the monthly check. Chair stands timed from the accelerometer, wall
   push-ups counted with MediaPipe, results reported as life first and numbers
   second, Same as a result, and the Quieter path shown once and quietly. This is
   what makes the four abilities real, and it is the largest unknown left.
2. **Phase 5**, Try it and see, and the visit summary. Build the job 6 validator
   and its adversarial corpus **before** the model is wired in. ADDENDUM-01 is
   explicit about that and the reasoning is in DECISIONS.md.
3. **Phase 6**, numbers-off, the four languages with RTL, text size, TalkBack,
   reminders with the two-a-week ceiling, Health Connect, export, import, delete.
4. **Phase 7**, hardening and release, plus `store-assets/` and `LAUNCH.md`.

The reader itself can be wired at any point after its jobs have fixtures, and it
is deliberately not on the critical path: every feature it touches already works
without it.

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
twice. Ask a question and Settings are their own view models now, and the split
works because Today, Move and Abilities each reload the profile when they come
back into view rather than being told to.

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

Phases 4 to 7 of MASTER_SPEC.md section 9. The screens are numbered 1 to 22 in
`design/screen-grid-v2-capability.html` and MASTER_SPEC section 6 references them
by number. Of those, screens 9 to 15 and 19 to 21 are not built.

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
- The repository history was rewritten deliberately.

---

## 7. Open questions and things waiting on the owner

The BLOCKED list is at the end of DECISIONS.md and is the authoritative version.
In short: publishing PRIVACY.md at kamsiob.com, the Play Console manual steps,
reviewing the twelve cards in CONTENT.md, and the two research items MASTER_SPEC
section 11 marks open (Springer per-cell norms and the Cooper category bands),
both of which ship without the unverified part until somebody checks a source.
