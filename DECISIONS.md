# DECISIONS.md: Steady Health by Kamsiob

Seeded September 6, 2026 from the design and research process. Claude Code appends below the line. Every entry: the decision, the reasoning, and what would reverse it.

## The reframe (the largest decision in the project)
- The app is a record of what a body can do, not a weight tracker. Weight is retained as one lever and never as the score. Reasoning: gait speed alone predicts survival about as well as a panel including BMI (Studenski, JAMA 2011); grip beats systolic blood pressure (PURE); the sitting-rising test separates mortality 3.8 to 6 fold. Function is the better measure and the one people feel. Full evidence in research/04. Reversal: none contemplated.
- Four abilities: Get up, Go, Carry, Steady. No combined score, ever. A single number would rebuild the scoreboard the app exists to remove.
- Target reader of the copy: adults roughly 50 to 70 who are not frail. The word senior does not appear. Reasoning: people healthy enough to use an app daily do not identify with decline framing, and the people the framing fits are often reached through a caregiver or facility instead. Reversal: real conversations with users showing the opposite.
- Four ways of getting around (on feet, walker or cane, wheelchair, mostly in bed) each get their own version of Today, Move, and the measures. Reasoning: the underserved end is genuinely underserved and the architecture supports it at low cost. Risk acknowledged: serving a very wide range can feel aimed at nobody. Watch for it in testing.
- Same is reported as a result, in the same visual weight as Better. Strength drops 1.5 to 5% a year from midlife if nothing is done; holding a number is the work.
- Decline is reported once per domain per six months, neutrally, with everything that held beside it, and it offers the visit summary. No colour change, no repetition. Reasoning: the Communication Predicament of Aging model; documented ableism risk in capability framing (research/04 Part 6).

## Product (carried from version 1, still binding)
- Gemma 4 E4B, not MedGemma. Apache-2.0 versus restrictive HAI-DEF terms, and the app avoids medical vocabulary so the medical model adds nothing.
- No calories, no food logging, no streaks, no scores, no targets set by the app, nothing red.
- Exclusions collected in movement terms only. The app never records or infers a condition.
- Pacing mode for post-exertional patterns: non-progressing, no offers, no experiments. NICE NG206.
- The PAR-Q+ is linked, not embedded. Licence.
- Reminders off by default, hard ceiling of two a week.
- Numbers-off mode ships in v1.
- Photos optional, weekly cap. Silhouettes are the person's own segmented photos; nothing hand-drawn or generated.
- Light theme only until a dark one is designed.

## Visible AI
- Three places, never a chatbot: words become tracked abilities; the phone times and counts the monthly check; try it and see runs a two-week comparison. Reasoning: the app needed AI to be a visible differentiator, and these are the three patterns the research says are both buildable on device and genuinely novel.
- The model never writes a life sentence, never decides Better or Same or Quieter, never words the decline sentence, and never states a number. Those are engine decisions from hand-written templates.
- Try it and see never uses eating, restriction, medication, or sleep duration as a variable.

## Visuals
- Version 1 ships with no exercise animations, written instructions only. Version 2 adds them. Pipeline tested and recorded in VISUALS.md: one generated style reference per view, then image-to-video four-second loops per movement. Reasoning: no permissively licensed visual database exists that covers chair, bed, walker, or wheelchair movements; Otago's illustrations are copyrighted; generated video from a fixed reference was tested and works. Reversal: if a physical therapist review rejects the generated clips, commission Rive animation instead.

## Design
- The approved look is unchanged from version 1: Headspace's colour confidence and rounded type with illustration built from shapes, no characters. Tokens in DESIGN.md. Rejected directions listed there; do not return to them.
- Third tab is Abilities, replacing History. The weight page and the months path live inside it.
- Plum joins the palette for the Steady ability only.

## BLOCKED (owner only)
- Confirm the Rive Android runtime licence at the version chosen.
- Decide whether to request PAR-Q+ Collaboration consent (default: no, link out).
- Publish PRIVACY.md at kamsiob.com with the same effective date.
- Play Console: create the app entry, first bundle upload, content rating questionnaire.
- Provide the Play service account JSON outside the repository.
- Review the twelve cards in CONTENT.md; an eating-disorder professional for cards 4 and 7, a physical therapist for 8 through 12.
- Confirm the target device is connected with USB debugging. (Done: Pixel 8,
  Android 17, connected and authorised.)
- **Rive, or not.** Measured in Phase 0: MIT runtime, 15 MB of APK, and it brings
  an HTTP stack onto the classpath. DESIGN.md section 5 names it for three
  animations that Compose can do. Version 1 is being built without it. If you want
  it back, it also needs a paid Rive plan to author `.riv` files and a decision
  about shipping a binary blob in an AGPL repository. Say either way and it takes
  an hour to change.
- **"Stop here, that counts" against the two minute rule.** See Phase 1 below. If
  a short session should tick the daily card, say so and it is a one line change.
- **The three translations.** The app ships in English. `locales_config.xml`
  declares Spanish, Chinese and Arabic and there are no `values-es`, `values-zh`
  or `values-ar` folders behind them. The switching mechanism is built and
  tested, and the picker on the welcome screen shows only the languages that have
  real strings, so today it shows nothing and it comes back on its own the moment
  a folder lands. Nothing else has to change.

  What is needed: about 260 strings in `values/strings.xml`, `values/cards.xml`,
  `values/check.xml` and `values/summary.xml`, translated by somebody who speaks
  the language, and the thirteen long-form cards in CONTENT.md among them. This
  is not a job for machine translation: every string has to pass the friend test
  in its own language and avoid that language's equivalents of the banned list,
  and the audience includes people who are frightened of what an app might say
  about them. Arabic also needs somebody to look at the app in RTL on a phone.

  Two things that are already done and do not need redoing: the language split is
  disabled in the bundle so every language ships inside the app and works
  offline, and per-app language switching is wired through the platform's own
  setting on Android 13 and up.

- **Confirm the Play target API requirement** before release. The build uses
  compileSdk 37 and targetSdk 36 on a device running Android 17. Google's floor
  moves on a schedule and the Play Console is the only place to read it.

---
(Claude Code appends here.)

## The reset, September 6, 2026

