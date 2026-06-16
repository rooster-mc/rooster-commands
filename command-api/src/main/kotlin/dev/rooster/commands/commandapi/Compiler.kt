package dev.rooster.commands.commandapi

import dev.rooster.commands.Argument
import dev.rooster.commands.ArgumentType
import dev.rooster.commands.Context
import dev.rooster.commands.IsValidResult
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

object Compiler {

    fun compile(label: String, vararg arguments: Argument<*, *>): CommandTree {
        return CommandTree(label).also { tree ->
            compileChildren(arguments.toList(), emptyList()).forEach { tree.then(it) }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun compileNode(
        argument: Argument<*, *>,
        ancestorPath: List<Argument<*, *>>,
        routeGuard: (Map<String, Any>.() -> Boolean)? = null,
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
                val ctx = Context(info.sender(), buildPartialContext(info.sender(), info.previousArgs(), ancestorPath))
                provider(ctx)
                    .map { s -> StringTooltip.ofString(s.value, s.tooltip) }
                    .toTypedArray()
            })
        }

        if (argument.executor != null) {
            cmdArg.executes(CommandExecutor { sender, args ->
                invokeExecutor(sender, args, fullPath, routeGuard)
            })
        }

        compileChildren(argument.children, fullPath, routeGuard).forEach { child ->
            cmdArg.then(child)
        }

        return cmdArg
    }

    private fun compileChildren(
        children: List<Argument<*, *>>,
        ancestorPath: List<Argument<*, *>>,
        routeGuard: (Map<String, Any>.() -> Boolean)? = null,
    ): List<CmdArg<*>> {
        val groups = children.groupBy { it.type::class }
        return groups.values.flatMap { group ->
            if (group.size == 1 || group[0].type is LiteralArgumentType) {
                group.map { compileNode(it, ancestorPath, routeGuard) }
            } else {
                listOf(compileMergedNode(group, ancestorPath, routeGuard))
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun compileMergedNode(
        alternatives: List<Argument<*, *>>,
        ancestorPath: List<Argument<*, *>>,
        outerRouteGuard: (Map<String, Any>.() -> Boolean)? = null,
    ): CmdArg<*> {
        val wildcards = alternatives.filter { it.isTarget == null }
        require(wildcards.size <= 1) {
            "Ambiguous merge: multiple arguments without isTarget at the same level: " +
                "${alternatives.map { it.key }}. Add isTarget to all but one."
        }

        val ordered = alternatives.filter { it.isTarget != null } + wildcards
        val syntheticKey = ordered.joinToString("|") { it.key }
        val routeKey = "_route_$syntheticKey"

        val derivations: Map<String, Context.() -> Any?> = buildMap {
            ordered.forEach { alt -> put(alt.key) { args[syntheticKey] } }
            put(routeKey) {
                val rawValue = args[syntheticKey]
                ordered.indexOfFirst { alt -> alt.invokeIsTarget(this, rawValue) }.takeIf { it >= 0 }
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
            isValid = isValid@{ k, result ->
                val winner = ordered.firstOrNull { alt -> alt.invokeIsTarget(this, k) }
                    ?: return@isValid IsValidResult.Invalid {}
                winner.invokeIsValid(this, k, result)
            },
            derivations = derivations,
            executor = mergedExecutor,
        )

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
                val ctx = Context(info.sender(), buildPartialContext(info.sender(), info.previousArgs(), ancestorPath))
                allSuggestions.flatMap { provider -> provider(ctx) }
                    .map { s -> StringTooltip.ofString(s.value, s.tooltip) }
                    .toTypedArray()
            })
        }

        if (mergedArg.executor != null) {
            cmdArg.executes(CommandExecutor { sender, args ->
                invokeExecutor(sender, args, fullPath, outerRouteGuard)
            })
        }

        ordered.forEachIndexed { index, alt ->
            val innerGuard: Map<String, Any>.() -> Boolean = { this[routeKey] == index }
            val combinedGuard: Map<String, Any>.() -> Boolean = if (outerRouteGuard != null) {
                { outerRouteGuard() && innerGuard() }
            } else {
                innerGuard
            }
            compileChildren(alt.children, fullPath, combinedGuard).forEach { child ->
                cmdArg.then(child)
            }
        }

        return cmdArg
    }

    private fun buildPartialContext(
        sender: CommandSender,
        previousArgs: CommandArguments,
        path: List<Argument<*, *>>
    ): Map<String, Any> {
        val contextArgs = mutableMapOf<String, Any>()
        for (node in path) {
            val rawValue: Any? = when (val t = node.type) {
                is LiteralArgumentType -> t.name
                else -> previousArgs[node.key] ?: continue
            }
            val result = node.invokeTransform(Context(sender, contextArgs.toMap()), rawValue)
            if (result is TransformResult.Success<*>) {
                result.value?.let { contextArgs[node.key] = it }
            }
        }
        for (node in path) {
            for ((key, block) in node.derivations) {
                block(Context(sender, contextArgs.toMap()))?.let { contextArgs[key] = it }
            }
        }
        return contextArgs
    }

    private fun invokeExecutor(
        sender: CommandSender,
        args: CommandArguments,
        path: List<Argument<*, *>>,
        routeGuard: (Map<String, Any>.() -> Boolean)? = null,
    ) {
        val contextArgs = mutableMapOf<String, Any>()

        for (node in path) {
            val rawValue: Any? = when (val t = node.type) {
                is LiteralArgumentType -> t.name
                else -> args[node.key]
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
        }

        for (node in path) {
            for ((key, block) in node.derivations) {
                block(Context(sender, contextArgs.toMap()))?.let { contextArgs[key] = it }
            }
        }

        if (routeGuard != null && !contextArgs.routeGuard()) return
        path.last().executor!!.invoke(Context(sender, contextArgs))
    }
}
