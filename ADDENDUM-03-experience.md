# ADDENDUM 03: the experience overhaul

The final structural overhaul. It supersedes Addendum 02's phase plan and every
earlier onboarding, navigation, and session specification.

Where this conflicts with an earlier document on behaviour, flow, timing,
notification, warmth, help, language, models, or what a session contains, this wins.
Design tokens, the compliance position, and the model output contracts stand unless
a part below says otherwise.

HOW TO RUN THIS. Build the entire app, every phase, without stopping for approval.
This is an unattended run and I will test the finished thing. Decide every judgment
call yourself, prefer the simpler and more reversible option, log each one in
DECISIONS.md, and continue. If something genuinely requires me, record it under
BLOCKED with exactly what I need to do, skip it, and keep building everything that
does not depend on it. Do not end a turn while work remains. Verify library,
framework, and model versions and the current recommended on-device paths at build
time rather than trusting anything named here. Commit and push tested increments
continuously and keep HANDOFF.md current to within one increment. Run the Phase 1
acceptance gate at the end of Part 21 before continuing past Phase 1b, and every
later phase gate as written.

===========================================================================
WHO THIS IS FOR, AND THE LANGUAGE OF DIRECTION
Replaces MASTER_SPEC.md section 3 and the framing rules in DESIGN.md section 6.
===========================================================================
Adults from around 50 onward, wherever mobility starts to slip. The app never names
an age or a stage. "Senior", "elderly", "frail", "decline", and "fall risk" stay off
the interface.

The app has been written as though everyone is holding onto what they have. That is
wrong for many of the people who will use it. Three directions exist and the app
speaks to all three without asking anyone to declare which they are.
- Rebuilding, after an illness, an operation, or a long slow drift.
- Keeping what they have, deliberately, which is real work.
- Building, wanting to be stronger than they have been in twenty years.

DIRECTION IS INFERRED, NEVER ASKED. It comes from what the person said they wanted
and from what their numbers do over the first month. It changes only the register of
copy templates, never the logic. Every template has three registers and the engine
picks one:
Rebuilding: "You're doing more than you were a month ago."
Keeping: "Same as last month, which takes doing."
Building: "Fourteen. That's four more than when you started."

Banned in all registers: anything implying the ceiling is holding steady, anything
implying improvement is unlikely, anything congratulating someone for not getting
worse, anything treating maintenance as second best. "Steady" here means reliable,
not stuck.

The positioning line stays "What can you do?" because it works for all three.

===========================================================================
PART 1: THE SESSION. The centre of the app.
MASTER_SPEC.md 6.11, LOGIC.md section 15, DESIGN.md section 5b.
===========================================================================
A session is four to eight minutes in which the app leads. It is not a timer. A
person should be able to put the phone on a table, do what it says, and never decide
anything mid-session.

THE FLOW
S1 THE OFFER, on Today. The session's name, its length, the movements in it, one
line saying what it feeds, and any adaptation sentence. Two actions: "Start" and
"Change it".
S2 READY. Full screen. The movement's name, its setup line, the target, the stop
rule. One button: "I'm ready".
S3 COUNT IN. Three, two, one, go, one second each, the number filling the screen.
S4 THE SET, LIVE. The dominant screen of the app. An enormous count that climbs as
reps are detected. Under it the target and the last result. A progress ring filling
toward the target. Pause, and the three exits (Part 2), always visible. For untimed
movements (walks, holds, bed work) the same screen shows elapsed time and, for a
walk, steps.
S5 REST. A shrinking arc, not a ticking number, with the next movement named
underneath and the set just finished shown with its number. "Skip the rest" always
available.
S6 BETWEEN MOVEMENTS. Same shape as S2, count-in skippable.
S7 DONE. What was done, each number against its last. One question: "How did that
feel?" easy / about right / hard. Then the next session appears on the same screen,
already adjusted by that answer: "Next time: eleven chair stands." Two actions:
"Save" and "Change next time".

AUDIO. You cannot read a phone while standing up from a chair. Android
TextToSpeech, on device, no network. The app speaks the setup line, the count in,
every rep as counted, two mid-set lines ("five more", "that's more than last time"),
the end of the set, the rest with the next movement named, the final five seconds,
and the end of the session. On by default, speaker toggle in the session top bar and
in You. Rate below system default, three steps. Ducks other audio rather than
stopping it. Never speaks outside a session. Never speaks a number the engine did not
produce. Without TTS the session runs silently with identical screen text.

