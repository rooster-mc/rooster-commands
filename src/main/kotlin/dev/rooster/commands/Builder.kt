package dev.rooster.commands

@DslMarker
annotation class CommandDsl

class ArgumentPart<TailT, TailK>(
    val head: ArgumentBuilder<*, *>,
    val tail: ArgumentBuilder<TailT, TailK>,
)

@CommandDsl
class ChildrenScope {
    internal val builders = mutableListOf<ArgumentBuilder<*, *>>()
    internal val preBuilt = mutableListOf<Argument<*, *>>()

    fun register(builder: ArgumentBuilder<*, *>) { builders.add(builder) }
    fun register(arg: Argument<*, *>) { preBuilt.add(arg) }

    operator fun ArgumentBuilder<*, *>.unaryPlus() = this@ChildrenScope.register(this)
    operator fun Argument<*, *>.unaryPlus() = this@ChildrenScope.register(this)

    fun buildChildren(): List<Argument<*, *>> =
        preBuilt + builders.map { it.build() }
}

@CommandDsl
class ArgumentBuilder<T, K>(
    internal val key: String,
    internal val type: ArgumentType<K>,
    internal val defaultTransform: Context.(K) -> TransformResult<T>,
) {
    private var suggestionsProvider: (Context.() -> List<Suggestion>)? = null
    private var executorCallback: (Context.() -> Unit)? = null
    private var onMissingCallback: (Context.() -> Unit)? = null
    private var onMissingChildCallback: (Context.() -> Unit)? = null
    private var syntaxValidCallback: (SyntaxContext<K>.() -> SyntaxResult)? = null
    private var validCallback: (Context.(K, TransformResult<T>) -> IsValidResult)? = null
    private var isOptional: Boolean = false
    private val derivations = mutableMapOf<String, Context.() -> Any?>()
    private val pendingChildren = mutableListOf<ArgumentBuilder<*, *>>()
    private val builtChildren = mutableListOf<Argument<*, *>>()

    fun suggest(block: Context.() -> List<Suggestion>) = apply { suggestionsProvider = block }

    @JvmName("suggestStrings")
    fun suggest(block: Context.() -> List<String>) = apply {
        suggestionsProvider = { block().map { Suggestion(it) } }
    }
    fun onExecute(block: Context.() -> Unit) = apply { executorCallback = block }
    fun onMissing(block: Context.() -> Unit) = apply { onMissingCallback = block }
    fun onMissingChild(block: Context.() -> Unit) = apply { onMissingChildCallback = block }
    fun isValid(block: Context.(K, TransformResult<T>) -> IsValidResult) = apply { validCallback = block }
    fun isSyntaxValid(block: SyntaxContext<K>.() -> SyntaxResult) = apply { syntaxValidCallback = block }
    fun optional() = apply { isOptional = true }
    fun derive(key: String, block: Context.() -> Any?) = apply { derivations[key] = block }

    fun then(block: ChildrenScope.() -> Unit) = apply {
        val scope = ChildrenScope()
        scope.block()
        pendingChildren.addAll(scope.builders)
        builtChildren.addAll(scope.preBuilt)
    }

    fun then(child: ArgumentBuilder<*, *>) = apply { pendingChildren.add(child) }
    fun then(child: Argument<*, *>) = apply { builtChildren.add(child) }

    fun <TailT, TailK> then(part: ArgumentPart<TailT, TailK>): ArgumentBuilder<TailT, TailK> {
        pendingChildren.add(part.head)
        return part.tail
    }

    fun build(): Argument<T, K> = Argument(
        key = key,
        type = type,
        transformValue = defaultTransform,
        suggestions = suggestionsProvider,
        children = builtChildren + pendingChildren.map { it.build() },
        executor = executorCallback,
        onMissing = onMissingCallback,
        onMissingChild = onMissingChildCallback,
        isSyntaxValid = syntaxValidCallback,
        isValid = validCallback,
        isOptional = isOptional,
        derivations = derivations.toMap(),
    )
}