The working tree and the GitHub repository were rebuilt to match the capability
specification. Recorded here because a forced history rewrite should never be
something a reader has to reconstruct from a diff.

**What was deleted.** Everything in the working directory except `.git`, then the
kept files were restored from the handoff archive. What went: a complete Kotlin
and Compose source tree for the weight-tracking version (app/, roughly 700 unit
tests, an instrumented suite, a Room schema, a working Gemma 4 E2B integration),
its Gradle scaffolding (build.gradle.kts, settings.gradle.kts, gradle.properties,
gradle/, gradlew, gradlew.bat, config/), its build output (build/, .gradle/,
.kotlin/, 379 MB in total), its repository documents (README.md, ARCHITECTURE.md,
CONTRIBUTING.md, SECURITY.md, CODE_OF_CONDUCT.md, CHANGELOG.md, HANDOFF.md,
LICENSE, .github/, .gitignore, docs/, licenses/), its earlier specification
documents, the old tools/ directory, an untracked `local.properties`, a
`gradle-env.sh` helper, and a stale `steady-project.zip`.

**What was kept.** Only what arrived in the archive: MASTER_SPEC.md, DESIGN.md,
LOGIC.md, AI.md, ONBOARDING.md, CONTENT.md, COMPLIANCE.md, PRIVACY.md, VISUALS.md,
DECISIONS.md, START_HERE.md, MASTER_PROMPT.md, and the design/, research/,
standards/ and tools/ directories.

**What could not be deleted.** Nothing. The repository had one branch and no tags,
releases, issues or pull requests, so there was nothing to close or relabel. The
repository itself was kept, as instructed, because the URL and settings are worth
keeping.

**The fresh initial commit** is `dd4c3bd491740a978ed58ebe65f2722ebf6d1e9b`, "Steady
Health: a record of what your body can do, and what changes it", force-pushed to
main so the previous 39 commits are no longer reachable. `.gitignore` is in that
commit, before any file that could carry a secret could exist.

**The repository was not renamed.** `steady-health` describes the app as
MASTER_SPEC.md describes it and was never named for the weight framing. Its
description, website and topics were rewritten: `weight-tracking` was removed and
`functional-fitness`, `mobility` and `gemma` were added.

**One inconsistency to note rather than fix silently.** DESIGN.md section 8 step 6
tells a builder to compare against `design/steady-today-approved.html`. The file in
the archive is `design/today-approved-look.html`. Same file, older name. Treated as
the current name, and DESIGN.md is left as written because it is the binding
document and correcting a filename inside it is the owner's call.

## ADDENDUM-01 folded in, September 6, 2026

The visit summary arrived as an addendum after the reset and after Phase 0 had
started. It was committed as received in `47465c9`, then folded into the six
documents it names in `230a3d2`, and the addendum now carries a merged note at its
top pointing at that commit. It stays in the repository as the record of what was
asked for, and is no longer a source of truth.

**Two things the addendum implied rather than stated, decided here.** AI.md opened
with "the model is visible in exactly three places" and MASTER_SPEC with "three
visible jobs". The addendum calls the summary "the app's principal use of the
language model", which makes four. Both were updated rather than left contradicting
the job 6 they now contain. Reversal: none contemplated; the alternative was a
document that argues with itself.

**The doctor page was renamed, not removed.** MASTER_SPEC 6.7 and LOGIC 13 are now
"the measures table", which is what they always described, and the written summary
sits above the table from Phase 5. Three stale references to "the doctor page" and
"the doctor summary PDF" elsewhere in LOGIC.md and DECISIONS.md were updated in the
same pass, because a rename that leaves the old name in three places is not a
rename.

**Build order consequence.** The addendum is explicit that the job 6 validator is
built before the model is wired in, with its adversarial corpus. That is recorded
here because it inverts the obvious order: the natural instinct is to get a
sentence out of the model first and check it afterwards, and for this one feature
that is the wrong way round. An invented chair-stand count in a page somebody hands
to a clinician is the failure that actually matters, and the validator is the only
thing standing between the model and that page.

**Phase.** The feature lands in Phase 5. Phase 0 was in progress when the addendum
arrived and continues from where it was.

## Phase 0

### The palette and the accessibility floor disagreed, and the floor won for words

DESIGN.md section 2 gives the approved colours and section 7 asks for 4.5:1 on
ground and on white. Measured, four of them cannot do both: ink3 is 2.53:1 as a
caption, orange-d is 3.22:1 as a word, green is 4.21:1, and the primary button,
white at 16/800 on orange, is 2.54:1 against a threshold of 3.0 for text at that
size and weight.

The resolution keeps every approved hex and adds nothing new to the palette. The
table gives fills, lines and illustration; a word painted in one of those hues
takes the darkest member of the same hue that clears the floor. So ink3-text
#6C708F, orange-text #BD5114, green-text #287855, each derived by darkening the
approved colour along its own hue until it passed on both surfaces. ink3 itself is
unchanged wherever it is a shape.

One fill moved with it. The primary button is filled orange-d #E86A22, already in
the table as "pressed state", which gives white 3.22:1. Orange #F5843E keeps every
other job it has: the walk, the weight line, the morning sky, the dial arc.

Written into DESIGN.md section 2 before being built, as section 8 requires, and
held by `ContrastTest`, which also checks that no colour in the palette is red.

### Two smaller readings of DESIGN.md, recorded so they are not mistaken for drift

The tab bar label is 11 sp, which section 3 specifies and which is small for the
people this is for. Kept, because it is sp and grows with the system font setting,
and because changing an approved size is a larger deviation than the one it would
fix. The inactive tab colour is ink3-text rather than ink3, for the contrast
reason above.

Section 8 step 6 says to compare against `design/steady-today-approved.html`. The
file in the archive is `design/today-approved-look.html`. Same file, older name.

### Figtree ships as one variable font

Google Fonts publishes Figtree only as a variable font now, and that is the better
answer anyway: 62 KB covers weights 400 through 900, against roughly 1.6 MB for
six static files covering six points on the same axis. Variable fonts are read
from API 26 and this app is API 29 up, so there is no fallback to maintain.
Font synthesis is turned off, because a synthesised bold would be a worse copy of
a weight the file already contains.

### Icons are bundled as vectors, not as a dependency

