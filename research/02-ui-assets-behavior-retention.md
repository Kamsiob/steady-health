# Research 2: Open-Source UI Assets, Long-Horizon Guidance Data, Behavioural Science, and Retention

Produced September 2026 for Steady.

## TL;DR
- The entire visual layer can be built from permissively licensed assets that coexist with AGPLv3: Vico (Apache-2.0) for charts, Material 3 Expressive shape, motion, and loading APIs (Apache-2.0, experimental opt-in), Lottie runtime (Apache-2.0) or Rive (better for state-driven animation), CC0 people illustrations (Open Peeps, Humaaans), and MIT, ISC, or Apache icon sets (Lucide, Phosphor, Tabler, Iconoir, Material Symbols). The trap is individual Lottie and illustration files, which carry their own licences.
- The evidence supports the contrarian design. "21 days to a habit" is a myth (Lally: median 66 days, range 18 to 254). The "10% rule" for progression is not evidence-based. Weight-loss plateaus around six months are physiological. Streaks, confetti, and scores are the controlling extrinsic mechanics self-determination theory predicts will undermine long-term motivation.
- Health and fitness apps retain about 3% of users at day 30. Privacy is a genuine differentiator but there is thin direct evidence it improves retention. Optimise for the returning occasional user, treat owned exportable data as the retention moat, and use autonomy-supportive copy, tiny prompts, and honest progress signals.

## Part 1. Assets and licences
Ground rule: AGPLv3 covers your code. Permissive dependencies (Apache-2.0, MIT, BSD, ISC) bundle fine. Apache-2.0 is compatible with GPLv3 and AGPLv3 but not GPLv2. CC0 imposes no obligations. Keep LICENSE and NOTICE texts and add an in-app credits screen.

Charts: Vico (patrykandpatrick/vico), Apache-2.0, Compose-first, Material 3 theming module. YCharts (codeandtheory), Apache-2.0, ships a TalkBack point-by-point accessibility readout. Koala Plot is MIT.

Material 3 Expressive: androidx.compose.material3 1.4.0-alpha10 and later. All Expressive APIs require @OptIn(ExperimentalMaterial3ExpressiveApi::class). 35 built-in shapes with animated morphing; spring-based MotionScheme (standard and expressive); ContainedLoadingIndicator and shape-cycling indicators. Material Symbols is a variable icon font, Apache-2.0.

Lottie: lottie-android runtime is Apache-2.0. The trap is files: LottieFiles free animations fall under the Lottie Simple License, which is share-alike and forbids relicensing; Marketplace files often forbid redistribution. Bundle only self-authored or clearly licensed files and record which is which.

Rive: open-source runtimes; state machines live in the asset; typically 3 to 5 times smaller than equivalent Lottie for icon-scale animations; idle CPU near zero. Heavier first load. Verify the Android runtime licence at build time. Use Rive for interactive state-driven pieces (done-state morph, Go button, walk marker), Lottie for play-once art.

Illustration: Open Peeps and Humaaans (Pablo Stanley) are CC0 with no restrictions; composable, so every body size can be depicted. unDraw is free with some pack-redistribution clauses. Storyset requires attribution. Standardise on one CC0 people library.

Icons: Lucide (ISC), Phosphor (MIT, six weights, state by fill not colour), Tabler (MIT, largest coverage), Iconoir (MIT), Remix (Apache-2.0, document changes), Material Symbols (Apache-2.0). Recommended pairing: Phosphor plus Material Symbols, Tabler for overflow.

Design systems to borrow patterns (not code): Radix, shadcn/ui, Base UI, Primer, Carbon, Polaris, Atlassian, Spectrum, Fluent.

Data visualisation: EWMA trend as bold line over faint dots; sparklines; calendar heatmap for presence without streaks; the walk ladder as a path; radial gauges sparingly and never score-like; skip ridgeline and stream graphs.

