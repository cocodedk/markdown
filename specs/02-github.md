# 02 — Everything the public repository needs

The repository is already public at `https://github.com/cocodedk/markdown-viewer` and `main` is
pushed. Add the files the repository and its releases need, and the scripts the owner runs. Do not
publish anything: no `git push`, no `gh` call, no network. The owner runs the scripts.

Identity (fixed): author Babak Bandpey, company [Cocode](https://cocode.dk), GitHub owner
`cocodedk`, security contact babak@cocode.dk. No website: the README is the front page.

## Public files

- `README.md`: what it is (one line, then three bullets: opens from any app, GitHub-flavoured
  Markdown in light and dark, no permissions and no network), install (the latest release's
  `MarkdownViewer.apk`, at `https://github.com/cocodedk/markdown-viewer/releases/latest/download/MarkdownViewer.apk`),
  building (`./gradlew assembleDebug`), testing (`./gradlew testDebugUnitTest`), the layout,
  license, and "Made by Babak Bandpey at [Cocode](https://cocode.dk)".
- `LICENSE` (Apache-2.0, copyright 2026 Babak Bandpey), `CONTRIBUTING.md`, `SECURITY.md` (report to
  babak@cocode.dk), `.gitattributes` (`* text=auto`, LF for `*.sh` and `gradlew`, CRLF for `*.bat`,
  binary for `*.jar` and `*.png`), `llms.txt` (the project in a few lines, with links to the README,
  the brief and the specs).
- `.github/`: a pull-request template, bug and feature issue templates, and `dependabot.yml`
  (github-actions and gradle, weekly).
- F-Droid metadata in `fastlane/metadata/android/en-US/`: `title.txt`, `short_description.txt`
  (under 80 characters), `full_description.txt`, `changelogs/1.txt`.

## Workflows

Pin every action by full commit SHA, with the version in a comment, exactly these:
`actions/checkout@9c091bb21b7c1c1d1991bb908d89e4e9dddfe3e0 # v7.0.0`,
`actions/setup-java@1bcf9fb12cf4aa7d266a90ae39939e61372fe520 # v5.4.0` (temurin 17, gradle cache),
`actions/upload-artifact@ea165f8d65b6e75b540449e92b4886f43607fa02 # v4`,
`softprops/action-gh-release@718ea10b132b3b2eba29c1007bb80653f286566b # v3.0.1`.

- `.github/workflows/ci.yml`, job id `verify`: on pull requests to `main` and pushes to any branch,
  `./gradlew --no-daemon assembleDebug testDebugUnitTest lintDebug`. It may cancel superseded runs.
- `.github/workflows/release-apk.yml`: manual `workflow_dispatch` only (a tag trigger would re-fire
  on its own tag), `permissions: contents: write`, concurrency group `release` with
  `cancel-in-progress: false`, full-depth checkout. It fails with a clear message naming any of the
  secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` that is missing; reads
  `VERSION_NAME` from `gradle.properties`, refuses a non-SemVer value and a version whose `v` tag
  already exists; decodes the keystore and checks it with `keytool -list` for the alias before
  building; runs `assembleRelease` with the four signing variables set; copies the APK to
  `MarkdownViewer.apk`; verifies it with the SDK's newest `apksigner verify --print-certs`; always
  removes the keystore; uploads the APK as an artifact; and creates the release and its tag
  `v<VERSION_NAME>` on `github.sha` with generated notes and `MarkdownViewer.apk` attached.

## Git hooks

Kept light, because the loop commits through them and Gradle is not available there:
`.githooks/pre-commit` refuses a staged `*.keystore`, `*.jks`, `keystore.properties` or
`local.properties`, and a staged code file (`.kt`, `.kts`, `.xml`, `.toml`, `.yml`, `.sh`) over
200 lines; `.githooks/commit-msg` requires a Conventional Commit subject
(`type(scope)?: text`, types feat, fix, docs, chore, refactor, test, ci, build, perf, style,
revert) and lets merge and revert messages through; `.githooks/pre-push` refuses a push URL outside
`github.com/cocodedk/` and a force-push or deletion of `main`. `scripts/install-hooks.sh` makes them
executable and sets `core.hooksPath` to `.githooks`. Files are committed without the executable bit
(the builder cannot set it); every script is run as `sh scripts/<name>.sh` and works in POSIX sh.

## Owner scripts

- `scripts/setup-repo.sh`, safe to run again. It names the repository once, as `cocodedk/markdown-viewer`,
  and never reads or trusts an existing remote. It sets the description, topics (android, markdown,
  markdown-viewer, kotlin, jetpack-compose, f-droid) and merge settings (squash and rebase only,
  delete branch on merge); installs the hooks; and, once CI has run, protects `main` (pull request
  required with 0 approvals, the `verify` check required, no force-push, no deletion, and **admins
  not enforced**, so the owner can still push the loop's landings to `main`). It prints each step
  and never uses `--force` or `--no-verify`.
- `scripts/setup-signing.sh`: reuses the owner's shared key at `$HOME/release.keystore` (override
  with `KEYSTORE_FILE`, alias `KEYSTORE_ALIAS`, default `android`), or generates one with `keytool`
  when it is missing; asks for the passwords without echoing them and restores the terminal on
  exit; uploads the four secrets to `cocodedk/markdown-viewer` with `gh secret set`, never printing
  a password or the keystore.

## Done when

A JVM test (`RepoFilesTest`, reading the files from the project directory, no network) checks:
every file above exists; each workflow uses only the four pinned SHAs above; `ci.yml` has a
`verify` job; `release-apk.yml` has `workflow_dispatch` and no `push` or `tags` trigger,
`cancel-in-progress: false`, and `MarkdownViewer.apk`; `install-hooks.sh` sets `core.hooksPath`;
`pre-push` names `cocodedk` and has no `<OWNER>` placeholder; `setup-repo.sh` contains no `--force`
and no `--no-verify`; `short_description.txt` is under 80 characters; and no tracked code file is
over 200 lines. The whole suite stays green.
