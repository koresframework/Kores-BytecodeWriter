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
package com.github.jonathanxd.codeapi.bytecode.processor.visitor

import com.github.jonathanxd.codeapi.base.ArgumentHolder
import com.github.jonathanxd.codeapi.bytecode.processor.IN_INVOKE_DYNAMIC
import com.github.jonathanxd.codeapi.bytecode.processor.METHOD_VISITOR
import com.github.jonathanxd.codeapi.bytecode.processor.require
import com.github.jonathanxd.codeapi.bytecode.util.CodePartUtil
import com.github.jonathanxd.codeapi.bytecode.util.InsnUtil
import com.github.jonathanxd.codeapi.factory.cast
import com.github.jonathanxd.codeapi.processor.CodeProcessor
import com.github.jonathanxd.codeapi.processor.Processor
import com.github.jonathanxd.codeapi.util.codeType
import com.github.jonathanxd.iutils.data.TypedData

object ArgumentHolderVisitor : Processor<ArgumentHolder> {

    override fun process(part: ArgumentHolder, data: TypedData, codeProcessor: CodeProcessor<*>) {
        // MUST be retrieved here to avoid the data to be removed too late
        val isInInvokeDynamic = IN_INVOKE_DYNAMIC.getOrNull(data) != null

        val visitor = METHOD_VISITOR.require(data)

        val mv = visitor.methodVisitor

        val types = part.types
        // Try to auto box and unbox
        val arguments = part.arguments.mapIndexed { i, it ->

            // Auto boxing and unboxing disabled for dynamic invocations (like lambdas)
            if(isInInvokeDynamic)
                return@mapIndexed it

            val type = CodePartUtil.getTypeOrNull(it)
            val argType = types[i].codeType

            if (type != null) {
                if (type.isPrimitive && !argType.isPrimitive) {
                    return@mapIndexed cast(type, argType, it)
                } else if (!type.isPrimitive && argType.isPrimitive) {
                    return@mapIndexed cast(type, argType, it)
                }
            }

            return@mapIndexed it
        }

        if (!part.array) {

            for (argument in arguments) {

                codeProcessor.process(argument::class.java, argument, data)
            }
        } else {
            for (i in arguments.indices) {

                InsnUtil.visitInt(i, mv) // Visit index

                val argument = arguments[i]

                codeProcessor.process(argument::class.java, argument, data)

            }
        }

    }

}