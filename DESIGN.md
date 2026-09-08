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

### Colour that carries text
Section 7 requires every piece of text to pass 4.5:1 on ground and on white, and
four of the tokens above cannot do that while staying the colour they are. The
resolution is that the table above gives fill, line and illustration colours, and
text takes a slightly darker member of the same hue. Nothing in the table changes,
and no new hue is introduced.

| Token | Hex | On white | On ground | Job |
|---|---|---|---|---|
| ink3-text | #6C708F | 4.83 | 4.55 | captions and the provenance line, wherever ink3 would have been the text colour |
| orange-text | #BD5114 | 4.83 | 4.56 | orange words, including the Summary text action |
| green-text | #287855 | 5.37 | 5.07 | the word done, and the Better pill (4.52 on green-l) |
| hill-front | #C45414 | white 4.54 | | the front hill of the morning hero |
| hill-back | #A54511 | white 6.07 | | the back hill, which is what the hero's words sit on |

The morning hero needs its own two, for the same reason. The render draws the
hills at #E86A22 and #D65A16 and puts white body text at 13.5/500 on top of them,
which is 3.22:1 and 3.94:1 and fails. The sky above stays the approved orange; only
the two hills darken, and the back one darkens further so the two still read as two.

One fill changes with it. The primary button is white at 16/800 on orange, which
is 2.54:1 and fails even the 3.0:1 that Android allows for text at that size and
weight. The button's fill is **orange-d #E86A22**, already in the table, which
gives 3.22:1. Orange #F5843E keeps every one of its other jobs: the walk, the
weight line, the morning sky, the dial arc.

ink3 itself stays exactly as it is wherever it is a shape rather than a word.

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
Doc page (the measures table). White, 20 radius, 11.5 px rows with hairlines, values 800 navy.
Window pill row. Three pills, 3 months / 6 months / All, 6 months selected by default. Standard pill component, no new tokens.
Ability strip. The four abilities as one white block, 24 radius, four rows separated by hairlines, each with its 32 px tinted glyph, the ability name 14/800, and its pill on the right. A compressed ability row, reusing the same tints. Same is styled identically to Better in weight and size; only the pill colour differs: Better is green-l on green, Same is sand on navy, and Quieter is also sand on navy, because Quieter must not read as an alarm.
Summary card. White, 26 radius, 18 px padding, three paragraphs at 15/400 ink, 1.5 line height, 10 px between them. Words quoted from the person are navy 500, never italic. No heading above it; the page title already says what it is.
Question list. A sand block, 24 radius, headed "Things you might ask about" 15/800 navy. Each question is a row: a small navy circle bullet, the question 14.5/600 navy, and under it the evidence line 12.5/600 ink2. With no candidates the whole block is replaced by one line in ink2.
Provenance line. 12/600 ink3, centred, at the foot of a page that was written rather than recorded. It says where the words came from and that nothing was sent.

## 4. Screen anatomy
Every content screen: a 44 px top row (round back button with a 2 px sand inset outline, title 20/800 navy, optional orange-d text action on the right), then blocks on the 12 px grid, then the primary button pinned in a 8/18/14 footer, then the tab bar where the screen is a tab root. Today and Welcome back replace the top row with the date (13/600 ink3), the greeting (28/800 navy), and a butter avatar disc.

**The four tabs**, from ADDENDUM-03 Part 20: Today, Sessions, Progress, You. Every
tab has the help dot in the same place, and every screen explains itself once.

## 4b. Help
From ADDENDUM-03 Part 4. Three layers, and no "I'm lost" button, because a permanent
one is an admission that the screen failed.

**L1, the screen explains itself once.** Every screen, on first open, one sand block
at the top with one sentence. Never a modal, never a tour, never a dark overlay. A
small dismiss, and it never returns. Today: "This is today. One session, and what the
app has noticed." Sessions: "Everything you can do, and everything you've done."
Progress: "What's changed since you started." You: "Your list, your settings, and how
the app works." Library: "Sixty or so movements. Tap any to read it, or start it now."

**L2, the help dot.** A "?" in the top right of every screen, the same place and the
same size, always. It opens a sheet: what this screen is for in two sentences, what
each thing on it means, and two or three likely questions with answers. Hand written
per screen, never generated. Dismissed by tapping anywhere.

**L3, every number explains itself.** Tap the info dot on any figure: "Chair stands:
how many times you stood up from a chair in thirty seconds. Yours have gone from nine
to fourteen since March." No number in this app is ever unexplained.

The first run of any feature gets one sand block explaining it in a sentence. Once.

Today's session card is never ambiguous: it always says what to do, how long it takes,
and has one obvious button. If a person could be lost on Today, that is a design bug
to fix rather than a help topic to write.

## 5. Motion
Replaced by ADDENDUM-03 Part 12, merged on commit 9837f2a. Flat shapes, big radii,
warm colour, no decoration. Motion follows the same rules. **Five movements and
nothing else.**

1. **Count up.** Any changing number animates old to new over 400ms with a spring.
2. **Fill.** Any bar or arc fills over 600ms, ease out.
3. **Morph.** Completion uses the Material 3 Expressive shape morph, squircle to
   cookie, 400ms spring. The app's one moment of delight, in the same shape language
   as everything else.
4. **Rise.** New content enters up 12dp and fades in over 250ms, staggered 40ms.
5. **Sun.** The hero's sun or moon drifts continuously, a 40 second cycle and 3dp of
   travel. Almost imperceptible, and it is what makes the app feel alive rather than
   printed.

