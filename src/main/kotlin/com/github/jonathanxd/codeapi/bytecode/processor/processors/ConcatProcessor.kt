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

import com.github.jonathanxd.codeapi.Types
import com.github.jonathanxd.codeapi.base.Concat
import com.github.jonathanxd.codeapi.base.MethodInvocation
import com.github.jonathanxd.codeapi.bytecode.processor.IN_EXPRESSION
import com.github.jonathanxd.codeapi.bytecode.processor.METHOD_VISITOR
import com.github.jonathanxd.codeapi.bytecode.processor.incrementInContext
import com.github.jonathanxd.codeapi.factory.constructorTypeSpec
import com.github.jonathanxd.codeapi.factory.invokeConstructor
import com.github.jonathanxd.codeapi.factory.invokeVirtual
import com.github.jonathanxd.codeapi.factory.typeSpec
import com.github.jonathanxd.codeapi.literal.Literals
import com.github.jonathanxd.codeapi.processor.Processor
import com.github.jonathanxd.codeapi.processor.ProcessorManager
import com.github.jonathanxd.codeapi.safeForComparison
import com.github.jonathanxd.iutils.data.TypedData
import com.github.jonathanxd.iutils.kt.require


object ConcatProcessor : Processor<Concat> {

    override fun process(part: Concat, data: TypedData, processorManager: ProcessorManager<*>) {
        val concatenations = part.concatenations

        val first = if (concatenations.isEmpty()) null else concatenations[0]

        val allIsEmpty = concatenations.isNotEmpty() && concatenations.all {
            val safe =
                it.safeForComparison; safe is Literals.StringLiteral && safe.original.isEmpty()
        }

        if (first != null && !allIsEmpty) {

            if (concatenations.size == 1) {
                IN_EXPRESSION.incrementInContext(data) {
                    processorManager.process(first::class.java, first, data)
                }

            } else if (concatenations.size == 2) {

                val stringConcat = invokeVirtual(
                    String::class.java, first, "concat",
                    typeSpec(String::class.java, String::class.java),
                    listOf(concatenations[1])
                )

                processorManager.process(MethodInvocation::class.java, stringConcat, data)
            } else {

                var strBuilder = Types.STRING_BUILDER.invokeConstructor(
                    constructorTypeSpec(String::class.java),
                    listOf(first)
                )

                (1..concatenations.size - 1)
                    .map { concatenations[it] }
                    .forEach {
                        strBuilder = invokeVirtual(
                            Types.STRING_BUILDER,
                            strBuilder,
                            "append",
                            typeSpec(Types.STRING_BUILDER, Types.STRING),
                            listOf(it)
                        )
                    }

                strBuilder = invokeVirtual(
                    Types.OBJECT,
                    strBuilder,
                    "toString",
                    typeSpec(Types.STRING),
                    emptyList()
                )

                processorManager.process(MethodInvocation::class.java, strBuilder, data)
            }
        } else {
            // If the concatenations is empty or if all concat is empty only put a empty string on stack
            // It is better to CodeAPI (less things to process), and is better to JVM.
            METHOD_VISITOR.require(data).methodVisitor.visitLdcInsn("")
        }

    }


}