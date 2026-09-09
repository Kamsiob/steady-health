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

### Three judgment calls in Part 3 and Part 4

**O3 has a text field and no microphone button of its own.** Part 3 asks for "a large
microphone button and a text field, equal size, side by side". A microphone of our own
means a RECORD_AUDIO permission prompt, and Part 3's own first screen says "no
permission requests". The system keyboard has a microphone on it, which is the one
people already use and which asks for nothing. So the field is the field and talking
is the keyboard's job. If this turns out to be wrong it is a screen with one more
button on it and a permission asked for at the moment it is used, not before.

**O5 says "both of these are optional" once rather than putting Skip beside each
question.** Part 3 asks for a visible skip on all three. The word Skip beside every
question reads as the app expecting to be refused, and "Nothing" is already the
visible skip for the first one. The Done button ends the screen whatever is answered.

**Numbers inside a live session have no info dot.** Part 4's L3 says no number in
this app is ever unexplained, and every number on Today goes through `Explained`. The
count on the live screen does not, because that screen is read from two feet away by
somebody standing up out of a chair, and an information dot beside the largest type in
the app is a second thing to aim at. The session's help sheet says what the two
numbers are instead.

### The Phase 1 acceptance gate, run

ADDENDUM-03 Part 21. Run on the Pixel 8 on 2026-09-08, against the build at the
commit this entry sits in. The scripts are in `tools/` and every one of them can be
run again.

**Install to a finished first session in under 90 seconds: pass, 48 seconds.**
`tools/gate-first-run.py`, which waits for each thing it presses rather than tapping
by coordinate, so the number is the app's time and not the script's. Sixteen of the
48 seconds are doing the movement at a human rate.

**A full session with the phone face down, audio only: pass, as a rule.**
`SessionRunnerTest.awholeSessionRunsToTheEndWithNobodyTouchingTheScreen` walks a
whole session forward on nothing but the clock and the accelerometer, presses
nothing, and requires it to reach the end with what was done recorded. Three
changes were needed to make that true: the ready screen starts on its own, a set the
phone is counting ends once the person stops, and a set with nothing happening at all
ends after ninety seconds having said so aloud first. **What the test cannot check is
whether it sounds right**, which is the first thing to try in TEST-ME.md.

**One obvious action per screen: pass**, by screenshot and by test.
`tools/gate-screens.py` captures each session screen and asserts exactly one primary
action on it; `SessionAccessibilityTest.everySessionScreenHasOneObviousNextAction`
asserts the same thing without a device.

**The three exits from every point, keeping what was done: pass.**
`SessionRunnerTest.everyExitWorksFromEveryPointAndKeepsWhatWasDone` presses each exit
from every stage of the pure machine. On the phone, `tools/gate-exits.py`: "That's
enough for today" kept the one repetition that had been done; "Skip this one" moved
on and recorded the skipped movement as left out; "Make it easier" brought the ask
from five to three.

**The pain button in one press: pass.** One press reached "Where does it hurt?", and
after naming the knee the next session on Today changed from "For Get up, Go, Carry"
to "For Go, Carry" on its own.

**Pause survives an interruption: pass**, for a screen lock and for pressing home,
which is how a phone call reaches the app as well. `tools/gate-interrupted.py`. An
actual incoming call was not simulated: doing that means an emulator, and the code
path is the same stopped activity. It is listed in TEST-ME.md as something to try.

**200 percent font scale and TalkBack: pass**, as tests rather than as a run.
`SessionAccessibilityTest` asserts the count, the primary action and all four exits
are displayed at twice the text size, and that nothing tappable in a session is
without a label, which is what TalkBack reads. Both are checked in the semantics
tree rather than by eye, and neither changes a setting on the owner's phone, which
the standing rules do not allow.

**The next session visibly differs after "hard": pass.** "About 8 minutes, starting
with stands from a high seat" becomes "About a minute, starting with marching on the
spot. A bit lighter than today, because that one was hard."

**No banned word: pass.** `tools/banned-words.py` reads the text of every string
resource and every literal in the movement library. Four exemptions, each with its
reason written in the script.

### BLOCKED: the phone's screen is locked

While running the gate's "pause survives a screen lock", the script pressed the power
button. The phone's keyguard is secured, so it cannot be dismissed from here and I
will not try: typing somebody's PIN is not covered by "installing and testing this one
application". `wm dismiss-keyguard` was tried once, which only works on an insecure
keyguard, and it did not.

**What the owner needs to do: unlock the phone.** Nothing else. No data was touched
and the phone is in the state it sits in every time it is put down.

Until then the thirteen UI instrumented tests cannot run, because a Compose test
launches its own activity and an activity cannot come up over a locked screen. The
seven that do not draw anything still pass. The gate items those thirteen answer are
recorded as run against the build before the phone locked, and the two font-scale
tests were written after it, so they are marked not yet run.

The screen lock is out of `tools/gate-interrupted.py` for good. It now uses the home
button, which is the same stopped activity, needs nothing from the owner, and is how
an incoming call reaches the app as well. Locking the screen mid session is a step in
TEST-ME.md instead, because only the owner can come back through their own keyguard.

### BLOCKED: no emulator on this machine

The emulator was wanted for the two gate items that mean changing a system setting,
and for simulating an incoming call. It creates an AVD and starts, reaches "Emulator
is performing a full startup", and then the process exits with nothing in its log.
Windowed and headless, two GPU modes, a fresh AVD from the installed API 36 image.
`/dev/kvm` is world read-write, so it is not permissions.

