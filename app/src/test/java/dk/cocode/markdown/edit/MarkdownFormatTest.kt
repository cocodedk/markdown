package dk.cocode.markdown.edit

import org.junit.Assert.assertEquals
import org.junit.Test

class MarkdownFormatTest {

    private fun apply(format: Format, text: String, start: Int, end: Int = start) =
        MarkdownFormat.apply(format, Edit(text, start, end))

    @Test
    fun `bold wraps the selection and keeps it selected`() {
        assertEquals(Edit("say **hi** now", 6, 8), apply(Format.BOLD, "say hi now", 4, 6))
    }

    @Test
    fun `bold on a bare cursor leaves the cursor between the markers`() {
        assertEquals(Edit("a****", 3), apply(Format.BOLD, "a", 1))
    }

    @Test
    fun `bold again removes the markers around the selection`() {
        assertEquals(Edit("say hi now", 4, 6), apply(Format.BOLD, "say **hi** now", 6, 8))
    }

    @Test
    fun `bold on a selection that includes the markers removes them`() {
        assertEquals(Edit("say hi now", 4, 6), apply(Format.BOLD, "say **hi** now", 4, 10))
    }

    @Test
    fun `italic, strikethrough and code use their own markers`() {
        assertEquals("_x_", apply(Format.ITALIC, "x", 0, 1).text)
        assertEquals("~~x~~", apply(Format.STRIKE, "x", 0, 1).text)
        assertEquals("`x`", apply(Format.CODE, "x", 0, 1).text)
    }

    @Test
    fun `a backwards selection is treated as forwards`() {
        assertEquals(Edit("**hi**", 2, 4), apply(Format.BOLD, "hi", 2, 0))
    }

    @Test
    fun `link wraps the selection and puts the cursor where the address goes`() {
        val out = apply(Format.LINK, "see docs", 4, 8)
        assertEquals("see [docs](https://)", out.text)
        assertEquals(Edit(out.text, 19), out)
    }

    @Test
    fun `link with nothing selected puts the cursor in the brackets`() {
        assertEquals(Edit("[](https://)", 1), apply(Format.LINK, "", 0))
    }

    @Test
    fun `heading cycles through three levels and back to plain`() {
        var e = Edit("Title", 2)
        val seen = (1..4).map { e = MarkdownFormat.apply(Format.HEADING, e); e.text }
        assertEquals(listOf("# Title", "## Title", "### Title", "Title"), seen)
        assertEquals(2, e.start)
    }

    @Test
    fun `heading changes only the cursor's line`() {
        assertEquals("one\n# two\nthree", apply(Format.HEADING, "one\ntwo\nthree", 5).text)
    }

    @Test
    fun `bullet marks every selected line and a second press removes the marks`() {
        val on = apply(Format.BULLET, "a\nb\nc", 0, 3)
        assertEquals("- a\n- b\nc", on.text)
        assertEquals("a\nb\nc", MarkdownFormat.apply(Format.BULLET, on).text)
    }

    @Test
    fun `numbered counts from one across the selected lines`() {
        assertEquals("x\n1. a\n2. b", apply(Format.NUMBERED, "x\na\nb", 2, 5).text)
    }

    @Test
    fun `a list kind replaces another and keeps the indent`() {
        assertEquals("  - [ ] a", apply(Format.TASK, "  1. a", 0).text)
        assertEquals("> a", apply(Format.QUOTE, "- a", 0).text)
        assertEquals("- a", apply(Format.BULLET, "- [x] a", 0).text)
    }

    @Test
    fun `the cursor moves with the text a line prefix adds`() {
        assertEquals(Edit("- hello", 5), apply(Format.BULLET, "hello", 3))
        assertEquals(Edit("hello", 0), apply(Format.BULLET, "- hello", 1))
    }
}
