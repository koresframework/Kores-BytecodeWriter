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
import com.github.jonathanxd.codeapi.base.IfStatement
import com.github.jonathanxd.codeapi.bytecode.BytecodeClass
import com.github.jonathanxd.codeapi.bytecode.common.Flow
import com.github.jonathanxd.codeapi.bytecode.common.MVData
import com.github.jonathanxd.codeapi.common.Data
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.gen.visit.VoidVisitor
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes

object IfStatementVisitor : VoidVisitor<IfStatement, BytecodeClass, MVData> {

    override fun voidVisit(t: IfStatement, extraData: Data, visitorGenerator: VisitorGenerator<BytecodeClass>, additional: MVData) {
        val startIfLabel = Label()
        val ifBody = Label()
        val endIfLabel = Label()

        val elseLabel = Label()

        val elseStatement = t.elseStatement

        val jumpLabel = if(t is SwitchVisitor.SwitchIfStatement) extraData.getRequired<Flow>(ConstantDatas.FLOW_TYPE_INFO).insideEnd else if(elseStatement.isNotEmpty) elseLabel else endIfLabel

        val methodVisitor = additional.methodVisitor

        methodVisitor.visitLabel(startIfLabel)

        visit(t.expressions, startIfLabel, ifBody, jumpLabel, false, extraData, visitorGenerator, additional)

        val body = t.body

        methodVisitor.visitLabel(ifBody)

        additional.enterNewFrame()

        visitorGenerator.generateTo(CodeSource::class.java, body, extraData, null, additional)

        additional.exitFrame()

        if (elseStatement.isNotEmpty) {
            methodVisitor.visitJumpInsn(Opcodes.GOTO, endIfLabel)
        }


        if (elseStatement.isNotEmpty) {
            methodVisitor.visitLabel(elseLabel)

            additional.enterNewFrame()

            visitorGenerator.generateTo(CodeSource::class.java, elseStatement, extraData, additional)

            additional.exitFrame()
        }

        methodVisitor.visitLabel(endIfLabel)
    }

}