# tools

Small scripts the build needs. Each one does a single thing and says so when run
with no arguments.

## screenshot.sh

    tools/screenshot.sh 01-today

Captures one screenshot from the connected phone into `docs/screenshots/`. It
refuses unless Steady Health is the focused window, and refuses again if the
image comes back too small to be a real screen. Both checks are mechanical
because the failure they prevent, somebody's personal screen in a public
repository, cannot be undone by noticing afterwards.

Captures under `docs/screenshots/raw/` are gitignored. A screenshot that ships
is moved out of `raw/` deliberately, one at a time.

## exercise-visuals/

The pipeline for the version 2 exercise animations. Nothing here runs during a
version 1 build. See VISUALS.md.
