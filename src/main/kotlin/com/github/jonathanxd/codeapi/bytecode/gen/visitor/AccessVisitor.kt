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

import com.github.jonathanxd.codeapi.base.Access
import com.github.jonathanxd.codeapi.bytecode.BytecodeClass
import com.github.jonathanxd.codeapi.bytecode.common.MVData
import com.github.jonathanxd.codeapi.common.Data
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.gen.visit.VoidVisitor
import org.objectweb.asm.Opcodes

object AccessVisitor : VoidVisitor<Access, BytecodeClass, MVData> {

    override fun voidVisit(t: Access, extraData: Data, visitorGenerator: VisitorGenerator<BytecodeClass>, additional: MVData) {
        val visitor = additional.methodVisitor

        when (t.type) {
            Access.Type.LOCAL, Access.Type.STATIC -> {
            }
            Access.Type.THIS, Access.Type.SUPER -> {
                visitor.visitVarInsn(Opcodes.ALOAD, 0)
            }
            Access.Type.OUTER -> {
                val localization = t.localization ?: throw NullPointerException("Localization is required to OUTER Access.")
                val part = Util.accessEnclosingClass(extraData, localization) ?: throw IllegalArgumentException("Cannot access \"outer class\" '$localization'.")

                visitorGenerator.generateTo(part::class.java, part, extraData, additional)
            }
            else -> {
                throw IllegalArgumentException("Cannot handle access of type '${t.type}'")
            }
        }
    }

}