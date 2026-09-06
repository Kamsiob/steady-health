# Research 4: Evidence Base for the Capability Frame

Produced September 2026 for Steady Health. This is the report that caused the app to be rebuilt around what a body can do rather than what it weighs. Read it before questioning any decision in MASTER_SPEC.md section 2.

## TL;DR
- Function beats weight as the organizing frame, and the evidence is strong. Gait speed, grip strength, chair rise, and the ability to get off the floor predict mortality, disability, and loss of independence as well as or better than body weight or BMI, and unlike weight they map directly to what a person can do.
- The four-domain frame (Get up, Go, Carry, Steady) is defensible and buildable by a solo developer. Validated self-tests exist across the full spectrum from bedbound (grip, range of motion, ADL frameworks) to active midlife (sitting-rising test, loaded carry). A phone can time or capture many of them via pose estimation (about 82 to 88% rep-detection accuracy) and waist accelerometry (over 94% timing accuracy for sit-to-stand).
- Visible on-device AI is realistic for rep counting, movement timing, and free-text-to-capability mapping. It is not realistic for diagnostic form correction or clinical-grade balance assessment.
- No mainstream consumer app leads with functional capability rather than weight or steps. Competitors cluster in the older-adult niche and mostly sell through health plans. The capability frame carries real ableism risk for people whose function is declining irreversibly, so decline must be reported without alarm.

## Part 1. The frame and its evidence
Gait speed: pooled analysis of 9 cohorts, 34,485 community-dwelling adults 65 and over, followed 6 to 21 years (Studenski et al., JAMA 2011;305(1):50-58). Pooled hazard ratio per 0.1 m/s was 0.88 (95% CI 0.87 to 0.90). Predicted survival from age, sex, and gait speed was as accurate as prediction from age, sex, chronic conditions, smoking history, blood pressure, BMI, and hospitalization. One functional measure matched a whole panel of clinical risk factors including BMI. This is the empirical core of demoting weight.

Grip strength: meta-analysis of 42 studies, 3,002,203 participants (Wu et al.), each 5 kg decrease carried HR 1.16 (95% CI 1.12 to 1.20) for all-cause mortality. PURE (Leong et al., Lancet 2015) found grip a stronger predictor of all-cause and cardiovascular mortality than systolic blood pressure. Newcastle 85+ found each kg per year of decline raised mortality risk 16% in men, 33% in women.

Sitting-rising test: Brito and Araujo et al. (Eur J Prev Cardiol 2014) followed 2,002 adults aged 51 to 80. Lowest scorers (0 to 3) had 5 to 6 times the mortality of top scorers; each 1-point increase conferred 21% better survival. The 2025 follow-up (4,282 adults, 46 to 75, median 12 years) found the lowest group had 3.8 times higher natural-cause and about 6 times higher cardiovascular mortality than those scoring 10.

Floor rise: "Nearly half of older adults are unable to get up from the floor after a fall in the absence of an injury, and floor-to-stand transfer ability is associated with falls, physical function, hospitalization, need for caregiver support, and even mortality," and it is "considered a geriatric functional milestone" (Geriatrics 2025). Tinetti's original figure is about 47% of non-injured fallers unable to rise unassisted. It is trainable: physiotherapist-led training helped 22 of 52 initially failing patients regain independent floor rise, and a 2-week, 6-session strategy intervention improved rise difficulty and success in disabled older adults.

Midlife is the right window. Muscle mass declines about 0.5 to 1% a year and strength 1.5 to 5% a year from midlife; a 2025 review notes decline "begins long before overt sarcopenia." Only about 22.6% of adults 45 to 64 meet both aerobic and muscle-strengthening guidelines (2024 NHIS), and 17.6% of women 50 to 64 (CDC NCHS Data Brief 443).

Motivation: brief anti-ageism messages increased physical-activity motivation among older adults (randomized, 7 senior centres, n=349); negative aging self-perceptions reduce health behaviour. Frame around agency and capability retention, not fear of loss. The Communication Predicament of Aging model shows cues signalling expected incompetence trigger a spiral of decline.

## Part 2. Measurement across the spectrum
Bedbound and very low function. Hand grip dynamometry (needs a device; a phone cannot measure force). Towel or ball squeeze as a non-quantitative accessibility fallback, flagged as such. Breath-hold (phone can time; weak validation as a general marker). Peak flow and spirometry need devices. Bed mobility and bed-to-chair transfer, tracked in the FIM and Barthel Index, need supervision for anyone at fall risk. Range of motion, ankle pumps, isometric holds: safe, equipment-free, phone can estimate joint angle. The Barthel Index and Katz ADL are frameworks for structuring capability questions in movement terms, not self-tests.

Wheelchair users. Seated functional reach (validated in stroke and SCI). Pressure relief: partial unloading of each buttock 15 s every 25 minutes and full off-loading at least 15 s every 2 hours; forward and side leans preferred over push-ups, which load the upper limbs heavily. This is a behaviour to prompt, not a test. Wheelchair Skills Test exists but Physiopedia warns many skills carry real risk and need trained help; catalogue but do not coach the risky ones. Community upper-body exercise improves function and quality of life in manual wheelchair users.

