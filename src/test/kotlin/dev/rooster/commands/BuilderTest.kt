package dev.rooster.commands

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock

class BuilderTest {

    private val sender = mock<org.bukkit.command.CommandSender>()
    private val emptyCtx = Context(sender, emptyMap())

    @Test
    fun `build produces argument with correct key and type`() {
        val type = object : ArgumentType<String> {}
        val arg = ArgumentBuilder("name", type) { TransformResult.Success(it) }.build()
        assertEquals("name", arg.key)
        assertSame(type, arg.type)
    }

    @Test
    fun `executor is set via onExecute`() {
        var fired = false
        val arg = ArgumentBuilder("x", object : ArgumentType<String> {}) { TransformResult.Success(it) }
            .onExecute { fired = true }
            .build()
        arg.executor!!.invoke(emptyCtx)
        assertTrue(fired)
    }

    @Test
    fun `isValid callback is stored and invoked`() {
        var invoked = false
        val arg = ArgumentBuilder("x", object : ArgumentType<String> {}) { TransformResult.Success(it) }
            .isValid { _, _ -> invoked = true; IsValidResult.Valid }
            .build()
        arg.invokeIsValid(emptyCtx, "v", TransformResult.Success("v"))
        assertTrue(invoked)
    }

    @Test
    fun `isTarget callback is stored and invoked`() {
        var invoked = false
        val arg = ArgumentBuilder("x", object : ArgumentType<String> {}) { TransformResult.Success(it) }
            .isTarget { invoked = true; true }
            .build()
        arg.invokeIsTarget(emptyCtx, "v")
        assertTrue(invoked)
    }

    @Test
    fun `derive adds derivation that runs in context`() {
        val arg = ArgumentBuilder("x", object : ArgumentType<String> {}) { TransformResult.Success(it) }
            .derive("computed") { "hello" }
            .build()
        val result = arg.derivations["computed"]!!.invoke(emptyCtx)
        assertEquals("hello", result)
    }

    @Test
    fun `then with builder adds child`() {
        val child = ArgumentBuilder("child", object : ArgumentType<String> {}) { TransformResult.Success(it) }
        val parent = ArgumentBuilder("parent", object : ArgumentType<String> {}) { TransformResult.Success(it) }
            .then(child)
            .build()
        assertEquals(1, parent.children.size)
        assertEquals("child", parent.children[0].key)
    }

    @Test
    fun `then with block adds multiple children`() {
        val parent = ArgumentBuilder("parent", object : ArgumentType<String> {}) { TransformResult.Success(it) }
            .then {
                val c1 = ArgumentBuilder("c1", object : ArgumentType<String> {}) { TransformResult.Success(it) }
                val c2 = ArgumentBuilder("c2", object : ArgumentType<String> {}) { TransformResult.Success(it) }
                +c1
                +c2
            }
            .build()
        assertEquals(2, parent.children.size)
    }

    @Test
    fun `ArgumentPart then returns tail not head`() {
        val tail = ArgumentBuilder("tail", object : ArgumentType<String> {}) { TransformResult.Success(it) }
        val head = ArgumentBuilder("head", object : ArgumentType<String> {}) { TransformResult.Success(it) }
        val part = ArgumentPart(head, tail)

        val result = ArgumentBuilder("root", object : ArgumentType<String> {}) { TransformResult.Success(it) }
            .then(part)
        assertSame(tail, result)
    }

    @Test
    fun `invokeIsTarget returns true when no isTarget set`() {
        val arg = ArgumentBuilder("x", object : ArgumentType<String> {}) { TransformResult.Success(it) }.build()
        assertTrue(arg.invokeIsTarget(emptyCtx, "anything"))
    }

    @Test
    fun `isWildcard returns true when isValid is null`() {
        val arg = ArgumentBuilder("x", object : ArgumentType<String> {}) { TransformResult.Success(it) }.build()
        assertTrue(arg.isWildcard())
    }

    @Test
    fun `isWildcard returns false when isTarget is set`() {
        val arg = ArgumentBuilder("x", object : ArgumentType<String> {}) { TransformResult.Success(it) }
            .isTarget { true }
            .build()
        assertFalse(arg.isWildcard())
    }
}
