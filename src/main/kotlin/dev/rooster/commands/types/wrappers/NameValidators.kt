package dev.rooster.commands.types.wrappers

import dev.rooster.commands.*

fun <Self> Self.notBlank(
    error: Context.() -> Unit = { sender.sendMessage("Value must not be blank.") },
): Self where Self : CanExtendIsValid<String, String> =
    isValid { raw, _ -> if (raw.isBlank()) IsValidResult.Invalid(error) else IsValidResult.Valid }

fun <Self> Self.noWhitespace(
    error: Context.() -> Unit = { sender.sendMessage("Value must not contain spaces.") },
): Self where Self : CanExtendIsValid<String, String> =
    isValid { raw, _ -> if (raw.any { it.isWhitespace() }) IsValidResult.Invalid(error) else IsValidResult.Valid }

fun <Self> Self.minLength(
    n: Int,
    error: Context.(String) -> Unit = { sender.sendMessage("Must be at least $n characters.") },
): Self where Self : CanExtendIsValid<String, String> =
    isValid { raw, _ -> if (raw.length < n) IsValidResult.Invalid { error.invoke(this, raw) } else IsValidResult.Valid }

fun <Self> Self.maxLength(
    n: Int,
    error: Context.(String) -> Unit = { sender.sendMessage("Must be at most $n characters.") },
): Self where Self : CanExtendIsValid<String, String> =
    isValid { raw, _ -> if (raw.length > n) IsValidResult.Invalid { error.invoke(this, raw) } else IsValidResult.Valid }

fun <Self> Self.matches(
    regex: Regex,
    error: Context.(String) -> Unit = { sender.sendMessage("'$it' is not in the correct format.") },
): Self where Self : CanExtendIsValid<String, String> =
    isValid { raw, _ -> if (!regex.matches(raw)) IsValidResult.Invalid { error.invoke(this, raw) } else IsValidResult.Valid }

fun <Self> Self.uniqueIn(
    existing: List<String>,
    ignoreCase: Boolean = false,
    error: Context.(String) -> Unit = { sender.sendMessage("'$it' is already taken.") },
): Self where Self : CanExtendIsValid<String, String> =
    isValid { raw, _ ->
        if (existing.any { it.equals(raw, ignoreCase) })
            IsValidResult.Invalid { error.invoke(this, raw) }
        else
            IsValidResult.Valid
    }

@JvmName("uniqueInDynamic")
fun <Self> Self.uniqueIn(
    existing: Context.() -> List<String>,
    ignoreCase: Boolean = false,
    error: Context.(String) -> Unit = { sender.sendMessage("'$it' is already taken.") },
): Self where Self : CanExtendIsValid<String, String> =
    isValid { raw, _ ->
        if (existing.invoke(this).any { it.equals(raw, ignoreCase) })
            IsValidResult.Invalid { error.invoke(this, raw) }
        else
            IsValidResult.Valid
    }
