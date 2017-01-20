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
package com.github.jonathanxd.codeapi.bytecode.gen.visitor

import com.github.jonathanxd.codeapi.CodeAPI
import com.github.jonathanxd.codeapi.base.*
import com.github.jonathanxd.codeapi.bytecode.BytecodeClass
import com.github.jonathanxd.codeapi.bytecode.common.MVData
import com.github.jonathanxd.codeapi.bytecode.util.CodeTypeUtil
import com.github.jonathanxd.codeapi.bytecode.util.InvokeTypeUtil
import com.github.jonathanxd.codeapi.bytecode.util.MethodInvocationUtil
import com.github.jonathanxd.codeapi.bytecode.util.TypeSpecUtil
import com.github.jonathanxd.codeapi.common.Data
import com.github.jonathanxd.codeapi.common.InvokeDynamic
import com.github.jonathanxd.codeapi.common.InvokeType
import com.github.jonathanxd.codeapi.common.MethodType
import com.github.jonathanxd.codeapi.gen.visit.Visitor
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.type.CodeType
import com.github.jonathanxd.iutils.container.MutableContainer
import org.objectweb.asm.Opcodes

object MethodInvocationVisitor : Visitor<MethodInvocation, BytecodeClass, MVData> {

    override fun visit(t: MethodInvocation, extraData: Data, visitorGenerator: VisitorGenerator<BytecodeClass>, additional: MVData): Array<out BytecodeClass> {
        val mv = additional.methodVisitor
        var methodInvocation = t

        var localization: CodeType = Util.resolveType(methodInvocation.localization, extraData, additional)

        val enclosingType: CodeType by lazy {
            extraData.getRequired<TypeDeclaration>(TypeVisitor.CODE_TYPE_REPRESENTATION, "Cannot determine current type!")
        }

        if (methodInvocation.spec.methodType == MethodType.SUPER_CONSTRUCTOR) {
            val part = methodInvocation.target

            val target = (
                    if (part is Access && part.type == Access.Type.SUPER)
                        if (enclosingType is SuperClassHolder) enclosingType.superClass else null
                    else
                        enclosingType
                    ) ?: throw IllegalArgumentException("Cannot invoke super constructor of type: '$enclosingType'. No Super class.")

            localization = target
        }

        val access = Util.access(methodInvocation, localization, visitorGenerator, extraData, additional)

        if (access != null)
            return access

        // Create container with localization
        val of = MutableContainer.of(localization)

        // Fix the access to inner class member.
        methodInvocation = Util.fixAccessor(methodInvocation, extraData, of) { mi, _ ->
            // Add 'this' argument to Inner class Constructor methods.
            if (mi.get().spec.methodName == "<init>") {
                val spec = mi.get().spec
                var methodDescription = spec.description

                val parameterTypes = java.util.ArrayList(methodDescription.parameterTypes)
                val arguments = java.util.ArrayList(mi.get().arguments)

                arguments.add(0, CodeAPI.argument(CodeAPI.accessThis()))
                parameterTypes.add(0, enclosingType)

                methodDescription = methodDescription.builder().withParameterTypes(parameterTypes).build()

                mi.set(mi.get().builder()
                        .withArguments(arguments)
                        .withSpec(
                                spec.builder().withDescription(methodDescription).build()
                        )
                        .build()
                )
            }
        }


        localization = of.get()

        var invokeType: InvokeType? = methodInvocation.invokeType
        val target = methodInvocation.target
        val specification = methodInvocation.spec

        if (localization == null) {
            localization = enclosingType

            // Throw exception in case of invalid invoke type
            if (invokeType == InvokeType.INVOKE_VIRTUAL || invokeType == InvokeType.INVOKE_INTERFACE) {

                val correctInvokeType = InvokeType.get(localization)

                if (invokeType != correctInvokeType) {
                    throw IllegalStateException("Invalid invocation type '$invokeType' for CodeType: '$localization' (correct invoke type: '$correctInvokeType')")
                }
            }
        }

        // If invoke type is not specified try to infer it from localization
        if (invokeType == null) {
            // Determine the invoke type.
            invokeType = InvokeType.get(localization)
        }

        if (specification.methodName == "<init>" && specification.methodType == MethodType.CONSTRUCTOR) {
            // Invoke constructor
            mv.visitTypeInsn(Opcodes.NEW, CodeTypeUtil.codeTypeToBinaryName(localization))
            mv.visitInsn(Opcodes.DUP)
        }

        if (target !is CodeType) {
            visitorGenerator.generateTo(target.javaClass, target, extraData, null, additional)
        }

        visitorGenerator.generateTo(ArgumentHolder::class.java, methodInvocation, extraData, null, additional)

        val invokeDynamic = methodInvocation.invokeDynamic

        if (invokeDynamic != null) {

            // Generate lambda 'invokeDynamic'
            if (invokeDynamic is InvokeDynamic.LambdaMethodReference) {

                val spec = if (invokeDynamic is InvokeDynamic.LambdaFragment) {
                    // Register fragment to gen
                    val newFragment = MethodFragmentVisitor.newFragment(invokeDynamic.methodFragment, extraData)

                    extraData.registerData(MethodFragmentVisitor.FRAGMENT_TYPE_INFO, newFragment)

                    specification.builder().withMethodName(newFragment.declaration.name).build()
                } else specification


                MethodInvocationUtil.visitLambdaInvocation(invokeDynamic, invokeType, localization, spec, mv)

            } else if (invokeDynamic is InvokeDynamic.Bootstrap) { // Generate bootstrap 'invokeDynamic'
                // Visit bootstrap invoke dynamic
                MethodInvocationUtil.visitBootstrapInvocation(invokeDynamic, specification, mv)
            }

        } else {

            mv.visitMethodInsn(
                    /*Type like invokestatic*/InvokeTypeUtil.toAsm(invokeType),
                    /*Localization*/CodeTypeUtil.codeTypeToBinaryName(localization),
                    /*Method name*/specification.methodName,
                    /*(ARGUMENT)RETURN*/TypeSpecUtil.typeSpecToAsm(specification.description),
                    invokeType.isInterface())
        }

        return emptyArray()
    }

}