package dev.rooster.commands.demo

import dev.jorel.commandapi.CommandAPI
import dev.jorel.commandapi.CommandAPIBukkitConfig
import dev.rooster.commands.commandapi.commands
import dev.rooster.core.initRooster
import org.bukkit.plugin.java.JavaPlugin

class DemoPlugin : JavaPlugin() {
    override fun onLoad() {
        CommandAPI.onLoad(CommandAPIBukkitConfig(this).verboseOutput(false))
    }

    override fun onEnable() {
        CommandAPI.onEnable()

        initRooster(this) {
            commands {
                registerDemoCommands()
            }
        }
    }

    override fun onDisable() {
        CommandAPI.onDisable()
    }
}
