package dev.denlogv.lexislearned.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType

/**
 * Requires a KDoc on every class, object, interface and function, private ones included, that documents every parameter
 * (every constructor parameter for a class) with `@param` or `@property`.
 *
 * detekt's own `UndocumentedPublic*` rules skip non-public declarations and do not look at parameters, and
 * `OutdatedDocumentation` accepts a KDoc with no tags at all. This rule closes that gap.
 *
 * @param config the rule's configuration.
 */
class KDocCompleteness(config: Config = Config.empty) : Rule(config) {
    override val issue = Issue(
        javaClass.simpleName,
        Severity.Maintainability,
        "Every class and function needs a KDoc that documents each parameter.",
        Debt.FIVE_MINS,
    )

    /**
     * Checks a class, interface or object. Enum entries, anonymous objects and companion objects are skipped: a companion
     * only holds constants and helpers, which are documented one by one.
     *
     * @param classOrObject the declaration.
     */
    override fun visitClassOrObject(classOrObject: KtClassOrObject) {
        super.visitClassOrObject(classOrObject)
        val companion = (classOrObject as? KtObjectDeclaration)?.isCompanion() == true
        val skipped = classOrObject is KtEnumEntry || classOrObject.isLocal || classOrObject.name == null || companion
        if (!skipped) check(classOrObject, "class", classOrObject.primaryConstructorParameters.mapNotNull { it.name })
    }

    /**
     * Checks a function. Local functions are skipped.
     *
     * @param function the declaration.
     */
    override fun visitNamedFunction(function: KtNamedFunction) {
        super.visitNamedFunction(function)
        if (!function.isLocal) check(function, "function", function.valueParameters.mapNotNull { it.name })
    }

    /**
     * Reports a declaration without a KDoc, or whose KDoc leaves out parameters.
     *
     * @param declaration the class or function.
     * @param kind "class" or "function", for the message.
     * @param parameters the names that must be documented.
     */
    private fun check(declaration: KtNamedDeclaration, kind: String, parameters: List<String>) {
        val doc = declaration.docComment
        if (doc == null) {
            report(declaration, "The $kind '${declaration.name}' has no KDoc.")
            return
        }
        val documented = doc.collectDescendantsOfType<KDocTag>()
            .filter { it.name == "param" || it.name == "property" }
            .mapNotNull { it.getSubjectName() }
            .toSet()
        parameters.filter { it !in documented }
            .forEach { report(declaration, "The KDoc of '${declaration.name}' does not document @param $it.") }
    }

    /**
     * Adds a finding.
     *
     * @param declaration where the problem is.
     * @param message what is wrong.
     */
    private fun report(declaration: KtNamedDeclaration, message: String) {
        report(CodeSmell(issue, Entity.atName(declaration), message))
    }
}
