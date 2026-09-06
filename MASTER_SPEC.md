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

## 3. Who it is for
Primary: adults roughly 50 to 70 who are not frail, are working or recently retired, gained weight gradually over decades, have watched a parent lose physical independence, and are motivated by staying the person who can. The word senior never appears in the app.

Also served, with their own version of the app: people who use a walker or cane, people who use a wheelchair, and people who are mostly in bed or a chair for now. A person in physical or occupational therapy can enter what their therapist gave them as their own list.

The tone never implies decline, never mentions falls or frailty, and never ranks the person against anyone.

## 4. The four abilities
Get up. Off the floor, out of a chair.
Go. Stairs, distance, pace. For a wheelchair user, wheeling distance.
Carry. Grip, upper body, carrying things.
Steady. Balance, reach, staying sure-footed.

Every measure, exercise, and sentence in the app belongs to one of these four. There is never a combined capability score; a single number would rebuild the scoreboard the app exists to remove.

## 5. Navigation
Three tabs: Today, Move, Abilities. A question mark on Today opens Ask a question. A gear opens Settings. Nothing else is a destination. Screens are numbered 1 to 22 in design/screen-grid-v2-capability.html and referenced by those numbers below.

## 6. Features
### 6.1 Today (grid 5, 6, 7)
Four ability tiles at the top, each with one sentence about life ("Floor without hands. New this month."). Then the daily three: weigh in, say how today went, move. Then the week row. The tiles and the daily three both change with how the person gets around; screens 5, 6 and 7 show the same screen for someone on their feet, in a wheelchair, and mostly in bed.

### 6.2 Your words become what is tracked (grid 3, 4)
At setup the person says what they would like to be able to do, in their own words, by voice or keyboard. The model maps the sentence onto tracked abilities and shows them for confirmation. The person rates each 0 to 10. This is the Patient-Specific Functional Scale; the app never says so. Re-rated monthly.

### 6.3 Move (grid 16, 17, 18)
What the person said they want sits at the top. Today's session is one strength set that feeds an ability plus a walk or wheel or bed set, with Go. The ladders, the talk test, the two-easy-sessions unlock, the 110% spike cap, the gap decay, and pacing mode all carry over unchanged from LOGIC.md.

### 6.4 Abilities (grid 8, 9, 19, 20)
Four rows with Better or Same and the life sentence. Tapping one opens the ability in depth: the life sentence, the measures behind it, and what fed it, including weight in plain words. Weight has its own page inside this tab with the smoothed line and the expected shape. The months path is here too.

### 6.5 The monthly check (grid 10, 11, 12, 13)
Three or four measures, ten minutes, chair and wall only. The phone times chair stands from its motion sensor and counts wall push-ups with the camera. Results are reported as life first, then the numbers. Same is reported as a result. Decline is reported once, quietly, with everything that held beside it.

### 6.6 Try it and see (grid 14, 15)
After six weeks of check-ins the app may offer a two-week test of one change it noticed in the person's own words. It states what it will measure, reports the result honestly including no difference, and can be turned off in settings.

### 6.7 The measures table (grid 21)
Function first, then the levers, as a table of rows and numbers. Nothing transmitted. From Phase 5 this table sits underneath the written visit summary (6.10) and is exported with it as one page. It was called "For your doctor" before the summary existed.

### 6.8 Ask a question (grid 22 in v1 grid; CONTENT.md)
Eleven hand-written cards. The model only picks which to show.

### 6.9 Settings and data (grid 22)
How you get around, working with a therapist, weigh in, show numbers, try it and see, the reader, anything to leave out, language, reminders, export, delete, Made with, Support this work. Under Your data, one row: Visit summary, subtitled "Read it, or export a page for your appointment."

### 6.10 The visit summary
One page, written on the phone from the person's own six months of data, saying what changed, what they mentioned, and what is worth asking about. Generated on demand from the Abilities tab and from the ability detail pages. Two outputs: read it on screen, or export a PDF that also carries the measures table (6.7). Requires at least eight weeks of data and at least two monthly checks; below that the button explains what is still needed instead of producing a thin page.

