/*
 *      Kores-BytecodeWriter - Translates Kores Structure to JVM Bytecode <https://github.com/JonathanxD/CodeAPI-BytecodeWriter>
 *
 *         The MIT License (MIT)
 *
 *      Copyright (c) 2021 TheRealBuggy/JonathanxD (https://github.com/JonathanxD/) <jonathan.scripter@programmer.net>
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
package com.koresframework.kores.bytecode.processor.processors

import com.koresframework.kores.Types
import com.koresframework.kores.base.Synchronized
import com.koresframework.kores.base.TryStatement
import com.koresframework.kores.base.VariableAccess
import com.koresframework.kores.base.VariableDeclaration
import com.koresframework.kores.bytecode.extra.Dup
import com.koresframework.kores.bytecode.processor.IN_EXPRESSION
import com.koresframework.kores.bytecode.processor.METHOD_VISITOR
import com.koresframework.kores.bytecode.processor.incrementInContext
import com.koresframework.kores.factory.accessVariable
import com.koresframework.kores.factory.variable
import com.koresframework.kores.processor.Processor
import com.koresframework.kores.processor.ProcessorManager
import com.koresframework.kores.typeOrNull
import com.github.jonathanxd.iutils.data.TypedData
import com.github.jonathanxd.iutils.kt.require
import com.koresframework.kores.Instructions
import org.objectweb.asm.Opcodes

object SynchronizedProcessor : Processor<Synchronized> {

    override fun process(
        part: Synchronized,
        data: TypedData,
        processorManager: ProcessorManager<*>
    ) {

        val mvHelper = METHOD_VISITOR.require(data)
        val visitor = mvHelper.methodVisitor

        val name = mvHelper.getUniqueVariableName("\$sync_var#0")
        val type = part.instruction.typeOrNull ?: Types.OBJECT

        val variable = variable(type, name, Dup(part.instruction))

        processorManager.process(VariableDeclaration::class.java, variable, data)

        visitor.visitInsn(Opcodes.MONITORENTER)

        TryStatement.Builder.builder()
            .body(part.body)
            .finallyStatement(Instructions.fromPart(
                InstructionCodePart.create { _, fdata, fprocessorManager ->
                    IN_EXPRESSION.incrementInContext(fdata) {
                        fprocessorManager.process(
                            VariableAccess::class.java,
                            accessVariable(variable),
                            fdata
                        )
                    }
                    visitor.visitInsn(Opcodes.MONITOREXIT)
                }
            ))
            .build().let {
            processorManager.process(TryStatement::class.java, it, data)
        }
    }
}