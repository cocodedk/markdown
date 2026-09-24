# Contributing

Thanks for helping. Keep changes small and focused.

1. Install the hooks once: `sh scripts/install-hooks.sh`.
2. Build and test before opening a pull request:

       ./gradlew assembleDebug testDebugUnitTest lintDebug

3. Every behaviour you add has a test. Tests run on the JVM (Robolectric), no emulator.
4. Keep every code file under 200 lines; split into helpers when it grows.
5. `render/` stays pure Kotlin: no `android.*` or `androidx.*` imports.
6. No permissions, no network, and only free-licensed dependencies, so the app stays F-Droid ready.
7. Commit subjects follow [Conventional Commits](https://www.conventionalcommits.org/):
   `type(scope): text`, with type one of feat, fix, docs, chore, refactor, test, ci, build, perf,
   style, revert.

Open a pull request against `main`. CI must be green before it merges.

By contributing you agree that your work is licensed under the Apache License 2.0.
