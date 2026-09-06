# MASTER PROMPT

Paste everything below the line into Claude Code, launched in the project folder with this zip already unzipped there.

---

This project is being reset. The specification in this folder replaces everything that came before it, including any code, documents, issues, or history already present locally or on GitHub. The app was rebuilt around a different organizing idea and the old version is not a starting point.

## First, reset the working tree and the repository

Treat the current contents of this folder and of the GitHub repository as superseded. I authorize you to delete or overwrite anything in either.

Do it in this order, and stop and tell me if any step fails rather than working around it:

1. Read START_HERE.md and this file first so you know what is being kept.
2. Everything that arrived in this zip is kept: MASTER_SPEC.md, DESIGN.md, LOGIC.md, AI.md, ONBOARDING.md, CONTENT.md, COMPLIANCE.md, PRIVACY.md, VISUALS.md, DECISIONS.md, START_HERE.md, this file, and the design/, research/, standards/, and tools/ folders. Nothing else in the folder is kept.
3. Delete every other file and folder in the working directory, including any previous source tree, build output, gradle caches, node modules, and any earlier specification documents. Keep the .git directory for now.
4. On GitHub, get the remote to match this fresh state. Prefer rewriting over deleting: create a fresh initial commit with only the kept files plus the new scaffolding, then force-push it to main so the old tree is replaced. If you have permission to delete branches, tags, releases, and old issues, do so; if you do not, close and relabel what you cannot delete and note it in DECISIONS.md. Do not delete the repository itself, because the URL, stars, and settings are worth keeping. If the remote does not exist yet, create it as public under the kamsiob account with the gh CLI, matching the description style and topic conventions of the other kamsiob repositories.
5. Rename the repository, its description, its About section, and its topics to describe Steady Health as MASTER_SPEC.md describes it. If the repository was previously named for the weight-tracking version, rename it.
6. Write .gitignore in the first commit, before anything sensitive can exist.
7. Record in DECISIONS.md exactly what you deleted, what you could not delete and why, and the commit hash of the fresh initial commit.

If the git history is not yours to rewrite for any reason, say so and stop rather than guessing.

## Then, build

You are building Steady Health by Kamsiob, a free, open-source, local-first Android app that keeps a record of what a person's body can do, and what changes it, from scratch to a release-ready state. Read this entire prompt before writing any code.

Read these in this order, in full, once:

1. MASTER_SPEC.md, the functional specification and phase plan. It carries the precedence statement and explains why the app is organized around capability rather than weight.
2. DESIGN.md, the binding visual and voice specification. Where code and DESIGN.md disagree, DESIGN.md wins. Three reference renders sit beside it. design/screen-grid-v2-capability.html is the current 22-screen grid and the one to build. design/today-approved-look.html is the moment the look was approved. design/screen-grid-v1-weight-reference.html is the earlier weight-first grid, kept only because some screens carry over unchanged; nothing in it is a requirement. Open all three in a browser and study them before building any screen. Screens not in the grid are built from DESIGN.md section 8.
3. LOGIC.md, every rule, threshold, and table the engine decides with. The language model never touches any of it.
4. AI.md, the three visible AI features, the five model jobs with their JSON contracts, the fixed 24-tag vocabulary, and what the model may never do.
5. ONBOARDING.md, the first run screen by screen with final copy, and how the app teaches itself afterward.
6. CONTENT.md and PRIVACY.md, final copy. Ship them as written.
7. COMPLIANCE.md, the legal and policy filter. Apply it before building, not after.
8. VISUALS.md, why version 1 ships with written exercise instructions and no animations, and the pipeline for version 2. Do not build placeholder art.
9. DECISIONS.md, the decisions already made and the BLOCKED list. Append as you work. Never reopen a decision under The reframe, Product, Visible AI, or Design without recording why.
10. standards/kamsiob-project-template.md, Parts A, C, and D. Every universal standard in Part A and every Android rule in Part C applies as a standing rule. The bracketed placeholders are: APP NAME = Steady Health, one sentence description = "a record of what your body can do, and what changes it."
11. standards/session-handoff-prompts.txt, the handoff discipline. HANDOFF.md must exist and be current from the first commit.
12. research/, four evidence reports. These are the evidence behind the specification, for consulting when a rule needs its source or its "why this" text. They are not instructions; where they suggest something the specification does not include, the specification wins.

Create the repository files the template requires: HANDOFF.md, README.md, ARCHITECTURE.md, CONTRIBUTING.md, SECURITY.md, CODE_OF_CONDUCT.md, LICENSE with the full AGPL-3.0 text, CHANGELOG.md, the .github templates and workflows, and store-assets/ and LAUNCH.md when you reach release.

Begin at Phase 0 in MASTER_SPEC.md section 9 and run continuously through Phase 7 without stopping for approval. This is an unattended run. Decide judgment calls yourself, prefer the simpler and more reversible option, log every one in DECISIONS.md, and continue. If something genuinely requires me, record it under BLOCKED with exactly what I need to do, skip it, and keep building everything that does not depend on it. Do not end a turn while work remains. Verify library, framework, and model versions and the current recommended Gemma 4 E4B on-device integration path at build time rather than trusting anything named in these documents. Commit and push tested increments continuously. Keep HANDOFF.md current to within one increment. If build work runs out before the session does, spend the remainder on user acceptance testing on the device as the template describes.

Five rules specific to this project that override any instinct:

1. Every user-facing string passes the friend test and uses no word from the banned list in DESIGN.md section 6. The banned list now includes senior, elderly, frail, frailty, fall risk, decline, sarcopenia, patient, diagnosis, and prescribe.
2. Nothing turns red, counts a streak, sets a numeric target, or produces a combined capability score.
3. The language model never states a number, never writes a life sentence, never decides whether an ability is Better, Same, or Quieter, and never sees the person's weight as a number.
4. Same is reported in the same visual weight as Better. Anything that styles maintenance as lesser is wrong.
5. No exercise animations or placeholder art in version 1. Written instructions only, per VISUALS.md.

A physical Pixel is expected over ADB. If it is not connected, say so once, defer device-dependent work, and continue with everything else.

Start now with the reset, then Phase 0.
