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
package com.github.jonathanxd.codeapi.bytecode.gen.visitor

import com.github.jonathanxd.codeapi.*
import com.github.jonathanxd.codeapi.common.*
import com.github.jonathanxd.codeapi.bytecode.BytecodeClass
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.inspect.SourceInspect
import com.github.jonathanxd.codeapi.interfaces.*
import com.github.jonathanxd.codeapi.types.CodeType
import com.github.jonathanxd.codeapi.util.Lazy
import com.github.jonathanxd.codeapi.util.element.ElementUtil
import com.github.jonathanxd.codeapi.bytecode.util.CodeTypeUtil
import com.github.jonathanxd.codeapi.bytecode.util.InnerUtil
import com.github.jonathanxd.codeapi.bytecode.util.ModifierUtil
import com.github.jonathanxd.iutils.container.MutableContainer
import com.github.jonathanxd.iutils.data.MapData
import com.github.jonathanxd.iutils.type.TypeInfo
import org.objectweb.asm.ClassWriter
import java.util.*
import java.util.function.BiConsumer

object Util {

    @Suppress("UNCHECKED_CAST")
    fun <T> find(typeInfo: TypeInfo<T>, data: MapData, additional: Any?): T {
        val aClass = typeInfo.aClass

        val optional = data.getOptional(typeInfo)

        if (additional != null && aClass.isInstance(additional)) {
            return additional as T
        } else {
            return optional.orElseThrow { IllegalArgumentException("Could not determine: $typeInfo! You must to register. Current additional data: $additional") }
        }
    }

    fun grabAndRemoveInnerDecl(source: CodeSource?): Pair<List<TypeDeclaration>, MutableCodeSource>? {

        if (source == null)
            return null

        val typeDeclarationList = java.util.ArrayList<TypeDeclaration>()
        val codeSource = MutableCodeSource()

        val pair = typeDeclarationList to codeSource

        for (part in source) {
            if (part is CodeSource) {
                val listCodeSourcePair = Util.grabAndRemoveInnerDecl(part)

                typeDeclarationList.addAll(listCodeSourcePair!!.first)

                codeSource.add(listCodeSourcePair.second)
            } else {
                if (part is TypeDeclaration) {
                    typeDeclarationList.add(part)
                } else {
                    codeSource.add(part)
                }
            }
        }

        return pair
    }

    fun visitInner(cw: ClassWriter, outer: TypeDeclaration, innerClasses: List<TypeDeclaration>): List<TypeDeclaration> {

        val visited = java.util.ArrayList<TypeDeclaration>()
        val name = CodeTypeUtil.codeTypeToSimpleAsm(outer)

        for (innerClass in innerClasses) {
            val modifiers = ModifierUtil.innerModifiersToAsm(innerClass)
            cw.visitInnerClass(CodeTypeUtil.codeTypeToSimpleAsm(innerClass), name, innerClass.qualifiedName, modifiers)


            val source = MutableCodeSource(innerClass.body.orElse(CodeSource.empty()))

            val instructionCodePart = InstructionCodePart.create { _, extraData, _, _ ->
                extraData.getRequired(TypeVisitor.CLASS_WRITER_REPRESENTATION)
                        .visitInnerClass(CodeTypeUtil.codeTypeToSimpleAsm(innerClass), name, innerClass.qualifiedName, modifiers)
            }

            source.add(0, instructionCodePart)

            visited.add(innerClass.setBody(source))
        }

        return visited
    }