Both gate items were answered another way, and better: the font scale and the
semantics tree are asserted in `SessionAccessibilityTest`, which changes nothing on
any device, and the interruption is driven with the home button, which is the same
stopped activity a call produces. Nothing is waiting on the emulator. It is recorded
because the next session should not spend an hour rediscovering it.

### What the Phase 3 review pass found

Three defects, all in code that compiled, passed its own tests, and read well. Worth
recording because all three are the same shape: a confident answer where an honest
uncertain one was required, which is the failure mode this app can least afford.

**A lab report could reach the reader.** The classifier guarded out-of-scope pages by
counting clues, so a half-photographed blood test that fired two report clues escaped
the guard and came back Unclear. Part 5's unclear screen offers "explain it in plain
words" as one of its two buttons, so a blood test was one tap from Part 7. It now
computes what the page looks like first and lets a single out-of-scope clue overrule
anything that would otherwise have been Unclear.

**An ordinary physio sheet could be condemned as an imaging page.** The two-clue bar
pooled across the three out-of-scope families, so one imaging word plus one medicine
word added up to a scan. "Technique" and "as needed for" are ordinary English on a
handout. The bar is now counted within one family, which is strictly more conservative
on real out-of-scope pages and stops condemning real sheets.

**The plan matcher read prohibitions as prescriptions.** "Avoid stairs" came back as
stairs, confidently matched, and "Avoid: deep squats and stairs" was split on the
"and" into two confident items. On a confirmation screen that puts somebody in front
of the exact thing the line was written to keep them away from. This is the worst
outcome available in Part 6 and the reason its restraint is written the way it is.

**One thing the review could not fix, so I did.** The classifier can say which of the
three out-of-scope families it saw, which is useful in a test and in a bug report.
Part 5 gives an out-of-scope page exactly one sentence and that sentence does not name
the kind of document. The scan screen shows what the app saw on every other outcome
and shows nothing on this one: telling somebody the app decided their page was a blood
test is the app saying something about their health.

### The Phase 3 gate

"The therapist plan and the app's suggestions are visibly separate on Today and in
history, verified by screenshot, and a movement in both is counted once."

**Pass**, as `PlanSeparateTest` on the phone rather than as a screenshot somebody
looked at once: both headings are on the card, the plan's movements sit under the
plan's heading and the app's under "Also, if you want more", the sentence about a
movement on both lists is said once and not once per movement, a clash is flagged with
the movement still on the list, and a card with no plan behind it offers no extras at
all because until then the app's suggestions are not extras, they are the session.

Writing that test found a real duplication: the plan's label was drawn twice, as the
eyebrow and as the card's big line. The big line now says how many movements the plan
has. A plan has no length the app can work out, because it does not know how long a
therapist expects any of it to take, and guessing would be the app adding something to
somebody else's plan.

### Phase 3, finished, and what was decided in it

**The camera is CameraX rather than the camera intent.** An intent hands the job to
whatever camera app is installed, and the photograph passes through that app's storage
on the way back. On device and out of anybody else's hands is the whole promise of
Part 5, and an intent breaks it before the app ever sees the picture.

**ML Kit's bundled model, not the one Play Services downloads.** The APK goes from 47
MB to 79.5 MB, which is a real cost for an audience on older phones. The alternative
downloads the model on first use, which breaks the promise that the app works offline
and adds a dependency on Play Services. Carrying it is the better trade for these
people, and the number is recorded because somebody will ask.

**Document pages live in the database, not in files beside it.** "Documents live in
the encrypted store" is then a property of where they are rather than of remembering
to encrypt them, and export and delete reach them without either having to know about
a second place. The cost is a larger database; a page of typed text at JPEG 80 is
small enough that this is not close.

**MedGemma is not offered at all, and the type enforces it.** `ModelRoom.OFFERED_NOW`
holds only the words model, and it is the default, so a caller that forgets to pass
anything cannot turn document reading on by omission. Part 7 puts the feature behind a
flag until an attorney reviews the HAI-DEF boundary, and a flag that defaults to on is
not a flag.

**A plan has no length on its card.** The app does not know how long a therapist
expects any of their plan to take, and putting a number there would be the app adding
something to somebody else's plan. The card says how many movements instead.

**"Report" is exempt from the banned list in four strings.** It is banned as the app's
noun for its own output, and this app never produces one. It is not banned as the name
of the document in somebody's hand, which has the word printed at the top of it, and
Part 5 writes those sentences itself.

**SteadyViewModel keeps its LargeClass suppression, with a date on it.** Four subjects
have moved out of it. What is left is Today and the walk, which share four pieces of
mutable state, and ADDENDUM-03 Part 21 rebuilds the walk in Phase 5. Splitting it now
means untangling that state twice. The suppression carries the reasoning in the file.

### The other three ways into a therapist's plan

**Speaking a plan uses the on-device recogniser or nothing.** Android's ordinary
`SpeechRecognizer` sends the audio to Google's servers, and the app's standing rule is
that everything runs locally and the internet is only ever touched by something the
person switched on. A recording of somebody describing their own therapy is exactly
what that rule is for, and there is no wording that makes sending it acceptable. So
`PlanVoice` only ever calls `createOnDeviceSpeechRecognizer`, gated on API 33 because
that is where `isOnDeviceRecognitionAvailable` arrived and the app will not create a
recogniser it cannot first ask about. Where the phone cannot do it, the screen says one
sentence and offers typing, which is the same screen the typed way in uses. The
fallback that would work is the one that sends somebody's voice to a server, so there
is no fallback, and the microphone is asked for on the tap that needs it and never on a
phone the app has already decided it cannot use.

