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
- Confirm the target device is connected with USB debugging.

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