    /**
     * Fixes access of outer classes trying to access inner class elements.
     *
     * @param accessor     Accessor
     * @param extraData    Data
     * @param localization Localization
     * @param consumer     Consumer
     * @param T          Type
     * @return Fixed accessor.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Accessor> fixAccessor(accessor: T, extraData: MapData,
                                   localization: MutableContainer<CodeType>,
                                   consumer: ((MutableContainer<T>, InnerType) -> Unit)?): T {
        var accessor = accessor


        val innerTypes = extraData.getAllAsList(TypeVisitor.INNER_TYPE_REPRESENTATION)

        for (innerType in innerTypes) {

            val originalDeclaration = innerType.originalDeclaration

            if (originalDeclaration.modifiers.contains(CodeModifier.STATIC))
                continue


            if (originalDeclaration.`is`(localization.get())) {
                localization.set(innerType.adaptedDeclaration)

                accessor = accessor.setLocalization(localization.get()) as T

                if (consumer != null) {
                    val container = MutableContainer.of(accessor)
                    consumer(container, innerType)
                    accessor = container.get()
                }

                return accessor
            }
        }


        return accessor
    }

    /**
     * Fixes access to outer class element from inner class.
     *
     * @param extraData    Data
     * @param target       Target
     * @param localization Localization
     * @return Access to its enclosing class.
     */
    fun accessEnclosingClass(extraData: MapData,
                             target: CodePart,
                             localization: CodeType?): CodePart? {
        val enclosingType = Lazy<CodeType> { extraData.getRequired(TypeVisitor.CODE_TYPE_REPRESENTATION, "Cannot determine current type!") }

        if (target is AccessThis && localization != null && !localization.`is`(enclosingType.get())) {
            val allAsList = extraData.getAllAsList(TypeVisitor.OUTER_FIELD_REPRESENTATION)

            for (fieldDeclaration in allAsList) {
                if (fieldDeclaration.variableType.`is`(localization)) {
                    return CodeAPI.accessThisField(fieldDeclaration.variableType, fieldDeclaration.name)
                }
            }

        }

        return null
    }

    /**
     * Fixes access to outer class element from inner class.
     *
     * @param extraData Data
     * @return Access to its enclosing class.
     */
    fun accessEnclosingClass(extraData: MapData,
                             type: CodeType): CodePart? {
        val allAsList = extraData.getAllAsList(TypeVisitor.OUTER_FIELD_REPRESENTATION)

        allAsList.filter { it.variableType.`is`(type) }
                .forEach { return CodeAPI.accessThisField(it.variableType, it.name) }


        return null
    }

    fun access(part: CodePart, localization: CodeType?, visitorGenerator: VisitorGenerator<BytecodeClass>, extraData: MapData, additional: Any): Array<BytecodeClass>? {
        if (localization != null) {
            var declaringOpt = extraData.getOptional(TypeVisitor.OUTER_TYPE_REPRESENTATION)

            var innerType: InnerType? = null
            var infos: MemberInfos? = null

            if (!declaringOpt.isPresent) {
                val innerTypes = extraData.getAllAsList(TypeVisitor.INNER_TYPE_REPRESENTATION)

                for (inner in innerTypes) {
                    val adaptedDeclaration = inner.adaptedDeclaration

                    if (adaptedDeclaration.`is`(localization)) {
                        infos = inner.memberInfos
                        declaringOpt = Optional.of(adaptedDeclaration)

                        innerType = inner
                    }
                }


            }

            if (innerType == null && extraData.parent != null) {
                infos = extraData.parent.getOptional(ConstantDatas.MEMBER_INFOS).orElse(null)
            }

            if (infos != null && declaringOpt.isPresent) {

                // If accessing enclosing class.
                if (localization.`is`(declaringOpt.get())) {


                    val memberInfo: MemberInfo?
                    var isConstructor = false

                    val codeArguments = ArrayList<CodeArgument>()

                    var target: CodePart? = (part as Accessor).target.orElse(null)

                    if (part is VariableAccess) {
                        memberInfo = infos.find(part)
                    } else {
                        val spec = (part as MethodInvocation).spec
                        memberInfo = infos.find(spec)
                        codeArguments.addAll(spec.arguments)
                        isConstructor = spec.methodName == "<init>"
                    }

                    if (memberInfo != null && !memberInfo.isAccessible) {
                        if (!memberInfo.hasAccessibleMember() || isConstructor) {
                            InnerUtil.genOuterAccessor(declaringOpt.get(), innerType, memberInfo, extraData, visitorGenerator, isConstructor)
                        }

                        val accessibleMember = memberInfo.accessibleMember as MethodDeclaration

                        if (isConstructor) {
                            codeArguments.add(CodeAPI.argument(CodeAPI.accessThis()))
                            target = null
                        }

                        val invoke = ElementUtil.invoke(accessibleMember, target, codeArguments, declaringOpt.get())

                        return visitorGenerator.generateTo(MethodInvocation::class.java, invoke, extraData, additional)

                    }
                }
            }

        }

        return null
    }

}