package dk.cocode.markdownviewer.ui

import android.app.Application
import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dk.cocode.markdownviewer.R
import dk.cocode.markdownviewer.render.MarkdownRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface ViewerState {
    data object Empty : ViewerState
    data object Loading : ViewerState
    data class Shown(val title: String?, val markdown: String, val html: String) : ViewerState
    data class Failed(val title: String?, @param:StringRes val message: Int) : ViewerState
}

/** Holds the open document's URI and its rendered page across configuration changes. */
class ViewerViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow<ViewerState>(ViewerState.Empty)
    val state: StateFlow<ViewerState> = _state

    var uri: Uri? = null
        private set

    private var dark = false
    private var job: Job? = null

    fun open(uri: Uri) {
        this.uri = uri
        val dark = dark
        _state.value = ViewerState.Loading
        job?.cancel()
        job = viewModelScope.launch {
            _state.value = withContext(Dispatchers.IO) { load(uri, dark) }
        }
    }

    /** Shows [text] itself as the document, with no file behind it. */
    fun showText(title: String, text: String) {
        uri = null
        job?.cancel()
        _state.value = ViewerState.Shown(title, text, MarkdownRenderer.render(text, dark))
    }

    /** Shows the could-not-open message, with no file behind it. */
    fun showCouldNotOpen() {
        uri = null
        job?.cancel()
        _state.value = ViewerState.Failed(null, R.string.could_not_open)
    }

    /** Follows the system theme; a shown page is rendered again in the new palette. */
    fun setDark(dark: Boolean) {
        if (dark == this.dark) return
        this.dark = dark
        val shown = _state.value as? ViewerState.Shown ?: return
        job?.cancel()
        job = viewModelScope.launch {
            _state.value = withContext(Dispatchers.Default) {
                shown.copy(html = MarkdownRenderer.render(shown.markdown, dark))
            }
        }
    }

    private fun load(uri: Uri, dark: Boolean): ViewerState {
        val resolver = getApplication<Application>().contentResolver
        val title = DocumentReader.displayName(resolver, uri)
        return try {
            val text = DocumentReader.readText(resolver, uri)
            ViewerState.Shown(title, text, MarkdownRenderer.render(text, dark))
        } catch (e: TooLargeException) {
            ViewerState.Failed(title, R.string.too_large)
        } catch (e: Exception) {
            ViewerState.Failed(title, R.string.could_not_open)
        }
    }
}