**A spoken sentence is cut in `PlanMatching`, not before it.** `lines` already cut on
"and" where both halves named a movement on their own, which is exactly Part 6's spoken
example. It now cuts on the comma under the same rule, for "chair stands, heel raises
and a walk", where only the last join is a word. The rule is what makes the comma safe:
"heel raises, 3 sets of 10, twice a day" has pieces that name no movement, so that line
stays whole and its numbers stay attached. Cutting in the speech screen instead would
have been a second vocabulary for the same job, and the scanned sheet would not have
got the fix.

**One chooser in front of the camera, and every caller repointed.** Sessions and You
used to open the camera directly. They open the four ways now, and the camera is behind
the first row, so nothing reaches it without passing the chooser. The camera row is
also the only one of the four that is not about a plan, since Part 5 sends a letter or
an appointment card down the same path, and the line under the title says so rather
than leaving somebody holding a letter to guess.

**The three new screens each hold their own view model, scoped to their own back stack
entry.** Each is one screen with one answer on it and nothing to share, and leaving the
screen is then what lets go of what it was holding, which for the spoken one is the
microphone. The draft they all fill is still `ScanViewModel`'s, because that is what
saves a plan, and one draft is what keeps "nothing is saved unconfirmed" a property of
the app rather than of each way in.

**Picking from the library offers warm ups and cool downs, which Sessions does not.** A
sheet can ask for shoulder rolls or calf stretches, and a list that could not offer them
would send that person back to typing. `TodayCards.library` took a `pieces` argument for
it, defaulted to what every existing caller already got.

### Where the phases stand

Phases 1, 2 and 3 are done, each with its gate run on the phone. 27 instrumented tests
and about 380 unit tests pass.

**Phase 4 is started but not finished.** Part 7 says the validator is built before the
model is wired in, and that was the right place to begin. `ai/Reading.kt` holds the
job 9 output type, closed on purpose so a fifth field cannot turn a reading into an
opinion. `ai/ReadingValidator.kt` and its fixtures were being written when the run
ended. Nothing downloads, nothing reads a document, and "Explain it" keeps the page
and returns to Today, which is an honest outcome rather than a stub.

**Phase 5 is partly done.** Part 15's interruptions and chair height are built, tested
and wired. Exclusions, readiness and pacing already existed from the earlier plan.
Ceilings and variants are in the session engine. The other ways of getting around
work, but have only been walked on the on-feet path.

**Phases 6, 7 and 8 have not started.**

### The wheelchair and bed libraries, and why `ways` lost its default

`Movement.ways` defaulted to on feet and with a walker, which meant a movement was
tagged for somebody standing up unless whoever added it remembered otherwise. Nine
had gone in that way, among them the bridges and every kind of chair stand, where the
default happened to be right, and nothing in the build would have said so if it had
been wrong. The default is gone. The compiler now asks the question of every new
movement, which is the only check that cannot be forgotten.

The two thin libraries were written out rather than borrowed. A movement belongs to a
way when its setup line can be followed exactly as written by somebody who gets around
that way, and where the sentence would have to change it is a second movement with its
own id and its own words. That is why "Hold something, lift one foot, and circle the
ankle" stayed on feet and `warm_seated_ankles` was written beside it, and why bridging
in a bed is `bed_bridge` rather than `glute_bridge` with the floor taken out of it: one
of them needs somebody to get down on the floor first and the other does not.

A wheelchair user's chair is not something the room might be missing, so pressure
relief lifts, transfers and sitting unsupported ask for nothing rather than for a
sturdy chair. The chair question in onboarding is about a chair to stand up from.

The band press now carries the pushing exclusion. LOGIC.md section 5 offers it as the
replacement for the pushing ladder, which is a different question from whether the app
may put a press into a session for somebody who said no pushing or pressing. The
ladder still does what LOGIC.md says; the session does not.

### Three things the four ways found in the session engine

A fortnight of planned sessions for each of the four ways, which is what
`EveryWayTest` runs, found three faults that a single plan could not.

**Steady was never offered.** `chooseMain` took one movement per ability in the order
`AbilityDomain` declares them and stopped at three, and Steady is declared fourth. The
abilities are now taken least recently fed first, so all four come round.

**A session could be sixteen minutes.** The Go movement chosen on the second day was
the ten minute brisk walk, because it was the one that had gone longest without being
done. Movements are now chosen against what the session has room for, worked out
before anything is picked from what the warm up and the cool down leave of eight
minutes.

**Every session opened and closed the same way.** The warm up was the first on the
list and the cool down the first two, every day. Both now rotate on recency like
everything else, which is also why the seated and bed libraries have four cool downs
each rather than the two they could have had.

The cool down threshold moved with them. It was read against the opening, so a session
that came to seven minutes with a cool down on the end was told it was under five and
did not get one. It is now read against the finished session, which is what
ADDENDUM-03 Part 1 means by a session over five minutes.

### The permission nobody typed

An audit of the four ways into a therapist's plan asked one question: does any audio,
text or image leave the phone by any path they opened. Nothing they opened does. The
spoken way builds only `createOnDeviceSpeechRecognizer`, on every path including the
error path and the say it again path, never falls back to the networked recogniser, and
does not lean on `EXTRA_PREFER_OFFLINE` to be true. Nothing anywhere in `app/src` logs
anything: there is not one `Log.` and not one `println` in it.

The merged manifest was another matter. `android.permission.INTERNET` was in the built
app and nobody typed it. `com.google.mlkit:text-recognition` reads a page with a model
that ships inside the APK and needs no network to do it, but it arrives with Google's
usage telemetry transport attached, `transport-backend-cct`, which declares the
permission and sends events of its own through `Transport.send`. So the top of
AndroidManifest.xml said this build cannot open a network connection while the built
app could.

