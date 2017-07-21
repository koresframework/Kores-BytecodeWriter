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

import com.github.jonathanxd.codeapi.CodeElement
import com.github.jonathanxd.codeapi.CodeInstruction
import com.github.jonathanxd.codeapi.CodeSource
import com.github.jonathanxd.codeapi.base.*
import com.github.jonathanxd.codeapi.bytecode.processor.*
import com.github.jonathanxd.codeapi.bytecode.util.ReflectType
import com.github.jonathanxd.codeapi.bytecode.util.allInnerTypes
import com.github.jonathanxd.codeapi.bytecode.util.allTypes
import com.github.jonathanxd.codeapi.common.FieldRef
import com.github.jonathanxd.codeapi.common.getNewName
import com.github.jonathanxd.codeapi.common.getNewNameBasedOnNameList
import com.github.jonathanxd.codeapi.factory.*
import com.github.jonathanxd.codeapi.type.CodeType
import com.github.jonathanxd.codeapi.util.*
import com.github.jonathanxd.codeapi.util.conversion.access
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

    while (parent != null) {
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

        argTypes += type
        args += Access.THIS

        return InnerConstructorSpec(argTypes, args)
    }

    return null
}

data class InnerConstructorSpec(val argTypes: List<Type>, val args: List<CodeInstruction>)

fun accessMemberOfType(memberOwner: Type, accessor: Accessor, data: TypedData): MemberAccess? {
    val type = TYPE_DECLARATION.getOrNull(data)

    val top = getTopLevelOuter(data)
    val all = top.allTypes()

    val target = all.firstOrNull { it.`is`(memberOwner) }
    val targetData = data.mainData

    if (target != null) {
        if (accessor.localization.`is`(target)) {
            val member: CodeElement? =
                    if (accessor is FieldAccess) {
                        val field = target.fields.firstOrNull {
                            it.name == accessor.name
                                    && it.type.isConcreteIdEq(accessor.type)
                        }

                        if (field != null && field.modifiers.contains(CodeModifier.PRIVATE))
                            field
                        else null
                    } else if (accessor is MethodInvocation) {
                        val method = target.methods.firstOrNull {
                            it.name == accessor.spec.methodName
                                    && it.typeSpec.isConreteEq(accessor.spec.typeSpec)
                        }

                        val ctr = getConstructors(target).firstOrNull {
                            it.name == accessor.spec.methodName
                                    && it.typeSpec.isConreteEq(accessor.spec.typeSpec)
                        }

                        if (method != null && method.modifiers.contains(CodeModifier.PRIVATE))
                            method
                        else if (accessor.invokeType == InvokeType.INVOKE_SPECIAL
                                && (ctr != null && ctr.modifiers.contains(CodeModifier.PRIVATE))
                                || (target as? ConstructorsHolder)?.constructors.orEmpty().isEmpty())
                            ctr
                        else null
                    } else null

            member?.let {
                MEMBER_ACCESSES.getOrNull(targetData)?.forEach { e ->
                    if (e.member == it)
                        return e
                }

                val existingNames = MEMBER_ACCESSES.getOrNull(targetData).orEmpty().filter { it.owner.`is`(target) }.map {
                    (it.newElementToAccess as Named).name
                }

                val name = getNewNameBasedOnNameList("accessor\$",
                        target.methods.map { it.name } + existingNames
                )

                val newMember: MethodDeclarationBase = when (it) {
                    is FieldDeclaration -> MethodDeclaration.Builder.builder()
                            .modifiers(CodeModifier.PACKAGE_PRIVATE, CodeModifier.SYNTHETIC, CodeModifier.STATIC)
                            .returnType(it.type)
                            .name(name)
                            .parameters(parameter(type = target, name = "this"))
                            .body(CodeSource.fromPart(
                                    returnValue(it.type, accessField(Alias.THIS, Access.THIS, it.type, it.name))
                            ))
                            .build()
                    is MethodDeclaration -> {
                        accessor as MethodInvocation

                        MethodDeclaration.Builder.builder()
                                .modifiers(CodeModifier.PACKAGE_PRIVATE, CodeModifier.SYNTHETIC, CodeModifier.STATIC)
                                .returnType(it.returnType)
                                .name(name)
                                .parameters(listOf(parameter(type = target, name = "this")) + it.parameters)
                                .body(CodeSource.fromPart(
                                        returnValue(it.type,
                                                invoke(accessor.invokeType,
                                                        Alias.THIS,
                                                        Access.THIS,
                                                        it.name,
                                                        it.typeSpec,
                                                        it.parameters.access
                                                )
                                        )

                                ))
                                .build()
                    }
                    is ConstructorDeclaration -> {
                        val newPname = getNewName("access\$",
                                it.parameters
                        )

                        val newName = getNewName("\$", INNER_CLASSES.require(data))

                        val innerType = ClassDeclaration.Builder.builder()
                                .outerClass(target)
                                .modifiers(CodeModifier.PACKAGE_PRIVATE, CodeModifier.SYNTHETIC, CodeModifier.STATIC)
                                .name(newName)
                                .build()

                        INNER_CLASSES.add(data, innerType)

                        ConstructorDeclaration.Builder.builder()
                                .modifiers(CodeModifier.PACKAGE_PRIVATE, CodeModifier.SYNTHETIC)
                                .innerTypes(innerType)
                                .parameters(it.parameters + parameter(type = innerType, name = newPname))
                                .body(CodeSource.fromPart(
                                        invokeThisConstructor(it.typeSpec, it.parameters.access)
                                ))
                                .build()

                    }
                    else -> TODO()
                }

                MemberAccess(type, member, target, newMember).also {
                    MEMBER_ACCESSES.add(targetData, it)
                    return it
                }
            }
        }
    }

    return null
}

fun getTopLevelOuter(data: TypedData): TypeDeclaration {
    var parent: TypedData? = data
    var last: TypeDeclaration? = null

    while (parent != null) {
        TYPE_DECLARATION.getOrNull(parent)?.let {
            last = it
        }

        parent = parent.parent
    }

    return last ?: throw IllegalStateException("Cannot find top level outer type in Data: {$data}")
}

fun getConstructors(part: TypeDeclaration): List<ConstructorDeclaration> {
    val isStatic = part.modifiers.contains(CodeModifier.STATIC)
    val outerType = part.outerClass

    if (!isStatic && outerType != null) {

        val localLocalPart = part
        if (localLocalPart is ConstructorsHolder && !isStatic && !outerType.isInterface) {

            val allNames = localLocalPart.fields.map { it.name } + localLocalPart.constructors.flatMap {
                it.parameters.map { it.name }
            }

            val singleName = getNewNameBasedOnNameList(TypeDeclarationProcessor.baseOuterName, allNames)

            val newCtrs =
                    if (localLocalPart.constructors.isNotEmpty()) {
                        localLocalPart.constructors.map {
                            val newParams = listOf(parameter(type = outerType, name = singleName)) + it.parameters

                            it.builder()
                                    .parameters(newParams)
                                    .build()
                        }
                    } else {
                        listOf(ConstructorDeclaration.Builder.builder()
                                .modifiers(part.modifiers.filter { it.modifierType == ModifierType.VISIBILITY }.toSet())
                                .parameters(parameter(type = outerType, name = singleName))
                                .build())
                    }

            return newCtrs
        }
    }

    return (part as? ConstructorsHolder)?.constructors.orEmpty()
}