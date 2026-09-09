# TEST-ME.md

Steady Health, Phases 1 to 3 of the ADDENDUM-03 plan, on your Pixel.

---

## Install it

The app is already installed on the phone at the current build. To reinstall it from
this folder, in one line:

```
cd "/var/home/Kamsiob/Kamiob Apps/-- Android/Steady Health" && source ./gradle-env.sh && ./gradlew :app:assembleDebug && adb install -r app/build/outputs/apk/debug/app-debug.apk
```

It is an in-place upgrade. It keeps whatever is already in the app.

To start from nothing, which the walkthrough below assumes:

```
adb shell pm clear com.kamsiob.steadyhealth
```

---

## What I need from you

1. **Photograph a real sheet of exercises, and a real letter.** The camera, the text
   reading and the classifier all work on the phone, and I tested them by pointing the
   phone at nothing in particular, where the app correctly said "I'm not sure what this
   is". What I could not test is the case the feature exists for: an actual physio
   sheet, and an actual letter. That is step 16 below and it is the most useful thing
   you can do.

2. **Stand up out of a chair ten times with the phone in your pocket and count.**
   The accelerometer now counts repetitions inside a session, and nobody has ever
   checked its number against a person's. It is the one measurement in the app whose
   accuracy is unverified, and it matters more than it did.

3. **A health tech attorney has to review the HAI-DEF Clinical Use boundary** before
   the report reading of ADDENDUM-03 Part 7 can be turned on. Phase 4 builds it behind
   a flag that stays off until that clears. Nothing is waiting on it right now.

4. **The three translations.** Spanish, Chinese and Arabic are declared and empty. The
   picker hides itself until a translation lands, so nothing is broken; it is Phase 8.

---

## Twenty steps, from a fresh install

Run `adb shell pm clear com.kamsiob.steadyhealth` first. Every step below has been
driven on the phone except where it says otherwise.

1. Open the app. One screen, one button: **Show me**. Nothing to sign, nothing to
   allow, nothing to read.
2. **Most days, how do you get around?** Tap **On my feet**. One tap is the answer and
   the answer is the navigation.
3. **What would you like to be able to do?** Tap a chip, or type in your own words,
   then **Continue**. Your sentence comes back exactly as you gave it.
4. **Here's something you can do right now.** Heel raises, its setup, one button.
   Tap **Start**.
5. **I'm ready**, then the count in. Now put the phone face down on the table and do
   ten heel raises by ear. It will talk you through them. If nothing is happening,
   pick it up and tap the circle once per repetition.
6. On the done screen, use the **plus and minus** beside the number to change it. The
   app says nothing about being corrected. Then answer **Hard**.
7. Read the line under "Next time". It should say about a minute, marching on the
   spot, and that it is lighter because that one was hard. Answer **Easy** and
   **About right** and watch it change each time.
8. **Save**. Then the two optional questions, then **Done**.
9. You are on Today. There is a session card with a length, what is in it, what it
   feeds, and one button. Under it, the week and what the app noticed.
10. Tap the **?** in the top right. Two sentences about the screen, what each thing on
    it means, and three questions people ask. Tap anywhere to close.
11. Tap the small **i** beside "About 8 minutes". One sentence, under the number.
12. Start a session and press **Something hurts**. One press stops it and asks where.
    Pick **Knee**. Back on Today, the session card should now say it left the knee
    movements out this week, and what it feeds should have changed.
13. Start another and press **Make it easier** mid set. The number should come down.
    Then **Skip this one**, then **That's enough for today**. All three keep what you
    did.
14. Start one and press the home button for half a minute, then come back. It should
    be exactly where you left it and say "Back to it".
15. **Lock the screen** mid session with the power button, wait, and unlock. Same
    thing. This is the one I could not test, because I cannot come back through your
    keyguard.
16. Go to **Sessions** and tap **Scan something**. Allow the camera. Photograph a real
    sheet of exercises from a physio if you have one, then **That's all of it**. The
    app should say it looks like exercises and offer to add them. Tap through to the
    confirmation screen: every line is shown as it was written, with what the app made
    of it underneath, and nothing is saved until you say so. **This is the step I most
    want you to try**, because I could only point the phone at a blank wall.
17. Try it again with a letter, and again with something the app should refuse, like a
    prescription or a blood test. It should say it is not something the app reads and
    offer to keep it, and it should not say what kind of document it thought it was.
18. Go to **Sessions**. Today's session at the top, what you have done under it, a way
    to add a session you already did, and the library of everything you can do.
19. Tap a session in the history. The numbers are editable and there is a
    **Remove this session**. Nothing asks why.
20. From Today's card, tap **Do it without the phone**. It writes the whole session
    out and will read it aloud. Then **I have done it** and log what you managed.
