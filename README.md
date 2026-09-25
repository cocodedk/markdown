# Markdown

A small, fast Android app that opens Markdown files, shows them nicely and lets you edit them.

- Opens from any app: tap a `.md` file in a file manager, a mail or a chat and pick “Markdown”.
- GitHub-flavoured Markdown (tables, strikethrough, autolinks, task lists) in light and dark.
- Edit the source and save it back, or start a new file with New.
- No permissions and no network.

## Website

- [English](https://markdown.cocode.dk/)
- [فارسی (Persian)](https://markdown.cocode.dk/fa/)

## Install

Download `Markdown.apk` from the latest release:
<https://github.com/cocodedk/markdown/releases/latest/download/Markdown.apk>

## Building

    ./gradlew assembleDebug

The APK lands in `app/build/outputs/apk/debug/`. The build needs a full JDK 21 or newer and the Android SDK.

## Testing

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

## License

Apache-2.0. See [LICENSE](LICENSE).

Made by Babak Bandpey at [Cocode](https://cocode.dk).
