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

import com.github.jonathanxd.kores.Types
import com.github.jonathanxd.kores.base.Return
import com.github.jonathanxd.kores.base.VariableDeclaration
import com.github.jonathanxd.kores.bytecode.processor.*
import com.github.jonathanxd.kores.common.KoresNothing
import com.github.jonathanxd.kores.common.Void
import com.github.jonathanxd.kores.factory.accessVariable
import com.github.jonathanxd.kores.factory.variable
import com.github.jonathanxd.kores.processor.Processor
import com.github.jonathanxd.kores.processor.ProcessorManager
import com.github.jonathanxd.kores.safeForComparison
import com.github.jonathanxd.kores.type.`is`
import com.github.jonathanxd.kores.type.javaSpecName
import com.github.jonathanxd.iutils.data.TypedData
import com.github.jonathanxd.iutils.kt.require
import com.github.jonathanxd.kores.factory.cast
import com.github.jonathanxd.kores.typeOrNull
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type

object ReturnProcessor : Processor<Return> {

    override fun process(part: Return, data: TypedData, processorManager: ProcessorManager<*>) {
        val mvHelper = METHOD_VISITOR.require(data)
        val mv = mvHelper.methodVisitor

        var value = part.value
        var finallyInlined = false

        val toRetType = part.type

        TRY_BLOCK_DATA.getOrNull(data)?.let { blocks ->
            if (blocks.isNotEmpty()) {
                val anyGen = blocks.any { it.canGen() }

                if (anyGen) {

                    if (!toRetType.`is`(Types.VOID)) {
                        val unique = mvHelper.getUniqueVariableName("\$tmpVar_")

                        val variable = variable(toRetType, unique, value)
                        processorManager.process(VariableDeclaration::class.java, variable, data)
                        value = accessVariable(variable.type, variable.name)
                    }

                    TRY_BLOCK_DATA.remove(data)

                    blocks.forEach { it.visit(processorManager, data) }

                    finallyInlined = true

                    TRY_BLOCK_DATA.set(data, blocks)
                }
            }
        }

        val safeValue = value.safeForComparison

        if (safeValue != Void && safeValue != KoresNothing) {
            value.typeOrNull?.let { origin ->
                if (!origin.`is`(toRetType))
                    value = cast(origin, toRetType, value)
            }

            IN_EXPRESSION.incrementInContext(data) {
                processorManager.process(value::class.java, value, data)
            }

        }

        var opcode = Opcodes.RETURN

        if (!toRetType.`is`(Types.VOID)) {
            val type = Type.getType(toRetType.javaSpecName)

            opcode = type.getOpcode(Opcodes.IRETURN) // ARETURN
        }


        if (finallyInlined) {
            C_LINE.getOrNull(data)?.lastOrNull()?.let {
                val lb = Label()
                mv.visitLabel(lb)
                mv.visitLineNumber(it.line, lb)
            }
        }

        mv.visitInsn(opcode)

    }


}