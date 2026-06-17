package dev.rooster.commands.commandapi

import dev.rooster.commands.*
import dev.rooster.commands.types.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock

class MergeTest {

    private val sender = mock<org.bukkit.command.CommandSender>()

    private fun execute(rawArgs: Map<String, Any?>, vararg path: Argument<*, *>) =
        Compiler.executeRaw(sender, rawArgs, path.toList())

    private fun strArg(
        key: String,
        transform: (String) -> String = { it },
        block: ArgumentBuilder<String, String>.() -> Unit = {},
    ): Argument<String, String> =
        ArgumentBuilder(key, StringArgumentType) { TransformResult.Success(transform(it)) }.apply(block).build()

    // -- Detection and ordering --

    @Test
    fun `two wildcards throws at build time`() {
        val a = strArg("a") { onExecute { } }
        val b = strArg("b") { onExecute { } }
        assertThrows(IllegalArgumentException::class.java) {
            Compiler.buildMergedArgument(listOf(a, b))
        }
    }

    @Test
    fun `isTarget nodes come before wildcard in ordered list`() {
        val a = strArg("a") { isTarget { true }; onExecute { } }
        val b = strArg("b") { onExecute { } }

        val info = Compiler.buildMergedArgument(listOf(b, a)) // b registered first
        assertEquals("a", info.ordered[0].key)  // a (has isTarget) is first
        assertEquals("b", info.ordered[1].key)  // b (wildcard) is last
    }

    @Test
    fun `synthetic key is ordered keys joined by pipe`() {
        val a = strArg("a") { isTarget { true } }
        val b = strArg("b")
        val info = Compiler.buildMergedArgument(listOf(a, b))
        assertEquals("a|b", info.syntheticKey)
    }

    // -- Dispatch --

    @Test
    fun `dispatches to first matching isTarget`() {
        var firedA = false
        var firedB = false
        val a = strArg("a") { isTarget { it.startsWith("@") }; onExecute { firedA = true } }
        val b = strArg("b") { onExecute { firedB = true } }

        val info = Compiler.buildMergedArgument(listOf(a, b))
        execute(mapOf(info.syntheticKey to "@alice"), info.mergedArg)

        assertTrue(firedA)
        assertFalse(firedB)
    }

    @Test
    fun `wildcard fires when no isTarget matches`() {
        var firedA = false
        var firedB = false
        val a = strArg("a") { isTarget { it.startsWith("@") }; onExecute { firedA = true } }
        val b = strArg("b") { onExecute { firedB = true } }

        val info = Compiler.buildMergedArgument(listOf(a, b))
        execute(mapOf(info.syntheticKey to "alice"), info.mergedArg)

        assertFalse(firedA)
        assertTrue(firedB)
    }

    @Test
    fun `no executor fires when nothing matches and no wildcard`() {
        var firedA = false
        val a = strArg("a") { isTarget { it.startsWith("@") }; onExecute { firedA = true } }
        val b = strArg("b") { isTarget { it.startsWith("#") }; onExecute { } }

        val info = Compiler.buildMergedArgument(listOf(a, b))
        execute(mapOf(info.syntheticKey to "alice"), info.mergedArg) // neither matches

        assertFalse(firedA)
    }

    @Test
    fun `multiple isTarget nodes checked in registration order`() {
        val fired = mutableListOf<String>()
        val a = strArg("a") { isTarget { true }; onExecute { fired.add("a") } }
        val b = strArg("b") { isTarget { true }; onExecute { fired.add("b") } }
        val c = strArg("c") { onExecute { fired.add("c") } }

        val info = Compiler.buildMergedArgument(listOf(a, b, c))
        execute(mapOf(info.syntheticKey to "x"), info.mergedArg)

        assertEquals(listOf("a"), fired) // first matching wins
    }

    // -- Context content after dispatch --

    @Test
    fun `route key derivation holds winner index`() {
        var capturedArgs: Map<String, Any>? = null
        val a = strArg("a") { isTarget { it.startsWith("@") }; onExecute { capturedArgs = args } }
        val b = strArg("b") { onExecute { capturedArgs = args } }

        val info = Compiler.buildMergedArgument(listOf(a, b))

        // a wins (index 0)
        execute(mapOf(info.syntheticKey to "@alice"), info.mergedArg)
        assertEquals(0, capturedArgs!![info.routeKey])

        capturedArgs = null

        // b wins (index 1)
        execute(mapOf(info.syntheticKey to "alice"), info.mergedArg)
        assertEquals(1, capturedArgs!![info.routeKey])
    }

