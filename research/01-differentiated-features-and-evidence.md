# Research 1: Differentiated Features for a Local-First, Health-Literate Weight and Body App

Produced September 2026 for Steady. Evidence quality is labelled. Use this to justify feature choices and to write the "why this" content in the app.

## TL;DR
- The most defensible differentiators are clinically grounded, non-numeric measures the whole category ignores: waist-to-height ratio (NICE 0.5 threshold), a muscle-preservation companion for people on GLP-1 medication (protein adequacy check-ins plus resistance-training prompts), and optional phone-measured functional self-tests (validated on Android). All stay inside FDA general wellness and Google Play boundaries.
- The app's anti-harm posture (no calorie counting, no streaks, no day-grading, smoothed trend line) is strongly evidence-supported. All-or-nothing calorie apps drive guilt, the abstinence-violation effect, and elevated disordered-eating symptoms; trend smoothing and self-compassion after lapses predict better outcomes.
- Avoid ideas that sound impressive but fail: photo-based body-fat percentage (not validated, edges into diagnostic claims), diagnostic labelling of frailty or sarcopenia (device territory), grip strength, gait speed, six-minute walk or TUG without hardware or supervision (hardware or fall risk), and any medication dose guidance.

## Part 1. What the category systematically ignores
Mainstream apps (MyFitnessPal, Lose It, Noom, MacroFactor, Cronometer) are built around calorie and macro logging with daily targets and green/red feedback. Documented failure modes:
- Guilt and shame loops. A UCL, Loughborough, and Westminster study (Bondaronek et al., British Journal of Health Psychology, October 2025) analysed 58,881 posts about the five highest-revenue fitness apps; 13,799 were negative. MyFitnessPal accounted for 8,464. Senior author Dr Paulina Bondaronek: "we found a lot of blame and shame, with people feeling they were not doing as well as they should be." Users reported shame when logging unhealthy foods, irritation at notifications, and demotivation leading to quitting. Red and green calorie visualisations are experienced as reward and punishment.
- All-or-nothing abandonment. The problem is not tracking itself but the friction, uncertainty, guilt, and clutter wrapped around it.
- The scale-anxiety gap. Daily fluctuation demoralises. Only niche apps (Happy Scale, Libra, Trale, openScale) smooth with a moving average; none of the big calorie apps lead with it.
- Things users hack in spreadsheets: trend lines, non-scale victories, waist measurements, progress-photo timelines, rate-of-loss awareness, GLP-1 timelines.

## Part 2. What actually predicts success
- National Weight Control Registry: over 10,000 maintainers who lost at least 13.6 kg and kept it off at least one year. Common behaviours: high physical activity (around an hour a day), regular breakfast, regular self-weighing, consistent eating pattern across weekdays and weekends. "Very little similarity in how these individuals lost weight." Maintenance gets easier after two to five years.
- Self-weighing: a 10,000-user smart-scale cohort (JMIR 2021) found self-weighing intensity inversely associated with weight change. An RCT (n=47) found daily weighers lost about 6.1 kg more. Meta-analysis: daily versus weekly shows no clear superiority; both work inside multi-component programmes.
- Implementation intentions: Gollwitzer and Sheeran (2006), 94 tests, n=8,461, medium-to-large effect (d=0.65) on goal attainment; health subdomain d=0.59. Stronger for adding behaviours than removing them.
- Lapse and recovery (abstinence-violation effect): patients who made characterological attributions for a first lapse ("I'm a failure") lost less. It is the breakdown in self-control after the lapse, not the lapse, that does damage. Direct evidence base for no-punishment design.
- App abandonment: burden, friction, guilt from red numbers, all-or-nothing. The Slip Buddy lapse-tracking RCT retained users comparably to calorie tracking (about 54% versus 58% of days).

## Part 3. Clinically legitimate measures consumer apps ignore
- Waist-to-height ratio. NICE NG246 (2022): adults with BMI under 35 should keep waist below half their height. Healthy central adiposity 0.4 to 0.49; increased 0.5 to 0.59; high 0.6 or more. Applies to both sexes and all ethnicities including high muscle mass. Predicts type 2 diabetes and cardiovascular risk at least as well as BMI. Feasible with a tape measure. Caveat: a 2025 study argues the single 0.5 cutoff is misleading for adolescents; scope the app to adults. Self-measured waist has technique-dependent error; give clear instructions (midpoint between lowest rib and iliac crest, end of normal exhale, measure twice and average).
- Functional measures. Smartphone sit-to-stand analysis discriminates sarcopenia and frailty. Ruiz-Cardenas et al. (Aging Clinical and Experimental Research, 2023) validated an Android version, ICC greater than 0.85, AUC 0.73 to 0.82 against EWGSOP2 sarcopenia. Otassha cohort (n=569, JMIR Formative Research 2026): optimism-corrected AUC 0.781, second only to grip strength. This is the single most credible on-device functional measure.
- Unintentional weight loss (over 5% in 6 to 12 months) is a geriatric red flag. Relevant to senior living, but the wrong product for a weight-loss app; cut from Steady.
- Rate-of-loss thresholds: clinical standard 1 to 2 lb per week. Gallstone risk rises exponentially above about 1.5 kg per week (American Journal of Medicine). Behavioural trial safety protocols cap at 3 lb per week for 3 to 4 consecutive weeks. Basis for a gentle informational note, never a target.
- Modest loss improves markers early: a 5 to 10% loss lowers systolic and diastolic BP by about 5 mmHg; improvement can begin at 2 to 5%.

