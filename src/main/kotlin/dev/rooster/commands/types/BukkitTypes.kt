package dev.rooster.commands.types

import dev.rooster.commands.*
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

data object PlayerArgumentType : ArgumentType<Player>
data object WorldArgumentType : ArgumentType<World>
data object ItemStackArgumentType : ArgumentType<ItemStack>

fun ChildrenScope.player(key: String): ArgumentBuilder<Player, Player> =
    ArgumentBuilder(key, PlayerArgumentType) { TransformResult.Success(it) }.also { register(it) }

fun ChildrenScope.player(key: String, block: ChildrenScope.() -> Unit): ArgumentBuilder<Player, Player> =
    ArgumentBuilder(key, PlayerArgumentType) { TransformResult.Success(it) }.then(block).also { register(it) }

fun ChildrenScope.world(key: String): ArgumentBuilder<World, World> =
    ArgumentBuilder(key, WorldArgumentType) { TransformResult.Success(it) }.also { register(it) }

fun ChildrenScope.world(key: String, block: ChildrenScope.() -> Unit): ArgumentBuilder<World, World> =
    ArgumentBuilder(key, WorldArgumentType) { TransformResult.Success(it) }.then(block).also { register(it) }

fun ChildrenScope.itemStack(key: String): ArgumentBuilder<ItemStack, ItemStack> =
    ArgumentBuilder(key, ItemStackArgumentType) { TransformResult.Success(it) }.also { register(it) }

fun ChildrenScope.itemStack(key: String, block: ChildrenScope.() -> Unit): ArgumentBuilder<ItemStack, ItemStack> =
    ArgumentBuilder(key, ItemStackArgumentType) { TransformResult.Success(it) }.then(block).also { register(it) }
