/*
 *      CodeAPI-BytecodeWriter - Framework to generate Java code and Bytecode code. <https://github.com/JonathanxD/CodeAPI-BytecodeWriter>
 *
 *         The MIT License (MIT)
 *
 *      Copyright (c) 2017 TheRealBuggy/JonathanxD (https://github.com/JonathanxD/ & https://github.com/TheRealBuggy/) <jonathan.scripter@programmer.net>
 *      Copyright (c) contributors
 *
 *
 *      Permission is hereby granted, free of charge, to any person obtaining a copy
 *      of this software and associated documentation files (the "Software"), to deal
 *      in the Software without restriction, including without limitation the rights
 *      to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *      copies of the Software, and to permit persons to whom the Software is
 *      furnished to do so, subject to the following conditions:
 *
 *      The above copyright notice and this permission notice shall be included in
 *      all copies or substantial portions of the Software.
 *
 *      THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *      IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *      FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *      AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *      LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *      OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *      THE SOFTWARE.
 */
package com.github.jonathanxd.codeapi.bytecode.gen.visitor

import com.github.jonathanxd.codeapi.CodeAPI
import com.github.jonathanxd.codeapi.CodePart
import com.github.jonathanxd.codeapi.CodeSource
import com.github.jonathanxd.codeapi.Types
import com.github.jonathanxd.codeapi.annotation.GenerateTo
import com.github.jonathanxd.codeapi.base.Case
import com.github.jonathanxd.codeapi.base.IfStatement
import com.github.jonathanxd.codeapi.base.SwitchStatement
import com.github.jonathanxd.codeapi.base.Typed
import com.github.jonathanxd.codeapi.builder.CaseBuilder
import com.github.jonathanxd.codeapi.builder.SwitchStatementBuilder
import com.github.jonathanxd.codeapi.bytecode.BytecodeClass
import com.github.jonathanxd.codeapi.bytecode.common.Flow
import com.github.jonathanxd.codeapi.bytecode.common.MVData
import com.github.jonathanxd.codeapi.bytecode.util.CodePartUtil
import com.github.jonathanxd.codeapi.common.SwitchTypes
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.gen.visit.VoidVisitor
import com.github.jonathanxd.codeapi.literal.Literals
import com.github.jonathanxd.iutils.data.MapData
import org.objectweb.asm.Label

object SwitchVisitor : VoidVisitor<SwitchStatement, BytecodeClass, MVData> {

    override fun voidVisit(t: SwitchStatement, extraData: MapData, visitorGenerator: VisitorGenerator<BytecodeClass>, additional: MVData) {
        val switchType = t.switchType
        var aSwitch: SwitchStatement = t

        if (switchType !== SwitchTypes.NUMERIC) {
            if (!switchType.isUnique) {
                aSwitch = this.insertEqualityCheck(aSwitch)
            }

            val generate = aSwitch.switchType.createGenerator().generate(aSwitch, this)

            visitorGenerator.generateTo(SwitchStatement::class.java, generate, extraData, additional)
        } else {
            val mv = additional.methodVisitor
            val value = aSwitch.value

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

            val flow = Flow(null, outsideStart, insideStart, defaultLabel, outsideEnd)

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

                    visitorGenerator.generateTo(CodeSource::class.java, aCase.body, extraData, additional)
                }
            }

            // /Generate Case code

            mv.visitLabel(defaultLabel)

            extraData.unregisterData(ConstantDatas.FLOW_TYPE_INFO, flow)

            val aDefault = this.getDefault(originCaseList)

            val codeSource = aDefault.body ?: CodeSource.empty()

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
                filledCases.add(EmptyCase(Literals.INT(i), CodeSource.empty()))
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
                .filterNot(Case::isDefault)
                .map { it.value as Literals.IntLiteral }
                .map { Integer.parseInt(it.name) }
                .min()
                ?: Integer.MAX_VALUE

        return last
    }

    private fun getMax(caseList: List<Case>): Int {
        val last = caseList
                .filterNot(Case::isDefault)
                .map { it.value as Literals.IntLiteral }
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

    private fun insertEqualityCheck(aSwitch: SwitchStatement): SwitchStatement {
        val switchValue = aSwitch.value

        if (switchValue.type.`is`(Types.INT))
            return aSwitch

        return aSwitch.builder().withCases(aSwitch.cases.map { aCase ->

            if (aCase.isDefault)
                return@map aCase

            val codeSource = aCase.body

            if (codeSource.isNotEmpty) {
                val caseValue = aCase.value!!

                val type = CodePartUtil.getType(caseValue)

                if (type.`is`(Types.INT))
                    return@map aCase

                return@map aCase.builder().withBody(CodeAPI.sourceOfParts(
                        SwitchIfStatement(codeSource,
                                CodeSource.empty(),
                                listOf(CodeAPI.checkTrue(
                                        CodeAPI.invokeVirtual(Any::class.java, switchValue, "equals",
                                                CodeAPI.typeSpec(Types.BOOLEAN, Types.OBJECT),
                                                listOf(CodeAPI.argument(caseValue))
                                        )
                                )))
                )).build()
            }

            return@map aCase
        }).build()
    }

    private fun getInt(aCase: Case): Int {
        if (aCase.isDefault)
            return -1

        return Integer.parseInt((aCase.value!! as Literals.IntLiteral).name)
    }

    @GenerateTo(IfStatement::class)
    internal class SwitchIfStatement(override val body: CodeSource, override val elseStatement: CodeSource, override val expressions: List<CodePart>) : IfStatement

    private class EmptyCase(override val value: Typed, override val body: CodeSource) : Case
}