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
Gemma 4 E4B: Apache-2.0. Vico: Apache-2.0. Material 3 Expressive: Apache-2.0. Phosphor: MIT. Material Symbols: Apache-2.0. Open Peeps: CC0. Figtree: SIL Open Font Licence. Rive runtime: verify at build time and record. Any Lottie file: self-authored only. Every licence text ships in the app's Made with screen and in the repository. MedGemma is not used.

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
Any diagnosis or condition inference (device territory, and the exclusion model is better). Calorie or macro tracking (harm evidence). Streaks and loss framing (dark pattern under the studio's own rules and under the research). Medication dose tracking (medication management). Photo-based body-fat estimates (unvalidated, diagnostic-adjacent). Grip strength and supervised-only tests (hardware or fall risk). Embedding the PAR-Q+ (licence). MedGemma (licence and no benefit). Any cloud service (studio rule).
