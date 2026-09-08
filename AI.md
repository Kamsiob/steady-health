# AI.md: what the model does in Steady Health, and what it may not

## The models
Replaced by ADDENDUM-03 Part 7, merged on commit 9837f2a. **Two optional models, and
the app is fully usable with neither.**

**Gemma 4 E4B**, instruction-tuned, Apache-2.0. Turns what the person says into what
the app tracks, tags their notes, and writes the weekly note. Verify the current
identifier, size and recommended on-device path at build time.

**MedGemma 1.5 4B**, from Google's Health AI Developer Foundations. Reads and explains
reports and letters from a therapist. **This reverses the earlier decision that
MedGemma is not used**, and the reason it reverses is that the earlier decision was
made for a feature that did not exist: the app avoided medical vocabulary, so a
medical model bought nothing. Reading a document written by a clinician is the
opposite case. The vocabulary is the entire difficulty: gait deviations, ROM in
degrees, MMT grades, transfer independence levels, and clinic-specific abbreviations.
That is the one job where a medical model earns its place.

**Two models, one choice.** Together they are around five gigabytes, which many phones
in this audience cannot spare. In You, one screen, "What the app can read":
- **Nothing extra.** The app works fully. Documents are saved as photos and are
  viewable, and the person types anything they want tracked. This is the default and
  is never presented as lesser.
- **Your own words** (Gemma 4 E4B, about 2.5 GB).
- **Documents from your therapist** (MedGemma 1.5 4B, about 2.5 GB).
- **Both**, if the phone has room.

The screen shows each size and the phone's free space and refuses gracefully rather
than filling the device. Either can be removed at any time and everything already
produced stays. Removing one falls back to its manual path and says so once. Neither
downloads on a metered connection without an explicit tap.

All model calls are single-turn. There is no chat. Every call has a fixed input schema
and a fixed output schema and returns JSON that either validates or is discarded. The
model never sees the person's weight as a number, never sees their age, never sees the
exclusions, and never receives any medical term the person typed into the free-text
line.

## Where the model sits, and where it does not
The model is visible in exactly four places, and it is never a chatbot.

1. Your words become what is tracked (job 1 below). The person speaks or types what they want to be able to do; the model turns it into tracked items with a domain each; the person confirms. This is the most differentiated use and the first thing a new user meets.
2. The phone times and counts the monthly check. This is not the language model at all: it is the accelerometer and MediaPipe. It is listed here because to the person it is the same capability, and because the copy must be honest that it happens on the phone and nothing is recorded.
3. Try it and see. The engine finds the pattern and runs the arithmetic; the model words the offer and the result. The honesty of "no difference" is enforced by the engine, not the model.
4. The visit summary (job 6). The principal use, and the only one that is synthesis rather than bounded extraction: months of measures, ratings, sentences, sessions and weight, read back as three paragraphs and a list of things worth asking about. The engine assembles the brief and generates every question candidate; the model words them; a deterministic validator checks every claim against the brief before anything renders.

## The jobs

### Job 1: Words to tracked items
**Reusable.** ADDENDUM-03 Part 18 makes this available any time from Progress as "Add
something you'd like to be able to do", not only at onboarding.

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

### Job 6: Write the visit summary

This is the app's principal use of the model. Everything above is bounded
extraction; this is synthesis across months of data, and it is the one job where
an invented fact could reach a clinician.

**Input:** the structured brief from LOGIC.md 13b, as JSON. Nothing else. No database access, no tool use, no second turn.

**Output:**
```json
{
  "paragraphs": [
    {"text": "string", "cites": ["fact_id", "fact_id"]},
    {"text": "string", "cites": ["fact_id"]},
    {"text": "string", "cites": ["fact_id"]}
  ],
  "questions": [
    {"text": "string", "candidate_id": "string"}
  ]
}
```

Every fact in the brief carries a `fact_id`. Every paragraph must cite the ids of the facts it uses. Every question must carry the `candidate_id` of the engine-generated candidate it rewords.

**Length:** three paragraphs, at most 90 words total. At most four questions, each one sentence.

