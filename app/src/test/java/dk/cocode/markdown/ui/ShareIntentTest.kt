package dk.cocode.markdown.ui

import android.content.Intent
import android.net.Uri
import android.os.Looper
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import dk.cocode.markdown.R
import dk.cocode.markdown.render.MarkdownRenderer
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
class ShareIntentTest {

    private val context = ApplicationProvider.getApplicationContext<android.app.Application>()

    private fun shareIntent(type: String = "text/markdown") =
        Intent(Intent.ACTION_SEND).setType(type).setClassName(context, MainActivity::class.java.name)

    private fun stateAfter(intent: Intent): ViewerState {
        val activity = Robolectric.buildActivity(MainActivity::class.java, intent).setup().get()
        val viewModel = ViewModelProvider(activity)[ViewerViewModel::class.java]
        repeat(500) {
            shadowOf(Looper.getMainLooper()).idle()
            val state = viewModel.state.value
            if (state != ViewerState.Loading) return state
            Thread.sleep(10)
        }
        error("document never loaded")
    }

    @Test
    fun `a shared file stream shows the rendered file`() {
        val uri = Uri.parse("content://docs.test/shared.md")
        val markdown = "# Shared\n\nسلام"
        shadowOf(context.contentResolver).registerInputStreamSupplier(
            uri, Supplier<InputStream> { ByteArrayInputStream(markdown.toByteArray(Charsets.UTF_8)) },
        )

        val state = stateAfter(shareIntent().putExtra(Intent.EXTRA_STREAM, uri))

        assertEquals(MarkdownRenderer.render(markdown, dark = false), (state as ViewerState.Shown).html)
    }

    @Test
    fun `shared text is rendered and titled with its subject`() {
        val text = "# Note\n\nbody"
        val intent = shareIntent("text/plain").putExtra(Intent.EXTRA_TEXT, text).putExtra(Intent.EXTRA_SUBJECT, "Meeting")

        val state = stateAfter(intent) as ViewerState.Shown

        assertEquals(MarkdownRenderer.render(text, dark = false), state.html)
        assertEquals("Meeting", state.title)
    }

    @Test
    fun `shared text without a subject is titled shared text`() {
        val state = stateAfter(shareIntent().putExtra(Intent.EXTRA_TEXT, "hello")) as ViewerState.Shown

        assertEquals(MarkdownRenderer.render("hello", dark = false), state.html)
        assertEquals("Shared text", state.title)
    }

    @Test
    fun `a share with neither stream nor text shows the could-not-open message`() {
        val state = stateAfter(shareIntent())

        assertEquals(R.string.could_not_open, (state as ViewerState.Failed).message)
        assertEquals("This file could not be opened.", context.getString(state.message))
    }

    @Test
    fun `a shared stream that is not a content uri is never read`() {
        val state = stateAfter(shareIntent().putExtra(Intent.EXTRA_STREAM, Uri.parse("file:///etc/hosts")))

        assertEquals(R.string.could_not_open, (state as ViewerState.Failed).message)
    }

    @Test
    fun `the activity is offered in the share sheet for markdown and plain text`() {
        for (type in listOf("text/markdown", "text/x-markdown", "text/plain")) {
            val matches = context.packageManager.queryIntentActivities(Intent(Intent.ACTION_SEND).setType(type), 0)
            assertTrue(type, matches.any { it.activityInfo.name == MainActivity::class.java.name })
        }
    }
}
