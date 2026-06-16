package dev.rooster.commands.commandapi

import dev.rooster.commands.Argument
import dev.rooster.commands.Context
import dev.rooster.commands.SyntaxResult
import dev.rooster.commands.TransformResult
import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.arguments.Argument as CmdArg
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.arguments.CustomArgument
import dev.jorel.commandapi.arguments.CustomArgument.CustomArgumentException
import dev.jorel.commandapi.arguments.StringTooltip
import dev.jorel.commandapi.executors.CommandArguments
import org.bukkit.command.CommandSender

object Compiler {

    fun compile(label: String, vararg arguments: Argument<*, *>): CommandTree {
        return CommandTree(label).also { tree ->
            arguments.forEach { arg ->
                tree.then(compileNode(arg, emptyList()))
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun compileNode(
        argument: Argument<*, *>,
        ancestorPath: List<Argument<*, *>>
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
                val ctx = Context(info.sender, buildPartialContext(info.sender, info.previousArgs(), ancestorPath))
                provider(ctx)
                    .map { s -> StringTooltip.ofString(s.value, s.tooltip) }
                    .toTypedArray()
            })
        }

        if (argument.executor != null) {
            cmdArg.anyExecutor { sender, args ->
                invokeExecutor(sender, args, fullPath)
            }
        }

        argument.children.forEach { child ->
            cmdArg.then(compileNode(child, fullPath))
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
            val ctx = Context(sender, contextArgs.toMap())
            val result = node.invokeTransform(ctx, rawValue)
            if (result is TransformResult.Success<*>) {
                result.value?.let { contextArgs[node.key] = it }
            }
        }
        return contextArgs
    }

    private fun invokeExecutor(
        sender: CommandSender,
        args: CommandArguments,
        path: List<Argument<*, *>>
    ) {
        val contextArgs = mutableMapOf<String, Any>()

        for (node in path) {
            val rawValue: Any? = when (val t = node.type) {
                is LiteralArgumentType -> t.name
                else -> args[node.key]
            }

            val ctx = Context(sender, contextArgs.toMap())
            val transformResult = node.invokeTransform(ctx, rawValue)

            if (!node.invokeIsValid(ctx, rawValue, transformResult)) return

            if (transformResult is TransformResult.Success<*>) {
                transformResult.value?.let { contextArgs[node.key] = it }
            }
        }

        path.last().executor!!.invoke(Context(sender, contextArgs))
    }
}