    @Test
    fun `winner alt key holds T value not raw K`() {
        var capturedArgs: Map<String, Any>? = null
        val a = strArg("a", transform = { "T_$it" }) {
            isTarget { it.startsWith("@") }
            onExecute { capturedArgs = args }
        }
        val b = strArg("b") { onExecute { capturedArgs = args } }

        val info = Compiler.buildMergedArgument(listOf(a, b))
        execute(mapOf(info.syntheticKey to "@alice"), info.mergedArg)

        assertEquals("T_@alice", capturedArgs!!["a"])  // T value (prefixed), not K
        assertNull(capturedArgs!!["b"])                // losing alt key is absent
    }

    @Test
    fun `loser alt key is absent from context`() {
        var capturedArgs: Map<String, Any>? = null
        val a = strArg("a") { isTarget { it.startsWith("@") }; onExecute { capturedArgs = args } }
        val b = strArg("b") { onExecute { capturedArgs = args } }

        val info = Compiler.buildMergedArgument(listOf(a, b))
        execute(mapOf(info.syntheticKey to "@alice"), info.mergedArg) // a wins

        assertFalse(capturedArgs!!.containsKey("b"))
    }

    // -- Child annotation and nested merge --

    @Test
    fun `withRouteIsTarget passes when route matches`() {
        val child = strArg("child") { onExecute { } }
        val a = strArg("a") { isTarget { true } }
        val b = strArg("b")
        val info = Compiler.buildMergedArgument(listOf(a, b))

        val annotated = child.withRouteIsTarget(info.routeKey, 0) // belongs to alt a (index 0)
        assertTrue(annotated.invokeIsTarget(
            Context(sender, mapOf(info.routeKey to 0)),
            "any"
        ))
    }

    @Test
    fun `withRouteIsTarget fails when route does not match`() {
        val child = strArg("child") { onExecute { } }
        val a = strArg("a") { isTarget { true } }
        val b = strArg("b")
        val info = Compiler.buildMergedArgument(listOf(a, b))

        val annotated = child.withRouteIsTarget(info.routeKey, 0) // belongs to alt a (index 0)
        assertFalse(annotated.invokeIsTarget(
            Context(sender, mapOf(info.routeKey to 1)), // b won, not a
            "any"
        ))
    }

    @Test
    fun `nested merge routes children to correct branch`() {
        val firedChild = mutableListOf<String>()

        val childA = strArg("child_a") { onExecute { firedChild.add("A") } }
        val childB = strArg("child_b") { onExecute { firedChild.add("B") } }

        val a = strArg("a") { isTarget { it.startsWith("@") } }
        val b = strArg("b")

        val parentInfo = Compiler.buildMergedArgument(listOf(a, b))

        // Annotate children as compileMergedNode would
        val annotatedChildA = childA.withRouteIsTarget(parentInfo.routeKey, 0)
        val annotatedChildB = childB.withRouteIsTarget(parentInfo.routeKey, 1)

        // Merge children (same type — both StringArgumentType)
        val childInfo = Compiler.buildMergedArgument(listOf(annotatedChildA, annotatedChildB))

        // "@alice" → a wins (route 0) → annotatedChildA's isTarget passes in child merge
        execute(
            mapOf(parentInfo.syntheticKey to "@alice", childInfo.syntheticKey to "reason"),
            parentInfo.mergedArg, childInfo.mergedArg
        )
        assertEquals(listOf("A"), firedChild)

        firedChild.clear()

        // "alice" → b wins (route 1) → annotatedChildB's isTarget passes in child merge
        execute(
            mapOf(parentInfo.syntheticKey to "alice", childInfo.syntheticKey to "reason"),
            parentInfo.mergedArg, childInfo.mergedArg
        )
        assertEquals(listOf("B"), firedChild)
    }

    @Test
    fun `child derivations see parent route key`() {
        var derivedRoute: Any? = null

        val childA = strArg("child_a") {
            derive("saw_route") { args[Compiler.buildMergedArgument(emptyList()).routeKey] } // resolved below
            onExecute { derivedRoute = args["saw_route"] }
        }

        val a = strArg("a") { isTarget { it.startsWith("@") } }
        val b = strArg("b")
        val parentInfo = Compiler.buildMergedArgument(listOf(a, b))

        // Build a child that derives from parent's route key
        val child = ArgumentBuilder("child_a", StringArgumentType) { TransformResult.Success(it) }
            .derive("saw_route") { args[parentInfo.routeKey] }
            .onExecute { derivedRoute = args["saw_route"] }
            .build()

        execute(
            mapOf(parentInfo.syntheticKey to "@alice", "child_a" to "x"),
            parentInfo.mergedArg, child
        )

        assertEquals(0, derivedRoute) // a won (index 0)
    }
}
