/*
 *      CodeAPI-BytecodeWriter - Framework to generate Java code and Bytecode code. <https://github.com/JonathanxD/CodeAPI-BytecodeWriter>
 *
 *         The MIT License (MIT)
 *
 *      Copyright (c) 2016 TheRealBuggy/JonathanxD (https://github.com/JonathanxD/ & https://github.com/TheRealBuggy/) <jonathan.scripter@programmer.net>
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

import com.github.jonathanxd.codeapi.literals.Literal
import com.github.jonathanxd.codeapi.literals.Literals
import com.github.jonathanxd.codeapi.util.Stack
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type

object LiteralUtil {

    fun visitLiteral(num: Literal, mv: MethodVisitor) {
        val name = num.name

        if (num == Stack.INSTANCE)
            return

        if (num === Literals.NULL) {

            mv.visitInsn(Opcodes.ACONST_NULL)

        } else if (num === Literals.TRUE) {

            mv.visitInsn(Opcodes.ICONST_1)

        } else if (num === Literals.FALSE) {

            mv.visitInsn(Opcodes.ICONST_0)

        } else if (num is Literals.QuotedStringLiteral) {

            mv.visitLdcInsn(name.substring(1, name.length - 1))

        } else if (num is Literals.ShortLiteral) {

            InsnUtil.visitInt(Integer.parseInt(name), mv)

        } else if (num is Literals.IntLiteral) {

            InsnUtil.visitInt(Integer.parseInt(name), mv)

        } else if (num is Literals.LongLiteral) {

            InsnUtil.visitLong(java.lang.Long.parseLong(name), mv)

        } else if (num is Literals.DoubleLiteral) {

            InsnUtil.visitDouble(java.lang.Double.parseDouble(name), mv)

        } else if (num is Literals.ByteLiteral) {

            mv.visitIntInsn(Opcodes.BIPUSH, java.lang.Byte.parseByte(name).toInt())

        } else if (num is Literals.CharLiteral) {

            mv.visitIntInsn(Opcodes.BIPUSH, name[0].toInt())

        } else if (num is Literals.FloatLiteral) {

            InsnUtil.visitFloat(java.lang.Float.parseFloat(name), mv)

        } else if (num is Literals.ClassLiteral) {

            val type = Type.getType(num.type.javaSpecName)

            mv.visitLdcInsn(type)
        }
    }

}