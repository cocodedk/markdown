package dk.cocode.markdown

import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** The harness itself: feeding stdin must not race the script it feeds. */
class ScriptHarnessTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun `a script that exits without reading stdin still returns its code and output`() {
        val input = "x".repeat(1024 * 1024) + "\n"
        repeat(25) {
            val result = ScriptHarness.run(tmp.root, listOf("sh", "-c", "echo early; exit 3"), stdin = input)
            assertEquals("run $it", 3, result.code)
            assertEquals("run $it", "early\n", result.out)
        }
    }

    @Test
    fun `a script that reads all of stdin gets all of it`() {
        val input = "line\n".repeat(200_000)
        val result = ScriptHarness.run(tmp.root, listOf("sh", "-c", "wc -l"), stdin = input)
        assertEquals(0, result.code)
        assertEquals("200000", result.out.trim())
    }
}
