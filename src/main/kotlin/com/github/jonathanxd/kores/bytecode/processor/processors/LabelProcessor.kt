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

import com.github.jonathanxd.iutils.data.TypedData
import com.github.jonathanxd.iutils.kt.add
import com.github.jonathanxd.iutils.kt.require
import com.github.jonathanxd.kores.Instructions
import com.github.jonathanxd.kores.bytecode.common.Flow
import com.github.jonathanxd.kores.bytecode.processor.FLOWS
import com.github.jonathanxd.kores.bytecode.processor.METHOD_VISITOR
import com.github.jonathanxd.kores.processor.Processor
import com.github.jonathanxd.kores.processor.ProcessorManager
import org.objectweb.asm.Label
import com.github.jonathanxd.kores.base.Label as CodeLabel

object LabelProcessor : Processor<CodeLabel> {

    override fun process(part: CodeLabel, data: TypedData, processorManager: ProcessorManager<*>) {
        val visitor = METHOD_VISITOR.require(data).methodVisitor
        val start = Label()
        val end = Label()

        visitor.visitLabel(start)

        val flow = Flow(part, start, start, end, end)

        FLOWS.add(data, flow)

        processorManager.process(Instructions::class.java, part.body, data)

        FLOWS.require(data).remove(flow)

        visitor.visitLabel(end)
    }

}