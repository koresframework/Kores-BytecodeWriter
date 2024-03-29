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
import com.github.jonathanxd.iutils.kt.require
import com.koresframework.kores.Instructions
import com.koresframework.kores.bytecode.processor.IN_EXPRESSION
import com.koresframework.kores.processor.Processor
import com.koresframework.kores.processor.ProcessorManager

object InstructionsProcessor : Processor<Instructions> {


    override fun process(
        part: Instructions,
        data: TypedData,
        processorManager: ProcessorManager<*>
    ) {
        val max = part.size - 1

        val visitLine = LineProcessor.visitLineOF(processorManager, data, max)

        var changed = false
        val inExpr = IN_EXPRESSION.require(data)

        if (inExpr > 0 && max > 0) {
            IN_EXPRESSION.set(data, 0)
            changed = true
        }

        for (i in 0..max) {
            visitLine(i)

            val codePart = part[i]

            if (inExpr > 0 && i == max && changed) {
                IN_EXPRESSION.set(data, inExpr)
                changed = false
            }

            processorManager.process(codePart::class.java, codePart, data)

        }

        if (changed) {
            IN_EXPRESSION.set(data, inExpr)
        }
    }


}