# DESIGN.md: Steady Health by Kamsiob

This document is the binding visual and voice specification. Where code and this document disagree, this document wins. It was approved by the owner on September 6, 2026 after ten rejected rounds; the rejected directions are listed at the end so nobody drifts back to them. Three HTML files sit beside it. design/today-approved-look.html is the moment the look was approved, two states. design/screen-grid-v2-capability.html is the current 22-screen grid and the one to build. design/screen-grid-v1-weight-reference.html is the earlier weight-first grid, kept only because several screens (weigh in, say how today went, months, settings) carry over unchanged; nothing in it is a requirement. When a screen is not in the grid, build it from the rules here and it will belong.

## 1. The look in one paragraph
Headspace's confidence with colour and its friendly, rounded type, with the illustration built from shapes instead of characters. Sunrise orange and deep navy carry the app. Colour blocks do the organising, so each of the three daily things has its own field and you can find it without reading. Big radii everywhere, consistently. Illustration is a sun, a moon, hills, an arc, a dotted path, a speech bubble with two dots. No faces, no doodles, no cartoon people, no stick figures, and never a hand-drawn silhouette. Warm without being cute.

## 2. Tokens

### Colour
| Token | Hex | Job |
|---|---|---|
| white | #FFFFFF | cards, done state |
| ground | #FBF8F3 | screen background |
| ink | #1F2340 | body text |
| ink2 | #5C6180 | secondary text |
| ink3 | #9A9DB3 | captions, disabled |
| orange | #F5843E | the walk, the weight line, primary action, morning sky |
| orange-d | #E86A22 | morning hills, pressed state |
| orange-l | #FFB27A | sun outer ring |
| peach | #FFD9BF | sun inner ring |
| navy | #1E2A5A | headings, where you are, evening sky, primary action on dark |
| navy-l | #2E3D7D | evening hills |
| sky | #7FA6F2 | the talk glyph |
| sky-l | #DCE7FC | the talk card tint |
| sand | #F6E7D3 | the weigh card tint, soft blocks |
| butter | #FFD766 | milestones, the moon, the avatar |
| plum | #B48BC8 | the Steady ability, only |
| plum-t | #F4E9F7 | the Steady ability tile |
| green | #2E8B62 | done, only |
| green-l | #DDF0E4 | the move card tint, done card outline |

Rules. Orange is the walk and the weight line and the primary button. Navy is where you are and any hero that is not the time-of-day sky. Green means done and nothing else. Butter marks a milestone. Sand, sky-l, and green-l sort the three daily things (weigh, talk, move) and never change jobs. The four abilities have their own fixed tints and never swap: Get up is sand, Go is green-l, Carry is sky-l, Steady is plum-t. There is no red anywhere in the app; a deadline or a warning uses navy text on sand. Nothing turns a different colour because a number went the wrong way.

Dark theme is not designed yet. Do not invent one; ship light only and record it in DECISIONS.md.

### Type
Figtree, bundled, Open Font Licence. Weights 400, 500, 600, 700, 800, 900.
| Role | Size | Weight | Tracking |
|---|---|---|---|
| Hero number | 64 | 900 | -0.05em |
| Hero headline (welcome) | 38 | 900 | -0.04em |
| Screen title (big) | 26 | 900 | -0.04em |
| Greeting | 28 | 800 | -0.035em |
| Section title | 17 | 800 | -0.02em |
| Block value | 24 | 900 | -0.04em |
| Card title | 14 | 800 | -0.02em |
| List item heading | 15 | 800 | -0.02em |
| Body | 14 | 400 to 600 | 0 |
| Caption | 12 | 600 | 0 |
| Button | 16 | 800 | -0.01em |
Nothing in between. Tabular figures wherever money or measurements appear. Sentence case everywhere. No uppercase labels. No italics.

### Radius
Hero 30. Cards and blocks 24. List items 22. Glyph tiles 16. Buttons, pills, tags fully round. No other values exist.

### Spacing
18 px side padding on every screen. 12 px between blocks. 16 to 18 px inside blocks. Section titles sit 6 px above their block. Nothing floats in a half-empty screen: the layout fills to the tab bar or the primary button, or the empty space has a reason (the welcome hero is tall on purpose).

### Glyphs and illustration
Glyphs are 52 px in a card, 28 px inside a 44 px tile in a list. Every glyph is a circle, an arc, a dotted path, a bubble, or a rounded rectangle. The three daily glyphs are fixed: a filled dot in a sand circle (weigh), a sky speech bubble with two white dots (talk), a green dotted arc ending in a dot (move). The hero art is a sun (two concentric circles) or a moon (one butter circle with three tiny stars) over two hills (two overlapping curves). People, when needed for empty states, are Open Peeps (CC0), drawn at a range of body sizes, never anything generated in code.

