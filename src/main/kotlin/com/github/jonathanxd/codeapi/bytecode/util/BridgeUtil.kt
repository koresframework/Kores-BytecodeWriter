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
import com.github.jonathanxd.codeapi.common.MethodTypeSpec
import com.github.jonathanxd.codeapi.generic.GenericSignature
import com.github.jonathanxd.codeapi.type.Generic
import com.github.jonathanxd.codeapi.util.*
import com.github.jonathanxd.jwiutils.kt.rightOrFail
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable
import java.util.*


object BridgeUtil {

    fun genBridgeMethods(typeDeclaration: TypeDeclaration): Set<MethodDeclaration> =
            typeDeclaration.methods.filterNot { it.modifiers.contains(CodeModifier.BRIDGE) }
                    .flatMap { BridgeUtil.genBridgeMethod(typeDeclaration, it) }
                    .filter { bridge ->
                        typeDeclaration.methods.none {
                            val itSpec = it.getMethodSpec(typeDeclaration)
                            val bridgeSpec = bridge.getMethodSpec(typeDeclaration)
                            itSpec.methodName == bridgeSpec.methodName
                                    && itSpec.typeSpec.isConreteEq(bridgeSpec.typeSpec)
                        }
                    }
                    .distinctBy { Spec(it.name, it.returnType.concreteType, it.parameters.map { it.type.concreteType }) }
                    .toSet()

    private data class Spec(val name: String, val rtype: Type, val ptypes: List<Type>)

    fun genBridgeMethod(typeDeclaration: TypeDeclaration, methodDeclaration: MethodDeclarationBase): Set<MethodDeclaration> {
        val bridgeMethod = BridgeUtil.findMethodToBridge(typeDeclaration, methodDeclaration)

        return bridgeMethod
                .map { com.github.jonathanxd.codeapi.factory.bridgeMethod(typeDeclaration, methodDeclaration, it) }
                .toSet()
    }

    fun findMethodToBridge(typeDeclaration: TypeDeclaration, methodDeclaration: MethodDeclarationBase): Set<MethodTypeSpec> {

        val methodSpec = methodDeclaration.getMethodSpec(typeDeclaration)

        return BridgeUtil.find(typeDeclaration, methodSpec).filterNot {
            it.compareTo(methodSpec) == 0
        }.toSet()
    }

    private fun find(typeDeclaration: TypeDeclaration, methodSpec: MethodTypeSpec): Set<MethodTypeSpec> {
        if (typeDeclaration !is SuperClassHolder && typeDeclaration !is ImplementationHolder)
            return emptySet()

        val types = mutableSetOf<Type>()

        if (typeDeclaration is SuperClassHolder) {

            val superTypeOpt = typeDeclaration.superClass

            getTypes(superTypeOpt, types)
        }

        if (typeDeclaration is ImplementationHolder) {

            typeDeclaration.implementations.stream()
                    .forEach { codeType -> getTypes(codeType, types) }

        }

        return types
                .flatMap { BridgeUtil.findIn(it, methodSpec) }
                .toSet()
    }

    private fun getTypes(type: Type, types: MutableSet<Type>) {
        types.add(type)

        val resolver = type.codeType.bindedDefaultResolver

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

        val generic = type.codeType as? Generic ?: Generic.type(type)
        val codeType = type.concreteType
        val bridges = mutableSetOf<MethodTypeSpec>()

        BridgeUtil.findInOverride(codeType, generic, methodSpec).let {
            bridges += it
        }

        BridgeUtil.findIn(codeType, generic, methodSpec)?.let {
            bridges += it
        }
        return bridges
    }

