package dev.rooster.commands.commandapi

import dev.rooster.commands.ArgumentBuilder
import dev.rooster.commands.ChildrenScope
import dev.rooster.commands.TransformResult
import dev.jorel.commandapi.CommandTree
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

// ── Root ────────────────────────────────────────────────────────────────────

fun command(label: String, block: ChildrenScope.() -> Unit): CommandTree {
    val scope = ChildrenScope()
    scope.block()
    return Compiler.compile(label, *scope.buildChildren().toTypedArray())
}

// ── Primitives ───────────────────────────────────────────────────────────────

fun ChildrenScope.integer(key: String, min: Int? = null, max: Int? = null): ArgumentBuilder<Int, Int> =
    ArgumentBuilder(key, IntegerArgumentType(min, max)) { TransformResult.Success(it) }.also { register(it) }

fun ChildrenScope.integer(key: String, min: Int? = null, max: Int? = null, block: ChildrenScope.() -> Unit): ArgumentBuilder<Int, Int> =
    ArgumentBuilder(key, IntegerArgumentType(min, max)) { TransformResult.Success(it) }.then(block).also { register(it) }

fun ChildrenScope.double(key: String, min: Double? = null, max: Double? = null): ArgumentBuilder<Double, Double> =
    ArgumentBuilder(key, DoubleArgumentType(min, max)) { TransformResult.Success(it) }.also { register(it) }

fun ChildrenScope.double(key: String, min: Double? = null, max: Double? = null, block: ChildrenScope.() -> Unit): ArgumentBuilder<Double, Double> =
    ArgumentBuilder(key, DoubleArgumentType(min, max)) { TransformResult.Success(it) }.then(block).also { register(it) }

fun ChildrenScope.float(key: String, min: Float? = null, max: Float? = null): ArgumentBuilder<Float, Float> =
    ArgumentBuilder(key, FloatArgumentType(min, max)) { TransformResult.Success(it) }.also { register(it) }

fun ChildrenScope.float(key: String, min: Float? = null, max: Float? = null, block: ChildrenScope.() -> Unit): ArgumentBuilder<Float, Float> =
    ArgumentBuilder(key, FloatArgumentType(min, max)) { TransformResult.Success(it) }.then(block).also { register(it) }

fun ChildrenScope.long(key: String, min: Long? = null, max: Long? = null): ArgumentBuilder<Long, Long> =
    ArgumentBuilder(key, LongArgumentType(min, max)) { TransformResult.Success(it) }.also { register(it) }

fun ChildrenScope.long(key: String, min: Long? = null, max: Long? = null, block: ChildrenScope.() -> Unit): ArgumentBuilder<Long, Long> =
    ArgumentBuilder(key, LongArgumentType(min, max)) { TransformResult.Success(it) }.then(block).also { register(it) }

fun ChildrenScope.boolean(key: String): ArgumentBuilder<Boolean, Boolean> =
    ArgumentBuilder(key, BooleanArgumentType) { TransformResult.Success(it) }.also { register(it) }

fun ChildrenScope.boolean(key: String, block: ChildrenScope.() -> Unit): ArgumentBuilder<Boolean, Boolean> =
    ArgumentBuilder(key, BooleanArgumentType) { TransformResult.Success(it) }.then(block).also { register(it) }

// ── Strings ──────────────────────────────────────────────────────────────────

fun ChildrenScope.string(key: String): ArgumentBuilder<String, String> =
    ArgumentBuilder(key, StringArgumentType) { TransformResult.Success(it) }.also { register(it) }

fun ChildrenScope.string(key: String, block: ChildrenScope.() -> Unit): ArgumentBuilder<String, String> =
    ArgumentBuilder(key, StringArgumentType) { TransformResult.Success(it) }.then(block).also { register(it) }

fun ChildrenScope.greedyString(key: String): ArgumentBuilder<String, String> =
    ArgumentBuilder(key, GreedyStringArgumentType) { TransformResult.Success(it) }.also { register(it) }

fun ChildrenScope.greedyString(key: String, block: ChildrenScope.() -> Unit): ArgumentBuilder<String, String> =
    ArgumentBuilder(key, GreedyStringArgumentType) { TransformResult.Success(it) }.then(block).also { register(it) }

// ── Literals ─────────────────────────────────────────────────────────────────

fun ChildrenScope.literal(name: String): ArgumentBuilder<String, String> =
    ArgumentBuilder(name, LiteralArgumentType(name)) { TransformResult.Success(it) }.also { register(it) }

fun ChildrenScope.literal(name: String, block: ChildrenScope.() -> Unit): ArgumentBuilder<String, String> =
    ArgumentBuilder(name, LiteralArgumentType(name)) { TransformResult.Success(it) }.then(block).also { register(it) }

// ── Bukkit types ─────────────────────────────────────────────────────────────

fun ChildrenScope.player(key: String): ArgumentBuilder<Player, Player> =
    ArgumentBuilder(key, PlayerArgumentType) { TransformResult.Success(it) }.also { register(it) }

fun ChildrenScope.player(key: String, block: ChildrenScope.() -> Unit): ArgumentBuilder<Player, Player> =
    ArgumentBuilder(key, PlayerArgumentType) { TransformResult.Success(it) }.then(block).also { register(it) }

fun ChildrenScope.world(key: String): ArgumentBuilder<World, World> =
    ArgumentBuilder(key, WorldArgumentType) { TransformResult.Success(it) }.also { register(it) }

fun ChildrenScope.world(key: String, block: ChildrenScope.() -> Unit): ArgumentBuilder<World, World> =
    ArgumentBuilder(key, WorldArgumentType) { TransformResult.Success(it) }.then(block).also { register(it) }

fun ChildrenScope.itemStack(key: String): ArgumentBuilder<ItemStack, ItemStack> =
    ArgumentBuilder(key, ItemStackArgumentType) { TransformResult.Success(it) }.also { register(it) }

fun ChildrenScope.itemStack(key: String, block: ChildrenScope.() -> Unit): ArgumentBuilder<ItemStack, ItemStack> =
    ArgumentBuilder(key, ItemStackArgumentType) { TransformResult.Success(it) }.then(block).also { register(it) }

fun ChildrenScope.location(key: String): ArgumentBuilder<Location, Location> =
    ArgumentBuilder(key, LocationArgumentType) { TransformResult.Success(it) }.also { register(it) }

fun ChildrenScope.location(key: String, block: ChildrenScope.() -> Unit): ArgumentBuilder<Location, Location> =
    ArgumentBuilder(key, LocationArgumentType) { TransformResult.Success(it) }.then(block).also { register(it) }
