package dk.cocode.markdown.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.TextRange
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EditorScreenTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val viewModel get() = ViewModelProvider(compose.activity)[ViewerViewModel::class.java]

    @Test
    fun `new on the empty screen opens an empty editor`() {
        compose.onNodeWithTag(NEW_BUTTON_TAG).performClick()
        compose.waitForIdle()

        assertEquals(ViewerState.Editing(null, "", ""), viewModel.state.value)
        compose.onNodeWithText("Untitled").assertExists()
        compose.onNodeWithTag(EDITOR_TAG).assertExists()
    }

    @Test
    fun `a shown document offers edit, and the editor holds its source`() {
        compose.runOnUiThread { viewModel.showText("Notes", "# Hi") }
        compose.onNodeWithText("Edit").performClick()
        compose.waitForIdle()

        compose.onNodeWithTag(EDITOR_TAG).assertTextEquals("# Hi")
    }

    @Test
    fun `save is enabled only when there are unsaved changes`() {
        compose.runOnUiThread { viewModel.showText("Notes", "# Hi"); viewModel.edit() }
        compose.onNodeWithText("Save").assertIsNotEnabled()

        compose.onNodeWithTag(EDITOR_TAG).performTextReplacement("# Hello")

        compose.onNodeWithText("Save").assertIsEnabled()
        assertEquals("# Hello", (viewModel.state.value as ViewerState.Editing).text)
    }

    @Test
    fun `done with unsaved changes asks first, and discard leaves the editor`() {
        compose.runOnUiThread { viewModel.showText("Notes", "# Hi"); viewModel.edit(); viewModel.change("x") }
        compose.onNodeWithText("Done").performClick()
        compose.onNodeWithText("Discard your changes?").assertExists()

        compose.onNodeWithText("Keep editing").performClick()
        assertTrue(viewModel.state.value is ViewerState.Editing)

        compose.onNodeWithText("Done").performClick()
        compose.onNodeWithText("Discard").performClick()
        compose.waitForIdle()
        assertTrue(viewModel.state.value is ViewerState.Shown)
    }

    @Test
    fun `done with nothing changed leaves without asking`() {
        compose.runOnUiThread { viewModel.showText("Notes", "# Hi"); viewModel.edit() }
        compose.onNodeWithText("Done").performClick()
        compose.waitForIdle()

        compose.onNodeWithText("Discard your changes?").assertDoesNotExist()
        assertTrue(viewModel.state.value is ViewerState.Shown)
    }

    @Test
    fun `the bold button wraps the selected word and marks the document unsaved`() {
        compose.runOnUiThread { viewModel.showText("Notes", "say hi"); viewModel.edit() }
        compose.onNodeWithTag(EDITOR_TAG).performTextInputSelection(TextRange(4, 6))

        compose.onNodeWithContentDescription("Bold").performClick()
        compose.waitForIdle()

        compose.onNodeWithTag(EDITOR_TAG).assertTextEquals("say **hi**")
        val state = viewModel.state.value as ViewerState.Editing
        assertEquals("say **hi**", state.text)
        assertTrue(state.dirty)
    }

    @Test
    fun `every formatting button is labelled for screen readers`() {
        compose.runOnUiThread { viewModel.newDocument() }
        listOf(
            "Bold", "Italic", "Strikethrough", "Heading", "Bulleted list",
            "Numbered list", "Task list", "Quote", "Code", "Link",
        ).forEach { compose.onNodeWithContentDescription(it).assertExists() }
    }

    @Test
    fun `the app answers edit intents for markdown files`() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        val edit = Intent(Intent.ACTION_EDIT).setDataAndType(Uri.parse("content://docs.test/a.md"), "text/markdown")
            .setPackage(context.packageName)

        val handlers = context.packageManager.queryIntentActivities(edit, 0)

        assertEquals(listOf(MainActivity::class.java.name), handlers.map { it.activityInfo.name })
    }
}
