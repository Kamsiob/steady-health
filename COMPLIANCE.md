# COMPLIANCE.md: the filter, applied before anything is built

App name: Steady Health by Kamsiob.

Steady ships publicly under a registered legal entity with an organisation Play Console account. Compliance is a filter, not a caveat. If a feature can only work through a non-compliant route, it is ruled out as specified.

## Google Play
- Health Content and Services policy: complete the Health apps declaration in Play Console accurately (health and fitness features, sensor use, camera use). Publish a privacy policy (PRIVACY.md mirrors the hosted version word for word). Include in the app description and in-app: "Steady is not a medical device and does not diagnose, treat, cure, or prevent any medical condition. Talk to a healthcare professional for medical advice." Steady makes no diagnostic or treatment claims and is not a regulated device.
- Sensor and camera health functions state device compatibility in the listing and request permissions in-flow with a prominent disclosure: ACTIVITY_RECOGNITION for steps, CAMERA for photos and the push-up counter, ACCESS_FINE_LOCATION only when the person starts a GPS walk. Health Connect read permissions requested only when the person turns Health Connect on, with the declaration Health Connect requires.
- No content that promotes disordered eating; no calorie counting; no numeric targets; a resources page pointing to the National Alliance for Eating Disorders helpline (not NEDA, which is disconnected).
- Data safety form: no data collected, no data shared. Everything on device. Optional model download from a stated source.
- Verify current policy text and the current target API level at build time; this file is not a substitute for the Play Console.

## FDA general wellness
Steady is a general wellness product: it encourages a healthy lifestyle and makes no claim to diagnose, cure, mitigate, treat, or prevent disease. Copy rules that keep it there: never "lowers your blood pressure," "treats," "prevents falls," "diagnoses," "sarcopenia," "frailty," "obesity class"; always "supports general fitness," "many people find," "general activity guidelines suggest," "worth mentioning to a clinician." Test results are shown as the person's own numbers against their own history, with typical ranges labelled as context for a population, never as a grade. The rate-of-loss, chair-stand, TUG, gait-speed, and single-leg notes say "worth mentioning to a clinician" and nothing more.

## FTC
Any outcome claim in the listing or in-app needs competent and reliable scientific evidence for that exact claim. Steady makes none: no "lose X," no "clinically proven," no testimonials. The README and store listing describe what the app does and does not do.

## PAR-Q+
The PAR-Q+ may not be reproduced or adapted in an electronic form without the written consent of the PAR-Q+ Collaboration. Steady does not embed it. The readiness screen uses original wording and links out to eparmedx.com with attribution: PAR-Q+ Collaboration (Warburton, Jamnik, Bredin and Gledhill, Health and Fitness Journal of Canada 4(2):3-23, 2011). If the owner obtains written consent, this decision can change; record it in DECISIONS.md.

## Licences bundled
Gemma 4 E4B: Apache-2.0. Vico: Apache-2.0. Material 3 Expressive: Apache-2.0. Phosphor: MIT. Material Symbols: Apache-2.0. Open Peeps: CC0. Figtree: SIL Open Font Licence. Any Lottie file: self-authored only. ML Kit text recognition: Apache-2.0, on device, no network. MedGemma 1.5 4B: Health AI Developer Foundations terms, shown and accepted before the download, weights never bundled. Every licence text ships in the app's Made with screen and in the repository.

## Disclaimers that are load-bearing
In app, in flow, not buried: not a medical device; talk to a clinician before harder efforts if you have a condition or symptoms; stop if you feel chest pain, faintness, or severe breathlessness (shown at the start of every test and every walk over 20 minutes); the data statement ("stays on your phone; there is no copy anywhere else").

