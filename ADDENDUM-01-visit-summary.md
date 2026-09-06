# ADDENDUM 01: The visit summary

> **Merged.** Folded into MASTER_SPEC.md, LOGIC.md, AI.md, DESIGN.md,
> ONBOARDING.md and COMPLIANCE.md in commit `230a3d2`, September 6, 2026. Those
> documents are the source of truth for this feature now. This file is kept as the
> record of what was asked for and is no longer authoritative; where it and a
> specification document disagree, the specification document wins.

Added September 6, 2026, after the build began. This is an addition to MASTER_SPEC.md, DESIGN.md, LOGIC.md, AI.md, ONBOARDING.md, and COMPLIANCE.md, not a replacement for any of them. Where this file and an earlier document disagree, this file wins for anything concerning the visit summary and nothing else.

Fold every section below into the document it names, in the same pass, rather than leaving this file as a separate source of truth. Delete nothing from the existing documents except where this file says to replace something. Record in DECISIONS.md that the addendum was folded in and on which commit.

---

## 1. What it is, and why

Steady Health already has a doctor page: a table of rows and numbers exported as a PDF. That page stays, and it is renamed. What is being added is the thing above the table.

The visit summary is one page, written on the phone, that says what has happened over the last few months and what is worth asking about at the next appointment. The person taps one button. They do not film anything, position anything, or learn anything new. It draws entirely on data the app already holds.

This is the app's principal use of the language model and the feature the whole AI story rests on. Everything else the model does (turning words into tracked items, tagging a sentence, wording an experiment) is bounded extraction. This is synthesis: looking across six months of measures, ratings, daily sentences, sessions, and weight, and deciding what matters enough to say. There is no rule that can be written for that, which is precisely why it is the model's job.

It also produces the artefact the audience actually wants. Adults over fifty overwhelmingly say they would welcome a clinician's involvement in an app they use; a page they can hand over is that involvement made concrete.

## 2. The hard constraint: the model may not invent

The model never sees a raw database. It never computes. It never states a number that was not handed to it. It never refers to an event that is not in its input. Every factual claim in the output is checked against the source data before the page renders, and any sentence that cannot be traced is dropped.

This is not a stylistic preference. A summary that invents a chair-stand count or a month is worse than no summary, because a clinician may act on it. The validator described in section 5 is the load-bearing engineering in this feature and must be built before the model is wired in, not after.

---

## 3. Add to MASTER_SPEC.md

Insert as section 6.10, and add the corresponding line to the phase plan.

### 6.10 The visit summary
One page, written on the phone from the person's own six months of data, saying what changed, what they mentioned, and what is worth asking about. Generated on demand from the Abilities tab and from the ability detail pages. Two outputs: read it on screen, or export a PDF that also carries the measures table (previously "For your doctor"). Requires at least eight weeks of data and at least two monthly checks; below that the button explains what is still needed instead of producing a thin page.

Phase change: this lands in **Phase 5**, alongside Try it and see, because both depend on the same longitudinal data being present and on the same validator infrastructure. The measures table (old grid screen 21) still ships in Phase 4 as it stands; Phase 5 puts the written summary above it.

---

## 4. Add to LOGIC.md

Insert as section 13b, replacing the existing section 13 heading with "13. The measures table (part of the visit summary export)".

### 13b. The visit summary: what the engine assembles

The engine does all selection, all arithmetic, and all thresholding. It produces a structured brief. The model receives only that brief.

**Window.** Default is the last six months, or since the first weigh-in if shorter. The person can choose three months or all time. Minimum to generate: 8 weeks of data and 2 completed monthly checks.

**What goes in the brief, and how each item is chosen:**

*Ability changes.* For each of the four domains, the engine computes the change between the first and last check inside the window, in the domain's own units, and labels it Better, Same, or Quieter by the rules already in section 3b. All four are always included, including Same.

*Measures.* For each measure taken at least twice in the window: first value with its date, last value with its date, number of times taken, and whether the difference exceeds that measure's detectable change. Never a percentage, never a projection.

*The person's own list.* Each tracked item with its verbatim text, its first rating and date, its last rating and date, and whether the change is at least 2 points.

*Mentions.* For each tag in the closed vocabulary, the count of days it appeared in the window, and the three most recent verbatim sentences containing it. A tag is only passed if it appeared on 5 or more days. Body tags (sore, pain, unwell) are always passed if they appeared at all, with their count and dates.

*Co-occurrence.* Any tag and measure or tag and tag pair the engine already found for Try it and see (section 9b rules), with its split. Nothing new is computed here; if the engine has not found a pattern, none is passed.

*Sessions.* Days moved and total minutes per month across the window, the current walk or set and its duration, and any gap of 14 days or more with its dates.

*Levers.* Weight direction as one of "a little lower," "about the same," "a little higher," plus the first and last smoothed values and the number of weigh-ins. Any blood pressure entries with their dates. Sleep average per month.

