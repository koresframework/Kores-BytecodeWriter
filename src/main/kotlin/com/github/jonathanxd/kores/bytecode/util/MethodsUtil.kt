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
import com.github.jonathanxd.iutils.recursion.Element
import com.github.jonathanxd.iutils.recursion.ElementUtil
import com.github.jonathanxd.iutils.recursion.Elements
import com.github.jonathanxd.kores.base.GenericSignatureHolder
import com.github.jonathanxd.kores.base.MethodDeclarationBase
import com.github.jonathanxd.kores.base.TypeDeclaration
import com.github.jonathanxd.kores.base.TypeSpec
import com.github.jonathanxd.kores.common.MethodTypeSpec
import com.github.jonathanxd.kores.generic.GenericSignature
import com.github.jonathanxd.kores.type.`is`
import com.github.jonathanxd.kores.type.concreteType
import com.github.jonathanxd.kores.type.koresType
import com.github.jonathanxd.kores.util.genericSignature
import java.lang.reflect.Method
import java.lang.reflect.Type

data class MethodTypeSpecSign(
    override val genericSignature: GenericSignature,
    val spec: MethodTypeSpec
) : GenericSignatureHolder {

    override fun builder(): GenericSignatureHolder.Builder<GenericSignatureHolder, *> =
        Bd(this)

    class Bd(private val spec: MethodTypeSpecSign) :
        GenericSignatureHolder.Builder<GenericSignatureHolder, Bd> {
        override fun genericSignature(value: GenericSignature): Bd =
            Bd(this.spec.copy(genericSignature = value))

        override fun build(): GenericSignatureHolder = this.spec

    }
}

fun TypeDeclaration.allMethodsWithType(): List<Pair<Type, List<MethodTypeSpecSign>>> {
    val methods = mutableListOf<Pair<Type, List<MethodTypeSpecSign>>>()

    val elements = Elements<Type>()
    elements.insert(Element(this))

    var next: Element<Type>? = elements.nextElement()

    while (next != null) {

        val value = next.value

        val type = value.concreteType

        val resolved = type.defaultResolver.resolve(type).rightOr(null)

        when (resolved) {
            is Class<*> -> resolved.methods.map { it.getMethodSpecSign(type) }
            is TypeDeclaration -> resolved.methods.map { it.getMethodSpecSign(type) }
            else -> type.defaultResolver.resolveMethods(type).mapRight {
                it.map { it.getMethodSpecSign(type) }
            }.rightOrFail
        }.also {
            methods += value to it
        }

        val resolver = type.koresType.bindedDefaultResolver

        val types = mutableListOf<Type>()

        resolver.getSuperclass().ifRight {
            it?.let {
                types += it
            }
        }

        resolver.getInterfaces().ifRight {
            it?.forEach {
                types += it
            }
        }

        if (types.isNotEmpty())
            elements.insertFromPair(ElementUtil.fromIterable(types))

        next = elements.nextElement()
    }

    return methods
}

fun TypeDeclaration.allMethods(): List<MethodTypeSpecSign> {
    val methods = mutableListOf<MethodTypeSpecSign>()

    this.allMethodsWithType().forEach { (_, typeMethods) ->
        typeMethods.forEach { other ->
            if (methods.none { it.spec.isConcreteEq(other.spec) })
                methods += other
        }
    }

    return methods
}

fun MethodTypeSpec.isConcreteEq(other: MethodTypeSpec) =
    this.methodName == other.methodName
            && this.typeSpec.returnType.concreteType.`is`(other.typeSpec.returnType.concreteType)
            && this.typeSpec.parameterTypes.map { it.concreteType }.`is`(other.typeSpec.parameterTypes.map { it.concreteType })

fun MethodDeclarationBase.getMethodSpecSign(type: Type): MethodTypeSpecSign =
    MethodTypeSpecSign(
        this.genericSignature,
        MethodTypeSpec(
            type,
            this.name,
            TypeSpec(this.returnType, this.parameters.map { it.type })
        )
    )

fun Method.getMethodSpecSign(type: Type): MethodTypeSpecSign =
    MethodTypeSpecSign(
        this.genericSignature,
        MethodTypeSpec(
            type,
            this.name,
            TypeSpec(this.returnType, this.parameters.map { it.type })
        )
    )

fun MethodDeclarationBase.getMethodSpec(typeDeclaration: TypeDeclaration): MethodTypeSpec =
    MethodTypeSpec(
        typeDeclaration,
        this.name,
        TypeSpec(this.returnType, this.parameters.map { it.type })
    )

