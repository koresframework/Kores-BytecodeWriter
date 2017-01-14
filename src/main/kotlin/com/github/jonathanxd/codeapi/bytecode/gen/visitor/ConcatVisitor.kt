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
import com.github.jonathanxd.codeapi.Types
import com.github.jonathanxd.codeapi.base.Concat
import com.github.jonathanxd.codeapi.base.MethodInvocation
import com.github.jonathanxd.codeapi.bytecode.BytecodeClass
import com.github.jonathanxd.codeapi.bytecode.common.MVData
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.gen.visit.VoidVisitor
import com.github.jonathanxd.iutils.data.MapData


object ConcatVisitor : VoidVisitor<Concat, BytecodeClass, MVData> {

    override fun voidVisit(t: Concat, extraData: MapData, visitorGenerator: VisitorGenerator<BytecodeClass>, additional: MVData) {
        val concatenations = t.concatenations

        val first = if (concatenations.isEmpty()) null else concatenations[0]

        if (first != null) {

            if (concatenations.size == 1) {

                visitorGenerator.generateTo(first.javaClass, first, extraData, additional)

            } else if (concatenations.size == 2) {

                val stringConcat = CodeAPI.invokeVirtual(String::class.java, first, "concat",
                        CodeAPI.typeSpec(String::class.java, String::class.java),
                        listOf(CodeAPI.argument(concatenations[1])))

                visitorGenerator.generateTo(MethodInvocation::class.java, stringConcat, extraData, additional)
            } else {

                var strBuilder = CodeAPI.invokeConstructor(
                        Types.STRING_BUILDER,
                        CodeAPI.constructorTypeSpec(String::class.java),
                        listOf(CodeAPI.argument(first))
                )

                (1..concatenations.size - 1)
                        .map { concatenations[it] }
                        .forEach {
                            strBuilder = CodeAPI.invokeVirtual(Types.STRING_BUILDER, strBuilder, "append", CodeAPI.typeSpec(Types.STRING_BUILDER, Types.STRING), listOf(CodeAPI.argument(it)))
                        }

                strBuilder = CodeAPI.invokeVirtual(Types.OBJECT, strBuilder, "toString", CodeAPI.typeSpec(Types.STRING), emptyList())

                visitorGenerator.generateTo(MethodInvocation::class.java, strBuilder, extraData, additional)
            }
        } else {
            // If the concatenations is empty
            // It is better to CodeAPI (less things to process), and is better to JVM.
            additional.methodVisitor.visitLdcInsn("")
        }

    }

}