*Way of getting around*, and whether it changed during the window.

*Exclusions and pacing mode*, as flags only, never with any reason attached.

**What is never in the brief:** the raw journal text beyond the three most recent sentences per passed tag; anything about food beyond the food tags; any exclusion reason; any age; any diagnosis, because none is stored.

**Question candidates.** The engine, not the model, decides what is worth asking about. A question candidate is generated only by one of these rules, and each carries the evidence that triggered it:

1. A body tag (sore, pain, unwell) appearing on 5 or more days in the window, or on 3 or more days that fall within 48 hours of a session.
2. A measure that declined beyond its detectable change across three consecutive checks (the Quieter rule).
3. A tracked item whose rating fell by 2 or more points.
4. A blood pressure entry outside the range the person's other entries sit in, stated only as "worth checking again," never interpreted.
5. Weight falling faster than 1.5 kg per week averaged over four weeks (the existing rate-of-loss rule).
6. A gap of 30 days or more in sessions where the person also logged a body tag in the same period.
7. A measure that improved beyond its detectable change while a related tracked item did not, which is worth raising because the person's experience and the number disagree.

At most four candidates go to the model, ordered by rule number. If there are none, the summary has no question list and says so in one line. The model may reword a candidate but may not add one, and any question in its output that does not map to a candidate is dropped by the validator.

**Refresh.** The summary is generated on demand, not on a schedule, and the last generated one is stored with its window and generation date. Regenerating replaces it. Nothing about it is ever notified.

---

## 5. Add to AI.md

Insert as Job 6, and add the corresponding row to the forbidden list and the fixture requirements.

### Job 6: Write the visit summary

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

This is the part that makes the feature safe, and it is not optional.

1. **Number check.** Extract every numeral and every quantity word (twice, three times, half, most, all, none) from the output. Every one must appear in, or be directly entailed by, a fact the paragraph cites. A number with no matching fact fails the paragraph.
2. **Citation check.** Every paragraph must cite at least one fact id, and every id must exist in the brief.
3. **Date check.** Every month, season, or date reference must match a date in a cited fact.
4. **Quote check.** Every quoted string must appear verbatim in the brief's verbatim sentences.
5. **Question check.** Every question must carry a valid candidate_id. Questions without one are dropped.
6. **Banned word check** against DESIGN.md section 6, plus a clinical-term list: diagnos*, condition, disease, syndrome, arthritis, sarcopenia, frailty, deficiency, deficit, weakness, risk, symptom, treat, prescribe, dose, medication, therapy.
7. **Claim shape check.** Reject any sentence containing "because," "due to," "caused by," "which means," "suggests," or "indicates," since every one of them is the model reaching past the data.

**On failure:** regenerate once with the failing paragraph named. On a second failure, drop that paragraph and render the summary with the remaining ones. If all three fail, render the fixed fallback: the measures table with a single line above it, "The written summary could not be produced this time. The numbers below are complete." Never show a partial or unvalidated sentence, and never show an error that blames the person.

**Without the model installed:** the summary page shows a deterministic version assembled from templates, which is plainer and shorter, plus the full measures table. The button is never absent and never disabled.

**Fixtures:** at least 40 briefs with expected outcomes, including these adversarial cases, each of which must be caught by the validator: a brief with no question candidates (must produce no question list); a brief where all four abilities are Same (must not imply failure); a brief with a Quieter domain (must not name a cause); a brief with a 60-day gap (must state it without judgement); a brief with body tags on 12 days (must produce a question, not an explanation); a brief with two measures moving in opposite directions (must state both). A job that fails its fixtures does not ship and the app falls back to the template version.

### Add to the forbidden list
The model may not, in this job or any other: compute a percentage, a rate, a total, or a projection; name a condition; suggest a cause; recommend an action; add a question the engine did not generate; quote anything not supplied verbatim; refer to a date, month, or event not in its input.

---

## 6. Add to DESIGN.md

Insert the components into section 3, the screen into section 4, and the strings into section 6.

### The tab bar changes
The third tab is currently **Abilities**. It stays. The visit summary is not a fourth tab; three tabs is a rule and adding a fourth would break it. Instead:

- The Abilities tab gains a **top-right text action** reading **Summary**, in orange-d at 13.5/800, in the same slot the top row already uses for actions elsewhere.
- Each ability detail page gains the same action, which opens the summary scrolled to that ability's sentence.
- Settings gains one row under Your data: **Visit summary**, with the subtitle "Read it, or export a page for your appointment."

No new icon is added to the tab bar, and no badge or dot ever appears on it. The summary is never notified and never announced.

### New components

**Summary page (new screen).** Top row with a back button, the title "Your visit summary" at 20/800 navy, and a text action reading "Export". Then, in order:

