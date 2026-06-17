package dev.rooster.commands.commandapi

import dev.rooster.commands.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock

class DispatchTest {

    private val sender = mock<org.bukkit.command.CommandSender>()

    private fun execute(rawArgs: Map<String, Any?>, vararg path: Argument<*, *>) =
        Compiler.executeRaw(sender, rawArgs, path.toList())

    private fun context(rawArgs: Map<String, Any?>, vararg path: Argument<*, *>) =
        Compiler.buildContextRaw(sender, rawArgs, path.toList())

    // -- Helpers --

    private fun strArg(key: String, block: ArgumentBuilder<String, String>.() -> Unit = {}): Argument<String, String> =
        ArgumentBuilder(key, StringArgumentType) { TransformResult.Success(it) }.apply(block).build()

    // -- Basic dispatch --

    @Test
    fun `single node transform and executor fire`() {
        var result: String? = null
        val arg = strArg("name") { onExecute { result = args["name"] as String } }

        execute(mapOf("name" to "alice"), arg)

        assertEquals("alice", result)
    }

    @Test
    fun `transform result is stored under node key`() {
        val arg = ArgumentBuilder("n", StringArgumentType) { TransformResult.Success("T_$it") }
            .onExecute { }
            .build()

        val ctx = context(mapOf("n" to "hello"), arg)

        assertEquals("T_hello", ctx["n"])
    }

    @Test
    fun `chained nodes build context in order`() {
        var capturedArgs: Map<String, Any>? = null
        val first = strArg("first")
        val second = strArg("second") { onExecute { capturedArgs = args } }

        execute(mapOf("first" to "a", "second" to "b"), first, second)

        assertEquals("a", capturedArgs!!["first"])
        assertEquals("b", capturedArgs!!["second"])
    }

    @Test
    fun `isValid failure fires error and stops executor`() {
        var errorFired = false
        var executorFired = false
        val arg = ArgumentBuilder("x", StringArgumentType) { TransformResult.Success(it) }
            .isValid { _, _ -> IsValidResult.Invalid { errorFired = true } }
            .onExecute { executorFired = true }
            .build()

        execute(mapOf("x" to "v"), arg)

        assertTrue(errorFired)
        assertFalse(executorFired)
    }

    @Test
    fun `isValid success allows executor to fire`() {
        var fired = false
        val arg = ArgumentBuilder("x", StringArgumentType) { TransformResult.Success(it) }
            .isValid { _, _ -> IsValidResult.Valid }
            .onExecute { fired = true }
            .build()

        execute(mapOf("x" to "v"), arg)
        assertTrue(fired)
    }

    @Test
    fun `isValid receives K and TransformResult`() {
        var receivedK: String? = null
        var receivedResult: TransformResult<String>? = null
        val arg = ArgumentBuilder("x", StringArgumentType) { TransformResult.Success("T_$it") }
            .isValid { k, result ->
                @Suppress("UNCHECKED_CAST")
                receivedK = k
                @Suppress("UNCHECKED_CAST")
                receivedResult = result as TransformResult<String>
                IsValidResult.Valid
            }
            .onExecute { }
            .build()

        execute(mapOf("x" to "hello"), arg)

        assertEquals("hello", receivedK)
        assertEquals(TransformResult.Success("T_hello"), receivedResult)
    }

    @Test
    fun `transform Failure with Valid isValid throws`() {
        val arg = ArgumentBuilder("x", StringArgumentType) { TransformResult.Failure }
            .isValid { _, _ -> IsValidResult.Valid }
            .onExecute { }
            .build()

        assertThrows(IllegalStateException::class.java) {
            execute(mapOf("x" to "v"), arg)
        }
    }

    @Test
    fun `derivation result is available in executor`() {
        var derived: Any? = null
        val arg = strArg("x") {
            derive("computed") { "prefix_${args["x"]}" }
            onExecute { derived = args["computed"] }
        }

        execute(mapOf("x" to "hello"), arg)

        assertEquals("prefix_hello", derived)
    }

    @Test
    fun `derivation on earlier node is available to later node executor`() {
        var derived: Any? = null
        val first = strArg("first") { derive("label") { "from_${args["first"]}" } }
        val second = strArg("second") { onExecute { derived = args["label"] } }

        execute(mapOf("first" to "a", "second" to "b"), first, second)

        assertEquals("from_a", derived)
    }

    @Test
    fun `literal node key is set from literal name`() {
        val ctx = context(emptyMap(), ArgumentBuilder("hello", LiteralArgumentType("hello")) { TransformResult.Success(it) }.build())
        assertEquals("hello", ctx["hello"])
    }

    @Test
    fun `isValid error context contains previously resolved args`() {
        var errorCtxArgs: Map<String, Any>? = null
        val first = strArg("first")
        val second = ArgumentBuilder("second", StringArgumentType) { TransformResult.Success(it) }
            .isValid { _, _ -> IsValidResult.Invalid { errorCtxArgs = args } }
            .onExecute { }
            .build()

        execute(mapOf("first" to "a", "second" to "b"), first, second)

        assertEquals("a", errorCtxArgs!!["first"])
    }
}