It is taken back out with `tools:node="remove"`, which is one line and reversible,
rather than by cutting the transport out of the dependency graph, which would leave ML
Kit calling a class that is not there on a phone nobody here can try it on.
`PermissionsTest` reads the merged manifest the build produced rather than the one in
src/main, because the one in src/main was never where the problem was. Part 7's
optional reader is the only thing that will ever want the permission, and that is the
day the node goes and it is declared properly.

Two smaller things went with it. The spoken screen let go of its recogniser only when
its entry left the back stack, so walking forward to the confirmation screen left one
constructed and the phone counting the microphone as held; pausing the screen now
destroys it, and a late callback from a recogniser that has been let go is ignored
rather than shown. And Android stops presenting its own microphone question after a
second refusal without telling the app, which left "Allow the microphone" as a button
that did nothing when pressed, so the typing row now sits under that sentence from the
first time the screen is opened.

### The backup is one more file inside the same export zip

Phase 8 asks for export, import, delete, backup and restore. Export and delete were
built in Phase 6 as CSV and PDF in a zip, which is what PRIVACY.md promises in those
words and is not changing. But CSV is for a person: it formats the dates, drops the
ids that join a day to its tags and a plan to its lines, and flattens the tags into a
column. Nothing can be rebuilt from it, and pretending otherwise would produce an
import that quietly loses whatever it could not parse back.

So the backup is a separate file, `backup.json`, written into the same zip beside the
spreadsheets. One artifact: somebody who kept their export can open the spreadsheets
and can put everything back, and did not have to decide in advance which of those they
were going to need. The alternative, a second thing to export and keep somewhere else,
is the one that is not there on the day it is wanted.

The rows in it are the Room entities themselves, serialised by kotlinx.serialization,
rather than a second set of shapes written beside them. A parallel set of row types is
a place for a column to go missing, and the column that goes missing is not noticed
until somebody restores. Reversal: none contemplated; if the zip ever gets too large
for a phone to build in memory, the JSON gets streamed into the zip rather than built
as a string first, and nothing else changes.

PRIVACY.md's sentence lists "spreadsheets, your photos, and a one-page summary". The
zip now also carries the backup. That sentence is still true and is not weakened by
one more file being in there, but it is no longer the whole list, and PRIVACY.md is a
binding document with an effective date on it. Adding the clause is an owner edit, and
it is recorded here rather than made. The app's own copy does say it: the export row
now reads "The same zip holds a backup this app can read back".

### Restore replaces everything; it never merges

Two histories of the same person cannot be joined honestly. The same day can hold two
different sentences, a weight from each, two ratings of the same thing a fortnight
apart. There is no rule that picks between them that is not the app inventing what
somebody meant, and whatever such a rule chose, nobody could check it afterwards,
because the two versions would be mixed and the join would be invisible.

Replacing is the one outcome that can be described in a sentence before it happens,
and a person can decide about a sentence. So the screen says everything here will be
replaced by what is in the file, asks once with "Keep what is here" as the primary
button under the thumb, and then does exactly that. It is one transaction, so the app
is never left holding half of one history and half of another.

### An older backup restores; a newer one is refused

A backup from a later version can hold a column this build has nowhere to put. Reading
it as far as it goes would put back most of somebody's history and drop the rest, and
the dropped part is invisible: they would find out months later by noticing something
missing, if ever. So it is refused by its header, with a sentence saying to update the
app, and nothing is touched.

Older has to work, because that is the whole point of a backup. Room's auto migrations
do not apply: they turn a database on disk into a newer one, and a backup is a file. So
a table added since the file was written comes back empty, and a column added since
comes back as the value a new row would get, which is the default on the property in
the entity. That is not a guess. It is exactly what Room's auto migration put into the
rows that were already on the phone when the column arrived, so a restored row and a
migrated row end up saying the same thing. Today that is one column, `item_ratings`.`sureness`,
which comes back null, meaning the second monthly question was not put, which is
different from a zero and is what `Confidence.wayOf` is built on.

The rule this depends on is that any column added to a table after that table existed
must have a default in Kotlin. `BackupCompletenessTest` checks that against every
schema version Room has written, so the next person to add a column finds out from a
test rather than from somebody's failed restore.

### Forgetting a table is a failing test, because reflection cannot see @Database

This file already says a table that first appears in a later phase "is exactly the kind
that gets left out of the export and is not noticed until somebody tries to restore".
So the backup is designed so that leaving one out is not possible quietly.

Reflection over the `@Database` annotation is the obvious way and it does not work:
Room's annotations are `AnnotationRetention.BINARY`, so they are not in the built code
and nothing at runtime can ask `SteadyDatabase` what entities it has. What is available
is better. Room writes a schema JSON for every version at build time, generated from
that same annotation, and they are checked in. `BackupCompletenessTest` reads the
newest one and holds it against the backup: every table in the database has a list in
the file, every column of every table is in that list, the restore registry covers
exactly those tables, and the schema version written into every backup is the version
the database is at.

Three more things make the rest of the path structural rather than remembered. The
`Backup` class gives none of its thirty-one lists a default, so building one is a
constructor call that does not compile until the new table is passed. Restore walks one
list of tables and nothing else, so a table cannot be emptied and then not refilled.
And the backup reads through its own queries, one `SELECT *` per table, rather than
through the queries the screens use, several of which leave rows out on purpose.

It was tried both ways round before being believed. Adding a dummy entity to
`SteadyDatabase` and bumping the version turned four tests red naming `dummy_things`;
adding it without bumping the version, so Room rewrote the existing schema file in
place, still turned three of them red. Then the dummy entity was removed and everything
went green again.

### Part 6 read end to end, once the four ways in existed

