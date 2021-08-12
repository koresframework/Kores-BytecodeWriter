/*
 *      Kores-BytecodeWriter - Translates Kores Structure to JVM Bytecode <https://github.com/JonathanxD/CodeAPI-BytecodeWriter>
 *
 *         The MIT License (MIT)
 *
 *      Copyright (c) 2018 TheRealBuggy/JonathanxD (https://github.com/JonathanxD/) <jonathan.scripter@programmer.net>
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
package com.github.jonathanxd.kores.bytecode.processor.processors

import com.github.jonathanxd.kores.Instruction
import com.github.jonathanxd.kores.base.Line
import com.github.jonathanxd.kores.base.Operate
import com.github.jonathanxd.kores.base.VariableAccess
import com.github.jonathanxd.kores.base.VariableDefinition
import com.github.jonathanxd.kores.bytecode.common.MethodVisitorHelper
import com.github.jonathanxd.kores.literal.Literal
import com.github.jonathanxd.kores.operator.Operators
import com.github.jonathanxd.kores.type.`is`
import com.github.jonathanxd.kores.type.javaSpecName
import org.objectweb.asm.Label

object VariableOperateProcessor {

    /**
     * Improve the operation and assign of a variable, returns true if this class improved
     * the operation, false otherwise.
     */
    fun visit(
        t: VariableDefinition,
        operate: Operate,
        part: Instruction,
        varPos: Int,
        mvHelper: MethodVisitorHelper
    ): Boolean {
        val mv = mvHelper.methodVisitor

        val target = operate.target

        require(
            target is VariableAccess
                    && target.name == t.name
                    && target.variableType.`is`(t.type),
            { "The operate target must be variable access of variable definition '$t'!" }
        )

        val operation = operate.operation

        val value = operate.value

        var constantVal = true

        var constant = 1

        if (value !is Literal || value.type.javaSpecName != "I") {
            constantVal = false
        } else {
            constant = Integer.valueOf(value.name)!!
        }

        val isIncrementOne = constant == 1

        fun visitLine() {
            if (part is Line) {
                Label().let {
                    mv.visitLabel(it)
                    mv.visitLineNumber(part.line, it)
                }
            }
        }

        return if (operation === Operators.ADD && isIncrementOne) {
            visitLine()
            mv.visitIincInsn(varPos, 1)
            true
        } else if (operation === Operators.SUBTRACT && isIncrementOne) {
            visitLine()
            mv.visitIincInsn(varPos, -1)
            true
        } else if (constantVal) {
            if (operation === Operators.ADD) {
                visitLine()
                mv.visitIincInsn(varPos, constant)
                true
            } else if (operation === Operators.SUBTRACT) {
                visitLine()
                mv.visitIincInsn(varPos, -constant)
                true
            } else {
                false
            }
        } else {
            false
        }
    }
}