## Specific to the capability frame
Never state or imply a clinical grade. A chair-stand count is the person's own number against their own history. Typical ranges for an age band appear only on request and are labelled as context for a population, not a grade. The words fall risk, frailty, sarcopenia, and decline never appear.
Never rank the person against an idealized body. Every comparison is to their own past. Documented ableism risk; see research/04 Part 6.
Camera use for rep counting: frames are processed in memory and never written to storage. The chip on screen says so, and the privacy policy says so.
Decline reporting: described once, neutrally, paired with an action (the visit summary and its measures table) and with everything that held. No colour change, no alarm, no repetition.
The visit summary reports and never advises. It states what the person logged and what changed, and it lists things to ask a clinician. It never names a condition, never interprets a symptom, never suggests a cause, and never recommends an action, which keeps it inside the FDA general wellness position that permits prompting a person to consult a professional as long as no specific condition is named. Every factual claim is validated against the source data before rendering, and unvalidated sentences are dropped rather than shown. The provenance line on the page and in the PDF states where the summary came from and that nothing was transmitted. The page carries the same standing disclaimer as the rest of the app: it is not a medical record and is not a clinical document, it is the person's own summary of their own logging, which they may choose to share.
Pacing mode: non-progressing, no offers, no experiments, per NICE NG206.

## What was ruled out, and why
Any diagnosis or condition inference (device territory, and the exclusion model is better). Calorie or macro tracking (harm evidence). Streaks and loss framing (dark pattern under the studio's own rules and under the research). Medication dose tracking (medication management). Photo-based body-fat estimates (unvalidated, diagnostic-adjacent). Grip strength and supervised-only tests (hardware or fall risk). Embedding the PAR-Q+ (licence). MedGemma, for the app as it then was (licence and no benefit). Reversed by ADDENDUM-03 Part 7 for the document-reading feature; see below. Any cloud service (studio rule).

## The therapist's plan (ADDENDUM-03 Part 6, merged on commit 9837f2a)
Here the app is a record keeper and not a clinician. It never interprets a plan, never
modifies one, never advises for or against one, and never suggests the person is doing
it wrong. It runs what it was given, exactly as given, and reports what was done.

A plan movement that conflicts with something the person said they avoid is flagged
rather than silently dropped, and the flag points at the therapist rather than at the
app's own judgement: "Your plan has wall push ups, and you said you avoid pushing. Ask
your therapist about it. We'll leave it in for now."

The export back to the therapist states what was prescribed, what was done and when,
the numbers, what hurt and when, and the person's own ratings. It draws no conclusion
from any of it.

## Reading reports and letters, and the HAI-DEF boundary (ADDENDUM-03 Part 7)
**This reverses the earlier decision that MedGemma is not used.** The earlier decision
was correct for the app as it then was and is wrong for the feature added here; the
reversal and its reasoning are recorded in DECISIONS.md.

HAI-DEF permits commercial use and redistribution with pass-through and prohibits
Clinical Use, defined as any use in diagnosis or treatment. Explaining the words in a
document the person already holds, without interpreting, advising, or diagnosing, sits
outside that definition. The constraints in AI.md job 9 are what keep it there, and
they are load-bearing rather than stylistic:

- Never diagnoses, and never names a condition the document did not name.
- Never says whether the news is good or bad, and never characterises progress.
- Never advises on exercise, treatment, medication, or whether to follow a plan.
- Never says whether to be worried, and never says not to be.
- Never contradicts or second-guesses what a clinician wrote.
- Never interprets a number against a normal range or a population.
- Never handles anything outside movement, therapy, or care logistics.

Printed at the top of every reading, and shown once on first use: "This explains the
words in your document. It doesn't say what they mean for you. That's a question for
whoever wrote it."

The weights are a separate, user-initiated download, never bundled in the repository
or in the APK, which keeps them clear of AGPL-3.0. HAI-DEF terms are shown before the
download and accepted there. Made with credits Google and names the licence.

**A health tech attorney reviews this boundary once before release.** Until they have,
it is on the BLOCKED list and the feature ships behind a flag that is off. That is not
a formality: the difference between explaining a word and interpreting a document is
the whole of the argument, and it is not mine to settle.

**Google Play:** the health apps declaration is updated to describe document reading.
The listing describes it as explaining documents in plain words and claims no
interpretation.
