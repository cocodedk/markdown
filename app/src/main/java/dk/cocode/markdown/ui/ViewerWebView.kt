package dk.cocode.markdown.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
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
        openOutside(view.context, request.url)
        return true
    }

    private fun openOutside(context: Context, uri: Uri) {
        if (uri.scheme?.lowercase() !in EXTERNAL_SCHEMES) return
        val intent = Intent(Intent.ACTION_VIEW, uri)
        if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // No app can open it: nothing happens.
        }
    }

    private companion object {
        val EXTERNAL_SCHEMES = setOf("http", "https", "mailto")
    }
}
