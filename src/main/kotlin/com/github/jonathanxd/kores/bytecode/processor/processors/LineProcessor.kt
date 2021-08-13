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

import com.github.jonathanxd.iutils.data.TypedData
import com.github.jonathanxd.iutils.kt.add
import com.github.jonathanxd.iutils.kt.require
import com.github.jonathanxd.iutils.kt.typedKeyOf
import com.github.jonathanxd.kores.KoresPart
import com.github.jonathanxd.kores.base.Line
import com.github.jonathanxd.kores.bytecode.VISIT_LINES
import com.github.jonathanxd.kores.bytecode.VisitLineType
import com.github.jonathanxd.kores.bytecode.pre.GenLineVisitor
import com.github.jonathanxd.kores.bytecode.processor.CLine
import com.github.jonathanxd.kores.bytecode.processor.C_LINE
import com.github.jonathanxd.kores.bytecode.processor.LINE
import com.github.jonathanxd.kores.bytecode.processor.METHOD_VISITOR
import com.github.jonathanxd.kores.processor.Processor
import com.github.jonathanxd.kores.processor.ProcessorManager
import org.objectweb.asm.Label

object LineProcessor : Processor<Line> {

    private val OFFSET = typedKeyOf<Int>("LINE_OFFSET")

    override fun process(part: Line, data: TypedData, processorManager: ProcessorManager<*>) {

        if (processorManager.options[VISIT_LINES] == VisitLineType.LINE_INSTRUCTION
                || processorManager.options[VISIT_LINES] == VisitLineType.GEN_LINE_INSTRUCTION
        ) {
            val mvHelper = METHOD_VISITOR.require(data)

            val label = Label()
            mvHelper.methodVisitor.visitLabel(label)
            mvHelper.methodVisitor.visitLineNumber(part.line, label)

            val cline = CLine(part.line, label)

            C_LINE.add(data, cline)

            processorManager.process(part.value, data)

            C_LINE.require(data).remove(cline)
        } else {
            processorManager.process(part.value, data)
        }
    }

    /**
     * Creates a offset-ed line visit function when [VISIT_LINES] is set to [VisitLineType.FOLLOW_CODE_SOURCE].
     */
    fun visitLineOF(manager: ProcessorManager<*>, data: TypedData, max: Int): (Int) -> Unit {

        val visit = manager.options.get(VISIT_LINES)

        if (visit != VisitLineType.FOLLOW_CODE_SOURCE)
            return {}

        val offset = OFFSET.getOrSet(data, 0)

        if (visit == VisitLineType.FOLLOW_CODE_SOURCE)
            OFFSET.set(data, offset + max + 1)

        return { i ->
            METHOD_VISITOR.getOrNull(data)?.let {
                if (visit == VisitLineType.FOLLOW_CODE_SOURCE) {
                    val line = i + 1 + offset
                    val label = Label()

                    it.methodVisitor.visitLabel(label)
                    it.methodVisitor.visitLineNumber(line, label)
                }
            }
        }
    }

    /**
     * Do incremental line visit transform
     */
    fun visitLineICT(part: Any, manager: ProcessorManager<*>): Any =
        if (part is KoresPart && manager.options.get(VISIT_LINES) == VisitLineType.GEN_LINE_INSTRUCTION) {
            GenLineVisitor.visit(part)
        } else part


    /**
     * Do incremental line visit when [VISIT_LINES] is set to [VisitLineType.INCREMENTAL].
     */
    fun visitLineIC(manager: ProcessorManager<*>, data: TypedData) {
        val mvData = METHOD_VISITOR.getOrNull(data)

        if (manager.options.get(VISIT_LINES) == VisitLineType.INCREMENTAL
                && mvData != null
        ) {

            val line = LINE.let {
                if (!it.contains(data)) {
                    it.set(data, 1)
                    0
                } else {
                    val get = it.require(data)
                    it.set(data, get + 1)
                    get
                }
            }

            val label = org.objectweb.asm.Label()

            mvData.methodVisitor.visitLabel(label)

            mvData.methodVisitor.visitLineNumber(line, label)
        }
    }
}