# Contributing

One person maintains Steady Health. That shapes everything below, so it is worth
saying first rather than in a footnote.

## The most useful thing you can do

Use the app and say what is wrong. Not what is missing, though that is welcome
too: what felt wrong, what you expected to happen, what you read twice, what you
could not find, and what made you close it. That is worth more than any amount of
automated testing and it cannot be produced any other way.

Open an issue, or send it to hello@kamsiob.com.

## How the code gets written

The implementation is written by a coding agent working from the specification
documents in this repository. `MASTER_SPEC.md`, `DESIGN.md`, `LOGIC.md`, `AI.md`,
`ONBOARDING.md`, `CONTENT.md` and `COMPLIANCE.md` are the source of truth, and the
code follows them rather than the other way round.

This matters if you are proposing a change. A change that contradicts one of those
documents is a change to the document first, and the reasoning for the current
version is usually in `DECISIONS.md`. If the reasoning is wrong, that is a real
conversation and worth having. If it is right, the answer to the request is
probably no, and you will get that answer plainly.

## Reporting something

Use the issue templates. A bug report needs what happened, what you expected, how
to make it happen again, and your phone and Android version. A change request
needs what you are trying to do, which is more useful than the feature you have in
mind, because there is often a simpler answer.

## If you want to send code

Open an issue first and say what you are planning. A pull request that arrives
without one may be turned down for a reason you could not have known about, which
wastes your time, and that is on the project rather than on you.

### Setting up

You need the Android SDK and JDK 21.

    source ./gradle-env.sh
    ./gradlew assembleDebug
    ./gradlew testDebugUnitTest
    ./gradlew lintDebug detekt

A device on ADB is needed for the instrumented tests:

    ./gradlew connectedDebugAndroidTest

### What the build enforces

Lint runs with warnings as errors, detekt runs at zero issues, and Kotlin compiles
with all warnings as errors. None of those are advisory.

Three tests will fail a change that breaks a promise the app makes, and they are
worth knowing about before you start:

- `VoiceTest` reads the real strings file and fails on any word from the banned
  list in `DESIGN.md` section 6, on an em dash or an en dash, and on an
  exclamation point.
- `ContrastTest` holds every text and surface pair to 4.5:1, and checks that no
  colour in the palette is red.
- The instrumented database tests check that the file on disk is not readable as
  plain SQLite and that deleting leaves no file and no key.

### Conventions

Commit messages say what changed and why, in plain sentences, with no em dashes.
Reference an issue number. Comments explain why something is the way it is, not
what the line does.

Any document your change makes wrong is corrected in the same commit. That
includes `MASTER_SPEC.md`, `DESIGN.md`, `HANDOFF.md`, `DECISIONS.md` and this
README. A specification that describes a version that no longer exists is worse
than no specification.

## What will not be accepted

Anything that adds an account, a server, analytics, a crash reporter, or an
advertisement. Anything that counts calories or asks what somebody ate. Streaks,
badges, scores, or a target the app sets. Anything that turns red. A combined
capability score. A diagnosis, a condition name, or an inference of one.

These are not oversights. Each one is written down with its reasoning in
`DECISIONS.md` and in `COMPLIANCE.md`.
