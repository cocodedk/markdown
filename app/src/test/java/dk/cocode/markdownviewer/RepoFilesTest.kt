package dk.cocode.markdownviewer

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The public repository's files, read from the project directory. No network. */
class RepoFilesTest {

    private val root: File = generateSequence(File("").absoluteFile) { it.parentFile }
        .first { File(it, "settings.gradle.kts").isFile }

    private fun text(path: String): String {
        val file = File(root, path)
        assertTrue("$path is missing", file.isFile)
        return file.readText()
    }

    private val pinned = setOf(
        "actions/checkout@9c091bb21b7c1c1d1991bb908d89e4e9dddfe3e0 # v7.0.0",
        "actions/setup-java@1bcf9fb12cf4aa7d266a90ae39939e61372fe520 # v5.4.0",
        "actions/upload-artifact@ea165f8d65b6e75b540449e92b4886f43607fa02 # v4",
        "softprops/action-gh-release@718ea10b132b3b2eba29c1007bb80653f286566b # v3.0.1",
    )

    private val workflows = listOf(".github/workflows/ci.yml", ".github/workflows/release-apk.yml")

    @Test
    fun `every public file exists`() {
        val files = listOf(
            "README.md", "LICENSE", "CONTRIBUTING.md", "SECURITY.md", ".gitattributes", "llms.txt",
            ".github/pull_request_template.md",
            ".github/ISSUE_TEMPLATE/bug_report.md",
            ".github/ISSUE_TEMPLATE/feature_request.md",
            ".github/dependabot.yml",
            ".githooks/pre-commit", ".githooks/commit-msg", ".githooks/pre-push",
            "scripts/install-hooks.sh", "scripts/setup-repo.sh", "scripts/setup-signing.sh",
            "scripts/release-check.sh", "scripts/release-keystore.sh",
            "fastlane/metadata/android/en-US/title.txt",
            "fastlane/metadata/android/en-US/short_description.txt",
            "fastlane/metadata/android/en-US/full_description.txt",
            "fastlane/metadata/android/en-US/changelogs/1.txt",
        ) + workflows
        val missing = files.filterNot { File(root, it).isFile }
        assertEquals("missing files", emptyList<String>(), missing)
    }

    @Test
    fun `workflows use only the pinned actions`() {
        for (path in workflows) {
            val uses = text(path).lines()
                .map { it.trim().removePrefix("- ").trim() }
                .filter { it.startsWith("uses:") }
                .map { it.removePrefix("uses:").trim() }
            assertTrue("$path uses no action", uses.isNotEmpty())
            uses.forEach { assertTrue("$path uses an unpinned action: $it", it in pinned) }
        }
    }

    @Test
    fun `ci has a verify job`() {
        val ci = text(".github/workflows/ci.yml")
        assertTrue(Regex("""(?m)^ {2}verify:\s*$""").containsMatchIn(ci))
        assertTrue(ci.contains("./gradlew --no-daemon assembleDebug testDebugUnitTest lintDebug"))
    }

    @Test
    fun `the release runs only by hand and attaches the apk`() {
        val release = text(".github/workflows/release-apk.yml")
        assertTrue(release.contains("workflow_dispatch"))
        assertFalse("release has a push trigger", release.contains("push"))
        assertFalse("release has a tags trigger", release.contains("tags"))
        assertTrue(release.contains("cancel-in-progress: false"))
        assertTrue(release.contains("MarkdownViewer.apk"))
        assertTrue("release does not run its checks", release.contains("sh scripts/release-check.sh"))
    }

    @Test
    fun `the release checks, builds, verifies and publishes in order`() {
        val release = text(".github/workflows/release-apk.yml")
        val order = listOf(
            "fetch-depth: 0",
            "sh scripts/release-check.sh",
            "sh scripts/release-keystore.sh",
            "./gradlew --no-daemon assembleRelease",
            "cp app/build/outputs/apk/release/app-release.apk MarkdownViewer.apk",
            "apksigner\" verify --print-certs MarkdownViewer.apk",
            "if: always()\n        run: rm -f \"\$RUNNER_TEMP/release.keystore\"",
            "tag_name: v\${{ steps.version.outputs.name }}",
        ).map { step -> release.indexOf(step).also { assertTrue("release lacks: $step", it >= 0) } }
        assertEquals("release steps out of order", order.sorted(), order)
        listOf(
            "permissions:\n  contents: write",
            "group: release",
            "target_commitish: \${{ github.sha }}",
            "generate_release_notes: true",
            "files: MarkdownViewer.apk",
        ).forEach { assertTrue("release lacks: $it", release.contains(it)) }
    }

    @Test
    fun `install-hooks sets core hooksPath`() {
        assertTrue(text("scripts/install-hooks.sh").contains("core.hooksPath"))
    }

    @Test
    fun `pre-push names the owner`() {
        val prePush = text(".githooks/pre-push")
        assertTrue(prePush.contains("cocodedk"))
        assertFalse(prePush.contains("<OWNER>"))
    }

    @Test
    fun `setup-repo never forces or skips hooks`() {
        val setupRepo = text("scripts/setup-repo.sh")
        assertFalse(setupRepo.contains("--force"))
        assertFalse(setupRepo.contains("--no-verify"))
    }

    @Test
    fun `the short description is under 80 characters`() {
        val short = text("fastlane/metadata/android/en-US/short_description.txt").trim()
        assertTrue("short description is ${short.length} characters", short.isNotEmpty() && short.length < 80)
    }

    @Test
    fun `no code file is over 200 lines`() {
        val skipped = setOf("build", ".gradle", ".git", ".kotlin", ".idea", ".cxx", "scratchpad", ".zvec-grep")
        val code = setOf("kt", "kts", "xml", "toml", "yml", "yaml", "sh")
        val tooLong = root.walkTopDown()
            .onEnter { it == root || it.name !in skipped }
            .filter { it.isFile && (it.extension in code || it.parentFile?.name == ".githooks") }
            .map { it.relativeTo(root).path to it.readLines().size }
            .filter { (_, lines) -> lines > 200 }
            .toList()
        assertEquals("code files over 200 lines", emptyList<Pair<String, Int>>(), tooLong)
    }
}
