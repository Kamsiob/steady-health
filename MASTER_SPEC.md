# MASTER_SPEC.md: Steady Health by Kamsiob

Precedence: this document, DESIGN.md, LOGIC.md, AI.md, ONBOARDING.md, CONTENT.md, COMPLIANCE.md, PRIVACY.md, DECISIONS.md, and the open GitHub issues are the current source of truth. Anything in an earlier prompt, an earlier repository state, or a research report that conflicts with them is superseded. The research reports in research/ are evidence, not instructions.

This specification replaces an earlier weight-tracking version of the app. That version is kept only as design/screen-grid-v1-weight-reference.html for visual reference; nothing in it is a requirement. Where the two differ, this document wins.

## 1. What it is
Steady Health is a free, open-source Android app that keeps a record of what a person's body can do, and what changes it. It is not a weight-loss app. Weight is tracked and shown as one of the levers that make things easier, never as the score.

Positioning line: "What can you do?"
Full name everywhere the app introduces itself: Steady Health by Kamsiob.

Everything runs on the phone. No account, no server, no analytics, no ads, no subscription. AGPL-3.0. An optional on-device model (Gemma 4 E4B) does four visible jobs described in AI.md, of which the visit summary is the principal one.

## 2. Why capability and not weight
Gait speed alone predicts survival about as well as a panel of age, sex, chronic conditions, smoking, blood pressure, BMI and hospitalization combined (Studenski, JAMA 2011). Grip strength beats systolic blood pressure as a mortality predictor (PURE, Lancet 2015). The sitting-rising test's lowest scorers had 3.8 to 6 times the mortality of the highest over 12 years, and each point is worth about 21% better survival. Function is the better measure and the one people feel. Full evidence in research/04.

## 3. Who it is for, and the three directions
Replaced by ADDENDUM-03, merged on commit 9837f2a.

Adults from around 50 onward, wherever mobility starts to slip. The app never names an
age or a stage. "Senior", "elderly", "frail", "decline" and "fall risk" stay off the
interface.

The app was first written as though everyone is holding onto what they have. That is
wrong for many of the people who will use it. Three directions exist and the app
speaks to all three without asking anyone to declare which they are:
- **Rebuilding**, after an illness, an operation, or a long slow drift.
- **Keeping** what they have, deliberately, which is real work.
- **Building**, wanting to be stronger than they have been in twenty years.

**Direction is inferred, never asked.** It comes from what the person said they wanted
and from what their numbers do over the first month. It changes only the register of
copy templates, never the logic. Every template has three registers and the engine
picks one:
- Rebuilding: "You're doing more than you were a month ago."
- Keeping: "Same as last month, which takes doing."
- Building: "Fourteen. That's four more than when you started."

Banned in all registers: anything implying the ceiling is holding steady, anything
implying improvement is unlikely, anything congratulating someone for not getting
worse, anything treating maintenance as second best. "Steady" here means reliable, not
stuck.

The positioning line stays "What can you do?" because it works for all three.

Also served, with their own version of the app: people who use a walker or cane,
people who use a wheelchair, and people who are mostly in bed or a chair for now. A
person in physical or occupational therapy has their therapist's programme as their
session; see 6.19.

## 4. The four abilities
Get up. Off the floor, out of a chair.
Go. Stairs, distance, pace. For a wheelchair user, wheeling distance.
Carry. Grip, upper body, carrying things.
Steady. Balance, reach, staying sure-footed.

Every measure, exercise, and sentence in the app belongs to one of these four. There is never a combined capability score; a single number would rebuild the scoreboard the app exists to remove.

## 5. Navigation
Four tabs, from ADDENDUM-03 Part 20, replacing the earlier three.

**Today.** What you said you want; the next thing; today's session, which is the
therapist's plan when one exists and the app's otherwise, with its reason and any
adaptation sentence; the app's extras below if enabled; the week line; what the app
noticed; "Got a minute?".

**Sessions.** Today's session and change it; the therapist's plan; "Scan something";
the library with Try this now; history with Do this again; log a past session.

**Progress.** What you're working toward; what changed; the four week view; the look
back card; the months; the numbers; weight if on; make a card; add something new; the
therapist export.

**You.** Your list; how you get around; equipment and chair; anything to leave out;
your therapist and review date; your documents; what the app can read; the places you
move through; reminders; audio; the cards that answer questions; your data; about.

Every tab has the help dot in the same place. Every screen explains itself once.

Screens numbered 1 to 22 in design/screen-grid-v2-capability.html are the earlier
grid. Where ADDENDUM-03 and the grid disagree on flow or navigation, the addendum
wins; the grid still governs the look.

## 6. Features
### 6.1 Today
The session card is the screen. In order: what you said you want; the next thing;
today's session, with its length, its movements, one line saying what it feeds and any
adaptation sentence; the app's extras below if a therapist's plan is running and the
extras are on; the week line; what the app noticed; and "Got a minute?".

