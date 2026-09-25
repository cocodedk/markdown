package dk.cocode.markdown.edit

/** Text with a selection from [start] to [end] (equal for a bare cursor). */
data class Edit(val text: String, val start: Int, val end: Int = start)

/** The editor's formatting buttons. */
enum class Format { BOLD, ITALIC, STRIKE, CODE, HEADING, BULLET, NUMBERED, TASK, QUOTE, LINK }

/** Applies Markdown formatting to a selection. Pure functions: no Android. */
object MarkdownFormat {

    private val listPrefix = Regex("""^(\s*)([-*+] \[[ xX]] |[-*+] |\d+\. |> )""")
    private val headingPrefix = Regex("""^#{1,6} """)

    fun apply(format: Format, edit: Edit): Edit {
        val e = edit.copy(start = minOf(edit.start, edit.end), end = maxOf(edit.start, edit.end))
        return when (format) {
            Format.BOLD -> wrap(e, "**")
            Format.ITALIC -> wrap(e, "_")
            Format.STRIKE -> wrap(e, "~~")
            Format.CODE -> wrap(e, "`")
            Format.LINK -> link(e)
            Format.HEADING -> lines(e) { line, _ -> nextHeading(line) }
            Format.BULLET -> listLines(e) { "- " }
            Format.NUMBERED -> listLines(e) { "${it + 1}. " }
            Format.TASK -> listLines(e) { "- [ ] " }
            Format.QUOTE -> listLines(e) { "> " }
        }
    }

    /** Wraps the selection in [marker], or removes the marker when it already wraps it. */
    private fun wrap(e: Edit, marker: String): Edit {
        val t = e.text
        val n = marker.length
        val inside = t.substring(e.start, e.end)
        if (inside.length >= 2 * n && inside.startsWith(marker) && inside.endsWith(marker)) {
            val bare = inside.substring(n, inside.length - n)
            return Edit(t.replaceRange(e.start, e.end, bare), e.start, e.end - 2 * n)
        }
        if (e.start >= n && t.startsWith(marker, e.start - n) && t.startsWith(marker, e.end)) {
            val out = t.removeRange(e.end, e.end + n).removeRange(e.start - n, e.start)
            return Edit(out, e.start - n, e.end - n)
        }
        return Edit(t.replaceRange(e.start, e.end, marker + inside + marker), e.start + n, e.end + n)
    }

    /** `[selection](https://)`, with the cursor where the address goes, or in the brackets when nothing is selected. */
    private fun link(e: Edit): Edit {
        val inside = e.text.substring(e.start, e.end)
        val out = e.text.replaceRange(e.start, e.end, "[$inside](https://)")
        val cursor = if (inside.isEmpty()) e.start + 1 else e.start + inside.length + 11
        return Edit(out, cursor)
    }

    /** # → ## → ### → plain. */
    private fun nextHeading(line: String): String {
        val level = headingPrefix.find(line)?.value?.trim()?.length ?: 0
        val body = line.removePrefix(headingPrefix.find(line)?.value.orEmpty())
        return if (level >= 3) body else "#".repeat(level + 1) + " " + body
    }

    /**
     * Puts [prefix] on every selected line, replacing any other list or quote prefix,
     * or takes it off when every line already has exactly that kind of prefix.
     */
    private fun listLines(e: Edit, prefix: (Int) -> String): Edit {
        val (first, last) = lineRange(e)
        val selected = e.text.split('\n').subList(first, last + 1)
        val kind = { line: String -> listPrefix.find(line)?.groupValues?.get(2)?.let(::kindOf) }
        val all = selected.all { kind(it) == kindOf(prefix(0)) }
        return lines(e) { line, i ->
            val found = listPrefix.find(line)
            val indent = found?.groupValues?.get(1).orEmpty()
            val body = if (found != null) line.substring(found.value.length) else line
            if (all) indent + body else indent + prefix(i) + body
        }
    }

    private fun kindOf(prefix: String): String = when {
        prefix.startsWith(">") -> "quote"
        prefix.first().isDigit() -> "numbered"
        prefix.contains("[") -> "task"
        else -> "bullet"
    }

    /** Rewrites each selected line with [change] (given the line and its index within the selection). */
    private fun lines(e: Edit, change: (String, Int) -> String): Edit {
        val all = e.text.split('\n').toMutableList()
        val (first, last) = lineRange(e)
        var startShift = 0
        var endShift = 0
        for (i in first..last) {
            val before = all[i]
            all[i] = change(before, i - first)
            val delta = all[i].length - before.length
            if (i == first) startShift = delta
            endShift += delta
        }
        val firstLineStart = all.take(first).sumOf { it.length + 1 }
        val start = maxOf(firstLineStart, e.start + startShift)
        return Edit(all.joinToString("\n"), start, maxOf(start, e.end + endShift))
    }

    /** The indices of the first and last line the selection touches. */
    private fun lineRange(e: Edit): Pair<Int, Int> {
        val first = e.text.substring(0, e.start).count { it == '\n' }
        val last = first + e.text.substring(e.start, e.end).count { it == '\n' }
        return first to last
    }
}
