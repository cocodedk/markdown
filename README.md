# Markdown

An Android app for reading, editing and saving Markdown files.

## Install

<!-- cocode-apps:install:start -->
- Coming to F-Droid
- [Download the Android installation file (APK) from GitHub](https://github.com/cocodedk/markdown/releases/latest/download/Markdown.apk)
- [Add the app to Obtainium, an app that keeps it up to date](https://apps.obtainium.imranr.dev/redirect?r=obtainium://add/https://github.com/cocodedk/markdown)
<!-- cocode-apps:install:end -->

## Features

- Tap a `.md` file in a file manager, email app or chat app and choose “Markdown” if it is offered. If it is not, start Markdown and tap Open to choose the file.
- Shows tables, strikethrough and task lists, and turns web addresses into clickable links. Follows your phone's light or dark theme.
- Edit the text and save it back, or start a new file with New.
- No permissions and no network.

## Website

- [English](https://markdown.cocode.dk/)
- [Dansk (Danish)](https://markdown.cocode.dk/da/)
- [فارسی (Persian)](https://markdown.cocode.dk/fa/)

## Privacy

Markdown asks for no permissions and cannot connect to the internet, so remote images stay unloaded.
A web link you tap opens in your browser and an email link in your mail app, never inside Markdown. Nothing in a document runs as a script.
There are no accounts.

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
| `website` | The project site at <https://markdown.cocode.dk>, English, Danish and Persian. `og.png` is rendered from `og-image.html`. |
| `.githooks`, `scripts` | Git hooks and the owner's setup scripts. |
| `fastlane/metadata` | F-Droid store metadata. |

## Contributing

Setup, the build and test commands, code style and the pull request checklist are in
[CONTRIBUTING.md](CONTRIBUTING.md). Bugs and ideas go to the
[issues page](https://github.com/cocodedk/markdown/issues).

## License

Apache-2.0. See [LICENSE](LICENSE).

Made by Babak Bandpey at [Cocode](https://cocode.dk).
