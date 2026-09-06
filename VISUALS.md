# VISUALS.md: the exercise animation pipeline, deferred to version 2

Version 1 ships with no exercise animations. Every exercise in the library has a written setup line, two or three form cues, and a stop rule, and those must stand alone well enough that a person can do the movement safely from the words. Do not build placeholder art. Do not use stock photography. Do not generate anything at build time.

This file records the pipeline that was tested and works, so version 2 can be produced without rediscovering it.

## The approach that was chosen, and why
Three routes were costed. Licensing an existing open database fails: free-exercise-db (Unlicense, 800 exercises) and wger (per-exercise Creative Commons) are gym-centric, with almost nothing chair-based, bed-based, or for a walker or wheelchair, and their photography is young athletic people in gyms, which is the wrong signal for this audience. Otago's illustrations are clinically right but the drawings are copyrighted to their creators and free-to-download for clinical use is not a licence to redistribute in an app; the exercises themselves can be implemented because a movement is not copyrightable.

Commissioned Rive animation costs roughly $1,500 to $3,000 for about 100 movements and gives exact joint control.

Generated video from a single style reference costs a Gemini Pro subscription and a few days, and was tested successfully in September 2026. That is the chosen route for version 2, with hand-animation reserved for any movement the generator cannot get right.

## The figure
One reference still per view, generated once and reused for every clip so the same body appears throughout. Three references are needed: standing front, standing side (profile movements are most of the library), and seated in a chair. A lying figure is needed for the bed set.

The prompt that produced the approved standing figure:

```
A single stylized human figure standing in a neutral A-pose,
front view, full body, centered.

Style: flat vector illustration. Solid fill shapes with no
outlines, no gradients, no shading, no texture. Rounded joints
and rounded limb ends. Simplified anatomy with correct human
proportions.

The figure has no face and no facial features. The head is a
plain rounded shape. No hair detail. No clothing detail: the
body reads as one continuous silhouette with a simple short-
sleeve top and knee-length shorts suggested only by two flat
color blocks.

Body type: an average adult around sixty, softly built,
slightly heavier through the middle, not athletic and not
thin. Natural posture.

Colors: figure body in deep navy #1E2A5A. The top block in
warm orange #F5843E. The shorts block in sand #F6E7D3. Pure
white background #FFFFFF, completely flat and empty.

Composition: the whole figure fits with even margin on all
sides. Feet flat, weight even. Lit evenly with no cast shadow
anywhere.

No text, no logo, no props, no floor line, no perspective.
```

Two fixes to apply when regenerating: add "hands are simple rounded mitten shapes with no separate fingers" (fingers turn to mush in motion) and thicken the arms slightly relative to the torso.

For the side view, append: "Same figure, viewed from the left side, standing neutral. Arms hanging at the side, one arm visible. Head, torso, hips, knees and feet all in true profile with no rotation toward the camera."

## The movement clips
Image-to-video, four seconds, using the matching reference still as the image input. Draft on the cheapest tier to find the wording, render once on the standard tier.

The pattern every movement prompt follows, in this order:
1. Name the view.
2. Describe one complete cycle in order, ending back at the exact starting position.
3. Give the timing and the loop instruction: "The last frame must match the first frame."
4. List what must not move and what must not happen. This block is where the exercise's form cues and stop rule go, so the prompt and the safety copy are written together.
5. Restate the style constraint from the reference.
6. Lock the camera and clear the background.

A worked example, the standard chair stand:

```
Animate the figure in the reference image performing a chair
stand, viewed from the side.

The movement, one complete cycle: the figure begins seated on
a plain chair, back straight, arms crossed over the chest,
feet flat on the ground. It leans the torso slightly forward,
pushes through both feet, and rises to fully standing with
legs straight. It then lowers back down with control and
returns to exactly the seated starting position.

Timing: rise over about 1.5 seconds, lower over about 1.5
seconds, ending in the seated position it started from. The
last frame must match the first frame.

Both feet stay flat and still on the ground the entire time.
The arms stay crossed and do not push off. Knees track
forward over the toes and never bend inward or outward.

Keep the exact style of the reference image: flat vector
shapes, no outlines, no shading, no face, same colors. Add a
simple flat chair in sand #F6E7D3 with no legs detail beyond
a plain shape.

Pure white background, completely flat and empty. Camera is
locked, side view, no movement, no zoom, no pan. No text, no
other objects, no shadows.
```

And the wall push-up, which tests a rigid whole-body movement:

```
Animate the figure in the reference image performing a wall
push-up, viewed from the side.

The movement, one complete cycle: the figure stands facing a
plain flat wall on the right side of the frame, arms extended
straight out at chest height with both palms flat against the
wall, feet together about two feet back from the wall so the
body leans slightly forward. It bends both elbows and lowers
its chest toward the wall until the elbows are bent about
ninety degrees, then pushes back to fully straight arms and
returns to exactly the starting position.

Timing: lower over about 1.5 seconds, push back over about
1.5 seconds, ending in the position it started from. The last
frame must match the first frame.

The torso and legs move as a single rigid unit. The body
stays in one straight line from head to heels, hinging only
at the ankles. The back does not arch, the hips do not sag or
lift, and the head stays in line with the spine. Heels stay
flat on the ground and the feet do not move. Elbows bend
backward and slightly outward, never flaring straight out to
the sides.

Keep the exact style of the reference image: flat vector
shapes, no outlines, no shading, no face, same colors. The
wall is a plain flat vertical block in sand #F6E7D3 running
the full height of the frame on the right.

Pure white background, completely flat and empty. Camera is
locked, side view, no movement, no zoom, no pan. No text, no
other objects, no shadows.
```

## Rules for whoever produces the rest
Two lines carry the most weight and must appear in every prompt: "The last frame must match the first frame" and "Camera is locked." Without the first there is no clean loop; without the second the clip drifts and the drift reads as a bug when it repeats.

The floor rise is the hardest movement in the library and the one where wrong form matters most. Test it early. If it fails after ten or so attempts, animate it and any other complex transfer by hand and generate only the simple ones.

Name every file by the exercise id used in the library so the files drop straight in: sit_to_stand_standard.mp4, wall_pushup.mp4, ankle_pumps.mp4.

Every clip is reviewed by a physical therapist before it ships, in one batch, against the form cues already written for that exercise. A wrong joint angle in a demonstration is a real problem regardless of how it was made.

Budget the file size: about 100 clips of MP4 is 50 to 100 MB in the app. If that is too much for the minimum device, convert the final approved clips to Rive or to a short WebP sequence, or download them on first use of each exercise rather than bundling them.

## What version 1 must do instead
Every exercise entry carries: a one-line setup ("Chair against a wall, arms crossed, feet flat"), two or three form cues written as what to do rather than what to avoid, a stop rule ("Stop if anything hurts"), and an easier and a harder variant. Those strings are the same ones that become the "must not" block of the animation prompt later, so writing them well now is not wasted work.
