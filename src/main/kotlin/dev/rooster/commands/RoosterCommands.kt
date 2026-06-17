package dev.rooster.commands

import dev.rooster.core.RoosterModuleBuilder
import dev.rooster.core.RoosterService
import dev.rooster.core.RoosterServices
import org.bukkit.plugin.java.JavaPlugin
import kotlin.reflect.KClass

data class CommandDefinition(val label: String, val arguments: List<Argument<*, *>>)

interface CommandCompilerService : RoosterService {
    override fun targetClass(): KClass<out RoosterService> = CommandCompilerService::class
    fun register(definition: CommandDefinition)
}

class CommandsScope {
    private val definitions = mutableListOf<CommandDefinition>()

    fun command(label: String, block: ChildrenScope.() -> Unit) {
        val scope = ChildrenScope()
        scope.block()
        definitions.add(CommandDefinition(label, scope.buildChildren()))
    }

    internal fun build(): List<CommandDefinition> = definitions.toList()
}

object RoosterCommands {
    private lateinit var plugin: JavaPlugin
    private val services = RoosterServices()
    private val compiler: CommandCompilerService by lazy { services.get() }

    fun init(
        plugin: JavaPlugin,
        definitions: List<CommandDefinition>,
        services: RoosterServices,
    ) {
        this.plugin = plugin
        this.services.byOther(services)
        definitions.forEach { compiler.register(it) }
    }
}

fun RoosterModuleBuilder.commandsRaw(block: CommandsScope.() -> Unit) {
    val scope = CommandsScope()
    scope.block()
    RoosterCommands.init(plugin, scope.build(), services)
}
