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
package com.github.jonathanxd.kores.bytecode.processor.processors

import com.github.jonathanxd.kores.base.ControlFlow
import com.github.jonathanxd.kores.bytecode.processor.FLOWS
import com.github.jonathanxd.kores.bytecode.processor.METHOD_VISITOR
import com.github.jonathanxd.kores.bytecode.processor.TRY_BLOCK_DATA
import com.github.jonathanxd.kores.processor.Processor
import com.github.jonathanxd.kores.processor.ProcessorManager
import com.github.jonathanxd.iutils.data.TypedData
import com.github.jonathanxd.iutils.kt.require
import org.objectweb.asm.Opcodes

object ControlFlowProcessor : Processor<ControlFlow> {

    override fun process(
        part: ControlFlow,
        data: TypedData,
        processorManager: ProcessorManager<*>
    ) {
        val flows = FLOWS.require(data)

        val flow =
            if (part.at == null) flows.last() else flows.findLast { it.label != null && part.at!!.name == it.label.name }!!

        val visitor = METHOD_VISITOR.require(data).methodVisitor

        TRY_BLOCK_DATA.getOrNull(data)?.let { blocks ->

            if (blocks.isNotEmpty()) {
                val anyGen = blocks.any { it.canGen() }

                if (anyGen) {
                    TRY_BLOCK_DATA.remove(data)

                    blocks.forEach {
                        val time =
                            if (part.at != null) FLOWS.require(data).first { it.label == part.at }.creationInstant
                            else flow.creationInstant

                        // Hacky check to determine whether block is inside of the try-catch or not
                        if (time.isBefore(it.creationInstant)) {
                            // If flow is created before the statement
                            it.visit(processorManager, data)
                        }
                    }

                    TRY_BLOCK_DATA.set(data, blocks)
                }
            }
        }

        if (part.type == ControlFlow.Type.BREAK) {
            visitor.visitJumpInsn(Opcodes.GOTO, flow.outsideEnd)
        } else if (part.type == ControlFlow.Type.CONTINUE) {
            visitor.visitJumpInsn(Opcodes.GOTO, flow.insideEnd)
        }
    }

}