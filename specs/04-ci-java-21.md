# 04 — CI runs on Java 21

The first CI run on GitHub failed: every Robolectric test class stopped with
`UnsupportedOperationException at DefaultSdkProvider.java:170`, because Robolectric's Android 16
(SDK 36) runtime needs Java 21 and `specs/02-github.md` set up Java 17. Local checks pass only
because this machine's JDK is newer. This supersedes "temurin 17" in `specs/02-github.md`.

## What it must do

- Both `.github/workflows/ci.yml` and `.github/workflows/release-apk.yml` set up Temurin **21**
  with the same pinned `actions/setup-java` SHA. Nothing else in them changes.
- The build keeps its JVM bytecode target of 17 and still pins no JDK toolchain.
- `README.md`'s building section says JDK 21 or newer is needed.

## Done when

`RepoFilesTest` (or a test beside it) checks that every `actions/setup-java` step in both
workflows has `java-version: '21'` and that none names 17. The whole suite stays green.
