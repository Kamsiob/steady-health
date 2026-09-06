# Kamsiob Project Template for Claude Code

A standardized starting point for handing any new app to Claude Code. Part A is universal and applies to every project. Part B is the master build prompt for Linux desktop apps. Part C is the master build prompt for Android apps. Part D is the pre-flight checklist.

To start a project: copy Part A plus the relevant platform prompt, fill in the bracketed placeholders, delete anything that does not apply, and add the project's own feature specification. Everything here is a default, not a cage. Change what the project needs changed, but change it deliberately.

---

## PART A: UNIVERSAL STANDARDS

### A1. Files Claude Code must create

**MASTER_SPEC.md**: the build instruction and functional specification. What the app is, who it is for, every feature and how it should behave, the phase plan, the testing requirements, and all standing rules. This is the document a stranger could build the app from. It is a living document, updated with every commit.

**DESIGN.md**: the binding visual and voice specification. Color tokens for every theme, typography, spacing, shape, motion system with timings and easing, component behavior, screen-by-screen layout, copy voice rules, final user-facing copy for onboarding and help content, and the accessibility floor. Where code and this document disagree, this document wins. Also a living document.

**HANDOFF.md**: the resume document, kept current at all times so that a session with no memory of any previous conversation can pick up cleanly after a disconnection, a crash, a context exhaustion, or a gap of weeks. It holds: exactly where the work stands right now and what the next concrete step is; everything tried that did not work and why, with whether it is worth revisiting; every measurement taken with real numbers; everything learned about the environment, toolchain, or hardware that is not obvious from the code; every decision and its reasoning, especially counterintuitive ones; a complete item by item inventory of remaining work across every task document, each marked verified, unverified, partial, not started, skipped, or blocked, with partial items described precisely enough to resume mid-task; the recommended order and any dependencies; anything deferred and what would un-defer it; anything done in code but not verified on the target device; the real state of the issue tracker as distinct from what its labels claim; anything with an external dependency or waiting on the owner; anything the owner asked for verbally that is not yet in a specification document; and every open question or unverified assumption.

Structure it so the state of play, the next step, and the remaining work inventory sit at the top, with the longer historical record below, and prune superseded detail rather than letting it grow without bound. A handoff that must be read in full every session becomes more expensive the longer the project runs, which is backwards.

**DECISIONS.md**: the running log. Every judgment call made without asking, every tradeoff, every measured figure, every deviation from the spec and why, every discovered constraint, and a BLOCKED section listing anything only the owner can resolve. This is what makes autonomous work auditable.

**README.md**: the public face of the repository. What the app is in the positioning language from the spec, real in-app screenshots, feature list, honest limits, build and install instructions, and license. Also carries the approach and methodology section, including the How this is built disclosure. Never marketing copy that overclaims. A living document, held to the same standard as the specifications rather than treated as marketing updated occasionally.

**ARCHITECTURE.md**: how the software is put together, for someone who wants to understand or modify it. Major components and their responsibilities, how significant subsystems are integrated, how data is stored and protected, the threading and lifecycle model, and where the real constraints come from. Updated whenever the structure changes.

**CONTRIBUTING.md**: how to report an issue, how to propose a change, how to set up a development environment, the coding and commit conventions in use, testing expectations, and what will and will not be accepted. Honest that it is maintained by one person. Notes in a sentence or two that the implementation is written by a coding agent working from the specifications in the repository, so a contributor understands why those documents are authoritative.

**SECURITY.md**: how to report a vulnerability privately and what to expect afterwards. Non negotiable for anything handling personal data or making privacy claims, where its absence is a visible inconsistency.

**CODE_OF_CONDUCT.md**: a standard text rather than a bespoke one.

