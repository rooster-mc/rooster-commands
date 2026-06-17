package dev.rooster.commands.sql

import dev.rooster.commands.*
import dev.rooster.commands.types.literal
import dev.rooster.commands.types.string
import dev.rooster.commands.types.wrappers.uniqueIn
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

@CommandDsl
class CrudScope<E : IntEntity> {
    internal var filter: (Context.(E) -> Boolean)? = null
    internal var onCreate: (Context.() -> Unit)? = null
    internal var onEdit: (Context.() -> Unit)? = null
    internal var onDelete: (Context.() -> Unit)? = null
    internal var onGet: (Context.() -> Unit)? = null

    fun filter(block: Context.(E) -> Boolean) { filter = block }
    fun onCreate(block: Context.() -> Unit) { onCreate = block }
    fun onEdit(block: Context.() -> Unit) { onEdit = block }
    fun onDelete(block: Context.() -> Unit) { onDelete = block }
    fun onGet(block: Context.() -> Unit) { onGet = block }
}

fun <E : IntEntity> ChildrenScope.crudArg(
    entity: IntEntityClass<E>,
    displayField: Column<String>,
    nameKey: String = "name",
    ignoreCase: Boolean = false,
    block: CrudScope<E>.() -> Unit = {},
) {
    val scope = CrudScope<E>().apply(block)

    literal("create") {
        string(nameKey)
            .uniqueIn(
                existing = {
                    transaction {
                        entity.wrapRows(entity.table.selectAll())
                            .map { it.readValues[displayField] }
                    }
                },
                ignoreCase = ignoreCase,
            )
            .apply { scope.onCreate?.let { executorCallback = it } }
    }

    for ((verb, executor) in listOf(
        "edit" to scope.onEdit,
        "delete" to scope.onDelete,
        "get" to scope.onGet,
    )) {
        literal(verb) {
            dbList(nameKey, entity, displayField, scope.filter, ignoreCase)
                .apply { executor?.let { executorCallback = it } }
        }
    }
}
