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
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.literal.Literals
import com.github.jonathanxd.codeapi.operator.Operators
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes

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

            val lbl = if (ifStatement is SwitchVisitor.SwitchIfStatement) { // Workaround ?
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