### Icons
Phosphor (MIT) for in-app icons, filled weight in the tab bar and duotone or regular elsewhere; Material Symbols (Apache-2.0) as fallback. Never Inter, never emoji as UI.

## 3. Components
Ability tile. Four in a two-by-two grid on Today. 24 radius, tinted by ability, 120 px tall, a 40 px glyph top-left, the ability name 15/900 navy, and one sentence about life 12/600. An optional white pill top-right for "New".
Ability row. In the Abilities tab. 22 radius white, a 44 px tinted glyph tile, name 15/800, one line 12.5/600, and a pill on the right reading Better (green-l on green) or Same (sand on navy). Same is never styled as lesser.
Life card. The result of a check or the top of an ability page. White, 26 radius, a 52 px tinted glyph on the left, a 17/900 sentence about what the person can now do, and a 13/600 line saying what it was before.
Rating row. Ten flat 26 px blocks, filled orange to the person's rating, with the number and its previous value underneath. Used for the person's own list.
Timing card. Navy, 30 radius, the instruction 13/700 at 85% opacity, the count at 76/900, the unit and countdown 13/600, and a row of small circles that fill as reps are counted.
Counting view. Near-black gradient, 30 radius, the count at 96/900 white, the label under it, a chip top-left reading "On this phone. Nothing recorded.", and the pose skeleton drawn in orange at the bottom.
Experiment card. Sky-l, 26 radius, an eyebrow 12.5/700 in deep blue, the observation 18/900 navy, the evidence line 13/600.

Hero. 30 radius, colour block (orange by day, navy by night on Today and Welcome back; navy everywhere else), sun or moon and hills as SVG behind, label 14/700 at 92% opacity, number 64/900, one explanatory line 13.5/500, one glass tag (white at 22% opacity, full round).
Daily card. Three across, 24 radius, 150 px tall, tinted (sand, sky-l, green-l), glyph top-left, title and one-line subtitle bottom-left. Done: white with a green-l outline and a green check disc top-right.
Walk module. Navy, 26 radius, dotted path decoration at low opacity on the right, label plus value on the left, orange Go pill on the right. Evening or done variant: white with a sand outline.
Block. 24 radius, white or tinted, label 12.5/700 ink2, value 24/900 navy. Two-up grid for pairs.
List item. 22 radius white, 44 px glyph tile, heading 15/800, subtitle 12.5/600, optional value 15/900 on the right. Done: green-l outline. Next: navy background with white text.
Buttons. Primary: orange, full round, 16 px padding, 16/800 white. Secondary: white with a 2 px sand inset outline, navy text. Navy variant for dark contexts.
Pills and tags. White with sand outline; selected is navy with white text. Tags in the vocabulary grid: white with sand outline; selected is orange with white text; suggested-but-unselected at 55% opacity.
Segmented control. White track, 4 px padding, selected segment navy.
Energy and talk selectors. Three equal cells, white with sand outline; selected is butter (energy) or green-l with a green outline (talk test).
Dial. 230 px, sand track 22 px, orange arc, white knob with orange ring. Number 54/900 navy in the middle.
Moon slider. Sand arc, orange filled portion, white knob with orange ring, value 22/900 under the arc.
Week row. Seven 26 px circles with day letters under; walked is orange, today is white with a 2.5 px navy ring, empty is #EDE7DE.
Path (months). A curving 12 px #EDE7DE path, 7 px orange dots for walked days, 10 px butter dots for milestones, 4 px #E5DED2 for empty days, a sand sun top-right, three small labels for the walks reached.
Curve band (weight). A sand band drawn from the expected weight-loss shape (fast, then slower, flat around six months), a dashed orange-l midline, the user's line in navy at 4 px with a white-ringed navy endpoint. Axis labels: Start, 6 months, 1 year.
Range band (waist). 12 px track, green-l zone to 0.5, sand zone 0.5 to 0.6, plain beyond; a 4 px navy marker with the value above it.
Tab bar. White, 82 px, hairline top #EFEAE2, three items, filled Phosphor icons 26 px over 11/700 labels; active is navy, inactive ink3. A 5 px navy home indicator.
Postcard. White, 26 radius, orange band with a sun and hills 110 px tall, week label 20/900 white bottom-left, body 14.5/400 ink.
Doc page (doctor summary). White, 20 radius, 11.5 px rows with hairlines, values 800 navy.

## 4. Screen anatomy
Every content screen: a 44 px top row (round back button with a 2 px sand inset outline, title 20/800 navy, optional orange-d text action on the right), then blocks on the 12 px grid, then the primary button pinned in a 8/18/14 footer, then the tab bar where the screen is a tab root. Today and Welcome back replace the top row with the date (13/600 ink3), the greeting (28/800 navy), and a butter avatar disc.

