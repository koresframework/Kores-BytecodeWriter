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

import com.github.jonathanxd.codeapi.base.*
import com.github.jonathanxd.codeapi.bytecode.processor.processors.Util
import com.github.jonathanxd.codeapi.common.MethodInvokeSpec
import com.github.jonathanxd.codeapi.common.MethodTypeSpec
import com.github.jonathanxd.codeapi.type.CodeType
import com.github.jonathanxd.codeapi.util.*
import com.github.jonathanxd.iutils.data.TypedData
import com.github.jonathanxd.iutils.description.DescriptionUtil
import org.objectweb.asm.Handle
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import java.util.ArrayList

object MethodInvocationUtil {

    fun visitLambdaInvocation(lambdaDynamic: InvokeDynamicBase.LambdaMethodRefBase,
                              invokeType: InvokeType,
                              localization: ReflectType,
                              spec: MethodTypeSpec,
                              mv: MethodVisitor) {

        val baseSam = lambdaDynamic.baseSam
        val expectedTypes = lambdaDynamic.expectedTypes

        val metafactory = Handle(Opcodes.H_INVOKESTATIC,
                "java/lang/invoke/LambdaMetafactory",
                "metafactory",
                "(Ljava/lang/invoke/MethodHandles\$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;",
                false)

        val objects = arrayOf(
                Type.getType(baseSam.typeSpec.typeDesc),
                Handle(/*Opcodes.H_INVOKEINTERFACE*/InvokeTypeUtil.toAsm_H(invokeType),
                        localization.internalName,
                        spec.methodName,
                        spec.typeSpec.typeDesc,
                        invokeType == InvokeType.INVOKE_INTERFACE),

                Type.getType(expectedTypes.typeDesc))
        // TODO: LOCAL VAR ACCESS HERE

        val additionalArguments = if (baseSam.typeSpec.parameterTypes.size !=
                                          lambdaDynamic.invocation.spec.typeSpec.parameterTypes.size) {
            val samSpec = baseSam.typeSpec
            val invkSpec = lambdaDynamic.invocation.spec.typeSpec

            invkSpec.parameterTypes.subList(0, (invkSpec.parameterTypes.size - samSpec.parameterTypes.size))
        } else emptyList()

        val local = "(${if (invokeType != InvokeType.INVOKE_STATIC) localization.typeDesc else ""}" +
                "${additionalArguments.typeDesc})${baseSam.localization.typeDesc}"
        // Additional arguments in the front of bootstrap description (local)
        // And them appears as the first arguments of function
        // Example b -> accept(b, x); // x = int
        // The parameter x appears in synthetic method as first parameter:
        // lambda$0(int, String)
        mv.visitInvokeDynamicInsn(baseSam.methodName, local, metafactory, *objects)

    }

    fun visitBootstrapInvocation(bootstrap: InvokeDynamicBase, spec: MethodTypeSpec, data: TypedData, mv: MethodVisitor) {
        val handle = MethodInvocationUtil.toHandle(bootstrap, data)

        mv.visitInvokeDynamicInsn(spec.methodName, spec.typeSpec.typeDesc, handle, *MethodInvocationUtil.toAsmArguments(bootstrap))
    }

    fun toAsmArguments(bootstrap: InvokeDynamicBase): Array<Any> {

        val asmArgs = arrayOfNulls<Any>(bootstrap.args.size)

        for (i in 0 until bootstrap.args.size) {
            val arg = bootstrap.args[i]
            val converted: Any

            if (arg is String || arg is Int || arg is Long || arg is Float || arg is Double) {
                converted = arg
            } else if (arg is CodeType) {
                converted = Type.getType(arg.javaSpecName)
            } else if (arg is MethodInvokeSpec) {

                val typeSpec = arg.methodTypeSpec

                converted = Handle(InvokeTypeUtil.toAsm_H(arg.invokeType),
                        typeSpec.localization.internalName,
                        typeSpec.methodName,
                        typeSpec.typeSpec.typeDesc,
                        arg.invokeType == InvokeType.INVOKE_INTERFACE)
            } else if (arg is TypeSpec) {

                val toAsm = arg.typeDesc

                converted = Type.getMethodType(toAsm)
            } else {
                throw IllegalArgumentException("Illegal argument at index '" + i + "' of arguments array [" + bootstrap.args + "], element type unsupported! Read the documentation.")
            }

            asmArgs[i] = converted
        }

        return asmArgs.requireNoNulls()

    }

    fun toHandle(bootstrap: InvokeDynamicBase, data: TypedData): Handle {
        val bootstrapMethodSpec = bootstrap.bootstrap
        val btpInvokeType = bootstrap.bootstrap.invokeType

        val methodName = bootstrapMethodSpec.methodTypeSpec.methodName
        val bsmLocalization = Util.resolveType(bootstrapMethodSpec.methodTypeSpec.localization, data)

        return Handle(InvokeTypeUtil.toAsm_H(btpInvokeType),
                bsmLocalization.internalName,
                methodName,
                bootstrapMethodSpec.methodTypeSpec.typeSpec.typeDesc,
                btpInvokeType.isInterface())
    }

    fun specFromHandle(handle: Handle, typeResolver: TypeResolver): MethodInvokeSpec {
        val invokeType = InvokeTypeUtil.fromAsm_H(handle.tag)

        val owner = typeResolver.resolveUnknown(handle.owner)
        val desc = owner.javaSpecName + ":" + handle.name + handle.desc

        val description = DescriptionUtil.parseDescription(desc)

        return MethodInvokeSpec(
                invokeType,
                MethodTypeSpec(
                        owner,
                        handle.name,
                        TypeSpec(typeResolver.resolveUnknown(description.type), description.parameterTypes.map { typeResolver.resolveUnknown(it) })
                )
        )
    }

    fun fromHandle(handle: Handle, args: Array<Any>, typeResolver: TypeResolver, methodInvocation: MethodInvocation, type: ReflectType): InvokeDynamicBase {
        val invokeType = InvokeTypeUtil.fromAsm_H(handle.tag)
        val fullMethodSpec = MethodInvocationUtil.specFromHandle(handle, typeResolver)

        return InvokeDynamic(bootstrap = fullMethodSpec,
                type = type,
                invocation = methodInvocation.builder().invokeType(invokeType).build(),
                args = MethodInvocationUtil.bsmArgsFromAsm(args, typeResolver))
    }

    fun bsmArgsFromAsm(asmArgs: Array<Any>?, typeResolver: TypeResolver): List<Any> {
        if (asmArgs == null || asmArgs.isEmpty())
            return emptyList()

        val codeAPIArgsList = ArrayList<Any>()

        for (asmArg in asmArgs) {
            if (asmArg is Int
                    || asmArg is Float
                    || asmArg is Long
                    || asmArg is Double
                    || asmArg is String) {
                codeAPIArgsList.add(asmArg)
            } else if (asmArg is Type) {

                val className = asmArg.className

                if (className != null) {
                    // Class
                    codeAPIArgsList.add(typeResolver.resolveUnknown(className))
                } else {
                    // Method
                    codeAPIArgsList.add(DescriptionHelper.toTypeSpec(asmArg.descriptor, typeResolver))
                }


            } else if (asmArg is Handle) {
                codeAPIArgsList.add(MethodInvocationUtil.specFromHandle(asmArg, typeResolver))
            } else {
                throw IllegalArgumentException("Unsupported ASM BSM Argument: " + asmArg)
            }
        }

        return codeAPIArgsList
    }

}