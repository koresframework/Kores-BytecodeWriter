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
import com.github.jonathanxd.codeapi.MutableCodeSource
import com.github.jonathanxd.codeapi.base.ForStatement
import com.github.jonathanxd.codeapi.base.IfStatement
import com.github.jonathanxd.codeapi.bytecode.common.Flow
import com.github.jonathanxd.codeapi.bytecode.processor.FLOWS
import com.github.jonathanxd.codeapi.bytecode.processor.IN_EXPRESSION
import com.github.jonathanxd.codeapi.bytecode.processor.METHOD_VISITOR
import com.github.jonathanxd.codeapi.bytecode.processor.incrementInContext
import com.github.jonathanxd.codeapi.common.CodeNothing
import com.github.jonathanxd.codeapi.processor.Processor
import com.github.jonathanxd.codeapi.processor.ProcessorManager
import com.github.jonathanxd.codeapi.util.add
import com.github.jonathanxd.codeapi.util.require
import com.github.jonathanxd.codeapi.util.safeForComparison
import com.github.jonathanxd.iutils.data.TypedData
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes

object ForStatementProcessor : Processor<ForStatement> {

    override fun process(part: ForStatement, data: TypedData, processorManager: ProcessorManager<*>) {
        val mv = METHOD_VISITOR.require(data).methodVisitor

        val outsideStart = Label()
        val whileStart = Label()
        val whileEnd = Label()
        val outsideEnd = Label()

        mv.visitLabel(outsideStart)

        val init = part.forInit

        if (init.safeForComparison != CodeNothing) {
            IN_EXPRESSION.incrementInContext(data) {
                processorManager.process(init::class.java, init, data)
            }
        }


        val source = MutableCodeSource.create()

        source.addAll(part.body)

        val ifStatement = IfStatement(expressions = part.forExpression,
                body = source,
                elseStatement = CodeSource.empty())

        mv.visitLabel(whileStart)

        val flow = Flow(null, outsideStart, whileStart, whileEnd, outsideEnd)

        FLOWS.add(data, flow)

        val instructionCodePart = InstructionCodePart.create { _, instructionData, iCodeProcessor ->

            mv.visitLabel(whileEnd)

            val update = part.forUpdate

            if (update.safeForComparison != CodeNothing) {
                IN_EXPRESSION.incrementInContext(data) {
                    iCodeProcessor.process(update::class.java, update, instructionData)
                }
            }

            mv.visitJumpInsn(Opcodes.GOTO, whileStart)
        }

        source.add(instructionCodePart)

        processorManager.process(IfStatement::class.java, ifStatement, data)

        FLOWS.require(data).remove(flow)

        mv.visitLabel(outsideEnd)
    }

}