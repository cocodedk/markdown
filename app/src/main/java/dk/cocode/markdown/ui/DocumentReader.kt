package dk.cocode.markdown.ui

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

/** The file is bigger than [DocumentReader.MAX_BYTES]. */
class TooLargeException : IOException("document too large")

/** Reads a document's text and name through a [ContentResolver]. Blocking: call off the main thread. */
object DocumentReader {

    const val MAX_BYTES = 2 * 1024 * 1024

    /** The document as UTF-8 text; throws on any failure, [TooLargeException] when over the limit. */
    fun readText(resolver: ContentResolver, uri: Uri): String {
        val stream = resolver.openInputStream(uri) ?: throw IOException("no stream for $uri")
        val bytes = stream.use { readAtMost(it, MAX_BYTES) }
        return String(bytes, Charsets.UTF_8)
    }

    /** The display name, or null when the provider has none. */
    fun displayName(resolver: ContentResolver, uri: Uri): String? = runCatching {
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            val column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (column >= 0 && cursor.moveToFirst()) cursor.getString(column) else null
        }
    }.getOrNull()?.takeIf { it.isNotBlank() }

    internal fun readAtMost(input: InputStream, max: Int): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(64 * 1024)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            out.write(buffer, 0, read)
            if (out.size() > max) throw TooLargeException()
        }
        return out.toByteArray()
    }
}
