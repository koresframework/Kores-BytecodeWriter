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
import com.github.jonathanxd.codeapi.base.IfExpr
import com.github.jonathanxd.codeapi.base.IfStatement
import com.github.jonathanxd.codeapi.bytecode.BytecodeClass
import com.github.jonathanxd.codeapi.bytecode.common.Flow
import com.github.jonathanxd.codeapi.bytecode.common.MVData
import com.github.jonathanxd.codeapi.bytecode.util.CodePartUtil
import com.github.jonathanxd.codeapi.bytecode.util.IfUtil
import com.github.jonathanxd.codeapi.bytecode.util.OperatorUtil
import com.github.jonathanxd.codeapi.common.Data
import com.github.jonathanxd.codeapi.common.IfGroup
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.literal.Literals
import com.github.jonathanxd.codeapi.operator.Operator
import com.github.jonathanxd.codeapi.operator.Operators
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes

fun visit(expressions: List<CodePart>,
          ifStart: Label,
          ifBody: Label,
          outOfIf: Label,
          isWhile: Boolean,
          extraData: Data,
          visitorGenerator: VisitorGenerator<BytecodeClass>,
          mvData: MVData,
          nextIsOr: Boolean = false) {

    val visitor = mvData.methodVisitor

    var index = 0

    fun hasOr() =
            index + 1 < expressions.size
                    && expressions.slice((index + 1)..(expressions.size - 1)).takeWhile { it !is IfGroup }.any { it is Operator && it.name == Operators.OR.name }

    fun nextIsOr() =
            if (index + 1 < expressions.size)
                (expressions[index + 1]).let { it is Operator && it.name == Operators.OR.name }
            else nextIsOr // fix for ifGroup

    var orLabel: Label? = null

    while (index < expressions.size) {
        val expr = expressions[index]

        val inverse = !nextIsOr() || isWhile

        val jumpLabel = if (hasOr() && !nextIsOr()) {
            if (orLabel == null) {
                orLabel = Label()
            }
            orLabel
        } else if (isWhile) ifStart else if(inverse) outOfIf else ifBody

        if (expr is IfExpr) {
            if(index - 1 > 0 && expressions[index - 1].let { it is Operator && it.name == Operators.OR.name } )
                orLabel?.let { visitor.visitLabel(it) }

            val expr1 = expr.expr1
            val operation = expr.operation
            val expr2 = expr.expr2

            genBranch(expr1, expr2, operation, jumpLabel, inverse, extraData, visitorGenerator, mvData)
        }

        if (expr is IfGroup) {
            visit(expr.expressions, ifStart, ifBody, outOfIf, isWhile, extraData, visitorGenerator, mvData, nextIsOr())
        }

        ++index
    }

}

