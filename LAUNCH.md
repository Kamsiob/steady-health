# Launching Steady Health

Everything between a finished build and an app somebody can install from Google Play.
Written for the owner, in order, with the parts only the owner can do marked.

Nothing in this file contains a secret, and nothing you do while following it should
put one in the repository. The repository is public.

## 1. The signing key. Owner only, once, and irreversibly.

Google Play signs what it serves, but the bundle you upload is signed by you, and the
key you create here is the only proof for the life of the app that an upload is from
you. Lose it and you cannot update this app, ever, under this package name.

Create it outside the repository:

    mkdir -p ~/.steady
    keytool -genkeypair -v \
      -keystore ~/.steady/steady-release.jks \
      -alias steady \
      -keyalg RSA -keysize 4096 -validity 10000 \
      -storetype PKCS12

Then write the properties file the build reads, also outside the repository:

    cat > ~/.steady/keystore.properties <<'EOF'
    storeFile=/home/YOU/.steady/steady-release.jks
    storePassword=...
    keyAlias=steady
    keyPassword=...
    EOF
    chmod 600 ~/.steady/keystore.properties

`app/build.gradle.kts` looks for that file at `~/.steady/keystore.properties`, or
wherever `STEADY_KEYSTORE_PROPERTIES` points. There is no file inside the repository to
edit, so there is nothing to commit by accident. A machine without the file still
builds, tests and installs; it just cannot produce something Play will accept, which is
the correct outcome.

**Back the keystore up somewhere that is not this machine and not this repository**,
before you upload anything. A password manager attachment or an encrypted drive. Do
this now rather than after the first release, because after the first release it is
the only copy of something irreplaceable.

Opt in to Play App Signing when you create the app entry. It means Google holds the
key that signs what users install, and yours becomes the upload key, which can be
reset if it is lost. This is the one thing that makes losing the key survivable, and
it has to be chosen at the start.

## 2. The version

`app/build.gradle.kts`, `versionCode` and `versionName`. Play refuses a `versionCode`
it has seen. Raise it by one for every upload including the ones you throw away in
internal testing.

The first public release is `versionCode = 1`, `versionName = "1.0"`.

## 3. Build the bundle

    source ./gradle-env.sh
    ./gradlew clean :app:bundleRelease

The file is `app/build/outputs/bundle/release/app-release.aab`.

Check it is signed with your key and not the debug key:

    jarsigner -verify -verbose -certs app/build/outputs/bundle/release/app-release.aab | head -20

If it says `CN=Android Debug`, the properties file was not found and the build fell
back. Fix that before uploading; Play will reject it anyway, but later and less
clearly.

## 4. Before you upload, run all of it

    source ./gradle-env.sh
    ./gradlew :app:testDebugUnitTest :app:detekt :app:lintRelease
    python3 tools/banned-words.py
    ./tools/device-tests.sh

Then install the release build on the phone and go through TEST-ME.md end to end. A
release build is minified and shrunk and a debug build is not, so a rule that keeps a
class alive is a thing you find here or in production, and nowhere in between.

Specifically check on the release build, because these are what minification breaks:
the database opens and old data is still there; a session runs and saves; the camera
reads a page; export produces a zip that opens; the widget draws.

## 5. The Play Console entry. Owner only.

Create the app: package `com.kamsiob.steadyhealth`, app not game, free, and opt in to
Play App Signing.

**Data safety.** Every answer is no. No data collected, no data shared, no data
transmitted off the device. Say that the app does not collect or share any user data,
that data is encrypted in transit (there is no transit), and that users can request
deletion (they delete it themselves, in two taps, and it is the only copy). This form
is short only because the app was built this way; do not soften any answer to fit a
category.

**Content rating.** The questionnaire will return Everyone. There is no violence, no
sex, no gambling, no user-generated content shared with anybody, no purchases, and no
advertising.

**Ads.** No.

**Target audience.** Adults. Not a children's app. Do not choose an age band that
would put it in Designed for Families.

**Health apps declaration.** Answer honestly and read Play's health policy. The app
keeps a record and leads exercise sessions. It does not diagnose, does not give
medical advice, does not name a condition, and does not interpret a clinical document.
COMPLIANCE.md is the document to have open while you fill this in.

**Store listing.** The copy is final in `store-assets/listing.md`. The first screenshot
is the session mid set. Take screenshots from the release build on the actual phone at
the default font scale.

## 6. Release in stages

Internal testing first, with the phone as the only tester. Then closed testing if you
want other people in it. Then production at a staged rollout, twenty percent, and
watch the crash rate for a few days before going to a hundred.

A staged rollout can be halted. A full rollout with a bad build in it cannot be taken
back, only replaced by a newer one that everybody has to receive.

## 7. Also, outside Play

- Publish PRIVACY.md at kamsiob.com with the same effective date it carries here.
  The listing links to it and Play checks that the link resolves.
- Print `store-assets/clinician-handout-a4.html` and the Letter version, on a real
  office printer, and check the ruled lines survive draft quality before you hand any
  out. Regenerate them with `python3 tools/handout.py` after any copy change.
- The QR code on the handout cannot be made until the listing URL exists. Generate it
  then, print it, and scan the printed page rather than the screen.

## What is still on the BLOCKED list at launch

These are in DECISIONS.md in full. In short, and all of them owner only:

- The signing keystore, section 1 above.
- The Play Console entry, the data safety form, and the content rating questionnaire.
- Publishing PRIVACY.md at kamsiob.com.
- The attorney review of the HAI-DEF boundary, which gates the optional report reading
  feature. It ships off. Nothing downloads and nothing reads a document until that
  review clears, and the code is written so that turning it on is a change to one set
  rather than a change to the app.
- Whether to request PAR-Q+ Collaboration consent. Default is no, and the app links out.
- Review of the health cards: an eating disorder professional for the two about food,
  a physical therapist for the ones about movement, exercise and recovery.
- The three translations. The app ships in English. The switching mechanism is built
  and the picker shows only languages that have real strings, so it shows nothing
  today and comes back on its own when a folder lands.

## What this file is not

It is not a checklist for shipping something unfinished. The gates in ADDENDUM-03
Part 21 are the check on whether the app is ready. This is the check on whether the
release is ready, which is a different question and a shorter one.
