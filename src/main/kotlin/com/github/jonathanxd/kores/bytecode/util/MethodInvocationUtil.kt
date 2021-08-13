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
package com.github.jonathanxd.kores.bytecode.util

import com.github.jonathanxd.kores.base.*
import com.github.jonathanxd.kores.bytecode.processor.processors.Util
import com.github.jonathanxd.kores.type.KoresType
import com.github.jonathanxd.kores.type.internalName
import com.github.jonathanxd.kores.type.javaSpecName
import com.github.jonathanxd.kores.util.TypeResolver
import com.github.jonathanxd.kores.util.resolveUnknown
import com.github.jonathanxd.kores.util.toTypeSpec
import com.github.jonathanxd.kores.util.typeDesc
import com.github.jonathanxd.iutils.data.TypedData
import com.github.jonathanxd.iutils.description.DescriptionUtil
import com.github.jonathanxd.kores.common.*
import com.github.jonathanxd.kores.type.isInterface
import org.objectweb.asm.*
import java.util.*

object MethodInvocationUtil {

    @Deprecated("Bootstrap visit can be used since 4.0.0")
    fun visitLambdaInvocation(
        lambdaDynamic: InvokeDynamicBase.LambdaMethodRefBase,
        invokeType: InvokeType,
        localization: ReflectType,
        spec: DynamicMethodSpec,
        mv: MethodVisitor
    ) {

        val baseSam = lambdaDynamic.baseSam
        val expectedTypes = lambdaDynamic.expectedTypes

        val metafactory = Handle(
            Opcodes.H_INVOKESTATIC,
            "java/lang/invoke/LambdaMetafactory",
            "metafactory",
            "(Ljava/lang/invoke/MethodHandles\$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;",
            false
        )

        val objects = arrayOf(
            Type.getType(baseSam.typeSpec.typeDesc),
            Handle(/*Opcodes.H_INVOKEINTERFACE*/InvokeTypeUtil.toAsm_H(invokeType),
                localization.internalName,
                spec.name,
                spec.typeSpec.typeDesc,
                invokeType == InvokeType.INVOKE_INTERFACE
            ),

            Type.getType(expectedTypes.typeDesc)
        )

        val additionalArguments = if (baseSam.typeSpec.parameterTypes.size !=
                lambdaDynamic.methodRef.methodTypeSpec.typeSpec.parameterTypes.size
        ) {
            val samSpec = baseSam.typeSpec
            val invkSpec = lambdaDynamic.methodRef.methodTypeSpec.typeSpec

            invkSpec.parameterTypes.subList(
                0,
                (invkSpec.parameterTypes.size - samSpec.parameterTypes.size)
            )
        } else emptyList()

        val local =
            "(${if (invokeType != InvokeType.INVOKE_STATIC) localization.typeDesc else ""}" +
                    "${additionalArguments.typeDesc})${baseSam.localization.typeDesc}"
        mv.visitInvokeDynamicInsn(baseSam.methodName, local, metafactory, *objects)

    }

    fun visitBootstrapInvocation(
        bootstrap: InvokeDynamicBase,
        spec: DynamicMethodSpec,
        data: TypedData,
        mv: MethodVisitor
    ) {
        val handle = MethodInvocationUtil.toHandle(bootstrap, data)

        mv.visitInvokeDynamicInsn(
            spec.name,
            Util.resolveType(spec.typeSpec, data).typeDesc,
            handle,
            *MethodInvocationUtil.toAsmArguments(bootstrap, data)
        )
    }

    fun toAsmArguments(bootstrap: InvokeDynamicBase, data: TypedData): Array<Any> {
        return toAsmArguments(bootstrap.bootstrapArgs, data)
    }

