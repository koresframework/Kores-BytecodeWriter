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
import com.github.jonathanxd.codeapi.base.IfExpr
import com.github.jonathanxd.codeapi.base.IfGroup
import com.github.jonathanxd.codeapi.base.IfStatement
import com.github.jonathanxd.codeapi.base.Line
import com.github.jonathanxd.codeapi.bytecode.common.MethodVisitorHelper
import com.github.jonathanxd.codeapi.bytecode.processor.IN_EXPRESSION
import com.github.jonathanxd.codeapi.bytecode.processor.incrementInContext
import com.github.jonathanxd.codeapi.bytecode.util.IfUtil
import com.github.jonathanxd.codeapi.bytecode.util.OperatorUtil
import com.github.jonathanxd.codeapi.bytecode.util.booleanValue
import com.github.jonathanxd.codeapi.common.CodeNothing
import com.github.jonathanxd.codeapi.factory.cast
import com.github.jonathanxd.codeapi.literal.Literal
import com.github.jonathanxd.codeapi.literal.Literals
import com.github.jonathanxd.codeapi.operator.Operator
import com.github.jonathanxd.codeapi.operator.Operators
import com.github.jonathanxd.codeapi.processor.ProcessorManager
import com.github.jonathanxd.codeapi.processor.processAs
import com.github.jonathanxd.codeapi.util.`is`
import com.github.jonathanxd.codeapi.util.isPrimitive
import com.github.jonathanxd.codeapi.util.safeForComparison
import com.github.jonathanxd.codeapi.util.type
import com.github.jonathanxd.iutils.data.TypedData
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes

fun visit(expressions: List<CodeInstruction>,
          ifStart: Label,
          ifBody: Label,
          outOfIf: Label,
          isWhile: Boolean,
          data: TypedData,
          processorManager: ProcessorManager<*>,
          mvHelper: MethodVisitorHelper,
          nextIsOr: Boolean = false,
          nextBitwise: Operator? = null,
          lastBitwise: Operator? = null) {

    val visitor = mvHelper.methodVisitor

    var index = 0

    fun hasOr() =
            index + 1 < expressions.size
                    && expressions.slice((index + 1)..(expressions.size - 1))
                    .takeWhile { it.safeForComparison !is IfGroup }
                    .any { val safe = it.safeForComparison; safe is Operator && safe.name == Operators.OR.name }

    fun nextIsOr() =
            if (index + 1 < expressions.size)
                (expressions[index + 1]).let { val safe = it.safeForComparison; safe is Operator && safe.name == Operators.OR.name }
            else nextIsOr // fix for ifGroup

    fun nextBitwise() =
            if (index + 1 < expressions.size)
                (expressions[index + 1]).let {
                    val safe = it.safeForComparison
                    if (safe is Operator
                            && (safe.name == Operators.BITWISE_AND.name
                            || safe.name == Operators.BITWISE_EXCLUSIVE_OR.name
                            || safe.name == Operators.BITWISE_INCLUSIVE_OR.name))
                        safe
                    else null
                }
            else nextBitwise // fix for ifGroup

    fun lastBitwise() =
            if (index - 1 > -1)
                (expressions[index - 1]).let {
                    val safe = it.safeForComparison
                    if (safe is Operator
                            && (safe.name == Operators.BITWISE_AND.name
                            || safe.name == Operators.BITWISE_EXCLUSIVE_OR.name
                            || safe.name == Operators.BITWISE_INCLUSIVE_OR.name))
                        safe
                    else null
                }
            else lastBitwise // fix for ifGroup

    var orLabel: Label? = null

    while (index < expressions.size) {
        val expr = expressions[index]

        val inverse = !nextIsOr() || isWhile

        val jumpLabel = if (hasOr() && !nextIsOr()) {
            if (orLabel == null) {
                orLabel = Label()
            }
            orLabel
        } else if (isWhile) ifStart else if (inverse) outOfIf else ifBody

        if (expr is Line) {
            // Line require explicit visit here
            processorManager.processAs<Line>(Line.NormalLine(expr.line, CodeNothing), data)
        }

        val safeExpr = expr.safeForComparison

        if (safeExpr is IfExpr) {
            if (index - 1 > 0 && expressions[index - 1].let {
                val safe = it.safeForComparison; safe is Operator && safe.name == Operators.OR.name
            }) orLabel?.let { visitor.visitLabel(it) }

            val expr1 = safeExpr.expr1
            val operation = safeExpr.operation
            val expr2 = safeExpr.expr2

            val nextBitwiseVar = nextBitwise()
            val lastBitwiseVar = lastBitwise()
            val isBoolean = isBooleanTrue(expr1, operation, expr2)

            if ((nextBitwiseVar != null || lastBitwiseVar != null)
                    && !isBoolean) {
                // Bit of desugar for BitWise
                val stm = IfStatement(listOf(IfExpr(expr1, operation, expr2)),
                        CodeSource.fromVarArgs(Literals.INT(1)),
                        CodeSource.fromVarArgs(Literals.INT(0)))
                IN_EXPRESSION.incrementInContext(data) {
                    processorManager.process(stm, data)
                }
            } else {
                genBranch(expr1, expr2, operation, jumpLabel, inverse, data, processorManager, mvHelper,
                        lastBitwiseVar == null && nextBitwise() == null)
            }

            // Bitwise section

            if (lastBitwiseVar != null) {
                val type = expr1.safeForComparison.type
                OperateProcessor.operateVisit(type, lastBitwiseVar, false, mvHelper)
                mvHelper.methodVisitor.visitJumpInsn(Opcodes.IFEQ, jumpLabel)
            }

            // /Bitwise section
        }

        if (safeExpr is IfGroup) {
            visit(safeExpr.expressions, ifStart, ifBody, outOfIf, isWhile, data, processorManager, mvHelper,
                    nextIsOr(),
                    nextBitwise(),
                    lastBitwise())
        }

        ++index
    }

}