**.github/ISSUE_TEMPLATE/**: templates for a bug report and for a feature or change, each prompting for the fields issues are required to carry.

**.github/PULL_REQUEST_TEMPLATE.md**: prompting for what changed, which issue it closes, how it was tested, and on what device or platform.

**.github/workflows/**: continuous integration running on every push and pull request, plus the release workflow including artefact provenance where the project distributes a build.

**.github/FUNDING.yml**: pointing at the support link, which places a native sponsor button on the repository.

**PRIVACY.md**: the privacy policy, mirroring the canonical hosted version word for word if one exists, with the same effective date.

**LICENSE**: AGPL-3.0, full text.

**.gitignore**: written in the first commit, before anything sensitive can exist. Covers credentials, keys, build artifacts, local exports, and editor junk.

**CHANGELOG.md**: user-facing release notes per version, written in plain language rather than commit messages.

**tools/**: any pipelines, generators, or scripts the project needs, with their own short README explaining one-command usage.

Android projects additionally create **store-assets/** for listing images and **LAUNCH.md** for the owner's remaining manual store steps.

### A1b. What must be kept continuously current

The following are living artefacts. Keeping each accurate is part of the definition of done for every change, not periodic cleanup. With every commit, ask which of these the change made wrong, and correct them in the same commit rather than noting it for later.

**In the repository:**

MASTER_SPEC.md, describing the app as it now is and as it is intended to be, with superseded instructions corrected in place rather than left beside their replacements, and anything pending marked pending rather than described as built.

DESIGN.md, matching what was actually built, including any token, component, or layout decision made during implementation.

HANDOFF.md, current to within one increment at all times, updated at every commit, before any pause, when available context starts running low rather than after, when anything fails while the details are fresh, and whenever a decision is made that a future session might reverse.

DECISIONS.md, with every judgment call, tradeoff, measured figure, deviation, discovered constraint, and researched finding recorded as it happens, plus a BLOCKED section listing anything only the owner can resolve.

README.md, including the capability and limitation lists, the install and build instructions, the badges, the approach and methodology section, and the screenshots.

ARCHITECTURE.md, whenever the structure or a significant integration changes.

CONTRIBUTING.md, whenever conventions, tooling, or expectations change.

CHANGELOG.md, per release, in plain user facing language.

PRIVACY.md, matching the canonical hosted version word for word, with the same effective date.

Screenshots, recaptured in the same pass as any material change to a screen, and recaptured in full before any release.

**On GitHub:**

The repository description, topic tags, About section, and website field, describing what the app currently is.

The issue tracker, with issues opened at the moment of discovery, working notes added as progress happens, and issues closed only when verified on the target device.

The project board, with status derived from issue state by automation where possible and Platform, Area, Priority, and Size set by hand, work in progress genuinely limited, and every blocked item naming its blocker.

The current milestone, with membership accurate as scope moves.

Board status updates, posted at meaningful points.

The pinned roadmap issue, including deliberate exclusions.

**The rule underneath the list.** Anything on it that is allowed to go stale is worse than not having it, because it makes a claim that is checkably wrong rather than merely absent. If any artefact here genuinely cannot be maintained for a given project, remove it deliberately and record why in DECISIONS.md rather than leaving it to decay.

### A2. Values and non-negotiables

These apply to every app and are not subject to reinterpretation.

Zero data collection by design. Everything runs locally. No tracking, no analytics beyond unavoidable platform defaults such as store install counts.

No paywalls, no subscriptions, no user accounts, no logins, no ads.

Internet access only for explicitly opt-in features, and only for those features. No hosted backend, no database with recurring costs.

Monetization is an optional donate link only. The visible label is always "Support this work". Never coffee or caffeine cliches, never framing that anchors support to small amounts, never anything that reads as begging or pressure. The canonical framing: built and carried by one person; if software made this way matters to you, there is a place to stand behind it; either way, it is yours.

Licensed AGPL-3.0. Any bundled content carries its own license alongside, correctly attributed.

Honest limits are a feature. State what the app cannot do, plainly, in the interface and in all public copy. Never overclaim capability or compare favorably to products the app cannot match.

Legal and terms of service compliance, applied as a filter before recommending rather than a caveat afterward. Only build or propose approaches that are fully legal and compliant with the terms of service of every platform involved, including app stores, hosting providers, APIs, model licenses, and content sources. Never propose something that violates a platform's terms or sits in a legal grey area, and never present such an approach as the main path with the legal problem noted afterward as a tradeoff to weigh. If a feature can only work through a non-compliant route, say so plainly and rule the feature out as specified rather than architecting around the restriction. This matters practically as well as on principle: the owner ships publicly under a registered legal entity with a store developer account and a public open source presence, so compliance risk carries real consequences.

No dark patterns. No engagement mechanics, streaks, badges, nagging notifications, or anything engineered to pull the user back. Deleting your own data is made easy, not tedious.

### A2b. Naming, and who is building this

Every app is named "[App Name] by Kamsiob". Use that full form in the About screen, the README title, store listings, and anywhere the app introduces itself. The short name alone is fine inside the interface where context is obvious.

The owner does not write code and does not intend to learn. Everything must be achievable end to end by Claude Code: setup, build, test, fix, release, and maintenance. Never leave a step that requires the owner to write, debug, or understand code in order to finish. If a proposed feature cannot realistically be built and maintained this way, say so plainly before starting rather than producing something half-built that only a programmer could rescue. The only external service the owner uses directly is GitHub, and even that should be reduced to occasional simple commands.

### A3. Process rules

**Autonomy.** This is an unattended run. Execute every phase in order without pausing for approval. When a judgment call arises, decide it, prefer the simpler and more reversible option, log it in DECISIONS.md, and continue. Never stop to ask a question. If something genuinely requires the owner, such as a login only he can complete or a physical action, record it under BLOCKED in DECISIONS.md with exactly what he needs to do, skip it, and keep building everything that does not depend on it. Summarize the BLOCKED list at the end.

**Do not end a turn while work remains.** The most common failure in a long unattended run is not refusing to work, it is ending a turn because a task felt complete. Treat finishing any item as the trigger to immediately begin the next one in the same turn. Do not write a summary and stop, do not report progress and wait, and do not ask what to do next, whether to continue, or which option is preferred. Decide, log it, and keep going. If unsure what comes next, consult the remaining work inventory in HANDOFF.md and take the next item.

When the owner sets a duration or a deadline for a run, check the real system clock with a date command after completing each item rather than estimating elapsed time, since there is no reliable internal sense of how long has passed. If time remains, start the next item immediately.

**If build work runs out before the time does, spend the remainder on user acceptance testing.** Do not stop early. Drive the finished application on the target device the way a real person would rather than the way its automated tests do: complete multi-step journeys end to end across feature boundaries, in every theme, at the largest accessibility font sizes, with a screen reader, offline and online, on a fresh install and on an upgraded install carrying existing data. Then try deliberately to break it with interruptions, process death during every long operation, no network, no storage space, denied permissions, rotation, backgrounding mid-operation, corrupt and truncated files, rapid repeated input, very long inputs, and low memory. Confirm that every promise the application makes to its users is actually true in the built software. Fix what is found, add a regression test for each fix, and open issues for anything that cannot be fixed immediately. Finding no problems is not a reason to stop; look harder in the areas tested least.

**Context discipline.** Do not load whole documents into context. Read HANDOFF.md in full once at the start of a session, since that is its purpose, then search the other documents for the sections relevant to the item being worked on and consult them again when moving to a different area. Scan issue titles and states rather than reading every comment, and open an individual issue only when about to work on it. Loading entire specifications repeatedly is the most common cause of a session exhausting its context and stopping mid-run.

**Commits.** Commit and push tested increments continuously, and always at phase boundaries, so progress is checkpointed and any interrupted session can resume cleanly.

**Living documents.** With every commit, update MASTER_SPEC.md, DESIGN.md, HANDOFF.md, README.md, and any other spec or design document so they always describe the app as it currently is and as it is intended to be. Superseded instructions are corrected in place, never left beside their replacements. Anything still pending is marked pending rather than described as built. This is part of the definition of done for every change, not periodic cleanup. Two reasons: a lost or disconnected session can resume from exactly where it left off, and because the project is open source, anyone forking it gets the current complete build path rather than a spec describing a version that no longer exists.

**Handoff discipline, so it never has to be requested.** The owner should never have to ask for a handoff to be written, because the repository should already be resumable at any moment. Update HANDOFF.md and commit it whenever any item is finished or partially finished, at every commit, before any pause or handback, when available context starts running low rather than after, when a session appears to be ending, whenever something fails or is rejected while the details are still fresh, and whenever a decision is made that a future session might reverse. If a session ends unexpectedly, the last committed HANDOFF.md is what the next one inherits, so it must never be more than one increment out of date. Keep it accurate rather than optimistic; overstating completion is worse than admitting something is half-finished, because the next session builds on top of it and the error compounds.

**Precedence statement.** MASTER_SPEC.md carries a short statement at the top establishing that it, DESIGN.md, DECISIONS.md, and the open GitHub issues are the current source of truth, and that anything in older prompts or earlier conversation which conflicts with them is superseded.

**GitHub Issues, used properly, the way a working developer would.** Open an issue for every bug, feature, and enhancement, including ones discovered rather than reported. Label and categorize them sensibly, for example bug, enhancement, design, blocked. Keep real working notes on each issue as progress is made, not only a closing comment. Reference issue numbers in commit messages so commits and issues link together. Close an issue only when the work is genuinely finished and verified on the target device or machine. Open issues are the authoritative record of what remains, readable by any future session or outside contributor.

**Versioning.** Choose version numbers using semantic versioning: bug fixes bump the third number, backward-compatible features the second, breaking changes the first. State the chosen number and the reasoning in one line. The owner does not track version numbers.

**Secrets never enter the repository.** The repository is public. Credentials, service account keys, and signing keystores live outside the repository or in a directory covered by .gitignore written in the first commit, and never appear in any commit, log, or document. If a credential file is found near the project at the start of a run, move it somewhere protected, reference it by path, and record where it went.

**Verify versions at build time.** Do not trust library, framework, or model versions named in a prompt as current. Check the projects' actual current releases and recommended integration paths before integrating, and note what was chosen and why.

**One copy only.** Keep exactly one copy of the app on the machine. Delete or overwrite previous builds and test artifacts so only the latest exists.

**Feasibility check.** When a new feature is proposed, assess whether it is realistically achievable in this stack and flag it plainly before building, rather than starting something that cannot be finished well.

### A4. Repository setup

Create the GitHub repository as public under the kamsiob account using the gh CLI, matching the description style, topic tags, and structure conventions of the other kamsiob repositories. Commit every project file including specification and design documents, not only source code. Once the app is visually functional, capture real in-app screenshots automatically for the README, never mockups.

### A4b. GitHub as the project management system

The repository is not just where the code lives. It is the project record, the public face of the work, and increasingly a reference someone may evaluate the owner by. Maintaining it is ongoing work, not a one time setup task.

**How to apply this section.** What follows is the standard model, not a rigid specification. Every project should follow it in substance, and every project may adapt it to its own shape. A tiny single purpose tool does not need the same field structure as a multi platform application; a library with no interface has no screenshots to keep current. Adapt deliberately and record what was adapted and why in DECISIONS.md, so a departure reads as a decision rather than an omission. What must not be adapted away is the underlying discipline: everything traceable, everything closed verifiable, nothing stale, nothing ambiguous, and nothing maintained only for appearance.

**The governing principle: restraint is the standard.** A small number of mechanisms kept immaculately reads as disciplined. A large number half filled in reads as process being performed rather than run, and is worse than having none. Add nothing that will not be kept current, and remove anything that stops being kept current. Before adding any mechanism, ask whether it will be maintained and whether it answers a question somebody actually has. Anything failing either test does not go in.

Never state or imply anywhere in a repository that its tracker, documentation, or process exists to demonstrate anything. It should simply be good.

**Repository metadata, kept current.** Description, topic tags, About section, and website field accurately describing what the project currently is, updated in the same commit as any change that would make them wrong. A social preview image so shared links render properly. Consistent conventions across all repositories.

**The front page is a living document, not marketing.** The README and the repository metadata are the first thing anyone sees and they go stale faster than anything else in the project, because nothing forces them to change when the software does. Keeping them accurate is part of the definition of done for every change.

With every commit, ask whether that change made anything on the front page wrong, and fix it in the same commit. The description, topics, About section, and website field must describe what the app currently is. The README must still correctly answer what this is and who it is for, what it looks like, what it can and cannot do, how to install it, how to build from source, and what licence it carries.

The list of what the app can and cannot do is the part most likely to become a lie, because features get added and limitations removed while nobody revisits the paragraph describing them. Re-read it against reality on every commit that changes capability. This matters more here than in most projects, since honest limits are a stated value and a README overstating what the software does undermines it directly.

The approach and methodology section must describe how the project is actually run now. If a process changes, that section changes. Badges must reflect real state; a badge showing a passing build while the build fails, or a stale version number, is worse than no badge.

**Screenshots, kept current.** Screenshots are the fastest thing in a repository to become wrong, and an out of date screenshot is immediately obvious to a visitor in a way that stale prose is not.

Whenever a screen changes materially, meaning its layout, its controls, its colours, or its copy, recapture the affected screenshots in the same pass as the change. Do not defer this and do not batch it for later, because a later pass never comes and the drift compounds.

Capture from the running software on the target device, never from a mockup or a design file, and only when the application is in the foreground. Capture in every theme the project supports, and keep the same set of screens represented so the README's story stays coherent rather than accumulating whatever happened to be captured most recently.

Store them in a predictable directory with stable filenames so replacing one is mechanical rather than requiring the README to be edited each time, at a sensible file size so the page loads quickly.

Before any release, recapture the full set regardless of what changed, since accumulated small changes will have drifted the whole picture even where no single change seemed to warrant it.

Projects without an interface are exempt.

**Required documentation.** Each of these answers a question a visitor actually has, and a public repository missing them looks unfinished regardless of the code inside.

README.md answering, in order: what this is and who it is for, what it looks like, what it can and cannot do, how to install it, how to build from source, and what licence it carries. Status badges at the top reflecting real state. It also carries an approach and methodology section describing plainly how the project is built and maintained: that it is specified before it is built and the specification is kept current with the code, that decisions are recorded with their reasoning as they are made, that the tracker and board are the authoritative record of state, that work is verified on real hardware before being marked complete, and that a resume document is maintained so the project can be picked up cold. State these as facts. Never use the words rigorous, professional, or best practice, and never congratulate the project on its own discipline; describing the process accurately is the point, and praising it undoes it.

ARCHITECTURE.md explaining how the software is put together for someone who wants to understand or modify it: the major components and their responsibilities, how significant subsystems are integrated, how data is stored and protected, the threading and lifecycle model, and where the real constraints come from. Include a diagram only if it will be kept accurate.

CONTRIBUTING.md covering how to report an issue, how to propose a change, how to set up a development environment, the coding and commit conventions in use, testing expectations, and what will and will not be accepted. Honest that it is maintained by one person and that response times vary.

SECURITY.md stating how to report a vulnerability privately and what to expect. Non negotiable for anything handling personal data or making privacy claims, where its absence is a visible inconsistency.

CODE_OF_CONDUCT.md using a standard text rather than a bespoke one.

Issue templates for a bug report and for a feature or change, each prompting for the fields required below. A pull request template prompting for what changed, which issue it closes, how it was tested, and on what device or platform.

.github/FUNDING.yml pointing at the support link, which places a native sponsor button on the repository. The least intrusive support surface available, and it costs nothing.

**Disclose how the software is built.** Every project built by directing a coding agent states so plainly, as a subsection of the approach and methodology section in the README, titled How this is built. Leaving it unstated is the only version of this that damages credibility; stating it directly costs nothing and answers the question before it is asked.

State the arrangement once, in the opening sentence, and never restate it. Repeating it turns a fact into a nervous disclaimer. Then, in order: what the owner's half of the work consists of, specifically rather than generally; what directing long autonomous runs turned out to require, naming the real failure modes and connecting each element of the documented process to the failure it answers; and what came out of it. End on what the process produced, never on a disclaimer, and never include a line stating that this has not made the owner a programmer, since the opening sentence already establishes it.

Write it flat and factual. No apology, no defensiveness, no argument that the approach is legitimate, since arguing for it implies it needs defending. Equally no overstatement: never architected an AI system, engineered a pipeline, or orchestrated agents at scale, and never leverage, harness, or cutting edge. Never claim multi agent orchestration, agent tooling development, or evaluation infrastructure. Four short paragraphs; anything longer reads as protesting. Add one or two sentences to CONTRIBUTING.md noting that the implementation is written by a coding agent working from the specifications in the repository, so a contributor understands why those documents are authoritative.

**Continuous integration.** The highest value mechanism after issue standards, because it is visible on every commit without anyone looking for it. Run on every push and every pull request: compile the software, compile every test source set including any not built by default, run the test suite, and run static analysis and formatting checks. Compiling every test source set is what enforces the standing rule that suites must actually compile, rather than relying on someone remembering.

Handle known environment failures honestly: either fix the toolchain in the integration environment so the suite genuinely passes, or exclude specific tests with a documented reason. Never configure the pipeline to pass while tests fail, and never leave a permanently red badge, since a badge everyone has learned to ignore is worse than none.

Enable automated dependency updates and security alerts, and act on them rather than letting them accumulate. Enable code scanning where it can be made to pass cleanly. Enable branch protection requiring status checks before merging, but never require review approvals, which one person cannot satisfy.

**How work moves through the repository.** Substantive work goes through a feature branch and a pull request rather than directly to the default branch. A history of direct pushes reads as one person working alone in a notebook; the same work as branches with pull requests referencing their issues, each with a passing integration run and a note on what was tested, reads as engineering practice and produces a better permanent record.

Branch names reference their issue. One logical change per branch. Every pull request references the issue it closes and states what was tested and where. Merge only with integration passing. Squash on merge with a message that reads well in the history. Trivial fixes and documentation touch ups may go directly to the default branch; anything changing behaviour does not. Apply this going forward only, never by rewriting existing history.

**Signed commits.** Enable commit signing, using SSH signing rather than managing a GPG key, so every commit carries a verified marker, and enable vigilant mode on the account so unsigned commits show as unverified rather than passing silently. This is visible on every commit without anyone looking for it, and for any project making security or privacy claims, unsigned commits are an inconsistency someone will notice. Apply going forward only; never rewrite or retroactively sign existing history.

**Commit message convention.** Adopt one consistent format, document it in CONTRIBUTING.md, and follow it without exception: a prefix indicating the kind of change, a short imperative summary, a body where the reasoning is not obvious, and the issue number referenced. The specific convention matters far less than its consistency. A log where every message follows one shape reads as discipline; a log mixing three styles reads as whoever happened to be typing.

**Build provenance on release artefacts.** Where a project distributes a build artefact, generate signed provenance for it in the release workflow, using the platform's keyless attestation so there is no key to manage, tying the artefact to the exact workflow run, commit, and repository that produced it. Document in the README how a user verifies it, in two or three lines, since a provenance chain nobody knows how to check provides nothing.

This is worth real weight for anything distributed outside an app store, where a user installing a package directly can confirm it came from this source and this commit. That is a genuine property rather than a badge. Do not pursue formal supply chain maturity levels, hermetic builds, or reproducible build guarantees; those belong to organisations with different threat models and would be ceremony here.

**Contribution signals.** Add good first issue and help wanted labels and apply them honestly to issues that genuinely fit. On a public repository these are what a prospective contributor looks for, and their absence suggests contributions are not actually wanted.

**Issues as specifications, not reminders.** This is the largest single difference between a tracker that reads as professional and one that does not.

Every issue has a title specific enough to be understood without opening it, describing the problem or outcome rather than the vague area, with phrasing consistent across the tracker. Every issue body states what the current situation is, why it matters or what it blocks, and acceptance criteria in checkable terms defining what done means. Acceptance criteria are load bearing: without them, closing an issue is a judgement call and nobody can verify the claim later.

Bugs state how to reproduce them and on what device or configuration. Design changes reference the relevant section of DESIGN.md rather than restating it. Dependencies between issues are stated and linked so ordering is visible rather than remembered.

Working notes go on the issue as progress happens, not only at closing, recording what was tried, what was found, the current position, and what remains, so any session with no memory can resume from a real position.

**Issue types, not type labels.** Use GitHub's issue type field rather than a label to carry the kind of work, with a small shared set along the lines of bug, feature, task, documentation, and initiative, and one applied to every issue. Types are consistent across repositories, which matters as soon as a project spans more than one, and they feed the project charts so the breakdown of work is derived rather than assembled by hand. Do not also keep a type label; holding the same meaning in two places guarantees they eventually disagree.

Labels then cover only what has no first class field: area, and release blocking status. A small set, consistently applied, no label used once and abandoned, documented in the repository so it stays stable.

**Parent and child issues.** Where a body of work has several genuinely separate parts, make it a parent issue holding the intent and the overall acceptance criteria, with a child issue per part carrying its own. Progress rolls up from children to parent, so the parent's figure is what someone glances at and it must be accurate.

Two limits. Never nest more than two levels; deeper stops being readable and becomes an organisational chart. Never create a parent with a single child, which is overhead with no benefit. Large specification documents in particular should become a parent with children rather than either one enormous issue or a scattering of unconnected ones, since neither of those reads well or shows progress honestly.

Issues are opened at the moment of discovery, whether reported by the owner or found while working. Issue numbers are referenced in commit messages so every commit traces to a reason. Issues are closed only when verified on the target device or platform; closing because code was written is a false record. Anything closed prematurely is reopened and said so, since correcting the record openly is the professional act.

**Project board.** One board with, at minimum, a single select Status field, since unlike labels an item cannot hold two conflicting statuses and so cannot drift into an incoherent state during unattended work. Add fields the project will actually use, which for multi platform work includes a platform field with a value for anything that must stay identical across platforms, and for any project may include area, priority, and a rough size estimate. Where size is recorded, record the actual afterwards too, since an estimate never checked against reality is ceremony and the gap between the two is the only information it produces.

Create only views that answer a question somebody would ask. A view that merely displays items is noise. Typically: a board by status as the default, a hierarchy view so the parent and child breakdown is visible, a table filtered to release blocking work sorted by priority, and a table filtered to blocked items. Where size and actual are recorded, a table comparing them, since that is the only reason to record either.

A roadmap or timeline view only if its dates will genuinely be maintained. A roadmap with stale dates is worse than none, because it makes a claim that is checkably wrong rather than merely absent. Where dates cannot be kept honest, use milestones to express sequencing and skip the timeline.

Configure the platform's built in automation before populating the board, so status derives from issue state rather than being maintained by hand. Status maintained by hand during a long unattended run will go stale; status derived from issue state cannot.

**Charts, kept few.** The board can generate charts from its own fields, which costs nothing to maintain since the data is already being kept current. Create a small number answering real questions: open work by area, showing where effort is concentrated; work by type, showing the balance between fixing and building; and progress against the current milestone, showing whether the release is converging. Stop there. A wall of charts is the same failure as a wall of badges.

**Written status updates.** The board supports posting status updates, and this is the highest value low effort mechanism available, because a periodic written summary is what distinguishes someone running a project from someone tracking tasks.

Post one at each meaningful point: a release shipped, a phase completed, a significant decision made, a direction changed. A few sentences plainly written, covering what moved, what is next, and what is blocked and on what. Never post an update merely to have posted one; a gap during a quiet period is honest, an empty update is not.

Fill in the board's description and README so someone arriving cold learns what it covers, which repositories feed it, and how to read it before they start clicking.

Keep work in progress genuinely limited. One person works on one thing, so more than one or two items in progress means the board is lying about focus. Every blocked item states on its issue exactly what it is waiting on and what would unblock it, since a blocked column of unexplained items is the most common failure in an otherwise decent tracker and is immediately visible to anyone looking.

**Milestones and releases.** A milestone per release with every issue that must be in it assigned, kept accurate as scope moves, and closed when the release actually ships. Release notes derived from the issues the milestone contained rather than written from memory. Tag with semantic versioning, create a release for the tag, and attach the distributable artefact. The chain from issue to milestone to release notes to shipped artefact should be traceable in both directions.

Pin one issue serving as a public roadmap, listing what is planned and what is deliberately not planned, kept current. This is also where deliberate exclusions live publicly, which turns a list of absences into evidence of considered scope.

**The profile.** The account profile is what someone sees first if they look up the owner rather than a specific project, so it carries a README orienting a visitor in a few short paragraphs: what the owner builds, the principles the work is built on, which projects exist and what each one is, and where to find the channel and website. Factual and short. No badge walls, no activity graphs, no metrics, no third person self description, no claims about skills. Pinned repositories chosen to represent the work, strongest first, each with an accurate description and a current README.

**Deliberate exclusions, recorded as decisions rather than gaps.** No wiki, since wikis go stale and duplicate documentation that belongs in the repository where it is versioned with the code. No code owners file. No review requirements one person cannot satisfy.

No iteration or sprint fields. Time boxing has no meaning for one person working continuously, and an iteration field that does not reflect real cadence is the clearest possible example of process being performed rather than run. Where throughput information is wanted, size and actual already provide it.

No story points. No delivery metrics of the kind used to assess engineering organisations, since they measure properties of teams and mean nothing applied to one person. No automated changelog generators producing unreadable output; release notes are derived from the issues in the milestone instead. No software bill of materials or supply chain levelling beyond artefact provenance unless a specific reason arises. No badge collections beyond the few carrying real information. Discussions only if genuinely used, since an empty tab is worse than none.

**The cold read test, run before any release.** Evaluate the repository as a stranger would and fix what fails. Open the project with no context: is it obvious what this is, what state it is in, and what is next? Open five issues at random including old ones: does each state its situation, its significance, and how to verify completion? Trace three recently closed issues to the commits that resolved them: does the trace hold? Does every blocked item name its blocker? Does the milestone percentage match what the resume document says about how close the release is? Do the badges show real state?

Does the hierarchy view show work broken down sensibly rather than as a flat list or an over nested tree, and do parent progress figures match reality? Are the charts derived from fields actually being maintained? Is the most recent status update recent enough to be meaningful given how active the work has been? Do recent commits show as verified?

Read the README as a stranger and check every factual claim in it against the built software, including the capability and limitation lists, the install and build instructions, and every screenshot. Where a claim cannot be verified quickly, that is itself a finding worth recording, since a claim nobody can check is a claim that will eventually be wrong.

Report what failed and what was corrected.

**The standard for all of it.** Every mechanism above must reflect reality while work is happening. A tracker that is wrong during active development is more damaging than no tracker, because decisions get made against it. A tracker that simply stops moving when the project pauses is not wrong; on a public repository it accurately signals the project is not currently being worked on, which is honest and useful. The requirement is correctness during active work, not the appearance of activity during dormancy.

### A5. Design and copy standards

**Before designing or writing anything, research current and past AI-slop tells and identifiers**, in both visual design and language, and deliberately avoid them so the output does not read as AI-generated. Do this research first, not after.

**Never use em dashes** in any user-facing copy, documentation, README, commit message, or store text. Use commas, periods, or colons.

**Copy voice.** Write like a person explaining something to a friend across a table. Plain words, short sentences, contractions welcome. No exclamation points. No hype words. No fear language. If a sentence could appear in a generic tech advertisement, rewrite it. Prefer verifiable claims over slogans. Name tradeoffs instead of hiding them. Buttons say exactly what they do, and an action keeps the same name through its whole flow. Interface labels use plain nouns.

**Motion.** Define exactly two spring personalities: a standard damped spring for everything by default, and an expressive spring with slight overshoot reserved for a small number of signature moments. Define three durations and use them consistently. Respect the system reduced-motion setting everywhere.

**Color discipline.** One accent color. Reserve any secondary accent for a strictly defined set of uses and never let it appear elsewhere. Never use pure black or pure white backgrounds. Color is never the only carrier of meaning.

**Accessibility floor.** Minimum touch targets, visible focus states, complete screen reader labels on every control including gesture-revealed actions, contrast meeting WCAG AA in every theme, dynamic type respected without breaking layouts, and reduced motion respected.

### A6. The About screen, links, and licenses

Every app includes an About screen containing the app mark, the app name, and a version line stating the version, the license (AGPL-3.0), and by Kamsiob. Below that, plain link rows all at equal visual weight, none emphasized over the others:

YouTube: https://youtube.com/@kamsiob
GitHub: https://github.com/kamsiob
Website: https://kamsiob.com
Telegram, the Kamsiob Lab group: https://t.me/+g5LKm9rUnNcxMjk5
Feedback email: hello@kamsiob.com
Privacy policy: the canonical hosted URL for this app, not a duplicate copy
Licenses: a screen listing every bundled open source component and its license, plus any content licenses such as CC BY-SA for bundled reference material

Below the links, the support line and the Support this work button, which is the one place a filled emphasis color is used on this screen. It links to https://buymeacoffee.com/kamsiob and follows the support copy rule from the values section.

The support button also appears at the bottom of the Settings screen, in addition to the About screen.

Every link must be verified working before release. The privacy policy row points at the single canonical hosted version so no second copy can drift out of sync, and PRIVACY.md in the repository must mirror that hosted version word for word with the same effective date.

If the app has a feedback path, it goes to GitHub issues and hello@kamsiob.com only, with no backend, accompanied by a short expectation-setting line explaining that one person builds this, everything gets read, and not everything gets a reply.

### A7. Universal interaction laws

**Nothing processes silently.** Any operation that is not instantaneous shows that something is happening immediately, from the moment it is triggered, not after a delay. This applies to generation, loading, downloading, transcription, synthesis, import, export, and search.

**Anything slow is cancellable.** There is an obvious way to stop it, and cancelling genuinely stops the underlying work rather than hiding the indicator. Work in progress never surfaces a surprise result later on a screen the user has returned to.

**Destructive actions use two confirmation tiers.** Routine single-item deletions get one clear confirmation. Major irreversible loss, such as deleting everything or a bulk delete, gets a two-step confirmation whose second step states plainly that it cannot be undone, with a stronger deliberate gesture for the largest wipes. Build one shared confirmation component rather than a different dialog per screen.

**Bulk selection is first-class.** Anywhere the app lists things that can be deleted, the user can multi-select, select all, and delete the selection in one action. Never force one-at-a-time deletion.

**No invisible walls.** Where a deliberate limit exists, state it before the user encounters it, and give a path forward at the moment they meet it rather than a dead end.

**Every list has an empty state**, written in the app voice as an invitation to the obvious next action.

**Every failure state explains itself** in plain words and offers a way forward. Errors never apologize theatrically and never go vague.

### A7b. Reusable Kamsiob patterns

These solutions are already proven across the owner's apps. Prefer them over inventing new approaches, and adapt rather than replace.

**Versioned content and asset distribution.** When an app needs downloadable content, models, voices, or data packs, use this pattern: a pipeline or script generates versioned files, computes a SHA-256 hash for each, and writes a manifest listing every item with its name, description, version, file size, download URL, and hash. Files and manifest are published as GitHub release assets, which have no bandwidth limit and a 2 GiB per-file cap, so distribution scales to any number of users at zero cost. The app reads the manifest, shows real sizes before download, verifies the hash before installing, and lists installed items in a storage screen with delete controls. Downloads are always user-initiated and described plainly as network use. This same pattern is used for Bearings content updates, Local AI Hub model installs, and Kam AI packs and models.

**Single portable data file.** Use one SQLite file as the entire data store, with the schema designed so an export can write everything to a single portable file that imports cleanly on another machine or device.

**Being considered and Not planned screens.** Include an in-app screen listing candidate features with no dates and no promises, each naming its real constraint in one line, alongside a Not planned list framed as deliberate decisions rather than gaps. Pair it with the feedback expectation-setting line. This turns a roadmap question into a statement of values and prevents repeated requests for things that are deliberate omissions.

**Honest limits as a feature.** Where the app cannot do something well, say so in the interface at the moment it matters, and where possible build a mechanism that turns the limitation into something useful rather than an apology.

### A7c. Data portability, backup, and restore

Every app that stores anything the user would be upset to lose must be able to get that data out and back in, completely and reliably. This section is a requirement, not a feature suggestion.

**The governing rule.** Anything the app can store, the export must contain and the import must restore. No feature is considered finished until it survives a full round trip. This prevents the common drift where a feature added later is quietly missing from backups and nobody notices until someone tries to restore.

**Round trip equality testing.** The test that matters is not whether an import completes without error, it is whether the restored state matches the original exactly. Populate the app with a realistic spread of every data type it holds, including the awkward cases: archived items, pinned state, completion flags, ordering, timestamps, relationships between records, empty and edge-case values, unicode and very long text, and any attached files. Export. Wipe completely. Import. Then assert equality field by field. Most import defects survive a smoke test and are only caught by an equality check.

**Version the format from the first release.** The first export format is version one and the file must say so. Someday a version one file will be imported into a much later version of the app, and without a version marker in the file that migration cannot be written cleanly. This costs nothing at the start and is unfixable later. Every future format change bumps the version and adds a migration path, and old files must keep importing.

**Import must be honest and atomic.** Never silently drop data the importer does not recognise; report it. State plainly what is about to be imported before doing it, and offer a clear merge or replace choice where both make sense. If anything cannot be restored, say exactly what and why rather than reporting success. If an import fails partway, leave the app exactly as it was rather than half-imported. A partially restored state that looks complete is worse than a clean failure.

**Backups are portable and self-contained.** A single file the user can store anywhere, that does not depend on the app's installation, and that can be moved between devices and platforms. Where the data is encrypted at rest, the export is encrypted too, with plain wording that a passphrase cannot be recovered.

**Prompting the user to back up.** Where the storage medium is not fully under the app's control, which is the case for any browser-based app, the app must offer backup rather than assume the user knows to. Make the offer once, at the moment it is genuinely relevant, meaning after the user has accumulated something worth losing rather than at first launch when the app is empty. Explain plainly why it matters, offer to set it up, and provide a real decline. If declined, never ask again.

Keep a quiet, permanent indicator of when the last backup happened somewhere in the interface, so the information is available without ever interrupting. This is a fact on a screen, not a reminder, and it must never nag.

If the user opts in, periodic prompts are appropriate because they asked for them. Trigger them on accumulated unsaved change rather than purely on elapsed time, since reminding someone who has not used the app in weeks is noise that teaches them to dismiss the app's messages. Never use system notifications for this; it is an engagement mechanic and it is banned.

Scale the insistence to the stakes. An app holding ideas can mention it once and move on. An app holding records someone depends on should be considerably more insistent in its one-time offer, because the failure mode is materially worse.

**Automatic backup where the platform allows it.** On platforms that support persistent access to a user-chosen folder, offer genuine automatic backup where the user picks a location once and the app writes there without further interaction. Where the platform does not support it, fall back to a user-initiated export or the system share sheet, and be honest in the interface about which of the two the user is getting rather than implying automation that does not exist.

**Restore must be as easy as backup, and equally tested.** An untested restore path provides a feeling of safety rather than safety. Verify restoring onto a fresh install, onto an install that already has data, onto a different device, and onto a device less capable than the one that produced the file, handling any capability mismatch gracefully rather than failing.

### A8. Testing standards

Testing is continuous, not a final phase.

**Every test suite must actually compile and run.** A suite that does not compile is worse than no suite, because it looks like coverage while providing none. Verify at the start of any session, and after any change to shared interfaces, that every test source set still compiles, including instrumented or device test sets that are not part of the default build and therefore rot silently. Run the full suite regularly rather than only the parts related to current work.

Where a suite has known failures caused by a toolchain or environment mismatch rather than by real defects, do not accept that noise as permanent background. Document the exact command or filter that separates genuine failures from known-environment noise, record it in DECISIONS.md and HANDOFF.md, and use it every time, because real failures hide inside that noise and can go unnoticed for weeks.

Each phase has a testing gate that must pass before moving on: unit tests for logic, integration or instrumented tests for behavior, and a manual pass on the real target device or machine.

Walk complete user journeys end to end, not only individual functions. Multi-step workflows that cross feature boundaries are where real bugs live.

Run a regression sweep of all previous phases after each new phase, because native, schema, and state changes ripple.

Write and run user testing scripts covering at minimum: a first-time user completing setup and core use, a heavy daily user exercising management features, a feature-specific deep path, and a hostile path.

The hostile path must include: no network, no storage space, denied permissions, process death at every stage of every long operation, corrupt and truncated files, rotation and resizing on every screen, rapid repeated input, very long inputs, very long sessions, and low memory conditions.

Every bug fixed gets a regression test so it cannot return.

Treat timeouts and hangs as bugs to diagnose and fix, not reasons to stall.

Before declaring completion, drive the finished app as a real user across the full range of intended use, in every theme, at large font sizes, on a fresh install and on an upgraded install with existing data, and confirm that every promise the app makes to users is true in the built software.

### A9. Closing steps for every session

Commit and push all changes to GitHub. Update the living documents. Update and close the relevant issues. Then handle the build export per the platform section below. End with a short plain summary of what was done, what remains, and anything in BLOCKED.

---

## PART B: MASTER BUILD PROMPT, LINUX DESKTOP APP

Use this for PySide6 desktop applications. Fill in the bracketed sections with the specific project's details, then append the project's full feature specification.

### B1. Prompt opening

You are building [APP NAME] by Kamsiob, a [one sentence description], from scratch to a release-ready state. Read this entire prompt before writing any code. DESIGN.md sits alongside this prompt and is the binding source of truth for all visuals, motion, and copy; commit it unchanged and check every screen against it. Work in the phases defined below, in order, completing each phase's build, testing, and commit steps before starting the next. All universal standards from Part A apply and are restated here as standing rules.

### B2. Platform specifics

Stack: Python with PySide6 for the interface, and a single SQLite file as the only data store. Design the schema so a future export can write the whole thing to one portable file.

The development machine runs Bazzite, an immutable Fedora Atomic system. The /usr filesystem is read-only, so standard installers that use useradd or write to system directories will fail. Install everything into the home directory, a virtual environment, or a container, and record which approach was used and how to invoke it.

Desktop integration: create a proper .desktop launcher file, generate application icons at every standard PNG size, and set the window class correctly so the application groups properly under KDE Plasma on Wayland rather than appearing as a generic window.

Distribution: publish releases as GitHub release assets. There is no bandwidth limit on release downloads, so this scales at zero cost.

### B3. Export workflow

Every session that changes the app ends the same way: commit and push all changes to GitHub, then delete any previously exported build from the desktop and place exactly one fresh current build there, so only the latest exists and GitHub always matches the machine.

### B4. Phase structure

Phase 0, repository and scaffolding. Initialize git, create the public GitHub repository, add the license and .gitignore, commit the specification and design documents, scaffold the project with the design tokens implemented, set up the database layer, and prove the application launches with a smoke test.

Phase 1, the working core. Build the primary feature set to the point where a person could use the app daily. Every screen implemented to DESIGN.md including motion, empty states, and error states.

Phases 2 through N, the remaining feature areas, one coherent area per phase, each with its own testing gate.

Penultimate phase, files, import, and export. Portable single-file export of everything, import with a clear merge-or-replace choice, and round-trip equality verified by automated test.

Final phase, hardening and release. Full self-review against DESIGN.md screen by screen, the complete user testing protocol, accessibility verification, real screenshots for the README, version selection, and the release build published to GitHub releases.

### B5. Screenshots

Once the application is visually functional, launch it and capture real screenshots automatically for the README and any documentation. Never mockups. Capture in every theme the app supports.

---

## PART C: MASTER BUILD PROMPT, ANDROID APP

Use this for Android applications. Fill in the bracketed sections, then append the project's full feature specification.

### C1. Prompt opening

You are building [APP NAME] by Kamsiob, a [one sentence description], from scratch to a release-ready state. Read this entire prompt before writing any code. DESIGN.md sits alongside this prompt and is the binding source of truth for all visuals, motion, and copy; commit it unchanged and check every screen against it. Work in the phases defined below, in order, completing each phase's build, testing, and commit steps before starting the next. All universal standards from Part A apply and are restated here as standing rules.

### C2. Platform specifics

Stack: Kotlin with Jetpack Compose, Material 3 with a fully custom theme implementing the DESIGN.md tokens and bundled fonts, single-activity architecture, and a single SQLite database as the only data store, encrypted at rest with SQLCipher using a key held in the Android Keystore. Design the schema so a future export can write the whole thing to one portable file, and so an existing plaintext database can be migrated into the encrypted one without data loss.

If native code is required, set up the NDK toolchain and a JNI bridge, vendor or fetch the native dependency at a pinned current version, compile for arm64, and prove it works with a smoke test on the connected device before building on top of it.

Minimum and target SDK: verify the current Play Store target API requirement and set accordingly.

Permissions: request the absolute minimum. Audit the merged manifest after every dependency addition, since libraries commonly introduce permissions silently. Every permission in the shipped manifest must be justifiable in one sentence to a user, and that justification belongs in DECISIONS.md.

### C3. Device workflow, and the rules about the phone

A physical device is connected over ADB. Use it for installs, instrumented tests, and screenshots.

Touch nothing else on the phone, ever. No file transfers to phone storage, no deletions, no reads or writes outside installing and testing this one application.

Exactly one copy of the application exists on the device at all times, and it is the current build. Never install a second, parallel, older, or differently named copy for any reason, including as a way to protect existing data during a test. A parallel install cannot receive an in-place upgrade, so it does not exercise the paths that matter, and it violates the one copy rule. Updates to the installed application are always in-place upgrade installs that preserve app data rather than uninstall and reinstall.

Destructive, risky, or data-affecting tests belong on an emulator, not on the owner's device. Schema migrations, data wipes, storage exhaustion, corruption handling, and anything that could damage real data are not device specific and can be verified truthfully on an emulator while leaving the owner's installation untouched. If such a test genuinely cannot run on an emulator, say so and ask before touching the device, and pull the device's application data off as a safety copy first. Never run a destructive test against the owner's real installation on your own initiative.

Screenshot safety: never capture the screen unless this application is in the foreground. Enforce this mechanically in the capture script rather than relying on timing, because a mistimed capture can put the owner's personal content into a public repository.

If the device is not connected, say so, defer device-dependent work, and continue with everything else.

Expect that reinstalling a debug build can silently clear system role selections such as the assistant role. This is a development-only annoyance that does not affect store-delivered updates. Note the restore commands in DECISIONS.md.

### C4. Export workflow

Every session that changes the app ends the same way: commit and push all changes to GitHub. Then delete any previously exported APK from the desktop and place exactly one fresh current APK there. If the device is connected, update the installed application in place with an upgrade install that preserves app data rather than uninstalling first, so no duplicate appears and existing data survives.

### C5. Release and distribution

Signing: generate the release keystore in the final phase, store it outside the repository, and plan for Play App Signing so the local key is an upload key and a lost local key is recoverable. Leave a plain note for the owner about where the keystore lives and that it should be backed up.

Two distribution paths, both maintained on every release. First, Google Play, using the Play service account and the Android Publisher API for everything the API permits, including listing text, Data Safety declarations, and bundle uploads. Second, a signed universal APK published as a GitHub release asset, for people who avoid the Play Store or run de-Googled devices. Release notes must explain in plain language that the two are signed differently, so switching between them requires uninstalling one first, and that the app's own backup and restore carries data across.

Account and automation details. The Play Console account is an organization account under the legal entity B7 Collective, with the public developer name set to Kamsiob, so B7 Collective is only visible if a user opens the about-the-developer view. The organization account was chosen deliberately because it avoids the closed-testing requirement that personal accounts face. Play automation uses the service account kamsiob@kamsiob-503213.iam.gserviceaccount.com, in Google Cloud project kamsiob-503213 with the Android Publisher API enabled, invited with admin rights in the Play Console. Its JSON key is provided with the project and must never be committed. Separately, identity verification for the industry-wide Android developer verification mandate is already completed under B7 Collective, which matters because that mandate applies to sideloaded applications as well as store-delivered ones.

Known Play constraints that cannot be automated: the app entry must be created manually in the Play Console, the very first bundle must be uploaded through the web interface before the API can manage releases, and the content rating questionnaire has no API and is always manual. Do not fight these. Produce LAUNCH.md listing the owner's exact remaining clicks in plain numbered steps, and nothing else, since everything else is done.

### C6. Phase structure

Phase 0, repository, scaffolding, and native smoke test.

Phase 1, the working core, to the point where a person could use the app daily, with every screen implemented to DESIGN.md.

Phases 2 through N, the remaining feature areas, one coherent area per phase.

System integration phase, covering platform surfaces such as share targets, text selection actions, widgets, and quick settings tiles, each with the minimum permissions required.

Files and backup phase, with portable encrypted export and import, and graceful handling of restoring onto a less capable device.

Hardening and release phase: full self-review against DESIGN.md in every theme, the complete user testing protocol on the device and on emulator profiles representing weaker hardware, accessibility verification, real ADB screenshots, store assets generated from the design system, version selection, and the signed bundle.

### C7. On-device model considerations

If the app runs local inference, the following are requirements rather than optimizations.

Build one memory manager that owns every load and unload decision, as the single source of truth for what is resident.

Memory-map model weights rather than reading them into heap memory, and verify this is genuinely happening rather than assuming it.

Track the inference cache separately from the weights, since it grows with session length and is real resident memory.

Respond to memory pressure in two stages: release the cache at moderate pressure while keeping weights mapped, and unload entirely at severe pressure, reloading transparently on next use.

Load lazily on first actual use, never at startup, and never let the interface fail to render because of a model.

Keep only one model resident at a time, and when switching, fully unload before loading so peak memory never doubles.

Guard every load against currently available memory plus a safety margin, refusing with a plain explanation rather than attempting a load that will destabilize the device.

Set tier assignments from measured performance and memory on real hardware, never from file size or specifications.

Speed is a gating requirement. A model that produces good output too slowly is not usable, and no other quality compensates.

The interface must reflect what the active model can actually do, driven by declarative capability metadata rather than hardcoded per model, with capabilities visible before download and explained on tap.

---

## PART D: PRE-FLIGHT CHECKLIST

Before handing anything to Claude Code:

1. The project folder exists and contains MASTER_SPEC.md, DESIGN.md, and any supporting documents such as a privacy policy or credentials the build will need.
2. The bracketed placeholders in the master prompt are filled in and irrelevant sections deleted.
3. The feature specification is complete enough that the first phase is unambiguous, and open questions are marked as open rather than left implicit.
4. For Android, the device has developer options and USB debugging enabled, is connected with a data cable, and is unlocked and nearby for the one-time authorization prompt.
5. Claude Code is launched in the project folder with permissions bypassed for an unattended run.
6. The kickoff message points at the documents by name and says to begin at Phase 0 and run continuously without stopping for approval.

During the run, the owner's only expected involvement is: approving the one device authorization prompt, reviewing anything that lands in BLOCKED, and providing feedback from actually using the app once it is installable. That last one is the highest-value contribution and cannot be replaced by any amount of automated testing.