1. **Window pill row.** Three pills: 3 months, 6 months (selected by default), All. Standard pill component.
2. **Ability strip.** The four abilities as a single white block, 24 radius, four rows separated by hairlines, each with its 32 px tinted glyph, the ability name at 14/800, and its Better or Same or Quieter pill on the right. This is a compressed version of the existing ability row and reuses its tints. Same is styled identically to Better in weight and size; only the pill colour differs (green-l on green for Better, sand on navy for Same, sand on navy for Quieter, since Quieter must not read as an alarm).
3. **The written summary.** A white card, 26 radius, 18 px padding, containing the three paragraphs at 15/400 ink with 1.5 line height and 10 px between them. Quoted words from the person appear in navy 500 rather than in italics. No heading above it, because the page title already says what it is.
4. **Question list.** A sand block, 24 radius, headed "Things you might ask about" at 15/800 navy, with each question as a row: a small navy circle bullet, the question at 14.5/600 navy, and under it the evidence line at 12.5/600 ink2 ("You mentioned sore knees on eleven days, mostly after longer walks"). If there are no candidates, this block is replaced by a single line in ink2: "Nothing stood out this time."
5. **Measures block.** The existing doc component, unchanged, under a section title "The numbers".
6. **Provenance line**, at the bottom in 12/600 ink3, centred: "Written on your phone from what you logged between March 2 and September 6. Nothing was sent anywhere."

The primary button is **Export as a PDF**. A secondary ghost button reads **Regenerate**.

**Empty state.** When there is not enough data, the page shows the ability strip if it exists, then a sand block: "The written summary needs about two months of logging and two monthly checks. You have six weeks and one check. The numbers below are complete and you can take them to an appointment now." Then the measures block and the export button. The button is never disabled.

**Loading state.** While the model runs, the summary card shows the Material 3 Expressive contained loading indicator with a single line, "Reading back through your months." No progress percentage, no skeleton text that could be mistaken for content.

### The PDF
The exported page is the same content in the same order, on white, with the app name and the date range at the top and the provenance line at the bottom. Figtree, same type scale, the ability tints reduced to a small coloured square before each name so it prints legibly in greyscale. One page if it fits, two if it does not; never more.

### Strings (final, do not paraphrase)
- Tab action and settings row: "Summary".
- Page title: "Your visit summary".
- Question block heading: "Things you might ask about".
- No candidates: "Nothing stood out this time."
- Measures heading: "The numbers".
- Provenance: "Written on your phone from what you logged between [date] and [date]. Nothing was sent anywhere."
- Loading: "Reading back through your months."
- Fallback when validation fails: "The written summary could not be produced this time. The numbers below are complete."
- Not enough data: as in the empty state above.
- Export button: "Export as a PDF".

### Add to the banned list
Already covered by section 6, but state explicitly for this feature: the summary never contains the words report, assessment, evaluation, findings, results, or recommendations. It is a summary, and the person's own page.

---

## 7. Add to ONBOARDING.md

The summary is not mentioned during onboarding. It is introduced once, in place, the first time the person opens the Abilities tab after the threshold is met: a sand block above the ability rows, shown once and then never again, reading:

"There's enough here now for a summary you can take to an appointment. It's written on your phone, from your own months."

With a single action, "Have a look". Dismissing it removes it permanently; the Summary action in the top row remains.

---

## 8. Add to COMPLIANCE.md

Under the capability-frame section:

The visit summary reports and never advises. It states what the person logged and what changed, and it lists things to ask a clinician. It never names a condition, never interprets a symptom, never suggests a cause, and never recommends an action, which keeps it inside the FDA general wellness position that permits prompting a person to consult a professional as long as no specific condition is named.

Every factual claim is validated against the source data before rendering, and unvalidated sentences are dropped rather than shown. The provenance line on the page and in the PDF states where the summary came from and that nothing was transmitted.

The page carries the same standing disclaimer as the rest of the app. It is not a medical record and is not a clinical document; it is the person's own summary of their own logging, which they may choose to share.

---

## 9. Testing requirements to add

- Unit tests for every question-candidate rule in LOGIC.md 13b, including the boundary cases at 5 days, 2 rating points, and three consecutive checks.
- Validator tests: a corpus of at least 30 deliberately bad model outputs (invented numbers, invented months, a fabricated quote, a causal claim, a condition name, an added question) each of which must be caught.
- Fixture tests for Job 6 as specified in AI.md.
- An instrumented test that generates a summary from a seeded six-month database and asserts that every numeral in the rendered page appears in the brief.
- A test that the summary renders correctly with the model absent, with the model failing, and with all three paragraphs failing validation.
- Accessibility: the summary page read end to end with TalkBack, at the largest font size, and the PDF checked in greyscale.

## 10. What this feature must not become

Not a chatbot, and not a text field. There is no question box on this page and no follow-up.
Not a notification. It is never pushed, badged, or announced.
Not a score. No summary sentence ever ranks the person or compares them to anyone.
Not a diagnosis. The question list is the only forward-looking content and every item on it is a question, not a finding.
Not a fourth tab.
