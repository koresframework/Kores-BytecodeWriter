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
package com.koresframework.kores.bytecode.util

import com.github.jonathanxd.iutils.kt.rightOrFail
import com.koresframework.kores.base.*
import com.koresframework.kores.common.MethodTypeSpec
import com.koresframework.kores.generic.GenericSignature
import com.koresframework.kores.type.*
import com.koresframework.kores.util.findType
import com.koresframework.kores.util.genericSignature
import com.koresframework.kores.util.inferType
import com.koresframework.kores.util.toTypeVars
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable
import java.util.*

/**
 * Bridge method generation utility. This class provides functions to generate bridge methods,
 * bridge methods are required for some generic methods to ensure that invocations will not end up with
 * an [AbstractMethodError], this is also the class used by BytecodeGenerator to generate bridges.
 *
 * The bridge method resolution is a expensive task,
 * in some cases, optimized codes will be used, but in others, methods of a type is resolved with
 * [KoresTypeResolver.resolveMethods], and the performance depends on how it is implemented,
 * for Kores types, a whole copy of original [TypeDeclaration] is returned, and this is very expensive from
 * performance perspective, the same applies for Java [classes][Class], but for both, a specialized
 * code is used to get methods, which is not expensive as much as the resolution.
 */
object BridgeUtil {

    fun genBridgeMethods(typeDeclaration: TypeDeclaration): Set<MethodDeclaration> =
        typeDeclaration.methods.filterNot { it.modifiers.contains(KoresModifier.BRIDGE) }
            .flatMap { BridgeUtil.genBridgeMethod(typeDeclaration, it) }
            .filter { bridge ->
                typeDeclaration.methods.none {
                    it.name == bridge.name && it.typeSpec.isConreteEq(
                        bridge.typeSpec
                    )
                }
            }
            .distinctBy {
                Spec(
                    it.name,
                    it.returnType.concreteType,
                    it.parameters.map { it.type.concreteType })
            }
            .toSet()

    private data class Spec(val name: String, val rtype: Type, val ptypes: List<Type>)

    fun genBridgeMethod(
        typeDeclaration: TypeDeclaration,
        methodDeclaration: MethodDeclarationBase
    ): Set<MethodDeclaration> {
        val bridgeMethod = BridgeUtil.findMethodToBridge(typeDeclaration, methodDeclaration)

        return bridgeMethod
            .map {
                com.koresframework.kores.factory.bridgeMethod(
                    typeDeclaration,
                    methodDeclaration,
                    it
                )
            }
            .toSet()
    }

    fun findMethodToBridge(
        typeDeclaration: TypeDeclaration,
        methodDeclaration: MethodDeclarationBase
    ): Set<MethodTypeSpec> {

        val methodSpec = methodDeclaration.getMethodSpec(typeDeclaration)

        return BridgeUtil.find(typeDeclaration, methodSpec).filterNot {
            it.methodName == methodSpec.methodName
                    && it.typeSpec.returnType.concreteType.`is`(methodSpec.typeSpec.returnType.concreteType)
                    && it.typeSpec.parameterTypes.map { it.concreteType }
                .`is`(methodSpec.typeSpec.parameterTypes.map { it.concreteType })
        }.toSet()
    }

    private fun find(
        typeDeclaration: TypeDeclaration,
        methodSpec: MethodTypeSpec
    ): Set<MethodTypeSpec> {
        if (typeDeclaration !is SuperClassHolder && typeDeclaration !is ImplementationHolder)
            return emptySet()

        val types = mutableSetOf<Type>()

        if (typeDeclaration is SuperClassHolder) {

            val superTypeOpt = typeDeclaration.superClass

            getTypes(superTypeOpt, types)
        }

        if (typeDeclaration is ImplementationHolder) {

            typeDeclaration.implementations.stream()
                .forEach { koresType -> getTypes(koresType, types) }

        }

        return types
            .flatMap { BridgeUtil.findIn(it, methodSpec) }
            .toSet()
    }

    private fun getTypes(type: Type, types: MutableSet<Type>) {
        types.add(type)

        val resolver = type.koresType.bindedDefaultResolver

        resolver.getSuperclass().ifRight {
            it?.let {
                types.add(it)
                getTypes(it, types)
            }
        }

        resolver.getInterfaces().ifRight {
            it?.forEach {
                types.add(it)
                getTypes(it, types)
            }
        }

    }

    private fun findIn(type: Type, methodSpec: MethodTypeSpec): Set<MethodTypeSpec> {

        val generic = type as? GenericType ?: type.toGeneric

        val koresType = type.concreteType
        val bridges = mutableSetOf<MethodTypeSpec>()

        BridgeUtil.findInOverride(koresType, methodSpec).let {
            bridges += it
        }

        BridgeUtil.findIn(koresType, generic, methodSpec)?.let {
            bridges += it
        }
        return bridges
    }

