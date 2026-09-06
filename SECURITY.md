# Security

## Reporting something

Report a vulnerability privately, not as a public issue.

Use GitHub's private reporting on this repository (Security, then Report a
vulnerability), or email hello@kamsiob.com.

One person maintains this. You should get an acknowledgement within a week. If a
fix is needed it will be written, released, and credited to you unless you would
rather not be. If a report turns out not to be a vulnerability you will get a
plain explanation of why rather than silence.

## What is worth reporting

Steady Health makes a small number of specific promises, and anything that breaks
one of them is worth a report even if it is not exploitable:

- Data leaving the device. The app has no server and no analytics. Any outbound
  connection other than the one opt-in model download is a bug of this kind.
- The database being readable. It is SQLCipher, keyed from the Android Keystore.
  A path that leaves it, or the passphrase, readable off the device matters.
- Camera frames being written to storage. During a check the camera counts
  repetitions in memory and nothing is saved. A frame reaching disk is a serious
  bug.
- Delete not deleting. "Gone. There is no copy anywhere else." is on a screen. A
  file, a key, or a row that survives it is a broken promise.
- A permission the app asks for that it does not need, or a dependency that
  introduces one.

## What is out of scope

Anything requiring a rooted device, physical access with the screen unlocked, or
a compromised operating system. Those are real, and this app cannot defend
against them, which is why it does not claim to.

## Supported versions

The latest release. This is a one person project and there is no back-porting.
