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

import com.github.jonathanxd.codeapi.CodeAPI
import com.github.jonathanxd.codeapi.CodeSource
import com.github.jonathanxd.codeapi.base.ImplementationHolder
import com.github.jonathanxd.codeapi.base.MethodDeclaration
import com.github.jonathanxd.codeapi.base.SuperClassHolder
import com.github.jonathanxd.codeapi.base.TypeDeclaration
import com.github.jonathanxd.codeapi.common.CodeParameter
import com.github.jonathanxd.codeapi.common.MethodTypeSpec
import com.github.jonathanxd.codeapi.common.TypeSpec
import com.github.jonathanxd.codeapi.generic.GenericSignature
import com.github.jonathanxd.codeapi.inspect.SourceInspect
import com.github.jonathanxd.codeapi.type.CodeType
import com.github.jonathanxd.codeapi.type.Generic
import com.github.jonathanxd.codeapi.type.LoadedCodeType
import com.github.jonathanxd.codeapi.util.TypeVarUtil
import com.github.jonathanxd.codeapi.util.element.ElementUtil

import java.lang.reflect.Method
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable
import java.util.ArrayList
import java.util.Objects
import java.util.Optional
import java.util.stream.Collectors

object BridgeUtil {

    fun genBridgeMethod(typeDeclaration: TypeDeclaration, methodDeclaration: MethodDeclaration): Optional<MethodDeclaration> {
        val bridgeMethod = BridgeUtil.findMethodToBridge(typeDeclaration, methodDeclaration)

        return if (bridgeMethod == null) Optional.empty() else Optional.of(CodeAPI.bridgeMethod(typeDeclaration, methodDeclaration, bridgeMethod))
    }

    fun findMethodToBridge(typeDeclaration: TypeDeclaration, methodDeclaration: MethodDeclaration): MethodTypeSpec? {
        val methodSpec = ElementUtil.getMethodSpec(typeDeclaration, methodDeclaration)

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

            if (superTypeOpt != null) {
                if (superTypeOpt is Generic)
                    types.add(superTypeOpt)
            }
        }

        if (typeDeclaration is ImplementationHolder) {

            typeDeclaration.implementations.stream()
                    .filter { codeType -> codeType is Generic }
                    .forEach { codeType -> types.add(codeType as Generic) }

        }

        for (type in types) {
            val `in` = BridgeUtil.findIn(type, methodSpec)

            if (`in` != null)
                return `in`
        }

        return null
    }

    private fun findIn(generic: Generic, methodSpec: MethodTypeSpec): MethodTypeSpec? {

        if (generic.isType) {
            val codeType = generic.codeType

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
                    CodeAPI.getJavaType(method.declaringClass),
                    method.name,
                    TypeSpec(CodeAPI.getJavaType(method.returnType), CodeAPI.getJavaTypeList(method.parameterTypes)))

            if (methodSpec.compareTo(spec) == 0) {
                return spec // No problem here, CodeAPI will not duplicate methods, it will only avoid the type inference (slow part of the bridge method inference)
            }

            val methodParameters = method.typeParameters

            val inferredReturnType = TypeVarUtil.toCodeType(method.genericReturnType, typeParameters, methodParameters, generic)

            val inferredParametersTypes = ArrayList<CodeType>()

            val genericParameterTypes = method.genericParameterTypes

            for (genericParameterType in genericParameterTypes) {
                inferredParametersTypes.add(TypeVarUtil.toCodeType(genericParameterType, methodParameters, typeParameters, generic))
            }

            val methodTypeSpec = MethodTypeSpec(
                    CodeAPI.getJavaType(method.declaringClass),
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

        val typeParameters = TypeVarUtil.toTypeVars(genericSignature)

        val inspect = SourceInspect.find { codePart -> codePart is MethodDeclaration }
                .include { bodied -> bodied is CodeSource }
                .mapTo { codePart -> codePart as MethodDeclaration }
                .inspect(theClass.body)

        for (method in inspect) {

            if (methodSpec.methodName != method.name)
                continue

            val parameterTypes = method.parameters.map { it.type }

            val spec = MethodTypeSpec(theClass, method.name,
                    TypeSpec(method.returnType, parameterTypes))

            if (methodSpec.compareTo(spec) == 0) {
                return spec // No problem here, CodeAPI will not duplicate methods, it will only avoid the type inference (slow part of the bridge method inference)
            }

            val methodSignature = method.genericSignature

            val methodParameters = TypeVarUtil.toTypeVars(methodSignature)

            val inferredReturnType = TypeVarUtil.toCodeType(method.returnType, typeParameters, methodParameters, generic)

            val inferredParametersTypes = ArrayList<CodeType>()

            for (genericParameterType in parameterTypes) {
                inferredParametersTypes.add(TypeVarUtil.toCodeType(genericParameterType, methodParameters, typeParameters, generic))
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
                newReturnType = returnType.codeType
            else
                newReturnType = Objects.requireNonNull(
                        if (genericSignature != null)
                            TypeVarUtil.findType(genericSignature, returnType.name)
                        else
                            TypeVarUtil.findType(typeVariables, returnType.name),
                        "Cannot infer correct return type of method '$method'!")


        }

        var newParameterTypes = spec.typeSpec.parameterTypes

        newParameterTypes = newParameterTypes.map { codeType ->
            if (codeType is Generic) {
                if (codeType.isType)
                    return@map codeType
                else
                    return@map Objects.requireNonNull(if (genericSignature != null)
                        TypeVarUtil.findType(genericSignature, codeType.name)
                    else
                        TypeVarUtil.findType(typeVariables, codeType.name), "Cannot infer correct parameter type of ParameterType '$codeType' of method '$method'!")
            }

            codeType
        }

        return MethodTypeSpec(spec.localization, spec.methodName, TypeSpec(newReturnType, newParameterTypes))
    }

    private fun find(name: String, typeVariables: Array<TypeVariable<*>>): TypeVariable<*>? {
        for (typeVariable in typeVariables) {
            if (typeVariable.name == name)
                return typeVariable
        }

        return null
    }
}