Phosphor is MIT and the five icons the shell needs were converted from its own SVG
source into Android vector drawables, with the licence in `licenses/`. A
dependency for five paths would be more to audit than to read.

### Rive is not in version 1, measured rather than argued

Phase 0 asks for a Rive smoke test. Three facts came out of it:

- The runtime is MIT, which answers the BLOCKED question. Confirmed from the
  published POM and the repository licence, not from memory.
- It adds **15 MB** to the debug APK: 39 MB to 54 MB, of which 6.2 MB is a native
  library per ABI.
- It brings `com.android.volley:volley` onto the runtime classpath. Volley is an
  HTTP stack, and this app declares no internet permission at all until the
  optional model download.

DESIGN.md section 5 names Rive for three things: the done morph, the Go press, and
the walk marker. All three are ordinary Compose animations. Fifteen megabytes and
an HTTP client for three animations, in an app whose privacy claim is that there
is nothing to send anywhere, is a bad trade.

There is a fourth cost the measurement does not show. A `.riv` file is authored in
the Rive editor, which needs a paid plan to export, and the result is a binary blob
whose real source is a proprietary project file. In an AGPL repository that is a
corresponding-source problem as well as an ongoing cost to the owner.

So version 1 animates in Compose. This is the reversible direction: adding Rive
later is a dependency line, whereas removing it once the animations are authored
as `.riv` is a rewrite. Recorded under BLOCKED as an owner decision because it
departs from a binding document, and the app is built without it in the meantime.

### The whole schema is defined at version 1

Rather than growing a table per phase. The standards require that anything the app
can store, the export contains and the import restores, and a table that first
appears in phase five is exactly the kind that gets left out of the export and is
not noticed until somebody tries to restore. LOGIC.md already says what the app
stores, so there is nothing to discover later that would justify a migration.

### Two things the Phase 0 device test found that no laptop could

SQLCipher's native library is not loaded for you. Room 3 opens the database
through `SQLCipherDriver` and the first query throws `UnsatisfiedLinkError` without
`System.loadLibrary("sqlcipher")`. A JVM test cannot catch this, because there is
no native library there to be missing.

Deleting the database by a list of known suffixes is not enough. The device had a
`steady.db.lck` that a list of `-wal`, `-shm` and `-journal` did not cover.
Deleting every file whose name starts with the database name is the only version
that makes "there is no copy anywhere else" true.

Both are the reason the phase has a smoke test rather than a compile check.

## Phase 1

### The spike cap as written makes the ladder unclimbable

LOGIC.md section 6 caps an offered walking step at 110% of the longest session in
the last month. Read as arithmetic against the next rung, that rule freezes
everybody at two minutes forever, because the ladder in the same document is 2, 4,
6, 8, 11, 14, 16, 20, 25, 30 and every one of those is more than 110% of the one
before it. The smallest jump on the whole ladder is 14%.

So the cap is implemented as what it is for. Frandsen et al. found that what
predicts injury is a single session much longer than anything done recently, not a
steady climb, and the ladder is the steady climb. The case worth catching is
somebody sitting at step seven whose actual walks have been eight minutes being
offered twenty. The rule: if they have been doing the step they are on, the next
rung passes; if their recent walks are below their current step, the 110%
arithmetic applies against what they have really been doing. A test walks every
rung of the ladder and asserts each one can be reached.

### Four ability glyphs, four shapes

The first version used one shape in four colours, and two of them reused the fixed
daily glyphs, so the speech bubble meant both "say how today went" and "Carry" on
the same screen. They are now four different shapes, which the accessibility floor
requires anyway: nothing in this app is carried by colour alone.

### Two defects only the phone could show

Every glyph drew at zero size inside its tile. A Canvas with no constraints draws
nothing, and the result looks like a missing icon rather than a bug in a default,
so the glyphs now fill their slot and a caller can still override.

The keyboard covered the primary button on every screen with a text field. Going
edge to edge means `adjustResize` no longer insets Compose content, so the screen
scaffold takes the keyboard inset once, in the same place it takes the status bar.
Found by trying to save a check-in and having the tap land on the letter v.

### A tension worth watching, not yet resolved

The walking screen's button says "Stop here, that counts", and LOGIC.md section 3
says a session counts as a day moved at two minutes or more. Somebody who stops at
twenty seconds is told it counts and then sees the daily card still undone. Both
sentences are specification. Left as written, because the first walk on the ladder
is two minutes and stopping early is the unusual case, but it is the kind of thing
a real user notices before anybody else does.

## Phase 2

### The four ways of getting around are four versions, not one with parts removed

The domain ids never change: everything stored carries Get up, Go, Carry or
Steady, and a row written last year has to keep meaning what it meant. What
changes is the word on the tile, the ladders behind it, and whether weighing in
is part of the day. So `WayOfGettingAround` carries structure only, `renamed` says
which of the four are called something else, and the words themselves are string
resources, because they are read by a person and the app ships in four languages.

The wheelchair version has its own ladders rather than the walking ones with
walking taken out. The pushing ladder is not offered from a chair because every
one of its steps is written for somebody standing at a wall or a counter, and
rewriting the words to mean something else would be describing a movement nobody
has thought about. The seated ladder carries Carry instead, with the band press
in it twice.

Wheeling uses the same numbers as walking: 2, 4, 6, 8, 11, 14, 16, 20, 25, 30. A
minute is a minute, the talk test works the same way, and giving that ladder its
own shorter shape would be the interface saying something about the person that
it has no business saying. A test holds the two lists equal.

### The bed set is a set, and its title comes from its parts

Screen 18 shows three things done back to back rather than one step at a time, so
`bedSet(stepIndex)` returns a list and progression adds a part rather than making
a part longer. The title is computed from what the parts add up to, so adding a
fourth changes the words without anybody having to remember to.

Rounded to the nearest minute, not up: the set runs a hundred and forty seconds
and the approved copy calls it two minutes. Rounding up would call it three,
which asks for more than the app is about to ask for.

### Digits, not words, for the length of the bed set

The grid writes "Two minutes in bed". Every other amount in the app is a digit
("A 2 minute walk", "8 times", "40 seconds"), and this one is computed, so
spelling one of them out would put two conventions on one screen. Both the fixed
card and the computed title now read "2 minutes in bed".