fun genBranch(expr1_: CodePart, expr2_: CodePart, operation: Operator.Conditional, target: Label, inverse: Boolean, extraData: Data, visitorGenerator: VisitorGenerator<BytecodeClass>, mvData: MVData) {

    var expr1 = expr1_
    var expr2 = expr2_

    val expr1Type = CodePartUtil.getType(expr1)
    val expr2Type = CodePartUtil.getType(expr2)

    val expr1Primitive = CodePartUtil.isPrimitive(expr1)
    val expr2Primitive = CodePartUtil.isPrimitive(expr2)

    val firstIsBoolean = expr1Primitive && CodePartUtil.isBoolean(expr1)
    val secondIsBoolean = expr2Primitive && CodePartUtil.isBoolean(expr2)

    if (firstIsBoolean || secondIsBoolean) {
        val operatorIsEq = operation === Operators.EQUAL_TO
        val value = if (firstIsBoolean) CodePartUtil.getBooleanValue(expr1) else CodePartUtil.getBooleanValue(expr2)
        var opcode = IfUtil.getIfNeEqOpcode(value)

        if (!operatorIsEq)
            opcode = IfUtil.invertIfNeEqOpcode(opcode)

        if (inverse)
            opcode = IfUtil.invertIfNeEqOpcode(opcode)

        if (firstIsBoolean) {
            visitorGenerator.generateTo(expr2::class.java, expr2, extraData, null, mvData)
            mvData.methodVisitor.visitJumpInsn(opcode, target)
        } else {
            visitorGenerator.generateTo(expr1::class.java, expr1, extraData, null, mvData)
            mvData.methodVisitor.visitJumpInsn(opcode, target)
        }

    } else {
        // Old Code ->
        // TODO: Rewrite

        if (expr1Primitive != expr2Primitive) {

            if (expr2Primitive) {
                expr1 = CodeAPI.cast(expr1Type, expr2Type, expr1)
            } else {
                expr2 = CodeAPI.cast(expr2Type, expr1Type, expr2)
            }
        }

        visitorGenerator.generateTo(expr1::class.java, expr1, extraData, null, mvData)

        if (expr2 === Literals.NULL) {
            mvData.methodVisitor.visitJumpInsn(OperatorUtil.nullCheckToAsm(operation, inverse), target)
        } else if (CodePartUtil.isPrimitive(expr1) && CodePartUtil.isPrimitive(expr2)) {
            visitorGenerator.generateTo(expr2::class.java, expr2, extraData, null, mvData)

            val firstType = CodePartUtil.getType(expr1)
            val secondType = CodePartUtil.getType(expr2)

            if (!firstType.`is`(secondType))
                throw IllegalArgumentException("'$expr1' and '$expr2' have different types, cast it to correct type.")

            var generateCMPCheck = false

            if (expr1Type.`is`(Types.LONG)) {
                mvData.methodVisitor.visitInsn(Opcodes.LCMP)
                generateCMPCheck = true
            } else if (expr1Type.`is`(Types.DOUBLE)) {
                mvData.methodVisitor.visitInsn(Opcodes.DCMPG)
                generateCMPCheck = true
            } else if (expr1Type.`is`(Types.FLOAT)) {
                mvData.methodVisitor.visitInsn(Opcodes.FCMPG)
                generateCMPCheck = true
            }

            var check = OperatorUtil.primitiveToAsm(operation, inverse)

            if (generateCMPCheck) {
                check = OperatorUtil.convertToSimpleIf(check)
            }

            mvData.methodVisitor.visitJumpInsn(check, target)
        } else {
            visitorGenerator.generateTo(expr2::class.java, expr2, extraData, null, mvData)

            mvData.methodVisitor.visitJumpInsn(OperatorUtil.referenceToAsm(operation, inverse), target)
        }
    }
}