    private fun findInOverride(
        theType: Type,
        methodSpec: MethodTypeSpec
    ): List<MethodTypeSpec> {
        val otherRType = methodSpec.typeSpec.returnType
        val otherParams = methodSpec.typeSpec.parameterTypes.map { it.concreteType }

        val filter: (MethodTypeSpec) -> Boolean = f@{

            val itRType = it.typeSpec.returnType.concreteType
            val itParams = it.typeSpec.parameterTypes.map { it.concreteType }

            if (it.methodName != methodSpec.methodName
                    || itParams.size != otherParams.size
                    || (otherRType.`is`(itRType) && otherParams.`is`(itParams))
            )
                return@f false

            val rTypeEq = itRType.`is`(otherRType)
                    || itRType.bindedDefaultResolver.isAssignableFrom(otherRType)
                .rightOr(true)

            val paramAssign = itParams.mapIndexed { index, koresType ->
                koresType.bindedDefaultResolver.isAssignableFrom(otherParams[index])
            }.all {
                it.rightOr(true)
            }

            val isParamEq = otherParams.size == itParams.size
                    && (otherParams.`is`(itParams)
                    || paramAssign)

            rTypeEq && isParamEq
        }

        val type = theType.concreteType

        val resolved = type.defaultResolver.resolve(type).rightOr(null)

        return when (resolved) {
            is Class<*> -> resolved.methods.map { it.getMethodSpec(theType) }.filter(filter)
            is TypeDeclaration -> resolved.methods.map { it.getMethodSpec(theType) }.filter(filter)
            else -> theType.defaultResolver.resolveMethods(theType).mapRight {
                it.map { it.getMethodSpec(theType) }.filter(filter)
            }.rightOrFail
        }
    }

    private fun findIn(
        theClass: TypeDeclaration,
        generic: GenericType,
        methodSpec: MethodTypeSpec
    ): MethodTypeSpec? {
        val genericSignature = theClass.genericSignature

        val typeParameters = toTypeVars(genericSignature)

        for (method in theClass.methods) {

            if (methodSpec.methodName != method.name)
                continue

            val parameterTypes = method.parameters.map { it.type }

            val spec = MethodTypeSpec(
                theClass, method.name,
                TypeSpec(method.returnType, parameterTypes)
            )

            if (methodSpec.compareTo(spec) == 0) {
                return spec // No problem here, CodeAPI will not duplicate methods, it will only avoid the type inference (slow part of the bridge method inference)
            }

            val methodSignature = method.genericSignature

            val methodParameters = toTypeVars(methodSignature)

            val inferredReturnType =
                method.returnType.inferType(typeParameters, methodParameters, generic)

            val inferredParametersTypes =
                parameterTypes.map { it.inferType(methodParameters, typeParameters, generic) }

            val methodTypeSpec = MethodTypeSpec(
                theClass,
                method.name,
                TypeSpec(inferredReturnType, inferredParametersTypes)
            )

            if (methodTypeSpec.compareTo(methodSpec) == 0) {
                return fixGenerics(spec, genericSignature, null, method)
            }
        }

        return null
    }

    private fun findIn(
        theClass: Class<*>,
        generic: GenericType,
        methodSpec: MethodTypeSpec
    ): MethodTypeSpec? {
        val genericSignature = theClass.genericSignature

        val typeParameters = toTypeVars(genericSignature)

        for (method in theClass.methods) {

            if (methodSpec.methodName != method.name)
                continue

            val parameterTypes = method.parameters.map { it.type }

            val spec = MethodTypeSpec(
                theClass, method.name,
                TypeSpec(method.returnType, parameterTypes)
            )

            if (methodSpec.compareTo(spec) == 0) {
                return spec // No problem here, CodeAPI will not duplicate methods, it will only avoid the type inference (slow part of the bridge method inference)
            }

            val methodSignature = method.genericSignature

            val methodParameters = toTypeVars(methodSignature)

            val inferredReturnType =
                method.returnType.inferType(typeParameters, methodParameters, generic)

            val inferredParametersTypes =
                parameterTypes.map { it.inferType(methodParameters, typeParameters, generic) }

            val methodTypeSpec = MethodTypeSpec(
                theClass,
                method.name,
                TypeSpec(inferredReturnType, inferredParametersTypes)
            )

            if (methodTypeSpec.compareTo(methodSpec) == 0) {
                return fixGenerics(spec, genericSignature, null, method)
            }
        }

        return null
    }

    private fun findIn(
        theType: Type,
        generic: GenericType,
        methodSpec: MethodTypeSpec
    ): MethodTypeSpec? {

        val type = theType.concreteType

        val resolved = type.defaultResolver.resolve(type).rightOr(null)

        return when (resolved) {
            is Class<*> -> findIn(resolved, generic, methodSpec)
            is TypeDeclaration -> findIn(resolved, generic, methodSpec)
            else -> findIn(
                theType.defaultResolver.resolveTypeDeclaration(theType).rightOrFail,
                generic,
                methodSpec
            )
        }
    }

    private fun fixGenerics(
        spec: MethodTypeSpec,
        genericSignature: GenericSignature?,
        typeVariables: Array<out TypeVariable<*>>?,
        method: Any?
    ): MethodTypeSpec {
        val returnType = spec.typeSpec.returnType

        var newReturnType = returnType

        if (returnType is GenericType) {
            newReturnType = if (returnType.isType)
                returnType.resolvedType
            else
                Objects.requireNonNull(
                    if (genericSignature != null)
                        findType(genericSignature, returnType.name)
                    else
                        findType(typeVariables, returnType.name),
                    "Cannot infer correct return type of method '$method'!"
                )!!.koresType


        }

        var newParameterTypes = spec.typeSpec.parameterTypes

        newParameterTypes = newParameterTypes.map { koresType ->
            if (koresType is GenericType) {
                if (koresType.isType)
                    return@map koresType
                else
                    return@map Objects.requireNonNull(
                        if (genericSignature != null)
                            findType(genericSignature, koresType.name)
                        else
                            findType(typeVariables, koresType.name),
                        "Cannot infer correct parameter type of ParameterType '$koresType' of method '$method'!"
                    )!!.koresType
            }

            koresType
        }

        return MethodTypeSpec(
            spec.localization,
            spec.methodName,
            TypeSpec(newReturnType, newParameterTypes)
        )
    }

}