PACING. The audio keeps time: "up... and down... up... and down", with slower and
faster options. On for the first three sessions of any new movement, then off unless
kept. This is the difference between counting what happened and leading it.

PAUSE. A pause button in every session, always visible. Pausing holds everything
indefinitely, dims the screen, and resumes exactly where it stopped. A session paused
more than an hour is saved as far as it got and the app says so plainly when
reopened. Someone will need to answer the door.

WARM UP. Every session over three minutes opens with 60 to 90 seconds of movement
prep. Part of the session, explained once: "A minute to get moving first. It makes
the rest easier."
COOL DOWN. Sessions over five minutes end with 60 seconds of easy movement and two
thirty second stretches. Skippable, and skipping is not remarked on.

EASY DAYS. After two consecutive strength sessions, or after any session rated hard,
the next offer is easy: two to three minutes of mobility, breathing, or a short walk,
as "Today's an easy one" with the reason. Never a blank screen and never the words
"rest day", which reads as permission to skip two.

CEILINGS. Reps are not the progression forever. Each movement carries a rep ceiling
and its next variant. At the ceiling the engine progresses by variant: "Twenty is
plenty. The next one is the same movement, slower, which is harder than it sounds."

ADAPTATION, ALWAYS VISIBLE. Every adaptation is deterministic and shown as a sentence
on the offer card at the moment it happens. Rated hard last time: targets drop about
ten percent, "A bit lighter today, since the last one felt hard." Body tag within a
day of a session: the likely movement is swapped, "Swapped the wall push ups for band
rows, since you mentioned your shoulder." Four or more days missed: shorter, "Shorter
one to get going again." Rated easy twice: one more rep or minute, "One more than
last time. You said the last two felt easy." The model never chooses or writes these.

===========================================================================
PART 2: THE BAD DAY. LOGIC.md 15, DESIGN.md 6.
===========================================================================
Three exits, at every point in a session, equal buttons, never buried:
"Make it easier", which drops to the easier variant mid set and continues. "Switched
to the easier one. That still counts."
"Skip this one", which moves to the next movement and continues the session.
"That's enough for today", which ends, saves everything done, "Done. That counts."
Never "cancelled", never "incomplete", no confirmation dialog.

