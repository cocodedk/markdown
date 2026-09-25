package dk.cocode.markdown.ui

import android.webkit.WebView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import dk.cocode.markdown.R

const val OPEN_BUTTON_TAG = "open-button"
const val NEW_BUTTON_TAG = "new-button"

/** What the screen's buttons and editor do. */
class ViewerActions(
    val onOpen: () -> Unit = {},
    val onNew: () -> Unit = {},
    val onEdit: () -> Unit = {},
    val onChange: (String) -> Unit = {},
    val onSave: () -> Unit = {},
    val onDone: () -> Unit = {},
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(state: ViewerState, actions: ViewerActions) {
    val title = when (state) {
        is ViewerState.Shown -> state.title
        is ViewerState.Failed -> state.title
        is ViewerState.Editing -> state.title ?: stringResource(R.string.untitled)
        else -> null
    } ?: stringResource(R.string.app_name)
    var confirmDiscard by remember { mutableStateOf(false) }
    val leave = { if ((state as? ViewerState.Editing)?.dirty == true) confirmDiscard = true else actions.onDone() }
    BackHandler(enabled = state is ViewerState.Editing, onBack = leave)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                actions = { BarActions(state, actions, leave) },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            when (state) {
                ViewerState.Empty -> Prompt(stringResource(R.string.empty_hint), actions)
                ViewerState.Loading -> CircularProgressIndicator()
                is ViewerState.Failed -> Prompt(stringResource(state.message), actions)
                is ViewerState.Shown -> Page(state.html)
                is ViewerState.Editing -> Editor(state, actions.onChange)
            }
        }
    }
    if (confirmDiscard && state is ViewerState.Editing) {
        DiscardDialog(
            onDiscard = { confirmDiscard = false; actions.onDone() },
            onKeep = { confirmDiscard = false },
        )
    }
}

@Composable
private fun BarActions(state: ViewerState, actions: ViewerActions, leave: () -> Unit) {
    when (state) {
        is ViewerState.Editing -> {
            TextButton(onClick = actions.onSave, enabled = state.dirty && !state.saving) {
                Text(stringResource(R.string.save))
            }
            TextButton(onClick = leave) { Text(stringResource(R.string.done)) }
        }
        is ViewerState.Shown -> {
            TextButton(onClick = actions.onEdit) { Text(stringResource(R.string.edit)) }
            TextButton(onClick = actions.onOpen) { Text(stringResource(R.string.open)) }
        }
        else -> TextButton(onClick = actions.onOpen) { Text(stringResource(R.string.open)) }
    }
}

@Composable
private fun Prompt(text: String, actions: ViewerActions) {
    Column(
        modifier = Modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        Button(onClick = actions.onOpen, modifier = Modifier.testTag(OPEN_BUTTON_TAG)) {
            Text(
                stringResource(R.string.open),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
        }
        OutlinedButton(onClick = actions.onNew, modifier = Modifier.testTag(NEW_BUTTON_TAG)) {
            Text(stringResource(R.string.new_document), modifier = Modifier.padding(horizontal = 16.dp))
        }
    }
}

@Composable
private fun Page(html: String) {
    AndroidView(
        factory = { ViewerWebView.create(it) },
        update = { webView: WebView ->
            if (webView.tag != html) {
                webView.tag = html
                ViewerWebView.show(webView, html)
            }
        },
        modifier = Modifier.fillMaxSize(),
    )
}
