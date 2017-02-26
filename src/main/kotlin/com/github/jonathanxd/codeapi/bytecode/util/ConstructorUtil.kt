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

import com.github.jonathanxd.codeapi.CodeAPI
import com.github.jonathanxd.codeapi.CodePart
import com.github.jonathanxd.codeapi.CodeSource
import com.github.jonathanxd.codeapi.MutableCodeSource
import com.github.jonathanxd.codeapi.base.*
import com.github.jonathanxd.codeapi.bytecode.common.MVData
import com.github.jonathanxd.codeapi.common.CodeModifier
import com.github.jonathanxd.codeapi.common.Data
import com.github.jonathanxd.codeapi.common.InvokeType
import com.github.jonathanxd.codeapi.common.MethodType
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.util.source.CodeSourceUtil
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

object ConstructorUtil {

    fun isInitForThat(methodInvocation: MethodInvocation): Boolean {
        val any = methodInvocation.spec.methodType == MethodType.SUPER_CONSTRUCTOR

        val access = methodInvocation.target as? Access

        val accept = access != null
                && (access.type == Access.Type.THIS || access.type == Access.Type.SUPER)

        return any
                && accept
                && methodInvocation.invokeType == InvokeType.INVOKE_SPECIAL
                && methodInvocation.spec.methodName == "<init>"

    }

    fun searchForInitTo(typeDeclaration: TypeDeclaration, codeParts: CodeSource, targetAccessPredicate: (CodePart?) -> Boolean): SearchResult {
        return ConstructorUtil.searchForInitTo(typeDeclaration, codeParts, true, targetAccessPredicate, false)
    }

    fun searchForInitTo(typeDeclaration: TypeDeclaration, codeParts: CodeSource?, includeChild: Boolean, targetAccessPredicate: (CodePart?) -> Boolean, isSub: Boolean): SearchResult {
        if (codeParts == null)
            return SearchResult.FALSE

        for (codePart in codeParts) {
            if (codePart is BodyHolder && includeChild) {
                val searchResult = ConstructorUtil.searchForInitTo(typeDeclaration, codePart.body, includeChild, targetAccessPredicate, true)

                if (searchResult.found)
                    return searchResult

            }

            if (codePart is CodeSource) { // Another CodeSource is part of the Enclosing Source
                val searchResult = ConstructorUtil.searchForInitTo(typeDeclaration, codePart, includeChild, targetAccessPredicate, true)

                if (searchResult.found)
                    return searchResult
            }

            if (codePart is MethodInvocation) {

                if (codePart.spec.methodType == MethodType.SUPER_CONSTRUCTOR
                        && targetAccessPredicate(codePart.target)
                        && codePart.invokeType == InvokeType.INVOKE_SPECIAL
                        && codePart.spec.methodName == "<init>") {
                    return SearchResult(true, isSub)
                }
            }

        }

        return SearchResult.FALSE
    }

    fun searchInitThis(typeDeclaration: TypeDeclaration, codeParts: CodeSource?, validate: Boolean): Boolean {
        var searchResult = ConstructorUtil.searchForInitTo(typeDeclaration, codeParts, !validate, { codePart ->
            codePart != null && codePart is Access && codePart.type == Access.Type.THIS
        }, false)

        if (validate)
            searchResult = ConstructorUtil.validateConstructor(searchResult)

        return searchResult.found
    }

    fun searchForSuper(typeDeclaration: TypeDeclaration, codeParts: CodeSource?, validate: Boolean): Boolean {
        var searchResult = ConstructorUtil.searchForInitTo(typeDeclaration, codeParts, !validate, { codePart ->
            codePart != null && codePart is Access && codePart.type == Access.Type.SUPER
        }, false)

        if (validate)
            searchResult = ConstructorUtil.validateConstructor(searchResult)

        return searchResult.found
    }

    fun validateConstructor(searchResult: SearchResult): SearchResult {
        if (searchResult.foundOnSub)
            throw IllegalArgumentException("Don't invoke super() or this() inside a Bodied Element.")

        return searchResult
    }

    fun <T> declareFinalFields(visitorGenerator: VisitorGenerator<T>, methodBody: CodeSource?, typeDeclaration: TypeDeclaration, mv: MethodVisitor, extraData: Data, mvData: MVData, validate: Boolean) {

        if (ConstructorUtil.searchInitThis(typeDeclaration, methodBody, validate)) {
            return
        }

        /**
         * Declare variables
         */
        val all = CodeSourceUtil.find<FieldDeclaration>(
                typeDeclaration.body,
                { codePart ->
                    codePart is FieldDeclaration
                            && !codePart.modifiers.contains(CodeModifier.STATIC)
                            && codePart.value != null
                }
        ) { codePart -> codePart as FieldDeclaration }


        val label = Label()
        mv.visitLabel(label)

        for (fieldDeclaration in all) {

            val value = fieldDeclaration.value

            if (value != null) {
                // No visitor overhead.
                mv.visitVarInsn(Opcodes.ALOAD, 0)

                visitorGenerator.generateTo(value::class.java, value, extraData, null, mvData)

                // No visitor overhead.
                mv.visitFieldInsn(Opcodes.PUTFIELD, CodeTypeUtil.codeTypeToBinaryName(typeDeclaration), fieldDeclaration.name, CodeTypeUtil.toTypeDesc(fieldDeclaration.type))
            }

        }
    }

    fun generateSuperInvoke(typeDeclaration: TypeDeclaration, mv: MethodVisitor) {
        mv.visitVarInsn(Opcodes.ALOAD, 0)

        val superType = (typeDeclaration as SuperClassHolder).superClass

        if (superType == null) {
            // No visitor overhead.
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
        } else {
            // No visitor overhead.
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, CodeTypeUtil.codeTypeToBinaryName(superType), "<init>", "()V", false)
        }
    }

    fun generateFinalFields(classSource: CodeSource): CodeSource {
        val codeSource = MutableCodeSource()

        /**
         * Declare variables
         */
        val all = CodeSourceUtil.find<FieldDeclaration>(
                classSource,
                { codePart ->
                    codePart is FieldDeclaration
                            && !codePart.modifiers.contains(CodeModifier.STATIC)
                            && codePart.value != null
                }
        ) { codePart -> codePart as FieldDeclaration }

        for (fieldDeclaration in all) {

            val type = fieldDeclaration.type
            val name = fieldDeclaration.name
            val value = fieldDeclaration.value

            if (value != null) {
                codeSource.add(CodeAPI.setThisField(type, name, value))
            }
        }

        return codeSource
    }

    class SearchResult constructor(val found: Boolean, val foundOnSub: Boolean) {
        companion object {
            val FALSE = SearchResult(false, false)
        }
    }
}