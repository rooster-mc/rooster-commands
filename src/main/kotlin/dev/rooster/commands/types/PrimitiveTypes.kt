// Non Bukkit, non wrapper (numbers, strings, literal)
package dev.rooster.commands.types

import dev.rooster.commands.*

// ── Numeric types ─────────────────────────────────────────────────────────────

data class IntegerArgumentType(val min: Int? = null, val max: Int? = null) : ArgumentType<Int>
data class DoubleArgumentType(val min: Double? = null, val max: Double? = null) : ArgumentType<Double>
data class FloatArgumentType(val min: Float? = null, val max: Float? = null) : ArgumentType<Float>
data class LongArgumentType(val min: Long? = null, val max: Long? = null) : ArgumentType<Long>

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

// ── Boolean ───────────────────────────────────────────────────────────────────

data object BooleanArgumentType : ArgumentType<Boolean>

fun ChildrenScope.boolean(key: String): ArgumentBuilder<Boolean, Boolean> =
    ArgumentBuilder(key, BooleanArgumentType) { TransformResult.Success(it) }.also { register(it) }

fun ChildrenScope.boolean(key: String, block: ChildrenScope.() -> Unit): ArgumentBuilder<Boolean, Boolean> =
    ArgumentBuilder(key, BooleanArgumentType) { TransformResult.Success(it) }.then(block).also { register(it) }

// ── Strings ───────────────────────────────────────────────────────────────────

data object StringArgumentType : ArgumentType<String>
data object GreedyStringArgumentType : ArgumentType<String>

fun ChildrenScope.string(key: String): ArgumentBuilder<String, String> =
    ArgumentBuilder(key, StringArgumentType) { TransformResult.Success(it) }.also { register(it) }

fun ChildrenScope.string(key: String, block: ChildrenScope.() -> Unit): ArgumentBuilder<String, String> =
    ArgumentBuilder(key, StringArgumentType) { TransformResult.Success(it) }.then(block).also { register(it) }

fun ChildrenScope.greedyString(key: String): ArgumentBuilder<String, String> =
    ArgumentBuilder(key, GreedyStringArgumentType) { TransformResult.Success(it) }.also { register(it) }

fun ChildrenScope.greedyString(key: String, block: ChildrenScope.() -> Unit): ArgumentBuilder<String, String> =
    ArgumentBuilder(key, GreedyStringArgumentType) { TransformResult.Success(it) }.then(block).also { register(it) }

// ── Literal ───────────────────────────────────────────────────────────────────

data class LiteralArgumentType(val name: String) : ArgumentType<String>

fun ChildrenScope.literal(name: String): ArgumentBuilder<String, String> =
    ArgumentBuilder(name, LiteralArgumentType(name)) { TransformResult.Success(it) }.also { register(it) }

fun ChildrenScope.literal(name: String, block: ChildrenScope.() -> Unit): ArgumentBuilder<String, String> =
    ArgumentBuilder(name, LiteralArgumentType(name)) { TransformResult.Success(it) }.then(block).also { register(it) }