## 5. Motion
Material 3 Expressive springs, standard scheme. The daily card morphs from its tint to white and the check disc scales in when done; no confetti, no sound by default. The Go pill scales on press. The hero sky crossfades at dawn and dusk over two seconds. Rive for the pieces that react to state (done morph, Go, the walk marker); Lottie only for play-once art the project authored. All motion stops under the system reduce-motion setting.

## 6. Voice
The app describes; the person concludes. Every sentence passes one test: would a friend say this out loud. The app may not use a word it has not taught; the first time an idea appears, the sentence carries its own explanation, and later it shortens.

Words that do not exist in the app: rung, tier, trail, trend, postcard, story, check-in, streak, score, goal, target, calories, burn, earn, cheat, fail, should, must, senior, elderly, frail, frailty, fall risk, decline, sarcopenia, patient, diagnosis, prescribe.
Words that do: walk, where you started, months, your weight smoothed, your week written up, say how today went, done, that counts, get up, go, carry, steady, better, same, what you said you want.

The ability sentence pattern, used everywhere a measure is reported: what you can do now, then what it was. "You got off the floor without your hands this month. In March you used a chair." Never the instrument's language, never the seconds first.

Fixed strings (final copy, do not paraphrase):
- Welcome: "Notice what changes. Never get graded." / "Your weight, your walks, and how your days go. All of it stays on your phone."
- Weight label: "Your weight, smoothed." First-week line: "Smoothed means one heavy morning doesn't move it." Daily line: "The scale said 228.1 today."
- Weigh-in note: "Smoothed weight stays at 226.4. Morning weight moves a couple of pounds with water and salt. That's normal, and it's why the app shows the smoothed number first."
- Daily cards: "Weighed in" / "Say how today went" with "Talk or type, 20 seconds" / "Move for two minutes" with "A short walk counts."
- Tags: "Here's what I picked up. From what you said. Tap anything that's wrong, or add one."
- Talk test: "Could you have held a conversation?" with "Yes, easily" / "Just about" / "No".
- Done: "Done. That counts." Affirmation pattern: "You walked today, on a day you said was busy." Never "great job."
- Next walk offer: "[Walk] is ready when you are." with "Try the longer one" / "Not yet". Never "you should."
- Sore rule: "If you're sore tomorrow, the app will suggest an easier walk. That's normal."
- Welcome back: "Your weight line is still here. So are your walks." Never mention a streak or a gap beyond its length.
- Plateau: "A flat stretch. That's the body doing what bodies do."
- Same: "Same is a result. Holding a number for a year is something most people don't do." Never "no change," never "stalled."
- Decline, once only: "Standing up has taken a little longer each month since June. Three months in one direction is worth a mention at your next appointment." Then, in the same view: "Everything else held." No colour change, no alarm, no repetition.
- Counting: "On this phone. Nothing recorded."
- Data: "Gone. There is no copy anywhere else." 
- Support link label, always: "Support this work."

Numbers-off mode replaces every figure with a direction word ("a little lower than last month") app-wide, hides the destination line, and removes the axis from charts.

## 7. Accessibility floor
Body text never below 14 px; all text passes 4.5:1 on ground and on white; every colour state carries a shape or a word (green check for done, a ring for next, the word "done"); tap targets at least 44 dp; every drag has a tap alternative; TalkBack labels describe meaning, not drawing ("smoothed weight 226.4, a little lower than a month ago"); the tab bar has labels; nothing is conveyed by colour alone.

## 8. Building a screen that is not in the grid
1. Start from the anatomy in section 4.
2. Choose one hero or one big object at most. If the screen is a form, no hero.
3. Use only the components in section 3. If a new one seems necessary, write it into this file first with tokens, then build it.
4. Every string passes the friend test and uses no word from the banned list.
5. Check the grid file for the nearest sibling screen and match its rhythm.
6. Render on device and compare against design/steady-today-approved.html for weight, spacing, and warmth before committing.

## 9. Rejected directions (do not return to these)
Cream ground with sage or forest green and a serif display. Dark charcoal ground with an amber lamp. Purple or blue-to-purple gradients. Inter. Glassmorphism as the main surface. Identical grey-shadowed card grids. Colour-blocked poster screens in saturated tangerine, cobalt, raspberry. Spatial desk or sticky-note metaphors. Chat-thread layouts. A breathing pebble or any mascot. Hand-drawn or parametric human silhouettes. Floating capsule navigation with oversized circles. Icon-only navigation. Uppercase eyebrow labels. Monospace data labels. Any screen that is a sentence over a card over a button with nothing to look at.
