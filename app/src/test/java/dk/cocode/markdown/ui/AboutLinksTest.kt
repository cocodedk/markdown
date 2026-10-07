package dk.cocode.markdown.ui

import androidx.test.core.app.ApplicationProvider
import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AboutLinksTest {

    private val github = AboutTargets(onFdroid = false, privacyUrl = null)
    private val fdroid = AboutTargets(onFdroid = true, privacyUrl = null)

    @Test
    fun `before F-Droid lists the app, updates are on the GitHub releases page`() {
        assertEquals("https://github.com/cocodedk/markdown/releases/latest", aboutUrl(AboutLink.Update, github))
    }

    @Test
    fun `once F-Droid lists the app, updates are on its F-Droid page`() {
        assertEquals("https://f-droid.org/packages/dk.cocode.markdown/", aboutUrl(AboutLink.Update, fdroid))
    }

    @Test
    fun `the F-Droid page is named by the application id the app really has`() {
        val id = ApplicationProvider.getApplicationContext<Context>().packageName
        assertTrue(aboutUrl(AboutLink.Update, fdroid)!!.endsWith("/$id/"))
    }

    @Test
    fun `website, source and issues do not depend on F-Droid or the policy`() {
        for (targets in listOf(github, fdroid, AboutTargets(true, "https://markdown.cocode.dk/privacy/"))) {
            assertEquals("https://markdown.cocode.dk", aboutUrl(AboutLink.Website, targets))
            assertEquals("https://github.com/cocodedk/markdown", aboutUrl(AboutLink.Source, targets))
            assertEquals("https://github.com/cocodedk/markdown/issues", aboutUrl(AboutLink.Issues, targets))
        }
    }

    @Test
    fun `the privacy link is the policy page when there is one`() {
        val targets = AboutTargets(onFdroid = false, privacyUrl = "https://markdown.cocode.dk/privacy/")
        assertEquals("https://markdown.cocode.dk/privacy/", aboutUrl(AboutLink.Privacy, targets))
    }

    @Test
    fun `there is no privacy link while there is no policy page`() {
        assertNull(aboutUrl(AboutLink.Privacy, github))
    }

    @Test
    fun `this build links to GitHub and has no policy page yet`() {
        assertFalse(appTargets.onFdroid)
        assertNull(appTargets.privacyUrl)
    }

    @Test
    fun `the version name is the one in the build`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertTrue(versionName(context).matches(Regex("""\d+\.\d+\.\d+""")))
    }
}
