package dk.cocode.markdownviewer.ui

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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import dk.cocode.markdownviewer.R

const val OPEN_BUTTON_TAG = "open-button"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(state: ViewerState, onOpen: () -> Unit) {
    val title = when (state) {
        is ViewerState.Shown -> state.title
        is ViewerState.Failed -> state.title
        else -> null
    } ?: stringResource(R.string.app_name)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                actions = { TextButton(onClick = onOpen) { Text(stringResource(R.string.open)) } },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            when (state) {
                ViewerState.Empty -> Prompt(stringResource(R.string.empty_hint), onOpen)
                ViewerState.Loading -> CircularProgressIndicator()
                is ViewerState.Failed -> Prompt(stringResource(state.message), onOpen)
                is ViewerState.Shown -> Page(state.html)
            }
        }
    }
}

@Composable
private fun Prompt(text: String, onOpen: () -> Unit) {
    Column(
        modifier = Modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        Button(onClick = onOpen, modifier = Modifier.testTag(OPEN_BUTTON_TAG)) {
            Text(
                stringResource(R.string.open),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
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