    fun toAsmArguments(bootstrapArgs: List<Any>, data: TypedData): Array<Any> {

        val asmArgs = Array(bootstrapArgs.size) { i ->
            val arg = bootstrapArgs[i]

            when (arg) {
                is String, is Int, is Long, is Float, is Double -> {
                    arg
                }
                is ReflectType -> {
                    Type.getType(Util.resolveType(arg, data).javaSpecName)
                }
                is MethodInvokeSpec -> {

                    val typeSpec = arg.methodTypeSpec

                    Handle(
                        InvokeTypeUtil.toAsm_H(arg.invokeType),
                        Util.resolveType(typeSpec.localization, data).internalName,
                        typeSpec.methodName,
                        Util.resolveType(typeSpec.typeSpec, data).typeDesc,
                        arg.invokeType == InvokeType.INVOKE_INTERFACE || typeSpec.localization.isInterface
                    )
                }
                is FieldAccessHandleSpec -> {

                    val fieldTypeSpec = arg.fieldTypeSpec

                    Handle(
                        InvokeTypeUtil.toAsm_H(arg.accessKind),
                        Util.resolveType(fieldTypeSpec.localization, data).internalName,
                        fieldTypeSpec.fieldName,
                        Util.resolveType(fieldTypeSpec.fieldType, data).typeDesc,
                        fieldTypeSpec.localization.isInterface
                    )
                }
                is MethodInvokeHandleSpec -> {
                    arg.toHandle(data)
                }
                is DynamicConstantSpec -> {
                    ConstantDynamic(
                        arg.constantName,
                        Util.resolveType(arg.type, data).typeDesc,
                        arg.bootstrapMethod.toHandle(data),
                        *toAsmArguments(arg.bootstrapArgs, data)
                    )
                }
                is TypeSpec -> {

                    val toAsm = Util.resolveType(arg, data).typeDesc

                    Type.getMethodType(toAsm)
                }
                else -> {
                    throw IllegalArgumentException(
                        "Illegal argument at index '$i' of arguments list [$bootstrapArgs], element type unsupported! Read the documentation."
                    )
                }
            }

        }

        return asmArgs
    }

    fun MethodInvokeHandleSpec.toHandle(data: TypedData): Handle =
        Handle(
            InvokeTypeUtil.toAsm_H(this.invokeType),
            Util.resolveType(this.methodTypeSpec.localization, data).internalName,
            this.methodTypeSpec.methodName,
            Util.resolveType(this.methodTypeSpec.typeSpec, data).typeDesc,
            this.methodTypeSpec.localization.isInterface
        )

    fun toHandle(bootstrap: InvokeDynamicBase, data: TypedData): Handle {
        val bootstrapMethodSpec = bootstrap.bootstrap
        val btpInvokeType = bootstrap.bootstrap.invokeType

        val methodName = bootstrapMethodSpec.methodTypeSpec.methodName
        val bsmLocalization =
            Util.resolveType(bootstrapMethodSpec.methodTypeSpec.localization, data)

        return Handle(
            InvokeTypeUtil.toAsm_H(btpInvokeType),
            bsmLocalization.internalName,
            methodName,
            Util.resolveType(bootstrapMethodSpec.methodTypeSpec.typeSpec, data).typeDesc,
            bsmLocalization.isInterface
        )
    }

    fun specFromHandle(handle: Handle, typeResolver: TypeResolver): MethodInvokeHandleSpec {
        val invokeType = InvokeTypeUtil.fromAsmIndy(handle.tag)

        val owner = typeResolver.resolveUnknown(handle.owner)
        val desc = owner.javaSpecName + ":" + handle.name + handle.desc

        val description = DescriptionUtil.parseDescription(desc)

        return MethodInvokeHandleSpec(
            invokeType,
            MethodTypeSpec(
                owner,
                handle.name,
                TypeSpec(
                    typeResolver.resolveUnknown(description.type),
                    description.parameterTypes.map { typeResolver.resolveUnknown(it) })
            )
        )
    }

    fun fromHandle(
        handle: Handle,
        args: Array<Any>,
        typeResolver: TypeResolver,
        dynamicMethod: DynamicMethodSpec,
        type: ReflectType
    ): InvokeDynamicBase {
        val methodInvokeSpec = MethodInvocationUtil.specFromHandle(handle, typeResolver)

        return InvokeDynamic(
            bootstrap = methodInvokeSpec,
            dynamicMethod = dynamicMethod,
            bootstrapArgs = MethodInvocationUtil.bsmArgsFromAsm(args, typeResolver)
        )
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
                    || asmArg is String
            ) {
                codeAPIArgsList.add(asmArg)
            } else if (asmArg is Type) {

                val className = asmArg.className

                if (className != null) {
                    // Class
                    codeAPIArgsList.add(typeResolver.resolveUnknown(className))
                } else {
                    // Method
                    codeAPIArgsList.add(toTypeSpec(asmArg.descriptor, typeResolver))
                }


            } else if (asmArg is Handle) {
                codeAPIArgsList.add(MethodInvocationUtil.specFromHandle(asmArg, typeResolver))
            } else {
                throw IllegalArgumentException("Unsupported ASM BSM Argument: $asmArg")
            }
        }

        return codeAPIArgsList
    }

}