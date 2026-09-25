package dk.cocode.markdown

import dk.cocode.markdown.ScriptHarness.commit
import dk.cocode.markdown.ScriptHarness.ok
import dk.cocode.markdown.ScriptHarness.run
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Runs scripts/release-check.sh, the release workflow's gate before it builds. */
class ReleaseCheckTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val repo: File by lazy { ScriptHarness.repo(tmp.newFolder("repo")) }

    private val secrets = mapOf(
        "KEYSTORE_BASE64" to "a2V5",
        "KEYSTORE_PASSWORD" to "store",
        "KEY_ALIAS" to "android",
        "KEY_PASSWORD" to "key",
    )

    private fun check(version: String, env: Map<String, String> = secrets): ScriptHarness.Result {
        File(repo, "gradle.properties").writeText("org.gradle.jvmargs=-Xmx2048m\nVERSION_NAME=$version\nVERSION_CODE=1\n")
        val cleared = secrets.keys.associateWith { "" }
        return run(repo, listOf("sh", "scripts/release-check.sh"), env = cleared + env)
    }

    @Test
    fun `a new semver version passes and is printed`() {
        listOf("0.1.0", "1.2.3", "1.0.0-rc.1", "2.0.0+build.5").forEach {
            val result = check(it)
            assertEquals("should pass: $it (${result.out})", 0, result.code)
            assertEquals(it, result.out.trim())
        }
    }

    @Test
    fun `a missing secret is named`() {
        val result = check("1.0.0", secrets - "KEY_ALIAS" - "KEY_PASSWORD")
        assertNotEquals(0, result.code)
        assertTrue(result.out.contains("KEY_ALIAS"))
        assertTrue(result.out.contains("KEY_PASSWORD"))
        assertFalse(result.out.contains("KEYSTORE_PASSWORD"))
    }

    @Test
    fun `every secret is required`() {
        secrets.keys.forEach { name ->
            val result = check("1.0.0", secrets - name)
            assertNotEquals("should fail without $name", 0, result.code)
            assertTrue(result.out.contains(name))
        }
    }

    @Test
    fun `a non-semver version is refused`() {
        listOf("1.2", "v1.2.3", "01.2.3", "1.2.3.4", "latest", "").forEach {
            val result = check(it)
            assertNotEquals("should refuse: '$it'", 0, result.code)
            assertTrue(result.out.contains("not SemVer"))
        }
    }

    @Test
    fun `a version whose tag exists is refused`() {
        commit(repo, "feat: a")
        ok(repo, "git", "tag", "v1.2.3")
        val result = check("1.2.3")
        assertNotEquals(0, result.code)
        assertTrue(result.out.contains("v1.2.3 already exists"))
        assertEquals(0, check("1.2.4").code)
    }
}
