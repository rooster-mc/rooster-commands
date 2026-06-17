package dev.rooster.commands.types.wrappers

import dev.rooster.commands.*

// ── Range (any Comparable) ────────────────────────────────────────────────────

fun <Self, K : Comparable<K>> Self.atLeast(
    min: K,
    error: Context.(K) -> Unit = { sender.sendMessage("Must be at least $min.") },
): Self where Self : CanExtendIsValid<K, K> =
    isValid { raw, _ -> if (raw < min) IsValidResult.Invalid { error.invoke(this, raw) } else IsValidResult.Valid }

fun <Self, K : Comparable<K>> Self.atMost(
    max: K,
    error: Context.(K) -> Unit = { sender.sendMessage("Must be at most $max.") },
): Self where Self : CanExtendIsValid<K, K> =
    isValid { raw, _ -> if (raw > max) IsValidResult.Invalid { error.invoke(this, raw) } else IsValidResult.Valid }

fun <Self, K : Comparable<K>> Self.inRange(
    range: ClosedRange<K>,
    error: Context.(K) -> Unit = { sender.sendMessage("Must be between ${range.start} and ${range.endInclusive}.") },
): Self where Self : CanExtendIsValid<K, K> =
    isValid { raw, _ -> if (raw !in range) IsValidResult.Invalid { error.invoke(this, raw) } else IsValidResult.Valid }

// ── Sign / zero (any Number) ─────────────────────────────────────────────────

fun <Self, K> Self.positive(
    error: Context.() -> Unit = { sender.sendMessage("Must be positive.") },
): Self where Self : CanExtendIsValid<K, K>, K : Number, K : Comparable<K> =
    isValid { raw, _ -> if (raw.toDouble() <= 0) IsValidResult.Invalid(error) else IsValidResult.Valid }

fun <Self, K> Self.nonNegative(
    error: Context.() -> Unit = { sender.sendMessage("Must not be negative.") },
): Self where Self : CanExtendIsValid<K, K>, K : Number, K : Comparable<K> =
    isValid { raw, _ -> if (raw.toDouble() < 0) IsValidResult.Invalid(error) else IsValidResult.Valid }

fun <Self, K> Self.nonZero(
    error: Context.() -> Unit = { sender.sendMessage("Must not be zero.") },
): Self where Self : CanExtendIsValid<K, K>, K : Number, K : Comparable<K> =
    isValid { raw, _ -> if (raw.toDouble() == 0.0) IsValidResult.Invalid(error) else IsValidResult.Valid }

fun <Self, K> Self.multipleOf(
    n: K,
    error: Context.(K) -> Unit = { sender.sendMessage("Must be a multiple of $n.") },
): Self where Self : CanExtendIsValid<K, K>, K : Number, K : Comparable<K> = isValid { raw, _ ->
    val d = n.toDouble()
    if (d == 0.0 || raw.toDouble() % d != 0.0)
        IsValidResult.Invalid { error.invoke(this, raw) }
    else
        IsValidResult.Valid
}