**What each paragraph is for**, stated in the prompt so the shape is consistent:
1. What changed most, and what stayed the same. Same is stated plainly and never apologised for.
2. What the person mentioned, in their own words, and anything the app noticed alongside it.
3. What has been happening lately: sessions, gaps, the current walk or set, the levers.

**Prompt rules given to the model:**
- Write in the second person, to the person, not about them.
- Use only facts in the brief. If something is not in the brief, it did not happen.
- Never state a number that is not in the brief, and never compute a new one, including percentages, rates, projections, and totals.
- Never name a condition, never interpret a symptom, never suggest a cause, never recommend an action.
- Use the person's own words where the brief supplies them, in quotation marks.
- Never use a word from the banned list in DESIGN.md section 6.
- Questions are things to ask, phrased as questions, never as findings.

### The validator (deterministic, runs before anything renders)

Built before the model is wired in, not after. An unvalidated sentence in this feature is the one failure that actually matters, because a clinician may act on it.

1. **Number check.** Extract every numeral and every quantity word (twice, three times, half, most, all, none) from the output. Every one must appear in, or be directly entailed by, a fact the paragraph cites. A number with no matching fact fails the paragraph.
2. **Citation check.** Every paragraph must cite at least one fact id, and every id must exist in the brief.
3. **Date check.** Every month, season, or date reference must match a date in a cited fact.
4. **Quote check.** Every quoted string must appear verbatim in the brief's verbatim sentences.
5. **Question check.** Every question must carry a valid candidate_id. Questions without one are dropped.
6. **Banned word check** against DESIGN.md section 6, plus a clinical-term list: diagnos*, condition, disease, syndrome, arthritis, sarcopenia, frailty, deficiency, deficit, weakness, risk, symptom, treat, prescribe, dose, medication, therapy.
7. **Claim shape check.** Reject any sentence containing "because," "due to," "caused by," "which means," "suggests," or "indicates," since every one of them is the model reaching past the data.

**On failure:** regenerate once with the failing paragraph named. On a second failure, drop that paragraph and render the summary with the remaining ones. If all three fail, render the fixed fallback: the measures table with a single line above it, "The written summary could not be produced this time. The numbers below are complete." Never show a partial or unvalidated sentence, and never show an error that blames the person.

**Without the model installed:** the summary page shows a deterministic version assembled from templates, plainer and shorter, plus the full measures table. The button is never absent and never disabled.

## Forbidden, in code and in prompts
The model may not: choose or change a walking step; write a life sentence for an ability (the engine picks from hand-written templates); decide whether an ability is Better, Same, or Quieter; word the decline sentence (fixed text in DESIGN.md); compute or state any number; interpret BMI, the waist band, a test result, or a weight change; describe movement as burning or earning anything; connect any restriction tag to a weight direction; produce nutrition claims; mention or reason about medication; produce a diagnosis, a symptom interpretation, or a recommendation to see or not see a clinician (the engine shows those notes, deterministically); hold a conversation.

And, in job 6 or any other: compute a percentage, a rate, a total, or a projection; name a condition; suggest a cause; recommend an action; add a question the engine did not generate; quote anything not supplied verbatim; refer to a date, month, or event not in its input.

## Failure handling
Model unavailable, out of memory, or output invalid: fall back silently to the no-model path for that job. Never show an error that blames the person. Log the failure locally for the owner's diagnostics screen. Loading follows the on-device model rules in standards/kamsiob-project-template.md section C7: lazy load, memory-mapped weights, one memory manager, unload under pressure, speed is a gating requirement.

## Testing the model jobs
Each job ships with a fixture set of at least 30 inputs and expected outputs (job 2: sentences and the tags they must and must not produce, including synonyms like "grabbed takeout," "ordered in," "ate at the diner" all mapping to ate out; job 3: weeks that must not yield a restriction-plus-weight sentence). A job that fails its fixtures does not ship; the app runs without it.

Job 6 ships with at least 40 briefs and expected outcomes, including these adversarial cases, each of which the validator must catch: a brief with no question candidates (must produce no question list); a brief where all four abilities are Same (must not imply failure); a brief with a Quieter domain (must not name a cause); a brief with a 60-day gap (must state it without judgement); a brief with body tags on 12 days (must produce a question, not an explanation); a brief with two measures moving in opposite directions (must state both). Separately, the validator ships with a corpus of at least 30 deliberately bad outputs, one per failure mode: invented numbers, invented months, a fabricated quote, a causal claim, a condition name, an added question. A job 6 that fails its fixtures does not ship and the app falls back to the template version.


