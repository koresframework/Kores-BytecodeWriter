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
package com.github.jonathanxd.codeapi.bytecode.processor.processors

import com.github.jonathanxd.codeapi.CodeInstruction
import com.github.jonathanxd.codeapi.CodeSource
import com.github.jonathanxd.codeapi.Types
import com.github.jonathanxd.codeapi.base.*
import com.github.jonathanxd.codeapi.bytecode.common.Flow
import com.github.jonathanxd.codeapi.bytecode.processor.FLOWS
import com.github.jonathanxd.codeapi.bytecode.processor.IN_EXPRESSION
import com.github.jonathanxd.codeapi.bytecode.processor.METHOD_VISITOR
import com.github.jonathanxd.codeapi.bytecode.processor.incrementInContext
import com.github.jonathanxd.codeapi.bytecode.util.ReflectType
import com.github.jonathanxd.codeapi.bytecode.util.SwitchOnEnum
import com.github.jonathanxd.codeapi.common.MethodTypeSpec
import com.github.jonathanxd.codeapi.factory.checkTrue
import com.github.jonathanxd.codeapi.factory.defaultCase
import com.github.jonathanxd.codeapi.factory.invokeVirtual
import com.github.jonathanxd.codeapi.factory.typeSpec
import com.github.jonathanxd.codeapi.literal.Literals
import com.github.jonathanxd.codeapi.processor.Processor
import com.github.jonathanxd.codeapi.processor.ProcessorManager
import com.github.jonathanxd.codeapi.util.*
import com.github.jonathanxd.iutils.data.TypedData
import org.objectweb.asm.Label

object SwitchProcessor : Processor<SwitchStatement> {

    override fun process(part: SwitchStatement, data: TypedData, processorManager: ProcessorManager<*>) {
        val switchType = part.switchType
        var aSwitch: SwitchStatement = part
        val mvHelper = METHOD_VISITOR.require(data)

        if (switchType !== SwitchType.NUMERIC) {

            if (switchType === SwitchType.ENUM) {
                val newSwitch = SwitchOnEnum.mappings(part, data)

                processorManager.process(SwitchStatement::class.java, newSwitch, data)
            } else {
                aSwitch = this.insertEqualityCheck(aSwitch)

                // Special handling
                val generate = when (switchType) {
                    SwitchType.STRING -> ObjectSwitchGenerator.generate(aSwitch)
                    SwitchType.ENUM -> EnumSwitchGenerator.generate(aSwitch)
                    else -> NumericSwitchGenerator.generate(aSwitch)
                }

                processorManager.process(SwitchStatement::class.java, generate, data)
            }
        } else {
            val mv = mvHelper.methodVisitor
            val value = aSwitch.value

            IN_EXPRESSION.incrementInContext(data) {
                processorManager.process(value::class.java, value, data)
            }

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

            FLOWS.add(data, flow)

            mv.visitLabel(outsideStart)

            val labels: Array<Label>

            if (!useLookupSwitch) {
                var min = this.getMin(filteredCaseList)
                var max = this.getMax(filteredCaseList)

                if (min < Integer.MIN_VALUE || min > Integer.MAX_VALUE
                        || max < Integer.MIN_VALUE || max > Integer.MAX_VALUE)
                    throw IllegalStateException("Too much table entries to generate: ${min..max}")

                filteredCaseList = this.fill(min.toInt(), max.toInt(), filteredCaseList)

                labels = this.toLabel(filteredCaseList, defaultLabel)

                min = this.getMin(filteredCaseList)
                max = this.getMax(filteredCaseList)

                if (min < Integer.MIN_VALUE || min > Integer.MAX_VALUE
                        || max < Integer.MIN_VALUE || max > Integer.MAX_VALUE)
                    throw IllegalStateException("Too much table entries to generate: ${min..max}")

                mv.visitTableSwitchInsn(min.toInt(), max.toInt(), defaultLabel, *labels)

            } else {
                labels = this.toLabel(filteredCaseList, defaultLabel)

                val keys = filteredCaseList.map(this::getInt).toIntArray()

                mv.visitLookupSwitchInsn(defaultLabel, keys, labels)
            }

            mv.visitLabel(insideStart)

            // Generate Case code

            labels.forEachIndexed { i, label ->
                val aCase = filteredCaseList[i]

                if (!aCase.body.contains(SwitchMarker)) {
                    mv.visitLabel(label)

                    processorManager.process(CodeSource::class.java, aCase.body, data)
                }
            }

            // /Generate Case code

            mv.visitLabel(defaultLabel)

            FLOWS.require(data).remove(flow)


            val aDefault = this.getDefault(originCaseList)

            val codeSource = aDefault.body

            processorManager.process(CodeSource::class.java, codeSource, data)


            mv.visitLabel(outsideEnd)
        }
    }


