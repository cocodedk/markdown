package dk.cocode.markdownviewer

import dk.cocode.markdownviewer.ScriptHarness.run
import java.io.File
import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Runs setup-signing.sh with stub gh, keytool and stty that record what they were given. */
class SetupSigningTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val repo: File by lazy { ScriptHarness.repo(tmp.newFolder("repo")) }
    private val home: File by lazy { tmp.newFolder("home") }
    private val ghLog: File by lazy { File(tmp.root, "gh.log") }
    private val keytoolLog: File by lazy { File(tmp.root, "keytool.log") }
    private val sttyLog: File by lazy { File(tmp.root, "stty.log") }
    private val d = "$"

    private val bin: File by lazy {
        tmp.newFolder("bin").also {
            ScriptHarness.stub(
                it, "gh",
                """
                echo "gh $d*" >> "${d}GH_LOG"
                if [ "${d}1" = secret ]; then printf 'stdin=%s\n' "${d}(cat)" >> "${d}GH_LOG"; fi
                """.trimIndent(),
            )
            ScriptHarness.stub(
                it, "keytool",
                """
                echo "keytool $d* storepass-env=${d}{STORE_PASS:+set}" >> "${d}KEYTOOL_LOG"
                case "$d*" in
                    *-genkeypair*)
                        while [ $d# -gt 0 ]; do
                            if [ "${d}1" = -keystore ]; then printf 'KEYDATA\n' > "${d}2"; fi
                            shift
                        done ;;
                    *-list*) [ -z "${d}KEYTOOL_FAIL" ] ;;
                esac
                """.trimIndent(),
            )
            ScriptHarness.stub(
                it, "stty",
                """
                echo "stty $d*" >> "${d}STTY_LOG"
                if [ "${d}1" = -g ]; then echo saved-state; fi
                """.trimIndent(),
            )
        }
    }

    private fun signing(stdin: String, env: Map<String, String> = emptyMap()): ScriptHarness.Result {
        val base = mapOf(
            "PATH" to ScriptHarness.pathWith(bin),
            "HOME" to home.absolutePath,
            "GH_LOG" to ghLog.absolutePath,
            "KEYTOOL_LOG" to keytoolLog.absolutePath,
            "STTY_LOG" to sttyLog.absolutePath,
            "KEYSTORE_FILE" to "",
            "KEYSTORE_ALIAS" to "",
            "KEYTOOL_FAIL" to "",
        )
        return run(repo, listOf("sh", "scripts/setup-signing.sh"), stdin, base + env)
    }

    /** The secrets uploaded, by name, with the value gh read on stdin. */
    private fun secrets(): Map<String, String> {
        if (!ghLog.exists()) return emptyMap()
        val lines = ghLog.readLines()
        return lines.withIndex().filter { it.value.startsWith("gh secret set ") }.associate { (i, line) ->
            assertTrue(line, line.endsWith("--repo cocodedk/markdown-viewer"))
            line.split(" ")[3] to lines[i + 1].removePrefix("stdin=")
        }
    }

    @Test
    fun `a missing keystore is generated and the four secrets uploaded`() {
        val result = signing("s3cret-pw\ns3cret-pw\n")
        assertEquals(result.out, 0, result.code)
        val keystore = File(home, "release.keystore")
        assertTrue(keystore.isFile)
        assertTrue(keytoolLog.readText().contains("-genkeypair -keystore ${keystore.absolutePath} -alias android"))
        val expected = mapOf(
            "KEYSTORE_BASE64" to Base64.getEncoder().encodeToString("KEYDATA\n".toByteArray()),
            "KEYSTORE_PASSWORD" to "s3cret-pw",
            "KEY_ALIAS" to "android",
            "KEY_PASSWORD" to "s3cret-pw",
        )
        assertEquals(expected, secrets())
    }

    @Test
    fun `an existing keystore is reused with its alias`() {
        val keystore = File(tmp.root, "shared.jks").apply { writeText("SHARED\n") }
        val env = mapOf("KEYSTORE_FILE" to keystore.absolutePath, "KEYSTORE_ALIAS" to "upload")
        val result = signing("store-pw\nkey-pw\n", env)
        assertEquals(result.out, 0, result.code)
        val calls = keytoolLog.readText()
        assertFalse(calls.contains("-genkeypair"))
        assertTrue(calls.contains("-list -keystore ${keystore.absolutePath} -storepass:env STORE_PASS -alias upload"))
        assertEquals("SHARED\n", keystore.readText())
        val uploaded = secrets()
        assertEquals("store-pw", uploaded["KEYSTORE_PASSWORD"])
        assertEquals("key-pw", uploaded["KEY_PASSWORD"])
        assertEquals("upload", uploaded["KEY_ALIAS"])
    }

    @Test
    fun `an empty key password means the store password`() {
        File(home, "release.keystore").writeText("K\n")
        assertEquals(0, signing("same-pw\n\n").code)
        assertEquals("same-pw", secrets()["KEY_PASSWORD"])
    }

    @Test
    fun `passwords reach keytool through the environment, never the command line or output`() {
        val result = signing("hidden-pw\nhidden-pw\n")
        assertEquals(0, result.code)
        assertFalse(result.out.contains("hidden-pw"))
        assertFalse(result.out.contains("KEYDATA"))
        assertFalse(keytoolLog.readText().contains("hidden-pw"))
        assertTrue(keytoolLog.readText().contains("storepass-env=set"))
    }

    @Test
    fun `echo is turned off for each password and the terminal restored`() {
        assertEquals(0, signing("pw-123456\npw-123456\n").code)
        val stty = sttyLog.readLines()
        assertEquals(2, stty.count { it == "stty -echo" })
        assertEquals(stty.count { it == "stty -echo" }, stty.count { it == "stty saved-state" })
        assertEquals("stty saved-state", stty.last())
    }

    @Test
    fun `different new passwords upload nothing`() {
        val result = signing("first-pw\nsecond-pw\n")
        assertNotEquals(0, result.code)
        assertTrue(secrets().isEmpty())
        assertFalse(File(home, "release.keystore").exists())
    }

    @Test
    fun `a keystore that does not open uploads nothing`() {
        File(home, "release.keystore").writeText("K\n")
        val result = signing("wrong-pw\n\n", mapOf("KEYTOOL_FAIL" to "1"))
        assertNotEquals(0, result.code)
        assertTrue(result.out.contains("does not open"))
        assertTrue(secrets().isEmpty())
    }
}
