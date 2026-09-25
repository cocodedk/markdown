package dk.cocode.markdown.ui

import android.content.ContentResolver
import android.net.Uri
import java.io.IOException

/** Writes a document's text through a [ContentResolver]. Blocking: call off the main thread. */
object DocumentWriter {

    /** Replaces the document with [text] as UTF-8; throws when the provider will not take a write. */
    fun writeText(resolver: ContentResolver, uri: Uri, text: String) {
        // "wt" truncates, so a shorter text leaves nothing of the old one behind.
        val stream = resolver.openOutputStream(uri, "wt") ?: throw IOException("no stream for $uri")
        stream.use { it.write(text.toByteArray(Charsets.UTF_8)) }
    }
}
