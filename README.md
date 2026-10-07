# Markdown

A small, fast Android app that opens Markdown files, shows them nicely and lets you edit them.

## Install

<!-- cocode-apps:install:start -->
- Coming to F-Droid
- [Download the APK from GitHub](https://github.com/cocodedk/markdown/releases/latest/download/Markdown.apk)
- [Auto-update the GitHub APK with Obtainium](https://apps.obtainium.imranr.dev/redirect?r=obtainium://add/https://github.com/cocodedk/markdown)
<!-- cocode-apps:install:end -->

## Features

- Opens from any app: tap a `.md` file in a file manager, a mail or a chat and pick “Markdown”.
- GitHub-flavoured Markdown (tables, strikethrough, autolinks, task lists) in light and dark.
- Edit the source and save it back, or start a new file with New.
- No permissions and no network.

## Website

- [English](https://markdown.cocode.dk/)
- [فارسی (Persian)](https://markdown.cocode.dk/fa/)

## Privacy

Markdown asks for no permissions and never goes online, so remote images stay unloaded. Nothing in a
document runs as a script. There are no accounts.

## Build

    ./gradlew assembleDebug

The APK lands in `app/build/outputs/apk/debug/`. The build needs a full JDK 21 or newer and the Android SDK.

### Test

    ./gradlew testDebugUnitTest

Every test runs on the JVM; Robolectric hosts the Android and Compose tests, so no emulator is needed.

## Layout

| Path | Contents |
|---|---|
| `app/src/main/java/dk/cocode/markdown/render` | Markdown text to an HTML page. Pure functions, no Android imports. |
| `app/src/main/java/dk/cocode/markdown/ui` | The activity, Compose screens, the WebView host. |
| `app/src/test/java/dk/cocode/markdown` | The tests, mirroring the layout above. |
| `spec/brief.md`, `specs/` | The product brief and one spec per feature. |
| `.github/workflows` | CI (`ci.yml`), the manual release (`release-apk.yml`) and the site deploy (`deploy-pages.yml`). |
| `website` | The project site at <https://markdown.cocode.dk>, English and Persian. `og.png` is rendered from `og-image.html`. |
| `.githooks`, `scripts` | Git hooks and the owner's setup scripts. |
| `fastlane/metadata` | F-Droid store metadata. |

## Contributing

Setup, the build and test commands, code style and the pull request checklist are in
[CONTRIBUTING.md](CONTRIBUTING.md). Bugs and ideas go to the
[issues page](https://github.com/cocodedk/markdown/issues).

## License

Apache-2.0. See [LICENSE](LICENSE).

Made by Babak Bandpey at [Cocode](https://cocode.dk).
