# 01 — The Gradle project and the viewer

The repository holds only the Gradle wrapper (`gradlew`, `gradle/wrapper/`), the owner's files and
`.gitignore`. This feature adds the whole Android project and the viewer.

## The project

- One `:app` module, Kotlin, Jetpack Compose with Material 3, a version catalog in
  `gradle/libs.versions.toml`. Keep exactly these versions (the machine has them cached, and the
  test runner's offline Android jars match SDK 36): Gradle 9.3.1 (the wrapper as it is), AGP 9.1.1
  with its built-in Kotlin, Kotlin 2.2.10 (compose compiler plugin), Compose BOM 2026.02.01,
  core-ktx 1.10.1, activity-compose 1.8.0, lifecycle 2.8.7, JUnit 4.13.2, Robolectric 4.16.1,
  androidx.test core 1.6.1. `compileSdk` and `targetSdk` 36, `minSdk` 26, JVM target 17.
- Markdown: `org.commonmark:commonmark` 0.30.0 with `commonmark-ext-gfm-tables`,
  `commonmark-ext-gfm-strikethrough`, `commonmark-ext-autolink` and `commonmark-ext-task-list-items`,
  all 0.30.0 (BSD-2-Clause).
- `rootProject.name = "MarkdownViewer"`, application id and namespace `dk.cocode.markdownviewer`,
  app label "Markdown Viewer", `VERSION_NAME=0.1.0` and `VERSION_CODE=1` as literals in
  `gradle.properties`, read by `app/build.gradle.kts`.
- Release: R8 and resource shrinking on, `dependenciesInfo` off, and signing only when the four
  environment variables `KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` are all
  set and the keystore exists; otherwise the release APK is unsigned.
- `testOptions { unitTests.isIncludeAndroidResources = true }`.
- An adaptive launcher icon drawn as vectors: a document with a bold "M↓" mark.
- The manifest requests **no permissions**. The WebView never needs the network.

## The viewer

1. **Opening.** The one activity opens a document from:
   - an `ACTION_VIEW` intent whose type is `text/markdown`, `text/x-markdown` or `text/plain`
     (intent filters for the `content` scheme; the activity is exported for them);
   - its **Open** button, which launches the system picker (`ActivityResultContracts.OpenDocument`)
     for `text/*` and `application/octet-stream`, since many pickers label `.md` that way;
   - a new `ACTION_VIEW` intent arriving while the app is open, which replaces the document.
2. **Reading.** The file is read as UTF-8 off the main thread. A file larger than 2 MB is not
   rendered; the screen says it is too large. A URI that cannot be opened or read shows a short
   message saying the file could not be opened, and the Open button stays available.
3. **Rendering** is a pure function in `render/`: Markdown text and a `dark` flag in, one complete
   HTML page out (`<!DOCTYPE html>`, `<meta charset="utf-8">`, a viewport meta, inline CSS, the
   body). CommonMark plus the four extensions above.
   - Raw HTML in the document is **escaped**, shown as text, never interpreted
     (`escapeHtml(true)`), and unsafe link targets are removed (`sanitizeUrls(true)`).
   - An image is never fetched: it renders as its alt text in italics, in brackets
     (`[image: alt]`), or `[image]` when the alt text is empty.
   - The CSS is readable on a phone: a sans-serif body at a comfortable size and line height,
     monospace code with a tinted background and horizontal scrolling for wide code blocks and
     tables, bordered table cells, quoted blocks with a left rule, images none. `dark = true` gives
     light text on a dark background, `dark = false` the reverse; the flag comes from the system's
     dark theme.
4. **Showing.** The page is shown in a `WebView` loaded with `loadDataWithBaseURL(null, html,
   "text/html", "utf-8", null)`. Its settings: JavaScript off, file access off, content access off,
   network loads blocked. A tapped link with an `http`, `https` or `mailto` target opens outside the
   app through an `ACTION_VIEW` intent (nothing happens if no app can open it); every other target,
   including a fragment-only `#link`, does nothing. The WebView never navigates away from the page.
5. **Screen.** A top bar shows the document's display name (from `OpenableColumns.DISPLAY_NAME`),
   or "Markdown Viewer" when there is none, and an Open action. With no document yet, the screen
   shows a short line of text and a large Open button. The shown document survives rotation
   (a `ViewModel` holds the URI and the rendered page).

## Done when

JVM tests (JUnit and Robolectric; pixels and WebView drawing are not tested, the HTML handed to the
WebView is) show:

- the renderer: a heading, a table, strikethrough, an autolinked URL, a checked and an unchecked
  task item, a fenced code block, Persian and emoji text kept intact in UTF-8, a `<script>` in the
  document escaped as text, a `javascript:` link target removed, an image shown as `[image: alt]`
  with no `<img`, and the `dark` flag changing the page's background and text colours;
- an `ACTION_VIEW` intent for a `content` URI registered with Robolectric's content resolver ends
  with the screen state holding the HTML of that file; a URI that fails to open ends with the
  could-not-open message; content over 2 MB ends with the too-large message;
- the Open button launches an `ACTION_OPEN_DOCUMENT` intent for `text/*` and
  `application/octet-stream`;
- the WebView the app builds has JavaScript, file access and content access off and network
  loads blocked, and its client opens an `https` link through an `ACTION_VIEW` intent, ignores a
  `#fragment` link, and never lets the WebView navigate;
- the installed package requests no permissions.

`./gradlew --no-daemon testDebugUnitTest` and `./gradlew --no-daemon assembleDebug` pass.
