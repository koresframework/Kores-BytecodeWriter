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

import com.github.jonathanxd.codeapi.bytecode.common.Variable
import com.github.jonathanxd.codeapi.common.CodeParameter
import org.objectweb.asm.Label

object ParameterUtil {

    fun parametersToVars(parameters: Collection<CodeParameter>, label: Label): List<Variable> {
        if (parameters.isEmpty())
            return emptyList()

        return parameters.map { d -> Variable(d.name, d.type, label, null) }
    }

    fun parametersToVars(parameters: Collection<CodeParameter>, target: MutableCollection<Variable>, label: Label) {
        if (parameters.isEmpty())
            return

        parameters.map { d -> Variable(d.name, d.type, label, null) }.forEach { target.add(it) }
    }

    fun parametersToMap(parameters: Collection<CodeParameter>, startAt: Int): Map<String, Int> {
        @Suppress("NAME_SHADOWING")
        var startAt = startAt

        if (parameters.isEmpty())
            return emptyMap()

        val map = java.util.HashMap<String, Int>()

        for (parameter in parameters) {
            map.put(parameter.name, startAt)
            ++startAt
        }

        return map
    }

}