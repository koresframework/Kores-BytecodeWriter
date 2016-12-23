/*
 *      CodeAPI-BytecodeWriter - Framework to generate Java code and Bytecode code. <https://github.com/JonathanxD/CodeAPI-BytecodeWriter>
 *
 *         The MIT License (MIT)
 *
 *      Copyright (c) 2016 TheRealBuggy/JonathanxD (https://github.com/JonathanxD/ & https://github.com/TheRealBuggy/) <jonathan.scripter@programmer.net>
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

import com.github.jonathanxd.codeapi.bytecode.BytecodeClass
import com.github.jonathanxd.codeapi.bytecode.common.MVData
import com.github.jonathanxd.codeapi.bytecode.util.CodeTypeUtil
import com.github.jonathanxd.codeapi.bytecode.util.InsnUtil
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.gen.visit.VoidVisitor
import com.github.jonathanxd.codeapi.interfaces.Argumenterizable
import com.github.jonathanxd.iutils.data.MapData
import com.github.jonathanxd.iutils.optional.Require
import org.objectweb.asm.Opcodes

object ArgumenterizabeVisitor : VoidVisitor<Argumenterizable, BytecodeClass, MVData> {

    override fun voidVisit(argumenterizable: Argumenterizable, extraData: MapData, visitorGenerator: VisitorGenerator<BytecodeClass>, additional: MVData) {
        val mv = additional.methodVisitor

        val arguments = argumenterizable.arguments

        if (!argumenterizable.isArray) {

            for (argument in arguments) {
                val value = Require.require(argument.value)

                visitorGenerator.generateTo(value.javaClass, value, extraData, null, additional)

                if (argument.isCasted) {
                    mv.visitTypeInsn(Opcodes.CHECKCAST, CodeTypeUtil.codeTypeToSimpleAsm(Require.require(argument.type)))
                }
            }
        } else {
            for (i in arguments.indices) {

                InsnUtil.visitInt(i, mv) // Visit index

                val argument = arguments[i]

                val value = Require.require(argument.value)

                visitorGenerator.generateTo(value.javaClass, value, extraData, null, additional)

                if (argument.isCasted) {
                    mv.visitTypeInsn(Opcodes.CHECKCAST, CodeTypeUtil.codeTypeToSimpleAsm(Require.require(argument.type)))
                }
            }
        }
    }

}