Weight is not on Today and never has its own tab. It is off by default, and when on it
does one job: it appears in a sentence beside an ability that changed. "Your legs got
stronger and there are eighteen fewer pounds to carry up those stairs." Smoothing and
the wall logic in LOGIC.md 1 are unchanged. ADDENDUM-03 Part 18.

### 6.2 Your words become what is tracked (grid 3, 4)
At setup the person says what they would like to be able to do, in their own words, by voice or keyboard. The model maps the sentence onto tracked abilities and shows them for confirmation. The person rates each 0 to 10. This is the Patient-Specific Functional Scale; the app never says so. Re-rated monthly.

### 6.3 Sessions
Today's session and change it; the therapist's plan; "Scan something"; the library
with Try this now; history with Do this again; log a past session. The session itself
is 6.11 and is the centre of the app.

### 6.4 Progress
What you're working toward; what changed; the four week view (ADDENDUM-03 Part 10);
the look back card; the months; the numbers; weight if on; make a card; add something
new; the therapist export. The four abilities and their detail pages live here.

Consistency is four bars, each week against its own target. Every week stands alone: a
quiet week does not erase the three beside it, and nothing is ever broken, lost or
reset.

### 6.5 The monthly check (grid 10, 11, 12, 13)
Three or four measures, ten minutes, chair and wall only. The phone times chair stands from its motion sensor and counts wall push-ups with the camera. Results are reported as life first, then the numbers. Same is reported as a result. Decline is reported once, quietly, with everything that held beside it.

### 6.6 Try it and see
Deferred to version 2 by ADDENDUM-03 Part 21. The engine and its screens exist and are
not offered.

### 6.7 The measures table (grid 21)
Function first, then the levers, as a table of rows and numbers. Nothing transmitted. From Phase 5 this table sits underneath the written visit summary (6.10) and is exported with it as one page. It was called "For your doctor" before the summary existed.

### 6.8 Ask a question (grid 22 in v1 grid; CONTENT.md)
Eleven hand-written cards. The model only picks which to show.

### 6.9 You
Your list; how you get around; equipment and chair; anything to leave out; your
therapist and review date; your documents; what the app can read; the places you move
through; reminders; audio; the cards that answer questions; your data; about.

### 6.10 The visit summary
The engine, the brief, the job 6 validator and its corpus are built and tested. The
interface is deferred to version 2 by ADDENDUM-03 Part 21. The therapist export (6.19)
is the artifact that matters first, and it is a different document for a different
reader.

### 6.11 The session
The centre of the app, specified in ADDENDUM-03 Part 1 and folded into LOGIC.md 15 and
DESIGN.md 5b. Four to eight minutes in which the app leads, seven screens, audio led,
with pacing, a warm up, a cool down, a pause that survives anything, three exits and a
pain button. A person should be able to put the phone on a table, do what it says, and
never decide anything mid-session.

### 6.15 The card
Sharing to exactly one other person, specified in ADDENDUM-03 Part 11. A rendered
1080x1350 image in the app's own design language, four kinds, always editable before
sending, through the system share sheet. No feed, no comparison, no account, no
server, nothing inbound.

### 6.17 Help
Three layers, specified in ADDENDUM-03 Part 4 and folded into DESIGN.md 4b. Every
screen explains itself once; every screen has a help dot in the same place; every
number explains itself. There is no "I'm lost" button, because a permanent one is an
admission of failure.

### 6.18 Motivation
Specified in ADDENDUM-03 Part 9. What the app noticed; "Got a minute?"; the look back
card; the next thing. All four are the person's own history said back to them, and
none of them is a streak, a badge, or a target.

### 6.19 The camera and the therapist's plan
Specified in ADDENDUM-03 Parts 5 and 6 and folded into LOGIC.md 17. One "Scan
something" button for every piece of paper, on-device text extraction, and then one
question about what the app is looking at.

When a therapist's plan exists **it is the session**, labelled as theirs, and the
app's own suggestions sit below it as "Also, if you want more", never merged. The app
runs the plan exactly as given: it does not add to it, remove from it, progress it, or
interpret it. The export back to the therapist is a one page PDF and is the artifact
that makes a clinician recommend the app to their next patient.

### 6.20 Beyond exercise
Specified in ADDENDUM-03 Part 8. The day between sessions; the places you move
through; sleep and energy, lightly; getting up from the floor, taught; and about
twenty-five hand-written cards. All optional and off by default except the passive
day-between-sessions line.

### 6.21 Reading reports and letters
Specified in ADDENDUM-03 Part 7 and folded into AI.md job 9, LOGIC.md 18 and
COMPLIANCE.md. The app explains the words in a document the person already holds. It
never interprets them, never says whether the news is good or bad, and never advises.
MedGemma 1.5 4B, an optional separate download, behind a flag that stays off until a
health tech attorney has reviewed the boundary.

