/*
 *      CodeAPI-BytecodeWriter - Translates CodeAPI Structure to JVM Bytecode <https://github.com/JonathanxD/CodeAPI-BytecodeWriter>
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
package com.github.jonathanxd.codeapi.bytecode.processor.processors

import com.github.jonathanxd.codeapi.CodeSource
import com.github.jonathanxd.codeapi.base.CatchStatement
import com.github.jonathanxd.codeapi.base.ThrowException
import com.github.jonathanxd.codeapi.base.TryStatement
import com.github.jonathanxd.codeapi.bytecode.processor.METHOD_VISITOR
import com.github.jonathanxd.codeapi.bytecode.processor.TRY_BLOCK_DATA
import com.github.jonathanxd.codeapi.bytecode.processor.TryBlockData
import com.github.jonathanxd.codeapi.common.CodeNothing
import com.github.jonathanxd.codeapi.factory.accessVariable
import com.github.jonathanxd.codeapi.factory.variable
import com.github.jonathanxd.codeapi.processor.Processor
import com.github.jonathanxd.codeapi.processor.ProcessorManager
import com.github.jonathanxd.codeapi.type.codeType
import com.github.jonathanxd.codeapi.type.internalName
import com.github.jonathanxd.iutils.data.TypedData
import com.github.jonathanxd.iutils.kt.add
import com.github.jonathanxd.iutils.kt.require
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes

object TryStatementProcessor : Processor<TryStatement> {

    override fun process(
        part: TryStatement,
        data: TypedData,
        processorManager: ProcessorManager<*>
    ) {
        val mvHelper = METHOD_VISITOR.require(data)
        val mv = mvHelper.methodVisitor

        val l0 = Label() // Code to surround
        val l1 = Label() // if no exceptions.
        val endTc = Label() // End of try catch
        val lCatchAll = Label()

        val finallySource = part.finallyStatement
        val genFinally = finallySource.isNotEmpty

        val catches = mutableMapOf<CatchStatement, TryBlockData>()

        val outOfIf = Label() // Out of if

        for (catchBlock in part.catchStatements) {

            val lCatch = Label()

            val exceptionTypes = catchBlock.exceptionTypes

            for (exceptionType in exceptionTypes) {
                mv.visitTryCatchBlock(l0, l1, lCatch, exceptionType.internalName)
            }

            catches.put(catchBlock, TryBlockData(lCatch, part))
        }

        fun TryBlockData.visitBlocks(start: Label, end: Label) {
            if (!genFinally) return

            if (this.labels.isEmpty()) {
                mv.visitTryCatchBlock(start, start, lCatchAll, null)
            } else {
                var last = start

                this.labels.forEach {
                    if (last.offset != it.start.offset) {
                        mv.visitTryCatchBlock(last, it.start, lCatchAll, null)
                    }
                    last = it.end
                }

                if (last.offset != end.offset)
                    mv.visitTryCatchBlock(last, end, lCatchAll, null)
            }

        }

        fun genFinally() {
            if (!genFinally) return
            TRY_BLOCK_DATA.getOrNull(data)?.let { blocks ->
                if (blocks.isNotEmpty()) {
                    TRY_BLOCK_DATA.remove(data)

                    blocks.forEach { it.visit(processorManager, data) }

                    TRY_BLOCK_DATA.set(data, blocks)
                }
            }
        }

        mv.visitLabel(l0)

        val tryBlockData = TryBlockData(l0, part)
        TRY_BLOCK_DATA.add(data, tryBlockData)

        val body = part.body

        mvHelper.enterNewFrame()

        processorManager.process(CodeSource::class.java, body, data)
        genFinally()

        mvHelper.exitFrame()

        TRY_BLOCK_DATA.require(data).remove(tryBlockData)

        mv.visitLabel(l1)

        tryBlockData.visitBlocks(l0, l1)

        mv.visitJumpInsn(Opcodes.GOTO, outOfIf)

        ///////////////////////////////

        val i_label = Label()

        mv.visitLabel(i_label)

        val endLabel = Label()

        catches.forEach { (_, field, codeSource), tdata ->

            TRY_BLOCK_DATA.add(data, tdata)
            val label = tdata.startLabel
            val end = Label()

            mv.visitLabel(label)
            mvHelper.enterNewFrame()

            val fieldValue = field.value

            val stackPos = mvHelper.storeVar(field.name, field.type.codeType, i_label, end)
                .orElseThrow({ mvHelper.failStore(field.name) })

            mv.visitLabel(Label())

            mv.visitVarInsn(Opcodes.ASTORE, stackPos)

            if (fieldValue != CodeNothing) {
                processorManager.process(fieldValue::class.java, fieldValue, data)

                mv.visitVarInsn(Opcodes.ASTORE, stackPos)
            }

            processorManager.process(CodeSource::class.java, codeSource, data)

            genFinally()

            mvHelper.exitFrame()

            mv.visitLabel(end)

            TRY_BLOCK_DATA.require(data).remove(tdata)

            tdata.visitBlocks(label, end)

            mv.visitJumpInsn(Opcodes.GOTO, outOfIf)
        }

        mv.visitLabel(endTc)

        if (genFinally) {
            mv.visitLabel(lCatchAll)
            val end = Label()

            val dummyName = mvHelper.getUniqueVariableName("dummy\$#")
            val name = mvHelper.getUniqueVariableName("finallyException\$#")

            mvHelper.enterNewFrame()

            val variable = variable(Throwable::class.java, name, CodeNothing)

            mvHelper.storeVar(dummyName, Exception::class.java.codeType, lCatchAll, end)
                .orElseThrow({ mvHelper.failStore(variable.name) }) // dummy

            val stackPos = mvHelper.storeVar(variable.name, variable.type.codeType, lCatchAll, end)
                .orElseThrow({ mvHelper.failStore(variable.name) })

            mv.visitLabel(Label())

            mv.visitVarInsn(Opcodes.ASTORE, stackPos)

            processorManager.process(CodeSource::class.java, finallySource, data)

            processorManager.process(
                ThrowException::class.java,
                ThrowException(accessVariable(variable)),
                data
            )

            mv.visitLabel(end)

            mvHelper.exitFrame()
        }

        mv.visitLabel(endLabel)

        mv.visitLabel(outOfIf)

        TRY_BLOCK_DATA.require(data).remove(tryBlockData)
    }

}