    fun toLabel(caseList: List<Case>, defaultLabel: Label): Array<Label> {
        return caseList.map { if (it.body.contains(SwitchMarker)) defaultLabel else Label() }.toTypedArray()
    }

    fun getDefault(caseList: List<Case>): Case {
        return caseList.firstOrNull(Case::isDefault) ?: defaultCase(CodeSource.empty())
    }

    /**
     * Fill the case list.
     */
    private fun fill(min: Int, max: Int, caseList: List<Case>): List<Case> {

        val filledCases = mutableListOf<Case>()

        for (i in min..max + 1 - 1) {
            val aCase = this.getCase(caseList, i)

            if (aCase != null) {
                filledCases.add(aCase)
            } else {
                filledCases.add(Case(Literals.INT(i), CodeSource.fromPart(SwitchMarker)))
            }
        }

        return filledCases
    }

    private fun getCase(caseList: List<Case>, i: Int): Case? =
            caseList.firstOrNull { this.getInt(it) == i }


    private fun getMin(caseList: List<Case>): Long =
            caseList
                    .filterNot(Case::isDefault)
                    .map { it.value.safeForComparison as Literals.IntLiteral }
                    .map { it.name.toLong() }
                    .min() ?: 0L


    private fun getMax(caseList: List<Case>): Long =
            caseList
                    .filterNot(Case::isDefault)
                    .map { it.value.safeForComparison as Literals.IntLiteral }
                    .map { it.name.toLong() }
                    .max() ?: 0L

    private fun getCasesToFill(caseList: List<Case>): Long {
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

        val tableSpaceCost = (4 * casesToFill)
        val tableTimeCost = 3
        val lookupSpaceCost = (3 + 2 * labels).toLong()

        // If lookup cost < table cost.
        return lookupSpaceCost + 3 * labels < tableSpaceCost + 3 * tableTimeCost
    }

