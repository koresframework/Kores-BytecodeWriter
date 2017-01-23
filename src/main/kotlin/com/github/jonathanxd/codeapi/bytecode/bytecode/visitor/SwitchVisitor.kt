package com.github.jonathanxd.codeapi.gen.bytecode.visitor

import com.github.jonathanxd.codeapi.CodeAPI
import com.github.jonathanxd.codeapi.CodePart
import com.github.jonathanxd.codeapi.CodeSource
import com.github.jonathanxd.codeapi.annotation.GenerateTo
import com.github.jonathanxd.codeapi.common.Flow
import com.github.jonathanxd.codeapi.common.MVData
import com.github.jonathanxd.codeapi.common.SwitchTypes
import com.github.jonathanxd.codeapi.gen.BytecodeClass
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.gen.visit.VoidVisitor
import com.github.jonathanxd.codeapi.helper.PredefinedTypes
import com.github.jonathanxd.codeapi.impl.CaseImpl
import com.github.jonathanxd.codeapi.impl.IfBlockImpl
import com.github.jonathanxd.codeapi.interfaces.*
import com.github.jonathanxd.codeapi.literals.Literals
import com.github.jonathanxd.codeapi.operators.Operator
import com.github.jonathanxd.codeapi.util.BiMultiVal
import com.github.jonathanxd.codeapi.util.gen.CodePartUtil
import com.github.jonathanxd.iutils.data.MapData
import org.objectweb.asm.Label

object SwitchVisitor : VoidVisitor<Switch, BytecodeClass, MVData> {

    override fun voidVisit(t: Switch, extraData: MapData, visitorGenerator: VisitorGenerator<BytecodeClass>, additional: MVData) {
        val switchType = t.switchType
        var aSwitch: Switch = t

        if (switchType !== SwitchTypes.NUMERIC) {
            if (!switchType.isUnique) {
                aSwitch = this.insertEqualityCheck(aSwitch)
            }

            val generate = aSwitch.switchType.generator.generate(aSwitch)

            visitorGenerator.generateTo(Switch::class.java, generate, extraData, additional)
        } else {
            val mv = additional.methodVisitor
            val value = aSwitch.value.orElseThrow(::NullPointerException)

            visitorGenerator.generateTo(value.javaClass, value, extraData, additional)

            val originCaseList = aSwitch.cases
            var filteredCaseList = originCaseList
                    .filter(Case::isNotDefault)
                    .sortedBy(this::getInt)// or Integer.compare(this.getInt(o1), this.getInt(o2))

            val useLookupSwitch = this.useLookupSwitch(filteredCaseList)

            val outsideStart = Label()
            val insideStart = Label()
            val outsideEnd = Label()


            val defaultLabel = Label()

            val flow = Flow(outsideStart, insideStart, defaultLabel, outsideEnd)

            extraData.registerData(ConstantDatas.FLOW_TYPE_INFO, flow)

            mv.visitLabel(outsideStart)

            val labels: Array<Label>

            if (!useLookupSwitch) {
                var min = this.getMin(filteredCaseList)
                var max = this.getMax(filteredCaseList)

                filteredCaseList = this.fill(min, max, filteredCaseList)

                labels = this.toLabel(filteredCaseList, defaultLabel)

                min = this.getMin(filteredCaseList)
                max = this.getMax(filteredCaseList)

                mv.visitTableSwitchInsn(min, max, defaultLabel, *labels)

            } else {
                labels = this.toLabel(filteredCaseList, defaultLabel)

                val keys = filteredCaseList.map(this::getInt).toIntArray()

                mv.visitLookupSwitchInsn(defaultLabel, keys, labels)
            }

            mv.visitLabel(insideStart)

            // Generate Case code

            labels.forEachIndexed { i, label ->
                val aCase = filteredCaseList[i]

                if (aCase !is EmptyCase) {
                    mv.visitLabel(label)

                    visitorGenerator.generateTo(CodeSource::class.java, aCase.body.orElseThrow(::NullPointerException), extraData, additional)
                }
            }

            // /Generate Case code

            mv.visitLabel(defaultLabel)

            extraData.unregisterData(ConstantDatas.FLOW_TYPE_INFO, flow)

            val aDefault = this.getDefault(originCaseList)

            val codeSource = aDefault.getBody().orElse(CodeSource.empty())

            visitorGenerator.generateTo(CodeSource::class.java, codeSource, extraData, additional)


            mv.visitLabel(outsideEnd)
        }

    }

