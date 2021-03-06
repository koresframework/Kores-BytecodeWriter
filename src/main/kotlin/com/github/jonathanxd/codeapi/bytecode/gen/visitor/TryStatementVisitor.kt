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
import com.github.jonathanxd.codeapi.base.*
import com.github.jonathanxd.codeapi.bytecode.BytecodeClass
import com.github.jonathanxd.codeapi.bytecode.common.MVData
import com.github.jonathanxd.codeapi.bytecode.util.CodeTypeUtil
import com.github.jonathanxd.codeapi.common.Data
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.gen.visit.VoidVisitor
import com.github.jonathanxd.codeapi.util.source.CodeSourceUtil
import com.github.jonathanxd.iutils.container.primitivecontainers.BooleanContainer
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes
import java.util.function.Predicate

object TryStatementVisitor : VoidVisitor<TryStatement, BytecodeClass, MVData> {

    override fun voidVisit(t: TryStatement, extraData: Data, visitorGenerator: VisitorGenerator<BytecodeClass>, additional: MVData) {
        val mv = additional.methodVisitor


        val l0 = Label() // Code to surround
        val l1 = Label() // if no exceptions.
        //Label lCatch = new Label(); // Catch block


        val finallySource = t.finallyStatement

        val finallyLabel = Label()

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

        var body = t.body

        additional.enterNewFrame()

        if(finallySource.isNotEmpty) {
            body = CodeSourceUtil.insertAfterOrEnd({
                it is ThrowException || it is Return || it is ControlFlow
            }, finallySource, body)
        }

        visitorGenerator.generateTo(CodeSource::class.java, body, extraData, null, additional)

        additional.exitFrame()

        mv.visitLabel(l1)

        /*if(finallySource.isNotEmpty) {
            additional.enterNewFrame()

            mv.visitLabel(finallyLabel)
            visitorGenerator.generateTo(CodeSource::class.java, finallySource, extraData, null, additional)

            additional.exitFrame()
        }*/

        mv.visitJumpInsn(Opcodes.GOTO, outOfIf)

        ///////////////////////////////

        val i_label = Label()

        mv.visitLabel(i_label)

        val endLabel = Label()


        catches.forEach { catchBlock, label ->

            mv.visitLabel(label)
            additional.enterNewFrame()

            val field = catchBlock.variable
            val fieldValue = field.value

            val stackPos = additional.storeVar(field.name, field.type, i_label, null)
                    .orElseThrow({ additional.failStore(field.name) })

            //additional.redefineVar(stackPos, field.name, field.variableType, label, endLabel)

            mv.visitVarInsn(Opcodes.ASTORE, stackPos)

            if (fieldValue != null) {
                visitorGenerator.generateTo(fieldValue::class.java, fieldValue, extraData, null, additional)

                mv.visitVarInsn(Opcodes.ASTORE, stackPos)
            }

            val codeSource = catchBlock.body

            //val booleanContainer = BooleanContainer(false)

            var codeSource1 = CodeSource.fromIterable(codeSource)


            codeSource1 = CodeSourceUtil.insertBeforeOrEnd({
                it is ThrowException || it is Return || it is ControlFlow
            }, finallySource, codeSource1)

            visitorGenerator.generateTo(CodeSource::class.java, codeSource1, extraData, null, additional)


            /*if (!booleanContainer.get()) {
                visitorGenerator.generateTo(CodeSource::class.java, finallySource, extraData, null, additional)
            }*/

            additional.exitFrame()

            mv.visitJumpInsn(Opcodes.GOTO, outOfIf)

        }

        mv.visitLabel(endLabel)

        mv.visitLabel(outOfIf)

        // OUT OF --
    }

}