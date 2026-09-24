# 03 — Reading comfort: right-to-left text, the share sheet, zoom

Found by using the 0.1.0 preview: a Persian paragraph is aligned left like English, a file shared
from a chat or mail app (Share, not Open with) never reaches the app, and small text cannot be
enlarged. The first heading also starts far below the top bar.

## What it must do

1. **Direction per block.** Every block the renderer emits (paragraphs, headings, list items,
   block quotes, table cells) carries `dir="auto"`, so a block whose first strong character is
   right-to-left (Persian, Arabic, Hebrew) reads and aligns right to left, and English stays left
   to right, in the same document. Code blocks and inline code stay left to right.
2. **Share target.** The activity also accepts `ACTION_SEND` for `text/markdown`,
   `text/x-markdown` and `text/plain`:
   - with `EXTRA_STREAM` (a `content` URI), it opens that file exactly as `ACTION_VIEW` does
     (same size limit, same messages);
   - with only `EXTRA_TEXT`, it renders that text as the document, titled with `EXTRA_SUBJECT`
     when present, otherwise "Shared text";
   - with neither, it shows the could-not-open message.
3. **Zoom.** The page can be pinch-zoomed (built-in zoom on, the on-screen zoom buttons hidden),
   and the page's viewport allows it (no `user-scalable=no`, no `maximum-scale=1`).
4. **No gap at the top.** The first element of the page has no top margin.

## Done when

JVM tests show: a Persian paragraph and an English paragraph each rendered with `dir="auto"`, a
heading, list item, block quote and table cell with `dir="auto"`, and a fenced code block without
it; `ACTION_SEND` with a registered `EXTRA_STREAM` URI ending with that file's HTML, with only
`EXTRA_TEXT` ending with that text's HTML and the subject as title (and "Shared text" without
one), and with neither ending with the could-not-open message; the WebView the app builds having
built-in zoom on and zoom buttons hidden; the page's viewport meta containing neither
`user-scalable=no` nor `maximum-scale=1`; and the first element's top margin set to 0 in the CSS.
The whole suite stays green.
