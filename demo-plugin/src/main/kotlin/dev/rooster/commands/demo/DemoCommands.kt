package dev.rooster.commands.demo

import dev.rooster.commands.CommandsScope
import dev.rooster.commands.IsValidResult
import dev.rooster.commands.types.*
import dev.rooster.commands.types.wrappers.*
import org.bukkit.entity.Player

fun CommandsScope.registerDemoCommands() {
    // rtest greet <player>        — greet a target player
    // rtest info <player>         — show target's location
    // rtest coords <x> <y> <z>   — echo integer coordinates
    command("rtest") {
        literal("greet") {
            player("target").onExecute {
                val target = args["target"] as Player
                sender.sendMessage("Hello, ${target.name}!")
            }
        }
        literal("info") {
            player("target").onExecute {
                val target = args["target"] as Player
                val loc = target.location
                sender.sendMessage("${target.name} is at ${loc.blockX}, ${loc.blockY}, ${loc.blockZ} in ${loc.world.name}")
            }
        }
        literal("coords") {
            val part = coordsPart("pos")
            register(part.head)
            part.tail.onExecute {
                val (x, y, z) = args["pos"] as Triple<*, *, *>
                sender.sendMessage("Coordinates: $x, $y, $z")
            }
        }
    }

    // rtell <@selector|name> <message>
    // Demonstrates same-type argument merging with isTarget dispatch:
    //   @-prefixed values route to "selector", plain names route to "player_name"
    command("rtell") {
        string("selector")
            .isTarget { it.startsWith("@") }
            .isValid { raw, _ ->
                if (raw.length < 2) IsValidResult.Invalid { sender.sendMessage("Selector too short.") }
                else IsValidResult.Valid
            }
            .then {
                greedyString("message").onExecute {
                    sender.sendMessage("[selector ${args["selector"]}] ${args["message"]}")
                }
            }
        string("player_name").then {
            greedyString("message").onExecute {
                sender.sendMessage("[player ${args["player_name"]}] ${args["message"]}")
            }
        }
    }
}