Four ways into a therapist's plan now exist and all four end on one confirmation
screen, which is the arrangement Part 6 asks for. Reading the rest of Part 6 against
what the app actually draws turned up five sentences the specification asks for and
the app either did not say, said in the wrong place, or said too often. A source
reading test now holds the arrangement itself: every way in leaves for the same route,
there is one confirmation screen, a plan is written in exactly one place, and the view
model behind the three ways with no page behind them holds nothing that could write.

**Two plans were one list.** The card read every live line through one flat query and
then took the label off whichever plan came back first, so somebody with a physio plan
and an OT plan was shown both sets of movements under the physio's name. The database
was right all along, since saving inserts a plan rather than replacing one; it was the
reading that merged them. Plans and their lines now come back together, the eyebrow
stops naming one therapist the moment there is a second, and each plan carries its own
name above its own lines. The same bug had two smaller copies: the sentence about a
movement on both lists named the wrong therapist, and the appointment line named the
oldest plan rather than the plan carrying the date.

**The clash was flagged a day late.** Part 6 flags a plan movement that goes against
something the person said they avoid. It was said on Today's card and nowhere else,
which meant that somebody picking wall push ups off the library, having told the app
they avoid pushing, heard nothing until the next morning. It is now said on the
confirmation screen as well, where the sheet is still in their hand. One sentence in
one place feeds both, so the two cannot drift into being two different sentences about
the same thing. Nothing is removed and nothing is blocked: the line stays and the save
button stays, which is Part 6's own instruction.

**"Once" was every time.** "This is your therapist's, not ours" was a permanent block
on the confirmation screen, drawn on every plan anybody ever confirmed. Part 6 says
once. It is now the sand block the rest of the app uses for a sentence it owes
somebody one time, and it is marked read when a plan is saved as well as when the
small dismiss is tapped. Marking it only on the dismiss would have left it standing
for everybody who read it and moved on, which is most people, and that is the
repetition this was meant to end. Getting to a saved plan means having passed it.

The other "once" in Part 6, the line about a movement being on the therapist's list
too, is deliberately not treated the same way. It is one sentence however many
movements overlap, and it is said whenever the overlap is true, because it explains
why a movement somebody expected is missing from the suggestions below it. An
explanation of what is on the screen today has to be on the screen today. The line
about whose plan this is is a fact about the app; that one keeps.

**One tap was three.** "The person can turn the app's extras off entirely, in one tap,
and many will." The only switch was in You, behind a tab, a scroll and a switch, and
the two strings written for the card were unused. The tap is now on the card itself,
under the suggestions it turns off, on Today and on Sessions, writing the same setting
the You tab writes. The row stays when they are off, saying so and offering them back,
because a tap that removes the only way to undo it is a trap.

**Small things.** A label is trimmed on the way in, so "physio " and "physio" are the
same therapist, and a label of nothing but spaces falls back to the plain word rather
than printing "From your" with nothing after it. The flat query that merged the two
plans is gone rather than left sitting there, because its comment said a session was
built from it and nothing builds a session from it.

**Still not built, and named here rather than quietly left.** Part 6 says "when a
therapist's plan exists, IT IS the session ... and it is what Start runs". Start does
not run it. `startTodays` plans through `SessionEngine` from the person's own history
and never opens the plan table, so the card offers the therapist's movements and the
button under it runs the app's own. Nothing else in Part 6 can be finished around
that: what was done against the plan, the tracking, and the export's "what was
prescribed, what was done and when" all rest on the plan having been run. The export
back is the other one: it renders the first live plan only, so a second therapist's
plan is not on the page taken to the appointment.

## Export everything, delete everything, held to the words, September 9 2026

PRIVACY.md makes two promises with strong words in them. "Export everything at any
time from Settings as ordinary files: spreadsheets, your photos, and a one-page
summary." And "Deletion is immediate and complete, and there is no copy anywhere else
to delete." Both were checked against what the code does rather than against what it
says it does. Neither was true.

### The export carried seven tables out of thirty-one

Five sheets: weigh-ins, days, sessions, checks, your-list. Between them they read
`weigh_ins`, `check_ins`, `check_in_tags`, `sessions`, `measure_results`,
`tracked_items` and `item_ratings`. The other twenty-four tables were in the database
and in no spreadsheet: waist, blood pressure, photos, runs and what was done in them,
sore areas, the monthly check's own answer, the Sunday write-ups, the patterns, the
try-it-and-see comparisons, the visit summaries, the scanned documents and their
pages, the plans and their lines, the phrases somebody taught the app, the names they
gave their own steps, where they are on each ladder, the settings, which is where
height and age live, what they asked to leave out, the readiness answers, and the
record of what the app itself said and when.

Nothing was removed to get there. Tables arrived phase by phase and sheets did not,
which is the failure the schema was designed at version 1 to avoid and which happened
anyway, because designing the schema early does not make anybody write the sheet.

There are now twenty-four sheets covering all thirty-one tables, in
`export/EverySheet.kt`. Each one declares the tables it carries beside itself, and
`EverySheetTest` holds that declaration against the schema files Room writes at build
time, the same files `BackupCompletenessTest` uses. Add a table and
`everyTableInTheDatabaseIsInASpreadsheet` fails and names it. Proven by removing `waist`
from one sheet's declaration and watching that test go red on that table.

The sheets are held to table coverage, not column coverage, and that is deliberate.
The backup is checked column by column because it has to reconstruct the database. The
spreadsheets are for a person to read: they format dates, drop the ids that join one
table to another, and leave out `briefJson`, which is the model's own working. Forcing
column coverage would drag all of that back into files somebody has to read.

### The export read through the screens' queries, which drop rows on purpose