@Deprecated(
        message = "Too much bugs, don't supports IfGroup, generate incorrect if bytecode, the source of the devil... Deprecated since: 3.2. Will be removed in 4.0",
        level = DeprecationLevel.ERROR,
        replaceWith = ReplaceWith("visit(ifStatement.expressions, ifStartLabel, ifBody, outOfIfLabel, jumpToStart, extraData, visitorGenerator, mvData)")
)
fun visit(ifStatement: IfStatement,
          ifStartLabel: Label,
          outOfIfLabel: Label,
          revert: Boolean,
          jumpToStart: Boolean,
          extraData: Data,
          visitorGenerator: VisitorGenerator<BytecodeClass>,
          mvData: MVData) {

    val visitor = mvData.methodVisitor

    val ifExpression = ArrayList(ifStatement.expressions)

    val listIterator = ifExpression.listIterator()

    val elseBody = ifStatement.elseStatement

    val inIfLabel = Label()

    val elseLabel = if (!elseBody.isEmpty) Label() else null

    while (listIterator.hasNext()) {
        val current = listIterator.next()
        var next: CodePart? = null

        if (listIterator.hasNext()) {
            next = listIterator.next()
            listIterator.previous()
        }

        if (current is IfExpr) {
            val isInverse = !revert == (next == null || next !== Operators.OR)

            val lbl = if (ifStatement is SwitchVisitor.SwitchIfStatement) {
                // Workaround ?
                extraData.getRequired<Flow>(ConstantDatas.FLOW_TYPE_INFO).insideEnd
            } else {
                if (jumpToStart) ifStartLabel else if (!isInverse) inIfLabel else elseLabel ?: outOfIfLabel // Jump to else if exists
            }

            val operation = current.operation

            var expr1 = current.expr1
            var expr2 = current.expr2

            val expr1Type = CodePartUtil.getType(expr1)
            val expr2Type = CodePartUtil.getType(expr2)

            val expr1Primitive = CodePartUtil.isPrimitive(expr1)
            val expr2Primitive = CodePartUtil.isPrimitive(expr2)

            val firstIsBoolean = expr1Primitive && CodePartUtil.isBoolean(expr1)
            val secondIsBoolean = expr2Primitive && CodePartUtil.isBoolean(expr2)

            if (firstIsBoolean || secondIsBoolean) {
                val operatorIsEq = operation === Operators.EQUAL_TO
                val value = if (firstIsBoolean) CodePartUtil.getBooleanValue(expr1) else CodePartUtil.getBooleanValue(expr2)
                var opcode = IfUtil.getIfNeEqOpcode(value)

                if (!operatorIsEq)
                    opcode = IfUtil.invertIfNeEqOpcode(opcode)

                if (isInverse)
                    opcode = IfUtil.invertIfNeEqOpcode(opcode)

                if (firstIsBoolean) {
                    visitorGenerator.generateTo(expr2::class.java, expr2, extraData, null, mvData)
                    visitor.visitJumpInsn(opcode, lbl)
                } else {
                    visitorGenerator.generateTo(expr1::class.java, expr1, extraData, null, mvData)
                    visitor.visitJumpInsn(opcode, lbl)
                }

            } else {
                // Old Code ->
                // TODO: Rewrite

                if (expr1Primitive != expr2Primitive) {

                    if (expr2Primitive) {
                        expr1 = CodeAPI.cast(expr1Type, expr2Type, expr1)
                    } else {
                        expr2 = CodeAPI.cast(expr2Type, expr1Type, expr2)
                    }
                }

                visitorGenerator.generateTo(expr1::class.java, expr1, extraData, null, mvData)

                if (expr2 === Literals.NULL) {
                    visitor.visitJumpInsn(OperatorUtil.nullCheckToAsm(operation, isInverse), lbl)
                } else if (CodePartUtil.isPrimitive(expr1) && CodePartUtil.isPrimitive(expr2)) {
                    visitorGenerator.generateTo(expr2::class.java, expr2, extraData, null, mvData)

                    val firstType = CodePartUtil.getType(expr1)
                    val secondType = CodePartUtil.getType(expr2)

                    if (!firstType.`is`(secondType))
                        throw IllegalArgumentException("'$expr1' and '$expr2' have different types, cast it to correct type.")

                    var generateCMPCheck = false

                    if (expr1Type.`is`(Types.LONG)) {
                        visitor.visitInsn(Opcodes.LCMP)
                        generateCMPCheck = true
                    } else if (expr1Type.`is`(Types.DOUBLE)) {
                        visitor.visitInsn(Opcodes.DCMPG)
                        generateCMPCheck = true
                    } else if (expr1Type.`is`(Types.FLOAT)) {
                        visitor.visitInsn(Opcodes.FCMPG)
                        generateCMPCheck = true
                    }

                    var check = OperatorUtil.primitiveToAsm(operation, isInverse)

                    if (generateCMPCheck) {
                        check = OperatorUtil.convertToSimpleIf(check)
                    }

                    visitor.visitJumpInsn(check, lbl)
                } else {
                    visitorGenerator.generateTo(expr2::class.java, expr2, extraData, null, mvData)

                    visitor.visitJumpInsn(OperatorUtil.referenceToAsm(operation, isInverse), lbl)
                }
            }

        }

    }

    visitor.visitLabel(inIfLabel)

    val body = ifStatement.body

    mvData.enterNewFrame()

    visitorGenerator.generateTo(CodeSource::class.java, body, extraData, null, mvData)

    mvData.exitFrame()

    if (elseLabel != null) {
        visitor.visitJumpInsn(Opcodes.GOTO, outOfIfLabel)
    }


    if (elseLabel != null) {
        visitor.visitLabel(elseLabel)

        if (!elseBody.isEmpty) {
            mvData.enterNewFrame()

            visitorGenerator.generateTo(CodeSource::class.java, elseBody, extraData, null, mvData)

            mvData.exitFrame()
        }
    }

}
