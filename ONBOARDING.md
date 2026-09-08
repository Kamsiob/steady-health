# ONBOARDING.md: the first run, and how the app teaches itself

Replaced in full by ADDENDUM-03 Part 3, merged on commit 9837f2a. The earlier
eleven-screen sequence, which asked for units, height, age, capability, exclusions,
readiness, an anchor and a weigh-in before the person had done anything, is gone.

**The rule:** the person does something real before answering anything optional, and
every question is asked at the moment it matters.

The measure of this document is a number: **under sixty seconds from launch to the
start of the first session.** Anything that does not survive that constraint is asked
later or not at all.

## The sequence

### O1. The hook
Full bleed sun and hills hero. Large: "What can you do?" Under it: "This app keeps
track of what your body can do, and helps you do more of it. Everything stays on your
phone." Language pills, showing only the languages this build actually speaks. One
button: "Show me".

No sign up, no email, no account, no permission request, nothing to read.

### O2. One question, four cards
"Most days, how do you get around?" On my feet / With a walker or cane / In a
wheelchair / Mostly in bed or a chair, for now. Under it: "There's a version of this
app for each. Change it any time." Cards at least 64dp tall, one tap, no Continue
button.

### O3. Their words
"What would you like to be able to do?" A large microphone button and a text field,
equal size, side by side.

Six starter chips, and deliberately not all of them maintenance: get off the floor /
stairs without stopping / carry the shopping in one trip / keep up with the grandkids
/ walk further than I do now / get stronger than I am.

Their sentence appears exactly as they said it, then "I'll keep track of this" with
the tracked item, editable. One item is enough. Skipping picks "move more easily" and
the app asks again on day four.

### O4. The first session, which is not a question
"Here's something you can do right now. It takes about two minutes." The movement, its
setup line, one button: "Start".

### O5. After the session, three things
One tap each, a visible Skip on all three.
1. "How did that feel?" easy / about right / hard.
2. "Anything you avoid, or have been told not to do?" Movement chips plus "nothing".
3. "Do you have a sturdy chair without arms?" yes / only with arms / no.

Then Today.

## Asked later, in place, each with one line saying why
- **Day 3:** the anchor time, when the daily prompt would fit.
- **Day 4:** a second tracked item.
- **Week 1**, before any session over five minutes: the readiness flags.
- **Week 2**, before the first measure: rough height, skippable.
- **Week 3:** the photo offer.
- **Week 4:** the confidence rating.

Age is never asked in onboarding. It is asked only if the person taps "show typical
ranges", and it is skippable there.

## Resumable
Quitting mid-onboarding resumes exactly where it stopped. The step reached is stored,
not inferred, so a person who closes the app on O3 and returns a week later lands on
O3 with what they had already typed still there.

## What onboarding never does
Ask for a target weight, ask about calories, ask what you eat, ask about medications,
ask about diagnoses, ask for a photo, request any permission, show a paywall, an
account screen, or an ad. The notification permission is requested the first time a
reminder is turned on, and never before.

## How the app teaches itself afterwards
Three layers, specified in ADDENDUM-03 Part 4 and folded into DESIGN.md section 4b.
Every screen explains itself once in a sand block; every screen has a help dot in the
same place; every number explains itself when its info dot is tapped.

First run of any feature gets one sand block explaining it in a sentence, once.

Today's session card is never ambiguous: it always says what to do, how long it takes,
and has one obvious button. If a person could be lost on Today, that is a design bug
to fix rather than a help topic to write.

## Returning after a gap
ADDENDUM-03 Part 15 replaces the old gap rule. After seven days or more the app asks
one question with four answers and no free text, and adapts by the answer. Gaps of
sixty days or more re-run O2 and O3 with the previous answers filled in. The word
"missed" never appears.