`your-list` read `itemsOnce()`, which hides an ability somebody put away. Had a plans
sheet existed it would have read `live()`, which hides a plan they finished with, and
a sore sheet would have read `soreOnce()`, which hides an area that has cleared. Every
one of those omissions is right for the screen it was written for and wrong for an
export, and each is invisible: the file is there, the rows in it are correct, and the
missing ones are missing only to somebody who knew they existed.

Every sheet now reads through the backup's own unfiltered `SELECT *` queries and sorts
in Kotlin. That is the same argument `BackupDaos.kt` already makes, applied to the
other half of the promise.

### "Your photos" was three words the export did not keep

The zip held spreadsheets and a summary. The only photographs this app has are the
pages somebody scanned, and they live in `document_pages` as bytes. They reached the
zip only once `backup.json` existed, as Base64 inside a file only this app reads. A
photograph a person cannot look at is not a photograph.

The pages now go into the zip as the JPEGs they already are, under
`photos/paper-<id>-page-<n>.jpg`, and the paperwork sheet prints that same name in its
`picture_file` column so a row and its picture are found together. The name is built
in one place, `Pictures.nameOf`, because two copies of that rule would drift and the
spreadsheet would point at files that are not there.

Still open, and not ours to close: the `photos` table is the Sunday photograph from
LOGIC.md, it stores a `fileName`, and nothing in the app writes a row or a file yet.
The sheet is there and will be empty until that feature exists. When it is built, the
file it writes has to go into the zip beside these and into the deletion below.

### The eighteen delete calls were doing nothing, and were also incomplete

`deleteEverything` named about eighteen delete calls and then called
`SteadyDatabase.destroy`, which deletes the file. Two things were wrong.

They were not reachable in their effect. Whatever they deleted, the next line deleted
the file that held it. Nothing could observe the difference.

And they were incomplete, in a way that read as complete. Twenty tables of thirty-one.
Waist, blood pressure, photos, documents, document pages, plans, plan items, visit
summaries, experiments, reminders sent and daily prompts had no delete call. Anybody
reading that list would have taken it for the list.

It is now one line, `db.clearAllTables()`, which Room generates from the same
`@Database` the app runs on, so it cannot be short by eleven tables. It is kept rather
than dropped for the case where deleting the files does not complete, and it is
honest about being the second line of defence rather than the mechanism.

The per-table `deleteAll` queries stay on the DAOs. `Daos.kt` says they exist because
"delete everything, immediately, and there is no other copy" is a promise the app has
to be able to keep, and that is still a fair thing for a DAO to be able to do. They
are no longer what keeps it. Anybody adding a table should not go looking for a delete
query to write; the generated call already covers it.

### A previous export was still sitting in the cache after a deletion

This is the one that mattered. Every file this app hands to the share sheet is written
into `cacheDir/shared` first: the export zip, `summary.pdf`, the page for a therapist,
the share card. Deleting everything did not touch any of them, and nothing else in the
app ever would. The system reclaims a cache at a moment nobody can name, and until it
does, somebody who exported in March and deleted in June still had their entire
history on the phone, unencrypted, in plain CSV, in a file they had been told was
gone.

`Erase.emptyOut` now empties the whole cache directory, and the external cache
directory beside it. The whole thing rather than the one folder exports go in, for the
same reason `destroy` deletes every file whose name starts with the database name
rather than a list of suffixes it knows about: a list of places to look is a list
somebody has to remember to add to, and the one it is missing is the one that matters.

The share sheet's own grant needs nothing done to it. `Share.file` grants read access
without `FLAG_GRANT_PERSISTABLE`, so the grant does not outlive the receiving task,
and it points at a file that no longer exists either way.

### The daily job kept running after everything was deleted

Nothing stopped the reminder work. A phone whose owner had deleted everything went on
waking once a day for a job that would open a freshly created empty database. It holds
no health data. What it holds is that this phone was running this app, which is the
kind of thing somebody deleting everything means to be rid of.

`Erase` now cancels all work and prunes it, and awaits both, so the job cannot wake
between there and the database closing.

WorkManager's own database file is left in place. It is a live library holding that
file open, and deleting a running library's database out from under it is how you get
a crash on the next launch rather than a cleaner phone. After the prune it holds no
rows about this person; what remains is a `last_cancel_all_time_ms` and a
`reschedule_needed` flag in WorkManager's own preferences, both of which say only that
the app once cancelled its work.

### Nothing is kept in preferences, and a test says so

There is no SharedPreferences and no DataStore anywhere in this app, which is the only
reason the deletion does not mention one. That is a fact that could stop being true
the first time somebody keeps something small somewhere convenient, and it would
survive a deletion, and nobody would find out. `ErasureTest` walks the source and
fails on the day it is added. The same test pins the two files allowed to name
`filesDir`: `DatabaseKey`, which writes the wrapped passphrase there and deletes it
itself, and `ModelsViewModel`, which reads free space at that path and writes nothing.

### Reversal, and the cost of both changes

The export now reads every scanned page into memory to write the pictures, on top of
the backup already being built as one string. A person with many scanned pages could
make that large. Same reversal as the backup's: stream page by page into the zip
instead of building lists first. It is written here rather than done because the shape
that fixes it fixes both at once and should be done once.

`clearAllTables` deletes every row and then vacuums, which on a large database is
slower than deleting the file it is about to delete anyway. If that shows up as a wait
on the delete screen, the line comes out and `destroy` does the work alone, which is
what it did before.

### What only a device can prove, and what a device test must check

None of the following can run on a laptop. They are written down as the checks a
device test has to make rather than left as things somebody meant to try.

