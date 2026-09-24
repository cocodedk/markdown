package dk.cocode.markdownviewer.ui

import android.content.Intent
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class OpenButtonTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun `with no document the title is the app name`() {
        compose.onNodeWithText("Markdown Viewer").assertExists()
    }

    @Test
    fun `the open button launches the document picker for text and octet-stream`() {
        compose.onNodeWithTag(OPEN_BUTTON_TAG).performClick()
        compose.waitForIdle()

        val started = shadowOf(compose.activity).nextStartedActivityForResult.intent
        assertEquals(Intent.ACTION_OPEN_DOCUMENT, started.action)
        assertArrayEquals(
            arrayOf("text/*", "application/octet-stream"),
            started.getStringArrayExtra(Intent.EXTRA_MIME_TYPES),
        )
    }
}
