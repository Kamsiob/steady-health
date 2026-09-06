# Start here

This folder is the complete handoff for Steady Health by Kamsiob. Unzip it inside the project folder you open Claude Code in, so these files sit at the top level.

It replaces an earlier weight-tracking version of the app. The prompt tells Claude Code to wipe the old local files and rewrite the GitHub repository to match. Nothing from the old version is a requirement.

## To start
1. Connect the Pixel over USB with developer options and USB debugging on, unlocked, nearby for the one-time authorisation prompt.
2. Open Claude Code in this folder with permissions bypassed for an unattended run.
3. Open MASTER_PROMPT.md, copy everything below the line, paste it, send.
4. Approve the device authorisation prompt when it appears. Otherwise leave it alone.

## What the app is now
Steady Health keeps a record of what your body can do, and what changes it. Four abilities lead: Get up, Go, Carry, Steady. Weight is one of the levers, not the score. You say what you would like to be able to do, in your own words, and the on-device model turns that into the things the app tracks. Once a month the phone times and counts a short check and reports the result as life, not seconds. Staying the same counts as a result. Four versions of the app cover being on your feet, using a walker or cane, using a wheelchair, or being mostly in bed for now.

## What is in here
- MASTER_PROMPT.md: the reset instruction and the kickoff prompt.
- MASTER_SPEC.md: what the app is, why capability and not weight, every feature, the phases.
- DESIGN.md: the approved look, every token, every component, the voice, the banned words, the rejected directions.
- LOGIC.md: every rule and threshold the app decides with.
- AI.md: the three visible AI features and the five model jobs with their contracts.
- ONBOARDING.md: the first run, screen by screen, and how the app teaches itself.
- CONTENT.md: twelve hand-written cards, final copy.
- PRIVACY.md: the privacy policy, final. Local only.
- COMPLIANCE.md: Play, FDA wellness, FTC, the PAR-Q+ licence, and what the capability frame specifically requires.
- VISUALS.md: why version 1 has no exercise animations, and the tested pipeline for version 2.
- DECISIONS.md: what was already decided, and the BLOCKED list that needs you.
- design/: the current 22-screen grid, the approved look, and the old grid for reference.
- research/: four evidence reports.
- standards/: your project template and handoff prompts, unchanged.

## What still needs you (also in DECISIONS.md under BLOCKED)
Publishing PRIVACY.md at kamsiob.com. The Play Console manual steps. The Rive runtime licence check. Reviewing the twelve cards. And the most valuable thing: using the app yourself once it installs, and saying what is wrong.

## Version 2, later
Exercise animations. The pipeline is written up in VISUALS.md with the prompts that worked, so it is a weekend of generation and a physical therapist review, not a rediscovery.
