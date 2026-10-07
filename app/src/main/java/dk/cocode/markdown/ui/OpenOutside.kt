package dk.cocode.markdown.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Hands [uri] to whichever app handles it, a browser for a web address, so it never opens inside this app.
 * False when no app can.
 */
fun openOutside(context: Context, uri: Uri): Boolean {
    val intent = Intent(Intent.ACTION_VIEW, uri)
    if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return try {
        context.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}