After tapping delete everything, on a phone with a real setup:

1. `getDatabasePath("steady.db").parentFile` holds no file whose name starts with
   `steady.db`. This is the check that found `steady.db.lck`, which a list of known
   suffixes missed, and it is why the deletion matches on the name rather than on a
   list of endings.
2. `DatabaseKey.exists(context)` is false: no Keystore alias and no wrapped passphrase
   file. Run against the test alias, never the owner's key.
3. `cacheDir` is empty, having first run an export, a summary, a therapist page and a
   share card so that all four files exist before the deletion. This is the finding
   above and it is the one worth asserting hardest.
4. `externalCacheDir`, where it exists, is empty.
5. No work is enqueued: `WorkManager.getInstance(context).getWorkInfosForUniqueWork`
   for `steady-reminders` comes back with nothing runnable.
6. `filesDir` holds nothing but what the next launch recreates. Glance keeps the
   widget's own state under `filesDir/datastore`, written by the library rather than
   by anything here, so this assertion is the one that will find a leftover nobody
   named. It holds no health data; the widget's text is the app's own words and the
   length of a session, read live from the database each time it draws.
7. `shared_prefs` holds nothing but WorkManager's own file.
8. The app relaunches into onboarding and the widget, if one is placed, draws the
   empty state rather than yesterday's session.

And for the export, on a device with scanned pages: the zip opens in an ordinary
unzipping tool, every CSV opens in a spreadsheet, `summary.pdf` opens in a PDF reader,
and every `photos/paper-*.jpg` opens in a picture viewer and is the page that was
photographed. The `picture_file` column of `paperwork.csv` names a file that is in the
zip, for every row.

One more, and it is the one a laptop most obviously cannot do: every row of every CSV
has as many columns as its heading. Eight of the twenty-four sheets build a row out of
two pieces, a parent and a child, with a run of blanks where the child is missing, and
a heading that has drifted by one column against the row it labels is a spreadsheet
that is wrong in every row and looks fine. The widths were counted by hand here and
they agree; a device test with a seeded database should count them instead.

The app's own copy was one word short of the truth and now says the pages are in
there: `data_export_why` reads "Spreadsheets, your scanned pages and a one-page
summary". PRIVACY.md itself needs no edit. Its sentence already promises the
photographs; this is the change that makes it true.

One thing this pass did not change. The app never sets `FLAG_SECURE`, so Android keeps
a snapshot of the last screen for the task switcher, and that snapshot is outside the
app's storage and cannot be deleted by it. Setting the flag would also stop somebody
screenshotting their own summary to send to their physio, and stop screen readers and
recording tools that some people rely on. That is a product decision with a real cost
on both sides and it is named here rather than taken quietly.

## The months, and what Progress carries, September 9 2026

ADDENDUM-03 Part 20 lists "the months" on Progress as its own item, beside the four
week view and the look back card. The honest question was whether it is a third thing
or the first two under a longer name.

**Built, and here is why.** The four week view is a fixed window on the present: it
only ever shows the last four weeks and there is no way to move it. The look back card
is one sentence about one comparison. Between them the app could say nothing at all
about a month that is not the current one, and after six months of use that is most of
what somebody has. The v1 grid already designed the answer as screen 15, a picture of
a month with a dot a day, and DESIGN.md says months is one of the screens that carries
over from it unchanged. `MonthOfSessions` in the visit summary was already named and
shaped for exactly that picture, days moved and minutes, so the data existed and only
the screen did not.

**What keeps it from becoming a trend.** One month on the screen at a time. No line
across months, no total over them, no sentence comparing one to the next, and no
arithmetic that reads two months at once. The two buttons choose which month is shown
and carry nothing from the one before it. Months with nothing in them are not in the
list at all, so nobody is handed a page of empty circles with their own name on it. In
numbers-off mode the two figures go and the picture is the whole screen; a direction
word in their place would have to be a direction against the month before, which is
the one thing this screen may not say.

**What the picture counts.** A session long enough to be one, the same threshold the
visit summary counts by, so a dot here and a day in that document mean the same thing.
The year is printed beside the month name only when it is not this year, because two
Augusts a year apart are two different pictures and a screen calling both of them
August is one somebody can be reading the wrong one of without knowing.

### Weight's way in

MASTER_SPEC 6.1 keeps weight off Today and out of the tabs, and TEST-ME.md recorded
its old way in on the Sessions tab as "a holding place rather than a decision". The
Sessions tab was rebuilt for Part 20 and the row went with it, which left the weigh-in
screen with no way in at all while the setting that turns it on was still on You. Part
20 says Progress carries it when it is on, so it is two rows low on Progress behind
`weighsIn`: the page, and weighing in. They are two rows rather than one because
writing a number down and reading the line back are different errands and a daily one
should not be two taps inside a page.

One thing this did not change: the weight page is still reachable from an ability
page, where it is offered whether or not weighing in is switched on. That row predates
the setting and is left alone here rather than changed in a file this pass did not own.

### Every gate, run again at the end, and what running them found

ADDENDUM-03 Part 21 sets two gates. Both pass. Run on the Pixel 8 on 2026-09-09
against the build at the commit this entry sits in, with `tools/all-gates.sh`.

| Gate | Result |
| --- | --- |
| Unit tests | 653, no failures |
| detekt, lint, banned words | clean |
| Release build with R8 and resource shrinking | builds, 33.3 MB |
| Instrumented tests on the phone | 43, no failures |
| Phase 1: install to a finished first session under 90 seconds | 48.5 seconds |
| Phase 1: one obvious action per screen | ready, count in, live, rest and done each have exactly one |
| Phase 1: the three exits, keeping what was done | pass |
| Phase 1: pause survives an interruption | pass at the home button |
| Phase 3: the plan and the app's suggestions visibly separate | pass, `PlanSeparateTest` |
| Phase 4: thirty fixtures and every adversarial case | pass, and the corpus is fifty five |

