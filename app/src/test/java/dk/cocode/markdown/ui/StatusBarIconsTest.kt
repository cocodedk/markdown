package dk.cocode.markdown.ui

import android.app.Application
import android.view.ContextThemeWrapper
import androidx.core.view.WindowCompat
import androidx.test.core.app.ApplicationProvider
import dk.cocode.markdown.R
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class StatusBarIconsTest {

    private fun lightStatusBarInTheme(): Boolean {
        val context = ContextThemeWrapper(ApplicationProvider.getApplicationContext<Application>(), R.style.Theme_Markdown)
        val attrs = context.theme.obtainStyledAttributes(intArrayOf(android.R.attr.windowLightStatusBar))
        return attrs.getBoolean(0, false).also { attrs.recycle() }
    }

    private fun lightStatusBarOnScreen(): Boolean {
        val window = Robolectric.buildActivity(MainActivity::class.java).setup().get().window
        return WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars
    }

    @Test
    fun `in the light theme the status-bar icons are dark`() {
        assertTrue(lightStatusBarInTheme())
        assertTrue(lightStatusBarOnScreen())
    }

    @Test
    @Config(qualifiers = "night")
    fun `in the dark theme the status-bar icons are light`() {
        assertFalse(lightStatusBarInTheme())
        assertFalse(lightStatusBarOnScreen())
    }
}
