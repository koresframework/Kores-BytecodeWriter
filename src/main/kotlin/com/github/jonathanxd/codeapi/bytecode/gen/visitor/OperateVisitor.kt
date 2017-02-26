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

import com.github.jonathanxd.codeapi.base.Operate
import com.github.jonathanxd.codeapi.bytecode.BytecodeClass
import com.github.jonathanxd.codeapi.bytecode.common.MVData
import com.github.jonathanxd.codeapi.bytecode.util.CodePartUtil
import com.github.jonathanxd.codeapi.common.Data
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.gen.visit.VoidVisitor
import com.github.jonathanxd.codeapi.helper.OperateHelper
import com.github.jonathanxd.codeapi.literal.Literals
import com.github.jonathanxd.codeapi.operator.Operator
import com.github.jonathanxd.codeapi.operator.Operators
import com.github.jonathanxd.codeapi.type.CodeType
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type

object OperateVisitor : VoidVisitor<Operate, BytecodeClass, MVData> {

    override fun voidVisit(t: Operate, extraData: Data, visitorGenerator: VisitorGenerator<BytecodeClass>, additional: MVData) {
        val target = t.target

        val operation = t.operation

        val value = t.value

        if (operation === Operators.UNARY_BITWISE_COMPLEMENT) { // ~
            value ?: throw IllegalArgumentException("Value cannot be null if operation is '$operation'!")
            // Desugar
            val desugar = OperateHelper.builder(target)
                    .subtract(
                            OperateHelper.builder(value)
                                    .xor(Literals.INT(-1))
                                    .build()
                    )
                    .build()

            visitorGenerator.generateTo(desugar::class.java, desugar, extraData, additional)

            return
        }

        visitorGenerator.generateTo(target::class.java, target, extraData, additional)

        if (value != null) {
            visitorGenerator.generateTo(value::class.java, value, extraData, additional)
        }

        when (operation) {
            Operators.ADD, Operators.SUBTRACT, Operators.MULTIPLY, Operators.DIVISION, Operators.REMAINDER,
            Operators.SIGNED_LEFT_SHIFT, // <<
            Operators.SIGNED_RIGHT_SHIFT, // >>
            Operators.UNSIGNED_RIGHT_SHIFT, // >>>
            Operators.BITWISE_EXCLUSIVE_OR, // ^
            Operators.BITWISE_INCLUSIVE_OR, // |
            Operators.BITWISE_AND // &
            -> {
                val type = CodePartUtil.getType(target)

                operateVisit(type, operation, value == null, additional)
            }
            else -> throw RuntimeException("Cannot handle operation: '$operation'!")
        }

    }

    internal fun operateVisit(codeType: CodeType, operation: Operator, valueIsNull: Boolean, mvData: MVData) {
        val type = Type.getType(codeType.javaSpecName)

        val opcode = if (operation === Operators.ADD) {
            type.getOpcode(Opcodes.IADD)
        } else if (operation === Operators.SUBTRACT) {
            if (!valueIsNull) {
                type.getOpcode(Opcodes.ISUB)
            } else {
                type.getOpcode(Opcodes.INEG)
            }
        } else if (operation === Operators.MULTIPLY) {
            type.getOpcode(Opcodes.IMUL)
        } else if (operation === Operators.DIVISION) {
            type.getOpcode(Opcodes.IDIV)
        } else if (operation === Operators.REMAINDER) {
            type.getOpcode(Opcodes.IREM)
        } else if (operation === Operators.BITWISE_AND) {
            type.getOpcode(Opcodes.IAND)
        } else if (operation === Operators.BITWISE_INCLUSIVE_OR) {
            type.getOpcode(Opcodes.IOR)
        } else if (operation === Operators.BITWISE_EXCLUSIVE_OR) {
            type.getOpcode(Opcodes.IXOR)
        } else if (operation === Operators.UNARY_BITWISE_COMPLEMENT) { // desugar
            throw IllegalArgumentException("Invalid operator: '$operation'!!!")
        } else -1

        if (opcode != -1)
            mvData.methodVisitor.visitInsn(opcode)


    }
}