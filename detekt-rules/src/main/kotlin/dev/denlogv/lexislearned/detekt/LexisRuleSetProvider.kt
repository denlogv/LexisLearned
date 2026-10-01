package dev.denlogv.lexislearned.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.RuleSet
import io.gitlab.arturbosch.detekt.api.RuleSetProvider

/** Registers this project's own detekt rules under the rule set id `lexis`. */
class LexisRuleSetProvider : RuleSetProvider {
    override val ruleSetId: String = "lexis"

    /**
     * Creates the rule set.
     *
     * @param config the detekt configuration.
     * @return the rules of this project.
     */
    override fun instance(config: Config): RuleSet = RuleSet(ruleSetId, listOf(KDocCompleteness(config)))
}