**The gates that were run for the first time found three things, and all three were
in the harness rather than in the app.** That is worth recording as its own finding:
a new gate is a piece of code nobody has run, and the first thing it measures is
itself.

**A gate counted what fits on a Pixel.** The four ways gate counted library rows off
a screen read, which is the top of the list. Thirteen for somebody who can see fifty.
It now scans to the bottom, and it counts movement names read out of `Movements.kt`
itself, so it cannot drift from the app it is checking.

**A gate believed an empty screen.** `uiautomator dump` comes back with nothing when
the window is mid-transition, and for no reason at all. Believing it makes a gate
report that the app has gone blank. One retry, and a real empty screen is still a
finding.

**A gate read only what was written.** The interruption gate came back INCONCLUSIVE
saying the count was never read, and it was right: the count on the live screen is
drawn as digits and spoken as a sentence, wrapped in `clearAndSetSemantics`, so there
is no text to read. The driver now reads what TalkBack reads.

And one thing that was not the harness: `onboard` cleared the app and launched it in
the same breath. `pm clear` returns before the process is gone, so one run in three
came up on the screen the old process was on. Failing one time in three is worse than
failing every time, because it looks like a bug in the app.

**And one thing the numbers-off gate found that was the app.** With the switch off,
Today still said "One this week. 2 more makes 3." and "Day 1." Both are counts of what
somebody did, reported back to them, which is the exact shape of figure the setting
exists to replace. Both are words now. Four more things it flagged were the rule not
saying what it meant, and each is now written into the gate with its reason: how long
a session takes and how long the daily question takes are the size of the job in front
of you rather than a measure of you; three sessions a week and two reminders left are
a choice somebody made and a ceiling the app keeps, and hiding either makes a setting
unusable; a date is allowed wherever it appears, including inside a sentence, which is
how the summary names the window it was written from; and a day named by how long ago
it was is a date said the way people say it.

The gate is written against a line that is argued at length in `NumbersOff.kt`, and
the argument is the useful part: a number the app reports back becomes a word, and a
number that is part of doing something right now stays. The gate prints the screens it
deliberately does not walk, with the reason for each, so that the list is a decision
rather than an omission.

## Part 20 read back clause by clause, September 9 2026

Every item of ADDENDUM-03 Part 20 was checked against the code that draws it rather
than against the report that said it was there. Thirty-six of the forty clauses hold.
Three of the four that do not are on the Sessions tab and one is the help system.
What follows is what was changed here and what was deliberately left.

### Progress said the first check was a month away, forever

`waiting` was passed as a literal `true`. The sentence under the list, "Your first
check is after a month. Until then this is what you told the app you wanted", is true
on day one and false the day after the first check, and it was being drawn for the
life of the app. It is now `results.isEmpty()`, which is the same question asked of
the checks themselves.

It also moved. It sat nine blocks below the list it is about, under the four week
bars, where it read as a sentence about the weeks rather than about rows with nothing
yet beside them. An explanation belongs against the thing it explains.

### Progress offered weight to somebody in bed

Today asks two things before it shows anything about weight: whether this way of
getting around weighs in at all, and whether the person has it switched on. Progress
asked only the second, so the bed version of the app, which turns weighing off because
a daily weight is not part of that picture, still carried both weight rows. Progress
now asks the same two questions in the same order.

Changing how you get around no longer switches weighing back **on**, either. It could
turn it off, which is right, and it also turned it on when somebody moved back to on
feet, which quietly undid a switch they had set themselves. A way of getting around
can take weighing away and can never hand it back.

### What was found and not fixed here, and why

Four things are true findings in files this pass did not own, and each is written out
in full in the handover rather than half-changed here:

- **Sessions has no "Change it".** `session_change` exists in `session.xml` and
  nothing uses it. Part 20 and DESIGN.md 5b both put it beside Start on the offer, and
  there is no screen behind it: the three things that do change a session today are
  the extras link, "Got a minute?" and doing it without the phone, all on the card
  already. It needs a decision about what a fourth one would do, not a row.
- **The library has no "Try this now"** and `sessions_try_this` is likewise unused.
  Tapping a row starts that movement immediately, while the sentence above the list
  says "tap any of these to read it, or start it on its own", and there is no page to
  read. The promise and the behaviour have to be made the same, either way round.
- **Sessions is in the wrong order**: the card, then history, then the scan and the
  log rows, then the library last. Part 20 puts the library directly under the card
  and history below it, which is the same argument Progress follows: the things
  somebody might do come before the things they have done, and history grows forever
  while the library does not.
- **The weight page is still offered from an ability page** whether or not weighing in
  is on, and the setting itself still defaults to **on** while Part 18 and MASTER_SPEC
  6.1 both say off.

### The help dot, which is one job and not thirteen

DESIGN.md 4b puts a dot on every screen and a sand block on every screen's first open.
Six screens of about sixty pass a `Place` to `SteadyScreen`, and those six are the
four tabs, the rest screen inside a session, and the dead Move screen. Every other
screen explains itself with a permanent paragraph instead of a dismissible one, except
the count in and the live set, which explain nothing and should not: they are the two
screens somebody is looking at while standing up out of a chair.

Six more topics were not written here. "The same place, the same size, always" is the
whole of what L2 promises, and a dot on eleven screens of sixty keeps that promise
less well than a dot on four tabs does. It is one pass over every screen, with the
copy written for each, or it is nothing.
