package dk.cocode.markdown.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Looper
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import dk.cocode.markdown.R
import dk.cocode.markdown.render.MarkdownRenderer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.util.function.Supplier

@RunWith(RobolectricTestRunner::class)
class EditTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val uri = Uri.parse("content://docs.test/notes.md")

    private fun activityWith(intent: Intent?) = Robolectric.buildActivity(MainActivity::class.java, intent).setup().get()

    private fun open(action: String, markdown: String = "# Notes"): Pair<MainActivity, ViewerViewModel> {
        shadowOf(context.contentResolver).registerInputStreamSupplier(
            uri, Supplier<InputStream> { ByteArrayInputStream(markdown.toByteArray()) },
        )
        val activity = activityWith(Intent(action, uri).setClassName(context, MainActivity::class.java.name))
        val viewModel = ViewModelProvider(activity)[ViewerViewModel::class.java]
        await(viewModel) { it != ViewerState.Loading }
        return activity to viewModel
    }

    private fun await(viewModel: ViewerViewModel, done: (ViewerState) -> Boolean): ViewerState {
        repeat(500) {
            shadowOf(Looper.getMainLooper()).idle()
            val state = viewModel.state.value
            if (done(state)) return state
            Thread.sleep(10)
        }
        error("state never settled: ${viewModel.state.value}")
    }

    private fun awaitSaved(viewModel: ViewerViewModel) =
        await(viewModel) { it is ViewerState.Editing && !it.saving } as ViewerState.Editing

    private fun createDocumentTitle(activity: MainActivity): String? {
        shadowOf(Looper.getMainLooper()).idle()
        val started = shadowOf(activity).nextStartedActivityForResult?.intent ?: return null
        assertEquals(Intent.ACTION_CREATE_DOCUMENT, started.action)
        assertEquals("text/markdown", started.type)
        return started.getStringExtra(Intent.EXTRA_TITLE)
    }

    @Test
    fun `edit shows the source and a change marks it unsaved`() {
        val (_, viewModel) = open(Intent.ACTION_VIEW)
        viewModel.edit()
        assertEquals(ViewerState.Editing(null, "# Notes", "# Notes"), viewModel.state.value)

        viewModel.change("# Notes\n\nmore")

        assertTrue((viewModel.state.value as ViewerState.Editing).dirty)
    }

    @Test
    fun `an edit intent opens straight into the editor`() {
        val (_, viewModel) = open(Intent.ACTION_EDIT)
        val state = await(viewModel) { it is ViewerState.Editing } as ViewerState.Editing
        assertEquals("# Notes", state.text)
        assertFalse(state.dirty)
    }

    @Test
    fun `done drops unsaved changes and shows the saved text`() {
        val (_, viewModel) = open(Intent.ACTION_VIEW)
        viewModel.edit()
        viewModel.change("changed")
        viewModel.done()
        assertEquals(ViewerState.Shown(null, "# Notes", MarkdownRenderer.render("# Notes", false)), viewModel.state.value)
    }

    @Test
    fun `save writes over the open file and truncates it`() {
        val file = File.createTempFile("notes", ".md").apply { writeText("a much longer old text") }
        val viewModel = ViewModelProvider(activityWith(null))[ViewerViewModel::class.java]
        viewModel.open(Uri.fromFile(file), edit = true)
        await(viewModel) { it is ViewerState.Editing }

        viewModel.change("short")
        viewModel.save()

        val saved = awaitSaved(viewModel)
        assertEquals("short", file.readText())
        assertFalse(saved.dirty)
    }

    @Test
    fun `a file that will not take a write asks where to save, then saves there`() {
        val (activity, viewModel) = open(Intent.ACTION_VIEW)
        viewModel.edit()
        viewModel.change("new text")
        viewModel.save()
        awaitSaved(viewModel)
        assertEquals("Untitled.md", createDocumentTitle(activity))

        val target = File.createTempFile("copy", ".md")
        viewModel.saveAs(Uri.fromFile(target))

        val saved = awaitSaved(viewModel)
        assertEquals("new text", target.readText())
        assertFalse(saved.dirty)
        assertEquals(Uri.fromFile(target), viewModel.uri)
    }

    @Test
    fun `a failed save as shows the could-not-save message`() {
        val viewModel = ViewModelProvider(activityWith(null))[ViewerViewModel::class.java]
        viewModel.newDocument()
        viewModel.change("text")
        viewModel.saveAs(Uri.parse("content://nobody.test/x.md"))
        assertEquals(R.string.could_not_save, awaitSaved(viewModel).error)
    }

    @Test
    fun `a new document starts empty, saves through the picker and leaves to the empty screen`() {
        val activity = activityWith(null)
        val viewModel = ViewModelProvider(activity)[ViewerViewModel::class.java]
        viewModel.newDocument()
        assertEquals(ViewerState.Editing(null, "", ""), viewModel.state.value)
        assertNull(viewModel.uri)

        viewModel.change("# Hi")
        viewModel.save()
        assertEquals("Untitled.md", createDocumentTitle(activity))

        viewModel.change("")
        viewModel.done()
        assertEquals(ViewerState.Empty, viewModel.state.value)
    }

    @Test
    fun `shared text saves under its subject as a markdown name`() {
        val activity = activityWith(null)
        val viewModel = ViewModelProvider(activity)[ViewerViewModel::class.java]
        viewModel.showText("Shopping", "- milk")
        viewModel.edit()
        viewModel.save()
        assertEquals("Shopping.md", createDocumentTitle(activity))
    }
}
