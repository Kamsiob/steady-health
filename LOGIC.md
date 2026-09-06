# LOGIC.md: the engine behind Steady Health

Everything the app decides is decided here, deterministically, on device. The on-device model never touches any of it (see AI.md). Every number is a tunable default with its source; where a value is precautionary rather than measured, it says so.

## 1. Weight
- Smoothed weight is an exponentially weighted moving average of daily weigh-ins with alpha = 0.10 on the new reading (Hacker's Diet; roughly a 20-day average). First reading seeds the average. Missing days are skipped, not interpolated.
- Display: smoothed weight large; the raw reading in the explanatory line. Never draw a straight line from the smoothed weight to any destination.
- Change chips compare the smoothed value with its value 30 days ago, phrased as "1.2 lb lower than a month ago." No change is "about the same."
- Soft wall: when the smoothed value is 2 lb (0.9 kg) above its own lowest value in the past 30 days, show the note once ("your weight line is a couple of pounds above where it was") and not again for 14 days. Hard wall at 5 lb (2.3 kg): the note offers the month view. Neither uses the word gain and neither changes colour.
- Rate-of-loss note: if the smoothed value has fallen faster than 1.5 kg (3.3 lb) per week averaged over four weeks, show once: "You've been losing a little faster than typical guidance. About 2.4 lb a week over the last four. Most guidance suggests 1 to 2. Faster loss is linked to gallstones and more muscle loss. Worth mentioning next time you see a clinician." Reappears only if the pace changes and returns.
- Expected-curve band on the History weight screen: a shape drawn from y(t) = a + b(1 minus e^(minus 3.2 t)) with a shallow rise after t = 0.75, where t is fraction of a year since the first weigh-in; band width plus or minus 20 px. Purpose is narrative (the six-month flat stretch is normal), not prediction. Label: "The sand shape is where most people's lines fall over a year. Yours is the dark one."
- Destination (optional, off by default): a user-entered weight drawn as a dashed line on the chart. No countdown, no percent, no notification on crossing. "At this pace" appears only on tap and says "Trends rarely hold, and that's fine."
- Numbers-off mode: all figures become direction words; charts keep shape and lose axes; the destination is hidden.
- Health Connect (optional, off): read weight, steps, sleep. Never write. Never required.

## 2. Body
- BMI = kg / m^2 from stored height and the smoothed weight. Shown as a number. The category name appears only on the "why this" page, never on the body screen.
- Waist-to-height ratio = waist / height in the same unit. Bands per NICE NG246: under 0.5 healthy, 0.5 to 0.59 increased, 0.6 and over high. Shown as the range band with the guideline in one sentence: "keep your waist under half your height." Adults only. Measurement instruction on entry: midway between the lowest rib and the top of the hip bone, after a normal breath out, measured twice, averaged.
- Photos: optional, weekly cap enforced (one per rolling seven days). Alignment uses MediaPipe Pose Landmarker (shoulder and hip midpoints) to translate and scale frames. Outline export uses MediaPipe Selfie Segmentation. Photos live in app-private storage and never leave the device unless exported by the user.

## 3. The daily three
- Weigh in, say how today went, move. Each has a done state that persists for the calendar day. The order on Today is fixed.
- Say how today went: a sentence (spoken or typed), sleep in half-hour steps 0 to 10 hours, and one of three day ratings (rough, okay, good). Tags come from the closed vocabulary in AI.md and are confirmed by the person before saving.
- Move: any session of 2 minutes or more counts. Minutes and steps come from the phone's step counter and elapsed time; GPS is optional.

## 3b. Abilities
Four fixed domains: get_up, go, carry, steady. Every measure, exercise, and tracked item carries exactly one domain id. There is no aggregate score and none may be computed.

Tracked items (the person's own list). Created at onboarding from their words (AI.md job 1), each with: verbatim text, domain, rating 0 to 10, created date, and a rating history. Re-rated monthly with the check. A change of 2 or more points is reported as a change; 1 point is within noise and is shown but not announced (from the Patient-Specific Functional Scale MCID range of about 1.3 to 3.0).

Ability state. Each domain is Better, Same, or Quieter, computed from its measures over the last three checks. Better: any measure improved beyond its detectable change and none declined. Same: all measures within detectable change. Quieter: any measure declined beyond detectable change in three consecutive checks. Same is displayed as a result, in the same visual weight as Better. Quieter is displayed once per domain per six months, with the sentence in DESIGN.md and every measure that held listed beside it, and it offers the doctor page. It never changes a colour and never repeats.

The life sentence. Each domain has a small set of hand-written sentence templates keyed to a measure crossing a threshold, filled deterministically by the engine (not the model): floor rise without hands, stairs without a stop, groceries in one trip, one foot for 20 seconds. The engine picks the sentence; the model never writes one.

Ways of getting around. One of on_feet, walker, wheelchair, in_bed. Set at onboarding, changeable in settings, re-asked after any gap over 90 days. It selects which measures, exercises, and tile sentences exist. The four domains are constant across all four; only their contents change. Nothing in the interface indicates that one setting is better than another.

## 4. Movement profile (set at onboarding, re-asked every 90 days and after any gap over 30 days)
Inputs:
1. Age (optional). Drives which normative ranges apply, whether balance tests appear, and whether the readiness check fires before harder efforts. If skipped: no norms shown, most conservative test set, readiness check fires.
2. Readiness flags, original wording (not the PAR-Q+, see COMPLIANCE.md). Any yes shows "worth a word with your doctor before harder efforts" once, links to the official PAR-Q+, and never blocks the app: chest pain at rest or with activity; fainting or losing balance from dizziness in the last year; a doctor said only exercise under supervision; pregnant; a bone or joint problem that could get worse.
3. Capability: how getting out of a low chair feels (hard, okay, easy); stairs (avoid, one flight with a stop, fine); how long you can walk before wanting to stop (under 5, 5 to 15, over 15 minutes); can you get down to and up from the floor (no, with help, yes).
4. Exclusions, movement terms only, multi-select: pushing or pressing; anything that strains my stomach; getting down on the floor; jumping or impact; deep knee bending; lifting overhead; twisting my back; deep forward bending; arching my back; lying flat on my back; straining or holding my breath.
5. One pattern question: "After activity, do you feel much worse for a day or more afterward?" Yes routes to pacing mode (section 7).
6. One free-text line: "Something you'd like to be able to do." Stored verbatim and shown on Move.

The app never stores a diagnosis, never infers one, and never explains an exclusion back to the person.

## 5. Ladders
Five independent ladders. Each is a list of steps defined by duration or reps and effort, never by place. The person names their own route the first time a walking step is done ("What's a two-minute walk from your door?") and that name is reused for that step.

Walking (duration in minutes): 2, 4, 6, 8, 11, 14, 16, 20, 25, 30, then 30 at brisk pace, 35, 40, 45 brisk, then intervals (1 brisk / 2 easy x 5), (2/2 x 5), (3/2 x 5), 12 minutes jogging, 20 minutes jogging, 30 minutes jogging. Hills are a variant unlocked only after the person answers yes to "Is there a hill or slope near you?"
Chair and standing strength: seated marching 1 min; seated leg extensions 8; seated band rows 8; sit-to-stand from a high seat 5; sit-to-stand standard chair 5; 8; 10; heel raises with support 10; step-ups low 8; mini squats to a chair 8; sit-to-stand no hands 10; step-ups higher 10; then add a second set at each.
Floor work: supported kneeling transfer; half-kneel to stand with a chair; floor to stand via side-sit; unassisted floor rise; glute bridge 8; bird-dog 6 each side; then second sets.
Pushing: wall push-ups 8; 12; counter push-ups 8; 12; low incline 8; knees on floor 6; full floor 5; then progress reps.
Balance: feet together 10 s; semi-tandem 10 s; tandem 10 s; single leg 10 s; single leg with head turns; single leg eyes closed 5 s. Always "near a wall or counter" in the instruction.

Visibility by exclusion: pushing hides the pushing ladder and replaces it with seated band press in range; floor hides floor work and replaces it with standing and seated core plus chair transfers; impact hides jogging and intervals and keeps brisk walking; deep knee bending caps sit-to-stand at partial range and hides mini squats; overhead caps every lift below shoulder height; stomach strain and breath-holding hide any bracing or loaded flexion and enforce exhale-on-effort cues; lying flat hides supine floor work. Guaranteed floor when nearly everything is excluded: seated ladder plus walking snacks plus breath-controlled band work.

## 6. Progression (deterministic)
- Talk test after every session: yes easily, just about, no. Next-day check: better, same, worse (asked once, the following day, only after a session above the current step).
- Unlock: the next step is offered after two sessions at the current step rated "yes, easily" or "just about" with no "worse" the next day. Offered, never assigned. "Not yet" suppresses the offer for 14 days.
- Spike cap: an offered walking step may not exceed 110% of the longest walking session in the prior 30 days (tunable 110 to 120; precautionary default from Frandsen et al. 2025).
- Step back: "worse" the next day drops the current step by one for the next session and shows the sore rule once. "No" on the talk test twice in a row at a step drops it by one.
- Gap decay: no sessions for 8 to 28 days, one step back; 29 days or more, two steps back with a 14-day easing period during which no offers are made. Under 8 days, nothing. Shown once on the welcome-back screen; never framed as loss.
- Re-entry after 90 days or more: re-run the capability questions (section 4) before any step is suggested.
- Strength ladders progress by reps first, then a second set, then a harder variant; never load.
- Balance advances only after holding the current stance 10 s on two occasions.

## 7. Pacing mode (ME/CFS, long COVID, any post-exertional pattern)
Triggered by the pattern question or by the person reporting "much worse for a day or more" after two sessions in a month. In pacing mode: no offers, no unlock, no spike cap needed because nothing increases; the person sets a fixed envelope (minutes and days) and the app only ever suggests staying at or below it; "worse" the next day reduces the envelope by 20% and says so; Today's move card reads "Move within your limit" and the explanation says "This is not graded exercise. Staying inside your energy limit is the method." Pacing mode is exited only by the person, from settings, and the app re-asks the pattern question when they do. Source: NICE NG206 (2021); CDC.

## 7b. The monthly check
Three or four measures selected by the way of getting around and the tracked items, plus the person's own list re-rated. Ten minutes. Chair and wall only. Never more than one measure per domain per check.

Phone-timed chair stands: phone in a pocket or held to the chest, accelerometer peak detection, 30 seconds, count of full stands. Validated at over 94% timing accuracy against manual counts in the literature; verify on device against a manual count before shipping, and fall back to a manual tap counter if it fails.

Camera-counted reps (wall push-ups, band rows): MediaPipe Pose Landmarker, on device, full reps only by joint-angle threshold, nothing recorded, no frames written to storage. If detection accuracy in real conditions falls below 80%, this becomes optional and the manual counter leads.

Results are reported life first (the sentence), then the measures, then Same where it applies.

## 8. Tests
Offered by profile; monthly by default; each shows the person's own history first and the typical range for their age band only on request, labelled with the population it was validated in. Never a grade. Rules and cutoffs, encoded as data:
- Chair stand, 30 seconds (Rikli and Jones): below-average lower bounds by age and sex as listed in research/03. Ages under 60: no norm shown; note that healthy adults under 35 average about 33.
- Five times sit-to-stand: over 15 s shows "worth mentioning to a clinician" once (Buatois 2008). Means by decade in research/03.
- Timed up and go: 12 s or more shows the same note (CDC STEADI). Needs a 3 m clear space; instruction says so.
- Four-stage balance (CDC STEADI): advance only on a 10 s hold; failing tandem routes to the balance ladder.
- Single-leg stance: under 10 s eyes open shows the note. Per-cell norms must be verified against Springer 2007 before shipping; ship without per-cell norms if unverified.
- Two-minute step: count of right-knee raises; percentile examples in research/03; floor-free cardio proxy.
- Six-minute walk: GPS on a flat route, with the caveat; Enright and Sherrill equations for predicted distance; hidden for anyone with a readiness flag.
- Gait speed: middle 6 m of a 10 m walk timed by the person; under 0.8 m/s shows the note.
- Push-ups, plank, wall sit: recreational and active profiles only; hidden under pushing, floor, or deep-knee exclusions respectively.
- Cooper 12-minute: recreational profile only, no readiness flags; VO2max = (metres minus 504.9) / 44.73; category bands not shown until verified against a Cooper Institute source.
Safety strings that ship with every test: sturdy chair or wall within reach; stop if anything hurts; skip if you have had a fracture or surgery in the last three months.

## 9. Profiles (the configuration the inputs produce)
Derived, not chosen by the person, and editable in settings by changing the inputs. Twelve configurations are described in research/03 Part 6; the engine implements them as: which ladders are visible, which step each starts on, which tests are offered, whether the readiness note has fired, and whether pacing mode is on. Two special cases: "already active" (over 15 minutes walking easily, stairs fine, no exclusions, age under 60, or a self-report of regular exercise) hides the walking ladder below step 10 and turns Move into a tracker with the Cooper and push-up tests available; "on a weight-loss medication" (a settings switch, never asked about the medication itself) adds a protein reminder ("a protein food at most meals") and makes the strength ladders the suggested next thing twice a week.

## 9b. Try it and see
Runs only after six weeks of check-ins and only when the engine finds a candidate: a tag or a time-of-day that co-occurs with a measure or a tracked-item rating in at least 5 of 7 weeks in one direction. The engine finds it; the model words it (AI.md job 4).

Protocol: two weeks of condition A, two weeks of condition B, one variable, chosen by the person from the offer. The measure is stated before it starts. The result is reported with both numbers side by side and one honest sentence, including "no difference" as a normal outcome. The person may stop at any time and nothing is recorded as a failure. Off by default in settings for anyone in pacing mode, and never offered to them.

Never offered on: anything involving eating, restriction, medication, or sleep duration as the variable. Permitted variables: time of day, order of exercises, which exercise, walk before or after, indoor or outdoor, with or without a rest between sets.

## 10. The Sunday write-up
Deterministic inputs collected for the model: days moved, minutes, tags with counts, day ratings, sleep average, the step name, whether a step was offered, the talk-test results, and the weight direction as one of "a little lower," "about the same," "a little higher." The number is never passed. The model's output is filtered: any sentence that pairs a restriction tag (ate light, skipped a meal, fasting) with a weight direction is dropped and the paragraph regenerated once, then shipped without that sentence.

## 11. Patterns
Computed after six weeks of check-ins. For each tag, compare weeks where the tag appeared on three or more days against weeks where it did not, on: weight direction, average sleep, days moved. Report a pattern only when the split is at least 5 of 7 weeks in one direction. Phrase through the model (AI.md job 4). Restriction tags are excluded from this computation entirely.

## 12. Reminders
Off by default. Per-type switches: walk reminder (at the anchor moment), Sunday photo, Sunday write-up ready, a longer walk is ready. Hard ceiling of two notifications in any rolling seven days regardless of switches; the settings screen shows the count used. Copy under ten words. No reminder ever references a missed day.

## 13. Doctor summary
One page, function first: the four abilities and what changed in each; the person's own list with then and now ratings; every measure's first and latest result; then the levers. Date range; smoothed weight then and now; waist-to-height then and now; days and minutes moved in the last four weeks; current walking step and duration; each test's first and latest result; blood pressure entries if any; the line "Not tracked here: medication." Exported as PDF to the share sheet. Nothing is transmitted.

## 14. Data
Single encrypted SQLite database (SQLCipher, key in Android Keystore). Export: a folder containing a CSV of weigh-ins, a CSV of check-ins with tags, a CSV of sessions and tests, the photos, and the doctor summary PDF. Delete: everything, immediately, with one confirmation, and the copy says there is no other copy. Import restores from the export folder.
