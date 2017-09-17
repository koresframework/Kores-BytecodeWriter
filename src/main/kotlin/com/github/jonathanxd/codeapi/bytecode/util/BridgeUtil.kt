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
import com.github.jonathanxd.codeapi.type.CodeType
import com.github.jonathanxd.codeapi.type.Generic
import com.github.jonathanxd.codeapi.type.LoadedCodeType
import com.github.jonathanxd.codeapi.util.codeType
import com.github.jonathanxd.codeapi.util.findType
import com.github.jonathanxd.codeapi.util.inferType
import com.github.jonathanxd.codeapi.util.toTypeVars
import java.lang.reflect.TypeVariable
import java.util.*


object BridgeUtil {

    fun genBridgeMethods(typeDeclaration: TypeDeclaration): List<MethodDeclaration> =
            typeDeclaration.methods.filterNot { it.modifiers.contains(CodeModifier.BRIDGE) }
                    .map { BridgeUtil.genBridgeMethod(typeDeclaration, it) }
                    .filter { it.isPresent }
                    .map { it.get() }
                    .filter { bridge ->
                        typeDeclaration.methods.none {
                            it.getMethodSpec(typeDeclaration).compareTo(bridge.getMethodSpec(typeDeclaration)) == 0
                        }
                    }

    fun genBridgeMethod(typeDeclaration: TypeDeclaration, methodDeclaration: MethodDeclarationBase): Optional<MethodDeclaration> {
        val bridgeMethod = BridgeUtil.findMethodToBridge(typeDeclaration, methodDeclaration)

        return if (bridgeMethod == null) Optional.empty()
        else Optional.of(com.github.jonathanxd.codeapi.factory.bridgeMethod(typeDeclaration, methodDeclaration, bridgeMethod))
    }

    fun findMethodToBridge(typeDeclaration: TypeDeclaration, methodDeclaration: MethodDeclarationBase): MethodTypeSpec? {

        val methodSpec = methodDeclaration.getMethodSpec(typeDeclaration)

        val found = BridgeUtil.find(typeDeclaration, methodSpec)

        if (found != null && found.compareTo(methodSpec) == 0)
            return null

        return found
    }

    private fun find(typeDeclaration: TypeDeclaration, methodSpec: MethodTypeSpec): MethodTypeSpec? {
        if (typeDeclaration !is SuperClassHolder && typeDeclaration !is ImplementationHolder)
            return null

        val types = ArrayList<Generic>()

        if (typeDeclaration is SuperClassHolder) {

            val superTypeOpt = typeDeclaration.superClass

            if (superTypeOpt is Generic)
                types.add(superTypeOpt)
        }

        if (typeDeclaration is ImplementationHolder) {

            typeDeclaration.implementations.stream()
                    .filter { codeType -> codeType is Generic }
                    .forEach { codeType -> types.add(codeType as Generic) }

        }

        return types
                .map { BridgeUtil.findIn(it, methodSpec) }
                .firstOrNull { it != null }
    }

    private fun findIn(generic: Generic, methodSpec: MethodTypeSpec): MethodTypeSpec? {

        if (generic.isType) {
            val codeType = generic.resolvedType

            if (codeType is LoadedCodeType<*>) {
                val loadedType = codeType.loadedType

                return BridgeUtil.findIn(loadedType, generic, methodSpec)
            }

            if (codeType is TypeDeclaration) {

                return BridgeUtil.findIn(codeType, generic, methodSpec)
            }
        }

        return null
    }

    private fun findIn(theClass: Class<*>, generic: Generic, methodSpec: MethodTypeSpec): MethodTypeSpec? {
        val typeParameters = theClass.typeParameters

        for (method in theClass.declaredMethods) {

            if (methodSpec.methodName != method.name)
                continue

            val spec = MethodTypeSpec(
                    method.declaringClass,
                    method.name,
                    TypeSpec(method.returnType, method.parameterTypes.toList()))

            if (methodSpec.compareTo(spec) == 0) {
                return spec // No problem here, CodeAPI will not duplicate methods, it will only avoid the type inference (slow part of the bridge method inference)
            }

            val methodParameters = method.typeParameters
            val inferredReturnType = method.genericReturnType.inferType(typeParameters, methodParameters, generic)


            val genericParameterTypes = method.genericParameterTypes

            val inferredParametersTypes = genericParameterTypes.map { it.inferType(methodParameters, typeParameters, generic) }

            val methodTypeSpec = MethodTypeSpec(
                    method.declaringClass,
                    method.name,
                    TypeSpec(inferredReturnType, inferredParametersTypes))

            if (methodTypeSpec.compareTo(methodSpec) == 0) {
                return fixGenerics(spec, null, typeParameters, null)
            }
        }

        return null
    }

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

            val inferredParametersTypes = ArrayList<CodeType>()

            for (genericParameterType in parameterTypes) {
                inferredParametersTypes.add(genericParameterType.inferType(methodParameters, typeParameters, generic))
            }

            val methodTypeSpec = MethodTypeSpec(theClass, method.name, TypeSpec(inferredReturnType, inferredParametersTypes))

            if (methodTypeSpec.compareTo(methodSpec) == 0) {
                return fixGenerics(spec, genericSignature, null, method)
            }
        }

        return null
    }

    private fun fixGenerics(spec: MethodTypeSpec, genericSignature: GenericSignature?, typeVariables: Array<out TypeVariable<*>>?, method: Any?): MethodTypeSpec {
        val returnType = spec.typeSpec.returnType

        var newReturnType = returnType

        if (returnType is Generic) {


            if (returnType.isType)
                newReturnType = returnType.resolvedType
            else
                newReturnType = Objects.requireNonNull(
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

    private fun find(name: String, typeVariables: Array<TypeVariable<*>>): TypeVariable<*>? {
        return typeVariables.firstOrNull { it.name == name }
    }
}

fun MethodDeclarationBase.getMethodSpec(typeDeclaration: TypeDeclaration): MethodTypeSpec {
    return MethodTypeSpec(
            typeDeclaration,
            this.name,
            TypeSpec(this.returnType, this.parameters.map { it.type })
    )
}