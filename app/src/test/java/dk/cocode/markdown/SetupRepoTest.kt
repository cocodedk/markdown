package dk.cocode.markdown

import dk.cocode.markdown.ScriptHarness.ok
import dk.cocode.markdown.ScriptHarness.run
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Runs install-hooks.sh and setup-repo.sh against a stub gh that records every call. */
class SetupRepoTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val repo: File by lazy { ScriptHarness.repo(tmp.newFolder("repo")) }
    private val log: File by lazy { File(tmp.root, "gh.log") }
    private val bin: File by lazy {
        tmp.newFolder("bin").also {
            ScriptHarness.stub(
                it, "gh",
                """
                echo "gh ${'$'}*" >> "${'$'}GH_LOG"
                case "${'$'}*" in
                    *check-runs*) echo "${'$'}{VERIFY_RUNS:-0}" ;;
                    *protection*) cat >> "${'$'}GH_LOG" ;;
                esac
                """.trimIndent(),
            )
        }
    }

    private fun setupRepo(verifyRuns: Int): ScriptHarness.Result {
        val env = mapOf("PATH" to ScriptHarness.pathWith(bin), "GH_LOG" to log.absolutePath, "VERIFY_RUNS" to "$verifyRuns")
        return run(repo, listOf("sh", "scripts/setup-repo.sh"), env = env)
    }

    private fun calls() = log.readLines().filter { it.startsWith("gh ") }

    @Test
    fun `install-hooks points git at githooks and makes them executable`() {
        val result = run(repo, listOf("sh", "scripts/install-hooks.sh"))
        assertEquals(result.out, 0, result.code)
        assertEquals(".githooks", ok(repo, "git", "config", "core.hooksPath"))
        listOf("pre-commit", "commit-msg", "pre-push").forEach {
            assertTrue("$it is not executable", File(repo, ".githooks/$it").canExecute())
        }
    }

    @Test
    fun `install-hooks works from outside the repository`() {
        val script = File(repo, "scripts/install-hooks.sh").absolutePath
        assertEquals(0, run(tmp.root, listOf("sh", script)).code)
        assertEquals(".githooks", ok(repo, "git", "config", "core.hooksPath"))
    }

    @Test
    fun `setup-repo configures the named repository and installs the hooks`() {
        val result = setupRepo(verifyRuns = 0)
        assertEquals(result.out, 0, result.code)
        val edits = calls().filter { it.startsWith("gh repo edit") }
        assertTrue(edits.isNotEmpty())
        edits.forEach { assertTrue(it, it.contains("gh repo edit cocodedk/markdown ")) }
        val all = edits.joinToString(" ")
        listOf("android", "markdown", "markdown-editor", "kotlin", "jetpack-compose", "f-droid")
            .forEach { assertTrue("topic $it", all.contains("--add-topic $it")) }
        listOf("--enable-squash-merge", "--enable-rebase-merge", "--enable-merge-commit=false", "--delete-branch-on-merge")
            .forEach { assertTrue(it, all.contains(it)) }
        assertEquals(".githooks", ok(repo, "git", "config", "core.hooksPath"))
    }

    @Test
    fun `setup-repo never trusts an existing remote`() {
        ok(repo, "git", "remote", "add", "origin", "https://github.com/someone-else/fork.git")
        assertEquals(0, setupRepo(verifyRuns = 1).code)
        assertFalse(log.readText().contains("someone-else"))
        calls().filter { it.startsWith("gh repo") || it.startsWith("gh api") }
            .forEach { assertTrue(it, it.contains("cocodedk/markdown")) }
    }

    @Test
    fun `setup-repo skips protection until CI has run`() {
        val result = setupRepo(verifyRuns = 0)
        assertEquals(0, result.code)
        assertTrue(result.out.contains("Skipped"))
        assertFalse(calls().any { it.contains("protection") })
    }

    @Test
    fun `setup-repo protects main without enforcing admins once CI has run`() {
        val result = setupRepo(verifyRuns = 1)
        assertEquals(result.out, 0, result.code)
        val put = calls().single { it.contains("protection") }
        assertTrue(put.contains("--method PUT repos/cocodedk/markdown/branches/main/protection"))
        val body = log.readText().replace(Regex("\\s+"), "")
        listOf(
            "\"contexts\":[\"verify\"]",
            "\"enforce_admins\":false",
            "\"required_approving_review_count\":0",
            "\"allow_force_pushes\":false",
            "\"allow_deletions\":false",
        ).forEach { assertTrue("protection body lacks $it", body.contains(it)) }
    }

    @Test
    fun `setup-repo is safe to run again and prints each step`() {
        val first = setupRepo(verifyRuns = 1)
        val second = setupRepo(verifyRuns = 1)
        assertEquals(0, first.code)
        assertEquals(0, second.code)
        listOf("Description and topics", "Merge settings", "Git hooks", "Protect main")
            .forEach { assertTrue(it, second.out.contains("==> $it")) }
        assertFalse(log.readText().contains("--force"))
    }
}
