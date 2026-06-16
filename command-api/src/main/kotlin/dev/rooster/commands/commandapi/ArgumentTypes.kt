package dev.rooster.commands.commandapi

import dev.rooster.commands.ArgumentType
import dev.jorel.commandapi.arguments.Argument as CmdArg
import dev.jorel.commandapi.arguments.*
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

data class LiteralArgumentType(val name: String) : ArgumentType<String>

data class IntegerArgumentType(val min: Int? = null, val max: Int? = null) : ArgumentType<Int>
data class DoubleArgumentType(val min: Double? = null, val max: Double? = null) : ArgumentType<Double>
data class FloatArgumentType(val min: Float? = null, val max: Float? = null) : ArgumentType<Float>
data class LongArgumentType(val min: Long? = null, val max: Long? = null) : ArgumentType<Long>

data object BooleanArgumentType : ArgumentType<Boolean>
data object StringArgumentType : ArgumentType<String>
data object GreedyStringArgumentType : ArgumentType<String>

data object PlayerArgumentType : ArgumentType<Player>
data object WorldArgumentType : ArgumentType<World>
data object ItemStackArgumentType : ArgumentType<ItemStack>
data object LocationArgumentType : ArgumentType<Location>

@Suppress("UNCHECKED_CAST")
fun <K> ArgumentType<K>.toCommandApiArg(key: String): CmdArg<K> = (when (this) {
    is LiteralArgumentType -> LiteralArgument(name)

    is IntegerArgumentType -> when {
        min != null && max != null -> IntegerArgument(key, min, max)
        min != null -> IntegerArgument(key, min)
        else -> IntegerArgument(key)
    }
    is DoubleArgumentType -> when {
        min != null && max != null -> DoubleArgument(key, min, max)
        min != null -> DoubleArgument(key, min)
        else -> DoubleArgument(key)
    }
    is FloatArgumentType -> when {
        min != null && max != null -> FloatArgument(key, min, max)
        min != null -> FloatArgument(key, min)
        else -> FloatArgument(key)
    }
    is LongArgumentType -> when {
        min != null && max != null -> LongArgument(key, min, max)
        min != null -> LongArgument(key, min)
        else -> LongArgument(key)
    }

    BooleanArgumentType -> BooleanArgument(key)
    StringArgumentType -> StringArgument(key)
    GreedyStringArgumentType -> GreedyStringArgument(key)

    PlayerArgumentType -> PlayerArgument(key)
    WorldArgumentType -> WorldArgument(key)
    ItemStackArgumentType -> ItemStackArgument(key)
    LocationArgumentType -> LocationArgument(key)

    else -> error("Unknown ArgumentType: ${this::class.simpleName}. Provide a custom toCommandApiArg mapping.")
}) as CmdArg<K>
