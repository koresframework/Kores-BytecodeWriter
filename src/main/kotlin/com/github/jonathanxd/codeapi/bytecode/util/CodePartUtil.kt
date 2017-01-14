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
import com.github.jonathanxd.codeapi.base.Typed
import com.github.jonathanxd.codeapi.literal.Literal
import com.github.jonathanxd.codeapi.literal.Literals
import com.github.jonathanxd.codeapi.type.CodeType
import com.github.jonathanxd.codeapi.util.CodePartUtil as BaseCodePartUtil

object CodePartUtil {

    fun isPrimitive(codePart: CodePart): Boolean {
        if (codePart is Literal) {
            return Literals.isPrimitive(codePart)
        } else if (codePart is Typed) {

            return (codePart.type ?: throw RuntimeException("Cannot determine type of '$codePart'")).isPrimitive
        } else {
            throw RuntimeException("Cannot determine type of part '$codePart'!")
        }

    }

    fun getTypeOrNull(codePart: CodePart): CodeType? = BaseCodePartUtil.getTypeOrNull(codePart)

    fun getType(codePart: CodePart): CodeType = BaseCodePartUtil.getType(codePart)

    fun isBoolean(part: CodePart): Boolean {
        return part is Literals.BoolLiteral
    }

    fun getBooleanValue(part: CodePart): Boolean {
        return (part as Literals.BoolLiteral).value
    }

}