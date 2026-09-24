# Markdown Viewer — working notes

A small, fast Android app that opens and shows Markdown files. Application id and namespace
`dk.cocode.markdownviewer`. The product brief is `spec/brief.md`; each feature the loop builds is
one file in `specs/`. Do not edit `spec/` or `specs/` from a feature: they are the owner's.

The machine-level mechanics (the suite, the build, the artifact, the paths a check needs) are in
[profile-android-gradle.md](profile-android-gradle.md).

## Commands

| What | Command |
|---|---|
| Debug build | `./gradlew assembleDebug` |
| All JVM tests | `./gradlew testDebugUnitTest` |
| Lint | `./gradlew lintDebug` |

Every test runs on the JVM. Robolectric hosts the Android and Compose tests, so no emulator and
no `adb` is needed for a check.

## Layout

`app/src/main/java/dk/cocode/markdownviewer/`

| Package | Contents | Android imports? |
|---|---|---|
| `render` | Markdown text to an HTML page. Pure functions. | no |
| `ui` | The activity, Compose screens, the WebView host. | yes |

Tests live in `app/src/test/java/dk/cocode/markdownviewer/`, mirroring that layout.
`render/**` imports nothing from `android.*` or `androidx.*`.

## House rules

- Every code file under 200 lines (Kotlin, Gradle, XML, TOML, YAML, shell).
- Every behaviour a feature adds has a test. Pixels are not tested; the state and the HTML that
  drive them are.
- F-Droid readiness: no permissions in the manifest, no JDK toolchain pin, no foojay,
  `dependenciesInfo` off, literal `VERSION_NAME`/`VERSION_CODE` in `gradle.properties`, R8 and
  resource shrinking on in release, and only free-licensed dependencies.
- The Gradle daemon needs a full JDK. If this machine's default `java` is a JRE, set
  `org.gradle.java.home` in `~/.gradle/gradle.properties`, never in the repo.
