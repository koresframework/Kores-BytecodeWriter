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

import com.github.jonathanxd.iutils.kt.rightOrFail
import com.github.jonathanxd.kores.base.*
import com.github.jonathanxd.kores.common.MethodTypeSpec
import com.github.jonathanxd.kores.generic.GenericSignature
import com.github.jonathanxd.kores.type.*
import com.github.jonathanxd.kores.util.*
import java.lang.reflect.Method
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

    fun genBridgeMethods(typeDeclaration: TypeDeclaration): Set<MethodDeclaration> {
        val allMethods = typeDeclaration.allMethodsWithType()

        return typeDeclaration.methods.filterNot { it.modifiers.contains(KoresModifier.BRIDGE) }
            .flatMap { BridgeUtil.genBridgeMethod(typeDeclaration, it, allMethods) }
            .filter { bridge ->
                typeDeclaration.methods.none {
                    val itSpec = it.getMethodSpec(typeDeclaration)
                    val bridgeSpec = bridge.getMethodSpec(typeDeclaration)
                    itSpec.isConcreteEq(bridgeSpec)
                }
            }
            .distinctBy {
                Spec(
                    it.name,
                    it.returnType.concreteType,
                    it.parameters.map { it.type.concreteType })
            }
            .toSet()
    }

    private data class Spec(val name: String, val rtype: Type, val ptypes: List<Type>) {
        override fun hashCode(): Int =
                Objects.hash(this.name, this.rtype.hash(), this.ptypes.map { it.hash() })

        override fun equals(other: Any?): Boolean =
                other is Spec && other.name == this.name
                        && other.rtype.`is`(this.rtype)
                        && other.ptypes.bothMatches(this.ptypes) { a, b -> a.`is`(b) }
    }

    fun genBridgeMethod(
        typeDeclaration: TypeDeclaration,
        current: MethodDeclarationBase,
        allMethods: List<Pair<Type, List<MethodTypeSpecSign>>> = typeDeclaration.allMethodsWithType()
    ): Set<MethodDeclaration> {
        val bridgeMethod = BridgeUtil.findMethodToBridge(typeDeclaration, current, allMethods)

        return bridgeMethod
            .map {
                com.github.jonathanxd.kores.factory.bridgeMethod(
                    typeDeclaration,
                    current,
                    it
                )
            }
            .toSet()
    }

    private fun findMethodToBridge(
        typeDeclaration: TypeDeclaration,
        methodDeclaration: MethodDeclarationBase,
        allMethods: List<Pair<Type, List<MethodTypeSpecSign>>>
    ): Set<MethodTypeSpec> {
        val methodSpec = methodDeclaration.getMethodSpec(typeDeclaration)

        return BridgeUtil.find(typeDeclaration, methodSpec, allMethods).filterNot {
            methodSpec.isConcreteEq(it)
        }.toSet()
    }

    private fun find(
        typeDeclaration: TypeDeclaration,
        methodSpec: MethodTypeSpec,
        allMethods: List<Pair<Type, List<MethodTypeSpecSign>>>
    ): Set<MethodTypeSpec> {
        if (typeDeclaration !is SuperClassHolder && typeDeclaration !is ImplementationHolder)
            return emptySet()

        return allMethods
            .flatMap { (type, methods) -> BridgeUtil.findIn(type, methodSpec, methods) }
            .toSet()
    }

    private fun findIn(type: Type,
                       methodSpec: MethodTypeSpec,
                       allMethods: List<MethodTypeSpecSign>): Set<MethodTypeSpec> {

        val generic = type as? GenericType ?: type.toGeneric

        val koresType = type.concreteType
        val bridges = mutableSetOf<MethodTypeSpec>()

        BridgeUtil.findInOverride(methodSpec, allMethods).let {
            bridges += it
        }

        BridgeUtil.findInUnknown(koresType, generic, methodSpec, allMethods)?.let {
            bridges += it
        }
        return bridges
    }

    private fun findInOverride(
        methodSpec: MethodTypeSpec,
        allMethods: List<MethodTypeSpecSign>
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

        return allMethods.map { it.spec }.filter(filter)
    }

    private fun findIn(
        theClass: TypeDeclaration,
        generic: GenericType,
        methodSpec: MethodTypeSpec,
        allMethods: List<MethodTypeSpecSign>
    ): MethodTypeSpec? {
        val genericSignature = theClass.genericSignature

        val typeParameters = toTypeVars(genericSignature)

        for (method in allMethods) {

            if (methodSpec.methodName != method.spec.methodName)
                continue

            if (methodSpec.isConcreteEq(method.spec)) {
                return method.spec // No problem here, Kores will not duplicate methods, it will only avoid the type inference (slow part of the bridge method inference)
            }

            val methodSignature = method.genericSignature

            val methodParameters = toTypeVars(methodSignature)

            val inferredReturnType =
                method.spec.typeSpec.returnType.inferType(typeParameters, methodParameters, generic)

            val inferredParametersTypes =
                method.spec.typeSpec.parameterTypes.map { it.inferType(methodParameters, typeParameters, generic) }

            val methodTypeSpec = method.spec.copy(typeSpec = TypeSpec(inferredReturnType, inferredParametersTypes))

            if (methodTypeSpec.compareTo(methodSpec) == 0) {
                return fixGenerics(method.spec, genericSignature, null, method)
            }
        }

        return null
    }

    private fun findIn(
        theClass: Class<*>,
        generic: GenericType,
        methodSpec: MethodTypeSpec,
        allMethods: List<MethodTypeSpecSign>
    ): MethodTypeSpec? {
        val genericSignature = theClass.genericSignature

        val typeParameters = toTypeVars(genericSignature)

        for (method in allMethods) {

            if (methodSpec.methodName != method.spec.methodName)
                continue

            if (methodSpec.isConcreteEq(method.spec)) {
                return method.spec // No problem here, Kores will not duplicate methods, it will only avoid the type inference (slow part of the bridge method inference)
            }

            val methodSignature = method.genericSignature

            val methodParameters = toTypeVars(methodSignature)

            val inferredReturnType =
                method.spec.typeSpec.returnType.inferType(typeParameters, methodParameters, generic)

            val inferredParametersTypes =
                method.spec.typeSpec.parameterTypes.map { it.inferType(methodParameters, typeParameters, generic) }

            val methodTypeSpec = method.spec.copy(typeSpec = TypeSpec(inferredReturnType, inferredParametersTypes))

            if (methodTypeSpec.compareTo(methodSpec) == 0) {
                return fixGenerics(method.spec, genericSignature, null, method)
            }
        }

        return null
    }

    private fun findInUnknown(
        theType: Type,
        generic: GenericType,
        methodSpec: MethodTypeSpec,
        allMethods: List<MethodTypeSpecSign>
    ): MethodTypeSpec? {

        val type = theType.concreteType

        val resolved = type.defaultResolver.resolve(type).rightOr(null)

        return when (resolved) {
            is Class<*> -> findIn(resolved, generic, methodSpec, allMethods)
            is TypeDeclaration -> findIn(resolved, generic, methodSpec, allMethods)
            else -> findIn(
                theType.defaultResolver.resolveTypeDeclaration(theType).rightOrFail,
                generic,
                methodSpec,
                allMethods
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

