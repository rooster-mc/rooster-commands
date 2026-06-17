package dev.rooster.commands

sealed interface AttachedNode

interface CanSuggest {
    var suggestCallback: (Context.() -> List<Suggestion>)?
}

interface CanOnExecute {
    var executorCallback: (Context.() -> Unit)?
}

interface CanOnMissing {
    var onMissingCallback: (Context.() -> Unit)?
}

interface CanOnMissingChild {
    var onMissingChildCallback: (Context.() -> Unit)?
}

interface HasIsValid<T, K>

interface CanIsValid<T, K> : HasIsValid<T, K> {
    var validCallback: (Context.(K, TransformResult<T>) -> IsValidResult)?
}

interface CanExtendIsValid<T, K> : HasIsValid<T, K> {
    val baseValidCallback: Context.(K, TransformResult<T>) -> IsValidResult
    val validExtensions: MutableList<Context.(K, TransformResult<T>) -> IsValidResult>
}

interface CanOverrideIsValid<T, K> : HasIsValid<T, K> {
    var validOverride: (Context.(K, TransformResult<T>) -> IsValidResult)?
}

interface CanIsTarget<K> {
    var isTargetCallback: (Context.(K) -> Boolean)?
}

interface CanIsSyntaxValid<K> {
    var syntaxValidCallback: (SyntaxContext<K>.() -> SyntaxResult)?
}

interface CanDerive {
    val derivations: MutableMap<String, Context.() -> Any?>
}

interface CanOptional {
    var isOptional: Boolean
}

interface CanThen {
    val pendingChildren: MutableList<ArgumentBuilder<*, *>>
    val builtChildren: MutableList<Argument<*, *>>
}

fun <T : CanSuggest> T.suggest(block: Context.() -> List<Suggestion>): T = apply {
    suggestCallback = block
}

@JvmName("suggestStrings")
fun <T : CanSuggest> T.suggest(block: Context.() -> List<String>): T = apply {
    suggestCallback = { block().map { Suggestion(it) } }
}

fun <T : CanOnExecute> T.onExecute(block: Context.() -> Unit): T = apply {
    executorCallback = block
}

fun <T : CanOnMissing> T.onMissing(block: Context.() -> Unit): T = apply {
    onMissingCallback = block
}

fun <T : CanOnMissingChild> T.onMissingChild(block: Context.() -> Unit): T = apply {
    onMissingChildCallback = block
}

fun <Self, T, K> Self.isValid(block: Context.(K, TransformResult<T>) -> IsValidResult): Self
    where Self : CanIsValid<T, K> = apply { validCallback = block }

fun <Self, T, K> Self.isValid(block: Context.(K, TransformResult<T>) -> IsValidResult): Self
    where Self : CanExtendIsValid<T, K> = apply { validExtensions.add(block) }

fun <Self, T, K> Self.overrideIsValid(block: Context.(K, TransformResult<T>) -> IsValidResult): Self
    where Self : CanOverrideIsValid<T, K> = apply { validOverride = block }

fun <T, K> buildExtendedIsValid(
    base: Context.(K, TransformResult<T>) -> IsValidResult,
    extensions: List<Context.(K, TransformResult<T>) -> IsValidResult>,
    override: (Context.(K, TransformResult<T>) -> IsValidResult)?,
): (Context.(K, TransformResult<T>) -> IsValidResult) = override ?: { k, t ->
    val ctx = this
    val baseResult = ctx.base(k, t)
    if (baseResult !is IsValidResult.Valid) baseResult
    else extensions.fold<_, IsValidResult>(IsValidResult.Valid) { acc, ext ->
        if (acc !is IsValidResult.Valid) acc else ctx.ext(k, t)
    }
}

fun <Self, K> Self.isTarget(block: Context.(K) -> Boolean): Self
    where Self : CanIsTarget<K> = apply { isTargetCallback = block }

fun <Self, K> Self.isSyntaxValid(block: SyntaxContext<K>.() -> SyntaxResult): Self
    where Self : CanIsSyntaxValid<K> = apply { syntaxValidCallback = block }

fun <T : CanDerive> T.derive(key: String, block: Context.() -> Any?): T = apply {
    derivations[key] = block
}

fun <T : CanOptional> T.optional(): T = apply {
    isOptional = true
}

fun <T : CanThen> T.then(block: ChildrenScope.() -> Unit): T = apply {
    val scope = ChildrenScope()
    scope.block()
    pendingChildren.addAll(scope.builders)
    builtChildren.addAll(scope.preBuilt)
}

fun <T : CanThen> T.then(child: ArgumentBuilder<*, *>): T = apply {
    pendingChildren.add(child)
}

fun <T : CanThen> T.then(child: Argument<*, *>): T = apply {
    builtChildren.add(child)
}

fun <T : CanThen, TailT, TailK> T.then(part: ArgumentPart<TailT, TailK>): AttachedNode {
    pendingChildren.add(part.head)
    return part.tail
}
