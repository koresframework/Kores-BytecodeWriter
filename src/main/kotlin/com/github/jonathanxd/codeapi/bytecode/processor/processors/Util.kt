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
package com.github.jonathanxd.codeapi.bytecode.processor.processors

import com.github.jonathanxd.codeapi.CodeInstruction
import com.github.jonathanxd.codeapi.CodePart
import com.github.jonathanxd.codeapi.base.*
import com.github.jonathanxd.codeapi.bytecode.processor.METHOD_VISITOR
import com.github.jonathanxd.codeapi.bytecode.processor.OUTER_TYPES_FIELDS
import com.github.jonathanxd.codeapi.bytecode.processor.TYPES
import com.github.jonathanxd.codeapi.bytecode.processor.TYPE_DECLARATION
import com.github.jonathanxd.codeapi.bytecode.util.ReflectType
import com.github.jonathanxd.codeapi.bytecode.util.allInnerTypes
import com.github.jonathanxd.codeapi.factory.accessField
import com.github.jonathanxd.codeapi.type.CodeType
import com.github.jonathanxd.codeapi.util.Alias
import com.github.jonathanxd.codeapi.util.`is`
import com.github.jonathanxd.codeapi.util.codeType
import com.github.jonathanxd.codeapi.util.require
import com.github.jonathanxd.iutils.data.TypedData
import java.lang.reflect.Type

object Util {

    fun resolveType(codeType: ReflectType, data: TypedData): CodeType {

        val type by lazy {
            TYPE_DECLARATION.require(data)
        }

        return if (codeType is Alias.THIS) {
            type
        } else if (codeType is Alias.SUPER) {
            (type as? SuperClassHolder)?.superClass?.codeType ?:
                    throw IllegalStateException("Type '$type' as no super types.")
        } else if (codeType is Alias.INTERFACE) {
            val n = codeType.n

            (type as? ImplementationHolder)?.implementations?.map { it.codeType }?.getOrNull(n) ?:
                    throw IllegalStateException("Type '$type' as no implementation or the index '$n' exceed the amount of implementations in the type.")

        } else {
            codeType.codeType
        }

    }

    tailrec fun getOwner(typeDeclaration: TypeDeclaration): TypeDeclaration =
            if (typeDeclaration.outerClass == null || typeDeclaration.outerClass !is TypeDeclaration)
                typeDeclaration
            else
                this.getOwner(typeDeclaration.outerClass as TypeDeclaration)


}

val MethodInvocation.isSuperConstructorInvocation get() = this.spec.methodName == "<init>" && this.target == Alias.SUPER

fun getTypes(current: TypeDeclaration, data: TypedData): List<TypeDeclaration> {
    val list = mutableListOf<TypeDeclaration>()
    var parent: TypedData? = data
    var found = false

    while(parent != null) {
        TYPES.getOrNull(parent)?.let {
            it.forEach {
                if (it.modifiers.contains(CodeModifier.STATIC)) {
                    if (!found && !it.`is`(current))
                        throw IllegalStateException("Found static outer class before finding the current class.")
                    if (found) list.add(it)
                    return list
                } else {
                    if (found) list.add(it)
                }

                if (!found && it.`is`(current))
                    found = true
            }

        }

        parent = parent.parent
    }

    return list
}

/**
 * Gets arguments to be used to construct an inner type.
 *
 * [typeToFind] is the type of the inner class
 */
fun getInnerSpec(typeToFind: Type, data: TypedData): InnerConstructorSpec? {
    val type = TYPE_DECLARATION.getOrNull(data)

    val all = type.allInnerTypes()

    val first = all.firstOrNull { it.`is`(typeToFind) }

    if (first != null) {
        val argTypes = mutableListOf<Type>()
        val args = mutableListOf<CodeInstruction>()

        if (first.modifiers.contains(CodeModifier.STATIC))
            return null

        val outer = OUTER_TYPES_FIELDS.getOrNull(data)?.firstOrNull { it.typeDeclaration.`is`(type) }

        outer?.fields?.mapTo(args) {
            argTypes += it.type
            accessField(Alias.THIS, Access.THIS, it.type, it.name)
        }

        argTypes += type
        args += Access.THIS

        return InnerConstructorSpec(argTypes, args)
    }

    return null
}

data class InnerConstructorSpec(val argTypes: List<Type>, val args: List<CodeInstruction>)