### "Shown as a lever, not a score" could not ship as written

Grid screen 22 writes the weigh-in row that way. The word is on DESIGN.md section
6's banned list and rule 1 of the brief admits no exception, not even for a
sentence that denies the thing. The row now reads "One of the levers, not the
point", which says the same thing in allowed words. Caught by the voice test,
which reads the real strings.xml.

### In bed, the talk test becomes how you feel

The grid's own note on screen 18. "Could you have held a conversation?" is the
wrong question for a set of ankle pumps. The three answers still carry the same
meaning to the progression rules, so nothing behind the screen changes: Good,
About the same, Tired.

### Pacing mode turns itself on, and only the person turns it off

LOGIC.md section 7 makes both the pattern question and two reports of "much worse
the next day" in a month triggers. Entering is automatic because pacing mode is
the careful state, it removes offers and stops anything increasing, and entering
it costs nothing that cannot be undone from settings. The app says so once, in a
block on Today, rather than changing quietly.

Leaving is not automatic and never will be. It has its own screen, and leaving
lands on the pattern question again rather than back in a list of switches,
because section 7 says the app re-asks it.

In pacing mode the ladder is not the headline: Move leads with "Up to 5 minutes"
rather than the step name, because naming a step would be the app suggesting a
number the person did not choose. The decision chain is not run at all, so there
is no offer to suppress.

### Gap decay runs when the app opens, not after a session

Somebody who has been away for a month meets the decision before they do
anything, not after, so `ReturningEngine` is separate from `ProgressionEngine`
and runs on refresh. It is applied once per gap: the notice is keyed by ladder
and by the day they last did something, so opening the app twice does not take
four steps off anybody. The fortnight of easing after a long gap suppresses
offers the same way a "not yet" does, which is the one thing both of them mean.

Two steps is the most it ever takes, however long somebody has been away, so
coming back after a year is not worse than coming back after a month.

### Settings exists now, because the way of getting around has to be changeable

LOGIC.md section 3b says it is set at onboarding and changeable in settings, and
until now the gear did nothing. Built: how you get around, working with a
therapist, weigh in, show numbers, anything to leave out, the pattern question,
and the limit. The rest of grid screen 22 (try it and see, the reader, language,
reminders, your data) lands with the features it belongs to, rather than as rows
that do nothing.

Changing how you get around keeps everything already recorded. Sessions carry the
ladder they were done on, so a walk from before a wheelchair is still a walk.

### Step names are still English literals in Ladders.kt

Every step's name and instruction is a Kotlin string, which cannot be translated.
This was already true of the walking ladder and is now true of three more. It is
a real gap and it belongs to Phase 6, which is where the four languages are.
Recorded here so it is not discovered there.

## Phase 3

### The on-device model path, verified at build time

The brief says to check rather than trust the documents, so: Gemma 4 E4B ships as
`gemma-4-E4B-it-litert-lm.litertlm`, 3.66 GB, Apache-2.0. The Android path is
LiteRT-LM, `com.google.ai.edge.litertlm:litertlm-android`, 0.17.0 as of today,
with `Engine`, `EngineConfig` and `Conversation` as the entry points and
`initialize()` taking up to ten seconds. MediaPipe's `.task` format is now the
legacy path and is not the one to build on. Nothing about constrained decoding is
documented, which is why the tag validator in TagReader is a validator and not a
prompt instruction.

### The no-model path is the app, not a fallback

AI.md makes the model an optional 3.66 GB download, off by default. That means
for most people the deterministic path is the whole feature, so it was built
first and built properly: the tag grid is how tags are chosen, the Sunday note is
hand-written sentences chosen by conditions, and Ask a question is a word search
over thirteen hand-written cards. None of it is a placeholder waiting to be
replaced.

Building it first also gives the model something to be checked against. Every
rule the model output has to pass, the template output passes too, and the same
tests hold both.

### The cards come out of CONTENT.md rather than being retyped

CONTENT.md is final copy and ships as written, so the strings were generated from
it into cards.xml and the voice test now reads that file as well as strings.xml.
Three sentences would not pass rule 1 as written and were changed: "not the
score" became "not the point" (the same fix as the settings row), "Your
prescriber's tools" became "Your doctor's tools", and nothing else.

Four uses of a banned word are pinned exceptions with reasons, in a map the voice
test enforces both ways: it fails if one of them appears anywhere else, and it
fails if one of them stops being needed. Two are the names of things, a research
method in a citation and the Patient-Specific Functional Scale. The other two are
card 7, whose subject is the thing the app does not do; a card called "Why there
are no calories here" cannot be written without the word, and banning it there
would leave somebody without the explanation the ban exists to give them.

### The Sunday write-up lives on Abilities

It is not in the grid. DESIGN.md section 8 says to build from the rules and match
the nearest sibling, and the nearest sibling is the Abilities tab: read-back is
what that tab is for, and Today is for today. Written once and stored, so it does
not quietly change under somebody who read it yesterday.

### Three view models, not one

`SteadyViewModel` had grown past the point where anybody could hold it, so Ask a
question and Settings moved out. Both splits are real rather than cosmetic: Ask
touches no repository at all, and Settings only writes. Nothing has to reach
across, because Today, Move and Abilities each read the profile again when they
come back into view, which is also what makes changing how you get around safe
from a screen that knows nothing about them.

### The restriction rule is a filter, not an instruction

No sentence may put a restriction tag and a weight direction together. It is
implemented as a post-filter over whatever produced the sentence, model or
template, because a prompt instruction is a request and this is a guarantee. It
is the one sentence this app exists not to say.

## Phase 4

### The check is four measures, not three

Grid screen 12 shows three rows and MASTER_SPEC section 6.5 says "three or four".
Four, because with three there is no measure of Go, and an ability that can never
change is worse than a check that takes two minutes longer. The fourth is the
two-minute step, and it is last in the order so that somebody who has had enough
can skip it without missing the others.

Every measure is skippable, and skipping leaves no row. There is no record
anywhere meaning "did not do", because a table of things somebody did not manage
is not what this is.

### A measure with no published detectable change never decides anything

