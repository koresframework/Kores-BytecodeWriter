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
import com.github.jonathanxd.codeapi.bytecode.BytecodeClass
import com.github.jonathanxd.codeapi.bytecode.processor.*
import com.github.jonathanxd.codeapi.bytecode.util.AnnotationVisitorCapable
import com.github.jonathanxd.codeapi.bytecode.util.ModifierUtil
import com.github.jonathanxd.codeapi.bytecode.util.SwitchOnEnum
import com.github.jonathanxd.codeapi.common.FieldRef
import com.github.jonathanxd.codeapi.common.getNewNames
import com.github.jonathanxd.codeapi.common.getNewNamesBaseOnNameList
import com.github.jonathanxd.codeapi.factory.accessVariable
import com.github.jonathanxd.codeapi.factory.parameter
import com.github.jonathanxd.codeapi.processor.Processor
import com.github.jonathanxd.codeapi.processor.ProcessorManager
import com.github.jonathanxd.codeapi.type.CodeType
import com.github.jonathanxd.codeapi.type.GenericType
import com.github.jonathanxd.codeapi.util.add
import com.github.jonathanxd.codeapi.util.codeType
import com.github.jonathanxd.codeapi.util.genericTypesToDescriptor
import com.github.jonathanxd.codeapi.util.parametersAndReturnToInferredDesc
import com.github.jonathanxd.iutils.data.TypedData
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import javax.lang.model.element.Modifier

/**
 * This class requires a strict debugging because `data` is recreated
 */
object TypeDeclarationProcessor : Processor<TypeDeclaration> {

    val baseOuterName = "outer\$"

