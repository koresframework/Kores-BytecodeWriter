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
import com.github.jonathanxd.codeapi.MutableCodeSource
import com.github.jonathanxd.codeapi.base.ForStatement
import com.github.jonathanxd.codeapi.base.IfStatement
import com.github.jonathanxd.codeapi.base.impl.IfStatementImpl
import com.github.jonathanxd.codeapi.bytecode.BytecodeClass
import com.github.jonathanxd.codeapi.bytecode.common.Flow
import com.github.jonathanxd.codeapi.bytecode.common.MVData
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.gen.visit.VoidVisitor
import com.github.jonathanxd.iutils.data.MapData
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes

object ForIVisitor : VoidVisitor<ForStatement, BytecodeClass, MVData> {

    override fun voidVisit(t: ForStatement, extraData: MapData, visitorGenerator: VisitorGenerator<BytecodeClass>, additional: MVData) {
        val mv = additional.methodVisitor

        val outsideStart = Label()
        val whileStart = Label()
        val whileEnd = Label()
        val outsideEnd = Label()

        mv.visitLabel(outsideStart)

        val init = t.forInit

        if (init != null) {
            visitorGenerator.generateTo(init.javaClass, init, extraData, null, additional)
        }


        val source = MutableCodeSource()

        source.addAll(t.body)

        val ifStatement = IfStatementImpl(t.forExpression, source, CodeSource.empty())

        mv.visitLabel(whileStart)

        val flow = Flow(null, outsideStart, whileStart, whileEnd, outsideEnd)

        extraData.registerData(ConstantDatas.FLOW_TYPE_INFO, flow)

        val instructionCodePart = InstructionCodePart.create { _, _, _, additional ->
            mv.visitLabel(whileEnd)
            val update = t.forUpdate

            if (update != null) {
                visitorGenerator.generateTo(update.javaClass, update, extraData, null, additional)
            }

            mv.visitJumpInsn(Opcodes.GOTO, whileStart)
        }

        source.add(instructionCodePart)

        visitorGenerator.generateTo(IfStatement::class.java, ifStatement, extraData, null, additional)

        extraData.unregisterData(ConstantDatas.FLOW_TYPE_INFO, flow)

        mv.visitLabel(outsideEnd)
    }

}