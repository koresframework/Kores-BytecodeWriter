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
import com.github.jonathanxd.codeapi.base.*
import com.github.jonathanxd.codeapi.bytecode.BytecodeClass
import com.github.jonathanxd.codeapi.bytecode.gen.BytecodeGenerator
import com.github.jonathanxd.codeapi.bytecode.util.CodeTypeUtil
import com.github.jonathanxd.codeapi.bytecode.util.GenericUtil
import com.github.jonathanxd.codeapi.bytecode.util.ModifierUtil
import com.github.jonathanxd.codeapi.bytecode.util.TypeDeclarationUtil
import com.github.jonathanxd.codeapi.common.CodeModifier
import com.github.jonathanxd.codeapi.common.InnerType
import com.github.jonathanxd.codeapi.factory.constructor
import com.github.jonathanxd.codeapi.factory.field
import com.github.jonathanxd.codeapi.gen.visit.Visitor
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.type.CodeType
import com.github.jonathanxd.codeapi.type.GenericType
import com.github.jonathanxd.codeapi.util.MemberInfosUtil
import com.github.jonathanxd.codeapi.util.source.CodeSourceUtil
import com.github.jonathanxd.iutils.data.MapData
import com.github.jonathanxd.iutils.type.TypeInfo
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import java.util.*

object TypeVisitor : Visitor<TypeDeclaration, BytecodeClass, Any?> {

    val CODE_TYPE_REPRESENTATION = TypeInfo.a(TypeDeclaration::class.java).setUnique(true).build()

    val CLASS_VISITOR_REPRESENTATION = TypeInfo.a(ClassVisitor::class.java).setUnique(true).build()

    val OUTER_TYPE_REPRESENTATION = TypeInfo.aUnique(TypeDeclaration::class.java)

    val INNER_TYPE_REPRESENTATION = TypeInfo.aUnique(InnerType::class.java)

    val OUTER_FIELD_REPRESENTATION = TypeInfo.aUnique(FieldDeclaration::class.java)

    override fun visit(t: TypeDeclaration, extraData: MapData, visitorGenerator: VisitorGenerator<BytecodeClass>, additional: Any?): Array<BytecodeClass> {

        val cw = ClassWriter(ClassWriter.COMPUTE_MAXS or ClassWriter.COMPUTE_FRAMES)


        extraData.registerData(CODE_TYPE_REPRESENTATION, t)
        extraData.registerData(CLASS_VISITOR_REPRESENTATION, cw)

        var any = false

        for (innerType in extraData.getAllAsList(INNER_TYPE_REPRESENTATION)) {
            if (innerType.adaptedDeclaration.`is`(t)) {
                extraData.registerData(ConstantDatas.MEMBER_INFOS, innerType.memberInfos)
                any = true
            }
        }

        if (!any)
            extraData.registerData(ConstantDatas.MEMBER_INFOS, MemberInfosUtil.createMemberInfos(t))

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
                .getOptional(BytecodeGenerator.SOURCE_FILE_FUNCTION)
                .map { func -> func(t) }
                .orElse("${t.simpleName}.cai") //CodeAPI Instructions

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

        val allAsList = extraData.getAllAsList(OUTER_TYPE_REPRESENTATION)

        if (!t.modifiers.contains(CodeModifier.STATIC)) {

            if (!allAsList.isEmpty()) {

                for (declaration in allAsList) {

                    val simpleName = declaration.simpleName

                    val name = Character.toLowerCase(simpleName[0]) + if (simpleName.length > 1) simpleName.substring(1) else ""

                    val newName = CodeSourceUtil.getNewFieldName(name + "\$outer", body)

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

        if (!hasConstructor && t.classType.isClass) { // Interfaces has no super call.
            val codeConstructor = constructor(EnumSet.of(CodeModifier.PUBLIC), arrayOf(), CodeSource.empty())
            visitorGenerator.generateTo(ConstructorDeclaration::class.java, codeConstructor, extraData, null, null)
        }



        MethodFragmentVisitor.visitFragmentsGeneration(visitorGenerator, extraData)

        StaticBlockVisitor.generate(extraData, visitorGenerator, cw, t)

        val bytecodeClassList = ArrayList<BytecodeClass>()


        // Visit inner classes

        val data0 = extraData.newChild()

        extraData.getAllAsList(OUTER_TYPE_REPRESENTATION)
                .forEach { typeDcl -> data0.registerData(OUTER_TYPE_REPRESENTATION, typeDcl) }

        data0.getAllAsList(ConstantDatas.MEMBER_INFOS)
                .forEach { memberInfos -> data0.registerData(ConstantDatas.MEMBER_INFOS, memberInfos) }

        data0.registerData(OUTER_TYPE_REPRESENTATION, t)

        extraData.getAllAsList(INNER_TYPE_REPRESENTATION).map { it.adaptedDeclaration }
                .forEach { declaration ->
                    val data = data0.clone() as MapData

                    val gen = visitorGenerator.gen(declaration, data, null)

                    Collections.addAll(bytecodeClassList, *gen)
                }


        cw.visitEnd()

        bytecodeClassList.add(0, BytecodeClass(t, cw.toByteArray()))

        return bytecodeClassList.toTypedArray()
    }

    override fun endVisit(r: Array<out BytecodeClass>, t: TypeDeclaration, extraData: MapData, visitorGenerator: VisitorGenerator<BytecodeClass>, additional: Any?) {
        extraData.unregisterData(CODE_TYPE_REPRESENTATION, t)

        val optional = extraData.getOptional(CLASS_VISITOR_REPRESENTATION)

        if (optional.isPresent) {
            extraData.unregisterData(CLASS_VISITOR_REPRESENTATION, optional.get())
        }

        extraData.unregisterData(OUTER_TYPE_REPRESENTATION, t)
        extraData.unregisterAllData(OUTER_FIELD_REPRESENTATION)
        extraData.unregisterAllData(INNER_TYPE_REPRESENTATION)
    }

}