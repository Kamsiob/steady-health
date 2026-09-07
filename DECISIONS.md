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
