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

Tracked items (the person's own list). Created at onboarding from their words (AI.md job 1), each with: verbatim text, domain, rating 0 to 10, created date, and a rating history. Re-rated monthly with the check, alongside a second optional rating from ADDENDUM-03 Part 18: "How sure do you feel about it?", zero to ten. Both lines go on the same chart in Progress, and together they produce the most useful sentence the app can say: "The chair stands got easier and you said you feel surer about the stairs too." Or, just as honestly: "The chair stands got easier but you don't feel any surer yet. That often follows later." Deterministic, never scored, never clinical. A change of 2 or more points is reported as a change; 1 point is within noise and is shown but not announced (from the Patient-Specific Functional Scale MCID range of about 1.3 to 3.0).

Ability state. Each domain is Better, Same, or Quieter, computed from its measures over the last three checks. Better: any measure improved beyond its detectable change and none declined. Same: all measures within detectable change. Quieter: any measure declined beyond detectable change in three consecutive checks. Same is displayed as a result, in the same visual weight as Better. Quieter is displayed once per domain per six months, with the sentence in DESIGN.md and every measure that held listed beside it, and it offers the visit summary. It never changes a colour and never repeats.

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
- Every session ends with one question, "How did that feel?", answered easy, about
  right, or hard. That answer is what progression turns on.
- Rated easy twice in a row at a movement: one more rep or one more minute next time.
- Rated hard: targets drop about ten percent next time.
- A body tag within a day of a session: the likely movement is swapped for its
  alternative.
- Four or more days without a session: the next one is shorter.
- **Ceilings.** Reps are not the progression forever. Each movement carries a rep
  ceiling and a next variant, and at the ceiling the engine progresses by variant
  rather than by number.
- **Spike cap** on walking, unchanged: an offered walk may not exceed 110% of the
  longest walk in the prior 30 days, read as in DECISIONS.md rather than as literal
  arithmetic against the next rung.
- Every adaptation is deterministic, and every one of them is shown as a sentence on
  the offer card at the moment it happens. The model never chooses or writes one.
- **Interruptions** replace the old gap decay entirely. See section 15b.

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

## 12. Reminders and the widget
Replaced by ADDENDUM-03 Part 13, merged on commit 9837f2a.

**One daily prompt**, on by default, at the anchor time, under ten words, never
mentioning a day without a session, never guilt, never a count of anything. It
rotates: "Ready when you are." / "Today's is a short one." / "Chair stands and a walk,
about five minutes."

Everything else is capped at two a week combined and is off by default.

**The app stops asking.** Dismissed without opening four days running, the daily
prompt stops for a week and returns once with "Still here whenever you want it." Four
more, and it turns itself off and says so in the settings.

**Widget:** two sizes, today's session name, its length, and a tap that starts it.

## 13. The measures table (part of the visit summary export)
One page, function first: the four abilities and what changed in each; the person's own list with then and now ratings; every measure's first and latest result; then the levers. Date range; smoothed weight then and now; waist-to-height then and now; days and minutes moved in the last four weeks; current walking step and duration; each test's first and latest result; blood pressure entries if any; the line "Not tracked here: medication." Exported as PDF to the share sheet. Nothing is transmitted. From Phase 5 this table is the second half of the visit summary export; the written summary sits above it.

## 13b. The visit summary: what the engine assembles

The engine does all selection, all arithmetic, and all thresholding. It produces a structured brief. The model receives only that brief.

**Window.** Default is the last six months, or since the first weigh-in if shorter. The person can choose three months or all time. Minimum to generate: 8 weeks of data and 2 completed monthly checks.

**What goes in the brief, and how each item is chosen.**

*Ability changes.* For each of the four domains, the change between the first and last check inside the window, in the domain's own units, labelled Better, Same, or Quieter by the rules in section 3b. All four are always included, including Same.

*Measures.* For each measure taken at least twice in the window: first value with its date, last value with its date, number of times taken, and whether the difference exceeds that measure's detectable change. Never a percentage, never a projection.

*The person's own list.* Each tracked item with its verbatim text, its first rating and date, its last rating and date, and whether the change is at least 2 points.

*Mentions.* For each tag in the closed vocabulary, the count of days it appeared in the window, and the three most recent verbatim sentences containing it. A tag is only passed if it appeared on 5 or more days. Body tags (sore, pain, unwell) are always passed if they appeared at all, with their count and dates.

*Co-occurrence.* Any tag-and-measure or tag-and-tag pair the engine already found for Try it and see (section 9b rules), with its split. Nothing new is computed here; if the engine has not found a pattern, none is passed.

*Sessions.* Days moved and total minutes per month across the window, the current walk or set and its duration, and any gap of 14 days or more with its dates.

*Levers.* Weight direction as one of "a little lower," "about the same," "a little higher," plus the first and last smoothed values and the number of weigh-ins. Any blood pressure entries with their dates. Sleep average per month.

*Way of getting around*, and whether it changed during the window.

*Exclusions and pacing mode*, as flags only, never with any reason attached.

**What is never in the brief:** the raw journal text beyond the three most recent sentences per passed tag; anything about food beyond the food tags; any exclusion reason; any age; any diagnosis, because none is stored.

**Question candidates.** The engine, not the model, decides what is worth asking about. A candidate is generated only by one of these rules, and each carries the evidence that triggered it:

1. A body tag (sore, pain, unwell) appearing on 5 or more days in the window, or on 3 or more days that fall within 48 hours of a session.
2. A measure that declined beyond its detectable change across three consecutive checks (the Quieter rule).
3. A tracked item whose rating fell by 2 or more points.
4. A blood pressure entry outside the range the person's other entries sit in, stated only as "worth checking again," never interpreted.
5. Weight falling faster than 1.5 kg per week averaged over four weeks (the existing rate-of-loss rule).
6. A gap of 30 days or more in sessions where the person also logged a body tag in the same period.
7. A measure that improved beyond its detectable change while a related tracked item did not, which is worth raising because the person's experience and the number disagree.

At most four candidates go to the model, ordered by rule number. If there are none, the summary has no question list and says so in one line. The model may reword a candidate but may not add one, and any question in its output that does not map to a candidate is dropped by the validator.

**Refresh.** Generated on demand, not on a schedule. The last generated summary is stored with its window and generation date. Regenerating replaces it. Nothing about it is ever notified.

## 14. Data
Single encrypted SQLite database (SQLCipher, key in Android Keystore). Export: a folder containing a CSV of weigh-ins, a CSV of check-ins with tags, a CSV of sessions and tests, the photos, and the visit summary PDF. Delete: everything, immediately, with one confirmation, and the copy says there is no other copy. Import restores from the export folder.

## 15. The session
From ADDENDUM-03 Part 1, merged on commit 9837f2a. The session is the centre of the
app and this is the engine behind it.

**Shape.** Four to eight minutes. A warm up of 60 to 90 seconds on any session over
three minutes. A cool down of 60 seconds plus two thirty-second stretches on any
session over five minutes, skippable, and skipping is never remarked on.

**Screens.** S1 the offer, on Today. S2 ready. S3 count in, three seconds. S4 the set,
live. S5 rest. S6 between movements. S7 done.

**What the engine decides**, all of it deterministic:
- Which movements, from the person's way of getting around, their exclusions, their
  equipment, any suppressed body area, and the therapist's plan if one exists.
- The target for each, from the last result and the last rating.
- Whether today is an easy day: after two consecutive strength sessions, or after any
  session rated hard. An easy day is two to three minutes of mobility, breathing or a
  short walk, offered as "Today's an easy one" with the reason. Never a blank screen,
  and never the words "rest day", which read as permission to skip two.
- The adaptation sentence, shown on the offer card at the moment it applies.

**Counting.** Reps come from the camera or the motion sensor where the movement
supports it and from the person tapping where it does not. Every counted number is
editable at the end of the set with plus and minus, and the app says nothing about
being corrected.

**Rest.** A shrinking arc rather than a ticking number, with the next movement named
underneath and the set just finished shown with its number. "Skip the rest" is always
available. Rest is the only timed thing in the app and it is skippable and extendable,
which is what keeps it inside the accessibility rule about time limits.

**The done screen.** What was done, each number against its last. One question, "How
did that feel?", answered easy, about right or hard. Then the next session, already
adjusted by that answer, on the same screen.

**Audio.** Android TextToSpeech, on device, no network. It speaks the setup line, the
count in, every rep as counted, two mid-set lines, the end of the set, the rest with
the next movement named, the final five seconds, and the end of the session. On by
default. Rate below the system default, three steps. It ducks other audio rather than
stopping it, never speaks outside a session, and never speaks a number the engine did
not produce. Without TTS the session runs silently with identical screen text.

**Pacing.** The audio keeps time: "up... and down... up... and down", slower and faster
available. On for the first three sessions of any new movement, then off unless kept.
This is the difference between counting what happened and leading it.

**Pause.** Always visible. Holds everything indefinitely, dims the screen, and resumes
exactly where it stopped. A session paused more than an hour is saved as far as it got
and the app says so plainly when reopened.

## 15b. The bad day, and interruptions
From ADDENDUM-03 Parts 2 and 15.

**Three exits**, at every point in a session, equal buttons, never buried.
- "Make it easier" drops to the easier variant mid set and continues: "Switched to the
  easier one. That still counts."
- "Skip this one" moves to the next movement.
- "That's enough for today" ends the session and saves everything done: "Done. That
  counts." Never "cancelled", never "incomplete", and no confirmation dialog.

**Pain.** A "Something hurts" button in every session. One press stops the session
immediately with no confirmation and asks only which area, from a short list. That
area's movements are suppressed for seven days, the next offer says so, and after
seven days the app asks once whether to bring them back. The same area twice in a
month produces one line, once: "Worth a word with your doctor about that shoulder."
No interpretation, no advice, no repetition.

**Tiredness.** "Not today, but something small" on the offer gives a ninety second
version, and it counts as a session.

**Interruptions**, replacing the old gap decay. After any gap of seven days or more,
one question with four answers and no free text:
- "Life got busy": one step smaller for two sessions.
- "I was unwell": two steps smaller, a two week ramp, and "We'll take it slowly for a
  couple of weeks. That's what bodies need after being unwell." Plus, once: "If you
  were in hospital or had a fall, it's worth a word with your doctor before the harder
  movements."
- "I was away": nothing beyond the standard one step.
- "I'd rather not say": treated as busy, and not asked again for that gap.

Gaps of sixty days or more re-run O2 and O3 with the previous answers filled in and
start three steps back with the ramp. The word "missed" never appears.

**Context.** Asked at O5 and editable in You: a sturdy chair without arms, a clear
wall, equipment (bands, weights, a step, none), and somewhere to prop the phone. The
answers filter the library, and nothing needing absent equipment is ever suggested.

**Chair height.** The chair stand is meaningless if the chair changes, so the app asks
once for a rough height, stores it, and shows it in the setup line every time. If it
changes, the app notes once that the numbers are not directly comparable, without
alarm.

## 16. The week
From ADDENDUM-03 Part 10. A week target set once and changeable: three, four or five
sessions. On Today: "Two this week. One more makes three." At target: "That's three.
Anything more is extra."

Consistency is four bars in Progress, each week against its target. Every week stands
alone. A quiet week does not erase the three beside it, and nothing is broken, lost or
reset: "Three good weeks and one quiet one. That's what most months look like."

The Sunday review is one screen reached from Today and never notified: what you did,
what moved, one thing noticed, the week ahead with something concrete, and the card.
It is the only place in the app that looks forward.

## 17. Documents and the therapist's plan
From ADDENDUM-03 Parts 5 and 6.

**One button**, "Scan something", in Sessions and in You. The camera opens, the person
photographs the page, and text is extracted on device with ML Kit. The app then asks
one question about what it is looking at: exercises, a report, both, unclear, or out
of scope. The photo is always saved, labelled by date and by who it came from, and is
viewable forever beside anything produced from it. "Just keep it" is always available
and is a legitimate outcome. A document can be several pages, photographed in
sequence, handled as one.

**Four ways into a plan:** photograph it, say it out loud, pick from the library, or
type it. All four end at the same confirmation screen, where every item is shown with
what the app matched it to, and nothing is saved unconfirmed.

**How the two plans coexist.** When a therapist's plan exists, it **is** the session.
Today's card shows it, labelled "From your physio", and that is what Start runs. The
app's own suggestions appear below as "Also, if you want more", clearly separate,
never merged, never counted as part of the plan, and turn-off-able in one tap. A
movement in both is done once and counts for both, and the app says so once.

**What the app does with a plan.** Runs it exactly as given. Does not add to it,
remove from it, or progress it. Reps and frequency change only when the person changes
them or marks that their therapist did. One line, once: "This is your therapist's, not
ours. We'll keep track of it and leave it as they set it." A plan movement that
conflicts with something the person avoids is flagged rather than silently dropped.
What was done and what was not is reported only in Progress and in the export, never
as a nag and never as a score.

**The export back.** A one page PDF for the next appointment, function first: what was
prescribed, what was done and when, the numbers, what hurt and when, and the person's
own ratings.

**Review date.** The person can set when they next see their therapist. Two days
before, one prompt: "You see your physio on Thursday. Your page is ready."

Multiple plans coexist, each labelled, each separate.

## 18. Reading reports and letters
From ADDENDUM-03 Part 7. The engine's part is the extraction and the validator; the
model's part is in AI.md job 9; the boundary that keeps this outside Clinical Use is
in COMPLIANCE.md.

The reader produces exactly four things and nothing else: what kind of document it is,
what it says in plain words, the words from the document explained, and things you
might ask. Anything in the document that is a home programme is pulled into a plan
through the confirmation flow in section 17, as a second step and never as one.

Documents live in the encrypted store. The original photo is always kept and always
viewable beside the reading. Camera frames used for extraction are never written to
disk. Both are included in export and both are deleted by delete. Nothing is
transmitted, and the scanning screen says so at that moment rather than in settings.