Never: confetti, sparkles, bounce, rotation, parallax, sound effects, celebration
screens, characters, or any transition over 600ms.

In a session: the count springs on each rep, the ring fills as reps accumulate, the
rest countdown is a shrinking arc, and the next movement's name rises in as rest
begins.

**Reduce motion.** Numbers change instantly, fills are immediate, the morph becomes a
cross fade, and the sun stops. Nothing is lost.

## 5b. The session screens
From ADDENDUM-03 Part 1. Seven screens, and the fourth is the dominant screen of the
whole app.

**S1 the offer**, on Today: the session's name, its length, the movements in it, one
line saying what it feeds, and any adaptation sentence. Two actions, "Start" and
"Change it".
**S2 ready**, full screen: the movement's name, its setup line, the target, the stop
rule. One button, "I'm ready".
**S3 count in**: three, two, one, go, one second each, the number filling the screen.
**S4 the set, live**: an enormous count that climbs as reps are detected, the target
and the last result under it, a progress ring filling toward the target, and pause and
the three exits always visible. For untimed movements the same screen shows elapsed
time, and steps as well for a walk.
**S5 rest**: a shrinking arc rather than a ticking number, the next movement named
underneath, the set just finished shown with its number, and "Skip the rest".
**S6 between movements**: the same shape as S2, with the count-in skippable.
**S7 done**: what was done, each number against its last; "How did that feel?"; then
the next session on the same screen, already adjusted.

The count on S4 is the largest type in the app. It has to be readable from two feet
away by somebody standing up out of a chair, which is the whole reason the screen
exists.

## 6. Voice
The app describes; the person concludes. Every sentence passes one test: would a
friend say this out loud. The app may not use a word it has not taught; the first time
an idea appears, the sentence carries its own explanation, and later it shortens.

**Words that do not exist in the app:** rung, tier, trail, trend, postcard, story,
check-in, streak, score, goal, target, calories, burn, earn, cheat, fail, should,
must, senior, elderly, frail, frailty, fall risk, decline, sarcopenia, patient,
diagnosis, prescribe, report, assessment, evaluation, findings, results,
recommendations, and, added by ADDENDUM-03: test, workout, routine, level, unlock,
achievement, complete, missed, streak, hazard, risk.

"Plan" is permitted only for a therapist's plan. "Steady" means reliable, not stuck.

**The three registers.** Every copy template that comments on a change has three
versions and the engine picks one from the person's inferred direction. Rebuilding:
"You're doing more than you were a month ago." Keeping: "Same as last month, which
takes doing." Building: "Fourteen. That's four more than when you started." Banned in
all three: anything implying the ceiling is holding steady, anything implying
improvement is unlikely, anything congratulating somebody for not getting worse, and
anything treating maintenance as second best.

**Warmth, in exactly four places.** From ADDENDUM-03 Part 16.
1. The first session: "That's the first one."
2. The first month: a card in Progress with then and now for their own item.
3. Returning after two weeks or more: "Good to see you. Everything's still here."
4. A tracked item crossing its threshold: "You said you wanted to get off the floor
   without your hands. You just did it."

Everywhere else, plain. No congratulation for an ordinary session, no emoji, no
exclamation marks, no praise. Affirmations name what was done and never evaluate it.

## 7. Accessibility as design
Replaced by ADDENDUM-03 Part 17. This is not a checklist applied afterwards; it is
what the layout is for.

- **Tap targets:** 56dp minimum inside a session, 48dp everywhere else. The primary
  action is at least 64dp tall and spans the content width.
- **No gesture is ever the only way to do anything.**
- **Body text floor 16sp.** Correct layout at 200 percent font scale, tested rather
  than assumed.
- **Contrast** 4.5:1 for text and 3:1 for meaningful non-text, verified.
- Every state is carried by a shape or a word as well as by colour.
- **No time limits** except the rest between sets, which is skippable and extendable.
- No double-tap gestures. No two targets within 8dp. A 300ms debounce on anything
  destructive.
- **Haptics** on every rep, every set and every rest end, so a session works without
  sight or sound.
- Every session action is reachable by system voice control.
- TalkBack and Switch Access tested end to end.
- The primary action sits in the bottom third of the screen.
- **Storage accessibility:** the app is fully usable with no model installed, and the
  model screen never implies the person is missing out.

## 8. Building a screen that is not in the grid
1. Start from the anatomy in section 4.
2. Choose one hero or one big object at most. If the screen is a form, no hero.
3. Use only the components in section 3. If a new one seems necessary, write it into this file first with tokens, then build it.
4. Every string passes the friend test and uses no word from the banned list.
5. Check the grid file for the nearest sibling screen and match its rhythm.
6. Render on device and compare against design/steady-today-approved.html for weight, spacing, and warmth before committing.

## 9. Rejected directions (do not return to these)
Cream ground with sage or forest green and a serif display. Dark charcoal ground with an amber lamp. Purple or blue-to-purple gradients. Inter. Glassmorphism as the main surface. Identical grey-shadowed card grids. Colour-blocked poster screens in saturated tangerine, cobalt, raspberry. Spatial desk or sticky-note metaphors. Chat-thread layouts. A breathing pebble or any mascot. Hand-drawn or parametric human silhouettes. Floating capsule navigation with oversized circles. Icon-only navigation. Uppercase eyebrow labels. Monospace data labels. Any screen that is a sentence over a card over a button with nothing to look at.
