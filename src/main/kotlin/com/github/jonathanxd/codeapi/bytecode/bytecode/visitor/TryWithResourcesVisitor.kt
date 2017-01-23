package com.github.jonathanxd.codeapi.gen.bytecode.visitor

import com.github.jonathanxd.codeapi.CodeAPI
import com.github.jonathanxd.codeapi.CodeSource
import com.github.jonathanxd.codeapi.common.MVData
import com.github.jonathanxd.codeapi.common.TypeSpec
import com.github.jonathanxd.codeapi.gen.BytecodeClass
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.gen.visit.VoidVisitor
import com.github.jonathanxd.codeapi.helper.Helper
import com.github.jonathanxd.codeapi.helper.PredefinedTypes
import com.github.jonathanxd.codeapi.impl.CodeField
import com.github.jonathanxd.codeapi.impl.TryBlockImpl
import com.github.jonathanxd.codeapi.interfaces.CatchBlock
import com.github.jonathanxd.codeapi.interfaces.TryWithResources
import com.github.jonathanxd.codeapi.literals.Literals
import com.github.jonathanxd.iutils.data.MapData

class TryWithResourcesVisitor : VoidVisitor<TryWithResources, BytecodeClass, MVData> {

    private var TRY_WITH_RESOURCES_VARIABLES = 0

    private fun getAndIncrementTryWithRes(): Int {
        val i = TRY_WITH_RESOURCES_VARIABLES

        ++TRY_WITH_RESOURCES_VARIABLES

        return i
    }

    override fun voidVisit(tryWithResources: TryWithResources, extraData: MapData, visitorGenerator: VisitorGenerator<BytecodeClass>, additional: MVData) {
        val variable = tryWithResources.variable

        // Generate try-catch initialize field
        visitorGenerator.generateTo(CodeField::class.java, variable, extraData, null, additional)

        val num = this.getAndIncrementTryWithRes()

        val throwableFieldName = "\$throwable#" + num

        // Generate exception field
        val throwableField = CodeField(
                throwableFieldName,
                PredefinedTypes.THROWABLE,
                Literals.NULL)

        visitorGenerator.generateTo(CodeField::class.java, throwableField, extraData, null, additional)

        // Generate try block
        val catch_ = "\$catch#" + num
        val catchBlock = Helper.catchBlock(listOf(PredefinedTypes.THROWABLE),
                catch_,
                Helper.sourceOf(
                        Helper.setLocalVariable(throwableFieldName, PredefinedTypes.THROWABLE, Helper.accessLocalVariable(catch_, PredefinedTypes.THROWABLE)),
                        Helper.throwException(Helper.accessLocalVariable(catch_, PredefinedTypes.THROWABLE))))

        val catch2_name = "\$catch_2#" + num

        //AutoCloseable#close();
        val closeInvocation = CodeAPI.invokeInterface(
                AutoCloseable::class.java,
                Helper.accessLocalVariable(variable),
                "close",
                TypeSpec(PredefinedTypes.VOID))

        //Throwable#addSuppressed(Throwable)
        val addSuppressedInvocation = CodeAPI.invokeVirtual(PredefinedTypes.THROWABLE,
                Helper.accessLocalVariable(throwableField),
                "addSuppressed",
                TypeSpec(PredefinedTypes.VOID, PredefinedTypes.THROWABLE),
                CodeAPI.argument(Helper.accessLocalVariable(catch2_name)))

        val surroundedCloseInvocation = Helper.surround(Helper.sourceOf(closeInvocation),
                listOf(Helper.catchBlock(listOf(PredefinedTypes.THROWABLE),
                        catch2_name,
                        Helper.sourceOf(
                                addSuppressedInvocation
                        )
                ))
        )

        val catchBlocks = java.util.ArrayList<CatchBlock>()

        catchBlocks.add(catchBlock)
        catchBlocks.addAll(tryWithResources.catchBlocks)

        val tryCatchBlock = Helper.surround(tryWithResources.requiredBody, catchBlocks,
                Helper.sourceOf(
                        Helper.ifExpression(Helper.createIfVal()
                                .add1(Helper.checkNotNull(Helper.accessLocalVariable(variable)))
                                .make(),
                                Helper.sourceOf(
                                        Helper.ifExpression(Helper.createIfVal()
                                                .add1(Helper.checkNotNull(Helper.accessLocalVariable(throwableField))).make(),
                                                Helper.sourceOf(
                                                        surroundedCloseInvocation
                                                ),
                                                Helper.elseExpression(Helper.sourceOf(
                                                        closeInvocation
                                                )))
                                )),
                        tryWithResources.finallyBlock.orElse(CodeSource.empty())
                ))

        visitorGenerator.generateTo(TryBlockImpl::class.java, tryCatchBlock, extraData, null, additional)
    }

}