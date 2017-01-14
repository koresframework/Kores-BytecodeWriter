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

import com.github.jonathanxd.codeapi.CodeAPI
import com.github.jonathanxd.codeapi.CodeSource
import com.github.jonathanxd.codeapi.base.CatchStatement
import com.github.jonathanxd.codeapi.base.ThrowException
import com.github.jonathanxd.codeapi.base.TryStatement
import com.github.jonathanxd.codeapi.bytecode.BytecodeClass
import com.github.jonathanxd.codeapi.bytecode.common.MVData
import com.github.jonathanxd.codeapi.bytecode.util.CodeTypeUtil
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.gen.visit.VoidVisitor
import com.github.jonathanxd.codeapi.util.source.CodeSourceUtil
import com.github.jonathanxd.iutils.container.primitivecontainers.BooleanContainer
import com.github.jonathanxd.iutils.data.MapData
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes

object TryStatementVisitor : VoidVisitor<TryStatement, BytecodeClass, MVData> {

    override fun voidVisit(t: TryStatement, extraData: MapData, visitorGenerator: VisitorGenerator<BytecodeClass>, additional: MVData) {
        val mv = additional.methodVisitor


        val l0 = Label() // Code to surround
        val l1 = Label() // if no exceptions.
        //Label lCatch = new Label(); // Catch block


        val finallySource = t.finallyStatement

        val finallyLabel = if (finallySource != null) {
            Label()
        } else {
            null
        }

        val catches = java.util.HashMap<CatchStatement, Label>()

        val outOfIf = Label() // Out of if

        for (catchBlock in t.catchStatements) {

            val lCatch = Label()

            val exceptionTypes = catchBlock.exceptionTypes

            for (exceptionType in exceptionTypes) {
                mv.visitTryCatchBlock(l0, l1, lCatch, CodeTypeUtil.codeTypeToBinaryName(exceptionType))
            }

            catches.put(catchBlock, lCatch)
        }

        mv.visitLabel(l0)

        val body = t.body

        if (body != null) {
            visitorGenerator.generateTo(CodeSource::class.java, body, extraData, null, additional)
        }

        mv.visitLabel(l1)

        if (finallyLabel != null) {
            mv.visitLabel(finallyLabel)
            visitorGenerator.generateTo(CodeSource::class.java, finallySource!!, extraData, null, additional)
        }


        mv.visitJumpInsn(Opcodes.GOTO, outOfIf)

        ///////////////////////////////

        val i_label = Label()

        mv.visitLabel(i_label)

        val endLabel = Label()

        val unkExceptionName = additional.getUniqueVariableName("unknownException$$")
        val stackPos = additional.storeVar(unkExceptionName, CodeAPI.getJavaType(Throwable::class.java), i_label, null)
                .orElseThrow({ additional.failStore(unkExceptionName) })

        catches.forEach { catchBlock, label ->

            mv.visitLabel(label)

            val field = catchBlock.variable
            val fieldValue = field.value

            additional.redefineVar(stackPos, field.name, field.variableType, label, endLabel)

            mv.visitVarInsn(Opcodes.ASTORE, stackPos)

            if (fieldValue != null) {
                visitorGenerator.generateTo(fieldValue.javaClass, fieldValue, extraData, null, additional)

                mv.visitVarInsn(Opcodes.ASTORE, stackPos)
            }

            val codeSource = catchBlock.body

            var toAdd = CodeSource.empty()

            if (finallyLabel != null) {
                toAdd = finallySource!!
            }

            val booleanContainer = BooleanContainer(false)

            if (codeSource != null) {
                var codeSource1 = CodeSource.fromIterable(codeSource)


                codeSource1 = CodeSourceUtil.insertBefore({ codePart ->
                    if (codePart is ThrowException) {
                        booleanContainer.set(true)
                        return@insertBefore true
                    }

                    false
                }, toAdd, codeSource1)

                visitorGenerator.generateTo(CodeSource::class.java, codeSource1, extraData, null, additional)
            }


            if (!booleanContainer.get()) {
                if (finallyLabel != null) {
                    visitorGenerator.generateTo(CodeSource::class.java, finallySource!!, extraData, null, additional)
                }
            }

            mv.visitJumpInsn(Opcodes.GOTO, outOfIf)

        }

        mv.visitLabel(endLabel)

        mv.visitLabel(outOfIf)

        // OUT OF --
    }

}