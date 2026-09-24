package dk.cocode.markdownviewer.render

import org.commonmark.Extension
import org.commonmark.ext.autolink.AutolinkExtension
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.ext.task.list.items.TaskListItemsExtension
import org.commonmark.node.Code
import org.commonmark.node.HardLineBreak
import org.commonmark.node.Image
import org.commonmark.node.Node
import org.commonmark.node.SoftLineBreak
import org.commonmark.node.Text
import org.commonmark.parser.Parser
import org.commonmark.renderer.NodeRenderer
import org.commonmark.renderer.html.HtmlNodeRendererContext
import org.commonmark.renderer.html.HtmlRenderer

/** Markdown text to one complete, self-contained HTML page. */
object MarkdownRenderer {

    private val extensions: List<Extension> = listOf(
        TablesExtension.create(),
        StrikethroughExtension.create(),
        AutolinkExtension.create(),
        TaskListItemsExtension.create(),
    )

    private val parser: Parser = Parser.builder().extensions(extensions).build()

    private val renderer: HtmlRenderer = HtmlRenderer.builder()
        .extensions(extensions)
        .escapeHtml(true)
        .sanitizeUrls(true)
        .nodeRendererFactory { ImageAsText(it) }
        .build()

    fun render(markdown: String, dark: Boolean): String {
        val body = renderer.render(parser.parse(markdown))
        return buildString {
            append("<!DOCTYPE html>\n<html>\n<head>\n")
            append("<meta charset=\"utf-8\">\n")
            append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n")
            append("<style>\n").append(PageStyle.css(dark)).append("</style>\n")
            append("</head>\n<body>\n").append(body).append("</body>\n</html>\n")
        }
    }
}

/** Images are never fetched: they show as `[image: alt]` in italics. */
private class ImageAsText(private val context: HtmlNodeRendererContext) : NodeRenderer {

    override fun getNodeTypes(): Set<Class<out Node>> = setOf(Image::class.java)

    override fun render(node: Node) {
        val alt = altText(node).trim()
        val writer = context.writer
        writer.tag("em")
        writer.text(if (alt.isEmpty()) "[image]" else "[image: $alt]")
        writer.tag("/em")
    }

    private fun altText(node: Node): String = buildString {
        var child = node.firstChild
        while (child != null) {
            when (child) {
                is Text -> append(child.literal)
                is Code -> append(child.literal)
                is SoftLineBreak, is HardLineBreak -> append(' ')
                else -> append(altText(child))
            }
            child = child.next
        }
    }
}
