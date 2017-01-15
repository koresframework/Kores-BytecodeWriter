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

import com.github.jonathanxd.codeapi.Types
import com.github.jonathanxd.codeapi.base.MethodDeclaration
import com.github.jonathanxd.codeapi.base.TypeDeclaration
import com.github.jonathanxd.codeapi.common.CodeParameter
import com.github.jonathanxd.codeapi.common.TypeSpec
import com.github.jonathanxd.codeapi.type.CodeType
import com.github.jonathanxd.codeapi.type.GenericType
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import com.github.jonathanxd.codeapi.util.CodeTypeUtil as BaseCodeTypeUtil

object CodeTypeUtil {

    fun resolveRealQualified(qualifiedName: String, outer: CodeType?): String = BaseCodeTypeUtil.resolveRealQualified(qualifiedName, outer)

    fun codeTypeToBinaryName(type: CodeType): String {
        return if (type.isPrimitive)
            primitiveToTypeDesc(type)
        else if (!type.isArray)
            type.type.replace('.', '/')
        else
            toTypeDesc(type)
    }

    fun toTypeDesc(type: CodeType): String = BaseCodeTypeUtil.codeTypeToFullAsm(type)

    fun primitiveToTypeDesc(type: CodeType): String = BaseCodeTypeUtil.primitiveCodeTypeToAsm(type)

    fun arrayToTypeDesc(codeType: CodeType): String =
            if (codeType.arrayDimension <= 1) {
                codeTypeToBinaryName(codeType)
            } else toTypeDesc(codeType)


    fun codeTypesToBinaryName(type: Iterable<CodeType>): String {
        val sb = StringBuilder()

        for (codeType in type) {
            sb.append(codeTypeToBinaryName(codeType))
        }

        return sb.toString()
    }

    fun codeTypesToTypeDesc(type: Iterable<CodeType>): String {
        val sb = StringBuilder()

        for (codeType in type) {
            sb.append(toTypeDesc(codeType))
        }

        return sb.toString()
    }

    fun typeSpecToTypeDesc(typeSpec: TypeSpec): String {
        return "(" + CodeTypeUtil.codeTypesToTypeDesc(typeSpec.parameterTypes) + ")" +
                CodeTypeUtil.toTypeDesc(typeSpec.returnType)
    }

    fun typeSpecToBinaryName(typeSpec: TypeSpec): String {
        return "(" + CodeTypeUtil.codeTypesToBinaryName(typeSpec.parameterTypes) + ")" +
                CodeTypeUtil.codeTypeToBinaryName(typeSpec.returnType)
    }

    fun toName(codeType: CodeType): String {
        if (codeType is GenericType) {

            val name = codeType.name

            val bounds = codeType.bounds

            if (bounds.isEmpty()) {
                if (!codeType.isType) {
                    if(codeType.isWildcard) {
                        return GenericUtil.fixResult("$name")
                    }else {
                        return GenericUtil.fixResult("T$name;")
                    }
                } else {
                    return name + ";"
                }
            } else {
                return GenericUtil.fixResult(if (!codeType.isWildcard)
                    name + "<" + GenericUtil.bounds(codeType.isWildcard, bounds) + ">;"
                else
                    GenericUtil.bounds(codeType.isWildcard, bounds) + ";")
            }

        } else {
            return GenericUtil.fixResult(toTypeDesc(codeType))
        }
    }

    fun parametersToTypeDesc(codeParameters: Collection<CodeParameter>): String {
        return codeTypesToTypeDesc(codeParameters.map { it.type })
    }

    fun parametersAndReturnToDesc(codeParameters: Collection<CodeParameter>, returnType: CodeType): String {
        return parametersTypeAndReturnToDesc(codeParameters.map { it.type }, returnType)
    }

    fun parametersTypeAndReturnToDesc(parameterTypes: Collection<CodeType>, returnType: CodeType): String {
        return "(${codeTypesToTypeDesc(parameterTypes)})${toTypeDesc(returnType)}"
    }

    /**
     * Infer bound of generic types specified in [method declaration][method] or in [type declaration][owner].
     */
    fun parametersAndReturnToInferredDesc(owner: TypeDeclaration, method: MethodDeclaration, codeParameters: Collection<CodeParameter>, returnType: CodeType): String {

        val genericSign = owner.genericSignature
        val methodGenericSign = method.genericSignature
        val parameterTypes = codeParameters.map { it.type }

        fun infer(codeType: CodeType): CodeType =
                if (codeType is GenericType && !codeType.isType) {
                    GenericUtil.find(methodGenericSign, codeType.name) ?: GenericUtil.find(genericSign, codeType.name) ?: codeType.codeType
                } else {
                    codeType
                }

        return parametersTypeAndReturnToDesc(parameterTypes.map(::infer), infer(returnType))
    }

    fun arrayOpcodeFromType(codeType: CodeType): Int {
        when (codeType.type) {
            "int" -> return Opcodes.T_INT
            "boolean" -> return Opcodes.T_BOOLEAN
            "byte" -> return Opcodes.T_BYTE
            "char" -> return Opcodes.T_CHAR
            "double" -> return Opcodes.T_DOUBLE
            "float" -> return Opcodes.T_FLOAT
            "short" -> return Opcodes.T_SHORT
            "long" -> return Opcodes.T_LONG
            else -> return Integer.MIN_VALUE
        }
    }

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

    fun getOpcodeForType(type: CodeType, opcode: Int): Int {
        val asmType = Type.getType(type.javaSpecName)

        return asmType.getOpcode(opcode)
    }

    fun convertToPrimitive(from: CodeType, to: CodeType, mv: MethodVisitor) {
        var opcode = -1

        if (from.isPrimitive && to.isPrimitive) {
            val fromTypeChar = Character.toUpperCase(from.canonicalName[0])
            val toTypeChar = Character.toUpperCase(to.canonicalName[0])

            opcode = this.getOpcode(fromTypeChar, toTypeChar)

            if (opcode == -1) {
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