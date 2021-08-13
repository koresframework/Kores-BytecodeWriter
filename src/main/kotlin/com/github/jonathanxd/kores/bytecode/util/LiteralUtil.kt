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
package com.github.jonathanxd.kores.bytecode.util

import com.github.jonathanxd.kores.Types
import com.github.jonathanxd.kores.common.Stack
import com.github.jonathanxd.kores.literal.Literal
import com.github.jonathanxd.kores.literal.Literals
import com.github.jonathanxd.kores.type.KoresType
import com.github.jonathanxd.kores.type.`is`
import com.github.jonathanxd.kores.util.typeDesc
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type

object LiteralUtil {

    fun visitLiteral(num: Literal, mv: MethodVisitor) {
        val value = num.value

        if (num == Stack)
            return

        if (num === Literals.NULL) {

            mv.visitInsn(Opcodes.ACONST_NULL)

        } else if (num === Literals.TRUE) {

            mv.visitInsn(Opcodes.ICONST_1)

        } else if (num === Literals.FALSE) {

            mv.visitInsn(Opcodes.ICONST_0)

        } else if (num.type.`is`(Types.STRING)) {

            mv.visitLdcInsn((value as String).substring(1, value.length - 1))

        } else if (num.type.`is`(Types.INT) && num !is Literals.ByteLiteral) {

            InsnUtil.visitInt(value as Int, mv)

        } else if (num.type.`is`(Types.LONG)) {

            InsnUtil.visitLong(value as Long, mv)

        } else if (num.type.`is`(Types.DOUBLE)) {

            InsnUtil.visitDouble(value as Double, mv)

        } else if (num is Literals.ByteLiteral) {

            mv.visitIntInsn(Opcodes.BIPUSH, (value as Byte).toInt())

        } else if (num.type.`is`(Types.CHAR)) {

            mv.visitIntInsn(Opcodes.BIPUSH, (value as Char).code)

        } else if (num.type.`is`(Types.FLOAT)) {

            InsnUtil.visitFloat(value as Float, mv)

        } else if (num.type.`is`(Types.KORES_TYPE)) {

            val type = num.value as KoresType

            if (type.isPrimitive) {
                val wrapperType = type.wrapperType
                        ?: throw IllegalArgumentException("Primitive type '$type' has no wrapper version.")

                val wrapperTypeSpec = wrapperType.internalName
                val classType = Types.CLASS.typeDesc

                mv.visitFieldInsn(Opcodes.GETSTATIC, wrapperTypeSpec, "TYPE", classType)
            } else {
                val asmType = Type.getType(type.javaSpecName)
                mv.visitLdcInsn(asmType)
            }
        } else {
            throw IllegalArgumentException("Cannot handle literal: $num")
        }
    }

}