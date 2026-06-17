package dev.rooster.commands.commandapi

import dev.rooster.commands.Argument
import dev.rooster.commands.ArgumentType
import dev.rooster.commands.Context
import dev.rooster.commands.IsValidResult
import dev.rooster.commands.SyntaxContext
import dev.rooster.commands.SyntaxResult
import dev.rooster.commands.TransformResult
import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.arguments.Argument as CmdArg
import dev.jorel.commandapi.StringTooltip
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.arguments.CustomArgument
import dev.jorel.commandapi.arguments.CustomArgument.CustomArgumentException
import dev.jorel.commandapi.executors.CommandArguments
import dev.jorel.commandapi.executors.CommandExecutor
import org.bukkit.command.CommandSender

internal data class MergedArgumentInfo(
    val syntheticKey: String,
    val routeKey: String,
    val ordered: List<Argument<*, *>>,
    val mergedArg: Argument<Any?, Any?>,
)

@Suppress("UNCHECKED_CAST")
internal fun Argument<*, *>.withRouteIsTarget(routeKey: String, routeIndex: Int): Argument<*, *> {
    val existing = isTarget as (Context.(Any?) -> Boolean)?
    val annotated: Context.(Any?) -> Boolean = { rawValue ->
        (args[routeKey] as? Int) == routeIndex &&
            (existing == null || existing.invoke(this, rawValue))
    }
    return Argument(
        key = key,
        type = type as ArgumentType<Any?>,
        transformValue = transformValue as Context.(Any?) -> TransformResult<Any?>,
        suggestions = suggestions,
        children = children,
        executor = executor,
        onMissing = onMissing,
        onMissingChild = onMissingChild,
        isSyntaxValid = isSyntaxValid as (SyntaxContext<Any?>.() -> SyntaxResult)?,
        isValid = isValid as (Context.(Any?, TransformResult<Any?>) -> IsValidResult)?,
        isOptional = isOptional,
        derivations = derivations,
        isTarget = annotated,
    )
}

object Compiler {

    fun compile(label: String, vararg arguments: Argument<*, *>): CommandTree {
        return CommandTree(label).also { tree ->
            compileChildren(arguments.toList(), emptyList()).forEach { tree.then(it) }
        }
    }

    @Suppress("UNCHECKED_CAST")
    internal fun buildMergedArgument(alternatives: List<Argument<*, *>>): MergedArgumentInfo {
        val wildcards = alternatives.filter { it.isTarget == null }
        require(wildcards.size <= 1) {
            "Ambiguous merge: multiple arguments without isTarget at the same level: " +
                "${alternatives.map { it.key }}. Add isTarget to all but one."
        }

        val ordered = alternatives.filter { it.isTarget != null } + wildcards
        val syntheticKey = ordered.joinToString("|") { it.key }
        val routeKey = "_route_$syntheticKey"

        val derivations: Map<String, Context.() -> Any?> = buildMap {
            put(routeKey) {
                val rawValue = args[syntheticKey]
                ordered.indexOfFirst { alt -> alt.invokeIsTarget(this, rawValue) }.takeIf { it >= 0 }
            }
            ordered.forEachIndexed { index, alt ->
                put(alt.key) altKey@{
                    if ((args[routeKey] as? Int) != index) return@altKey null
                    val rawValue = args[syntheticKey]
                    (alt.invokeTransform(this, rawValue) as? TransformResult.Success<*>)?.value
                }
            }
        }

        val mergedExecutor: (Context.() -> Unit)? = if (ordered.any { it.executor != null }) {
            executor@{
                val routeIndex = args[routeKey] as? Int ?: return@executor
                ordered.getOrNull(routeIndex)?.executor?.invoke(this)
            }
        } else null

        val mergedArg = Argument<Any?, Any?>(
            key = syntheticKey,
            type = ordered[0].type as ArgumentType<Any?>,
            transformValue = { TransformResult.Success(it) },
            isValid = isValid@{ k, _ ->
                val winner = ordered.firstOrNull { alt -> alt.invokeIsTarget(this, k) }
                    ?: return@isValid IsValidResult.Invalid {}
                winner.invokeIsValid(this, k, winner.invokeTransform(this, k))
            },
            derivations = derivations,
            executor = mergedExecutor,
        )

        return MergedArgumentInfo(syntheticKey, routeKey, ordered, mergedArg)
    }

    // Test entry points — avoid needing CommandArguments in tests
    internal fun executeRaw(sender: CommandSender, rawArgs: Map<String, Any?>, path: List<Argument<*, *>>) =
        executePathCore(sender, rawArgs::get, path)

    internal fun buildContextRaw(sender: CommandSender, rawArgs: Map<String, Any?>, path: List<Argument<*, *>>) =
        buildContextCore(sender, rawArgs::get, path)

