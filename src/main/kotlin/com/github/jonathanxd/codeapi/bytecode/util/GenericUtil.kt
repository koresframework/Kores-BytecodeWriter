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

import com.github.jonathanxd.codeapi.generic.GenericSignature
import com.github.jonathanxd.codeapi.helper.PredefinedTypes
import com.github.jonathanxd.codeapi.interfaces.MethodDeclaration
import com.github.jonathanxd.codeapi.interfaces.TypeDeclaration
import com.github.jonathanxd.codeapi.types.CodeType
import com.github.jonathanxd.codeapi.types.GenericType
import java.util.*

object GenericUtil {

    fun genericTypesToAsmString(typeDeclaration: TypeDeclaration, superClass: CodeType, implementations: Collection<CodeType>, superClassIsGeneric: Boolean, anyInterfaceIsGeneric: Boolean): String? {
        val types = typeDeclaration.genericSignature.types

        var genericRepresentation: String? = null

        if (types.isNotEmpty()) {
            genericRepresentation = GenericUtil.genericTypesToAsmString(types)
        }

        if (types.isNotEmpty() || superClassIsGeneric || anyInterfaceIsGeneric) {

            if (genericRepresentation == null)
                genericRepresentation = ""

            genericRepresentation += CodeTypeUtil.toAsm(superClass)
        }

        if (types.isNotEmpty() || anyInterfaceIsGeneric) {
            val sb = StringBuilder()

            implementations.forEach { codeType -> sb.append(CodeTypeUtil.toAsm(codeType)) }

            genericRepresentation += sb.toString()
        }

        return genericRepresentation
    }

    fun genericSignatureToAsmString(signature: GenericSignature<GenericType>): String {
        val types = signature.types

        if (types.size == 1 && types[0].isType)
            return GenericUtil.genericTypeToAsmString(types[0])
        else
            return GenericUtil.genericTypesToAsmString(types)
    }

    fun genericTypesToAsmString(generics: Array<GenericType>): String {
        val sj = StringJoiner(";")

        for (generic in generics) {
            sj.add(genericTypeToAsmString_plain(generic))
        }

        return "<" + fixResult(sj.toString()) + ">"
    }

    fun genericTypeToAsmString(generic: GenericType): String {
        return "<" + fixResult(genericTypeToAsmString_plain(generic)) + ">"

    }

    fun fixResult(str: String): String {
        val sb = StringBuilder()

        val chars = str.toCharArray()

        var ign = false

        for (aChar in chars) {
            if (aChar == ';') {
                if (!ign) {
                    sb.append(aChar)
                }

                ign = true
            } else {
                sb.append(aChar)
                ign = false
            }
        }

        return sb.toString()
    }

    private fun genericTypeToAsmString_plain(generic: GenericType): String {
        val name = generic.name()


        var gen2 = false
        if (generic.bounds().size != 0) {
            val codeTypeBound = generic.bounds()[0]

            val type = codeTypeBound.type

            if (type is GenericType) {
                gen2 = type.bounds().size > 0
            }
        }

        return name + (if (!generic.isType) ":" else "") + (if (gen2) ":" else "") + boundToMain(generic.isWildcard, generic.bounds())

    }

    fun bounds(isWildcard: Boolean, bounds: Array<GenericType.Bound<CodeType>>): String {

        val sb = StringBuilder()

        for (bound in bounds) {

            val boundType = bound.type

            if (boundType is GenericType && !boundType.isType) {
                sb.append(if (isWildcard) bound.sign() else "").append(CodeTypeUtil.toAsm(boundType))
            } else {
                sb.append(if (isWildcard) bound.sign() else "").append(CodeTypeUtil.toAsm(boundType))
            }

        }

        return sb.toString()
    }

    private fun boundToMain(isWildcard: Boolean, bounds: Array<GenericType.Bound<CodeType>>): String {

        val sb = StringBuilder()

        for (i in bounds.indices) {

            val isLast = i + 1 >= bounds.size

            val bound = bounds[i]

            val boundType = bound.type

            if (boundType is GenericType && !boundType.isType) {
                sb.append(if (isWildcard) bound.sign() else "").append(CodeTypeUtil.toAsm(boundType)).append(if (!isLast) ":" else "")
            } else {
                sb.append(if (isWildcard) bound.sign() else "").append(CodeTypeUtil.toAsm(boundType)).append(if (!isLast) ":" else "")
            }
        }

        return sb.toString()
    }

    fun generateToBound(codeType: CodeType): String {
        if (codeType is GenericType) {
            return genericTypeToAsmString(codeType)
        } else {
            return codeType.javaSpecName
        }
    }

    //"<T::Ljava/lang/CharSequence;>(Ljava/util/List<TT;>;Ljava/lang/String;)TT;
    fun methodGenericSignature(codeMethod: MethodDeclaration): String? {

        val returnType = codeMethod.returnType.orElse(PredefinedTypes.VOID)

        val signatureBuilder = StringBuilder()

        val methodSignature = codeMethod.genericSignature


        val generateGenerics = methodSignature.isNotEmpty
                || codeMethod.parameters.stream().anyMatch { parameter -> parameter.requiredType is GenericType }
                || returnType is GenericType


        if (generateGenerics && methodSignature.isNotEmpty) {
            signatureBuilder.append(genericTypesToAsmString(methodSignature.types))
        }

        if (generateGenerics) {
            signatureBuilder.append('(')

            codeMethod.parameters.stream().map { parameter -> CodeTypeUtil.toAsm(parameter.requiredType) }.forEach{ signatureBuilder.append(it) }

            signatureBuilder.append(')')
        }

        if (generateGenerics) {
            signatureBuilder.append(CodeTypeUtil.toAsm(returnType))
        }

        return if (signatureBuilder.isNotEmpty()) signatureBuilder.toString() else null

    }

}