LOGIC.md 3b computes Better, Same and Quieter from whether a measure moved beyond
its detectable change. Most of the measures here have no published one: the chair
stand does (2 to 3 repetitions, Rikli and Jones, and the cautious end is taken),
and the four-stage balance does by construction, and the rest do not.

So `Measure.detectableChange` is nullable, and a measure with none is recorded and
shown and can never move an ability off Same. A test holds that: a wall push-up
count falling from thirty to five across six checks still reports Same. The
alternative is telling somebody their body is failing on the strength of a number
nobody has established is meaningful, which is the single worst thing this app
could do.

### Quieter costs three consecutive checks

One month is a bad morning, two is a coincidence, and the app says nothing about
either. Three months of silence is the price of never telling somebody their body
is going backwards because they were tired on a Tuesday, and it is worth paying.
Every step of the run has to be beyond the measure's own detectable change, so a
single steady month breaks it.

### Counting is allowed to be low and is never allowed to be high

`RepCounter` is deliberately conservative. Under-counting means somebody's number
is a little low and they can count by hand instead. Over-counting means the app
tells somebody they did fourteen stands when they did nine, which is a lie about
their body and would poison every comparison after it. There is a test whose only
job is that the count never exceeds the truth, across four rep counts and three
cadences.

The counter has no Android types in it, so all of that runs without a device. The
traces are synthetic and that is stated in the test file rather than implied: they
show the counter does what it is written to do, not that a Pixel in a pocket
produces this shape.

### The life sentence is a table, and an unmeasured tile keeps the person's words

Twenty-four hand-written sentences keyed to a measure crossing a threshold, with
the highest one reached being the one shown, so a sentence only ever describes
something the person has actually done. Before the first check there is no
sentence, and the tile carries their own words from setup instead. A made-up
sentence about somebody's life is worse than none, and an empty tile says nothing
at all.

The thresholds are not norms and are compared to nobody. They are the points where
a number stops being a number: fourteen chair stands is getting out of a low sofa
without thinking about it.

### The wheelchair check does not include a standing balance test

Every stage of the four-stage balance test is standing. Replacing it with sitting
unsupported is not a smaller version of the same thing; it is the right measure
for the ability. Same reasoning as the ladders.

### A test that every route has a screen

A route was declared, navigated to from two places, and had no `composable`
registered for it. The compiler cannot see that and no other test would have
caught it; it would have been a crash on tapping an ability tile. `RoutesTest`
reads the source and holds every declared route to having something that draws
it, and every navigation to a route that exists. Crude, and the only way to check
it without a device.

`Route.OFFER` was removed at the same time. The offer is a thing the app is saying
rather than a place somebody went, so it covers the screen from outside the graph
and never needed a route.

### The ability page is useful before anything is measured

Grid screen 9 shows an ability with months of history behind it. On the first day
there is none, and an empty page teaches nobody what the ability is, so the
exercises that feed it are listed from the start. The three states are separated
in the copy as well: nothing measured, measured once, and measured twice with
something to compare.

### Band rows feed Carry, whatever ladder they sit in

LOGIC.md groups seated band rows into the chair-and-standing ladder because of
where they are done. The ability they feed is Carry. MASTER_SPEC requires every
exercise to belong to exactly one of the four, so the step carries the domain and
the ladder it sits in carries none.

## Phase 5

### The validator was built first, and the model still is not wired

ADDENDUM-01 is explicit: "the validator in AI.md Job 6 is not optional: build it
before the model is wired in, with the adversarial fixture corpus." So the order
was validator, corpus, engine, template writer, screen, and the model has not been
touched. It goes into the same slot the template writer occupies and through the
same check.

The corpus is 37 deliberately bad paragraphs, every one of them a sentence a
language model writes readily against the brief: a computed percentage, a
projection, a month that is not in a cited fact, an altered quotation, "which
means", a condition name. All 37 are caught. Six honest paragraphs are in the
same file and all six survive, because a validator that rejects everything
protects nobody: the summary would always fall back and the feature would be
dead.

### "Directly entailed" is read strictly

AI.md says every number must appear in, or be directly entailed by, a cited fact.
A looser reading would need the validator to do arithmetic, and a validator that
computes is a second thing that can be wrong. So: a digit passes when its value is
in a cited fact's numbers, and a number written as a word passes either that way
or when the word itself is in a cited fact's text.

That second half exists because "One foot" is the name of a measure, not a claim
that something happened once, and the engine's own sentence says so. Found by the
test, not by thinking about it.

### The template summary passes the same validator

Forty-eight briefs, generated from the axes that actually change the output rather
than typed out, because a Quieter domain with a gap and a falling item is a
different brief from any of them alone and is exactly the combination that breaks
a template. Every one of their template summaries passes the check.

A template that could not pass would be one nobody should trust either, so the
property is worth more than the coverage number.

### Every fact declares its own numbers

A fact whose text says "A 14 minute walk" has to declare 14, or the validator is
right to reject a summary that repeats it. Found by the test on the first run,
and it is the kind of thing that would have shipped as a mysterious fallback.

### The PDF is drawn, not rendered

Android has no HTML-to-PDF path that does not involve a WebView, and a WebView in
a local-first app is a network stack sitting next to somebody's health record for
no reason. So the page is drawn onto a `PdfDocument` canvas.

It leaves through a `FileProvider` that exposes exactly one cache directory and
nothing else, so a mistake in a share intent cannot hand somebody the database.
Nothing is transmitted, and the app has no network permission with which to.

## Phase 6

### The database is resolved on every use, not held

Deleting everything closes the database and destroys its key, and every view
model was holding the old instance in a `lazy`. The next write threw "Database is
closed", which on the phone meant a crash on the first screen of setup,
immediately after somebody had deleted everything. That is the worst possible
moment for this app to crash, and it was found by actually pressing the button
rather than by reasoning about it.

The repositories are stateless wrappers, so resolving them per call costs one
object allocation and removes the whole class of bug.

### The export is CSV and PDF in a zip, not a format only this app reads

PRIVACY.md promises "ordinary files: spreadsheets, your photos, and a one-page
summary". An export somebody cannot open is not an export, so it is five CSVs and
the summary PDF, and the CSV writer is tested against sentences containing a
comma, a quotation mark, a newline, Arabic and Chinese. The person's own
sentences are the part of that file nobody else could reproduce.

