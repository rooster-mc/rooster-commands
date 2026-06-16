package dev.rooster.commands

sealed class ArgumentRule {
    data object Accepted : ArgumentRule()
    data class Rejected(val error: Context.() -> Unit) : ArgumentRule() {
        fun toInvalid() = IsValidResult.Invalid(error)
    }
}

class Rules(
    vararg rules: Pair<ArgumentRule, Context.() -> Boolean>,
    val further: (Context.() -> Rules)? = null,
) {
    private val rules = rules.toList()

    fun evaluate(ctx: Context): IsValidResult {
        for ((rule, condition) in rules) {
            if (rule is ArgumentRule.Rejected && condition(ctx)) return rule.toInvalid()
        }
        return further?.invoke(ctx)?.evaluate(ctx) ?: IsValidResult.Valid
    }
}
