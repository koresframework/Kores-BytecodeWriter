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

import com.github.jonathanxd.codeapi.CodeInstruction
import com.github.jonathanxd.codeapi.CodeSource
import com.github.jonathanxd.codeapi.MutableCodeSource
import com.github.jonathanxd.codeapi.base.*
import com.github.jonathanxd.codeapi.bytecode.common.MethodVisitorHelper
import com.github.jonathanxd.codeapi.common.CodeNothing
import com.github.jonathanxd.codeapi.factory.setFieldValue
import com.github.jonathanxd.codeapi.processor.ProcessorManager
import com.github.jonathanxd.codeapi.util.Alias
import com.github.jonathanxd.codeapi.util.internalName
import com.github.jonathanxd.codeapi.util.safeForComparison
import com.github.jonathanxd.codeapi.util.typeDesc
import com.github.jonathanxd.iutils.data.TypedData
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

// Some operations calls directly asm MethodVisitor to avoid processor overhead, this does not
// means that operation is faster or that processors will slow down the process (at this point
// probably JVM already made a lot of optimizations which makes the code runs faster even
// calling processors. But processors are designed to process values in an environment which
// states can change the behavior and processors are designed to avoid code duplication, but for this
// utility class, there is no reason to use processors, not only because of the overhead
// but because some operations are constants (never change). In future we can change to
// processor if needed, but at the moment, this is not required.

/**
 * Utilities method to find `super` and `this` call and field default values declaration.
 */
object ConstructorUtil {

    fun isInitForThat(methodInvocation: MethodInvocation): Boolean {
        // Constructors requires a [New] target, super invocations does not have this target
        val safeTarget = methodInvocation.target.safeForComparison
        val superInvocation = safeTarget !is New

        val target = safeTarget

        val accept = (target is Access
                && (target == Access.THIS || target == Access.SUPER))
                || (target === Alias.SUPER || target === Alias.THIS)

        return superInvocation
                && accept
                && methodInvocation.invokeType == InvokeType.INVOKE_SPECIAL
                && methodInvocation.spec.methodName == "<init>"

    }

    fun searchForInitTo(typeDeclaration: TypeDeclaration,
                        source: CodeSource,
                        targetAccessPredicate: (CodeInstruction) -> Boolean): SearchResult {
        return ConstructorUtil.searchForInitTo(typeDeclaration, source, true, targetAccessPredicate, false)
    }

    fun searchForInitTo(typeDeclaration: TypeDeclaration,
                        codeParts: CodeSource?,
                        includeChild: Boolean,
                        targetAccessPredicate: (CodeInstruction) -> Boolean, isSub: Boolean): SearchResult {
        if (codeParts == null)
            return SearchResult.FALSE

        for (codePart in codeParts) {
            val safe = codePart.safeForComparison

            if (safe is BodyHolder && includeChild) {
                val searchResult = ConstructorUtil.searchForInitTo(typeDeclaration, safe.body, includeChild, targetAccessPredicate, true)

                if (searchResult.found)
                    return searchResult
            }

            if (codePart is MethodInvocation) {

                // Constructors requires a [New] target, super invocations does not have this target
                if (codePart.target.safeForComparison !is New
                        && targetAccessPredicate(codePart.target.safeForComparison)
                        && codePart.invokeType == InvokeType.INVOKE_SPECIAL
                        && codePart.spec.methodName == "<init>") {
                    return SearchResult(true, isSub)
                }
            }

        }

        return SearchResult.FALSE
    }

    fun searchInitThis(typeDeclaration: TypeDeclaration, codeParts: CodeSource, validate: Boolean): Boolean {
        var searchResult = ConstructorUtil.searchForInitTo(typeDeclaration, codeParts, !validate, { instruction ->
            instruction == Access.THIS || instruction == Alias.THIS
        }, false)

        if (validate)
            searchResult = ConstructorUtil.validateConstructor(searchResult)

        return searchResult.found
    }

    fun searchForSuper(typeDeclaration: TypeDeclaration, codeParts: CodeSource?, validate: Boolean): Boolean {
        var searchResult = ConstructorUtil.searchForInitTo(typeDeclaration, codeParts, !validate, { instruction ->
            instruction == Access.SUPER || instruction == Alias.SUPER
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

    fun declareFinalFields(processorManager: ProcessorManager<*>,
                           methodBody: CodeSource,
                           typeDeclaration: TypeDeclaration,
                           mv: MethodVisitorHelper,
                           data: TypedData,
                           validate: Boolean) {

        if (ConstructorUtil.searchInitThis(typeDeclaration, methodBody, validate)) {
            return
        }

        /**
         * Declare variables
         */
        val all = typeDeclaration.fields.filter {
            !it.modifiers.contains(CodeModifier.STATIC)
                    && it.value != CodeNothing
        }


        val label = Label()
        mv.methodVisitor.visitLabel(label)

        for (fieldDeclaration in all) {

            val value = fieldDeclaration.value

            if (value != CodeNothing) {
                // No processor overhead.
                mv.methodVisitor.visitVarInsn(Opcodes.ALOAD, 0)

                processorManager.process(value::class.java, value, data)

                // No processor overhead.
                mv.methodVisitor.visitFieldInsn(Opcodes.PUTFIELD,
                        typeDeclaration.internalName,
                        fieldDeclaration.name,
                        fieldDeclaration.type.typeDesc)
            }

        }
    }

    fun generateSuperInvoke(typeDeclaration: TypeDeclaration, mv: MethodVisitor) {
        mv.visitVarInsn(Opcodes.ALOAD, 0)

        val superType = (typeDeclaration as SuperClassHolder).superClass

        // No processor overhead.
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, superType.internalName, "<init>", "()V", false)
    }

    fun generateFinalFields(elementsHolder: ElementsHolder): CodeSource {
        val codeSource = MutableCodeSource.create()

        /**
         * Declare variables
         */
        val all = elementsHolder.fields.filter {
            !it.modifiers.contains(CodeModifier.STATIC)
                    && it.value != CodeNothing
        }


        for (fieldDeclaration in all) {

            val type = fieldDeclaration.type
            val name = fieldDeclaration.name
            val value = fieldDeclaration.value

            if (value.safeForComparison != CodeNothing) {
                codeSource.add(setFieldValue(Alias.THIS, Access.THIS, type, name, value))
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