package dk.cocode.markdownviewer

import dk.cocode.markdownviewer.ScriptHarness.run
import java.io.File
import java.util.Base64
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Runs scripts/release-keystore.sh, the release workflow's keystore check, with a stub keytool. */
class ReleaseKeystoreTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val repo: File by lazy { ScriptHarness.repo(tmp.newFolder("repo")) }
    private val log: File by lazy { File(tmp.root, "keytool.log") }
    private val keystore: File by lazy { File(tmp.root, "release.keystore") }
    private val bytes = byteArrayOf(0x30, 0x82.toByte(), 0x01, 0x00, 0x7f, 0x0a)
    private val d = "$"

    private val bin: File by lazy {
        tmp.newFolder("bin").also {
            ScriptHarness.stub(
                it, "keytool",
                """
                echo "keytool $d* password-env=${d}{KEYSTORE_PASSWORD:+set}" >> "${d}KEYTOOL_LOG"
                [ -z "${d}KEYTOOL_FAIL" ]
                """.trimIndent(),
            )
        }
    }

    private fun check(fail: Boolean = false) = run(
        repo, listOf("sh", "scripts/release-keystore.sh"),
        env = mapOf(
            "PATH" to ScriptHarness.pathWith(bin),
            "KEYTOOL_LOG" to log.absolutePath,
            "KEYTOOL_FAIL" to if (fail) "1" else "",
            "KEYSTORE_BASE64" to Base64.getEncoder().encodeToString(bytes),
            "KEYSTORE_PATH" to keystore.absolutePath,
            "KEYSTORE_PASSWORD" to "store-secret",
            "KEY_ALIAS" to "upload",
            "KEY_PASSWORD" to "key-secret",
        ),
    )

    @Test
    fun `the keystore is decoded byte for byte`() {
        assertEquals(0, check().code)
        assertArrayEquals(bytes, keystore.readBytes())
    }

    @Test
    fun `keytool lists the alias before the build`() {
        val result = check()
        assertEquals(result.out, 0, result.code)
        val call = log.readText()
        assertTrue(call, call.contains("-list -keystore ${keystore.absolutePath} -storepass:env KEYSTORE_PASSWORD -alias upload"))
    }

    @Test
    fun `a keystore that does not open fails, names the alias and is removed`() {
        val result = check(fail = true)
        assertNotEquals(0, result.code)
        assertTrue(result.out.contains("'upload'"))
        assertFalse(keystore.exists())
    }

    @Test
    fun `the password never reaches keytool's command line or the output`() {
        val result = check()
        val call = log.readText()
        assertTrue(call.contains("password-env=set"))
        assertFalse(call.contains("store-secret"))
        assertFalse(result.out.contains("store-secret"))
        assertFalse(result.out.contains("key-secret"))
    }
}