fun genBranch(expr1_: CodeInstruction, expr2_: CodeInstruction, operation: Operator.Conditional,
              target: Label, inverse: Boolean, data: TypedData, processorManager: ProcessorManager<*>,
              mvHelper: MethodVisitorHelper,
              shouldJump: Boolean) {

    var expr1 = expr1_
    var expr2 = expr2_
    var safeExpr1 = expr1.safeForComparison
    var safeExpr2 = expr2.safeForComparison

    val isBoolean = { it: CodeInstruction -> it.isPrimitive && it is Literal && it.type.`is`(Types.BOOLEAN) }

    if (isBoolean(safeExpr1) || isBoolean(safeExpr2)) {
        val operatorIsEq = operation === Operators.EQUAL_TO
        val value = if (isBoolean(safeExpr1)) safeExpr1.booleanValue else safeExpr2.booleanValue
        var opcode = IfUtil.getIfNeEqOpcode(value)

        if (!operatorIsEq)
            opcode = IfUtil.invertIfNeEqOpcode(opcode)

        if (inverse)
            opcode = IfUtil.invertIfNeEqOpcode(opcode)

        if (isBoolean(safeExpr1)) {
            IN_EXPRESSION.incrementInContext(data) {
                processorManager.process(expr2::class.java, expr2, data)
            }
            if (shouldJump) mvHelper.methodVisitor.visitJumpInsn(opcode, target)
        } else {
            IN_EXPRESSION.incrementInContext(data) {
                processorManager.process(expr1::class.java, expr1, data)
            }
            if (shouldJump) mvHelper.methodVisitor.visitJumpInsn(opcode, target)
        }

    } else {
        if (safeExpr1.isPrimitive != safeExpr2.isPrimitive) {

            if (safeExpr2.isPrimitive) {
                expr1 = cast(safeExpr1.type, safeExpr2.type, expr1)
                safeExpr1 = expr1.safeForComparison
            } else {
                expr2 = cast(safeExpr2.type, safeExpr1.type, expr2)
                safeExpr2 = expr2.safeForComparison
            }
        }

        IN_EXPRESSION.incrementInContext(data) {
            processorManager.process(expr1::class.java, expr1, data)
        }

        if (safeExpr2 === Literals.NULL) {
            if (shouldJump) mvHelper.methodVisitor.visitJumpInsn(OperatorUtil.nullCheckToAsm(operation, inverse), target)
        } else if (safeExpr1.isPrimitive && safeExpr2.isPrimitive) {
            IN_EXPRESSION.incrementInContext(data) {
                processorManager.process(expr2::class.java, expr2, data)
            }

            val firstType = safeExpr1.type
            val secondType = safeExpr2.type

            if (!firstType.`is`(secondType))
                throw IllegalArgumentException("'$expr1' and '$expr2' have different types, cast it to correct type.")

            var generateCMPCheck = false

            if (safeExpr1.type.`is`(Types.LONG)) {
                if (shouldJump) mvHelper.methodVisitor.visitInsn(Opcodes.LCMP)
                generateCMPCheck = true
            } else if (safeExpr1.type.`is`(Types.DOUBLE)) {
                if (shouldJump) mvHelper.methodVisitor.visitInsn(Opcodes.DCMPG)
                generateCMPCheck = true
            } else if (safeExpr1.type.`is`(Types.FLOAT)) {
                if (shouldJump) mvHelper.methodVisitor.visitInsn(Opcodes.FCMPG)
                generateCMPCheck = true
            }

            var check = OperatorUtil.primitiveToAsm(operation, inverse)

            if (generateCMPCheck) {
                check = OperatorUtil.convertToSimpleIf(check)
            }

            if (shouldJump) mvHelper.methodVisitor.visitJumpInsn(check, target)
        } else {
            IN_EXPRESSION.incrementInContext(data) {
                processorManager.process(expr2::class.java, expr2, data)
            }

            if (shouldJump) mvHelper.methodVisitor.visitJumpInsn(OperatorUtil.referenceToAsm(operation, inverse), target)
        }
    }
}

fun CodeInstruction.isBoolean(): Boolean =
        this.safeForComparison.let {
            it.isPrimitive && it is Literal && it.type.`is`(Types.BOOLEAN)
        }

fun isBooleanTrue(expr1_: CodeInstruction, operator: Operator, expr2_: CodeInstruction): Boolean {
    val expr1Boolean = expr1_.isBoolean()
    val expr2Boolean = expr2_.isBoolean()

    if (expr1Boolean || expr2Boolean) {
        val value = if (expr1Boolean) expr1_.booleanValue else expr2_.booleanValue
        return (operator == Operators.EQUAL_TO && value)
                || (operator == Operators.NOT_EQUAL_TO && !value)
    }

    return false
}