### Deleting asks once, and "Keep it" is the primary button

There is no undo and the screen does not pretend there might be. The safer answer
is the one under your thumb, and the destructive one is the ghost button, which is
the opposite of the usual arrangement and the right way round here.

The summary is on the same screen, above both, so somebody about to delete
everything is one tap from taking a copy first.

### The visit summary is built in one place

It is shown on screen and put in the export, and both have to say the same thing.
Building it from two call sites is how they stop saying the same thing, so
`SummaryPages.build` does it once and both use it.

### Numbers off replaces the weight figure, and leaves the counts alone

DESIGN.md says numbers-off replaces "every figure" app-wide, and the example it
gives, the destination line it hides and the chart axis it removes are all weight.
LOGIC.md section 10 extends it to the minutes in the Sunday note, and that is
done too.

What it does not do is replace a chair-stand count with a direction word. Fourteen
stands, up from nine, is the thing this app exists to show, "a little more" cannot
express it, and screen 12 of the grid shows those numbers with no numbers-off
variant beside it. The switch exists so nobody has to look at their weight as a
figure; it is not a switch for looking at less of your own capability.

The hero drops to the title size when it is carrying a word rather than a figure,
because "A little lower" set at sixty-four point nine hundred wraps to three lines
and reads as shouting.

### The accessibility floor is checked on the device, in the semantics tree

A screen whose labels are right in the source and wrong in the tree is the failure
worth catching, and it only exists once something has composed. Nine instrumented
tests: every tile and card says what it is and what state it is in, nothing
tappable is unlabelled, and Today still lays out with text at twice the size.

The font scale is overridden inside the test rather than set on the phone.
Changing a system setting to run a test leaves the owner's phone changed if the
test crashes, and this app's rule is to touch nothing else on the phone, ever.

An earlier attempt to audit this by reading uiautomator's dump concluded that
every tile was unlabelled. It was reading the wrong node: Compose puts the merged
label on one node and the click action shows on another in that dump. The
semantics tree, which is what TalkBack actually reads, has them together, and
`assertHasClickAction` on the labelled node proves it.

### Two a week is a rule in the app, and the screen says so

LOGIC.md section 12: "Hard ceiling of two notifications in any rolling seven days
regardless of switches." Regardless is the word that matters, so the ceiling is a
pure function every path asks before sending, rather than something the scheduler
is trusted to respect. A scheduler is a thing that runs at seven in the morning
with nobody watching.

Rolling, not weekly: there is no Monday on which somebody's allowance comes back
all at once. The settings row shows how many are left, because a limit somebody
cannot see is a limit they have to take on trust.

Only a notification that actually went out counts against the ceiling. One the
system swallowed should not cost somebody their week.

The permission is asked the first time a switch goes on and never before, which
ONBOARDING.md is explicit about. `areNotificationsEnabled` is the question rather
than the permission state, because the runtime permission only exists from Android
13 and somebody on 12 can still switch notifications off.

### The device tests were wiping the phone

Two separate problems, both found by running them.

`DatabaseSmokeTest.destroyLeavesNoFileAndNoKey` destroyed the real database and
the real key, because that is what it was written to prove. It now uses its own
database name and its own Keystore alias, with its own wrapped-key file, and a
fourth test whose only job is to assert those are not the real ones.

Separately, Gradle's `connectedDebugAndroidTest` uninstalls both APKs when it
finishes, which takes the app's data with it. That is a wipe dressed up as a test
run on a phone somebody is using. `tools/device-tests.sh` installs and runs
through adb instead and leaves everything where it was.

Both are the template's rule about data-affecting tests, learned the hard way on
the owner's own device.

### The language picker shows only the languages the app can speak

It stored a choice and never applied it, which meant tapping Español on the first
screen did nothing at all. Somebody who taps their own language and sees English
concludes the app is broken, not that it is unfinished, and they are not wrong to.

So the picker asks the resources which languages actually have strings behind
them and shows only those. Today that is English alone and the row does not
appear. It comes back on its own when a `values-es` folder lands, and nothing has
to be changed for that to happen.

Switching goes through the platform's own per-app language setting rather than
through appcompat, which would mean adding a whole legacy UI toolkit for one
call. That means it works from Android 13 up and below it the app follows the
phone, which is stated in the code rather than papered over.

Lint then caught the part that would have failed in the store: an app bundle
splits language resources by default and fetches the rest through Play Core on
demand. This app has to work offline from the moment it is installed, so the
split is off and every language ships inside it.

### Try it and see offers one variable in version 1

LOGIC.md 9b permits six: time of day, order of exercises, which exercise, walk
before or after, indoor or outdoor, and rest between sets. Version 1 offers the
first, which is the one grid screen 14 draws, and the enum holds all six so
adding another is a table entry rather than a design.

One honest test beats six half-thought ones, and the interesting work here was
never the number of variables. It was the three rules about not finding things: a
restriction tag can never produce a pattern, a difference inside the measure's own
noise has no winner, and nobody in pacing mode is ever offered any of it.

That last one is not a convenience. For a post-exertional pattern, deliberately
varying what you do for a fortnight to see what happens is the thing that causes
harm, so it is not a decision the person has to make while unwell.

The offer row on Abilities appears only when there is something to offer or
something to report, and is never badged. An app that nags somebody about an
optional experiment has misunderstood what the experiment is for.

### The tiles carry the short form of a life sentence

Grid screens 5 and 8 write these telegraphically: "Floor without hands", "Two
flights without a stop", "Groceries in one trip". The full sentences, which are
right on the check result and on the ability page, get an ellipsis put through
the middle of them in a tile, and a truncated sentence about somebody's life says
less than either version.

So there are two forms of each, and the tile takes the short one. A tile with
nothing measured and nothing on the person's own list says what the ability is
instead of standing empty, which also teaches somebody what Carry means here
without a page about it.

The tiles are a minimum height rather than a fixed one, and the two in a row match
each other, so a two-line sentence takes its neighbour up with it. That is also
what stops them breaking at twice the text size.

### The monthly check re-rates the person's own list

LOGIC.md 3b says so in four words, "Re-rated monthly with the check", and it was
the missing half of the check: the measures were being taken and the ratings that
CONTENT.md card 12 calls the reason the app asks at all were not.

