package dk.cocode.markdown.ui

import android.content.Context
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

/** Builds the locked-down WebView that shows a rendered page. */
object ViewerWebView {

    fun create(context: Context): WebView = WebView(context).apply {
        settings.javaScriptEnabled = false
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        settings.blockNetworkLoads = true
        // Pinch zoom only: no on-screen buttons.
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        webViewClient = LinkClient()
    }

    fun show(webView: WebView, html: String) {
        webView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
    }
}

/** The WebView never navigates: web and mail links open outside the app, the rest do nothing. */
class LinkClient : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val uri = request.url
        // If no app can open it, nothing happens.
        if (uri.scheme?.lowercase() in EXTERNAL_SCHEMES) openOutside(view.context, uri)
        return true
    }

    private companion object {
        val EXTERNAL_SCHEMES = setOf("http", "https", "mailto")
    }
}
