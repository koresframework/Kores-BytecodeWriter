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

import com.github.jonathanxd.codeapi.CodeSource
import com.github.jonathanxd.codeapi.base.Annotable
import com.github.jonathanxd.codeapi.base.ConstructorDeclaration
import com.github.jonathanxd.codeapi.base.ImplementationHolder
import com.github.jonathanxd.codeapi.base.TypeDeclaration
import com.github.jonathanxd.codeapi.bytecode.BytecodeClass
import com.github.jonathanxd.codeapi.bytecode.gen.BytecodeGenerator
import com.github.jonathanxd.codeapi.bytecode.util.*
import com.github.jonathanxd.codeapi.common.CodeModifier
import com.github.jonathanxd.codeapi.common.Data
import com.github.jonathanxd.codeapi.common.InnerType
import com.github.jonathanxd.codeapi.common.MemberInfos
import com.github.jonathanxd.codeapi.factory.constructor
import com.github.jonathanxd.codeapi.factory.field
import com.github.jonathanxd.codeapi.gen.visit.Visitor
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.type.CodeType
import com.github.jonathanxd.codeapi.type.GenericType
import com.github.jonathanxd.codeapi.util.MemberInfosUtil
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import java.util.*

object TypeVisitor : Visitor<TypeDeclaration, BytecodeClass, Any?> {

    // TypeDeclaration
    val TYPE_DECLARATION_REPRESENTATION = "TYPE_DECLARATION"

    // ClassVisitor
    val CLASS_VISITOR_REPRESENTATION = "CLASS_VISITOR"

    // TypeDeclaration
    val OUTER_TYPE_REPRESENTATION = "OUTER_YPE"

    // InnerType
    val INNER_TYPE_REPRESENTATION = "INNER_TYPE"

    // FieldDeclaration
    val OUTER_FIELD_REPRESENTATION = "OUTER_FIELD"

