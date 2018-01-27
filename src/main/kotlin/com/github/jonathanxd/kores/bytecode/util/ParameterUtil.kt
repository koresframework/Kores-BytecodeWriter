/*
 *      Kores-BytecodeWriter - Translates CodeAPI Structure to JVM Bytecode <https://github.com/JonathanxD/CodeAPI-BytecodeWriter>
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
package com.github.jonathanxd.kores.bytecode.util

import com.github.jonathanxd.kores.base.KoresParameter
import com.github.jonathanxd.kores.bytecode.common.Variable
import org.objectweb.asm.Label

object ParameterUtil {

    fun parametersToVars(parameters: Collection<KoresParameter>, label: Label): List<Variable> {
        if (parameters.isEmpty())
            return emptyList()

        return parameters.map { Variable(it.name, it.type, label, null) }
    }

    fun parametersToVars(
        parameters: Collection<KoresParameter>,
        target: MutableCollection<Variable>,
        label: Label
    ) {
        if (parameters.isEmpty())
            return

        parameters.mapTo(target) { Variable(it.name, it.type, label, null) }
    }

    fun parametersToMap(parameters: Collection<KoresParameter>, startAt: Int): Map<String, Int> {
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