/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.replacement

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.idea.core.asExpression
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.intentions.InsertExplicitTypeArgumentsIntention
import org.jetbrains.kotlin.idea.references.canBeResolvedViaImport
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getReceiverExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.isReallySuccess
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitReceiver
import java.util.*

class ReplacementBuilder(
        private val targetCallable: CallableDescriptor,
        private val resolutionFacade: ResolutionFacade
) {
    private val psiFactory = KtPsiFactory(resolutionFacade.project)

    fun buildReplacementCode(
            mainExpression: KtExpression?,
            statementsBefore: List<KtExpression>,
            analyze: () -> BindingContext,
            importFqNames: Collection<FqName> = emptyList()
    ): ReplacementCode {
        var bindingContext = analyze()

        val result = MutableReplacementCode(mainExpression, statementsBefore.toMutableList(), importFqNames.toMutableSet())

        val typeArgsToAdd = ArrayList<Pair<KtCallExpression, KtTypeArgumentList>>()
        result.forEachDescendantOfType<KtCallExpression> {
            if (InsertExplicitTypeArgumentsIntention.isApplicableTo(it, bindingContext)) {
                typeArgsToAdd.add(it to InsertExplicitTypeArgumentsIntention.createTypeArguments(it, bindingContext)!!)
            }
        }

        if (typeArgsToAdd.isNotEmpty()) {
            for ((callExpr, typeArgs) in typeArgsToAdd) {
                callExpr.addAfter(typeArgs, callExpr.calleeExpression)
            }

            // reanalyze expression - new usages of type parameters may be added
            bindingContext = analyze()
        }

        val receiversToAdd = ArrayList<Pair<KtExpression, KtExpression>>()

        result.forEachDescendantOfType<KtSimpleNameExpression> { expression ->
            val target = bindingContext[BindingContext.REFERENCE_TARGET, expression] ?: return@forEachDescendantOfType

            //TODO: other types of references ('[]' etc)
            if (expression.mainReference.canBeResolvedViaImport(target)) {
                result.fqNamesToImport.add(target.importableFqName!!)
            }

            if (expression.getReceiverExpression() == null) {
                if (target is ValueParameterDescriptor && target.containingDeclaration == targetCallable) {
                    expression.putCopyableUserData(ReplacementCode.PARAMETER_USAGE_KEY, target.name)
                }
                else if (target is TypeParameterDescriptor && target.containingDeclaration == targetCallable) {
                    expression.putCopyableUserData(ReplacementCode.TYPE_PARAMETER_USAGE_KEY, target.name)
                }

                val resolvedCall = expression.getResolvedCall(bindingContext)
                if (resolvedCall != null && resolvedCall.isReallySuccess()) {
                    val receiver = if (resolvedCall.resultingDescriptor.isExtension)
                        resolvedCall.extensionReceiver
                    else
                        resolvedCall.dispatchReceiver
                    if (receiver is ImplicitReceiver) {
                        val resolutionScope = expression.getResolutionScope(bindingContext, resolutionFacade)
                        val receiverExpression = receiver.asExpression(resolutionScope, psiFactory)
                        if (receiverExpression != null) {
                            receiversToAdd.add(expression to receiverExpression)
                        }
                    }
                }
            }
        }

        // add receivers in reverse order because arguments of a call were processed after the callee's name
        for ((expr, receiverExpression) in receiversToAdd.asReversed()) {
            val expressionToReplace = expr.parent as? KtCallExpression ?: expr
            result.replaceExpression(expressionToReplace,
                                              psiFactory.createExpressionByPattern("$0.$1", receiverExpression, expressionToReplace))
        }

        return result.toNonMutable()
    }
}