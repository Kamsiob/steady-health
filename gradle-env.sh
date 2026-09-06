#!/usr/bin/env bash
# Source this before any Gradle command: `source ./gradle-env.sh`.
#
# The system JDK is 26 and the Android Gradle Plugin does not run on it. JDK 21
# is the version AGP 9 targets, and it is the version CI uses, so both agree.
export JAVA_HOME=/home/linuxbrew/.linuxbrew/opt/openjdk@21/libexec
export PATH="$JAVA_HOME/bin:$PATH"
export ANDROID_HOME="$HOME/Android/Sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