    override fun process(part: TypeDeclaration, data: TypedData, processorManager: ProcessorManager<*>) {
        val outerType: TypeDeclaration? = TYPE_DECLARATION.getOrNull(data)
        val location: InnerTypesHolder? = LOCATION.getOrNull(data)
        val outerVisitor: ClassVisitor? = CLASS_VISITOR.getOrNull(data)
        val at = BYTECODE_CLASS_LIST.getOrSet(data, mutableListOf()).size
        var tmpPart = part

        if (TYPES.getOrNull(data).orEmpty().contains(part))
            throw IllegalStateException("Revisiting type '$part'. Accidental recursive type visiting")

        TYPES.add(data, part)
        // Data parameter because each type has your own Data.
        @Suppress("NAME_SHADOWING")
        val data = TypedData(data)

        outerVisitor?.also {
            // NOTE: Inner transformation
            // - Adds outer class fields with unique names
            // - Adds a constructor with outer parameters
            // or map constructors and add outer parameters with unique names
            // - Or add static modifier if the type does not have constructors
            outerType ?: throw IllegalStateException("Found outer visitor but not found outer type")

            val isStatic = part.modifiers.contains(CodeModifier.STATIC)

            if (!isStatic) {
                val get = getTypes(part, data)

                if (!isStatic && get.isEmpty() && !outerType.modifiers.contains(CodeModifier.STATIC))
                    throw IllegalStateException("Outer types are not registered in TYPES")

                val localLocalPart = tmpPart
                if (localLocalPart is ConstructorsHolder && !isStatic && !outerType.isInterface) {

                    val allNames = localLocalPart.fields.map { it.name } + localLocalPart.constructors.flatMap {
                        it.parameters.map { it.name }
                    }

                    val names = getNewNamesBaseOnNameList(baseOuterName, get.size, allNames)

                    val outerFields = names.mapIndexed { i, it ->
                        FieldDeclaration.Builder.builder()
                                .modifiers(CodeModifier.PRIVATE, CodeModifier.FINAL)
                                .type(get[i])
                                .name(it)
                                // ConstructorUtil will add final fields value to constructor after super() or this()
                                // or at the start.
                                // So there is no problem
                                .value(accessVariable(get[i], names[i]))
                                .build()
                    }

                    val newFields = outerFields + localLocalPart.fields

                    val newCtrs =
                            if (localLocalPart.constructors.isNotEmpty()) {
                                localLocalPart.constructors.map {
                                    val newParams = get.indices.map {
                                        parameter(type = get[it], name = names[it])
                                    } + it.parameters


                                    it.builder()
                                            .parameters(newParams)
                                            .build()
                                }
                            } else {
                                listOf(ConstructorDeclaration.Builder.builder()
                                        .parameters(get.indices.map {
                                            parameter(type = get[it], name = names[it])
                                        })
                                        .build())
                            }

                    tmpPart = tmpPart.builder().fields(newFields).build()
                    tmpPart = (tmpPart as ConstructorsHolder).builder().constructors(newCtrs).build()
                            as TypeDeclaration

                    OUTER_TYPES_FIELDS.add(data, OuterClassFields(tmpPart, outerFields.map {
                        FieldRef(it.localization, it.target, it.type, it.name)
                    }))
                } else if ((tmpPart !is ConstructorsHolder && !isStatic) || outerType.isInterface) {
                    tmpPart = tmpPart.builder().modifiers(tmpPart.modifiers + CodeModifier.STATIC).build()
                }
            }
        }

        val localPart = tmpPart

        val cw = ClassWriter(ClassWriter.COMPUTE_MAXS or ClassWriter.COMPUTE_FRAMES)

        TYPE_DECLARATION.set(data, localPart)
        CLASS_VISITOR.set(data, cw)

        val version = CLASS_VERSION.getOrSet(data, VERSION)

        val name = localPart.internalName
        val implementations: List<CodeType> = (localPart as? ImplementationHolder)?.let { it.implementations.map { it.codeType } } ?: emptyList()
        val superClass = ((localPart as? SuperClassHolder)?.superClass ?: Any::class.java).codeType

        val genericRepresentation = genericTypesToDescriptor(localPart, superClass,
                implementations,
                superClass is GenericType,
                implementations.any { it is GenericType })

        // ***************************************************************************************** //

        outerVisitor?.let {
            outerType ?: throw IllegalStateException("Found outer visitor but not found outer type")
            visitInner(localPart, cw, outerType.internalName)
            visitInner(localPart, it, outerType.internalName)
        }

        if (localPart.outerClass != null) {
            visitOuter(localPart, cw, location)
        }

        // ***************************************************************************************** //

        val modifiers = ModifierUtil.modifiersToAsm(localPart).let {
            if (localPart is AnnotationDeclaration)
                it + Opcodes.ACC_ANNOTATION
            it
        }

        cw.visit(version,
                modifiers,
                name,
                genericRepresentation,
                superClass.internalName,
                implementations.map { it.internalName }.toTypedArray())

        cw.visitSource(SOURCE_FILE_FUNCTION.getOrSet(data, { "${Util.getOwner(it).simpleName}.cai" })(localPart), null)
        // ***************************************************************************************** //

        ANNOTATION_VISITOR_CAPABLE.set(data, AnnotationVisitorCapable.ClassVisitorVisitorCapable(cw), true)
        processorManager.process(Annotable::class.java, localPart, data)

        processorManager.process(ElementsHolder::class.java, localPart, data)

        if (localPart is AnnotationDeclaration) {
            localPart.properties.forEach {
                processorManager.process(AnnotationProperty::class.java, it, data)
            }
        }


        // A check is required here, some problems may occurs if inner classes accesses the enum
        SwitchOnEnum.MAPPINGS.getOrNull(data)?.forEach {
            // Why here and not in SwitchOnEnum?
            // The reason is simple: SwitchOnEnum mappings grows as the code accesses the enum fields.
            // Then only at this point all mappings will be available
            processorManager.process(TypeDeclaration::class.java, it.buildClass(), data)
        }

        cw.visitEnd()

        TYPE_DECLARATION.remove(data)
        CLASS_VISITOR.remove(data)
        SwitchOnEnum.MAPPINGS.remove(data)

        BYTECODE_CLASS_LIST.getOrSet(data.mainData, mutableListOf()).add(at, BytecodeClass(localPart, cw.toByteArray()))
        TYPES.getOrNull(data)?.removeAll { it.`is`(localPart) }
        OUTER_TYPES_FIELDS.getOrNull(data)?.removeAll { it.typeDeclaration.`is`(localPart) }
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

    override fun endProcess(part: TypeDeclaration, data: TypedData, processorManager: ProcessorManager<*>) {
    }

}