    @Suppress("UNCHECKED_CAST")
    private fun compileNode(
        argument: Argument<*, *>,
        ancestorPath: List<Argument<*, *>>,
    ): CmdArg<*> {
        val fullPath = ancestorPath + argument
        val baseArg = argument.type.toCommandApiArg(argument.key)

        val cmdArg = if (argument.type is LiteralArgumentType) {
            baseArg
        } else {
            CustomArgument(baseArg as CmdArg<Any?>) { info ->
                val result = argument.invokeSyntaxValid(info.sender(), info.input(), info.currentInput())
                if (result is SyntaxResult.Invalid) {
                    throw CustomArgumentException.fromString(result.message ?: "Invalid argument")
                }
                info.currentInput()
            }
        }

        argument.suggestions?.let { provider ->
            cmdArg.replaceSuggestions(ArgumentSuggestions.stringsWithTooltips { info ->
                val ctx = Context(info.sender(), buildContextCore(info.sender(), { info.previousArgs()[it] }, ancestorPath))
                provider(ctx)
                    .map { s -> StringTooltip.ofString(s.value, s.tooltip) }
                    .toTypedArray()
            })
        }

        if (argument.executor != null) {
            cmdArg.executes(CommandExecutor { sender, args ->
                executePathCore(sender, { args[it] }, fullPath)
            })
        }

        compileChildren(argument.children, fullPath).forEach { child ->
            cmdArg.then(child)
        }

        return cmdArg
    }

    private fun compileChildren(
        children: List<Argument<*, *>>,
        ancestorPath: List<Argument<*, *>>,
    ): List<CmdArg<*>> {
        val groups = children.groupBy { it.type::class }
        return groups.values.flatMap { group ->
            if (group.size == 1 || group[0].type is LiteralArgumentType) {
                group.map { compileNode(it, ancestorPath) }
            } else {
                listOf(compileMergedNode(group, ancestorPath))
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun compileMergedNode(
        alternatives: List<Argument<*, *>>,
        ancestorPath: List<Argument<*, *>>,
    ): CmdArg<*> {
        val (syntheticKey, routeKey, ordered, mergedArg) = buildMergedArgument(alternatives)
        val fullPath = ancestorPath + mergedArg

        val cmdArg = CustomArgument(ordered[0].type.toCommandApiArg(syntheticKey) as CmdArg<Any?>) { info ->
            val anyValid = ordered.any { alt ->
                alt.invokeSyntaxValid(info.sender(), info.input(), info.currentInput()) is SyntaxResult.Valid
            }
            if (!anyValid) throw CustomArgumentException.fromString("Invalid argument")
            info.currentInput()
        }

        val allSuggestions = ordered.mapNotNull { it.suggestions }
        if (allSuggestions.isNotEmpty()) {
            cmdArg.replaceSuggestions(ArgumentSuggestions.stringsWithTooltips { info ->
                val ctx = Context(info.sender(), buildContextCore(info.sender(), { info.previousArgs()[it] }, ancestorPath))
                ordered
                    .filter { alt ->
                        alt.suggestions != null &&
                            try { alt.invokeIsTarget(ctx, info.currentInput()) } catch (_: Exception) { true }
                    }
                    .flatMap { alt -> alt.suggestions!!(ctx) }
                    .map { s -> StringTooltip.ofString(s.value, s.tooltip) }
                    .toTypedArray()
            })
        }

        if (mergedArg.executor != null) {
            cmdArg.executes(CommandExecutor { sender, args ->
                executePathCore(sender, { args[it] }, fullPath)
            })
        }

        val annotatedChildren = ordered.flatMapIndexed { index, alt ->
            alt.children.map { child -> child.withRouteIsTarget(routeKey, index) }
        }
        compileChildren(annotatedChildren, fullPath).forEach { child ->
            cmdArg.then(child)
        }

        return cmdArg
    }

    private fun buildContextCore(
        sender: CommandSender,
        getArg: (String) -> Any?,
        path: List<Argument<*, *>>,
    ): Map<String, Any> {
        val contextArgs = mutableMapOf<String, Any>()
        for (node in path) {
            val rawValue: Any? = when (val t = node.type) {
                is LiteralArgumentType -> t.name
                else -> getArg(node.key) ?: continue
            }
            val result = node.invokeTransform(Context(sender, contextArgs.toMap()), rawValue)
            if (result is TransformResult.Success<*>) {
                result.value?.let { contextArgs[node.key] = it }
            }
            for ((key, block) in node.derivations) {
                block(Context(sender, contextArgs.toMap()))?.let { contextArgs[key] = it }
            }
        }
        return contextArgs
    }

    private fun executePathCore(
        sender: CommandSender,
        getArg: (String) -> Any?,
        path: List<Argument<*, *>>,
    ) {
        val contextArgs = mutableMapOf<String, Any>()
        for (node in path) {
            val rawValue: Any? = when (val t = node.type) {
                is LiteralArgumentType -> t.name
                else -> getArg(node.key)
            }
            val ctx = Context(sender, contextArgs.toMap())
            val transformResult = node.invokeTransform(ctx, rawValue)
            val validResult = node.invokeIsValid(ctx, rawValue, transformResult)
            if (validResult is IsValidResult.Invalid) {
                validResult.error(Context(sender, contextArgs.toMap()))
                return
            }
            if (transformResult !is TransformResult.Success<*>) {
                error("Argument '${node.key}': isValid returned Valid but transformValue returned Failure")
            }
            transformResult.value?.let { contextArgs[node.key] = it }
            for ((key, block) in node.derivations) {
                block(Context(sender, contextArgs.toMap()))?.let { contextArgs[key] = it }
            }
        }
        path.last().executor!!.invoke(Context(sender, contextArgs))
    }
}