## Part 2. Guidance data for day 1 to day 1000
Exercise progression: the 10% rule is not evidence-based (Buist et al. 2008 found no injury difference; a 2020 systematic review of 23,047 runners concluded it is not justified). Frandsen et al. (BJSM) reframe risk around single-session spikes: exceeding about 110% of the longest run in the prior 30 days sharply raises injury risk. Cap jumps relative to the recent longest effort, not weekly totals, and treat repeating a step as progress. Encode Borg RPE 6 to 20 and CR10 as intensity language; the talk test is a validated plain-language moderate-intensity proxy. Progression: when a walk feels like RPE 3 to 4 (could talk) for two sessions, the next one opens.

Normative data (skews older adults; label validated ranges):
- 30-second chair stand (Rikli and Jones): healthy young adults average about 33; MCID 4 to 6.
- Five times sit-to-stand (Bohannon 2006): worse than average if slower than 11.4 s (60 to 69), 12.6 s (70 to 79), 14.8 s (80 to 89). Fall-risk cutoff over 15 s (Buatois 2008).
- Timed up and go (Bohannon 2006): 8.1 s (60 to 69), 9.2 s (70 to 79), 11.3 s (80 to 99); high fall risk at 13.5 s or more; CDC STEADI uses 12 s or more.
- Six-minute walk (Enright and Sherrill 1998): medians 576 m men, 494 m women; Casanova 2011 seven-country means 611 m (40 to 49) to 514 m (70 to 80).
- Gait speed (Bohannon and Andrews 2011): under 0.6 m/s high risk, under 0.8 m/s frailty limit, 1.0 m/s or more well; each 0.1 m/s about 12% lower mortality (Studenski 2011).
- Single-leg balance (Springer 2007): about 43 s eyes open at 18 to 39 declining to under 10 s at 80 plus; inability to hold 10 s linked to mortality (Araujo 2022).
- Push-ups: ACSM norms; Yang 2019 (JAMA Network Open, 1,104 active male firefighters) found over 40 push-ups associated with about 96% lower CVD events than under 10, observational and male-only.
- Grip strength: not phone-measurable; each 5 kg lower is about 16% higher all-cause mortality (PURE).

Weight-loss trajectory: fast early loss, deceleration at 3 to 6 months, plateau at 6 to 8 months (physiological: metabolic adaptation, reduced energy expenditure, increased appetite), gradual regain over 1 to 2 years. Switching diets does not overcome the plateau (Stanford crossover RCT 2024). About 20% maintain at 5 years. Real-world commercial data (667 users): mean loss about 7.9% over 133 days; 47% follow decrease, plateau, partial regain; 31% steady decrease; 16% decrease, plateau, further decline. Early loss predicts 3 to 12 month outcome. STEP-1: semaglutide mean change 14.9% at week 68, nadir around week 60, steepest weeks 4 to 28. Design implication: pre-empt the plateau narrative with the expected curve; never draw a straight line to a goal.

Daily variability and EWMA: daily swings of 1 to 2 kg are normal. The Hacker's Diet approach uses a smoothing constant giving today's reading about 10% weight (alpha about 0.1), roughly a 20-day moving average. Walker's soft wall (2 lb) and hard wall (5 lb) are non-punitive review prompts.

Detraining: one week off, no meaningful loss (often beneficial). Two weeks, aerobic fitness starts declining (VO2max about 7% by 12 days); strength retained. One month, VO2max down about 6%, small strength losses; in previously sedentary people cardiometabolic gains can return toward baseline in 2 to 4 weeks. Connective tissue re-adapts slower than muscle, so the first two weeks back are the highest injury risk. Design: step back modestly and temporarily, scaled to gap length, framed as easing back in.

Dropout: 40 to 65% within the first 5 to 8 months of a structured programme. Holidays, injury, illness, travel are the classic disruptors.

## Part 3. Behavioural science
Self-determination theory (Ryan and Deci): autonomy, competence, relatedness. Autonomy in interface terms: user-selected goals, editable recommendations, opt-in reminders, no forced paths. Competence: progress feedback combining encouragement with facts, graded difficulty. Relatedness: hardest for a no-cloud app; met by the doctor summary and human copy, not social features. Competitive metrics, rankings, and surveillance-style tracking are perceived as controlling and generate anxiety.

