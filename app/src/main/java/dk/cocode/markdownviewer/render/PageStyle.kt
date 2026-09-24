package dk.cocode.markdownviewer.render

/** The inline stylesheet of a rendered page, in a light and a dark palette. */
object PageStyle {

    data class Palette(
        val background: String,
        val text: String,
        val muted: String,
        val codeBackground: String,
        val border: String,
        val link: String,
    )

    val LIGHT = Palette(
        background = "#ffffff",
        text = "#1f2328",
        muted = "#59636e",
        codeBackground = "#f0f2f5",
        border = "#d1d9e0",
        link = "#0969da",
    )

    val DARK = Palette(
        background = "#121417",
        text = "#e6e8eb",
        muted = "#9198a1",
        codeBackground = "#23272e",
        border = "#3d444d",
        link = "#58a6ff",
    )

    fun css(dark: Boolean): String {
        val p = if (dark) DARK else LIGHT
        return """
            html { -webkit-text-size-adjust: 100%; }
            body {
              margin: 0; padding: 16px;
              background: ${p.background}; color: ${p.text};
              font-family: sans-serif; font-size: 17px; line-height: 1.6;
              overflow-wrap: break-word;
            }
            h1, h2, h3, h4, h5, h6 { line-height: 1.25; margin: 1.2em 0 0.5em; }
            h1, h2 { border-bottom: 1px solid ${p.border}; padding-bottom: 0.3em; }
            a { color: ${p.link}; }
            code, pre { font-family: monospace; font-size: 0.9em; background: ${p.codeBackground}; }
            code { padding: 0.15em 0.35em; border-radius: 4px; }
            pre { padding: 12px; border-radius: 6px; overflow-x: auto; }
            pre code { padding: 0; background: none; }
            table { display: block; overflow-x: auto; border-collapse: collapse; margin: 1em 0; }
            th, td { border: 1px solid ${p.border}; padding: 6px 12px; }
            th { background: ${p.codeBackground}; }
            blockquote { margin: 1em 0; padding: 0 1em; color: ${p.muted}; border-left: 4px solid ${p.border}; }
            hr { border: none; border-top: 1px solid ${p.border}; }
            li > input { margin-right: 0.4em; }
            img { display: none; }
        """.trimIndent() + "\n"
    }
}
