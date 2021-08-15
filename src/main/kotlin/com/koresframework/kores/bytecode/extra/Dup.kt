/*
 *      Kores-BytecodeWriter - Translates Kores Structure to JVM Bytecode <https://github.com/JonathanxD/CodeAPI-BytecodeWriter>
 *
 *         The MIT License (MIT)
 *
 *      Copyright (c) 2021 TheRealBuggy/JonathanxD (https://github.com/JonathanxD/) <jonathan.scripter@programmer.net>
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
package com.koresframework.kores.bytecode.extra

import com.koresframework.kores.Instruction
import com.koresframework.kores.KoresPart
import com.koresframework.kores.base.Typed
import com.koresframework.kores.typeOrNull
import java.lang.reflect.Type

/**
 * CodeAPI-BytecodeWriter Dup part. This part will dup result of [part].
 */
data class Dup(val part: KoresPart, override val type: Type) : Typed, Instruction {

    constructor(part: Typed) : this(part, part.type)

    constructor(part: KoresPart) : this(part, part.typeOrNull ?: Any::class.java)

    override fun builder(): Builder = Builder(this)

    class Builder() : Typed.Builder<Dup, Builder> {

        lateinit var part: KoresPart
        lateinit var type: Type

        constructor(defaults: Dup) : this() {
            this.part = defaults.part
            this.type = defaults.type
        }

        fun part(value: KoresPart): Builder {
            this.part = value
            return this
        }

        override fun type(value: Type): Builder {
            this.type = value
            return this
        }

        override fun build(): Dup = Dup(this.part, this.type)

        companion object {
            @JvmStatic
            fun builder() = Builder()
        }
    }

}