    private fun insertEqualityCheck(aSwitch: SwitchStatement): SwitchStatement {
        val switchValue = aSwitch.value

        if (switchValue.type.`is`(Types.INT))
            return aSwitch

        return aSwitch.builder().cases(aSwitch.cases.map { aCase ->

            if (aCase.isDefault)
                return@map aCase

            val codeSource = aCase.body

            if (codeSource.isNotEmpty) {
                val caseValue = aCase.value

                val type = caseValue.type

                if (type.`is`(Types.INT))
                    return@map aCase

                return@map aCase.builder().body(CodeSource.fromPart(
                        IfStatement(body = codeSource + SwitchMarker,
                                elseStatement = CodeSource.empty(),
                                expressions = listOf(checkTrue(
                                        invokeVirtual(Any::class.java, switchValue, "equals",
                                                typeSpec(Types.BOOLEAN, Types.OBJECT),
                                                listOf(caseValue)
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

        return Integer.parseInt((aCase.value.safeForComparison as Literals.IntLiteral).name)
    }


    object SwitchMarker : CodeInstruction

    object SwitchMarkerProcessor : Processor<SwitchMarker> {
        override fun process(part: SwitchMarker, data: TypedData, processorManager: ProcessorManager<*>) {
        }

    }


    private object NumericSwitchGenerator : SwitchGenerator() {

        private fun autoUnboxing(part: CodeInstruction, type: ReflectType): CodeInstruction {
            if (!type.isPrimitive) {
                return Cast(type, Types.INT, part)
            }

            return part
        }

        private fun isAcceptable(type: ReflectType?): Boolean {
            return type != null && (type.`is`(Types.CHAR)
                    || type.`is`(Types.BYTE)
                    || type.`is`(Types.SHORT)
                    || type.`is`(Types.INT)
                    || !type.isPrimitive && isAcceptable(type.primitiveType))
        }

        override fun generate(t: SwitchStatement): SwitchStatement {
            val part = t.value
            val type = t.value.type

            if (isAcceptable(type)) {
                return t.builder().value(autoUnboxing(part, type)).build()
            }


            throw IllegalArgumentException("Cannot switch part '${t.value}' of type: '$type'. The part is not numeric.")
        }
    }

    private object ObjectSwitchGenerator : SwitchGenerator() {

        override fun translateSwitch(aSwitch: SwitchStatement): SwitchStatement {
            return this.translate(aSwitch)
        }

        override fun translateCase(aCase: Case, aSwitch: SwitchStatement): Case {

            if (aCase.isDefault)
                return aCase

            if (aCase.type.`is`(Types.INT))
                return aCase

            return aCase.builder().value(Literals.INT(EnumTypeUtil.resolve(aCase.value.safeForComparison))).build()
        }


        private fun translate(aSwitch: SwitchStatement): SwitchStatement {
            return aSwitch.builder().value(
                    MethodInvocation(
                            invokeType = InvokeType.INVOKE_VIRTUAL,
                            target = aSwitch.value,
                            arguments = emptyList(),
                            spec = MethodTypeSpec(
                                    localization = Types.OBJECT,
                                    methodName = "hashCode",
                                    typeSpec = TypeSpec(Types.INT)
                            )

                    )
            ).build()
        }
    }

    private object EnumSwitchGenerator : SwitchGenerator() {

        override fun translateSwitch(aSwitch: SwitchStatement): SwitchStatement {
            return this.translate(aSwitch)
        }

        override fun translateCase(aCase: Case, aSwitch: SwitchStatement): Case {
            if (aCase.isDefault)
                return aCase

            return aCase.builder().value(Literals.INT(EnumTypeUtil.resolve(aCase.value.safeForComparison))).build()
        }

        private fun translate(aSwitch: SwitchStatement): SwitchStatement {
            return aSwitch.builder()
                    .value(
                            MethodInvocation(
                                    invokeType = InvokeType.INVOKE_VIRTUAL,
                                    target = aSwitch.value,
                                    arguments = emptyList(),
                                    spec = MethodTypeSpec(
                                            localization = Types.ENUM,
                                            methodName = "ordinal",
                                            typeSpec = TypeSpec(Types.INT)
                                    )

                            )

                    )
                    .build()
        }

    }


    abstract class SwitchGenerator {
        open fun translateSwitch(aSwitch: SwitchStatement): SwitchStatement {
            return aSwitch
        }

        open fun translateCase(aCase: Case, aSwitch: SwitchStatement): Case {
            return aCase
        }

        open fun generate(t: SwitchStatement): SwitchStatement {
            val caseList = t.cases.map { aCase -> if (aCase.isDefault) aCase else this.checkType(this.translateCase(aCase, t)) }

            val translatedSwitch = this.checkType(this.translateSwitch(t))

            val switchStmt = SwitchStatement(
                    value = translatedSwitch.value,
                    cases = caseList,
                    switchType = SwitchType.NUMERIC
            )

            return switchStmt
        }

        private fun <R : Typed> checkType(typed: R): R {
            if (!typed.type.`is`(Types.INT)) {
                throw IllegalArgumentException("Translated switch is not a numeric switch!")
            }

            return typed
        }
    }
}