Habit formation: the 21-day rule is a myth (Maltz 1960). Lally et al. 2010 (n=96, 12 weeks): time to 95% of automaticity ranged 18 to 254 days, median 66; the curve is asymptotic; missing a single day did not derail habit formation. Design: frame over months, reassure that missed days don't reset, anchor to an existing routine.

Fogg Behavior Model: behaviour happens when motivation, ability, and a prompt converge; motivation is the least reliable lever; make the behaviour tiny and anchor it. Troubleshoot prompt, then ability, then motivation.

Behavioural economics: endowed progress (Nunes and Dreze 2006: pre-stamped card 34% completion versus 19%) is ethical only if the progress is honest. Goal gradient runs off remaining distance; frame steps to go. Loss aversion and sunk cost are dark patterns in health; owned exportable data is the ethical version. Commitment devices only when user-chosen and non-punitive.

Motivational interviewing: elicit change talk, roll with resistance, affirmations over praise ("you chose to move today, even when it was hard" over "great job").

ACT: values-based action, cognitive defusion, psychological flexibility. Anchor reflection in what movement let you do, not the number.

Progress principle and small wins: visible, credible progress motivates; tracking backfires through obsessive checking and shame during plateaus. Surface process progress (showed up), capability progress (a walk got easier, a test improved), and outcome (weight) separately.

Autonomy-supportive language: a JMIR 2019 2x2 experiment (n=526) found providing choice reliably improved evaluation; wording alone had weaker effects. Offer genuine choices, and prefer "may" and "could" over "should" and "must."

Notifications: 46% would disable if an app sends 2 to 5 messages in a week; about 32% uninstall at more than 6 (Localytics). A health RCT found adaptive timing did not beat simple daily timing. Ship off or minimal by default, per-type controls, copy under ten words, never disguised as urgent.

Exercise initiation in inactive or higher-weight people (Ekkekakis): exercise above the ventilatory threshold produces negative in-the-moment affect that predicts non-adherence; self-selected comfortable intensity predicts adherence better than intensity targets. Weight stigma in fitness contexts is a documented barrier. Self-efficacy builds through mastery experiences.

## Part 4. Retention
Benchmarks: health and fitness apps 3% day-30 retention (Business of Apps 2023); activation 26% day one dropping to 10% by day 28; "good" is 20% day 1, 7 to 8.5% day 7, 3.5 to 4% day 30. Apps that deliver value within about three minutes see about twice the retention. Day-one completion of a meaningful first action is the strongest predictor.

Drop-off: January effect, two-week wall, six-week wall, post-plateau abandonment (thin direct quantification; industry-sourced). Re-engagement: well-timed, low-frequency, relevant prompts work; guilt and loss framing drive uninstalls. Data investment plausibly supports retention; direct causal evidence is limited. Design first for the returning occasional user: shame-free re-entry, surviving trend, gently reset walks. Onboarding: value-first, first meaningful action under a minute, progressive disclosure. Privacy positioning: strong evidence users say they value it; little direct evidence it improves retention; treat as a trust and acquisition story.

## Recommendations
Stage 0, licensing hygiene: Vico, M3 Expressive, Phosphor plus Material Symbols, Open Peeps or Humaaans; in-app credits screen; Lottie for play-once, Rive for state-driven.
Stage 1, the engine: EWMA alpha 0.1, walls, normative tables labelled by validated age range, progression keyed to talk test and recent-longest-effort spikes, gap-aware reset, expected weight-loss curve shape.
Stage 2, behaviour: SDT as constitution, Fogg tiny anchored habits, MI and ACT copy, only honest behavioural-economics tools, notifications off by default with a ceiling.
Stage 3, retention: day-one aha within a minute, design for the returning user, owned exportable data, privacy as trust.

## Caveats
Verify every dependency licence at build time (especially Rive). Lottie file licensing is the trap. M3 Expressive APIs are experimental. Norms skew older adults. The Yang push-up finding is male-firefighter-specific. The 10% rule, 21-day habit, and streaks-boost-retention narratives are not supported. Two-week and six-week wall quantification, onboarding-abandonment percentages, data-investment causality, and privacy-improves-retention are industry-sourced and directional only.
