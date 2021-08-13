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
package com.github.jonathanxd.kores.bytecode.processor.processors

import com.github.jonathanxd.kores.base.*
import com.github.jonathanxd.kores.bytecode.util.InvokeTypeUtil
import com.github.jonathanxd.kores.factory.invokeConstructor
import com.github.jonathanxd.kores.processor.Processor
import com.github.jonathanxd.kores.processor.ProcessorManager
import com.github.jonathanxd.kores.safeForComparison
import com.github.jonathanxd.kores.type.`is`
import com.github.jonathanxd.kores.type.internalName
import com.github.jonathanxd.kores.type.isInterface
import com.github.jonathanxd.iutils.data.TypedData
import com.github.jonathanxd.iutils.kt.require
import com.github.jonathanxd.kores.bytecode.isSyntheticAccess
import com.github.jonathanxd.kores.bytecode.nestAccessGenerationMode
import com.github.jonathanxd.kores.bytecode.processor.*
import com.github.jonathanxd.kores.type.KoresType
import org.objectweb.asm.Opcodes
import java.lang.reflect.Type

object MethodInvocationProcessor : Processor<MethodInvocation> {

    override fun process(
        part: MethodInvocation,
        data: TypedData,
        processorManager: ProcessorManager<*>
    ) {
        val mv = METHOD_VISITOR.require(data).methodVisitor

        val localization: Type = Util.resolveType(part.localization, data)

        // Inner transformation

        var newSpecification = part.spec
        var newPart = part

        if (part.invokeType.isSpecial() && !part.isSuperConstructorInvocation) {
            val innerSpec = getInnerSpec(localization, data)

            if (innerSpec != null) {
                newSpecification = newSpecification.copy(
                    typeSpec = newSpecification.typeSpec.copy(
                        parameterTypes = innerSpec.argTypes + newSpecification.typeSpec.parameterTypes
                    )
                )
                newPart = newPart.builder()
                    .spec(newSpecification)
                    .arguments(innerSpec.args + newPart.arguments).build()
            }
        }

        // /Inner transformation

        // Synthetic accessor redirection
        var syntheticPart = part

        val version = data.findClassVersion()

        if (processorManager.options.nestAccessGenerationMode(version).isSyntheticAccess()) {
            val access = accessMemberOfType(localization, newPart, data)

            // RETURN AT END OF IF
            if (access != null) {
                val args = if (access.newElementToAccess is ConstructorDeclaration) {
                    newPart.arguments + access.newElementToAccess.parameters.last().type.invokeConstructor()
                } else newPart.arguments

                val invk = access.createInvokeToNewElement(newPart.target, args)

                syntheticPart = invk
                newPart = syntheticPart
                newSpecification = syntheticPart.spec
            }
        }
        // /Synthetic accessor redirection

        val invokeType: InvokeType = syntheticPart.invokeType
        val target = syntheticPart.target
        val safeTarget = target.safeForComparison

        // Throw exception in case of invalid invoke type
        if (invokeType == InvokeType.INVOKE_VIRTUAL || invokeType == InvokeType.INVOKE_INTERFACE) {

            val correctInvokeType = InvokeType.get(localization)

            if (invokeType != correctInvokeType) {
                throw IllegalStateException("Invalid invocation type '$invokeType' for CodeType: '$localization' (correct invoke type: '$correctInvokeType')")
            }
        }

        if (!syntheticPart.isSuperConstructorInvocation) {
            // Invoke constructor
            // NOT REQUIRED, SEE 'NewProcessor'
            //mv.visitTypeInsn(Opcodes.NEW, localization.internalName)
            //mv.visitInsn(Opcodes.DUP)
        } else {
            mv.visitVarInsn(Opcodes.ALOAD, 0)
        }

        if (safeTarget !is KoresType && !syntheticPart.isSuperConstructorInvocation) {
            IN_EXPRESSION.incrementInContext(data) {
                processorManager.process(target::class.java, target, data)
            }

            if (safeTarget is New)
                mv.visitInsn(Opcodes.DUP) // New does not dup, it is intended
        }

        processorManager.process(ArgumentsHolder::class.java, newPart, data)

        mv.visitMethodInsn(
            /*Type like invokestatic*/InvokeTypeUtil.toAsm(invokeType),
            /*Localization*/localization.internalName,
            /*Method name*/newSpecification.methodName,
            /*(ARGUMENT)RETURN*/newSpecification.typeSpec.typeDesc,
            localization.isInterface
        )

        if (!syntheticPart.spec.typeSpec.returnType.`is`(Void.TYPE) && IN_EXPRESSION.require(data) == 0) {
            mv.visitInsn(Opcodes.POP)
        }
    }

}