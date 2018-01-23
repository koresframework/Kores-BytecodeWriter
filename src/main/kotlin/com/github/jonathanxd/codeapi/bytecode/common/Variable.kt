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
package com.github.jonathanxd.codeapi.bytecode.common

import com.github.jonathanxd.codeapi.type.`is`
import com.github.jonathanxd.iutils.string.ToStringHelper
import org.objectweb.asm.Label
import java.lang.reflect.Type
import java.util.*

/**
 * Internal class undocumented.
 *
 * isVisible -> If false, this variable will be ignored by get operations.
 */
data class Variable @JvmOverloads constructor(
    val name: String,
    val type: Type,
    val startLabel: Label,
    val endLabel: Label?,
    val isTemp: Boolean = false,
    val isVisible: Boolean = true
) {

    override fun hashCode(): Int =
        Objects.hash(this.name, this.type)

    override fun equals(other: Any?): Boolean =
        other is Variable && other.name == this.name && other.type.`is`(this.type)

    override fun toString(): String {
        return ToStringHelper.defaultHelper(this::class.java.simpleName)
            .add("name", this.name)
            .add("type", this.type)
            .add("isTemp", this.isTemp)
            .add("startLabel", this.startLabel)
            .add("endLabel", this.endLabel)
            .toString()
    }
}