21. Go to **Progress**. Four bars, one per week, each against the number of sessions a
    week you chose. Nothing joins them. Under them, one sentence saying a quiet week
    is what most months look like.
22. Go to **You**. The daily prompt switch, how many sessions a week, and everything
    else. Turn the week number to five and go back to Today: the line under the card
    should change.

That is twenty two steps rather than twenty, because scanning a document is where
ADDENDUM-03 Part 22 asks the walkthrough to end and it now exists.

**What a scan does not do yet: explain a letter.** Tapping "Explain it" keeps the page
and returns you to Today. Part 7's reader is Phase 4 and stays behind a flag until an
attorney has reviewed the boundary, which is on the list above. The validator that
guards it is being built first, which is the order Part 7 asks for.

---

## The Phase 1 acceptance gate

Run on the phone on 2026-09-08, against the build before the phone locked. The
scripts are in `tools/` and every one can be run again.

| Gate item | Result |
| --- | --- |
| Install to a finished first session under 90 seconds | **Pass.** 48 seconds, of which 16 was doing the movement. `tools/gate-first-run.py` |
| A full session face down, audio only | **Pass as a rule**, not by ear. A test walks a whole session forward on nothing but the clock and the accelerometer and requires it to finish. Step 5 above is you checking it sounds right. |
| One obvious action per screen, by screenshot | **Pass.** `tools/gate-screens.py`: ready, count in, live, rest and done each have exactly one. |
| The three exits from every point, keeping what was done | **Pass.** Every exit from every stage in `SessionRunnerTest`; each one once on the phone in `tools/gate-exits.py`. |
| The pain button in one press | **Pass.** One press reached "Where does it hurt?", and naming the knee changed the next session on its own. |
| Pause survives an interruption | **Pass for the home button**, which is the same stopped activity a call produces. The screen lock is step 15 above, for you. |
| 200% font scale and TalkBack | **Written, not run.** `SessionAccessibilityTest` asserts both without changing anything on any device. It has not run because the phone is locked. |
| The next session differs after "hard" | **Pass.** "About 8 minutes, starting with stands from a high seat" becomes "About a minute, starting with marching on the spot. A bit lighter than today, because that one was hard." |
| No banned word on any screen | **Pass.** `tools/banned-words.py` reads the text of every string resource and every literal in the movement library. It found eight on its first run, all fixed. |

Three things had to change to make the face-down session true, and they are the
changes I would look at first: the ready screen starts on its own after twenty
seconds, a set the phone is counting ends eight seconds after you stop, and a set with
nothing happening at all ends after ninety seconds having said so aloud first.

---

## The judgment calls I am least sure about

**The ready screen starting on its own.** Twenty seconds on the first movement, ten
after that. Without it a session with the phone face down stops dead at a button
nobody can see. But somebody slow to get to a chair will be talked at while they are
still walking. If it feels rushed, the numbers are `FIRST_READY_SECONDS` and
`READY_SECONDS` in `SessionRunner`.

**A stalled set ending after ninety seconds.** Somebody resting mid set for a minute
and a half has their set ended and recorded at what they had done. I think a session
that cannot end is worse. I am not certain.

**"Make it easier" lowering the ask by a third** when there is no easier movement.
Before this it did nothing at all, which is the app shrugging at somebody who just
said they are struggling. A third is a guess.

**No microphone button on the third onboarding screen.** ADDENDUM-03 asks for one
beside the text field. A microphone of our own means a permission prompt, and the same
screen's rules say no permission requests. The keyboard has a microphone on it. If you
want ours, it is a button and a prompt at the moment it is used.

**Weight came off Today entirely.** MASTER_SPEC 6.1 says it is not on Today and never
has its own tab. Its way in now sits on the Sessions tab, which is a holding place
rather than a decision.

**Numbers inside a live session have no information dot**, though every other number
in the app does. That screen is read from two feet away by somebody standing up out of
a chair.

**The daily prompt turning itself off after eight dismissals.** It is the strongest
thing in the app that happens without being asked for. Settings says why and turning
it back on starts from nothing.

**Logging a past session asks for no numbers.** Somebody logging Tuesday's walk on
Thursday does not remember how many. What is recorded is that it happened.

---

## Where the work stands

Phases 1a, 1b and 2 of ADDENDUM-03 Part 21 are built. Phases 3 to 8 have not started:
the camera and the therapist's plan, report reading behind its flag, the other ways of
getting around in full, passive measures and Progress in full, beyond exercise and the
card, and then languages, export, import and release.

Most of the app from the earlier plan is still here and still works. Some of it, the
Move screen's walk-first shape and the Abilities grid, is superseded by the addendum
and gets rebuilt in a later phase.

HANDOFF.md is current. DECISIONS.md has every judgment call with its reasoning, the
gate results in full, and the BLOCKED list.
