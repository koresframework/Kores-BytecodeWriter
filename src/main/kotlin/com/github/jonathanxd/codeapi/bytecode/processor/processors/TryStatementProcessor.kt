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
import com.github.jonathanxd.codeapi.base.*
import com.github.jonathanxd.codeapi.bytecode.processor.METHOD_VISITOR
import com.github.jonathanxd.codeapi.common.CodeNothing
import com.github.jonathanxd.codeapi.processor.Processor
import com.github.jonathanxd.codeapi.processor.ProcessorManager
import com.github.jonathanxd.codeapi.util.*
import com.github.jonathanxd.iutils.data.TypedData
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes

object TryStatementProcessor : Processor<TryStatement> {

    override fun process(part: TryStatement, data: TypedData, processorManager: ProcessorManager<*>) {
        val mvHelper = METHOD_VISITOR.require(data)
        val mv = mvHelper.methodVisitor

        val l0 = Label() // Code to surround
        val l1 = Label() // if no exceptions.

        val finallySource = part.finallyStatement

        val catches = java.util.HashMap<CatchStatement, Label>()

        val outOfIf = Label() // Out of if

        for (catchBlock in part.catchStatements) {

            val lCatch = Label()

            val exceptionTypes = catchBlock.exceptionTypes

            for (exceptionType in exceptionTypes) {
                mv.visitTryCatchBlock(l0, l1, lCatch, exceptionType.internalName)
            }

            catches.put(catchBlock, lCatch)
        }

        mv.visitLabel(l0)

        var body = part.body

        mvHelper.enterNewFrame()

        if (finallySource.isNotEmpty) {
            body = insertAfterOrEnd({
                it is ThrowException || it is Return || it is ControlFlow
            }, finallySource, body)
        }

        processorManager.process(CodeSource::class.java, body, data)

        mvHelper.exitFrame()

        mv.visitLabel(l1)

        mv.visitJumpInsn(Opcodes.GOTO, outOfIf)

        ///////////////////////////////

        val i_label = Label()

        mv.visitLabel(i_label)

        val endLabel = Label()


        catches.forEach { (_, field, codeSource), label ->

            mv.visitLabel(label)
            mvHelper.enterNewFrame()

            val fieldValue = field.value

            val stackPos = mvHelper.storeVar(field.name, field.type.codeType, i_label, null)
                    .orElseThrow({ mvHelper.failStore(field.name) })

            mv.visitVarInsn(Opcodes.ASTORE, stackPos)

            if (fieldValue != CodeNothing) {
                processorManager.process(fieldValue::class.java, fieldValue, data)

                mv.visitVarInsn(Opcodes.ASTORE, stackPos)
            }

            var codeSource1 = CodeSource.fromIterable(codeSource)

            codeSource1 = insertBeforeOrEnd({
                it is ThrowException || it is Return || it is ControlFlow
            }, finallySource, codeSource1)

            processorManager.process(CodeSource::class.java, codeSource1, data)

            mvHelper.exitFrame()

            mv.visitJumpInsn(Opcodes.GOTO, outOfIf)

        }

        mv.visitLabel(endLabel)

        mv.visitLabel(outOfIf)


    }


}