It comes after the measures, because the numbers are the part that needs a chair
and a wall and somebody who stops there has still done the useful half. Somebody
with nothing on their list does not get an empty screen asking them to rate
nothing.

Two points or more is a change and one point is not, which is the Patient-Specific
Functional Scale's own detectable change of about 1.3 to 3 rounded to the
cautious side. It is the same rule as the measures, for the same reason.

The result screen shows whatever was actually done. Skipping every measure and
only re-rating your list used to produce a screen saying "Done" with nothing under
it, which makes it look as though nothing happened.

### The release build, and what R8 nearly broke

The debug APK is 41.7 MB, almost all of it unminified DEX. The release build with
R8 and resource shrinking is 7.5 MB, which is the number that matters: this app is
for people who may be on a metered connection and an old phone.

The keep rules are short and every one of them names what it protects. The one
that would have hurt is SQLCipher: its JNI layer looks classes up by name from C,
so R8 cannot see the reference, and getting it wrong is an `UnsatisfiedLinkError`
on the first query, in release only, on somebody's actual phone. That is exactly
the class of bug a debug-only test run never finds, so the release build was
installed on the Pixel and driven end to end: the database opened with existing
rows in it, the monthly check ran, the visit summary generated and exported as a
PDF, the export zip was written, the tag grid saved, and a card opened.

Release is signed with the debug key for now, so it installs over the debug build
in place and keeps the data. That is a testing convenience and is marked as one in
the build file: a debug-signed build cannot go to Play, and the real keystore is
an owner task on the BLOCKED list.

### `lintRelease` fails on an out-of-date Kotlin, and it is right

It runs `NewerVersionAvailable`, which `lintDebug` does not. Kotlin 2.4.20 was an
RC when the versions were pinned in Phase 0 and is now the stable release, so the
pin is genuinely stale. Recorded rather than fixed in the same breath as a
handover build, because a compiler upgrade is its own change with its own
verification.

## ADDENDUM-03 merged, September 7 2026

Saved as `ADDENDUM-03-experience.md` on commit **9837f2a** and folded into the
documents it names in the commits that follow. The file stays in the repository as the
record of what was asked for, with a note at the top saying it has been merged.

