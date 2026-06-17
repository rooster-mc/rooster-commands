package dev.rooster.commands.commandapi

import dev.rooster.commands.ChildrenScope
import dev.rooster.commands.CommandsScope
import dev.rooster.commands.commandsRaw
import dev.rooster.core.RoosterModuleBuilder
import dev.jorel.commandapi.CommandTree

fun command(label: String, block: ChildrenScope.() -> Unit): CommandTree {
    val scope = ChildrenScope()
    scope.block()
    return Compiler.compile(label, *scope.buildChildren().toTypedArray())
}

fun RoosterModuleBuilder.commands(block: CommandsScope.() -> Unit) {
    services.set(CommandApiCompilerService())
    commandsRaw(block)
}
