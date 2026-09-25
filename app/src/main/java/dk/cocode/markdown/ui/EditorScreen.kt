package dk.cocode.markdown.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import dk.cocode.markdown.R

const val EDITOR_TAG = "editor"

/** The Markdown source as plain, editable text. */
@Composable
fun Editor(state: ViewerState.Editing, onChange: (String) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        state.error?.let {
            Text(
                stringResource(it),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        BasicTextField(
            value = state.text,
            onValueChange = onChange,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface,
                // Each paragraph follows its own first strong character, like the rendered page.
                textDirection = TextDirection.Content,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxSize().padding(16.dp).testTag(EDITOR_TAG),
        )
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
