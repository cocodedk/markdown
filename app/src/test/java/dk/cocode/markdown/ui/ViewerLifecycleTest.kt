package dk.cocode.markdown.ui

import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Looper
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import dk.cocode.markdown.render.MarkdownRenderer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.function.Supplier

@RunWith(RobolectricTestRunner::class)
class ViewerLifecycleTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val uri = Uri.parse("content://docs.test/notes.md")
    private val markdown = "# Notes"

    private fun showDocument() = Robolectric.buildActivity(
        MainActivity::class.java,
        Intent(Intent.ACTION_VIEW, uri).setClassName(context, MainActivity::class.java.name),
    ).also {
        shadowOf(context.contentResolver).registerInputStreamSupplier(
            uri, Supplier<InputStream> { ByteArrayInputStream(markdown.toByteArray()) },
        )
    }.setup()

    private fun awaitShown(viewModel: ViewerViewModel): ViewerState.Shown {
        repeat(500) {
            shadowOf(Looper.getMainLooper()).idle()
            (viewModel.state.value as? ViewerState.Shown)?.let { return it }
            Thread.sleep(10)
        }
        error("document never shown")
    }

    @Test
    fun `the shown document survives rotation`() {
        val controller = showDocument()
        val viewModel = ViewModelProvider(controller.get())[ViewerViewModel::class.java]
        val shown = awaitShown(viewModel)

        controller.recreate()

        val after = ViewModelProvider(controller.get())[ViewerViewModel::class.java]
        assertSame(viewModel, after)
        assertSame(shown, after.state.value)
        assertEquals(uri, after.uri)
    }

    @Test
    @Config(qualifiers = "night")
    fun `the system dark theme renders a dark page`() {
        val viewModel = ViewModelProvider(showDocument().get())[ViewerViewModel::class.java]

        assertEquals(MarkdownRenderer.render(markdown, dark = true), awaitShown(viewModel).html)
    }

    @Test
    fun `the display name comes from the provider`() {
        Robolectric.setupContentProvider(NamedProvider::class.java, "names.test")

        assertEquals("guide.md", DocumentReader.displayName(context.contentResolver, Uri.parse("content://names.test/1")))
    }

    @Test
    fun `a uri without a provider has no display name`() {
        assertNull(DocumentReader.displayName(context.contentResolver, Uri.parse("content://nobody.test/1")))
    }

    class NamedProvider : ContentProvider() {
        override fun onCreate() = true
        override fun query(uri: Uri, p: Array<out String>?, s: String?, a: Array<out String>?, o: String?): Cursor =
            MatrixCursor(arrayOf(OpenableColumns.DISPLAY_NAME)).apply { addRow(arrayOf("guide.md")) }
        override fun getType(uri: Uri): String? = null
        override fun insert(uri: Uri, values: ContentValues?): Uri? = null
        override fun delete(uri: Uri, s: String?, a: Array<out String>?) = 0
        override fun update(uri: Uri, v: ContentValues?, s: String?, a: Array<out String>?) = 0
    }
}
