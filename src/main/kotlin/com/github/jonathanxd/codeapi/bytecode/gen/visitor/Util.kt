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
package com.github.jonathanxd.codeapi.bytecode.gen.visitor

import com.github.jonathanxd.codeapi.CodeAPI
import com.github.jonathanxd.codeapi.CodePart
import com.github.jonathanxd.codeapi.CodeSource
import com.github.jonathanxd.codeapi.MutableCodeSource
import com.github.jonathanxd.codeapi.base.*
import com.github.jonathanxd.codeapi.bytecode.BytecodeClass
import com.github.jonathanxd.codeapi.bytecode.util.CodeTypeUtil
import com.github.jonathanxd.codeapi.bytecode.util.InnerUtil
import com.github.jonathanxd.codeapi.bytecode.util.ModifierUtil
import com.github.jonathanxd.codeapi.common.*
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.type.CodeType
import com.github.jonathanxd.codeapi.util.Alias
import com.github.jonathanxd.codeapi.util.element.ElementUtil
import com.github.jonathanxd.iutils.container.MutableContainer
import com.github.jonathanxd.iutils.description.Description
import org.objectweb.asm.ClassVisitor
import java.util.*
import java.util.concurrent.ThreadLocalRandom

object Util {

    fun resolveType(codeType: CodeType, data: Data, additional: Any?): CodeType {

        val type by lazy {
            this.find<TypeDeclaration>(TypeVisitor.TYPE_DECLARATION_REPRESENTATION, data, additional)
        }

        return if (codeType is Alias.THIS) {
            type
        } else if (codeType is Alias.SUPER) {
            (type as? SuperClassHolder)?.superClass ?:
                    throw IllegalStateException("Type '$type' as no super types.")
        } else if (codeType is Alias.INTERFACE) {
            val n = codeType.n

            (type as? ImplementationHolder)?.implementations?.getOrNull(n) ?:
                    throw IllegalStateException("Type '$type' as no implementation or the index '$n' exceed the amount of implementations in the type.")

        } else {
            codeType
        }

    }

    tailrec fun getOwner(typeDeclaration: TypeDeclaration): TypeDeclaration =
            if (typeDeclaration.outerClass == null || typeDeclaration.outerClass !is TypeDeclaration)
                typeDeclaration
            else
                this.getOwner(typeDeclaration.outerClass as TypeDeclaration)


    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : Any> find(key: String, data: Data, additional: Any?): T {
        val optional = data.getOptional<T>(key)

        if (additional != null && T::class.java.isInstance(additional)) {
            return additional as T
        } else {
            return optional.orElseThrow { IllegalArgumentException("Could not determine value of: $key! You must to register. Current additional data: $additional") }
        }
    }

    fun grabAndRemoveInnerDecl(source: CodeSource): Pair<List<TypeDeclaration>, MutableCodeSource> {

        val typeDeclarationList = java.util.ArrayList<TypeDeclaration>()
        val codeSource = MutableCodeSource()

        val pair = typeDeclarationList to codeSource

        for (part in source) {
            if (part is CodeSource) {
                val listCodeSourcePair = Util.grabAndRemoveInnerDecl(part)

                typeDeclarationList.addAll(listCodeSourcePair.first)

                codeSource.add(listCodeSourcePair.second)
            } else {
                if (part is TypeDeclaration) {
                    typeDeclarationList.add(part)
                } else if (part is MethodDeclaration) {
                    val grabRemovePair = Util.grabAndRemoveInnerDecl(part.body)

                    typeDeclarationList.addAll(grabRemovePair.first)

                    val modifiedMethod = part.builder().withBody(grabRemovePair.second).build()

                    codeSource.add(modifiedMethod)

                } else {
                    codeSource.add(part)
                }
            }
        }

        return pair
    }

    fun visitInner(cw: ClassVisitor, outer: TypeDeclaration, innerClasses: List<TypeDeclaration>): List<TypeDeclaration> {

        val visited = java.util.ArrayList<TypeDeclaration>()
        val name = CodeTypeUtil.codeTypeToBinaryName(outer)

        for (innerClass in innerClasses) {
            val modifiers = ModifierUtil.innerModifiersToAsm(innerClass)
            cw.visitInnerClass(CodeTypeUtil.codeTypeToBinaryName(innerClass), name, innerClass.specifiedName, modifiers)


            val source = MutableCodeSource(innerClass.body)

            val instructionCodePart = InstructionCodePart.create { _, extraData, _, _ ->
                extraData.getRequired<ClassVisitor>(TypeVisitor.CLASS_VISITOR_REPRESENTATION)
                        .visitInnerClass(CodeTypeUtil.codeTypeToBinaryName(innerClass), name, innerClass.specifiedName, modifiers)
            }

            source.add(0, instructionCodePart)

            visited.add(innerClass.builder().withBody(source).build())
        }

        return visited
    }

    fun getNewName(element: Named, extraData: Data): String {
        return this.getNewName(element.name, element, extraData)
    }

    fun getNewName(baseName: String, extraData: Data): String {

        extraData.getOptional<MemberInfos>(ConstantDatas.MEMBER_INFOS).let {
            if (it.isPresent) {
                it.get().let {
                    var endName: String = baseName
                    val memberList = it.getMemberInfoList().map { it.description.elementName }

                    while (memberList.contains(endName))
                        endName += 1

                    return endName
                }
            }
        }

        return baseName + ThreadLocalRandom.current().nextInt().toString().replace('-', '_')

    }

