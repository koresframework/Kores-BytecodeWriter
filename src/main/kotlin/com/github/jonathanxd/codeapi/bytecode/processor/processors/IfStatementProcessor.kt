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
import com.github.jonathanxd.codeapi.base.IfStatement
import com.github.jonathanxd.codeapi.bytecode.processor.FLOWS
import com.github.jonathanxd.codeapi.bytecode.processor.METHOD_VISITOR
import com.github.jonathanxd.codeapi.processor.Processor
import com.github.jonathanxd.codeapi.processor.ProcessorManager
import com.github.jonathanxd.iutils.data.TypedData
import com.github.jonathanxd.jwiutils.kt.require
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes

object IfStatementProcessor : Processor<IfStatement> {

    override fun process(part: IfStatement, data: TypedData, processorManager: ProcessorManager<*>) {
        val mvHelper = METHOD_VISITOR.require(data)

        val startIfLabel = Label()
        val ifBody = Label()
        val endIfLabel = Label()

        val elseLabel = Label()

        val elseStatement = part.elseStatement

        val jumpLabel =
                when {
                    part.body.contains(SwitchProcessor.SwitchMarker) -> FLOWS.require(data).last().insideEnd
                    elseStatement.isNotEmpty -> elseLabel
                    else -> endIfLabel
                }

        val methodVisitor = mvHelper.methodVisitor

        methodVisitor.visitLabel(startIfLabel)

        visit(part.expressions, startIfLabel, ifBody, jumpLabel, false, data, processorManager, mvHelper)

        val body = part.body

        methodVisitor.visitLabel(ifBody)

        mvHelper.enterNewFrame()

        processorManager.process(CodeSource::class.java, body, data)

        mvHelper.exitFrame()

        if (elseStatement.isNotEmpty) {
            methodVisitor.visitJumpInsn(Opcodes.GOTO, endIfLabel)
        }

        if (elseStatement.isNotEmpty) {
            methodVisitor.visitLabel(elseLabel)

            mvHelper.enterNewFrame()

            processorManager.process(CodeSource::class.java, elseStatement, data)

            mvHelper.exitFrame()
        }

        methodVisitor.visitLabel(endIfLabel)
    }

}