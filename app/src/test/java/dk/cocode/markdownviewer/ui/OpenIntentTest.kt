package dk.cocode.markdownviewer.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Looper
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import dk.cocode.markdownviewer.R
import dk.cocode.markdownviewer.render.MarkdownRenderer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.function.Supplier

@RunWith(RobolectricTestRunner::class)
class OpenIntentTest {

    private val context = ApplicationProvider.getApplicationContext<android.app.Application>()

    private fun register(uri: Uri, bytes: ByteArray) {
        shadowOf(context.contentResolver).registerInputStreamSupplier(
            uri, Supplier<InputStream> { ByteArrayInputStream(bytes) },
        )
    }

    private fun viewIntent(uri: Uri) = Intent(Intent.ACTION_VIEW, uri).setClassName(context, MainActivity::class.java.name)

    /** Opens the activity with [intent] and waits for the document to leave the loading state. */
    private fun stateAfter(intent: Intent): ViewerState {
        val activity = Robolectric.buildActivity(MainActivity::class.java, intent).setup().get()
        val viewModel = ViewModelProvider(activity)[ViewerViewModel::class.java]
        return awaitLoaded(viewModel)
    }

    private fun awaitLoaded(viewModel: ViewerViewModel): ViewerState {
        repeat(500) {
            shadowOf(Looper.getMainLooper()).idle()
            val state = viewModel.state.value
            if (state != ViewerState.Loading) return state
            Thread.sleep(10)
        }
        error("document never loaded")
    }

    @Test
    fun `a view intent shows the rendered file`() {
        val uri = Uri.parse("content://docs.test/readme.md")
        val markdown = "# Hello\n\nسلام 😀"
        register(uri, markdown.toByteArray(Charsets.UTF_8))

        val state = stateAfter(viewIntent(uri))

        assertTrue(state is ViewerState.Shown)
        val html = (state as ViewerState.Shown).html
        assertEquals(MarkdownRenderer.render(markdown, dark = false), html)
        assertTrue(html.contains("<h1>Hello</h1>"))
        assertTrue(html.contains("سلام 😀"))
    }

    @Test
    fun `a uri that cannot be read shows the could-not-open message`() {
        val state = stateAfter(viewIntent(Uri.parse("content://docs.test/missing.md")))

        assertTrue(state is ViewerState.Failed)
        val message = context.getString((state as ViewerState.Failed).message)
        assertEquals("This file could not be opened.", message)
    }

    @Test
    fun `a file over 2 MB shows the too-large message`() {
        val uri = Uri.parse("content://docs.test/huge.md")
        register(uri, ByteArray(DocumentReader.MAX_BYTES + 1) { 'a'.code.toByte() })

        val state = stateAfter(viewIntent(uri))

        assertTrue(state is ViewerState.Failed)
        assertEquals(R.string.too_large, (state as ViewerState.Failed).message)
        assertTrue(context.getString(R.string.too_large).contains("too large"))
    }

    @Test
    fun `a file of exactly 2 MB is shown`() {
        val uri = Uri.parse("content://docs.test/big.md")
        register(uri, ByteArray(DocumentReader.MAX_BYTES) { 'a'.code.toByte() })

        assertTrue(stateAfter(viewIntent(uri)) is ViewerState.Shown)
    }

    @Test
    fun `a new view intent replaces the document`() {
        val first = Uri.parse("content://docs.test/first.md")
        val second = Uri.parse("content://docs.test/second.md")
        register(first, "# First".toByteArray())
        register(second, "# Second".toByteArray())
        val controller = Robolectric.buildActivity(MainActivity::class.java, viewIntent(first)).setup()
        val viewModel = ViewModelProvider(controller.get())[ViewerViewModel::class.java]
        awaitLoaded(viewModel)

        controller.newIntent(viewIntent(second))

        val state = awaitLoaded(viewModel) as ViewerState.Shown
        assertTrue(state.html.contains("<h1>Second</h1>"))
        assertEquals(second, viewModel.uri)
    }

    @Test
    fun `the installed package requests no permissions`() {
        val info = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
        // androidx.test:core adds REORDER_TASKS to the unit-test manifest only, never to the app's.
        val requested = info.requestedPermissions.orEmpty().toList() - "android.permission.REORDER_TASKS"
        assertEquals(emptyList<String>(), requested)
        assertTrue(info.permissions.isNullOrEmpty())
    }
}
