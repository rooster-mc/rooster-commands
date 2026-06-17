package dev.rooster.commands.commandapi

import dev.rooster.commands.CommandCompilerService
import dev.rooster.commands.CommandDefinition

class CommandApiCompilerService : CommandCompilerService {
    override fun register(definition: CommandDefinition) {
        Compiler.compile(definition.label, *definition.arguments.toTypedArray()).register()
    }
}