    fun getNewName(baseName: String, element: CodePart, extraData: Data): String {
        val optionalMemberInfos = extraData.getOptional<MemberInfos>(ConstantDatas.MEMBER_INFOS)
        val optionalRoot = extraData.getOptional<TypeDeclaration>(TypeVisitor.TYPE_DECLARATION_REPRESENTATION)

        if (optionalMemberInfos.isPresent && optionalRoot.isPresent)
            return this.getNewName(baseName, element, optionalMemberInfos.get(), optionalRoot.get())

        return baseName + ThreadLocalRandom.current().nextInt().toString().replace('-', '_')
    }

    fun getNewName(baseName: String, element: CodePart, memberInfos: MemberInfos, typeDeclaration: TypeDeclaration): String {

        var desc = MemberInfo.getDescription(typeDeclaration, element)
        var number = 1L

        val elementType = desc.elementType

        val memberInfoList = memberInfos.getMemberInfoList()
                .filter { it.description.elementType == elementType }
                .map { it.description.plainDescription }

        while (memberInfoList.contains(desc.plainDescription)) {
            ++number
            desc = Description(desc.binaryClassName, "$baseName$number", desc.parameterTypes, desc.type, desc.elementType)
        }

        return "$baseName$number"
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
    fun <T : Accessor> fixAccessor(accessor: T, extraData: Data,
                                   localization: MutableContainer<CodeType>,
                                   consumer: ((MutableContainer<T>, InnerType) -> Unit)?): T {
        @Suppress("NAME_SHADOWING")
        var accessor = accessor


        val innerTypes = extraData.getAllAsList<InnerType>(TypeVisitor.INNER_TYPE_REPRESENTATION)

        for (innerType in innerTypes) {

            val originalDeclaration = innerType.originalDeclaration

            if (originalDeclaration.modifiers.contains(CodeModifier.STATIC))
                continue


            if (originalDeclaration.`is`(localization.get())) {
                localization.set(innerType.adaptedDeclaration)

                accessor = accessor.builder().withLocalization(localization.get()).build() as T

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
    fun accessEnclosingClass(extraData: Data,
                             target: CodePart,
                             localization: CodeType?): CodePart? {
        val enclosingType by lazy { extraData.getRequired<TypeDeclaration>(TypeVisitor.TYPE_DECLARATION_REPRESENTATION, "Cannot determine current type!") }

        if ((target is Access && target.type == Access.Type.THIS) && localization != null && !localization.`is`(enclosingType)) {
            val allAsList = extraData.getAllAsList<FieldDeclaration>(TypeVisitor.OUTER_FIELD_REPRESENTATION)

            for (fieldDeclaration in allAsList) {
                if (fieldDeclaration.type.`is`(localization)) {
                    return CodeAPI.accessThisField(fieldDeclaration.type, fieldDeclaration.name)
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
    fun accessEnclosingClass(extraData: Data,
                             type: CodeType): CodePart? {
        val allAsList = extraData.getAllAsList<FieldDeclaration>(TypeVisitor.OUTER_FIELD_REPRESENTATION)

        allAsList.filter { it.type.`is`(type) }
                .forEach { return CodeAPI.accessThisField(it.type, it.name) }


        return null
    }

    fun access(part: CodePart, localization: CodeType?, visitorGenerator: VisitorGenerator<BytecodeClass>, extraData: Data, additional: Any): Array<out BytecodeClass>? {
        if (localization != null) {
            var declaringOpt = extraData.getOptional<TypeDeclaration>(TypeVisitor.OUTER_TYPE_REPRESENTATION)

            var innerType: InnerType? = null
            var infos: MemberInfos? = null

            if (!declaringOpt.isPresent) {
                val innerTypes = extraData.getAllAsList<InnerType>(TypeVisitor.INNER_TYPE_REPRESENTATION)

                for (inner in innerTypes) {
                    val adaptedDeclaration = inner.adaptedDeclaration

                    if (adaptedDeclaration.`is`(localization)) {
                        infos = inner.memberInfos
                        declaringOpt = Optional.of(adaptedDeclaration)

                        innerType = inner
                    }
                }


            }

            val parent = extraData.parent

            if (innerType == null && parent != null) {
                infos = parent.getOptional<MemberInfos>(ConstantDatas.MEMBER_INFOS).orElse(null)
            }

            if (infos != null && declaringOpt.isPresent) {

                // If accessing enclosing class.
                if (localization.`is`(declaringOpt.get())) {


                    val memberInfo: MemberInfo?
                    var isConstructor = false

                    val codeArguments = ArrayList<CodePart>()

                    var target: CodePart = (part as Accessor).target

                    if (part is FieldAccess) {
                        memberInfo = infos.find(part)
                    } else {

                        val invocation = part as MethodInvocation
                        val spec = invocation.spec
                        memberInfo = infos.find(spec)
                        codeArguments.addAll(invocation.arguments)
                        isConstructor = spec.methodName == "<init>"
                    }

                    if (memberInfo != null && !memberInfo.isAccessible) {
                        if (!memberInfo.hasAccessibleMember() || isConstructor) {
                            InnerUtil.genOuterAccessor(declaringOpt.get(), innerType, memberInfo, extraData, visitorGenerator, isConstructor)
                        }

                        val accessibleMember = memberInfo.accessibleMember as MethodDeclaration

                        if (isConstructor) {
                            codeArguments.add(CodeAPI.accessThis())
                            target = part.localization
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