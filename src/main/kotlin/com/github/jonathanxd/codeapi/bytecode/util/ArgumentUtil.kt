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
package com.github.jonathanxd.codeapi.bytecode.util

import com.github.jonathanxd.codeapi.CodePart
import com.github.jonathanxd.codeapi.base.impl.CastImpl
import com.github.jonathanxd.codeapi.util.TypeResolver
import com.github.jonathanxd.iutils.description.Description

object ArgumentUtil {

    fun createArguments(description: Description, arguments: List<CodePart>, typeResolver: TypeResolver): List<CodePart> {
        val parameterTypes = description.parameterTypes

        if (parameterTypes.size != arguments.size)
            throw IllegalArgumentException("Parameter types size doesn't matches arguments size.")

        val codeArgumentList = java.util.ArrayList<CodePart>()

        for (i in parameterTypes.indices) {
            val parameterTypeStr = parameterTypes[i]
            val parameterType = typeResolver.resolveUnknown(parameterTypeStr)

            val codePart = arguments[i]

            codeArgumentList.add(CastImpl(null, parameterType, codePart))

        }

        return codeArgumentList
    }

}