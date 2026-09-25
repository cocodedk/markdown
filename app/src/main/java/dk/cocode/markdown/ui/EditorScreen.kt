package dk.cocode.markdown.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import dk.cocode.markdown.R
import dk.cocode.markdown.edit.Edit
import dk.cocode.markdown.edit.MarkdownFormat

const val EDITOR_TAG = "editor"

/** The Markdown source as plain, editable text, with formatting buttons above the keyboard. */
@Composable
fun Editor(state: ViewerState.Editing, onChange: (String) -> Unit) {
    // The selection lives here; the text lives in the ViewModel. A text from outside (a new document) resets it.
    var field by remember { mutableStateOf(TextFieldValue(state.text)) }
    if (field.text != state.text) field = TextFieldValue(state.text)
    val update = { value: TextFieldValue ->
        field = value
        if (value.text != state.text) onChange(value.text)
    }
    // Edge to edge, the window does not shrink for the keyboard, so the editor makes room itself.
    Column(Modifier.fillMaxSize().imePadding()) {
        state.error?.let {
            Text(
                stringResource(it),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        BasicTextField(
            value = field,
            onValueChange = update,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface,
                // Each paragraph follows its own first strong character, like the rendered page.
                textDirection = TextDirection.Content,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp).testTag(EDITOR_TAG),
        )
        FormatBar { format ->
            val out = MarkdownFormat.apply(format, Edit(field.text, field.selection.start, field.selection.end))
            update(TextFieldValue(out.text, TextRange(out.start, out.end)))
        }
    }
}

/** Asks before unsaved changes are dropped. */
@Composable
fun DiscardDialog(onDiscard: () -> Unit, onKeep: () -> Unit) {
    AlertDialog(
        onDismissRequest = onKeep,
        title = { Text(stringResource(R.string.discard_title)) },
        confirmButton = { TextButton(onClick = onDiscard) { Text(stringResource(R.string.discard)) } },
        dismissButton = { TextButton(onClick = onKeep) { Text(stringResource(R.string.keep_editing)) } },
    )
}