This is the app's principal use of the language model and the feature the AI story rests on. Everything else the model does is bounded extraction; this is synthesis across months of measures, ratings, sentences, sessions and weight, deciding what matters enough to say. There is no rule that can be written for that, which is why it is the model's job.

The engine does all selection, all arithmetic, and all thresholding, and hands the model a structured brief in which every fact carries an id. The model never sees a database, never computes, and never states a number it was not given. Every factual claim is checked against the source data before the page renders, and any sentence that cannot be traced is dropped. That validator is the load-bearing engineering here and is built before the model is wired in. LOGIC.md 13b and AI.md job 6.

## 7. What is deliberately absent
Calorie counting or any food logging. Streaks, chains, badges, scores. Numeric targets set by the app. Anything red. A combined capability score. Any diagnosis, condition name, or inference of one. Medication tracking. Any social feature. Any cloud service.

## 8. Platform
Kotlin, Jetpack Compose, Material 3 Expressive with a fully custom theme implementing DESIGN.md, single activity, SQLCipher database keyed from the Android Keystore, bundled Figtree, Vico for charts, Rive for state-driven animation, MediaPipe Pose Landmarker for rep counting, the device accelerometer for movement timing, Health Connect optional, Gemma 4 E4B via the current recommended on-device path with the memory rules in standards/kamsiob-project-template.md section C7. Verify all versions and the current Play target API at build time.

## 9. Phases
Phase 0: repository, scaffolding, theme from DESIGN.md, database, three-tab shell, screenshot script with foreground guard, CI. Smoke-test Vico and Rive on device.
Phase 1: onboarding through the first weigh-in and first ability (ONBOARDING.md), Today for the on-your-feet path, the daily three, Abilities with self-rated items only. Daily-usable at the end.
Phase 2: the other three ways of getting around, each with its own Today tiles, Move sets, and measures. Exclusions, readiness flags, pacing mode, gap decay.
Phase 3: the model. Words-to-abilities, tagging, the Sunday write-up, card retrieval, with fixtures and fallbacks.
Phase 4: the monthly check. Accelerometer timing for chair stands, MediaPipe counting for push-ups and band rows, results reported as life, the decline path, the measures table.
Phase 5: Try it and see, and the visit summary. Pattern detection, the offer, the two-week protocol, the honest result. Then the summary brief, the job 6 validator built before the model is wired in, the summary page, and the PDF. Both depend on the same longitudinal data and the same validator infrastructure.
Phase 6: numbers-off mode, four languages with RTL, text size, TalkBack, reminders with the two-a-week ceiling, Health Connect, export, import, delete.
Phase 7: hardening and release per the template.

Visuals for the exercise library are deferred to a later version. See VISUALS.md. Ship version 1 with text instructions and form cues only, written so they stand alone.

## 10. Testing
Unit tests for every rule in LOGIC.md with thresholds as named constants, including every visit-summary question-candidate rule and its boundary cases at 5 days, 2 rating points, and three consecutive checks. Fixture tests for every model job. A validator corpus of at least 30 deliberately bad job 6 outputs, each of which must be caught. Instrumented tests for: onboarding to first ability under two minutes; the accelerometer chair-stand count against a manual count on device; numbers-off mode showing no digits anywhere; the two-a-week reminder ceiling; delete leaving no files; a visit summary generated from a seeded six-month database, asserting that every numeral on the rendered page appears in the brief; and the summary rendering correctly with the model absent, with the model failing, and with all three paragraphs failing validation. Full user-testing protocol from the template in every theme, largest font sizes, TalkBack, fresh install and upgrade, offline throughout.

## 11. Open questions, marked open
Rive Android runtime licence at the version used. The exact Gemma 4 E4B on-device integration path on the current Android release. Whether MediaPipe rep counting holds above 80% in real home conditions with older users; if not, drop automated counting to optional and lead with self-report. Springer single-leg per-cell norms (ship without until verified). Cooper category bands (formula only until verified). Dark theme (not designed; light only).
