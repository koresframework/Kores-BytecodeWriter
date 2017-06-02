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

import com.github.jonathanxd.codeapi.base.Operate
import com.github.jonathanxd.codeapi.bytecode.common.MethodVisitorHelper
import com.github.jonathanxd.codeapi.bytecode.processor.METHOD_VISITOR
import com.github.jonathanxd.codeapi.bytecode.util.ReflectType
import com.github.jonathanxd.codeapi.common.CodeNothing
import com.github.jonathanxd.codeapi.helper.OperateHelper
import com.github.jonathanxd.codeapi.literal.Literals
import com.github.jonathanxd.codeapi.operator.Operator
import com.github.jonathanxd.codeapi.operator.Operators
import com.github.jonathanxd.codeapi.processor.CodeProcessor
import com.github.jonathanxd.codeapi.processor.Processor
import com.github.jonathanxd.codeapi.util.javaSpecName
import com.github.jonathanxd.codeapi.util.require
import com.github.jonathanxd.codeapi.util.safeForComparison
import com.github.jonathanxd.codeapi.util.type
import com.github.jonathanxd.iutils.data.TypedData
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type

object OperateProcessor : Processor<Operate> {

    override fun process(part: Operate, data: TypedData, codeProcessor: CodeProcessor<*>) {
        val target = part.target

        val operation = part.operation

        val value = part.value
        val safeValue = value.safeForComparison

        if (operation === Operators.UNARY_BITWISE_COMPLEMENT) {
            // ~
            if (safeValue == CodeNothing)
                throw IllegalArgumentException("Value cannot be null if operation is '$operation'!")

            // Desugar
            val desugar = OperateHelper.builder(target)
                    .subtract(
                            OperateHelper.builder(value)
                                    .xor(Literals.INT(-1))
                                    .build()
                    )
                    .build()

            codeProcessor.process(desugar::class.java, desugar, data)

            return
        }

        codeProcessor.process(target::class.java, target, data)

        if (safeValue != CodeNothing) {
            codeProcessor.process(value::class.java, value, data)
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
                val type = target.type

                operateVisit(type, operation, safeValue == CodeNothing, METHOD_VISITOR.require(data))
            }
            else -> throw RuntimeException("Cannot handle operation: '$operation'!")
        }
    }

    internal fun operateVisit(codeType: ReflectType, operation: Operator, valueIsNull: Boolean, mvData: MethodVisitorHelper) {
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
        } else if (operation === Operators.UNARY_BITWISE_COMPLEMENT) {
            // desugar
            throw IllegalArgumentException("Invalid operator: '$operation'!!!")
        } else -1

        if (opcode != -1)
            mvData.methodVisitor.visitInsn(opcode)


    }
}