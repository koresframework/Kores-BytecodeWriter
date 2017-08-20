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

import com.github.jonathanxd.codeapi.CodeSource
import com.github.jonathanxd.codeapi.bytecode.VISIT_LINES
import com.github.jonathanxd.codeapi.bytecode.VisitLineType
import com.github.jonathanxd.codeapi.bytecode.processor.FLOWS
import com.github.jonathanxd.codeapi.bytecode.processor.IN_EXPRESSION
import com.github.jonathanxd.codeapi.bytecode.processor.METHOD_VISITOR
import com.github.jonathanxd.codeapi.processor.Processor
import com.github.jonathanxd.codeapi.processor.ProcessorManager
import com.github.jonathanxd.codeapi.util.require
import com.github.jonathanxd.codeapi.util.typedKeyOf
import com.github.jonathanxd.iutils.data.TypedData
import org.objectweb.asm.Label

object CodeSourceProcessor : Processor<CodeSource> {

    val OFFSET = typedKeyOf<Int>("LINE_OFFSET")

    override fun process(part: CodeSource, data: TypedData, processorManager: ProcessorManager<*>) {
        val max = part.size - 1

        val visit = processorManager.options.get(VISIT_LINES)

        val offset = OFFSET.getOrSet(data, 0)

        if (visit == VisitLineType.FOLLOW_CODE_SOURCE)
            OFFSET.set(data, offset + max + 1)

        var changed = false
        val inExpr = IN_EXPRESSION.require(data)

        if (inExpr > 0 && max > 0) {
            IN_EXPRESSION.set(data, 0)
            changed = true
        }

        for (i in 0..max) {

            METHOD_VISITOR.getOrNull(data)?.let {
                if (visit == VisitLineType.FOLLOW_CODE_SOURCE) {
                    val line = i + 1 + offset
                    val label = Label()

                    it.methodVisitor.visitLabel(label)
                    it.methodVisitor.visitLineNumber(line, label)
                }
            }

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