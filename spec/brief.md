# Brief: Markdown Viewer

Owner: Claude, for Babak Bandpey ([Cocode](https://cocode.dk)). Written 2026-09-24.

## What it is

An Android app that opens a Markdown file and shows it nicely, at once. Tap a `.md` file in a
file manager, a mail or a chat, pick "Markdown Viewer", read it. Or open the app and pick a file.
It only views: no editing, no accounts, no network.

## What the owner will accept at the end

1. It opens Markdown from other apps and from its own Open button, and renders CommonMark with
   GitHub's extensions (tables, strikethrough, autolinks, task lists) readably, in light and dark.
2. It is safe: no permissions, no network, no scripts run from a document.
3. It is a public GitHub repository, `cocodedk/markdown-viewer`, with green CI, hooks, the usual
   public files, and a release workflow that builds the APK.

## Features, in build order

1. `specs/01-viewer.md`: the Gradle project and the viewer.
2. `specs/02-github.md`: everything the public repository needs.
3. `specs/03-reading-comfort.md`: right-to-left text, the share sheet, zoom.

## Left out on purpose

Editing, a file browser, recent files, search, themes beyond light/dark, remote images, a website.
Add them only when someone asks.
