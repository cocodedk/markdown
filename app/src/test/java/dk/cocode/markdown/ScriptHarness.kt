package dk.cocode.markdown

import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/** Runs the repository's hooks and scripts in throwaway git repositories, with stub tools. */
object ScriptHarness {

    val root: File = generateSequence(File("").absoluteFile) { it.parentFile }
        .first { File(it, "settings.gradle.kts").isFile }

    class Result(val code: Int, val out: String)

    fun run(
        dir: File,
        command: List<String>,
        stdin: String = "",
        env: Map<String, String> = emptyMap(),
    ): Result {
        val builder = ProcessBuilder(command).directory(dir).redirectErrorStream(true)
        builder.environment().apply {
            put("GIT_CONFIG_GLOBAL", "/dev/null")
            put("GIT_CONFIG_NOSYSTEM", "1")
            put("GIT_AUTHOR_NAME", "Test")
            put("GIT_AUTHOR_EMAIL", "test@example.com")
            put("GIT_COMMITTER_NAME", "Test")
            put("GIT_COMMITTER_EMAIL", "test@example.com")
            // An empty value unsets the variable, so a script sees its own default.
            env.forEach { (name, value) -> if (value.isEmpty()) remove(name) else put(name, value) }
        }
        val process = builder.start()
        // Fed on its own thread, so a script that writes a lot before reading cannot deadlock us.
        val feeder = thread(name = "stdin: ${command.first()}") {
            try {
                process.outputStream.use { it.write(stdin.toByteArray()) }
            } catch (_: IOException) {
                // The script exited without reading all of its input: its choice, not an error.
            }
        }
        val out = process.inputStream.bufferedReader().readText()
        check(process.waitFor(60, TimeUnit.SECONDS)) { "timed out: $command" }
        feeder.join()
        return Result(process.exitValue(), out)
    }

    /** Runs [command] and fails the test when it does not exit 0. */
    fun ok(dir: File, vararg command: String): String {
        val result = run(dir, command.toList())
        check(result.code == 0) { "${command.joinToString(" ")} exited ${result.code}: ${result.out}" }
        return result.out.trim()
    }

    /** A fresh git repository in [dir] holding copies of `.githooks/` and `scripts/`. */
    fun repo(dir: File): File {
        File(root, ".githooks").copyRecursively(File(dir, ".githooks"))
        File(root, "scripts").copyRecursively(File(dir, "scripts"))
        ok(dir, "git", "init", "-q")
        ok(dir, "git", "symbolic-ref", "HEAD", "refs/heads/main")
        return dir
    }

    fun commit(dir: File, message: String): String {
        ok(dir, "git", "commit", "-q", "--allow-empty", "-m", message)
        return ok(dir, "git", "rev-parse", "HEAD")
    }

    /** Writes an executable stub called [name] into [bin]. */
    fun stub(bin: File, name: String, body: String) {
        bin.mkdirs()
        val file = File(bin, name)
        file.writeText("#!/bin/sh\n$body\n")
        file.setExecutable(true)
    }

    fun pathWith(bin: File): String = bin.absolutePath + File.pathSeparator + System.getenv("PATH")
}
