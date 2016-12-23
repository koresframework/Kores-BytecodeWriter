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
package com.github.jonathanxd.codeapi.bytecode.gen.visitor

import com.github.jonathanxd.codeapi.CodeAPI
import com.github.jonathanxd.codeapi.common.*
import com.github.jonathanxd.codeapi.bytecode.BytecodeClass
import com.github.jonathanxd.codeapi.bytecode.common.MVData
import com.github.jonathanxd.codeapi.bytecode.util.CodeTypeUtil
import com.github.jonathanxd.codeapi.bytecode.util.MethodInvocationUtil
import com.github.jonathanxd.codeapi.bytecode.util.TypeSpecUtil
import com.github.jonathanxd.codeapi.gen.visit.Visitor
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.helper.Helper
import com.github.jonathanxd.codeapi.interfaces.AccessSuper
import com.github.jonathanxd.codeapi.interfaces.Argumenterizable
import com.github.jonathanxd.codeapi.interfaces.Extender
import com.github.jonathanxd.codeapi.interfaces.MethodInvocation
import com.github.jonathanxd.codeapi.types.CodeType
import com.github.jonathanxd.iutils.container.MutableContainer
import com.github.jonathanxd.iutils.data.MapData
import org.objectweb.asm.Opcodes

object MethodInvocationVisitor : Visitor<MethodInvocation, BytecodeClass, MVData> {

    override fun visit(t: MethodInvocation, extraData: MapData, visitorGenerator: VisitorGenerator<BytecodeClass>, additional: MVData): Array<BytecodeClass> {
        val mv = additional.methodVisitor
        var methodInvocation = t

        var localization: CodeType? = methodInvocation.localization.orElse(null)

        val enclosingType: CodeType by lazy {
            extraData.getRequired(TypeVisitor.CODE_TYPE_REPRESENTATION, "Cannot determine current type!")
        }

        if (localization == null && methodInvocation.spec.methodType == MethodType.SUPER_CONSTRUCTOR) {
            val part = methodInvocation.target.orElse(null)

            val target = (
                    if (part == null || part is AccessSuper)
                        if (enclosingType is Extender) enclosingType.superType.orElse(null) else null
                    else
                        enclosingType
                    ) ?: throw IllegalArgumentException("Cannot invoke super constructor of type: '$enclosingType'. No Super class.")

            localization = target
        }

        val access = Util.access(methodInvocation, localization, visitorGenerator, extraData, additional)

        if (access != null)
            return access

        // If localization is not null
        if (localization != null) {
            // Create container with localization
            val of = MutableContainer.of(localization)

            // Fix the access to inner class member.
            methodInvocation = Util.fixAccessor(methodInvocation, extraData, of) { mi, _ ->
                // Add 'this' argument to Inner class Constructor methods.
                if (mi.get().spec.methodName == "<init>") {
                    val spec = mi.get().spec
                    var methodDescription = spec.methodDescription

                    val parameterTypes = java.util.ArrayList(methodDescription.parameterTypes)
                    val arguments = java.util.ArrayList(spec.arguments)

                    arguments.add(0, CodeAPI.argument(Helper.accessThis()))
                    parameterTypes.add(0, enclosingType)

                    methodDescription = methodDescription.setParameterTypes(parameterTypes)

                    mi.set(mi.get().setSpec(spec.setArguments(arguments).setMethodDescription(methodDescription)))
                }
            }


            localization = of.get()
        }

        var invokeType: InvokeType? = methodInvocation.invokeType
        val target = methodInvocation.target.orElse(null)
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
            mv.visitTypeInsn(Opcodes.NEW, CodeTypeUtil.codeTypeToSimpleAsm(localization))
            mv.visitInsn(Opcodes.DUP)
        }

        if (target != null && target !is CodeType) {
            visitorGenerator.generateTo(target.javaClass, target, extraData, null, additional)
        }

        visitorGenerator.generateTo(Argumenterizable::class.java, specification, extraData, null, additional)

        val invokeDynamic = methodInvocation.invokeDynamic.orElse(null)

        if (invokeDynamic != null) {

            // Generate lambda 'invokeDynamic'
            if (InvokeDynamic.isInvokeDynamicLambda(invokeDynamic)) {

                val lambdaDynamic = invokeDynamic as InvokeDynamic.LambdaMethodReference

                MethodInvocationUtil.visitLambdaInvocation(lambdaDynamic, invokeType!!, localization, specification, mv)

                if (invokeDynamic is InvokeDynamic.LambdaFragment) {
                    // Register fragment to gen
                    extraData.registerData(MethodFragmentVisitor.FRAGMENT_TYPE_INFO, invokeDynamic.methodFragment)
                }
            } else if (InvokeDynamic.isInvokeDynamicBootstrap(invokeDynamic)) { // Generate bootstrap 'invokeDynamic'
                val bootstrap = invokeDynamic as InvokeDynamic.Bootstrap
                // Visit bootstrap invoke dynamic
                MethodInvocationUtil.visitBootstrapInvocation(bootstrap, specification, mv)
            }

        } else {

            mv.visitMethodInsn(
                    /*Type like invokestatic*/InvokeType.toAsm(invokeType),
                    /*Localization*/CodeTypeUtil.codeTypeToSimpleAsm(localization),
                    /*Method name*/specification.methodName,
                    /*(ARGUMENT)RETURN*/TypeSpecUtil.typeSpecToAsm(specification.methodDescription),
                    invokeType!!.isInterface)
        }

        return emptyArray()
    }

}