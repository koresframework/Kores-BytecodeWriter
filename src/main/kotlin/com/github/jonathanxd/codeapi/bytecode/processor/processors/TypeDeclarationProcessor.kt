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

import com.github.jonathanxd.codeapi.base.*
import com.github.jonathanxd.codeapi.builder.build
import com.github.jonathanxd.codeapi.bytecode.BytecodeClass
import com.github.jonathanxd.codeapi.bytecode.processor.*
import com.github.jonathanxd.codeapi.bytecode.util.*
import com.github.jonathanxd.codeapi.factory.constructorDec
import com.github.jonathanxd.codeapi.processor.CodeProcessor
import com.github.jonathanxd.codeapi.processor.Processor
import com.github.jonathanxd.codeapi.type.CodeType
import com.github.jonathanxd.codeapi.type.GenericType
import com.github.jonathanxd.codeapi.util.codeType
import com.github.jonathanxd.codeapi.util.genericTypesToDescriptor
import com.github.jonathanxd.codeapi.util.parametersAndReturnToInferredDesc
import com.github.jonathanxd.iutils.data.TypedData
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes

/**
 * This class requires a strict debugging because `data` is recreated
 */
object TypeDeclarationProcessor : Processor<TypeDeclaration> {

    override fun process(part: TypeDeclaration, data: TypedData, codeProcessor: CodeProcessor<*>) {
        val outerType: TypeDeclaration? = TYPE_DECLARATION.getOrNull(data)
        val location: InnerTypesHolder? = LOCATION.getOrNull(data)
        val outerVisitor: ClassVisitor? = CLASS_VISITOR.getOrNull(data)
        val at = BYTECODE_CLASS_LIST.getOrSet(data, mutableListOf()).size
        // Data parameter because each type has your own Data.
        @Suppress("NAME_SHADOWING")
        val data = TypedData(data)

        val cw = ClassWriter(ClassWriter.COMPUTE_MAXS or ClassWriter.COMPUTE_FRAMES)

        TYPE_DECLARATION.set(data, part)
        CLASS_VISITOR.set(data, cw)

        val version = CLASS_VERSION.getOrSet(data, VERSION)

        val name = part.internalName
        val implementations: List<CodeType> = (part as? ImplementationHolder)?.let { it.implementations.map { it.codeType } } ?: emptyList()
        val superClass = ((part as? SuperClassHolder)?.superClass ?: Any::class.java).codeType

        val genericRepresentation = genericTypesToDescriptor(part, superClass,
                implementations,
                superClass is GenericType,
                implementations.any { it is GenericType })

        // ***************************************************************************************** //

        outerVisitor?.let {
            outerType ?: throw IllegalStateException("Found outer visitor but not found outer type")
            visitInner(part, cw, outerType.internalName)
            visitInner(part, it, outerType.internalName)
        }

        if (part.outerClass != null) {
            visitOuter(part, cw, location)
        }

        // ***************************************************************************************** //

        val modifiers = ModifierUtil.modifiersToAsm(part).let {
            if (part is AnnotationDeclaration)
                it + Opcodes.ACC_ANNOTATION
            it
        }

        cw.visit(version,
                modifiers,
                name,
                genericRepresentation,
                superClass.internalName,
                implementations.map { it.internalName }.toTypedArray())

        cw.visitSource(SOURCE_FILE_FUNCTION.getOrSet(data, { "${Util.getOwner(it).simpleName}.cai" })(part), null)
        // ***************************************************************************************** //

        ANNOTATION_VISITOR_CAPABLE.set(data, AnnotationVisitorCapable.ClassVisitorVisitorCapable(cw), true)
        codeProcessor.process(Annotable::class.java, part, data)

        codeProcessor.process(ElementsHolder::class.java, part, data)

        if (part is AnnotationDeclaration) {
            part.properties.forEach {
                codeProcessor.process(AnnotationProperty::class.java, it, data)
            }
        }


        // A check is required here, some problems may occurs if inner classes accesses the enum
        SwitchOnEnum.MAPPINGS.getOrNull(data)?.forEach {
            // Why here and not in SwitchOnEnum?
            // The reason is simple: SwitchOnEnum mappings grows as the code accesses the enum fields.
            // Then only at this point all mappings will be available
            codeProcessor.process(TypeDeclaration::class.java, it.buildClass(), data)
        }

        cw.visitEnd()

        TYPE_DECLARATION.remove(data)
        CLASS_VISITOR.remove(data)
        SwitchOnEnum.MAPPINGS.remove(data)

        BYTECODE_CLASS_LIST.getOrSet(data.mainData, mutableListOf()).add(at, BytecodeClass(part, cw.toByteArray()))
    }

    fun visitInner(innerType: TypeDeclaration, classVisitor: ClassVisitor, outerClassName: String?) {
        val name = if (innerType !is AnonymousClass) innerType.specifiedName else null
        val outerName = if (name != null) outerClassName else null
        val modifiers = ModifierUtil.innerModifiersToAsm(innerType)
        classVisitor.visitInnerClass(innerType.internalName, outerName, name, modifiers)
    }

    fun visitInners(innerTypes: Iterable<TypeDeclaration>, classVisitor: ClassVisitor, outerClassName: String?) {
        innerTypes.forEach {
            visitInner(it, classVisitor, outerClassName)
        }
    }

    fun visitOuter(declaration: TypeDeclaration, classVisitor: ClassVisitor, location: InnerTypesHolder?) {

        val outer = declaration.outerClass

        if (outer != null) {
            val outerClassName = outer.codeType.internalName

            when (location) {
                is FieldDeclaration -> classVisitor.visitOuterClass(outerClassName, null, null)
                is MethodDeclarationBase, is LocalCode -> {
                    val base = (location as? LocalCode)?.declaration ?: location as MethodDeclarationBase
                    val desc = parametersAndReturnToInferredDesc(lazy { declaration }, base, base.parameters, base.returnType)
                    classVisitor.visitOuterClass(outerClassName, base.name, desc)
                }
            }

            visitInners(listOf(declaration), classVisitor, outerClassName)
        }


    }

    override fun endProcess(part: TypeDeclaration, data: TypedData, codeProcessor: CodeProcessor<*>) {

        // Switch on enums is not required
    }

}