    override fun visit(t: TypeDeclaration, extraData: Data, visitorGenerator: VisitorGenerator<BytecodeClass>, additional: Any?): Array<BytecodeClass> {

        /*val extraData = if(additional == null)
            data
        else
            data.clone()*/

        val baseDataClone = extraData.clone()

        /*if(additional != null) {
            val outer = data.getRequired<TypeDeclaration>(TYPE_DECLARATION_REPRESENTATION)
            val cw = data.getRequired<ClassVisitor>(CLASS_VISITOR_REPRESENTATION)

            if(!outer.`is`(t.outerClass))
                throw IllegalArgumentException("Outer class specified to '" + t + "' don't matches the real outer class. " +
                        "Specified: '" + t.outerClass + "'," +
                        "Real: '" + outer + "'!")

            val inn = Util.visitInner(cw, outer, listOf(t))



            data.registerData(INNER_TYPE_REPRESENTATION, )
        }*/

        val cw = ClassWriter(ClassWriter.COMPUTE_MAXS or ClassWriter.COMPUTE_FRAMES)


        extraData.registerData(TYPE_DECLARATION_REPRESENTATION, t)
        extraData.registerData(CLASS_VISITOR_REPRESENTATION, cw)

        var any = false

        var memberInfos: MemberInfos? = null

        for (innerType in extraData.getAllAsList<InnerType>(INNER_TYPE_REPRESENTATION)) {
            if (innerType.adaptedDeclaration.`is`(t)) {
                extraData.registerData(ConstantDatas.MEMBER_INFOS, innerType.memberInfos)
                memberInfos = innerType.memberInfos
                any = true
            }
        }

        if (!any) {
            memberInfos = MemberInfosUtil.createMemberInfos(t)
            extraData.registerData(ConstantDatas.MEMBER_INFOS, memberInfos)
        }

        memberInfos!!

        val implementations = if (t is ImplementationHolder)
            t.implementations
        else
            emptyList<CodeType>()

        // ASM Class name
        val className = TypeDeclarationUtil.getClassName(t)
        // ASM Class modifiers
        val modifiers = ModifierUtil.modifiersToAsm(t)
        // ASM Super Class implementation
        val superClass = TypeDeclarationUtil.getSuperClass(t)
        // ASM Implementations
        val asmImplementations = implementations.map { CodeTypeUtil.codeTypeToBinaryName(it) }.toTypedArray()

        val superClassIsGeneric = superClass is GenericType
        val anyInterfaceIsGeneric = implementations.stream().anyMatch { codeType -> codeType is GenericType }

        // Generic Types
        val genericRepresentation = GenericUtil.genericTypesToAsmString(t, superClass, implementations, superClassIsGeneric, anyInterfaceIsGeneric)

        // Visit class
        cw.visit(52, modifiers, className, genericRepresentation, CodeTypeUtil.codeTypeToBinaryName(superClass), asmImplementations)

        val sourceFile = extraData
                .getOptional<(TypeDeclaration) -> String>(BytecodeGenerator.SOURCE_FILE_FUNCTION)
                .map { func -> func(t) }
                .orElse("${Util.getOwner(t).simpleName}.cai") //CodeAPI Instructions

        cw.visitSource(sourceFile, null)


        // Visit Annotations
        visitorGenerator.generateTo(Annotable::class.java, t, extraData, null, null)

        val pair = Util.grabAndRemoveInnerDecl(t.body)

        var typeDeclarationList: List<TypeDeclaration>

        typeDeclarationList = pair.first

        for (declaration in typeDeclarationList) {
            val outerClass = declaration.outerClass ?: throw IllegalArgumentException("No outer class defined to type: '$declaration'!")

            if (!outerClass.`is`(t))

                throw IllegalArgumentException("Outer class specified to '" + declaration + "' don't matches the real outer class. " +
                        "Specified: '" + outerClass + "'," +
                        "Real: '" + t + "'!")
        }

        val originalDeclList = ArrayList(typeDeclarationList)

        typeDeclarationList = Util.visitInner(cw, t, typeDeclarationList)

        for (i in originalDeclList.indices) {
            // Register inner types.
            extraData.registerData(INNER_TYPE_REPRESENTATION, InnerType(originalDeclList[i], typeDeclarationList[i]))
        }

        val body = pair.second

        // Create outer fields

        val allAsList = extraData.getAllAsList<TypeDeclaration>(OUTER_TYPE_REPRESENTATION)

        if (!t.modifiers.contains(CodeModifier.STATIC)) {

            if (!allAsList.isEmpty()) {

                for (declaration in allAsList) {

                    val simpleName = declaration.simpleName

                    val name = Character.toLowerCase(simpleName[0]) + if (simpleName.length > 1) simpleName.substring(1) else ""

                    //val newName = CodeSourceUtil.getNewFieldName(name + "\$outer", body)

                    // All Members are cached, this method should be better than reading the entirely body
                    val newName = Util.getNewName(name + "\$outer", declaration, memberInfos, t)

                    val field = field(EnumSet.of(CodeModifier.PRIVATE, CodeModifier.FINAL), declaration, newName)

                    extraData.registerData(OUTER_FIELD_REPRESENTATION, field)

                    body.add(0, field)
                }

            }
        }


        // /Create outer fields

        if (body.size > 0) {
            visitorGenerator.generateTo(CodeSource::class.java, body, extraData, null, null)
        }

        val hasConstructor = body.stream().filter { c -> c is ConstructorDeclaration }.findAny().isPresent

        if (!hasConstructor && !t.isInterface) { // Interfaces has no super call.
            val codeConstructor = constructor(EnumSet.of(CodeModifier.PUBLIC), arrayOf(), CodeSource.empty())
            visitorGenerator.generateTo(ConstructorDeclaration::class.java, codeConstructor, extraData, null, null)
        }



        MethodFragmentVisitor.visitFragmentsGeneration(visitorGenerator, extraData)

        StaticBlockVisitor.generate(extraData, visitorGenerator, cw, t)

        val bytecodeClassList = ArrayList<BytecodeClass>()


        // Visit inner classes

        val data0 = extraData.newChild()

        extraData.getAllAsList<TypeDeclaration>(OUTER_TYPE_REPRESENTATION)
                .forEach { typeDcl -> data0.registerData(OUTER_TYPE_REPRESENTATION, typeDcl) }

        data0.registerData(OUTER_TYPE_REPRESENTATION, t)

        extraData.getAllAsList<SwitchOnEnum.Mapping>(SwitchOnEnum.MAPPINGS).forEach {
            val clone = baseDataClone.clone()

            val gen = visitorGenerator.gen(it.buildClass(), clone, null)

            Collections.addAll(bytecodeClassList, *gen)
        }

        extraData.getAllAsList<InnerType>(INNER_TYPE_REPRESENTATION).map { it.adaptedDeclaration }
                .forEach { declaration ->
                    val data = data0.clone()

                    val gen = visitorGenerator.gen(declaration, data, null)

                    Collections.addAll(bytecodeClassList, *gen)
                }


        cw.visitEnd()

        bytecodeClassList.add(0, BytecodeClass(t, cw.toByteArray()))

        return bytecodeClassList.toTypedArray()
    }

    override fun endVisit(r: Array<out BytecodeClass>, t: TypeDeclaration, extraData: Data, visitorGenerator: VisitorGenerator<BytecodeClass>, additional: Any?) {
        extraData.unregisterData(TYPE_DECLARATION_REPRESENTATION, t)

        val optional = extraData.getOptional<ClassVisitor>(CLASS_VISITOR_REPRESENTATION)

        if (optional.isPresent) {
            extraData.unregisterData(CLASS_VISITOR_REPRESENTATION, optional.get())
        }

        extraData.unregisterData(OUTER_TYPE_REPRESENTATION, t)
        extraData.unregisterAllData(OUTER_FIELD_REPRESENTATION)
        extraData.unregisterAllData(INNER_TYPE_REPRESENTATION)
    }

}