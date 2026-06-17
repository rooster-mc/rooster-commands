package dev.rooster.commands.sql

import dev.rooster.commands.*
import dev.rooster.commands.types.StringArgumentType
import dev.rooster.commands.types.wrappers.ListBuilder
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

// Creates a list argument backed by a DB table column.
//
// The transform resolves the matched entity, so isValid reuses that result
// rather than running a second query. User-added .isValid { } extensions
// receive the entity in TransformResult.Success and run in-memory.
fun <E : IntEntity> ChildrenScope.dbList(
    key: String,
    entity: IntEntityClass<E>,
    displayField: Column<String>,
    filter: (Context.(E) -> Boolean)? = null,
    ignoreCase: Boolean = false,
    notMatchingError: Context.(String) -> Unit = { sender.sendMessage("'$it' is not a valid option.") },
): ListBuilder<E> {
    val suggestions: Context.() -> List<Suggestion> = {
        val ctx = this
        transaction {
            entity.wrapRows(entity.table.selectAll())
                .let { rows -> if (filter != null) rows.filter { filter.invoke(ctx, it) } else rows }
                .map { Suggestion(it.readValues[displayField]) }
        }
    }

    val transform: Context.(String) -> TransformResult<E> = { raw ->
        val ctx = this
        val match = transaction {
            entity.wrapRows(
                entity.table.selectAll().where {
                    if (ignoreCase) displayField.lowerCase() eq raw.lowercase()
                    else displayField eq raw
                }
            ).let { rows -> if (filter != null) rows.filter { filter.invoke(ctx, it) } else rows }
                .firstOrNull()
        }
        if (match != null) TransformResult.Success(match) else TransformResult.Failure
    }

    // The transform already resolved whether the entity exists, so base
    // validation just promotes Failure → Invalid without a second query.
    val base: Context.(String, TransformResult<E>) -> IsValidResult = { raw, result ->
        when (result) {
            is TransformResult.Success -> IsValidResult.Valid
            TransformResult.Failure -> IsValidResult.Invalid { notMatchingError.invoke(this, raw) }
        }
    }

    val inner = ArgumentBuilder(key, StringArgumentType, transform).suggest(suggestions)
    return ListBuilder(inner, base).also { register(it) }
}