    private fun findInOverride(theType: Type, generic: Generic, methodSpec: MethodTypeSpec): List<MethodTypeSpec> =
            theType.defaultResolver.resolveMethods(theType).mapRight {
                it.map { it.getMethodSpec(theType) }.filter {
                    it.methodName == methodSpec.methodName
                            && it.typeSpec.parameterTypes.map { it.concreteType }
                            .`is`(methodSpec.typeSpec.parameterTypes.map { it.concreteType })
                            && !it.typeSpec.returnType.concreteType
                            .`is`(methodSpec.typeSpec.returnType.concreteType)
                            && it.typeSpec.returnType.codeType.bindedDefaultResolver.isAssignableFrom(methodSpec.typeSpec.returnType)
                            .rightOr(true)
                }
            }.rightOrFail

    private fun findIn(theClass: TypeDeclaration, generic: Generic, methodSpec: MethodTypeSpec): MethodTypeSpec? {
        val genericSignature = theClass.genericSignature

        val typeParameters = toTypeVars(genericSignature)

        for (method in theClass.methods) {

            if (methodSpec.methodName != method.name)
                continue

            val parameterTypes = method.parameters.map { it.type }

            val spec = MethodTypeSpec(theClass, method.name,
                    TypeSpec(method.returnType, parameterTypes))

            if (methodSpec.compareTo(spec) == 0) {
                return spec // No problem here, CodeAPI will not duplicate methods, it will only avoid the type inference (slow part of the bridge method inference)
            }

            val methodSignature = method.genericSignature

            val methodParameters = toTypeVars(methodSignature)

            val inferredReturnType = method.returnType.inferType(typeParameters, methodParameters, generic)

            val inferredParametersTypes = parameterTypes.map { it.inferType(methodParameters, typeParameters, generic) }

            val methodTypeSpec = MethodTypeSpec(theClass, method.name, TypeSpec(inferredReturnType, inferredParametersTypes))

            if (methodTypeSpec.compareTo(methodSpec) == 0) {
                return fixGenerics(spec, genericSignature, null, method)
            }
        }

        return null
    }

    private fun findIn(theType: Type, generic: Generic, methodSpec: MethodTypeSpec): MethodTypeSpec? {
        val type = theType.defaultResolver.resolveTypeDeclaration(theType).rightOrFail

        return findIn(type, generic, methodSpec)
    }

    private fun fixGenerics(spec: MethodTypeSpec, genericSignature: GenericSignature?, typeVariables: Array<out TypeVariable<*>>?, method: Any?): MethodTypeSpec {
        val returnType = spec.typeSpec.returnType

        var newReturnType = returnType

        if (returnType is Generic) {
            newReturnType = if (returnType.isType)
                returnType.resolvedType
            else
                Objects.requireNonNull(
                        if (genericSignature != null)
                            findType(genericSignature, returnType.name)
                        else
                            findType(typeVariables, returnType.name),
                        "Cannot infer correct return type of method '$method'!")!!.codeType


        }

        var newParameterTypes = spec.typeSpec.parameterTypes

        newParameterTypes = newParameterTypes.map { codeType ->
            if (codeType is Generic) {
                if (codeType.isType)
                    return@map codeType
                else
                    return@map Objects.requireNonNull(if (genericSignature != null)
                        findType(genericSignature, codeType.name)
                    else
                        findType(typeVariables, codeType.name), "Cannot infer correct parameter type of ParameterType '$codeType' of method '$method'!")!!.codeType
            }

            codeType
        }

        return MethodTypeSpec(spec.localization, spec.methodName, TypeSpec(newReturnType, newParameterTypes))
    }

    private fun find(name: String, typeVariables: Array<TypeVariable<*>>): TypeVariable<*>? =
            typeVariables.firstOrNull { it.name == name }
}

fun MethodDeclarationBase.getMethodSpec(typeDeclaration: TypeDeclaration): MethodTypeSpec =
        MethodTypeSpec(
                typeDeclaration,
                this.name,
                TypeSpec(this.returnType, this.parameters.map { it.type })
        )

fun MethodDeclarationBase.getMethodSpec(type: Type): MethodTypeSpec = MethodTypeSpec(
        type,
        this.name,
        TypeSpec(this.returnType, this.parameters.map { it.type })
)