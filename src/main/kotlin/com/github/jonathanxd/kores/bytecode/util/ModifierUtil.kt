/*
 *      Kores-BytecodeWriter - Translates Kores Structure to JVM Bytecode <https://github.com/JonathanxD/CodeAPI-BytecodeWriter>
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

import com.github.jonathanxd.kores.base.KoresModifier
import com.github.jonathanxd.kores.base.ModifierType
import com.github.jonathanxd.kores.base.TypeDeclaration
import org.objectweb.asm.Opcodes

object ModifierUtil {

    var CLASS = 0
    var FIELD = 1
    var METHOD = 2
    var PARAMETER = 3

    fun modifiersToAsm(typeDeclaration: TypeDeclaration): Int {
        val modifiers = typeDeclaration.modifiers.toMutableList()

        if (modifiers.contains(KoresModifier.STATIC))
            modifiers.remove(KoresModifier.STATIC)

        return (if (!typeDeclaration.isInterface) Opcodes.ACC_SUPER else 0) + ModifierUtil.modifiersToAsm(
            modifiers,
            typeDeclaration.isInterface
        )
    }

    fun modifiersToAsm(codeModifiers: Collection<KoresModifier>): Int {
        return ModifierUtil.toAsmAccess(codeModifiers)
    }

    fun modifiersToAsm(codeModifiers: Collection<KoresModifier>, isInterface: Boolean): Int {
        return (if (isInterface) Opcodes.ACC_ABSTRACT + Opcodes.ACC_INTERFACE else 0) + ModifierUtil.toAsmAccess(
            codeModifiers
        )
    }

    fun innerModifiersToAsm(typeDeclaration: TypeDeclaration): Int {
        val modifiers = typeDeclaration.modifiers/*.let {
            if (!it.contains(CodeModifier.STATIC))
                it + CodeModifier.STATIC // TODO: Remove
            else it
        }*/

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
    fun toAsmAccess(codeModifier: KoresModifier): Int {
        return when (codeModifier) {
            KoresModifier.DEFAULT -> 0
            KoresModifier.ABSTRACT -> Opcodes.ACC_ABSTRACT
            KoresModifier.FINAL -> Opcodes.ACC_FINAL
            KoresModifier.NATIVE -> Opcodes.ACC_NATIVE
            KoresModifier.PRIVATE -> Opcodes.ACC_PRIVATE
            KoresModifier.PROTECTED -> Opcodes.ACC_PROTECTED
            KoresModifier.PUBLIC -> Opcodes.ACC_PUBLIC
            KoresModifier.STATIC -> Opcodes.ACC_STATIC
            KoresModifier.STRICTFP -> Opcodes.ACC_STRICT
            KoresModifier.SYNCHRONIZED -> Opcodes.ACC_SYNCHRONIZED
            KoresModifier.TRANSIENT -> Opcodes.ACC_TRANSIENT
            KoresModifier.VOLATILE -> Opcodes.ACC_VOLATILE
            KoresModifier.BRIDGE -> Opcodes.ACC_BRIDGE
            KoresModifier.VARARGS -> Opcodes.ACC_VARARGS
            KoresModifier.SYNTHETIC -> Opcodes.ACC_SYNTHETIC
            KoresModifier.ANNOTATION -> Opcodes.ACC_ANNOTATION
            KoresModifier.ENUM -> Opcodes.ACC_ENUM
            KoresModifier.MANDATED -> Opcodes.ACC_MANDATED
            KoresModifier.OPEN -> Opcodes.ACC_OPEN
            KoresModifier.TRANSITIVE -> Opcodes.ACC_TRANSITIVE
            KoresModifier.STATIC_PHASE -> Opcodes.ACC_STATIC_PHASE
            else -> 0
        }
    }

    /**
     * Convert [CodeModifier]s to asm modifiers
     *
     * @param modifiers Modifiers to convert
     * @return ASM modifiers flags
     */
    fun toAsmAccess(modifiers: Collection<KoresModifier>): Int {

        val mods = modifiers.toMutableList()

        if (mods.isEmpty())
            return Opcodes.ACC_PUBLIC

        if (mods.none { it.modifierType == ModifierType.VISIBILITY })
            mods.add(KoresModifier.PUBLIC)

        return modifiers
            .map { toAsmAccess(it) }
            .filter { it != 0 }
            .sum()
    }

}