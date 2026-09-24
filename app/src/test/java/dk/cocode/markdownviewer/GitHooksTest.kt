package dk.cocode.markdownviewer

import dk.cocode.markdownviewer.ScriptHarness.commit
import dk.cocode.markdownviewer.ScriptHarness.ok
import dk.cocode.markdownviewer.ScriptHarness.run
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Runs the git hooks in a throwaway repository. */
class GitHooksTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val repo: File by lazy { ScriptHarness.repo(tmp.newFolder("repo")) }

    private fun commitMsg(subject: String): Int {
        val message = File(tmp.root, "MSG").apply { writeText("$subject\n\nbody\n") }
        return run(repo, listOf("sh", ".githooks/commit-msg", message.absolutePath)).code
    }

    private fun preCommitWith(path: String, lines: Int): ScriptHarness.Result {
        val file = File(repo, path)
        file.parentFile?.mkdirs()
        file.writeText((1..lines).joinToString("") { "line $it\n" })
        ok(repo, "git", "add", path)
        return run(repo, listOf("sh", ".githooks/pre-commit"))
    }

    private fun prePush(url: String, vararg refs: String) =
        run(repo, listOf("sh", ".githooks/pre-push", "origin", url), stdin = refs.joinToString("") { "$it\n" })

    private val zero = "0".repeat(40)
    private val url = "https://github.com/cocodedk/markdown-viewer.git"

    @Test
    fun `commit-msg accepts conventional, merge and revert subjects`() {
        listOf(
            "feat(01-viewer): 01-viewer",
            "docs(specs): everything the public repository needs",
            "chore: seed the owner files and the Gradle wrapper",
            "ci: pin actions",
            "Merge branch 'feature' into main",
            "Revert \"feat: something\"",
        ).forEach { assertEquals("should accept: $it", 0, commitMsg(it)) }
    }

    @Test
    fun `commit-msg refuses other subjects`() {
        listOf("update stuff", "feature: x", "feat:no space", "feat(): empty scope", "wip(ui): x", "")
            .forEach { assertNotEquals("should refuse: $it", 0, commitMsg(it)) }
    }

    @Test
    fun `pre-commit refuses key material and local config`() {
        listOf("release.keystore", "upload.jks", "keystore.properties", "app/local.properties").forEach {
            val result = preCommitWith(it, 1)
            assertNotEquals("should refuse $it", 0, result.code)
            assertTrue(result.out.contains(it))
            ok(repo, "git", "rm", "-q", "--cached", it)
        }
    }

    /**
     * Stages a file named by the printf format [name] in a fresh repository, so non-ASCII and
     * control characters are made by the shell, not the JVM's file-name encoding.
     */
    private fun preCommitWithRaw(name: String, lines: Int): ScriptHarness.Result {
        val fresh = ScriptHarness.repo(tmp.newFolder())
        val stage = """
            f=${'$'}(printf "${'$'}1"); mkdir -p "${'$'}(dirname -- "${'$'}f")"; : > "${'$'}f"; i=0
            while [ ${'$'}i -lt "${'$'}2" ]; do echo "line ${'$'}i" >> "${'$'}f"; i=${'$'}((i + 1)); done
            git add -- "${'$'}f"
        """.trimIndent()
        val staged = run(fresh, listOf("sh", "-c", stage, "sh", name, "$lines"))
        assertEquals(staged.out, 0, staged.code)
        return run(fresh, listOf("sh", ".githooks/pre-commit"))
    }

    @Test
    fun `pre-commit refuses key material whatever the file name holds`() {
        listOf(
            "r\\303\\251lease.keystore",
            "\\346\\227\\245\\346\\234\\254.jks",
            "\\303\\246pp/local.properties",
            "my release.jks",
            "quo\"te.keystore",
            "back\\\\slash.keystore",
            "tab\\there.keystore",
            "RELEASE.KEYSTORE",
            "app/Keystore.Properties",
            "evil\\nrelease.keystore",
        ).forEach {
            val result = preCommitWithRaw(it, 1)
            assertNotEquals("should refuse $it", 0, result.code)
            assertTrue(it, result.out.contains("never committed"))
        }
    }

    @Test
    fun `pre-commit counts lines of code files with unusual names`() {
        listOf("B\\303\\270g.kt", "sp ace.kts", "evil\\nlong.kt").forEach {
            assertNotEquals("should refuse $it", 0, preCommitWithRaw(it, 201).code)
        }
        assertEquals(0, preCommitWithRaw("\\303\\230k.kt", 200).code)
    }

    @Test
    fun `pre-commit refuses a staged code file over 200 lines`() {
        listOf("Big.kt", "build.gradle.kts", "a.xml", "libs.toml", "ci.yml", "x.sh").forEach {
            val result = preCommitWith(it, 201)
            assertNotEquals("should refuse $it", 0, result.code)
            assertTrue(result.out.contains("201 lines"))
            ok(repo, "git", "rm", "-q", "--cached", it)
        }
    }

    @Test
    fun `pre-commit accepts code at 200 lines and long prose`() {
        assertEquals(0, preCommitWith("Ok.kt", 200).code)
        assertEquals(0, preCommitWith("README.md", 500).code)
    }

    @Test
    fun `pre-commit judges the staged copy, not the working file`() {
        preCommitWith("Grow.kt", 10)
        File(repo, "Grow.kt").writeText("x\n".repeat(300))
        assertEquals(0, run(repo, listOf("sh", ".githooks/pre-commit")).code)
    }

    @Test
    fun `pre-push refuses remotes outside github cocodedk`() {
        val sha = commit(repo, "feat: a")
        listOf(
            "https://gitlab.com/cocodedk/markdown-viewer.git",
            "https://github.com/someone/markdown-viewer.git",
            "https://evil.example/github.com/cocodedk/x.git",
        ).forEach {
            assertNotEquals("should refuse $it", 0, prePush(it, "refs/heads/x $sha refs/heads/x $zero").code)
        }
        assertEquals(0, prePush("git@github.com:cocodedk/markdown-viewer.git", "refs/heads/x $sha refs/heads/x $zero").code)
    }

    @Test
    fun `pre-push allows a fast-forward of main and new branches`() {
        val first = commit(repo, "feat: a")
        val second = commit(repo, "feat: b")
        assertEquals(0, prePush(url, "refs/heads/main $second refs/heads/main $first").code)
        assertEquals(0, prePush(url, "refs/heads/main $first refs/heads/main $zero").code)
    }

    @Test
    fun `pre-push refuses rewriting main`() {
        val first = commit(repo, "feat: a")
        val second = commit(repo, "feat: b")
        val result = prePush(url, "refs/heads/main $first refs/heads/main $second")
        assertNotEquals(0, result.code)
        assertTrue(result.out.contains("rewrite main"))
    }

    @Test
    fun `pre-push allows rewriting another branch`() {
        val first = commit(repo, "feat: a")
        val second = commit(repo, "feat: b")
        assertEquals(0, prePush(url, "refs/heads/topic $first refs/heads/topic $second").code)
    }

    @Test
    fun `pre-push refuses deleting main`() {
        val sha = commit(repo, "feat: a")
        val result = prePush(url, "(delete) $zero refs/heads/main $sha")
        assertNotEquals(0, result.code)
        assertTrue(result.out.contains("delete main"))
    }
}
