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
package com.koresframework.kores.bytecode.util

import com.koresframework.kores.Types
import com.koresframework.kores.base.ConstructorsHolder
import com.koresframework.kores.base.EnumDeclaration
import com.koresframework.kores.base.InnerTypesHolder
import com.koresframework.kores.base.TypeDeclaration
import com.koresframework.kores.type.canonicalName
import com.koresframework.kores.type.isArray
import com.koresframework.kores.type.isPrimitive
import com.koresframework.kores.type.javaSpecName
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type

typealias ReflectType = java.lang.reflect.Type

object CodeTypeUtil {

    /*
    int I2L = 133; // visitInsn
    int I2F = 134; // -
    int I2D = 135; // -
    int L2I = 136; // -
    int L2F = 137; // -
    int L2D = 138; // -
    int F2I = 139; // -
    int F2L = 140; // -
    int F2D = 141; // -
    int D2I = 142; // -
    int D2L = 143; // -
    int D2F = 144; // -
    int I2B = 145; // -
    int I2C = 146; // -
    int I2S = 147; // -
     */
    fun getOpcode(from: Char, to: Char): Int {

        return when (from) {
            'I' -> when (to) {
                'L' -> Opcodes.I2L
                'F' -> Opcodes.I2F
                'D' -> Opcodes.I2D
                'B' -> Opcodes.I2B
                'C' -> Opcodes.I2C
                'S' -> Opcodes.I2S
                else -> -1
            }
            'L' -> when (to) {
                'I' -> Opcodes.L2I
                'F' -> Opcodes.L2F
                'D' -> Opcodes.L2D
                else -> -1
            }
            'F' -> when (to) {
                'I' -> Opcodes.F2I
                'L' -> Opcodes.F2L
                'D' -> Opcodes.F2D
                else -> -1
            }
            'D' -> when (to) {
                'I' -> Opcodes.D2I
                'L' -> Opcodes.D2L
                'F' -> Opcodes.D2F
                else -> -1
            }
            else -> -1
        }
    }

    fun getOpcodeForType(type: ReflectType, opcode: Int): Int {
        val asmType = Type.getType(type.javaSpecName)

        return asmType.getOpcode(opcode)
    }

    fun convertToPrimitive(from: ReflectType, to: ReflectType, mv: MethodVisitor) {
        var opcode = -1

        if (!from.isArray && !to.isArray && from.isPrimitive && to.isPrimitive) {
            var fromTypeChar = Character.toUpperCase(from.canonicalName[0])
            val toTypeChar = Character.toUpperCase(to.canonicalName[0])

            // Fixes Short, Byte & Char conversion to int
            if (fromTypeChar == 'S' || fromTypeChar == 'B' || fromTypeChar == 'C') {
                if (toTypeChar == 'I')
                    return
                else
                    fromTypeChar = 'I'
            }

            opcode = this.getOpcode(fromTypeChar, toTypeChar)

            if (opcode == -1) {
                if (this.getOpcode(fromTypeChar, 'I') == -1 || this.getOpcode(
                            'I',
                            toTypeChar
                        ) == -1
                ) {
                    throw IllegalArgumentException("Can't cast from '$from' to '$to'.")
                }
                CodeTypeUtil.convertToPrimitive(from, Types.INT, mv)
                CodeTypeUtil.convertToPrimitive(Types.INT, to, mv)
                return
            }

        }
        if (opcode != -1)
            mv.visitInsn(opcode)
        else
            throw IllegalArgumentException("Cannot cast '$from' to '$to'!")
    }

}


fun TypeDeclaration.allInnerTypes(): List<TypeDeclaration> {
    val list = mutableListOf<TypeDeclaration>()
    return this.allInnerTypes(list)
}

private fun TypeDeclaration.allInnerTypes(list: MutableList<TypeDeclaration>): List<TypeDeclaration> {
    val func = { it: InnerTypesHolder ->
        list.addAll(it.innerTypes)
        it.innerTypes.forEach {
            it.allInnerTypes(list)
        }
    }

    func(this)

    if (this is ConstructorsHolder) {
        this.constructors.forEach {
            func(it)
        }
    }

    this.methods.forEach {
        func(it)
    }

    this.fields.forEach {
        func(it)
    }

    func(this.staticBlock)

    if (this is EnumDeclaration) {
        this.entries.forEach {
            func(it)
        }
    }

    return list
}

fun TypeDeclaration.allTypes(): List<TypeDeclaration> {
    val list = mutableListOf<TypeDeclaration>()
    return this.allTypes(list)
}

private fun TypeDeclaration.allTypes(list: MutableList<TypeDeclaration>): List<TypeDeclaration> {
    val func = { it: InnerTypesHolder ->
        (it as? TypeDeclaration)?.let { list.add(it) }
        it.innerTypes.forEach {
            it.allTypes(list)
        }
    }

    func(this)

    if (this is ConstructorsHolder) {
        this.constructors.forEach {
            func(it)
        }
    }

    this.methods.forEach {
        func(it)
    }

    this.fields.forEach {
        func(it)
    }

    func(this.staticBlock)

    if (this is EnumDeclaration) {
        this.entries.forEach {
            func(it)
        }
    }

    return list
}