PAIN. A button in every session: "Something hurts". One press stops the session
immediately, no confirmation, and asks only which area from a short list. That area's
movements are suppressed seven days, the next offer says so ("Left the shoulder
movements out this week"), and after seven days it asks once whether to bring them
back. The same area twice in a month produces one line, once: "Worth a word with your
doctor about that shoulder." No interpretation, no advice, no repetition.

TIREDNESS. "Not today, but something small" on the offer gives a ninety second
version. It counts as a session.

===========================================================================
PART 3: ONBOARDING, REBUILT. Replaces ONBOARDING.md in full.
===========================================================================
The rule: the person does something real before answering anything optional, and
every question is asked at the moment it matters.

O1 THE HOOK. Full bleed sun and hills hero. Large: "What can you do?" Under it: "This
app keeps track of what your body can do, and helps you do more of it. Everything
stays on your phone." Language pills. One button: "Show me". No sign up, no email, no
account, no permission requests, nothing to read.

O2 ONE QUESTION, FOUR CARDS. "Most days, how do you get around?" On my feet / With a
walker or cane / In a wheelchair / Mostly in bed or a chair, for now. Under it:
"There's a version of this app for each. Change it any time." Cards at least 64dp
tall, one tap.

O3 THEIR WORDS. "What would you like to be able to do?" A large microphone button and
a text field, equal size, side by side. Six starter chips, not all maintenance: get
off the floor / stairs without stopping / carry the shopping in one trip / keep up
with the grandkids / walk further than I do now / get stronger than I am. Their
sentence appears exactly as said, then "I'll keep track of this" with the tracked
item, editable. One item is enough. Skipping picks "move more easily" and the app
asks again on day four.

O4 THE FIRST SESSION, not a question. "Here's something you can do right now. It
takes about two minutes." The movement, its setup, one button: "Start". Under sixty
seconds from launch.

O5 AFTER THE SESSION, three things, one tap each, visible Skip on all:
"How did that feel?" easy / about right / hard.
"Anything you avoid, or have been told not to do?" movement chips plus "nothing".
"Do you have a sturdy chair without arms?" yes / only with arms / no.
Then Today.

ASKED LATER, IN PLACE, each with one line saying why. Day 3: the anchor time. Day 4:
a second tracked item. Week 1, before any session over five minutes: readiness flags.
Week 2, before the first measure: rough height, skippable. Week 3: the photo offer.
Week 4: the confidence rating. Age is never asked in onboarding, only if the person
taps "show typical ranges".

RESUMABLE. Quitting mid-onboarding resumes exactly where it stopped.

===========================================================================
PART 4: HELP, BUILT IN RATHER THAN BOLTED ON.
DESIGN.md section 4b, MASTER_SPEC.md 6.17.
===========================================================================
The goal is that help is rarely needed. A permanent "I'm lost" button is an admission
of failure, so it does not exist. Three layers instead.

L1 THE SCREEN EXPLAINS ITSELF ONCE. Every screen, first open, one sand block at the
top with one sentence. Never a modal, never a tour, never a dark overlay. Small
dismiss, never returns. Today: "This is today. One session, and what the app has
noticed." Sessions: "Everything you can do, and everything you've done." Progress:
"What's changed since you started." You: "Your list, your settings, and how the app
works." Library: "Sixty or so movements. Tap any to read it, or start it now."

L2 THE HELP DOT. A "?" in the top right of every screen, same place, same size,
always. It opens a sheet: what this screen is for in two sentences, what each thing
on it means, and two or three likely questions with answers. Hand written per screen,
never generated. Dismissed by tapping anywhere.

L3 EVERY NUMBER EXPLAINS ITSELF. Tap the info dot on any figure: "Chair stands: how
many times you stood up from a chair in thirty seconds. Yours have gone from nine to
fourteen since March." No number in this app is ever unexplained.

FIRST RUN OF ANY FEATURE gets one sand block explaining it in a sentence. Once.

Today's session card is never ambiguous: it always says what to do, how long it
takes, and has one obvious button. If a person could be lost on Today, that is a
design bug to fix, not a help topic to write.

===========================================================================
PART 5: THE CAMERA. One entry for every piece of paper.
MASTER_SPEC.md 6.19, LOGIC.md section 17.
===========================================================================
There are two document features (a therapist's programme, and reports and letters)
and they must not be two buttons. A person standing in a hallway holding a sheet of
paper should not have to classify it first.

ONE BUTTON, in Sessions and in You: "Scan something". The camera opens. The person
photographs the page. Text is extracted on device with ML Kit. The app identifies
what it is looking at and asks one question:
If it looks like a list of exercises: "This looks like exercises someone gave you.
Add them?" leading to Part 6.
If it looks like a report or a letter: "This looks like a report from a therapist.
Explain it in plain words?" leading to Part 7.
If both: both offers, in that order.
If unclear: "I'm not sure what this is. What would you like to do?" with both options
and "Just keep it".
If out of scope (a lab report, imaging, a medication list): "This isn't something the
app reads. It's saved here if you want to keep it."

In every case the photo is saved, labelled by date and by who it came from, and is
viewable forever beside anything the app produced from it. "Just keep it" is always
available and is a legitimate outcome. A document can be several pages, photographed
in sequence, handled as one document.

===========================================================================
PART 6: THE THERAPIST'S PLAN. The strongest reason to recommend this app.
MASTER_SPEC.md 6.19, LOGIC.md section 17, COMPLIANCE.md.
===========================================================================
Most people handed a home programme never do it. The app should be where that
programme lives.

FOUR WAYS IN: photograph the sheet (Part 5); say it out loud ("My physio wants me
doing ten sit to stands twice a day and heel raises"); pick from the library; type it.
All four end in the same confirmation screen where every item is shown with what the
app matched it to, and nothing is saved unconfirmed.

HOW THE TWO PLANS COEXIST, and this must be unambiguous:
When a therapist's plan exists, IT IS the session. Today's card shows their plan,
labelled "From your physio", and it is what "Start" runs. The app's own suggestions
appear below it as "Also, if you want more", clearly separate, never merged, never
counted as part of the plan.
If a movement appears in both, it is done once and counts for both, and the app says
so once: "This one's on your physio's list too."
The person can turn the app's extras off entirely, in one tap, and many will.

WHAT THE APP DOES WITH THE PLAN:
- Runs it exactly as given. Does not add, remove, or progress it. Reps and frequency
  change only when the person changes them or marks that their therapist did.
- One line, once: "This is your therapist's, not ours. We'll keep track of it and
  leave it as they set it."
- If a plan movement conflicts with something the person said they avoid, it flags it
  rather than silently dropping it: "Your plan has wall push ups, and you said you
  avoid pushing. Ask your therapist about it. We'll leave it in for now."
- Tracks what was done and what was not, reported only in Progress and in the
  therapist export. Never nags about a therapist's plan, never scores it.

THE EXPORT BACK. A one page PDF for the next appointment, function first: what was
prescribed, what was done and when, the numbers, what hurt and when, and the person's
own ratings. This is the artifact that makes a therapist recommend the app to their
next patient. It should be excellent.

REVIEW DATE. The person can set when they next see their therapist. Two days before,
one prompt: "You see your physio on Thursday. Your page is ready."

MULTIPLE PLANS. A physio plan and an OT plan can coexist, each labelled, each
separate.

Compliance: the app is a record keeper here, not a clinician. It never interprets a
plan, never modifies one, never advises for or against one, and never suggests the
person is doing it wrong.

===========================================================================
PART 7: READING REPORTS AND LETTERS. MedGemma.
MASTER_SPEC.md 6.21, AI.md, LOGIC.md section 18, COMPLIANCE.md, DECISIONS.md.
This supersedes the earlier decision that MedGemma is not used. Record the reversal.
===========================================================================
People in therapy receive documents they cannot read: progress reports written for a
referring physician, discharge summaries, referral letters, insurer letters about
authorised visits, plans of care with goals in clinical shorthand. They are the
subject of all of it and the audience for none of it.

THE MODEL. This is the one job where a medical model earns its place, because the
vocabulary is the difficulty: gait deviations, ROM in degrees, MMT grades, transfer
independence levels, and clinic-specific abbreviations. Use MedGemma 1.5 4B from
Google's Health AI Developer Foundations as a second, separate, optional download.
Verify the current identifier, size, and recommended on-device path at build time.

TWO MODELS, ONE CHOICE. Together they are around five gigabytes, which many phones in
this audience cannot spare. In You, one screen, "What the app can read":
- Nothing extra. The app works fully. Documents are saved as photos and viewable, and
  the person types anything they want tracked. This is the default and is never
  presented as lesser.
- Your own words (Gemma 4 E4B, about 2.5 GB). Turns what you say into what the app
  tracks, tags your notes, writes the weekly note.
- Documents from your therapist (MedGemma 1.5 4B, about 2.5 GB). Reads and explains
  reports and letters.
- Both, if the phone has room.
The screen shows each size and the phone's free space, and refuses gracefully rather
than filling the device. Either can be removed at any time and everything already
produced stays. Removing one falls back to its manual path and says so once. Neither
downloads on a metered connection without an explicit tap.

WHAT THE READER PRODUCES, and only this:
1. WHAT KIND OF DOCUMENT IT IS. "This looks like a progress report from a physical
   therapist." From the document itself, never guessed.
2. WHAT IT SAYS, IN PLAIN WORDS. Three to five short paragraphs, second person, every
   sentence traceable to text in the document.
3. WORDS FROM THE DOCUMENT, EXPLAINED. The clinical terms and abbreviations that
   appeared, each with one plain line. This is the part people will use most. "MMT
   4/5: manual muscle testing. A clinician graded that muscle four out of five, which
   usually means it moves against resistance but not full resistance."
4. THINGS YOU MIGHT ASK. Questions drawn from the document, never from inference:
   something mentioned but not explained, a goal with a date, an unclear instruction,
   a named follow up. Each phrased as a question, never a finding.

Then, separately: anything in the document that is a home programme can be pulled into
the therapist's plan through Part 6's confirmation flow. Reading and extraction are
two steps, never one.

WHAT IT NEVER DOES. This is the line that keeps the app outside HAI-DEF Clinical Use
and it is not stylistic.
- Never diagnoses, and never names a condition the document did not name.
- Never says whether the news is good or bad, never characterises progress, never
  reassures. If a report says a range of motion decreased, the app says the report
  says it, and nothing more.
- Never advises: not on exercise, treatment, medication, or whether to follow a plan.
- Never says whether to be worried, and never says not to be.
- Never contradicts, questions, or second guesses what a clinician wrote.
- Never interprets a number against a normal range or a population.
- Never handles anything outside movement, therapy, or care logistics.

Shown once on first use and printed at the top of every reading: "This explains the
words in your document. It doesn't say what they mean for you. That's a question for
whoever wrote it."

THE VALIDATOR, built before the model is wired in.
- Every proper noun, number, date, and measurement in the output must appear in the
  extracted text.
- Every explained term must appear in the extracted text.
- Every question must reference something in the document.
- Banned list fires on: diagnos, prognos, condition, disease, should, recommend,
  suggest, indicates, means that, worse, better, improving, declining, worrying,
  concerning, normal, abnormal, healthy, unhealthy, good news, bad news.
- Any sentence containing because, due to, caused by, which means, or suggests is
  rejected.
On failure: regenerate once. On second failure, drop that section. If the plain words
section fails entirely, show only the term explanations and the photo: "The app could
only pull out the words this time. Your document is saved above."

STORAGE. Documents live in the encrypted store. The original photo is always kept and
always viewable beside the reading. Camera frames used for extraction are never
written to disk. Both are included in export and both are deleted by delete. Nothing
is transmitted, and the scanning screen says so at that moment, not in settings.

LICENSING. HAI-DEF permits commercial use and redistribution with pass through and
prohibits Clinical Use, defined as any use in diagnosis or treatment. Explaining the
words in a document the person already holds, without interpreting, advising, or
diagnosing, sits outside that definition, and the constraints above are what keep it
there. The weights are a separate user-initiated download, never bundled in the
repository or the APK, which keeps them clear of AGPL-3.0. HAI-DEF terms are shown
before the download and accepted there. Made with credits Google and names the
licence. A health tech attorney reviews this boundary once before release; record it
as BLOCKED until they have, and ship the feature behind a flag that is off until it
clears.

Google Play: the health apps declaration is updated to describe document reading. The
listing describes it as explaining documents in plain words and claims no
interpretation.

===========================================================================
PART 8: BEYOND EXERCISE. MASTER_SPEC.md 6.20.
===========================================================================
Five additions, all light, all optional, none a tracker with a target.
1. THE DAY BETWEEN SESSIONS. Steps and time spent up, from the phone, one line on
   Today with no goal attached: "You were up and about more than usual today." Long
   sitting produces at most one gentle prompt a day, only if enabled.
2. THE PLACES YOU MOVE THROUGH. A one time walkthrough, six questions, repeatable:
   lighting on the stairs and the night route to the bathroom, loose rugs and cables,
   where a rail would help, what is on the floor near the bed, something to hold in
   the bathroom, indoor shoes. Each answer offers one plain fix. No products, no
   score, no risk language. Framed as "the places you move through", never as hazards
   and never as a safety assessment.
3. SLEEP AND ENERGY, LIGHTLY. One optional question a day, two taps. Its only job is
   to appear in "what the app noticed" when a pattern is real: "Your sessions have
   felt easier on the days after a good night."
4. GETTING UP FROM THE FLOOR, TAUGHT. A short plain guide in the library, offered once
   in month two: how to get up if you find yourself on the floor, in stages, and how
   to practise it safely with something to hold. Almost nobody is taught this.
   Written, not a session, and it never mentions falling.
5. WHAT TO ASK, AND WHAT TO KNOW. Expand the card library from twelve to about twenty
   five: why strength matters more than cardio after fifty; balance as a skill; what
   to expect after an operation; what a physio does and how to get one; how to talk to
   a doctor about mobility; footwear and balance; protein and muscle; what morning
   stiffness usually is. Hand written, sourced, no advice.
All optional and off by default except the passive day-between-sessions line.

===========================================================================
PART 9: MOTIVATION. MASTER_SPEC.md 6.18.
===========================================================================
1. WHAT THE APP NOTICED. On Today, one to three lines, no interaction, true before
they opened it. Never empty; if there is nothing, one true small thing ("Day nine").
2. "GOT A MINUTE?" A permanent option on Today: one movement, sixty seconds, suited to
the day. It counts. The most likely path to a habit.
3. THE LOOK BACK. A card in Progress showing the same thing then and now, rotating
weekly: first session against last, rating a month ago against today, first walk
against longest. Entirely their own history.
4. THE NEXT THING. Always on Today: where they are on the way to something they said
they wanted. "Eleven chair stands. Around fourteen is where getting off the floor
usually stops needing hands."

===========================================================================
PART 10: THE WEEK, AND CONSISTENCY WITHOUT STREAKS. LOGIC.md 16.
===========================================================================
A week target set once, changeable: three, four, or five sessions. On Today: "Two this
week. One more makes three." At target: "That's three. Anything more is extra."
CONSISTENCY: four bars in Progress, each week against its target. Every week stands
alone. A bad week does not erase the three beside it. Nothing is broken, lost, or
reset. "Three good weeks and one quiet one. That's what most months look like."
THE SUNDAY REVIEW: one screen from Today, never notified: what you did, what moved,
one thing noticed, the week ahead with something concrete ("Thursday would make
four"), and the card. The only place in the app that looks forward.

===========================================================================
PART 11: THE CARD. Sharing, properly. MASTER_SPEC.md 6.15.
===========================================================================
Exactly one other person. No feed, no comparison, no account, no server, no inbound
anything.
THE CARD: a rendered image, 1080x1350, in the app's design language: the sun and hills
hero in the app's orange, the person's sentence in Figtree 900 navy, the figure
beneath, the app name small at the bottom. It looks like the app, not a screenshot of
it.
FOUR KINDS: the week card from the Sunday review; the milestone card when a tracked
item crosses its threshold, which is the one people will actually send; the month card
with then and now; the blank card where they write their own line.
EVERY CARD IS EDITABLE BEFORE SENDING, with the suggested line already in the field.
NEVER ON A CARD: weight, any clinically named measure, any rating out of ten, anything
about a condition, anything they did not choose.
Sends via the system share sheet; the recipient needs no app; nothing about them is
stored. Offered once at the second Sunday review: "Some people send these to someone
who'd like to know. Entirely up to you." Declining hides it until Progress. The month
card also exports as PDF.

===========================================================================
PART 12: MOTION, IN THE DESIGN LANGUAGE. DESIGN.md section 5.
===========================================================================
Flat shapes, big radii, warm colour, no decoration. Motion follows the same rules.
Five movements and nothing else.
1. COUNT UP: any changing number animates old to new over 400ms with a spring.
2. FILL: any bar or arc fills over 600ms, ease out.
3. MORPH: completion uses the Material 3 Expressive shape morph, squircle to cookie,
   400ms spring. The app's one moment of delight, in the same shape language.
4. RISE: new content enters up 12dp, fades in over 250ms, staggered 40ms.
5. SUN: the hero's sun or moon drifts continuously, 40 second cycle, 3dp of travel.
   Almost imperceptible, and it is what makes the app feel alive rather than printed.
Never: confetti, sparkles, bounce, rotation, parallax, sound effects, celebration
screens, characters, or any transition over 600ms.
IN A SESSION: the count springs on each rep, the ring fills as reps accumulate, the
rest countdown is a shrinking arc, the next movement's name rises in as rest begins.
REDUCE MOTION: numbers change instantly, fills are immediate, morph becomes a cross
fade, the sun stops. Nothing is lost.

===========================================================================
PART 13: NOTIFICATIONS AND WIDGET. Replaces LOGIC.md 12.
===========================================================================
ONE daily prompt, on by default, at the anchor time, under ten words, never mentioning
a missed day, never guilt, never a streak. Rotates: "Ready when you are." "Today's is
a short one." "Chair stands and a walk, about five minutes."
Everything else capped at two a week combined, off by default.
Dismissed without opening four days running: stops a week, returns once with "Still
here whenever you want it." Four more: turns itself off and says so in Settings.
WIDGET: two sizes, today's session name, its length, tap to start.

===========================================================================
PART 14: CORRECTION, EDITING, PHONE FREE SESSIONS. LOGIC.md 15.
===========================================================================
Any counted number editable at the end of the set with plus and minus, and the app
says nothing about being corrected. Any session loggable after the fact for the last
seven days in three taps. "Do it without the phone": the app reads the session out
first, the person does it, logs what they managed, and self reported numbers are
marked as such in a footnote without implying they are worth less. Any past session
editable or deletable.

===========================================================================
PART 15: CONTEXT AND INTERRUPTIONS.
===========================================================================
CONTEXT, asked at O5 and editable in You: sturdy chair without arms, a clear wall,
equipment (bands, weights, a step, none), somewhere to prop the phone. Answers filter
the library; nothing needing absent equipment is suggested.
CHAIR HEIGHT: the chair stand is meaningless if the chair changes. Ask once for a
rough height, store it, show it in the setup line every time. If changed, note once
that the numbers are not directly comparable, without alarm.
INTERRUPTIONS, replacing the gap rule in LOGIC.md 6. After any gap of seven days or
more, one question, four answers, no free text:
"Life got busy", which takes one step smaller for two sessions.
"I was unwell", which takes two steps smaller with a two week ramp, and: "We'll take
it slowly for a couple of weeks. That's what bodies need after being unwell." Plus,
once: "If you were in hospital or had a fall, it's worth a word with your doctor
before the harder movements."
"I was away", which does nothing beyond the standard one step.
"I'd rather not say", treated as busy, and not asked again for that gap.
Gaps of 60 days or more re run O2 and O3 with answers prefilled and start three steps
back with the ramp. The word "missed" never appears.

===========================================================================
PART 16: WARMTH, IN EXACTLY FOUR PLACES. DESIGN.md 6.
===========================================================================
1. The first session: "That's the first one."
2. The first month: a card in Progress with then and now for their own item.
3. Returning after two weeks or more: "Good to see you. Everything's still here."
4. A tracked item crossing its threshold: "You said you wanted to get off the floor
   without your hands. You just did it."
Everywhere else plain. No congratulation for ordinary sessions, no emoji, no
exclamation marks, no praise. Affirmations name what was done, never evaluate it.

===========================================================================
PART 17: ACCESSIBILITY AS DESIGN. Replaces DESIGN.md 7.
===========================================================================
56dp minimum tap target in a session, 48dp elsewhere; primary action at least 64dp
tall spanning the content width. No gesture is ever the only way to do anything. Body
text floor 16sp; correct layout at 200 percent font scale, tested. Contrast 4.5:1 text
and 3:1 meaningful non text, verified. Every state carried by shape or word as well as
colour. No time limits except the skippable, extendable rest. No double tap gestures,
no two targets within 8dp, 300ms debounce on destructive actions. Haptics on every rep,
set, and rest end so a session works without sight or sound. Every session action
reachable by system voice control. TalkBack and Switch Access tested end to end.
Primary action in the bottom third.
STORAGE ACCESSIBILITY: the app must be fully usable with no model installed, and the
model screen must never imply the person is missing out.

===========================================================================
PART 18: CONFIDENCE, WEIGHT, AND THE MODEL JOBS.
===========================================================================
CONFIDENCE (LOGIC.md 3b). Each tracked item gets a second optional monthly rating:
"How sure do you feel about it?", zero to ten. Both lines on the same chart in
Progress. Produces the most useful sentence the app can say: "The chair stands got
easier and you said you feel surer about the stairs too." Or: "The chair stands got
easier but you don't feel any surer yet. That often follows later." Deterministic,
never scored, never clinical.
WEIGHT (MASTER_SPEC.md 6.1). Off by default. When on it does one job: it appears in a
sentence beside an ability that changed. "Your legs got stronger and there are eighteen
fewer pounds to carry up those stairs." Never on Today, never its own tab, never leads
anything. Smoothing and wall logic unchanged.
MODEL JOBS (AI.md). Job 1 becomes reusable: "Add something you'd like to be able to
do", any time from Progress. New Job 7: the optional end-of-session note, one line,
spoken or typed, tagged against the closed vocabulary, feeding "what the app noticed".
New Job 8: reading a therapist's sheet, output a list of items each mapped to a library
movement id or kept verbatim, with reps and frequency where stated, all confirmed by
the person; the model never invents a movement and never interprets the plan. New Job
9: reading a report or letter (Part 7), single turn, every element citing the extracted
text, no follow up and no question box. Same forbidden list, same fallbacks when a
model is absent.
FIXTURES for Jobs 8 and 9: at least thirty documents including a progress note, a plan
of care, a discharge summary, an insurance letter, a referral, a handwritten
programme, a badly photographed one, one in Spanish, one out of scope, and one blank.
Adversarial cases that must be caught: characterising progress, naming a condition
absent from the text, recommending anything, inventing a measurement, answering a
question the document raises.

===========================================================================
PART 19: DISCOVERY. MASTER_SPEC.md section 12; store-assets/clinician-handout.md.
===========================================================================
A one page printable handout, A4 and US Letter, for a clinician to hand a patient:
that it holds the home programme they were just given, what the app does in four
lines, free with no account and no ads, nothing leaves the phone, the QR code and
store link, and three ruled lines for the clinician to write what to start with. Black
and white, legible on a bad office printer. Lead with the home programme, because that
is the reason it works.
The store listing leads with what the person will be able to do. The first screenshot
is the session mid set.

===========================================================================
PART 20: THE TABS. Final. Replaces navigation in DESIGN.md 4.
===========================================================================
TODAY: what you said you want; the next thing; today's session (the therapist's plan
when one exists, the app's otherwise) with its reason and any adaptation sentence; the
app's extras below if enabled; the week line; what the app noticed; "Got a minute?".
SESSIONS: today's session and change it; the therapist's plan; "Scan something"; the
library with Try this now; history with Do this again; log a past session.
PROGRESS: what you're working toward; what changed; the four week view; the look back
card; the months; the numbers; weight if on; make a card; add something new; the
therapist export.
YOU: your list; how you get around; equipment and chair; anything to leave out; your
therapist and review date; your documents; what the app can read (models); the places
you move through; reminders; audio; the cards that answer questions; your data; about.
Every tab has the help dot. Every screen explains itself once.

===========================================================================
WHAT DOES NOT CHANGE
===========================================================================
Design tokens and the look. The voice rules and the banned list, now also banning:
test, workout, routine, level, unlock, achievement, complete, missed, streak, hazard,
risk. ("Plan" is permitted only for a therapist's plan.) Pacing mode. Exclusions in
movement terms. The compliance position: nothing diagnoses, grades, or advises, and
the app never modifies or interprets a therapist's plan or a report. The model never
states a number, never decides an ability's state, never writes a life sentence. No
calories, no scores, no targets set by the app, nothing red.

===========================================================================
PART 21: PHASES AND GATES. Replaces all earlier plans.
Build every phase. Do not stop for approval. Each gate is a check you run yourself.
===========================================================================
Phase 1a: the session engine end to end for the on-feet path, audio led with pacing,
warm up and cool down, pause, the three exits and the pain button, motion, at the full
accessibility bar. Hardcode one session so it runs on device early.
Phase 1b: onboarding (3), the help system (4), Today with the session card and one
noticed line.

PHASE 1 ACCEPTANCE GATE. Before Phase 2, run these on the physical device and record
the results in DECISIONS.md. Fix anything failing before continuing.
- Install to completed first session in under 90 seconds, timed, no skipping.
- A full session completed with the phone face down on a table, using audio only, and
  nothing important was missed.
- Every screen in the session has one obvious next action, verified by screenshot.
- Each of the three exits works from every point in the session and saves what was
  done.
- The pain button stops the session in one press and suppresses that area next time.
- Pause survives a phone call and a screen lock, and resumes in place.
- The session runs correctly at 200 percent font scale and with TalkBack.
- The next session on the done screen visibly differs after answering "hard".
- No string on any screen uses a banned word. Grep for the list and prove it.

Phase 2: Today in full including motivation (9), the week and consistency (10), the
library and history, correction and phone free sessions (14), the daily prompt and
widget (13).
Phase 3: the camera (5) and the therapist's plan (6) in all four ways in, plus the
export and review date. The model download screen and the two model choice.
GATE: the therapist plan and the app's suggestions are visibly separate on Today and
in history, verified by screenshot, and a movement in both is counted once.
Phase 4: report and letter reading (7) with MedGemma, its validator, and its fixtures,
behind a flag that is off until the attorney review clears.
GATE: all thirty fixtures pass and every adversarial case is caught. If any adversarial
case passes through, the feature stays off and it is recorded in DECISIONS.md.
Phase 5: the other ways of getting around; exclusions, readiness, pacing mode;
interruptions and context (15); ceilings and variants.
Phase 6: passive measure capture, the monthly confirm, Progress in full, confidence
and weight (18), the first month arc.
Phase 7: beyond exercise (8), the card in all four kinds (11), the Sunday review, the
remaining model jobs.
Phase 8: numbers off mode, four languages with RTL, export, import, delete, backup and
restore, hardening, the clinician handout (19), store assets, signed bundle, LAUNCH.md.
Deferred to version 2: exercise animations (VISUALS.md), the visit summary interface
(Addendum 01), Try it and see.

===========================================================================
PART 22: WHAT TO HAND ME AT THE END
===========================================================================
When every phase is done, produce a single file, TEST-ME.md, containing:
- How to install it on my device in one line.
- A numbered walkthrough of what to try, in order, starting with a fresh install and
  ending with a scanned document, no more than twenty steps.
- Every gate result from Part 21, pass or fail.
- Everything in BLOCKED, with what I need to do for each.
- Every judgment call you made that you are least sure about, listed plainly, so I know
  where to look hardest.
