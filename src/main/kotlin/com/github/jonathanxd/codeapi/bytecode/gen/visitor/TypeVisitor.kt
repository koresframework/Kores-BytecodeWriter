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

import com.github.jonathanxd.codeapi.CodeAPI
import com.github.jonathanxd.codeapi.CodeSource
import com.github.jonathanxd.codeapi.common.CodeModifier
import com.github.jonathanxd.codeapi.common.InnerType
import com.github.jonathanxd.codeapi.bytecode.BytecodeClass
import com.github.jonathanxd.codeapi.bytecode.gen.BytecodeGenerator
import com.github.jonathanxd.codeapi.gen.visit.Visitor
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.impl.CodeConstructor
import com.github.jonathanxd.codeapi.interfaces.*
import com.github.jonathanxd.codeapi.types.CodeType
import com.github.jonathanxd.codeapi.types.GenericType
import com.github.jonathanxd.codeapi.bytecode.util.CodeTypeUtil
import com.github.jonathanxd.codeapi.bytecode.util.GenericUtil
import com.github.jonathanxd.codeapi.bytecode.util.ModifierUtil
import com.github.jonathanxd.codeapi.bytecode.util.TypeDeclarationUtil
import com.github.jonathanxd.codeapi.util.MemberInfosUtil
import com.github.jonathanxd.codeapi.util.source.CodeSourceUtil
import com.github.jonathanxd.iutils.data.MapData
import com.github.jonathanxd.iutils.type.TypeInfo
import org.objectweb.asm.ClassWriter
import java.lang.reflect.Modifier
import java.util.*

object TypeVisitor : Visitor<TypeDeclaration, BytecodeClass, Any?> {

    val CODE_TYPE_REPRESENTATION = TypeInfo.a(TypeDeclaration::class.java).setUnique(true).build()

    val CLASS_WRITER_REPRESENTATION = TypeInfo.a(ClassWriter::class.java).setUnique(true).build()

    val OUTER_TYPE_REPRESENTATION = TypeInfo.aUnique(TypeDeclaration::class.java)

    val INNER_TYPE_REPRESENTATION = TypeInfo.aUnique(InnerType::class.java)

    val OUTER_FIELD_REPRESENTATION = TypeInfo.aUnique(FieldDeclaration::class.java)

