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
package com.github.jonathanxd.codeapi.bytecode.extra

import com.github.jonathanxd.codeapi.CodePart
import com.github.jonathanxd.codeapi.base.Typed
import com.github.jonathanxd.codeapi.type.CodeType

/**
 * CodeAPI-BytecodeWriter Dup part. This part will dup result of [part].
 */
data class Dup(val part: CodePart, override val type: CodeType) : Typed {

    constructor(part: Typed) : this(part, part.type)

    override fun builder(): Builder = Builder(this)

    class Builder() : Typed.Builder<Dup, Builder> {

        lateinit var part: CodePart
        lateinit var type: CodeType

        constructor(defaults: Dup) : this() {
            this.part = defaults.part
            this.type = defaults.type
        }

        fun withPart(value: CodePart): Builder {
            this.part = value
            return this
        }

        override fun withType(value: CodeType): Builder {
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