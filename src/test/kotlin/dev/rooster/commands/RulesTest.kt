package dev.rooster.commands

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock

class RulesTest {

    private val sender = mock<org.bukkit.command.CommandSender>()
    private val ctx = Context(sender, emptyMap())

    @Test
    fun `no rules returns Valid`() {
        val result = Rules().evaluate(ctx)
        assertEquals(IsValidResult.Valid, result)
    }

    @Test
    fun `Rejected rule fires when condition is true`() {
        var errorFired = false
        val rules = Rules(
            ArgumentRule.Rejected { errorFired = true } to { true }
        )
        val result = rules.evaluate(ctx)
        assertTrue(result is IsValidResult.Invalid)
        (result as IsValidResult.Invalid).error(ctx)
        assertTrue(errorFired)
    }

    @Test
    fun `Rejected rule does not fire when condition is false`() {
        val rules = Rules(
            ArgumentRule.Rejected { } to { false }
        )
        assertEquals(IsValidResult.Valid, rules.evaluate(ctx))
    }

    @Test
    fun `first matching Rejected rule wins`() {
        val fired = mutableListOf<Int>()
        val rules = Rules(
            ArgumentRule.Rejected { fired.add(1) } to { true },
            ArgumentRule.Rejected { fired.add(2) } to { true },
        )
        rules.evaluate(ctx).let { if (it is IsValidResult.Invalid) it.error(ctx) }
        assertEquals(listOf(1), fired)
    }

    @Test
    fun `Accepted rule does not block evaluation`() {
        val rules = Rules(
            ArgumentRule.Accepted to { true },
        )
        assertEquals(IsValidResult.Valid, rules.evaluate(ctx))
    }

    @Test
    fun `further is evaluated when all rules pass`() {
        var furtherCalled = false
        val rules = Rules(
            further = {
                furtherCalled = true
                Rules()
            }
        )
        rules.evaluate(ctx)
        assertTrue(furtherCalled)
    }

    @Test
    fun `further is not evaluated when a Rejected rule fires`() {
        var furtherCalled = false
        val rules = Rules(
            ArgumentRule.Rejected { } to { true },
            further = { furtherCalled = true; Rules() }
        )
        rules.evaluate(ctx)
        assertFalse(furtherCalled)
    }

    @Test
    fun `further Rejected fires correctly`() {
        var errorFired = false
        val rules = Rules(
            further = {
                Rules(ArgumentRule.Rejected { errorFired = true } to { true })
            }
        )
        val result = rules.evaluate(ctx)
        assertTrue(result is IsValidResult.Invalid)
        (result as IsValidResult.Invalid).error(ctx)
        assertTrue(errorFired)
    }
}
