# HANDOFF.md

The resume document. A session with no memory of any previous conversation should
be able to pick this up and continue without repeating work, reversing a decision
it cannot see the reasoning for, or breaking something it does not understand.

Read this in full. Then DECISIONS.md. Then MASTER_SPEC.md and DESIGN.md. Then
`git log`.

**Last updated:** 2026-09-06, after Phase 0 completed and CI went green on the
fresh repository.

---

## 1. Where the work stands

**The project was reset on 2026-09-06.** An earlier weight-tracking version of
this app existed, was built to roughly Phase 3, and was deleted. The capability
specification in this folder replaces it entirely and the old tree is not a
starting point. The repository history was rewritten: the root commit is
`dd4c3bd` and nothing before it is reachable. Details in DECISIONS.md under "The
reset".

**Phase 0 is complete. Phase 1 has started: the component library exists, no
screens do.**

What runs on the phone right now: the app installs, opens to a three tab shell
(Today, Move, Abilities) with the real theme, the bundled Figtree, and the
Phosphor tab icons. The three tab screens are placeholders that say so.

What is proven rather than assumed:

- The encrypted database opens, holds a row, and the file on disk is not readable
  as plain SQLite. Instrumented, on device.
- Deleting leaves no database file and no Keystore key. Instrumented, on device.
- Vico renders a line chart. Instrumented, on device.
- Every colour pair in the palette meets its contrast threshold, and nothing in
  the palette is red. Unit test.
- Every shipped string passes the banned word list, the dash rule and the
  no-shouting rule. Unit test, reading the real resource file.

CI ran green on the fresh repository (run 34063225991, 5m15s): assemble, unit
tests, lint with warnings as errors, and detekt at zero.

Built and compiling but not yet used by any screen: `SteadyScreen` (the section 4
anatomy, taking the status bar inset once so no screen has to remember),
`TopRow`, `SectionTitle`, `Paragraph`, `PrimaryButton`, `SecondaryButton`,
`Pill`, `ThreeUpChoice`, `TextLink`, and the glyph language (weigh, talk, move,
sun, moon, glyph tile).

**The very next concrete step:** the rest of the components DESIGN.md section 3
names, which Phase 1 needs: Hero, Daily card, Block, List item, Ability tile,
Ability row, Life card, Rating row, Week row, Dial. Then ONBOARDING.md screen by
screen from screen 1.

### What is uncommitted or mid-flight

Nothing at the last commit. Check `git status` before assuming.

### What would break if somebody assumed it was finished

- **There are no screens.** Three placeholders. Nothing in ONBOARDING.md is built.
- **Nothing writes to the database except the smoke test.** The DAOs exist and no
  repository sits on top of them yet.
- **The engine does not exist.** Not one rule from LOGIC.md is implemented.
- **The model is not integrated.** No dependency, no INTERNET permission.
- **strings.xml has about fifteen strings in it.** English only.

---

## 2. The next concrete steps, in order

1. **Phase 1**, which is the largest single piece of value: onboarding through the
   first weigh-in and the first ability, Today for the on-your-feet path, the
   daily three, and Abilities with self-rated items. ONBOARDING.md is screen by
   screen with final copy. The measured target is the first weigh-in and the first
   tracked ability both saved inside two minutes.
2. **Phase 2**, the other three ways of getting around.
3. **Phase 3**, the model. Verify the integration path at build time rather than
   trusting AI.md, which says so itself.
4. **Phase 4**, the monthly check. The accelerometer and MediaPipe work is the
   largest unknown in the build.
5. **Phase 5**, Try it and see, and the visit summary. Build the job 6 validator
   and its adversarial corpus **before** the model is wired in. ADDENDUM-01 is
   explicit about that and the reasoning is in DECISIONS.md.
6. **Phases 6 and 7.**

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

Everything in MASTER_SPEC.md section 9 except Phase 0. Nothing in Phases 1 to 7
has been started. The screens are numbered 1 to 22 in
`design/screen-grid-v2-capability.html` and MASTER_SPEC section 6 references them
by number.

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
