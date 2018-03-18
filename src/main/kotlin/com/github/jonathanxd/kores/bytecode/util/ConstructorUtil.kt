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

import com.github.jonathanxd.kores.Instruction
import com.github.jonathanxd.kores.Instructions
import com.github.jonathanxd.kores.MutableInstructions
import com.github.jonathanxd.kores.base.*
import com.github.jonathanxd.kores.bytecode.common.MethodVisitorHelper
import com.github.jonathanxd.kores.bytecode.processor.IN_EXPRESSION
import com.github.jonathanxd.kores.bytecode.processor.incrementInContext
import com.github.jonathanxd.kores.common.KoresNothing
import com.github.jonathanxd.kores.factory.setFieldValue
import com.github.jonathanxd.kores.processor.ProcessorManager
import com.github.jonathanxd.kores.safeForComparison
import com.github.jonathanxd.kores.type.internalName
import com.github.jonathanxd.kores.util.typeDesc
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

    fun searchForInitTo(
        typeDeclaration: TypeDeclaration,
        source: Instructions,
        targetAccessPredicate: (Instruction) -> Boolean
    ): SearchResult {
        return ConstructorUtil.searchForInitTo(
            typeDeclaration,
            source,
            true,
            targetAccessPredicate,
            false
        )
    }

    fun searchForInitTo(
        typeDeclaration: TypeDeclaration,
        codeParts: Instructions?,
        includeChild: Boolean,
        targetAccessPredicate: (Instruction) -> Boolean, isSub: Boolean
    ): SearchResult {
        if (codeParts == null)
            return SearchResult.FALSE

        for (codePart in codeParts) {
            val safe = codePart.safeForComparison

            if (safe is BodyHolder && includeChild) {
                val searchResult = ConstructorUtil.searchForInitTo(
                    typeDeclaration,
                    safe.body,
                    includeChild,
                    targetAccessPredicate,
                    true
                )

                if (searchResult.found)
                    return searchResult
            }

            if (safe is MethodInvocation) {

                // Constructors requires a [New] target, super invocations does not have this target
                if (safe.target.safeForComparison !is New
                        && targetAccessPredicate(safe.target.safeForComparison)
                        && safe.invokeType == InvokeType.INVOKE_SPECIAL
                        && safe.spec.methodName == "<init>"
                ) {
                    return SearchResult(true, isSub)
                }
            }

        }

        return SearchResult.FALSE
    }

    fun searchInitThis(
        typeDeclaration: TypeDeclaration,
        codeParts: Instructions,
        validate: Boolean
    ): Boolean {
        var searchResult =
            ConstructorUtil.searchForInitTo(typeDeclaration, codeParts, !validate, { instruction ->
                instruction == Access.THIS || instruction == Alias.THIS
            }, false)

        if (validate)
            searchResult = ConstructorUtil.validateConstructor(searchResult)

        return searchResult.found
    }

    fun searchForSuper(
        typeDeclaration: TypeDeclaration,
        codeParts: Instructions?,
        validate: Boolean
    ): Boolean {
        var searchResult =
            ConstructorUtil.searchForInitTo(typeDeclaration, codeParts, !validate, { instruction ->
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

    fun declareFinalFields(
        processorManager: ProcessorManager<*>,
        methodBody: Instructions,
        typeDeclaration: TypeDeclaration,
        mv: MethodVisitorHelper,
        data: TypedData,
        validate: Boolean
    ) {

        if (ConstructorUtil.searchInitThis(typeDeclaration, methodBody, validate)) {
            return
        }

        /**
         * Declare variables
         */
        val all = typeDeclaration.fields.filter {
            !it.modifiers.contains(KoresModifier.STATIC)
                    && it.value != KoresNothing
        }


        val label = Label()
        mv.methodVisitor.visitLabel(label)

        for (fieldDeclaration in all) {

            val value = fieldDeclaration.value

            if (value.safeForComparison != KoresNothing) {
                // No processor overhead.
                mv.methodVisitor.visitVarInsn(Opcodes.ALOAD, 0)

                IN_EXPRESSION.incrementInContext(data) {
                    processorManager.process(value::class.java, value, data)
                }

                // No processor overhead.
                mv.methodVisitor.visitFieldInsn(
                    Opcodes.PUTFIELD,
                    typeDeclaration.internalName,
                    fieldDeclaration.name,
                    fieldDeclaration.type.typeDesc
                )
            }

        }
    }

    fun generateSuperInvoke(typeDeclaration: TypeDeclaration, mv: MethodVisitor) {
        mv.visitVarInsn(Opcodes.ALOAD, 0)

        val superType = (typeDeclaration as SuperClassHolder).superClass

        // No processor overhead.
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, superType.internalName, "<init>", "()V", false)
    }

    fun generateFinalFields(elementsHolder: ElementsHolder): Instructions {
        val instructions = MutableInstructions.create()

        /**
         * Declare variables
         */
        val all = elementsHolder.fields.filter {
            !it.modifiers.contains(KoresModifier.STATIC)
                    && it.value != KoresNothing
        }


        for (fieldDeclaration in all) {

            val type = fieldDeclaration.type
            val name = fieldDeclaration.name
            val value = fieldDeclaration.value

            if (value.safeForComparison != KoresNothing) {
                instructions.add(setFieldValue(Alias.THIS, Access.THIS, type, name, value))
            }
        }

        return instructions
    }

    class SearchResult constructor(val found: Boolean, val foundOnSub: Boolean) {
        companion object {
            val FALSE = SearchResult(false, false)
        }
    }
}