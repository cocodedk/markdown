package dk.cocode.markdownviewer.ui

import android.content.ContentResolver
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.IntentCompat
import dk.cocode.markdownviewer.R

class MainActivity : ComponentActivity() {

    private val viewModel: ViewerViewModel by viewModels()

    private val openDocument = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::open)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.setDark(isNightMode())
        // After a rotation the ViewModel already holds the document.
        if (savedInstanceState == null) handle(intent)
        setContent {
            MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
                val state by viewModel.state.collectAsState()
                ViewerScreen(state, onOpen = ::pickDocument)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handle(intent)
    }

    private fun pickDocument() {
        openDocument.launch(PICKER_TYPES)
    }

    private fun handle(intent: Intent?) {
        if (intent == null) return
        val uri = intent.data
        when (intent.action) {
            Intent.ACTION_VIEW -> if (uri != null) viewModel.open(uri)
            Intent.ACTION_SEND -> handleShare(intent)
        }
    }

    /** A shared file opens like a viewed one; shared text is the document itself. */
    private fun handleShare(intent: Intent) {
        val stream = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
        val text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
        val subject = intent.getCharSequenceExtra(Intent.EXTRA_SUBJECT)?.toString()?.takeIf { it.isNotBlank() }
        when {
            stream != null && stream.scheme == ContentResolver.SCHEME_CONTENT ->viewModel.open(stream)
            text != null -> viewModel.showText(subject ?: getString(R.string.shared_text), text)
            else -> viewModel.showCouldNotOpen()
        }
    }

    private fun isNightMode(): Boolean =
        resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

    companion object {
        /** Many pickers label `.md` as octet-stream. */
        val PICKER_TYPES = arrayOf("text/*", "application/octet-stream")
    }
}