### Job 7: The end-of-session note
Optional, offered at the end of a session, one line, spoken or typed.
Input: { "text": string, "vocabulary": [24 fixed tags], "person_synonyms": [...] }.
Output: { "tags": [up to 3 strings from vocabulary], "confidence": [numbers] }.
Rules: identical to job 2's, because it is job 2 pointed at a different moment. Its
only extra purpose is to feed "what the app noticed". Without the model the person
picks from the grid, and most will skip it entirely, which is fine.

### Job 8: Reading a therapist's sheet
Input: { "text": string, "movements": [{"id": string, "name": string, "aliases": [...]}] }.
Output: { "items": [ { "verbatim": string, "movement_id": string | null, "reps": int | null, "sets": int | null, "times_per_day": int | null, "days_per_week": int | null } ] }.
Rules: every item carries the verbatim line it came from. `movement_id` is null when
nothing in the library matches, and the item is kept verbatim rather than forced onto
the nearest movement. The model never invents a movement, never adds one the sheet did
not contain, never changes a number, and never interprets the plan. Reps and frequency
are filled only where the sheet states them. Every item is confirmed by the person
before anything is saved.

Without the model: the person picks from the library or types, through the same
confirmation screen.

### Job 9: Reading a report or a letter
Input: { "text": string } from on-device extraction, and nothing else. Single turn, no
follow up, no question box.
Output:
```json
{
  "kind": "string",
  "paragraphs": ["string"],
  "terms": [{"term": "string", "plain": "string"}],
  "questions": ["string"]
}
```
Rules, from ADDENDUM-03 Part 7:
- `kind` comes from the document itself, never a guess.
- `paragraphs`: three to five short paragraphs, second person, every sentence traceable
  to text in the document.
- `terms`: the clinical terms and abbreviations that actually appeared, each with one
  plain line. This is the part people will use most.
- `questions`: drawn from the document, never from inference. Something mentioned but
  not explained, a goal with a date, an unclear instruction, a named follow up. Each
  phrased as a question, never as a finding.

**What it never does**, and this is the line that keeps the app outside HAI-DEF
Clinical Use rather than a matter of style:
- Never diagnoses, and never names a condition the document did not name.
- Never says whether the news is good or bad, never characterises progress, never
  reassures. If a report says a range of motion decreased, the app says the report says
  it, and nothing more.
- Never advises: not on exercise, treatment, medication, or whether to follow a plan.
- Never says whether to be worried, and never says not to be.
- Never contradicts, questions or second-guesses what a clinician wrote.
- Never interprets a number against a normal range or a population.
- Never handles anything outside movement, therapy, or care logistics.

Shown once on first use and printed at the top of every reading: "This explains the
words in your document. It doesn't say what they mean for you. That's a question for
whoever wrote it."

### The job 9 validator, built before the model is wired in
- Every proper noun, number, date and measurement in the output must appear in the
  extracted text.
- Every explained term must appear in the extracted text.
- Every question must reference something in the document.
- The banned list fires on: diagnos, prognos, condition, disease, should, recommend,
  suggest, indicates, means that, worse, better, improving, declining, worrying,
  concerning, normal, abnormal, healthy, unhealthy, good news, bad news.
- Any sentence containing because, due to, caused by, which means, or suggests is
  rejected.

On failure: regenerate once. On a second failure, drop that section. If the plain
words section fails entirely, show only the term explanations and the photo: "The app
could only pull out the words this time. Your document is saved above."

### Fixtures for jobs 8 and 9
At least thirty documents, including a progress note, a plan of care, a discharge
summary, an insurance letter, a referral, a handwritten programme, a badly
photographed one, one in Spanish, one out of scope, and one blank.

Adversarial cases that must be caught: characterising progress, naming a condition
absent from the text, recommending anything, inventing a measurement, and answering a
question the document raises.
