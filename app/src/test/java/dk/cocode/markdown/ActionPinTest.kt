package dk.cocode.markdown

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** A workflow `uses:` value: one of the allowed actions, at a full commit SHA, with a `# v…` comment. */
object ActionPin {
    private val actions = listOf(
        "actions/checkout", "actions/setup-java", "actions/upload-artifact", "softprops/action-gh-release",
        "actions/configure-pages", "actions/upload-pages-artifact", "actions/deploy-pages",
    )
    private val pattern = Regex(
        """^(${actions.joinToString("|") { Regex.escape(it) }})@[0-9a-f]{40} # v\S+$""",
    )

    fun isValid(uses: String): Boolean = pattern.matches(uses)
}

class ActionPinTest {

    private val sha = "0123456789abcdef0123456789abcdef01234567"

    @Test
    fun `a different full sha with a version comment is pinned`() {
        assertTrue(ActionPin.isValid("actions/checkout@$sha # v8.1.0"))
        assertTrue(ActionPin.isValid("actions/setup-java@$sha # v6"))
        assertTrue(ActionPin.isValid("actions/upload-artifact@$sha # v5.0.0"))
        assertTrue(ActionPin.isValid("softprops/action-gh-release@$sha # v3.1.0"))
    }

    @Test
    fun `the pages actions are allowed`() {
        assertTrue(ActionPin.isValid("actions/configure-pages@$sha # v6.0.0"))
        assertTrue(ActionPin.isValid("actions/upload-pages-artifact@$sha # v5.0.0"))
        assertTrue(ActionPin.isValid("actions/deploy-pages@$sha # v4.0.5"))
    }

    @Test
    fun `a tag is not pinned`() {
        assertFalse(ActionPin.isValid("actions/checkout@v7"))
        assertFalse(ActionPin.isValid("actions/checkout@v7 # v7"))
    }

    @Test
    fun `a short sha is not pinned`() {
        assertFalse(ActionPin.isValid("actions/checkout@${sha.take(7)} # v7.0.0"))
    }

    @Test
    fun `an uppercase sha is not pinned`() {
        assertFalse(ActionPin.isValid("actions/checkout@${sha.uppercase()} # v7.0.0"))
    }

    @Test
    fun `a missing version comment is not pinned`() {
        assertFalse(ActionPin.isValid("actions/checkout@$sha"))
        assertFalse(ActionPin.isValid("actions/checkout@$sha # 7.0.0"))
    }

    @Test
    fun `an action outside the list is not allowed`() {
        assertFalse(ActionPin.isValid("actions/cache@$sha # v4.2.0"))
        assertFalse(ActionPin.isValid("evil/actions/checkout@$sha # v7.0.0"))
    }
}
