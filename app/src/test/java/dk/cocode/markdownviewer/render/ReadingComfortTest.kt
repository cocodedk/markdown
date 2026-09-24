package dk.cocode.markdownviewer.render

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingComfortTest {

    private fun html(markdown: String) = MarkdownRenderer.render(markdown, dark = false)

    @Test
    fun `a persian and an english paragraph each take their own direction`() {
        val page = html("سلام دنیا\n\nHello world")
        assertTrue(page.contains("<p dir=\"auto\">سلام دنیا</p>"))
        assertTrue(page.contains("<p dir=\"auto\">Hello world</p>"))
    }

    @Test
    fun `headings, list items, block quotes and table cells carry dir auto`() {
        val page = html("## عنوان\n\n- item\n\n> quote\n\n| a |\n|---|\n| ب |")
        assertTrue(page.contains("<h2 dir=\"auto\">عنوان</h2>"))
        assertTrue(page.contains("<li dir=\"auto\">item</li>"))
        assertTrue(page.contains("<blockquote dir=\"auto\">"))
        assertTrue(page.contains("<th dir=\"auto\">a</th>"))
        assertTrue(page.contains("<td dir=\"auto\">ب</td>"))
    }

    @Test
    fun `code blocks and inline code carry no dir and stay left to right`() {
        val page = html("```\nval x = 1\n```\n\nسلام `code`")
        assertTrue(page.contains("<pre><code>val x = 1"))
        assertTrue(page.contains("<code>code</code>"))
        assertFalse(page.contains("<pre dir"))
        assertFalse(page.contains("<code dir"))
        assertTrue(PageStyle.css(dark = false).contains("pre, code { direction: ltr; }"))
    }

    @Test
    fun `the viewport allows zoom`() {
        val viewport = Regex("<meta name=\"viewport\"[^>]*>").find(html("x"))!!.value
        assertFalse(viewport.contains("user-scalable=no"))
        assertFalse(viewport.contains("maximum-scale=1"))
    }

    @Test
    fun `the first element has no top margin`() {
        for (dark in listOf(false, true)) {
            assertTrue(PageStyle.css(dark).contains("body > :first-child { margin-top: 0; }"))
        }
    }
}
