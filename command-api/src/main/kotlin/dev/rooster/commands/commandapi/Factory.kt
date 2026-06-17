package dev.rooster.commands.commandapi

import dev.rooster.commands.ChildrenScope
import dev.jorel.commandapi.CommandTree

fun command(label: String, block: ChildrenScope.() -> Unit): CommandTree {
    val scope = ChildrenScope()
    scope.block()
    return Compiler.compile(label, *scope.buildChildren().toTypedArray())
}