## Part 4. The GLP-1 era
- Lean-mass loss is real. STEP-1: semaglutide reduced lean mass by 6.92 kg against 15.3 kg total, about 45% of weight lost. SURMOUNT-1: tirzepatide 5.67 kg against 22.1 kg total, about 26% (Neeland et al., Diabetes, Obesity and Metabolism 2024). A 2024 network meta-analysis of 22 RCTs found lean mass about 25% of loss, with relative lean mass preserved. Mitigation: resistance training plus adequate protein (about 1.2 to 1.6 g per kg per day during active loss). Resistance training preserves nearly all lean mass during caloric restriction.
- What patients struggle with: protein under appetite suppression, side-effect timing, injection-site rotation, knowing stalls are normal.
- Existing GLP-1 apps (Shotsy, MeAgain, OurGLP1, GLP AI, MyNetDiary GLP-1, VitalTrack) cover injection logging, dose timelines, side effects. Differentiator is not dose management (saturated, regulatory risk) but muscle preservation, framed around protein and strength, never medication.

## Part 5. Photos and visual progress
- Evidence that photos improve adherence is practitioner-level (trainer blogs), not RCT-grade. Legitimate points: photos capture recomposition the scale misses and counter distorted daily self-perception. Be honest that hard evidence is thin.
- A Yale study found photo-based food logging is dissatisfying and abandoned; words beat pictures for food. Supports the plain-language journal and body photos, not food photos.
- Body-image risk is real: frequent body checking links to dissatisfaction; adolescent self-weighing links to lower self-esteem. Optional, weekly-capped photos and silhouette-only export are protective.
- On-device feasibility: MediaPipe Pose Landmarker (33 landmarks) for alignment across photos; Selfie Segmentation for silhouettes. Both run on mobile CPU or GPU with no server.

## Part 6. Safety, ethics, regulatory
- Disordered eating: Levinson, Fewell and Brosof (2017, Eating Behaviors, n=105 diagnosed patients): 73.1% said MyFitnessPal at least somewhat contributed to their eating disorder; 30.3% said very much. A well-designed RCT (n=200, low-risk undergraduate women) found one month of MyFitnessPal did not increase risk; risk concentrates in vulnerable users and is amplified by numbers and red/green feedback. The no-calorie, no-grading design is on the harm-reduction side.
- FDA General Wellness guidance (January 2026): FDA does not regulate low-risk general wellness products intended to maintain or encourage a healthy lifestyle that make no disease claims. Wellness products may prompt users to consult a professional when outputs fall outside normal thresholds, as long as they make no disease-specific, diagnostic, or treatment statements.
- Google Play Health Content and Services policy: health apps must complete the Health apps declaration, post a privacy policy, and if not a regulated device include the disclaimer that the app "is not a medical device and does not diagnose, treat, cure, or prevent any medical condition" and remind users to consult a healthcare professional. Any health feature brings the app into scope. Sensor and camera health functions must state device compatibility and obtain affirmative consent for in-scope permissions. Google Play prohibits apps promoting eating disorders. A sit-to-stand test triggers the declaration and sensor disclosure but is not prohibited unless framed as diagnosing frailty.
- Screening for disordered eating: do not deploy a clinical screener (SCOFF, EDE-Q) as a gate. Use always-available resources plus design guardrails.

## Part 7. Ranked features
1. Waist-to-height ratio with the guideline (top pick). Low build, very high credibility.
2. GLP-1 muscle-preservation companion: protein adequacy check-in and resistance prompts. No dose management.
3. On-device sit-to-stand functional self-test, framed as your own trend. Hardest build.
4. Smoothed trend line (EWMA). Table stakes done right.
5. Rate-of-loss safety note above about 1.5 kg per week sustained. Informational.
6. Unintentional-loss awareness. Cut for Steady (wrong product).
7. Auto-aligned progress-photo timelapse with silhouette export.
8. Non-scale marker log: blood pressure and resting heart rate, occasional, not daily.
9. Implementation-intention prompt in the journal.

Cut: photo body-fat percentage; grip, gait speed, six-minute walk, TUG without supervision; GLP-1 dose scheduling; clinical eating-disorder screeners as a gate; streaks and gamification.

## Caveats
- Evidence quality varies sharply. Self-weighing and NWCR are robust; progress-photo adherence claims are practitioner-level.
- GLP-1 lean-mass percentages come from trial substudies with different populations and methods.
- Regulatory guidance is current but non-binding and tightening. Conservative posture: no diagnosis, no dose advice, clear disclaimers, professional-referral prompts.
- Resources: direct to the National Alliance for Eating Disorders helpline, not NEDA (disconnected).
