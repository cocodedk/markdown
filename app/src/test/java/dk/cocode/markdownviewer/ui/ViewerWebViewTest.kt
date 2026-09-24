package dk.cocode.markdownviewer.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class ViewerWebViewTest {

    private val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
    private val webView = ViewerWebView.create(activity)
    private val client = shadowOf(webView).webViewClient

    private fun request(url: String) = object : WebResourceRequest {
        override fun getUrl(): Uri = Uri.parse(url)
        override fun isForMainFrame() = true
        override fun isRedirect() = false
        override fun hasGesture() = true
        override fun getMethod() = "GET"
        override fun getRequestHeaders(): Map<String, String> = emptyMap()
    }

    @Test
    fun `the webview has scripts, file and content access off and network blocked`() {
        val settings = webView.settings
        assertFalse(settings.javaScriptEnabled)
        assertFalse(settings.allowFileAccess)
        assertFalse(settings.allowContentAccess)
        assertTrue(settings.blockNetworkLoads)
    }

    @Test
    fun `an https link opens outside the app`() {
        assertTrue(client.shouldOverrideUrlLoading(webView, request("https://example.com/page")))

        val started = shadowOf(activity).nextStartedActivity
        assertEquals(Intent.ACTION_VIEW, started.action)
        assertEquals(Uri.parse("https://example.com/page"), started.data)
    }

    @Test
    fun `a mailto link opens outside the app`() {
        assertTrue(client.shouldOverrideUrlLoading(webView, request("mailto:a@example.com")))

        assertEquals(Uri.parse("mailto:a@example.com"), shadowOf(activity).nextStartedActivity.data)
    }

    @Test
    fun `a fragment link is ignored and never navigates`() {
        assertTrue(client.shouldOverrideUrlLoading(webView, request("about:blank#section")))
        assertTrue(client.shouldOverrideUrlLoading(webView, request("#section")))

        assertNull(shadowOf(activity).nextStartedActivity)
        assertNull(shadowOf(webView).lastLoadedUrl)
    }

    @Test
    fun `other schemes are ignored and never navigate`() {
        for (url in listOf("file:///etc/hosts", "content://x/y", "intent://x", "javascript:alert(1)")) {
            assertTrue(url, client.shouldOverrideUrlLoading(webView, request(url)))
        }
        assertNull(shadowOf(activity).nextStartedActivity)
    }

    @Test
    fun `the page is loaded as utf-8 html without a base url`() {
        val html = "<!DOCTYPE html><p>سلام</p>"
        ViewerWebView.show(webView, html)

        val loaded = shadowOf(webView).lastLoadDataWithBaseURL
        assertNull(loaded.baseUrl)
        assertEquals(html, loaded.data)
        assertEquals("text/html", loaded.mimeType)
        assertEquals("utf-8", loaded.encoding)
    }
}