    fun toLabel(caseList: List<Case>, defaultLabel: Label): Array<Label> {
        return caseList.map { if (it is EmptyCase) defaultLabel else Label() }.toTypedArray()
    }

    fun getDefault(caseList: List<Case>): Case {
        return caseList.firstOrNull(Case::isDefault) ?: CodeAPI.caseDefault(CodeAPI.sourceOfParts())
    }

    /**
     * Fill the case list.
     */
    private fun fill(min: Int, max: Int, caseList: List<Case>): List<Case> {

        val filledCases = java.util.ArrayList<Case>()

        for (i in min..max + 1 - 1) {
            val aCase = this.getCase(caseList, i)

            if (aCase != null) {
                filledCases.add(aCase)
            } else {
                filledCases.add(EmptyCase(Literals.INT(i), null))
            }
        }

        return filledCases
    }

    private fun getCase(caseList: List<Case>, i: Int): Case? {
        for (aCase in caseList) {
            if (this.getInt(aCase) == i)
                return aCase
        }

        return null
    }

    private fun getMin(caseList: List<Case>): Int {
        val last = caseList
                .filterNot { it.isDefault }
                .map { it.value.map { codePart -> codePart as Literals.IntLiteral }.orElseThrow(::NullPointerException) }
                .map { Integer.parseInt(it.name) }
                .min()
                ?: Integer.MAX_VALUE

        return last
    }

    private fun getMax(caseList: List<Case>): Int {
        val last = caseList
                .filterNot { it.isDefault }
                .map { it.value.map { codePart -> codePart as Literals.IntLiteral }.orElseThrow(::NullPointerException) }
                .map { Integer.parseInt(it.name) }
                .max()
                ?: 0

        return last
    }

    private fun getCasesToFill(caseList: List<Case>): Int {
        val min = this.getMin(caseList)
        val max = this.getMax(caseList)

        val size = caseList.size - 1

        return max - (min + size)
    }

    /**
     * Based on java sources.
     */
    private fun useLookupSwitch(caseList: List<Case>): Boolean {

        val casesToFill = this.getCasesToFill(caseList)
        val labels = caseList.size

        val tableSpaceCost = (4 * casesToFill).toLong()
        val tableTimeCost = 3
        val lookupSpaceCost = (3 + 2 * labels).toLong()

        // If lookup cost < table cost.
        return lookupSpaceCost + 3 * labels < tableSpaceCost + 3 * tableTimeCost
    }

    private fun insertEqualityCheck(aSwitch: Switch): Switch {
        val switchValue = aSwitch.value.orElseThrow(::NullPointerException)

        if ((switchValue as Typed).type.orElseThrow(::NullPointerException).`is`(PredefinedTypes.INT))
            return aSwitch

        return aSwitch.setCases(aSwitch.cases.map { aCase ->

            if (aCase.isDefault)
                return@map aCase

            val codeSource = aCase.body.orElse(null)

            if (codeSource != null) {
                val caseValue = aCase.value.orElseThrow(::NullPointerException)

                val type = CodePartUtil.getType(caseValue)

                if (type.`is`(PredefinedTypes.INT))
                    return@map aCase

                return@map aCase.setBody(CodeAPI.sourceOfParts(
                        SwitchIfBlock(codeSource,
                                listOf(CodeAPI.checkTrue(
                                        CodeAPI.invokeVirtual(Any::class.java, switchValue, "equals",
                                                CodeAPI.typeSpec(PredefinedTypes.BOOLEAN, PredefinedTypes.OBJECT),
                                                CodeAPI.argument(caseValue)
                                        )
                                )))
                ))
            }

            return@map aCase
        })
    }

    private fun getInt(aCase: Case): Int {
        if (aCase.isDefault)
            return -1

        return Integer.parseInt(aCase.value.map { codePart -> codePart as Literals.IntLiteral }.orElseThrow(::NullPointerException).name)
    }

    @GenerateTo(IfBlock::class)
    internal class SwitchIfBlock : IfBlockImpl {

        constructor(body: CodeSource, ifExprs: List<CodePart>) : super(body, ifExprs, null)

        constructor(body: CodeSource, ifExpressions: BiMultiVal<CodePart, IfExpr, Operator>) : super(body, ifExpressions, null)
    }

    private class EmptyCase(value: Typed, body: CodeSource?) : CaseImpl(value, body)
}