    override fun visit(typeDeclaration: TypeDeclaration, extraData: MapData, visitorGenerator: VisitorGenerator<BytecodeClass>, additional: Any?): Array<BytecodeClass> {
        val cw = ClassWriter(ClassWriter.COMPUTE_MAXS or ClassWriter.COMPUTE_FRAMES)

        val sourceFile = extraData
                .getOptional(BytecodeGenerator.SOURCE_FILE_FUNCTION)
                .map { func -> func(typeDeclaration) }
                .orElse("${typeDeclaration.simpleName}.cai") //CodeAPI Instructions

        cw.visitSource(sourceFile, null)

        extraData.registerData(CODE_TYPE_REPRESENTATION, typeDeclaration)
        extraData.registerData(CLASS_WRITER_REPRESENTATION, cw)

        var any = false

        for (innerType in extraData.getAllAsList(INNER_TYPE_REPRESENTATION)) {
            if (innerType.adaptedDeclaration.`is`(typeDeclaration)) {
                extraData.registerData(ConstantDatas.MEMBER_INFOS, innerType.memberInfos)
                any = true
            }
        }

        if (!any)
            extraData.registerData(ConstantDatas.MEMBER_INFOS, MemberInfosUtil.createMemberInfos(typeDeclaration))

        val implementations = if (typeDeclaration is Implementer)
            typeDeclaration.implementations
        else
            emptyList<CodeType>()

        // ASM Class name
        val className = TypeDeclarationUtil.getClassName(typeDeclaration)
        // ASM Class modifiers
        val modifiers = ModifierUtil.modifiersToAsm(typeDeclaration)
        // ASM Super Class implementation
        val superClass = TypeDeclarationUtil.getSuperClass(typeDeclaration)
        // ASM Implementations
        val asmImplementations = implementations.map({ CodeTypeUtil.codeTypeToSimpleAsm(it) }).toTypedArray()

        val superClassIsGeneric = superClass is GenericType
        val anyInterfaceIsGeneric = implementations.stream().anyMatch { codeType -> codeType is GenericType }

        // Generic Types
        val genericRepresentation = GenericUtil.genericTypesToAsmString(typeDeclaration, superClass, implementations, superClassIsGeneric, anyInterfaceIsGeneric)

        // Visit class
        cw.visit(52, modifiers, className, genericRepresentation, CodeTypeUtil.codeTypeToSimpleAsm(superClass), asmImplementations)

        // Visit Annotations
        visitorGenerator.generateTo(Annotable::class.java, typeDeclaration, extraData, null, null)

        val bodyOpt = typeDeclaration.body

        val pair = Util.grabAndRemoveInnerDecl(bodyOpt.orElse(null))

        var typeDeclarationList: List<TypeDeclaration>

        if (bodyOpt.isPresent) {

            typeDeclarationList = pair!!.first

            for (declaration in typeDeclarationList) {
                val outerClassOpt = declaration.outerClass

                if (!outerClassOpt.isPresent)
                    throw IllegalArgumentException("No outer class defined to type: '$declaration'!")

                if (!outerClassOpt.get().`is`(typeDeclaration))

                    throw IllegalArgumentException("Outer class specified to '" + declaration + "' don't matches the real outer class. " +
                            "Specified: '" + outerClassOpt.get() + "'," +
                            "Real: '" + typeDeclaration + "'!")
            }

            val originalDeclList = ArrayList(typeDeclarationList)

            typeDeclarationList = Util.visitInner(cw, typeDeclaration, typeDeclarationList)

            for (i in originalDeclList.indices) {
                // Register inner types.
                extraData.registerData(INNER_TYPE_REPRESENTATION, InnerType(originalDeclList[i], typeDeclarationList[i]))
            }

            val body = pair.second

            // Create outer fields

            val allAsList = extraData.getAllAsList(OUTER_TYPE_REPRESENTATION)

            if (!typeDeclaration.modifiers.contains(CodeModifier.STATIC)) {

                if (!allAsList.isEmpty()) {

                    for (declaration in allAsList) {

                        val simpleName = declaration.simpleName

                        val name = Character.toLowerCase(simpleName[0]) + if (simpleName.length > 1) simpleName.substring(1) else ""

                        val newName = CodeSourceUtil.getNewFieldName(name + "\$outer", body)

                        val field = CodeAPI.field(Modifier.PRIVATE or Modifier.FINAL, declaration, newName)

                        extraData.registerData(OUTER_FIELD_REPRESENTATION, field)

                        body.add(0, field)
                    }

                }
            }


            // /Create outer fields

            if (body.size() > 0) {
                visitorGenerator.generateTo(CodeSource::class.java, body, extraData, null, null)
            }

            val constructor = body.stream().filter { c -> c is ConstructorDeclaration }.findAny().isPresent

            if (!constructor && typeDeclaration.classType.isClass) { // Interfaces has no super call.
                val codeConstructor = CodeConstructor(typeDeclaration, setOf(CodeModifier.PUBLIC), emptyList(), null)
                visitorGenerator.generateTo(ConstructorDeclaration::class.java, codeConstructor, extraData, null, null)
            }

        }

        MethodFragmentVisitor.visitFragmentsGeneration(visitorGenerator, extraData)

        StaticBlockVisitor.generate(extraData, visitorGenerator, cw, typeDeclaration)

        val bytecodeClassList = ArrayList<BytecodeClass>()

        if (pair != null) {
            // Visit inner classes

            val data0 = extraData.newChild()

            extraData.getAllAsList(OUTER_TYPE_REPRESENTATION)
                    .forEach { typeDcl -> data0.registerData(OUTER_TYPE_REPRESENTATION, typeDcl) }

            data0.getAllAsList(ConstantDatas.MEMBER_INFOS)
                    .forEach { memberInfos -> data0.registerData(ConstantDatas.MEMBER_INFOS, memberInfos) }

            data0.registerData(OUTER_TYPE_REPRESENTATION, typeDeclaration)

            extraData.getAllAsList(INNER_TYPE_REPRESENTATION).map { it.adaptedDeclaration }
                    .forEach { declaration ->
                        val data = data0.clone() as MapData

                        val gen = visitorGenerator.gen(declaration, data, null)

                        Collections.addAll(bytecodeClassList, *gen)
                    }
        }

        cw.visitEnd()

        bytecodeClassList.add(0, BytecodeClass(typeDeclaration, cw.toByteArray(), extraData.clone() as MapData))

        return bytecodeClassList.toTypedArray()
    }

    override fun endVisit(r: Array<out BytecodeClass>, typeDeclaration: TypeDeclaration, extraData: MapData, visitorGenerator: VisitorGenerator<BytecodeClass>, additional: Any?) {
        extraData.unregisterData(CODE_TYPE_REPRESENTATION, typeDeclaration)

        val optional = extraData.getOptional(CLASS_WRITER_REPRESENTATION)

        if (optional.isPresent) {
            extraData.unregisterData(CLASS_WRITER_REPRESENTATION, optional.get())
        }

        extraData.unregisterData(OUTER_TYPE_REPRESENTATION, typeDeclaration)
        extraData.unregisterAllData(OUTER_FIELD_REPRESENTATION)
        extraData.unregisterAllData(INNER_TYPE_REPRESENTATION)
    }

}