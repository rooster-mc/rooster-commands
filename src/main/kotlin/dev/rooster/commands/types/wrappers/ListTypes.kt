package dev.rooster.commands.types.wrappers

import dev.rooster.commands.*
import dev.rooster.commands.types.StringArgumentType

class ListBuilder<T>(
    private val inner: ArgumentBuilder<T, String>,
    baseIsValid: Context.(String, TransformResult<T>) -> IsValidResult,
) : Buildable,
    CanExtendIsValid<T, String>,
    CanOverrideIsValid<T, String>,
    CanIsTarget<String> by inner,
    CanOnExecute by inner,
    CanOnMissing by inner,
    CanOnMissingChild by inner,
    CanDerive by inner,
    CanOptional by inner,
    CanThen by inner {

    override val baseValidCallback = baseIsValid
    override val validExtensions = mutableListOf<Context.(String, TransformResult<T>) -> IsValidResult>()
    override var validOverride: (Context.(String, TransformResult<T>) -> IsValidResult)? = null

    override fun build(): Argument<T, String> {
        inner.validCallback = buildExtendedIsValid(baseValidCallback, validExtensions, validOverride)
        return inner.build()
    }
}

private fun <T> makeListBuilder(
    key: String,
    listProvider: Context.() -> List<String>,
    transform: Context.(String) -> TransformResult<T>,
    ignoreCase: Boolean,
    notMatchingError: Context.(String) -> Unit,
): ListBuilder<T> {
    val suggestions: Context.() -> List<Suggestion> = {
        listProvider.invoke(this).map { s -> Suggestion(s) }
    }
    val inner = ArgumentBuilder(key, StringArgumentType, transform).suggest(suggestions)

    val base: Context.(String, TransformResult<T>) -> IsValidResult = { raw, _ ->
        if (listProvider.invoke(this).none { it.equals(raw, ignoreCase) })
            IsValidResult.Invalid { notMatchingError.invoke(this, raw) }
        else
            IsValidResult.Valid
    }

    return ListBuilder(inner, base)
}

fun ChildrenScope.list(
    key: String,
    items: List<String>,
    ignoreCase: Boolean = false,
    notMatchingError: Context.(String) -> Unit = { sender.sendMessage("'$it' is not a valid option.") },
): ListBuilder<String> = makeListBuilder(
    key, { items }, { s -> TransformResult.Success(s) }, ignoreCase, notMatchingError,
).also { register(it) }

@JvmName("listDynamic")
fun ChildrenScope.list(
    key: String,
    items: Context.() -> List<String>,
    ignoreCase: Boolean = false,
    notMatchingError: Context.(String) -> Unit = { sender.sendMessage("'$it' is not a valid option.") },
): ListBuilder<String> = makeListBuilder(
    key, items, { s -> TransformResult.Success(s) }, ignoreCase, notMatchingError,
).also { register(it) }

fun <T> ChildrenScope.list(
    key: String,
    items: Map<String, T>,
    ignoreCase: Boolean = false,
    notMatchingError: Context.(String) -> Unit = { sender.sendMessage("'$it' is not a valid option.") },
): ListBuilder<T> = makeListBuilder(
    key,
    { items.keys.toList() },
    { raw -> items.entries.firstOrNull { e -> e.key.equals(raw, ignoreCase) }
        ?.let { TransformResult.Success(it.value) } ?: TransformResult.Failure },
    ignoreCase,
    notMatchingError,
).also { register(it) }

@JvmName("listDynamicMap")
fun <T> ChildrenScope.list(
    key: String,
    items: Context.() -> Map<String, T>,
    ignoreCase: Boolean = false,
    notMatchingError: Context.(String) -> Unit = { sender.sendMessage("'$it' is not a valid option.") },
): ListBuilder<T> = makeListBuilder(
    key,
    { items.invoke(this).keys.toList() },
    { raw -> items.invoke(this).entries.firstOrNull { e -> e.key.equals(raw, ignoreCase) }
        ?.let { TransformResult.Success(it.value) } ?: TransformResult.Failure },
    ignoreCase,
    notMatchingError,
).also { register(it) }
