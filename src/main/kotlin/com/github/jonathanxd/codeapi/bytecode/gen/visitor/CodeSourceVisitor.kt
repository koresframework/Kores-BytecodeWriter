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
package com.github.jonathanxd.codeapi.bytecode.gen.visitor

import com.github.jonathanxd.codeapi.CodeSource
import com.github.jonathanxd.codeapi.bytecode.BytecodeClass
import com.github.jonathanxd.codeapi.bytecode.VISIT_LINES
import com.github.jonathanxd.codeapi.bytecode.VisitLineType
import com.github.jonathanxd.codeapi.bytecode.common.MVData
import com.github.jonathanxd.codeapi.common.Data
import com.github.jonathanxd.codeapi.gen.visit.Visitor
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import org.objectweb.asm.Label
import java.util.function.Consumer

object CodeSourceVisitor : Visitor<CodeSource, BytecodeClass, Any?> {

    // Int
    val OFFSET = "LINE_OFFSET"

    override fun visit(t: CodeSource, extraData: Data, visitorGenerator: VisitorGenerator<BytecodeClass>, additional: Any?): Array<out BytecodeClass> {
        val appender = visitorGenerator.createAppender()

        val max = t.size - 1

        val visit = visitorGenerator.options.get(VISIT_LINES).get()

        val offset = extraData.getOptional<Int>(OFFSET).orElse(0)

        if (visit == VisitLineType.FOLLOW_CODE_SOURCE)
            extraData.registerData(OFFSET, offset + max + 1)

        for (i in 0..max) {

            if (additional is MVData && visit == VisitLineType.FOLLOW_CODE_SOURCE) {
                val line = i + 1 + offset
                val label = Label()

                additional.methodVisitor.visitLabel(label)
                additional.methodVisitor.visitLineNumber(line, label)
            }

            val codePart = t[i]

            val aClass = codePart::class.java

            visitorGenerator.generateTo(aClass, codePart, extraData, Consumer {
                appender.add(it)
            }, additional)
        }

        return appender.get()
    }

}