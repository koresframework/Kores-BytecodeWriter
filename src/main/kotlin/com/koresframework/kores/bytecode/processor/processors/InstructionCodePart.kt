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

import com.github.jonathanxd.iutils.data.TypedData
import com.koresframework.kores.Instruction
import com.koresframework.kores.data.KoresData
import com.koresframework.kores.processor.Processor
import com.koresframework.kores.processor.ProcessorManager

interface InstructionCodePart : Instruction {

    fun apply(value: Any, data: TypedData, processorManager: ProcessorManager<*>)

    companion object {
        inline fun create(crossinline func: (value: Any, data: TypedData, processorManager: ProcessorManager<*>) -> Unit): InstructionCodePart {
            return object : InstructionCodePart {
                override val data: KoresData = KoresData()
                override fun apply(
                    value: Any,
                    data: TypedData,
                    processorManager: ProcessorManager<*>
                ) {
                    func(value, data, processorManager)
                }
            }
        }
    }

    object InstructionCodePartVisitor : Processor<InstructionCodePart> {

        override fun process(
            part: InstructionCodePart,
            data: TypedData,
            processorManager: ProcessorManager<*>
        ) {
            part.apply(part, data, processorManager)
        }

    }

}
