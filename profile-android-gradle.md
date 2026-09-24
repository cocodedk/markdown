# Profile: android-gradle

A campaign links to this profile when it builds an Android app with Gradle. The loop carries the
file; it reads the fields, it does not know what they mean.

## proof_command

    ./gradlew testDebugUnitTest --rerun --tests '<class>'

Never `-q`: at quiet level the console carries no per-test failure text.
Add `--rerun` when the gate reads the report: the builder has often run the same tests already,
and an `UP-TO-DATE` task writes no new report, so the gate would judge a stale one.

## suite_command

    ./gradlew --no-daemon testDebugUnitTest --continue

The whole JVM suite: the one check every change must pass. Failing test names are in the
reports below; `--continue` runs every test instead of stopping at the first failing task.
`--no-daemon`: each check runs in a throwaway home, so a daemon could never be reused and would
only be left running after the check ends.

## build_command

    ./gradlew --no-daemon assembleDebug

## artifact

    app/build/outputs/apk/debug/app-debug.apk

## report

    app/build/test-results/testDebugUnitTest/TEST-<class>.xml

The failing test names and their exception types live here, not in the console.

## paths_the_gate_needs

    $ANDROID_HOME          the SDK, wherever the machine keeps it
    $GRADLE_USER_HOME      a writable cache directory, outside any masked home
    Robolectric's jars     `-Drobolectric.offline=true -Drobolectric.dependency.dir=<dir>` in
                           JAVA_TOOL_OPTIONS, the android-all jars copied there: it reads ~/.m2
    $ANDROID_USER_HOME     a writable directory holding a copy of the debug.keystore the owner's
                           own builds sign with (by default ~/.android/; match fingerprints with
                           `apksigner verify --print-certs`): without it the build signs with a
                           throwaway key, and its APK cannot update the installed app. This hands
                           the build's code that debug key; a dedicated per-project debug key
                           avoids it, at the cost of one reinstall (which clears the app's data)

A gate box that hides the user's home hides both. The SDK is read-only; the cache must be writable
and should survive between gates, or every gate downloads the toolchain again.

## machine_is_ready

    ./gradlew --version

Fails when the JDK is a JRE, when the SDK is unreachable, or when the wrapper cannot download.

## the_machine_not_the_card

    SDK location not found
    does not provide the required capabilities
    Daemon startup failed
    Could not start ... daemon
    Unable to locate a Java Runtime
    Cannot allocate memory

A gate ending with one of these is the machine's fault. The card keeps its status and its rounds.

## after_a_gate

The build leaves a daemon alive per gate, and each holds a gigabyte or more. They must be closed
with the gate that started them, or the machine runs out of memory and the next card is blamed.

## red_first

A judge is red when every case in the report fails with `NotImplementedError` from a `TODO()` body.
Count the failures in the XML, not in the console.
A gate that expects a failure message prints the report's `<failure message=...>` lines itself
(e.g. `grep -o '<failure message="[^"]*"' "$report"`): the console never carries them, and the loop
looks for the expected message in what the gate prints.

That is the whole red gate: run the class with `--rerun` (its failing exit is expected), require its
report, print the failure messages, and check that the named tests fail with their messages while the
rest pass. No mutant implementations, no probe scripts, no exit-code bookkeeping: a longer gate breaks
on its own machinery.

A lasting gate (what a kept judge keeps checking) runs only its own tests, with Gradle's method filter
(`--tests '<class>.<method>'`, one per test), and requires them to pass —
never the class's test count or its total failures. Later cards add tests to the same class, some red
on purpose, and a gate that pins the whole class forbids them.

## pixels

A test that reads pixels (`captureToImage`, a colour at a point) under Robolectric needs
`@GraphicsMode(GraphicsMode.Mode.NATIVE)` on its class; without it every capture times out, even the
control. The annotation lives in the test file, so no build file changes.
Even so, capture can time out on a new SDK; prefer a pixel-free proof — a semantics property
such as a state description — whenever the spec names one.

## house

The JDK is not pinned in the repository, no toolchain resolver is declared, the manifest gains no
permission, and release keeps R8 and resource shrinking on — an F-Droid build must stay reproducible.
