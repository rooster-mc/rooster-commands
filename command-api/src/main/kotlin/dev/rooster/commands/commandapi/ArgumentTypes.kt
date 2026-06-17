package dev.rooster.commands.commandapi

import dev.rooster.commands.*
import dev.rooster.commands.types.*
import dev.jorel.commandapi.arguments.Argument as CmdArg
import dev.jorel.commandapi.arguments.*

@Suppress("UNCHECKED_CAST")
fun <K> ArgumentType<K>.toCommandApiArg(key: String): CmdArg<K> = (when (this) {
    is LiteralArgumentType -> LiteralArgument(name)

    is IntegerArgumentType -> when {
        min != null && max != null -> IntegerArgument(key, min!!, max!!)
        min != null -> IntegerArgument(key, min!!)
        else -> IntegerArgument(key)
    }
    is DoubleArgumentType -> when {
        min != null && max != null -> DoubleArgument(key, min!!, max!!)
        min != null -> DoubleArgument(key, min!!)
        else -> DoubleArgument(key)
    }
    is FloatArgumentType -> when {
        min != null && max != null -> FloatArgument(key, min!!, max!!)
        min != null -> FloatArgument(key, min!!)
        else -> FloatArgument(key)
    }
    is LongArgumentType -> when {
        min != null && max != null -> LongArgument(key, min!!, max!!)
        min != null -> LongArgument(key, min!!)
        else -> LongArgument(key)
    }

    BooleanArgumentType -> BooleanArgument(key)
    StringArgumentType -> StringArgument(key)
    GreedyStringArgumentType -> GreedyStringArgument(key)

    PlayerArgumentType -> PlayerArgument(key)
    WorldArgumentType -> WorldArgument(key)
    ItemStackArgumentType -> ItemStackArgument(key)

    else -> error("Unknown ArgumentType: ${this::class.simpleName}. Provide a custom toCommandApiArg mapping.")
}) as CmdArg<K>