## 7. What is deliberately absent
Calorie counting or any food logging. Streaks, chains, badges, scores. Numeric targets set by the app. Anything red. A combined capability score. Any diagnosis, condition name, or inference of one. Medication tracking. Any social feature. Any cloud service.

## 8. Platform
Kotlin, Jetpack Compose, Material 3 Expressive with a fully custom theme implementing DESIGN.md, single activity, SQLCipher database keyed from the Android Keystore, bundled Figtree, Vico for charts, MediaPipe Pose Landmarker for rep counting, ML Kit text recognition on device for scanned documents, Android TextToSpeech for the session audio, the device accelerometer for movement timing, Health Connect optional, two optional models, Gemma 4 E4B and MedGemma 1.5 4B, each a separate user-initiated download via the current recommended on-device path with the memory rules in standards/kamsiob-project-template.md section C7. Verify all versions and the current Play target API at build time.

## 9. Phases
Replaced by ADDENDUM-03 Part 21. Every phase is built without stopping for approval,
and each gate is a check run on the physical device with its result recorded in
DECISIONS.md.

**Phase 1a.** The session engine end to end for the on-feet path: audio led with
pacing, warm up and cool down, pause, the three exits and the pain button, motion, at
the full accessibility bar. One session hardcoded so it runs on device early.
**Phase 1b.** Onboarding (Part 3), the help system (Part 4), Today with the session
card and one noticed line.

**Phase 1 acceptance gate**, run before anything past 1b. Listed in full in
ADDENDUM-03 Part 21 and recorded in DECISIONS.md.

**Phase 2.** Today in full including motivation, the week and consistency, the library
and history, correction and phone-free sessions, the daily prompt and widget.
**Phase 3.** The camera and the therapist's plan in all four ways in, the export and
review date, the model download screen and the two-model choice. Gate: the plan and
the app's suggestions are visibly separate, and a movement in both is counted once.
**Phase 4.** Report and letter reading with MedGemma, its validator and its fixtures,
behind a flag that is off until the attorney review clears. Gate: all thirty fixtures
pass and every adversarial case is caught.
**Phase 5.** The other ways of getting around; exclusions, readiness, pacing mode;
interruptions and context; ceilings and variants.
**Phase 6.** Passive measure capture, the monthly confirm, Progress in full,
confidence and weight, the first month arc.
**Phase 7.** Beyond exercise, the card in all four kinds, the Sunday review, the
remaining model jobs.
**Phase 8.** Numbers-off mode, four languages with RTL, export, import, delete, backup
and restore, hardening, the clinician handout, store assets, signed bundle, LAUNCH.md.

Deferred to version 2: exercise animations (VISUALS.md), the visit summary interface
(ADDENDUM-01), and Try it and see.

## 10. Testing
Unit tests for every rule in LOGIC.md with thresholds as named constants, including every visit-summary question-candidate rule and its boundary cases at 5 days, 2 rating points, and three consecutive checks. Fixture tests for every model job. A validator corpus of at least 30 deliberately bad job 6 outputs, each of which must be caught. Instrumented tests for: onboarding to first ability under two minutes; the accelerometer chair-stand count against a manual count on device; numbers-off mode showing no digits anywhere; the two-a-week reminder ceiling; delete leaving no files; a visit summary generated from a seeded six-month database, asserting that every numeral on the rendered page appears in the brief; and the summary rendering correctly with the model absent, with the model failing, and with all three paragraphs failing validation. Full user-testing protocol from the template in every theme, largest font sizes, TalkBack, fresh install and upgrade, offline throughout.

## 11. Open questions, marked open
The exact on-device paths for Gemma 4 E4B and MedGemma 1.5 4B on the current Android
release, verified at build time rather than trusted from here. Whether MediaPipe rep
counting holds above 80% in real home conditions with older users; if not, drop
automated counting to optional and lead with self-report. Whether the accelerometer
chair-stand count matches a human counting, which needs a person and a chair. Springer
single-leg per-cell norms (ship without until verified). Cooper category bands (formula
only until verified). Dark theme (not designed; light only). The HAI-DEF Clinical Use
boundary for 6.21, which is an attorney's question and is on the BLOCKED list.

## 12. Discovery
ADDENDUM-03 Part 19. A one page printable handout, A4 and US Letter, for a clinician to
hand a patient: that it holds the home programme they were just given, what the app
does in four lines, free with no account and no ads, nothing leaves the phone, the QR
code and store link, and three ruled lines for the clinician to write what to start
with. Black and white, legible on a bad office printer. It leads with the home
programme, because that is the reason it works.

The store listing leads with what the person will be able to do. The first screenshot
is the session mid set.
