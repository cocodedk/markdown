package dk.cocode.markdown.ui

import android.content.Intent
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.ViewModelProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class AboutScreenTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val viewModel get() = ViewModelProvider(compose.activity)[ViewerViewModel::class.java]

    private fun openAbout() {
        compose.onNodeWithTag(ABOUT_BUTTON_TAG).performClick()
        compose.waitForIdle()
    }

    private fun showAbout(targets: AboutTargets) {
        compose.runOnUiThread { compose.activity.setContent { MaterialTheme { AboutScreen({}, targets) } } }
        compose.waitForIdle()
    }

    private fun tap(label: String): Intent {
        compose.onNodeWithText(label).performScrollTo().performClick()
        compose.waitForIdle()
        return shadowOf(compose.activity).nextStartedActivity
    }

    private fun headings(): List<String> =
        compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)).fetchSemanticsNodes()
            .mapNotNull { it.config.getOrNull(SemanticsProperties.Text)?.joinToString("") }

    @Test
    fun `the main screen offers About, and it opens`() {
        openAbout()

        compose.onNodeWithText("Version", substring = true).assertExists()
        compose.onNodeWithTag(OPEN_BUTTON_TAG).assertDoesNotExist()
    }

    @Test
    fun `each section title is a heading, in the order of the standard`() {
        openAbout()

        // The Scaffold lists its top bar after the content, so the page title is checked on its own.
        val all = headings()
        assertTrue("About Markdown" in all)
        assertEquals(
            listOf(
                "Name and version", "What the app does", "Privacy", "Links", "Credits and licenses", "Made by Cocode",
            ),
            all - "About Markdown",
        )
    }

    @Test
    fun `the latest-version button opens the GitHub releases page in a browser`() {
        openAbout()

        val intent = tap("See the latest version")
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals("https://github.com/cocodedk/markdown/releases/latest", intent.dataString)
        assertNull(intent.component)
    }

    @Test
    fun `the website, source and report links open the right pages`() {
        openAbout()

        assertEquals("https://markdown.cocode.dk", tap("Open the website").dataString)
        assertEquals("https://github.com/cocodedk/markdown", tap("See the source code on GitHub").dataString)
        assertEquals("https://github.com/cocodedk/markdown/issues", tap("Report a problem on GitHub").dataString)
    }

    @Test
    fun `this build offers the privacy policy and opens its page`() {
        openAbout()

        assertEquals("https://markdown.cocode.dk/privacy/", tap("Read the privacy policy").dataString)
    }

    @Test
    @Config(qualifiers = "da")
    fun `with the app in Danish the website and privacy links open the Danish pages`() {
        openAbout()

        assertEquals("https://markdown.cocode.dk/da/", tap("Åbn hjemmesiden").dataString)
        assertEquals("https://markdown.cocode.dk/da/privacy/", tap("Læs privatlivspolitikken").dataString)
    }

    @Test
    fun `without a policy page there is no privacy link`() {
        showAbout(AboutTargets(onFdroid = false, privacyUrl = null))

        compose.onNodeWithText("Read the privacy policy").assertDoesNotExist()
        compose.onNodeWithText("Markdown collects no personal data.", substring = true).assertExists()
    }

    @Test
    fun `with a policy page the privacy link opens it`() {
        showAbout(AboutTargets(onFdroid = true, privacyUrl = "https://markdown.cocode.dk/privacy/"))

        assertEquals("https://markdown.cocode.dk/privacy/", tap("Read the privacy policy").dataString)
        assertEquals("https://f-droid.org/packages/dk.cocode.markdown/", tap("See the latest version").dataString)
    }

    @Test
    fun `when no app can open a link the page says so`() {
        shadowOf(compose.activity.application).checkActivities(true)
        openAbout()
        compose.onNodeWithText("No app on this phone", substring = true).assertDoesNotExist()

        compose.onNodeWithText("Open the website").performScrollTo().performClick()
        compose.waitForIdle()

        compose.onNodeWithText("No app on this phone", substring = true).assertIsDisplayed()
    }

    @Test
    fun `back and the Back button return to the main screen`() {
        openAbout()
        compose.onNodeWithText("Back").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(OPEN_BUTTON_TAG).assertExists()

        openAbout()
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
        compose.onNodeWithTag(OPEN_BUTTON_TAG).assertExists()
    }

    @Test
    fun `a document that opens meanwhile takes the screen back`() {
        openAbout()

        compose.runOnUiThread { viewModel.showText("Notes", "# Hi") }
        compose.waitForIdle()

        compose.onNodeWithText("Notes").assertExists()
        compose.onNodeWithText("Name and version").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w700dp-h260dp-land")
    fun `on a short screen the main screen scrolls to its last button`() {
        compose.onNodeWithTag(ABOUT_BUTTON_TAG).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `a shown document does not offer About`() {
        compose.runOnUiThread { viewModel.showText("Notes", "# Hi") }
        compose.waitForIdle()

        compose.onNodeWithTag(ABOUT_BUTTON_TAG).assertDoesNotExist()
    }
}
