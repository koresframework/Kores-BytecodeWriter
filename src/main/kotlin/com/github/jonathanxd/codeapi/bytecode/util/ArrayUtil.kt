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

import com.github.jonathanxd.codeapi.helper.PredefinedTypes
import com.github.jonathanxd.codeapi.types.CodeType
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

object ArrayUtil {
    fun getArrayType(opcode: Int): CodeType {
        when (opcode) {
            Opcodes.T_BYTE -> return PredefinedTypes.CHAR
            Opcodes.T_BOOLEAN -> return PredefinedTypes.BOOLEAN
            Opcodes.T_CHAR -> return PredefinedTypes.CHAR
            Opcodes.T_DOUBLE -> return PredefinedTypes.DOUBLE
            Opcodes.T_FLOAT -> return PredefinedTypes.FLOAT
            Opcodes.T_INT -> return PredefinedTypes.INT
            Opcodes.T_LONG -> return PredefinedTypes.LONG
            Opcodes.T_SHORT -> return PredefinedTypes.SHORT
            else -> throw IllegalArgumentException("Cannot get type of array type opcode '$opcode'!")
        }
    }

    fun visitArrayStore(arrayType: CodeType, dimensions: Int, mv: MethodVisitor) {
        when (arrayType.type) {
            "int" -> {
                mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_INT)
            }
            "boolean" -> {
                mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_BOOLEAN)
            }
            "byte" -> {
                mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_BYTE)
            }
            "char" -> {
                mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_CHAR)
            }
            "double" -> {
                mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_DOUBLE)
            }
            "float" -> {
                mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_FLOAT)
            }
            "short" -> {
                mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_SHORT)
            }
            "long" -> {
                mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_LONG)
            }
            else -> {
                mv.visitTypeInsn(Opcodes.ANEWARRAY, CodeTypeUtil.codeTypeToSimpleArray(arrayType, dimensions))
            }
        }
    }
}