Walker and cane users. The SPPB explicitly allows walking aids in the 4-metre walk. Record aid use as a covariate, never an exclusion. Normative cutoffs assume no aid, so for aided users track within-person trend only.

Rehabilitation. Patient-Specific Functional Scale: the person names 3 to 5 activities they struggle with and rates each 0 to 10; MCID about 1.3 to 3.0 depending on population. Excellent fit for a "what can you do" app: validated, patient-defined, no diagnosis needed. Goal Attainment Scaling is the individualized alternative. MCID is the smallest difference patients perceive as beneficial; pair with Minimal Detectable Change to separate real change from measurement error. Both are population averages, not person-specific.

Older adults and general population. Short Physical Performance Battery: balance (feet together, semi-tandem, tandem, 10 s each) plus 4 m gait speed plus 5x chair stand, each scored 0 to 4, total 0 to 12. Cutoffs: 0-3 severe, 4-6 moderate, 7-9 mild mobility impairment, 10-12 minimal. Under 10 predicts all-cause mortality and falls; 8 or under flags sarcopenia. Senior Fitness Test (Rikli and Jones): 30-second chair stand, 30-second arm curl, 2-minute step, chair sit-and-reach, back scratch, 8-foot up-and-go, 6-minute walk, with percentile norms by 5-year band and sex from 7,183 adults aged 60 to 94. Four-stage balance test (CDC STEADI), equipment-free, spotter or wall for tandem and single-leg.

Midlife and active. Sitting-rising test, 0 to 10, deduct 1 per hand or knee support and 0.5 for unsteadiness; equipment-free, under 2 minutes, mortality-validated. Barefoot with a spotter; contraindicated with acute knee or joint problems. Self-report scoring is more reliable than pose detection here. Push-up test, plank, wall sit, deep squat hold with existing norms; isometrics raise blood pressure so caution with uncontrolled hypertension. Loaded carry has no consensus protocol; track load times distance within-person. Stair climb, 1-mile walk, Cooper 12-minute for aerobic capacity, with cardiac contraindications.

Phone capture summary. The phone reliably times movements, counts reps, estimates joint angles, and detects weight shifts. It cannot measure force or reliably grade subtle form or balance sway at clinical quality.

## Part 3. Programming across the spectrum
Bedbound: ankle pumps, quad sets, glute sets, heel slides, bridging, isometrics, active and passive range of motion, deep breathing and incentive spirometry. Frame as maintenance.

Wheelchair: seated cardio (arm ergometry), band resistance, pressure-relief behaviours.

Adapted guidelines: WHO 2020 added first-ever specific recommendations for adults with disability within the general guidance (150 to 300 minutes a week moderate aerobic plus strengthening 2 or more days; "some physical activity is better than none"). ACSM's 2022 Expert Consensus Statement on prescribing exercise for people with disabilities gives five practical recommendations. NCHPAD is the key resource repository.

Chair-based exercise: Klempel et al. (IJERPH 2021), systematic review and meta-analysis, 25 studies, 1,388 participants aged 50 and over, found improvements in handgrip (MD 2.10 kg, 95% CI 0.76 to 3.43), 30-second arm curl (MD 2.82, CI 1.34 to 4.31), and 30-second chair stand (MD 2.25, CI 0.64 to 3.86), concluding chair-based programmes "are effective and should be promoted."

Home exercise adherence: a systematic review of 23 studies found pooled adherence of 21%; a real-world older-adult falls cohort averaged 65%. Predictors of better adherence (Ricke et al., Front Sports Act Living 2023): intention to exercise (OR 1.47), positive attitude (OR 1.76), higher education, better physical health. Pain during exercise, depression, and perceived barriers hurt. Design consequence: reduce friction, build self-efficacy, avoid pain.

Floor transfer training: teach intermediate positions (half-kneel most commonly, quadruped and roll-to-stand valid alternatives); Backward Chaining is used in trials.

## Part 4. Visible AI that is not a chatbot
Pose estimation. MediaPipe Pose (33 landmarks, on device) reports 88.3% (jumping jacks), 85% (squats), 83.3% (push-ups), 82% (sit-ups) detection across 240 reps in one evaluation; rep counting by joint-angle peak detection is robust. Accuracy degrades with poor lighting, camera angle, occlusion, and partial-body visibility; monocular depth is unreliable. Verdict: rep counting and gross form flags are buildable, precise clinical form correction is not.

Sit-to-stand from accelerometer. Waist-mounted smartphone accelerometry times sit-to-stand with ICC 0.92 to 0.97, absolute error under 6%, accuracy over 94%, and can detect rising strategies and subtle midlife mobility change. This is the single best validated, visible, phone-only functional measurement available.

Balance. Smartphone inertial features estimated Berg Balance Scale (cross-validated RMSE 2.91) and falls risk at about 69% accuracy. Research grade, not clinical. Show as a personal trend, never a grade.

