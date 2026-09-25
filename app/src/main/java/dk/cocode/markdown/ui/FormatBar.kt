package dk.cocode.markdown.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import dk.cocode.markdown.R
import dk.cocode.markdown.edit.Format

/** One formatting button: what it shows, how that looks, and what a screen reader says. */
private class FormatButton(val format: Format, val glyph: String, @param:StringRes val label: Int, val style: SpanStyle = SpanStyle())

private val buttons = listOf(
    FormatButton(Format.BOLD, "B", R.string.format_bold, SpanStyle(fontWeight = FontWeight.Bold)),
    FormatButton(Format.ITALIC, "I", R.string.format_italic, SpanStyle(fontStyle = FontStyle.Italic, fontFamily = FontFamily.Serif)),
    FormatButton(Format.STRIKE, "S", R.string.format_strike, SpanStyle(textDecoration = TextDecoration.LineThrough)),
    FormatButton(Format.HEADING, "H", R.string.format_heading, SpanStyle(fontWeight = FontWeight.Bold)),
    FormatButton(Format.BULLET, "•", R.string.format_bullet),
    FormatButton(Format.NUMBERED, "1.", R.string.format_numbered),
    FormatButton(Format.TASK, "☐", R.string.format_task),
    FormatButton(Format.QUOTE, "❝", R.string.format_quote),
    FormatButton(Format.CODE, "</>", R.string.format_code, SpanStyle(fontFamily = FontFamily.Monospace)),
    FormatButton(Format.LINK, "🔗", R.string.format_link),
)

/** A scrolling row of formatting buttons, kept just above the keyboard. */
@Composable
fun FormatBar(onFormat: (Format) -> Unit) {
    Surface(tonalElevation = 3.dp) {
        Column {
            HorizontalDivider()
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                buttons.forEach { button ->
                    val label = stringResource(button.label)
                    TextButton(
                        onClick = { onFormat(button.format) },
                        modifier = Modifier.semantics { contentDescription = label },
                    ) {
                        Text(button.glyph, style = MaterialTheme.typography.titleMedium.merge(button.style))
                    }
                }
            }
        }
    }
}
