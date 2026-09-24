package dk.cocode.markdownviewer.render

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownRendererTest {

    private fun html(markdown: String, dark: Boolean = false) = MarkdownRenderer.render(markdown, dark)

    private fun count(haystack: String, needle: String) = haystack.windowed(needle.length).count { it == needle }

    @Test
    fun `the page is a complete html document`() {
        val page = html("hello")
        assertTrue(page.startsWith("<!DOCTYPE html>"))
        assertTrue(page.contains("<meta charset=\"utf-8\">"))
        assertTrue(page.contains("<meta name=\"viewport\""))
        assertTrue(page.contains("<style>"))
        assertTrue(page.contains("<body>\n<p dir=\"auto\">hello</p>"))
    }

    @Test
    fun `a heading renders`() {
        assertTrue(html("# Title").contains("<h1 dir=\"auto\">Title</h1>"))
    }

    @Test
    fun `a table renders`() {
        val page = html("| a | b |\n|---|---|\n| 1 | 2 |")
        assertTrue(page.contains("<table>"))
        assertTrue(page.contains("<th dir=\"auto\">a</th>"))
        assertTrue(page.contains("<td dir=\"auto\">2</td>"))
    }

    @Test
    fun `strikethrough renders`() {
        assertTrue(html("~~gone~~").contains("<del>gone</del>"))
    }

    @Test
    fun `a bare url is autolinked`() {
        val page = html("see https://example.com now")
        assertTrue(page.contains("href=\"https://example.com\">https://example.com</a>"))
    }

    @Test
    fun `task items render checked and unchecked`() {
        val page = html("- [x] done\n- [ ] todo")
        assertEquals(2, count(page, "type=\"checkbox\""))
        assertEquals(1, count(page, "checked"))
        assertTrue(page.contains("done"))
        assertTrue(page.contains("todo"))
    }

    @Test
    fun `a fenced code block renders`() {
        val page = html("```kotlin\nval x = 1 < 2\n```")
        assertTrue(page.contains("<pre><code"))
        assertTrue(page.contains("val x = 1 &lt; 2"))
    }

    @Test
    fun `persian and emoji text stay intact`() {
        val text = "سلام دنیا 😀🚀"
        assertTrue(html(text).contains(text))
    }

    @Test
    fun `raw html is escaped as text`() {
        val page = html("before\n\n<script>alert(1)</script>\n\nand <b>inline</b>")
        assertFalse(page.contains("<script"))
        assertFalse(page.contains("<b>"))
        assertTrue(page.contains("&lt;script&gt;alert(1)&lt;/script&gt;"))
    }

    @Test
    fun `a javascript link target is removed`() {
        val page = html("[click](javascript:alert(1))")
        assertFalse(page.contains("javascript:"))
        assertTrue(page.contains("click"))
    }

    @Test
    fun `an image shows as its alt text and is never fetched`() {
        val page = html("![a *cat*](https://example.com/cat.png)")
        assertTrue(page.contains("<em>[image: a cat]</em>"))
        assertFalse(page.contains("<img"))
        assertFalse(page.contains("cat.png"))
    }

    @Test
    fun `an image without alt text shows as image`() {
        val page = html("![](cat.png)")
        assertTrue(page.contains("<em>[image]</em>"))
        assertFalse(page.contains("<img"))
    }

    @Test
    fun `the dark flag changes background and text colours`() {
        val light = html("x", dark = false)
        val dark = html("x", dark = true)
        assertNotEquals(PageStyle.LIGHT.background, PageStyle.DARK.background)
        assertNotEquals(PageStyle.LIGHT.text, PageStyle.DARK.text)
        assertTrue(light.contains("background: ${PageStyle.LIGHT.background}; color: ${PageStyle.LIGHT.text};"))
        assertTrue(dark.contains("background: ${PageStyle.DARK.background}; color: ${PageStyle.DARK.text};"))
        assertFalse(dark.contains(PageStyle.LIGHT.background))
    }
}
