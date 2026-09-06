# ARCHITECTURE.md

How Steady Health is put together, for somebody who wants to understand it or
change it.

## The shape of it

One Android application, one activity, Jetpack Compose throughout, and one
encrypted SQLite database. There is no server, no sync, and no second process.

    MainActivity
      SteadyTheme            DESIGN.md as code: palette, type, radii, spacing
        SteadyApp            three tabs and a stack, no other destinations
          screens            Compose, one file per area
            components       the components named in DESIGN.md section 3
          view models        state for a screen, and nothing else
            repositories     the only things that touch the database
              SteadyDatabase Room 3 over SQLCipher
            engine           every rule in LOGIC.md, pure functions
            ai               the five model jobs, prompts and validators

The two directions that matter: a screen never reads the database, and the engine
never reads a clock or a device. Both are about testability, and both are load
bearing rather than tidy.

## The engine is pure, and that is the point

Everything in `engine/` is a pure function. It takes the day it is being run on as
a parameter rather than asking the system for it, so the same inputs always give
the same answer and every rule in `LOGIC.md` can be tested without a device, a
database, or a clock.

This is why the thresholds are named constants rather than numbers in an `if`.
`LOGIC.md` says what each one is and where it came from, and a constant with the
same name is the link between the document and the behaviour.

## The model never decides anything

`AI.md` is the contract and `ai/` implements it. The pattern is the same for all
six jobs: the engine assembles a structured input, the job builds a prompt and a
JSON schema, the model answers, and a deterministic validator checks the answer
before anything reaches a screen. Every job has a path that works with no model at
all, and that path is the one the app is designed around rather than a fallback
bolted on.

The visit summary is the strictest case and the reason the shape exists. The
engine does all selection, arithmetic and thresholding and hands over a brief in
which every fact carries an id; the model writes sentences that must cite those
ids; the validator drops any sentence carrying a number, a date, a quote or a
question it cannot trace back. That validator is built before the model is wired
in, because an invented chair stand count on a page somebody hands to a clinician
is the one failure that actually matters.

## Data

One database, `steady.db`, Room 3 over SQLCipher. The passphrase is 32 random
bytes wrapped with an AES-256-GCM key held in the Android Keystore, StrongBox
where the phone has it. The wrapping key cannot be read out of the Keystore, so a
copy of the database taken off the device is a file nobody can open.

`allowBackup` is false for the same reason: a cloud backup of an encrypted
database without its key is a file that will never open again, and offering one
would be a promise the app cannot keep. Export from Settings is the way data
moves, and it writes ordinary files.

The whole schema is defined at version 1 rather than grown a table per phase, so
that everything the app can store is in the export from the first release. A table
that appears late is exactly the kind that gets left out of a backup and is not
noticed until somebody tries to restore.

Deleting is: close the database, destroy the Keystore key, then delete every file
the database name owns. In that order, because a key left behind for a database
that no longer exists makes "there is no copy anywhere else" false.

## Threading

Compose on the main thread, everything else on `Dispatchers.IO` through Room's
coroutine context. The engine is pure and runs wherever it is called. Model
inference runs off the main thread and nothing waits for it: a screen draws first
and takes the answer when it arrives, which is why a slow or absent model is never
visible as a delay.

## What decides how a screen looks

`DESIGN.md`, and it wins over the code. The palette, the type scale, the radii and
the spacing are in `ui/theme/` as named values with no others, so a screen that
wants a nineteenth size or a fifth radius has misread the document rather than
found a gap.

Two places where the document argues with itself are resolved in code and recorded
in `DECISIONS.md`: the approved palette cannot meet the 4.5:1 contrast floor for
every word, so the palette gives fills and a small set of darker variants give
words; and the primary button is filled with orange-d rather than orange because
white on orange is 2.54:1.

## Testing

Unit tests for every rule, on the machine, with no Android in them. Instrumented
tests for the things a JVM cannot reach: the Keystore, SQLCipher's native library,
and rendering. Fixture sets for every model job, which run without a model because
what they check is the prompt and the validator, and an on-device acceptance run
against the real model for the parts that cannot be checked any other way.