**Where it went.**
- ONBOARDING.md: replaced in full by Part 3.
- MASTER_SPEC.md: section 3 (the three directions), section 5 (four tabs, Part 20),
  section 6 (6.1 Today, 6.3 Sessions, 6.4 Progress, 6.6 deferred, 6.9 You, 6.10
  deferred, and new 6.11, 6.15, 6.17, 6.18, 6.19, 6.20, 6.21), section 8 (platform),
  section 9 (Part 21's phases), section 11, and a new section 12 (Part 19).
- DESIGN.md: section 4 (four tabs), new 4b (help, Part 4), section 5 (motion, Part 12),
  new 5b (the session screens, Part 1), section 6 (voice, the banned additions, the
  three registers, warmth in four places), section 7 (accessibility as design, Part 17).
- LOGIC.md: 3b (confidence, Part 18), 6 (progression rewritten around the session), 12
  (notifications and the widget, Part 13), and new sections 15, 15b, 16, 17, 18.
- AI.md: the models section rewritten for two optional models, job 1 made reusable, and
  new jobs 7, 8 and 9 with job 9's validator and the fixture requirements.
- COMPLIANCE.md: the therapist's plan, and the HAI-DEF boundary for document reading.
- CONTENT.md: the card library expansion, Part 8 item 5.

### What this supersedes

**Superseded outright:**
1. **The whole of ONBOARDING.md's eleven-screen sequence.** It asked for units, height,
   age, capability, exclusions, readiness, an anchor and a weigh-in before the person
   had done anything at all. The new rule is that the person does something real
   before answering anything optional, and the measure is under sixty seconds from
   launch to the first session.
2. **The three-tab navigation** (Today, Move, Abilities). Four tabs now: Today,
   Sessions, Progress, You.
3. **"Move" as a screen of one strength set plus a walk.** Replaced by the session: a
   led, audio-driven, four-to-eight minute thing with seven screens.
4. **The gap decay rule in LOGIC.md 6** (8 to 28 days one step, 29 or more two steps).
   Replaced by the interruption question in Part 15, which asks why rather than
   assuming.
5. **The reminder design in LOGIC.md 12.** The two-a-week ceiling stays for everything
   except the one daily prompt, which is now on by default and stops asking on its own.
6. **The accessibility floor in DESIGN.md 7.** Raised: 56dp in a session, 16sp body
   text, 200 percent font scale tested, haptics on every rep.
7. **Rive**, which was already dropped in Phase 0 and is now not named in the platform
   section at all.
8. **The phase plan in MASTER_SPEC.md 9**, and Addendum 02's.
9. **The visit summary interface** and **Try it and see**, both deferred to version 2.
   Their engines, validators and corpora are built and stay built.

**Reversed:**
10. **MedGemma is not used.** This was recorded in Phase 0 on the reasoning that the
    app deliberately avoids medical vocabulary, so a medical model bought nothing
    against a restrictive licence. That reasoning was correct for the app as it then
    was. ADDENDUM-03 Part 7 adds a feature where the medical vocabulary **is** the
    difficulty: a person holding a progress report full of MMT grades and ROM in
    degrees, who is the subject of the document and the audience for none of it.

    The reversal is recorded rather than quietly made because it departs from a
    decision under "Visible AI", and the rule is that those are never reopened without
    saying why. The why is that the earlier decision answered a different question.

    What makes it safe is not the model but the boundary around it: the app explains
    the words and never interprets the document, and that boundary is what keeps it
    outside HAI-DEF's Clinical Use definition. It ships behind a flag that is off until
    a health tech attorney has read it, which is on the BLOCKED list.

**Kept unchanged**, as the addendum says: the design tokens and the look, the
compliance position, the model output contracts, pacing mode, exclusions in movement
terms, and the rule that the model never states a number, never decides an ability's
state, and never writes a life sentence.

### The banned list grew

Added: test, workout, routine, level, unlock, achievement, complete, missed, streak,
hazard, risk. "Plan" is permitted only for a therapist's plan.

Three of these are already in shipped strings and have to change: the card "What the
talk test is" becomes "How hard should it feel?", the talk-test question keeps its
wording but loses the name, and the monthly check's "Skip this one" is unaffected. The
voice test is the thing that will find the rest, and it reads the real resource files.

## Addendum 03, Phase 1a

### Targets move from the last ask, not the last result

Found by running two sessions on the phone rather than by reading the rule. The
first version took the last result as the base, so a thirty-second warm up that
somebody stopped after five seconds asked for five next time, and would have asked
for four after that. A doom loop, from one interrupted set.

Targets now move from what was asked, which is what the adaptation rules in
ADDENDUM-03 Part 1 actually describe: "targets drop about ten percent" is a
proportion of the ask. The exception is falling a long way short, under sixty
percent, where the ask comes down to meet what was managed, because a target nobody
can reach is not a target, it is a reminder of what they cannot do.

### The session is one route, not seven

The seven screens are one destination whose content changes. A back gesture in the
middle of a set should not drop somebody onto the count-in screen of the movement
before, and a session is one thing that is happening rather than a place somebody
navigated to. The exits are how a session ends, and there is no back button on any
of the seven.

The tab bar is hidden during a session for the same reason: it is a fourth way out
that is not one of the three exits and does not save what was done.

### The state machine holds no Android types

`SessionRunner` has no context, no timer and no voice; the view model drives it with
`tick` and the screens read it. That split is what lets a test walk every state a
session passes through and press each of the three exits and the pain button from
all of them. ADDENDUM-03 Part 21 asks for exactly that check, and it is more points
than anybody would tap through by hand.

### Fifty movements, not sixty

ADDENDUM-03 Part 4 says "sixty or so movements" in the library. There are fifty,
covering all four abilities across all four ways of getting around with easier and
harder variants. The remaining ten are content rather than engineering and are
better written against a real library than guessed at now; adding one is a table
entry.

### The schema went to version 2

The session needs three tables that did not exist: runs, run movements, and the
areas somebody said hurt. Purely additive, so the migration is generated rather
than written and nobody's rows move. This is the first departure from "the whole
schema is defined at version 1", and it is one the addendum forced rather than one
that was foreseeable.

### The import sort key was wrong twice

Worth recording because it cost two build cycles. ktlint puts `java.`, `javax.` and
`kotlin.` last, in that order. `kotlinx` is not `kotlin.`, and a prefix check on
"kotlin" sorts kotlinx.coroutines after net.zetetic. A tuple of three booleans also
sorts kotlin before java, because false sorts before true. It needs a single rank.

### Phase 1b, and what was decided while building it

**The first session's movement is named, not derived.** O4 has to need nothing in the
room, be over in about two minutes, and look like something rather than like a warm
up. A rule that picked "the shortest available movement" got one of those three
right, so `Movements.first(way)` names one per way of getting around: heel raises on
feet or with a walker, an overhead reach in a wheelchair, sitting up to the edge in
bed. `SessionEngine.first` still falls back to the smallest ask if the named one is
excluded, so nobody ever reaches a first session with nothing in it.

**The chair is asked about after the first session, not before.** O5 asks it, which
means the first session cannot assume a chair exists. That is why the named first
movements all need nothing but the floor.

**The phone counts stands and steps.** `Sensed` is a field on a movement rather than
a heuristic: a stand lifts the phone a long way and a step lifts it a little, and
everything else is counted by the person tapping, which is the honest answer rather
than a sensor guessing. The listener is started and stopped from the session state
rather than from each button, because there are eight ways out of a live set and one
forgotten would leave the accelerometer running.

**The ring is the button.** `onTap` existed in `SessionActions` and nothing called
it, so a rep-counted movement could not be counted by hand at all. The ring is now
the tap target, with a line under it saying so, because a separate "add one" control
beside a circle that size is a second thing to aim at while standing up from a chair.

**Two sets of adaptation sentences.** `adapt_*` says it in the present for Today's
card; `next_*` says it in the future for the done screen. Two sets rather than one
because "today" and "next time" are not the same sentence and a screen that says the
wrong one reads as a bug rather than a tense.

**The done screen said nothing on an easy day.** It read only the main movements to
describe the next session, and an easy day has none, so answering "hard" appeared to
change nothing at the exact moment the app most needs to show that it listened. It
now falls back to the first step of whatever the plan is.

**"The last two felt easy" after one session.** The done screen passed the answer
just given as both the last and the one before it. It now reads the session before
this one, so the rule needs two real sessions.

**Weight came off Today.** MASTER_SPEC 6.1 says weight is not on Today and never has
its own tab. The hero and the weigh-in card are gone from Today; the way in sits on
Move until Phase 2 rebuilds that tab. Asking a question moved to Settings, because
the help dot owns the top right corner of every screen now and two question marks on
one screen is one too many.

**The card lists the main movements only.** A six line list of every step including
the warm up and the cool down is a list rather than a card, and nobody reads the
sixth. The bookends are one line under them.

**Screens without a help topic have no help dot.** Five places are written: Today,
the session, Move, Abilities and Settings. A dot that opens onto nothing is worse
than no dot, so the rest get one when their screen is rebuilt in a later phase.

**Two tools that should have existed earlier.** `tools/banned-words.py` reads the
text of every string resource and every literal in the movement library and proves
no banned word is in user-facing copy. It found eight on its first run, all fixed.
Four strings are exempt, each with its reason written in the script: the card whose
whole subject is that there are no calories here, which CONTENT.md ships as written.
`tools/tidy-imports.py` sorts imports the way ktlint wants them, which is the rule
this project has now got wrong three times.

**The talk test card is renamed.** CONTENT.md already recorded that ADDENDUM-03 adds
"test" to the banned list and the card had to change. The title it suggested, "How
hard should it feel?", uses "should", which is also on the list. It is now "How a
good pace feels" and the body no longer contains the phrase. Source and substance
unchanged.

**A driver that reads the screen.** `tools/drive.py` finds what it taps by text and
bounds from `uiautomator dump /dev/tty`, streamed rather than written to the phone's
storage. The first attempt at the timed gate was a script of fixed coordinates and
sleeps; its first tap landed before the app had drawn, was swallowed, and the run
carried on pressing the wrong things while reporting a time that meant nothing.
