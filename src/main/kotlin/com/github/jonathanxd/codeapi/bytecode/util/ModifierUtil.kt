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

import com.github.jonathanxd.codeapi.base.CodeModifier
import com.github.jonathanxd.codeapi.base.TypeDeclaration
import org.objectweb.asm.Opcodes

object ModifierUtil {

    var CLASS = 0
    var FIELD = 1
    var METHOD = 2
    var PARAMETER = 3

    fun modifiersToAsm(typeDeclaration: TypeDeclaration): Int {
        val modifiers = java.util.ArrayList(typeDeclaration.modifiers)

        if (modifiers.contains(CodeModifier.STATIC))
            modifiers.remove(CodeModifier.STATIC)

        return (if (!typeDeclaration.isInterface) Opcodes.ACC_SUPER else 0) + ModifierUtil.modifiersToAsm(modifiers, typeDeclaration.isInterface)
    }

    fun modifiersToAsm(codeModifiers: Collection<CodeModifier>): Int {
        return ModifierUtil.toAsmAccess(codeModifiers)
    }

    fun modifiersToAsm(codeModifiers: Collection<CodeModifier>, isInterface: Boolean): Int {
        return (if (isInterface) Opcodes.ACC_ABSTRACT + Opcodes.ACC_INTERFACE else 0) + ModifierUtil.toAsmAccess(codeModifiers)
    }

    fun innerModifiersToAsm(typeDeclaration: TypeDeclaration): Int {
        val modifiers = typeDeclaration.modifiers.let {
            if (!it.contains(CodeModifier.STATIC))
                it + CodeModifier.STATIC
            else it
        }

        return ModifierUtil.modifiersToAsm(modifiers, typeDeclaration.isInterface)
    }

    fun isClassOrMethod(elementType: Int): Boolean {
        return elementType == CLASS || elementType == METHOD
    }

    fun isClassOrField(elementType: Int): Boolean {
        return elementType == CLASS || elementType == FIELD
    }

    fun isClassFieldOrMethod(elementType: Int): Boolean {
        return elementType == CLASS || elementType == FIELD || elementType == METHOD
    }

    fun isClassFieldMethodOrParameter(elementType: Int): Boolean {
        return elementType == CLASS || elementType == METHOD || elementType == FIELD || elementType == PARAMETER
    }

    /**
     * Convert a [CodeModifier] to asm modifiers
     *
     * @param codeModifier Modifier to convert
     * @return ASM modifiers flags
     */
    fun toAsmAccess(codeModifier: CodeModifier): Int {
        return when (codeModifier) {
            CodeModifier.DEFAULT -> Opcodes.ACC_ABSTRACT
            CodeModifier.ABSTRACT -> Opcodes.ACC_ABSTRACT
            CodeModifier.FINAL -> Opcodes.ACC_FINAL
            CodeModifier.NATIVE -> Opcodes.ACC_NATIVE
            CodeModifier.PRIVATE -> Opcodes.ACC_PRIVATE
            CodeModifier.PROTECTED -> Opcodes.ACC_PROTECTED
            CodeModifier.PUBLIC -> Opcodes.ACC_PUBLIC
            CodeModifier.STATIC -> Opcodes.ACC_STATIC
            CodeModifier.STRICTFP -> Opcodes.ACC_STRICT
            CodeModifier.SYNCHRONIZED -> Opcodes.ACC_SYNCHRONIZED
            CodeModifier.TRANSIENT -> Opcodes.ACC_TRANSIENT
            CodeModifier.VOLATILE -> Opcodes.ACC_VOLATILE
            CodeModifier.BRIDGE -> Opcodes.ACC_BRIDGE
            CodeModifier.VARARGS -> Opcodes.ACC_VARARGS
            CodeModifier.SYNTHETIC -> Opcodes.ACC_SYNTHETIC
            CodeModifier.ANNOTATION -> Opcodes.ACC_ANNOTATION
            CodeModifier.ENUM -> Opcodes.ACC_ENUM
            CodeModifier.MANDATED -> Opcodes.ACC_MANDATED
            else -> 0
        }
    }

    /**
     * Convert [CodeModifier]s to asm modifiers
     *
     * @param modifiers Modifiers to convert
     * @return ASM modifiers flags
     */
    fun toAsmAccess(modifiers: Collection<CodeModifier>): Int {
        var end = 0

        if (modifiers.isEmpty())
            return Opcodes.ACC_PUBLIC

        for (modifier in modifiers) {
            val toAsmAccess = toAsmAccess(modifier)

            if (toAsmAccess != 0) {
                end += toAsmAccess
            }
        }

        return end
    }

}