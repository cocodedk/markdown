package dk.cocode.markdown.ui

import android.app.Application
import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dk.cocode.markdown.R
import dk.cocode.markdown.render.MarkdownRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface ViewerState {
    data object Empty : ViewerState
    data object Loading : ViewerState
    data class Shown(val title: String?, val markdown: String, val html: String) : ViewerState
    data class Failed(val title: String?, @param:StringRes val message: Int) : ViewerState

    /** The text being edited and the text last saved (or opened); held only here, never in saved state. */
    data class Editing(
        val title: String?,
        val text: String,
        val saved: String,
        val saving: Boolean = false,
        @param:StringRes val error: Int? = null,
    ) : ViewerState {
        val dirty: Boolean get() = text != saved
    }
}

/** Holds the open document's URI and its rendered page across configuration changes. */
class ViewerViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow<ViewerState>(ViewerState.Empty)
    val state: StateFlow<ViewerState> = _state

    var uri: Uri? = null
        private set

    private val _saveAs = Channel<String>(Channel.BUFFERED)

    /** A suggested file name, each time a save needs the user to choose where the file goes. */
    val saveAsRequests: Flow<String> = _saveAs.receiveAsFlow()

    private var dark = false
    private var job: Job? = null

    /** Opens [uri]; with [edit], straight into the editor. */
    fun open(uri: Uri, edit: Boolean = false) {
        this.uri = uri
        val dark = dark
        _state.value = ViewerState.Loading
        job?.cancel()
        job = viewModelScope.launch {
            _state.value = withContext(Dispatchers.IO) { load(uri, dark) }
            if (edit) edit()
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

    private val editing get() = _state.value as? ViewerState.Editing

    /** Edits the shown document. */
    fun edit() {
        val shown = _state.value as? ViewerState.Shown ?: return
        _state.value = ViewerState.Editing(shown.title, shown.markdown, shown.markdown)
    }

    /** Starts an empty document with no file behind it yet. */
    fun newDocument() {
        uri = null
        job?.cancel()
        _state.value = ViewerState.Editing(null, "", "")
    }

    fun change(text: String) {
        val editing = editing ?: return
        _state.value = editing.copy(text = text, error = null)
    }

    /** Writes over the open file; when there is none, or it will not take a write, asks where to save. */
    fun save() {
        val editing = editing ?: return
        val target = uri ?: return requestSaveAs(editing)
        write(target, editing) { requestSaveAs(it) }
    }

    /** Writes to [target], the place the user chose, which becomes the open file. */
    fun saveAs(target: Uri) {
        val editing = editing ?: return
        write(target, editing) { _state.value = it.copy(error = R.string.could_not_save) }
    }

    /** Leaves the editor, showing the text last saved; unsaved changes are dropped. */
    fun done() {
        val editing = editing ?: return
        _state.value = if (editing.title == null && editing.saved.isEmpty()) {
            ViewerState.Empty
        } else {
            ViewerState.Shown(editing.title, editing.saved, MarkdownRenderer.render(editing.saved, dark))
        }
    }

    private fun requestSaveAs(editing: ViewerState.Editing) {
        _state.value = editing.copy(saving = false)
        val name = editing.title ?: getApplication<Application>().getString(R.string.untitled)
        _saveAs.trySend(if (name.endsWith(".md")) name else "$name.md")
    }

    private fun write(target: Uri, editing: ViewerState.Editing, onFailure: (ViewerState.Editing) -> Unit) {
        val text = editing.text
        val resolver = getApplication<Application>().contentResolver
        _state.value = editing.copy(saving = true, error = null)
        viewModelScope.launch {
            val name = withContext(Dispatchers.IO) {
                runCatching {
                    DocumentWriter.writeText(resolver, target, text)
                    DocumentReader.displayName(resolver, target)
                }
            }
            // Typing may have gone on during the write; keep it, and mark only what was written as saved.
            val now = this@ViewerViewModel.editing ?: return@launch
            name.fold(
                onSuccess = {
                    uri = target
                    _state.value = now.copy(title = it ?: now.title, saved = text, saving = false)
                },
                onFailure = { onFailure(now.copy(saving = false)) },
            )
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
