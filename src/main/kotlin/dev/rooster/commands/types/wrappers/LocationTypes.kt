package dev.rooster.commands.types.wrappers

import dev.rooster.commands.*
import dev.rooster.commands.types.IntegerArgumentType

private fun buildCoordsPart(prefix: String): ArgumentPart<Int, Int> {
    val z = ArgumentBuilder("${prefix}_z", IntegerArgumentType()) { TransformResult.Success(it) }
        .derive(prefix) { Triple(args["${prefix}_x"] as Int, args["${prefix}_y"] as Int, args["${prefix}_z"] as Int) }
    val y = ArgumentBuilder("${prefix}_y", IntegerArgumentType()) { TransformResult.Success(it) }.then(z)
    val x = ArgumentBuilder("${prefix}_x", IntegerArgumentType()) { TransformResult.Success(it) }.then(y)
    return ArgumentPart(head = x, tail = z)
}

fun coordsPart(prefix: String): ArgumentPart<Int, Int> = buildCoordsPart(prefix)

fun ChildrenScope.coordsPart(prefix: String): ArgumentPart<Int, Int> {
    val part = buildCoordsPart(prefix)
    register(part.head)
    return part
}
