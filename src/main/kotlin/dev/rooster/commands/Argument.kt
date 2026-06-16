package dev.rooster.commands

import org.bukkit.command.CommandSender

class Context(
    val sender: CommandSender,
    val args: Map<String, Any>
)

class SyntaxContext<out K>(
    val sender: CommandSender,
    val raw: String,
    val value: K
)

sealed class TransformResult<out T> {
    data class Success<out T>(val value: T) : TransformResult<T>()
    data object Failure : TransformResult<Nothing>()
}

sealed class SyntaxResult {
    data object Valid : SyntaxResult()
    data class Invalid(val message: String? = null) : SyntaxResult()
}

sealed class IsValidResult {
    data object Valid : IsValidResult()
    data class Invalid(val error: Context.() -> Unit) : IsValidResult()
}

data class Suggestion(val value: String, val tooltip: String? = null)

interface ArgumentType<out K>

class Argument<T, K>(
    val key: String,
    val type: ArgumentType<K>,
    val transformValue: Context.(K) -> TransformResult<T>,
    val suggestions: (Context.() -> List<Suggestion>)? = null,
    val children: List<Argument<*, *>> = emptyList(),
    val executor: (Context.() -> Unit)? = null,
    val onMissing: (Context.() -> Unit)? = null,
    val onMissingChild: (Context.() -> Unit)? = null,
    val isSyntaxValid: (SyntaxContext<K>.() -> SyntaxResult)? = null,
    val isValid: (Context.(K, TransformResult<T>) -> IsValidResult)? = null,
    val isOptional: Boolean = false,
    val derivations: Map<String, Context.() -> Any?> = emptyMap(),
) {
    @Suppress("UNCHECKED_CAST")
    fun invokeTransform(context: Context, rawValue: Any?): TransformResult<T> =
        context.transformValue(rawValue as K)

    @Suppress("UNCHECKED_CAST")
    fun invokeIsValid(context: Context, rawValue: Any?, result: TransformResult<*>): IsValidResult =
        isValid?.invoke(context, rawValue as K, result as TransformResult<T>) ?: when (result) {
            is TransformResult.Success -> IsValidResult.Valid
            TransformResult.Failure -> IsValidResult.Invalid {}
        }

    @Suppress("UNCHECKED_CAST")
    fun invokeSyntaxValid(sender: CommandSender, raw: String, typedValue: Any?): SyntaxResult =
        isSyntaxValid?.invoke(SyntaxContext(sender, raw, typedValue as K)) ?: SyntaxResult.Valid
}
