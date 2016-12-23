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

import com.github.jonathanxd.codeapi.common.FullInvokeSpec
import com.github.jonathanxd.codeapi.common.InvokeDynamic
import com.github.jonathanxd.codeapi.common.InvokeType
import com.github.jonathanxd.codeapi.common.TypeSpec
import com.github.jonathanxd.codeapi.interfaces.MethodSpecification
import com.github.jonathanxd.codeapi.types.CodeType
import com.github.jonathanxd.codeapi.util.DescriptionHelper
import com.github.jonathanxd.codeapi.util.TypeResolver
import com.github.jonathanxd.iutils.description.DescriptionUtil
import org.objectweb.asm.Handle
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import java.util.*

object MethodInvocationUtil {

    fun visitLambdaInvocation(lambdaDynamic: InvokeDynamic.LambdaMethodReference,
                              invokeType: InvokeType,
                              localization: CodeType,
                              spec: MethodSpecification,
                              mv: MethodVisitor) {

        val methodSpec = lambdaDynamic.methodSpec
        val expectedTypes = lambdaDynamic.expectedTypes

        val metafactory = Handle(Opcodes.H_INVOKESTATIC,
                "java/lang/invoke/LambdaMetafactory",
                "metafactory",
                "(Ljava/lang/invoke/MethodHandles\$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;",
                false)

        val objects = arrayOf(
                Type.getType(CodeTypeUtil.fullSpecToFullAsm(methodSpec)),
                Handle(/*Opcodes.H_INVOKEINTERFACE*/InvokeType.toAsm_H(invokeType),
                        CodeTypeUtil.codeTypeToSimpleAsm(localization),
                        spec.methodName,
                        TypeSpecUtil.typeSpecToAsm(spec.methodDescription),
                        invokeType == InvokeType.INVOKE_INTERFACE),

                Type.getType(CodeTypeUtil.fullSpecToFullAsm(expectedTypes)))

        val local = "(${if (invokeType != InvokeType.INVOKE_STATIC) CodeTypeUtil.codeTypeToFullAsm(localization) else ""})${CodeTypeUtil.codeTypeToFullAsm(methodSpec.localization)}"

        mv.visitInvokeDynamicInsn(methodSpec.methodName, local, metafactory, *objects)

    }

    fun visitBootstrapInvocation(bootstrap: InvokeDynamic.Bootstrap, spec: MethodSpecification, mv: MethodVisitor) {
        val handle = MethodInvocationUtil.toHandle(bootstrap)

        mv.visitInvokeDynamicInsn(spec.methodName, TypeSpecUtil.typeSpecToAsm(spec.methodDescription), handle, *MethodInvocationUtil.toAsmArguments(bootstrap))
    }

    fun toAsmArguments(bootstrap: InvokeDynamic.Bootstrap): Array<Any> {

        val asmArgs = arrayOfNulls<Any>(bootstrap.arguments.size)

        for (i in 0..bootstrap.arguments.size - 1) {
            val arg = bootstrap.arguments[i]
            val converted: Any

            if (arg is String || arg is Int || arg is Long || arg is Float || arg is Double) {
                converted = arg
            } else if (arg is CodeType) {
                converted = Type.getType(arg.javaSpecName)
            } else if (arg is FullInvokeSpec) {

                converted = Handle(InvokeType.toAsm_H(arg.invokeType),
                        CodeTypeUtil.codeTypeToSimpleAsm(arg.localization),
                        arg.methodName,
                        TypeSpecUtil.typeSpecToAsm(arg),
                        arg.invokeType == InvokeType.INVOKE_INTERFACE)
            } else if (arg is TypeSpec) {

                val toAsm = TypeSpecUtil.typeSpecToAsm(arg)

                converted = Type.getMethodType(toAsm)
            } else {
                throw IllegalArgumentException("Illegal argument at index '" + i + "' of arguments array [" + Arrays.toString(bootstrap.arguments) + "], element type unsupported! Read the documentation.")
            }

            asmArgs[i] = converted
        }

        return asmArgs.requireNoNulls()

    }

    fun toHandle(bootstrap: InvokeDynamic.Bootstrap): Handle {
        val bootstrapMethodSpec = bootstrap.methodSpec
        val btpInvokeType = bootstrap.invokeType

        val methodName = bootstrapMethodSpec.methodName
        val bsmLocalization = bootstrapMethodSpec.localization

        return Handle(InvokeType.toAsm_H(btpInvokeType),
                CodeTypeUtil.codeTypeToSimpleAsm(bsmLocalization),
                methodName,
                CodeTypeUtil.fullSpecToFullAsm(bootstrapMethodSpec),
                btpInvokeType.isInterface)
    }

    fun specFromHandle(handle: Handle, typeResolver: TypeResolver): FullInvokeSpec {
        val invokeType = InvokeType.fromAsm_H(handle.tag)

        val owner = typeResolver.resolveUnknown(handle.owner)
        val desc = owner.javaSpecName + ":" + handle.name + handle.desc

        val description = DescriptionUtil.parseDescription(desc)

        return FullInvokeSpec(invokeType,
                owner,
                typeResolver.resolveUnknown(description.returnType),
                handle.name,
                *description.parameterTypes.map { typeResolver.resolveUnknown(it) }.toTypedArray())
    }

    fun fromHandle(handle: Handle, args: Array<Any>, typeResolver: TypeResolver): InvokeDynamic {
        val invokeType = InvokeType.fromAsm_H(handle.tag)
        val fullMethodSpec = MethodInvocationUtil.specFromHandle(handle, typeResolver)

        return InvokeDynamic.invokeDynamicBootstrap(invokeType, fullMethodSpec, *MethodInvocationUtil.bsmArgsFromAsm(args, typeResolver))
    }

    fun bsmArgsFromAsm(asmArgs: Array<Any>?, typeResolver: TypeResolver): Array<Any> {
        if (asmArgs == null || asmArgs.isEmpty())
            return emptyArray()

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

        return codeAPIArgsList.toTypedArray()
    }

}