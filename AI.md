# AI.md: what the model does in Steady Health, and what it may not

## The model
Gemma 4 E4B, instruction-tuned, Apache-2.0, released by Google April 2, 2026. Chosen over MedGemma because Steady deliberately avoids medical vocabulary, medications, and lab values, so the medical model buys nothing, and MedGemma's HAI-DEF licence is restrictive while Gemma 4 is Apache-2.0 with no complication inside an AGPLv3 app. E4B supports native audio input on device, which removes the need for a separate speech model. Verify the current recommended Android integration path (LiteRT-LM, MediaPipe, or the AICore preview) at build time; do not trust this document for that.

The model is an optional download, off by default. Everything in the app works without it: without the model, the check-in is typed, tags are chosen from the grid by hand, the Sunday write-up is a fixed template filled from the engine's numbers, and patterns are shown as plain data rows. The download screen states size, whether the phone has the memory, who made it, and its licence, and says plainly: "It reads. It does not diagnose."

All model calls are single-turn. There is no chat. Every call has a fixed input schema and a fixed output schema and returns JSON that either validates or is discarded. The model never sees the person's weight as a number, never sees their age, never sees the exclusions, and never receives any medical term the person may have typed into the free-text line (that line is stored verbatim and shown back, but is not passed to the model except as job 2 input for tagging, where the output can only be tags).

## The three visible features, and where the model sits in each
The model is visible in exactly three places, and it is never a chatbot.

1. Your words become what is tracked (job 1 below). The person speaks or types what they want to be able to do; the model turns it into tracked items with a domain each; the person confirms. This is the most differentiated use and the first thing a new user meets.
2. The phone times and counts the monthly check. This is not the language model at all: it is the accelerometer and MediaPipe. It is listed here because to the person it is the same capability, and because the copy must be honest that it happens on the phone and nothing is recorded.
3. Try it and see. The engine finds the pattern and runs the arithmetic; the model words the offer and the result. The honesty of "no difference" is enforced by the engine, not the model.

## The five jobs

### Job 1: Words to tracked items
Input: { "text": string, "domains": ["get_up","go","carry","steady"], "way_of_getting_around": string }.
Output: { "items": [ { "text": string, "domain": string } ] }, at most four.
Rules: item text is a short, plain restatement in the person's own vocabulary, never clinical. "I want to get down on the floor with my grandson and get back up without it being a whole thing" yields { "Get down to the floor and back up", "get_up" }. The domain must come from the fixed list. The person confirms or edits every item before it is saved; nothing is stored unconfirmed. If the model returns nothing usable, the app shows a hand-written starter list and the person picks.

### Job 1b: Hear
Input: audio of the check-in sentence, on device.
Output: { "text": string }.
Rule: text is shown to the person before anything else happens. If the person edits it, the edited text is what job 2 receives.

### Job 2: Tag
Input: { "text": string, "vocabulary": [24 fixed tags], "person_synonyms": [{"phrase": string, "tag": string}] }.
Output: { "tags": [up to 3 strings from vocabulary], "confidence": [numbers] }.
Rules: constrained decoding or a strict validator so no tag outside the vocabulary can be returned. Tags are shown as "Here's what I picked up" and confirmed by the person. Corrections are stored as person_synonyms and passed back in future calls, so the mapping improves without the vocabulary growing. Adding a new tag is a deliberate act by the person, in settings, and the app first asks whether an existing tag fits.

The vocabulary (24, fixed, reviewed for neutrality; no tag is a judgement):
Food: ate out, cooked, ate light, ate a lot, late night, snacked.
Sleep: slept well, slept badly, short sleep, rested.
Mood: stressed, calm, low, good.
Movement: walked, active, sat a lot.
Body: sore, pain, unwell.
Life: busy, travel, social, family.
"Overate," "binged," "cheated," "bad day," and any moralising word do not exist and cannot be produced.

### Job 3: Say the week
Input: { "days_moved": int, "minutes": int, "tags": [{"tag": string, "count": int}], "day_ratings": [...], "sleep_avg_hours": number, "walk_name": string, "step_offered": bool, "talk_results": [...], "weight_direction": "a little lower" | "about the same" | "a little higher", "what_they_want": string }.
Output: { "paragraphs": [string, string] } totalling at most 80 words.
Rules: second person; describes what happened; never says what the person should have done; never uses a banned word (DESIGN.md section 6); never mentions a number for weight. Post-filter: any sentence containing a restriction tag (ate light, skipped, fasting, late night) in the same sentence as a weight direction is removed; regenerate once, then ship without it. If the person is in numbers-off mode, the input contains no minutes either.

### Job 4: Word an experiment
Input: { "observation": {"variable": string, "measure": string, "direction": string, "split": string}, "stage": "offer" | "result", "numbers": {...} }.
Output: { "headline": string, "detail": string }.
Rules: the engine found the pattern and computed the comparison; the model only words it. The headline for a result must be true to the numbers passed, including when they show no difference. Post-filter: if the numbers show a difference smaller than the measure's detectable change and the headline implies a winner, discard and use the fixed no-difference string.

### Job 4b: Phrase a pattern
Input: { "tag": string, "weeks_with": int, "weeks_without": int, "measure": "sleep" | "days_moved" | "weight_direction", "direction": string, "split": "5 of 7" }.
Output: { "sentence": string, "detail": string }.
Rules: the engine found the pattern; the model only words it. Restriction tags are never passed to this job. Output must contain the split as given.

### Job 5: Pick a card
Input: { "question": string, "cards": [{"id": string, "title": string, "summary": string}] }.
Output: { "card_id": string | null }.
Rules: retrieval only. The model never writes an answer. If no card fits, the app says "There isn't a card for that yet" and offers the closest three titles. The cards are hand-written by the owner and reviewed (see CONTENT in MASTER_SPEC.md); the first set: why the scale jumps overnight; why weight goes flat; does two minutes count; how intermittent fasting works (with its cautions in the card: not for anyone pregnant, breastfeeding, on insulin, or with a history of disordered eating); muscle and weight-loss medications; why the app shows a smoothed number; why there are no calories here; what the talk test is; why walks get longer slowly; what to do after a break.

## Forbidden, in code and in prompts
The model may not: choose or change a walking step; write a life sentence for an ability (the engine picks from hand-written templates); decide whether an ability is Better, Same, or Quieter; word the decline sentence (fixed text in DESIGN.md); compute or state any number; interpret BMI, the waist band, a test result, or a weight change; describe movement as burning or earning anything; connect any restriction tag to a weight direction; produce nutrition claims; mention or reason about medication; produce a diagnosis, a symptom interpretation, or a recommendation to see or not see a clinician (the engine shows those notes, deterministically); hold a conversation.

## Failure handling
Model unavailable, out of memory, or output invalid: fall back silently to the no-model path for that job. Never show an error that blames the person. Log the failure locally for the owner's diagnostics screen. Loading follows the on-device model rules in standards/kamsiob-project-template.md section C7: lazy load, memory-mapped weights, one memory manager, unload under pressure, speed is a gating requirement.

## Testing the model jobs
Each job ships with a fixture set of at least 30 inputs and expected outputs (job 2: sentences and the tags they must and must not produce, including synonyms like "grabbed takeout," "ordered in," "ate at the diner" all mapping to ate out; job 3: weeks that must not yield a restriction-plus-weight sentence). A job that fails its fixtures does not ship; the app runs without it.
