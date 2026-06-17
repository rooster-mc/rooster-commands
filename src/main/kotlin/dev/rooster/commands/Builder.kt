package dev.rooster.commands

@DslMarker
annotation class CommandDsl

class ArgumentPart<TailT, TailK>(
    val head: ArgumentBuilder<*, *>,
    val tail: ArgumentBuilder<TailT, TailK>,
) : CanOnExecute, CanOnMissing {
    override var executorCallback: (Context.() -> Unit)?
        get() = tail.executorCallback
        set(value) { tail.executorCallback = value }

    override var onMissingCallback: (Context.() -> Unit)?
        get() = tail.onMissingCallback
        set(value) { tail.onMissingCallback = value }
}

@CommandDsl
class ChildrenScope {
    internal val builders = mutableListOf<Buildable>()
    internal val preBuilt = mutableListOf<Argument<*, *>>()

    fun register(builder: Buildable) { builders.add(builder) }
    fun register(arg: Argument<*, *>) { preBuilt.add(arg) }

    operator fun Buildable.unaryPlus() = this@ChildrenScope.register(this)
    operator fun Argument<*, *>.unaryPlus() = this@ChildrenScope.register(this)

    fun buildChildren(): List<Argument<*, *>> =
        preBuilt + builders.map { it.build() }
}

@CommandDsl
class ArgumentBuilder<T, K>(
    internal val key: String,
    internal val type: ArgumentType<K>,
    internal val defaultTransform: Context.(K) -> TransformResult<T>,
) : Buildable,
    AttachedNode,
    CanSuggest,
    CanOnExecute,
    CanOnMissing,
    CanOnMissingChild,
    CanExtendIsValid<T, K>,
    CanIsTarget<K>,
    CanIsSyntaxValid<K>,
    CanDerive,
    CanOptional,
    CanThen {

    override var suggestCallback: (Context.() -> List<Suggestion>)? = null
    override var executorCallback: (Context.() -> Unit)? = null
    override var onMissingCallback: (Context.() -> Unit)? = null
    override var onMissingChildCallback: (Context.() -> Unit)? = null
    override var syntaxValidCallback: (SyntaxContext<K>.() -> SyntaxResult)? = null
    override val baseValidCallback: Context.(K, TransformResult<T>) -> IsValidResult = { _, _ -> IsValidResult.Valid }
    override val validExtensions = mutableListOf<Context.(K, TransformResult<T>) -> IsValidResult>()
    override var isTargetCallback: (Context.(K) -> Boolean)? = null
    override var isOptional: Boolean = false
    override val derivations = mutableMapOf<String, Context.() -> Any?>()
    override val pendingChildren = mutableListOf<Buildable>()
    override val builtChildren = mutableListOf<Argument<*, *>>()

    override fun build(): Argument<T, K> = Argument(
        key = key,
        type = type,
        transformValue = defaultTransform,
        suggestions = suggestCallback,
        children = builtChildren + pendingChildren.map { it.build() },
        executor = executorCallback,
        onMissing = onMissingCallback,
        onMissingChild = onMissingChildCallback,
        isSyntaxValid = syntaxValidCallback,
        isValid = if (validExtensions.isEmpty()) null
                  else buildExtendedIsValid(baseValidCallback, validExtensions, null),
        isTarget = isTargetCallback,
        isOptional = isOptional,
        derivations = derivations.toMap(),
    )
}
