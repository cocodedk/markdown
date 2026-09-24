# Markdown Viewer

A small, fast Android app that opens Markdown files and shows them nicely.

- Opens from any app: tap a `.md` file in a file manager, a mail or a chat and pick Markdown Viewer.
- GitHub-flavoured Markdown (tables, strikethrough, autolinks, task lists) in light and dark.
- No permissions and no network.

## Install

Download `MarkdownViewer.apk` from the latest release:
<https://github.com/cocodedk/markdown-viewer/releases/latest/download/MarkdownViewer.apk>

## Building

    ./gradlew assembleDebug

The APK lands in `app/build/outputs/apk/debug/`. The build needs a full JDK 21 or newer and the Android SDK.

## Testing

    ./gradlew testDebugUnitTest

Every test runs on the JVM; Robolectric hosts the Android and Compose tests, so no emulator is needed.

## Layout

| Path | Contents |
|---|---|
| `app/src/main/java/dk/cocode/markdownviewer/render` | Markdown text to an HTML page. Pure functions, no Android imports. |
| `app/src/main/java/dk/cocode/markdownviewer/ui` | The activity, Compose screens, the WebView host. |
| `app/src/test/java/dk/cocode/markdownviewer` | The tests, mirroring the layout above. |
| `spec/brief.md`, `specs/` | The product brief and one spec per feature. |
| `.github/workflows` | CI (`ci.yml`) and the manual release (`release-apk.yml`). |
| `.githooks`, `scripts` | Git hooks and the owner's setup scripts. |
| `fastlane/metadata` | F-Droid store metadata. |

## License

Apache-2.0. See [LICENSE](LICENSE).

Made by Babak Bandpey at [Cocode](https://cocode.dk).