Natural language to structured capability. Mapping a person's own words ("I can't kneel to weed the garden anymore") into a capability schema is exactly the bounded extraction task an on-device 4B model does well, and it is the most differentiated visible AI pattern available for this app.

N-of-1 and single-case experimental design. N-of-1 trials are multi-crossover randomized trials of sample size one, elevated by the Oxford Centre for Evidence-Based Medicine to the highest level of individual evidence. Platforms exist (StudyU, JMIR 2022). As a consumer pattern: let the person test whether A or B works better for them over alternating blocks, computed on device.

Change detection. Joint modelling of serial gait speed predicts mortality-risk trajectory; two measurements suffice to estimate a slope. On device, flag a personal downward trend as a gentle prompt, never an alarm, never a diagnosis.

Realistic for a solo builder with a 4B model plus MediaPipe: rep counting, movement timing, range-of-motion estimation, free-text-to-capability mapping, adaptive session selection, personal trend detection, N-of-1 comparisons. Not realistic: clinical-grade form correction, diagnostic balance grading, force from a camera.

## Part 5. Market
Bold: at-home online exercise for 65+, designed by physicians and PTs, claims balance programmes decrease fall risk by more than 40%, distributed mostly free through Medicare Advantage (Renew Active, One Pass), free Basic tier, founded 2019.
Nymbl Science: dual-task balance training, about 10 minutes a day, health-plan distribution, founded 2014.
Mighty Health: all-in-one for 50+, strength, flexibility, nutrition, human coaching, $29 a month or $179.99 a year, founded 2018.
Vivo: live small-group (max 6) strength classes over Zoom for 55+, four difficulty levels including seated, $99 to $199 a month. Founder: "Most digital fitness products are not designed for seniors."
Pepper: EMS muscle activation suit plus app, US launch June 2026, vendor efficacy claims are marketing.
AgeWell: fragmented across at least six unrelated organizations.
GoGoGrandparent: concierge service, not a fitness product.

Is any consumer app organized around functional capability rather than weight or steps? Yes in the older-adult and clinical niche (Vivo, AgeWell Europe, Mighty Health, SilverSneakers GO), but the mainstream paradigm remains weight, calories, and steps. A local-first, free, capability-first app spanning the full ability spectrum with visible on-device AI is an open niche.

Adoption: AARP (July 2024, n=694 adults 50+): 71% use smartphones or tablets and are open to health apps; 55% use apps to track fitness; only 20% of those with a chronic condition use an app to manage it; cost is the biggest barrier beyond free offerings; 88% would welcome a physician recommendation. Retention is brutal: a JMIR scoping review (18 studies, 525,824 participants) found a median 70% of users discontinued within 100 days; health and fitness apps show about 3% day-30 retention. Free and local-first attacks the top barrier directly.

Terminology: healthspan, functional aging, healthy aging, prehabilitation. Healthspan is ascendant and explicitly functional.

Adaptive and accessible fitness is thinner and more clinical; few consumer apps serve wheelchair or bedbound users well.

## Part 6. Safety and ethics of the capability frame
Ableism is documented, not hypothetical. A Frontiers in Sports and Active Living (2023) critique warns that normative capability standards "diminish the unique and embodied capability of others while simultaneously validating ableism," and that "ableism emerges from ageism, where value and preference are linked to certain levels of capability." Compulsory able-bodiedness and internalized ableism are the specific harms. Design consequence: never rank a person against an idealized body; anchor everything to their own trajectory and self-chosen goals.

Reporting decline. The Communication Predicament of Aging model shows cues signalling expected incompetence trigger a negative feedback loop. Alternatives: the Frailty-Focused Communication model (empower proactive measures) and supportive-communication research (acknowledge emotion, encourage discussion). Rules that follow: report decline as neutral trend not alarm; pair any decline signal with an agency-preserving action; celebrate maintenance as success.

Does measuring function motivate or discourage? Mixed and person-dependent. Self-efficacy and positive attitude predict adherence; pain, depression, and perceived barriers predict dropout. An RCT of an app-based fitness programme in early-retirement older adults improved self-perceived functioning. The no-scores, no-streaks, nothing-red design is protective.

Regulatory. FDA General Wellness policy (updated January 2026): low-risk products that maintain or encourage general health or a healthy activity without disease diagnosis or treatment claims are outside device regulation; the guidance permits telling users to consult a professional when outputs fall outside normal thresholds if no specific condition is named. Functional measurement crosses into device territory only when it claims to diagnose, treat, or clinically grade. Pacing mode for post-exertional malaise must be non-progressing: NICE NG206 (2021) states graded exercise therapy should not be offered and people should remain within their energy limit.

## Caveats
Norms assume unaided, supervised testing; for aided users and self-administration treat all numbers as within-person trends. Phone measurement is validated for timing and counting, not force or clinical grading; grip strength, a top mortality predictor, needs a dynamometer. Pose accuracy is condition-dependent and older adults' homes will stress it. The mortality literature is associational: improving gait speed or grip does not guarantee proportional benefit. Ableism and decline-reporting are the biggest product risks. FDA general-wellness guidance is non-binding.
