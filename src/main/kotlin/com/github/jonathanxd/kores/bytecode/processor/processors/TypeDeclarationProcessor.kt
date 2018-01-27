/*
 *      Kores-BytecodeWriter - Translates CodeAPI Structure to JVM Bytecode <https://github.com/JonathanxD/CodeAPI-BytecodeWriter>
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
package com.github.jonathanxd.kores.bytecode.processor.processors

import com.github.jonathanxd.iutils.data.TypedData
import com.github.jonathanxd.iutils.kt.add
import com.github.jonathanxd.kores.base.*
import com.github.jonathanxd.kores.bytecode.processor.*
import com.github.jonathanxd.kores.bytecode.util.AnnotationVisitorCapable
import com.github.jonathanxd.kores.bytecode.util.ModifierUtil
import com.github.jonathanxd.kores.bytecode.util.SwitchOnEnum
import com.github.jonathanxd.kores.bytecode.util.allInnerTypes
import com.github.jonathanxd.kores.common.FieldRef
import com.github.jonathanxd.kores.common.getNewNameBasedOnNameList
import com.github.jonathanxd.kores.factory.accessVariable
import com.github.jonathanxd.kores.factory.parameter
import com.github.jonathanxd.kores.processor.Processor
import com.github.jonathanxd.kores.processor.ProcessorManager
import com.github.jonathanxd.kores.type.GenericType
import com.github.jonathanxd.kores.type.KoresType
import com.github.jonathanxd.kores.type.koresType
import com.github.jonathanxd.kores.util.genericTypesToDescriptor
import com.github.jonathanxd.kores.util.parametersAndReturnToInferredDesc
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes

/**
 * This class requires a strict debugging because `data` is recreated
 */
object TypeDeclarationProcessor : Processor<TypeDeclaration> {

    val baseOuterName = "outer\$"

    override fun process(
        part: TypeDeclaration,
        data: TypedData,
        processorManager: ProcessorManager<*>
    ) {
        val outerType: TypeDeclaration? = TYPE_DECLARATION.getOrNull(data)
        val location: InnerTypesHolder? = LOCATION.getOrNull(data)
        val outerVisitor: ClassVisitor? = CLASS_VISITOR.getOrNull(data)
        val at = BYTECODE_CLASS_LIST.getOrSet(data, mutableListOf()).size
        var tmpPart = part
        METHOD_DECLARATIONS.set(data, mutableListOf())

        if (TYPES.getOrNull(data).orEmpty().contains(part))
            throw IllegalStateException("Revisiting type '$part'. Accidental recursive type visiting")

        TYPES.add(data, part)
        // Data parameter because each type has your own Data.
        @Suppress("NAME_SHADOWING")
        val data = TypedData(data)

        INNER_CLASSES.set(data, part.allInnerTypes().toMutableList())

        outerVisitor?.also {
            // NOTE: Inner transformation
            // - Adds outer class fields with unique names
            // - Adds a constructor with outer parameters
            // or map constructors and add outer parameters with unique names
            // - Or add static modifier if the type does not have constructors
            outerType ?: throw IllegalStateException("Found outer visitor but not found outer type")

            val isStatic = part.modifiers.contains(KoresModifier.STATIC)

            if (!isStatic) {

                val localLocalPart = tmpPart
                if (localLocalPart is ConstructorsHolder && !isStatic && !outerType.isInterface) {

                    val allNames =
                        localLocalPart.fields.map { it.name } + localLocalPart.constructors.flatMap {
                            it.parameters.map { it.name }
                        }

                    val singleName = getNewNameBasedOnNameList(baseOuterName, allNames)

                    val outerField =
                        FieldDeclaration.Builder.builder()
                            .modifiers(
                                KoresModifier.PROTECTED,
                                KoresModifier.FINAL,
                                KoresModifier.SYNTHETIC
                            )
                            .type(outerType)
                            .name(singleName)
                            // ConstructorUtil will add final fields value to constructor after super() or this()
                            // or at the start.
                            // So there is no problem
                            .value(accessVariable(outerType, singleName))
                            .build()

                    val newFields = listOf(outerField) + localLocalPart.fields
                    // Similar code in Util.kt
                    val newCtrs =
                        if (localLocalPart.constructors.isNotEmpty()) {
                            localLocalPart.constructors.map {
                                val newParams = listOf(
                                    parameter(
                                        type = outerType,
                                        name = singleName
                                    )
                                ) + it.parameters

                                it.builder()
                                    .parameters(newParams)
                                    .build()
                            }
                        } else {
                            listOf(
                                ConstructorDeclaration.Builder.builder()
                                    .modifiers(part.modifiers.filter { it.modifierType == ModifierType.VISIBILITY }.toSet())
                                    .parameters(parameter(type = outerType, name = singleName))
                                    .build()
                            )
                        }

                    tmpPart = tmpPart.builder().fields(newFields).build()
                    tmpPart =
                            (tmpPart as ConstructorsHolder).builder().constructors(newCtrs).build()
                                    as TypeDeclaration

                    OUTER_TYPE_FIELD.set(
                        data,
                        OuterClassField(
                            tmpPart,
                            FieldRef(
                                outerField.localization,
                                outerField.target,
                                outerField.type,
                                outerField.name
                            )
                        )
                    )
                } else if ((tmpPart !is ConstructorsHolder && !isStatic) || outerType.isInterface) {
                    tmpPart = tmpPart.builder().modifiers(tmpPart.modifiers + KoresModifier.STATIC)
                        .build()
                }
            }
        }

        val localPart = tmpPart

        val cw = ClassWriter(ClassWriter.COMPUTE_MAXS or ClassWriter.COMPUTE_FRAMES)

        TYPE_DECLARATION.set(data, localPart)
        CLASS_VISITOR.set(data, cw)

        val version = CLASS_VERSION.getOrSet(data, VERSION)

        val name = localPart.internalName
        val implementations: List<KoresType> =
            (localPart as? ImplementationHolder)?.let { it.implementations.map { it.koresType } }
                    ?: emptyList()
        val superClass = ((localPart as? SuperClassHolder)?.superClass ?: Any::class.java).koresType

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

        if (localPart.outerType != null) {
            visitOuter(localPart, cw, location)
        }

        // ***************************************************************************************** //

        val modifiers = ModifierUtil.modifiersToAsm(localPart).let {
            if (localPart is AnnotationDeclaration)
                it + Opcodes.ACC_ANNOTATION
            it
        }

        cw.visit(
            version,
            modifiers,
            name,
            genericRepresentation,
            superClass.internalName,
            implementations.map { it.internalName }.toTypedArray()
        )

        cw.visitSource(SOURCE_FILE_FUNCTION.getOrSet(data, {
            when (it) {
                is TypeDeclaration -> "${Util.getOwner(it).simpleName}.cai"
                is ModuleDeclaration -> "module-info.cai" // Maybe module-info_${it.name}.cai ?
                else -> it.name
            }
        })(localPart), null)
        // ***************************************************************************************** //

        ANNOTATION_VISITOR_CAPABLE.set(
            data,
            AnnotationVisitorCapable.ClassVisitorVisitorCapable(cw),
            true
        )
        processorManager.process(Annotable::class.java, localPart, data)

        processorManager.process(ElementsHolder::class.java, localPart, data)

        if (localPart is AnnotationDeclaration) {
            localPart.properties.forEach {
                processorManager.process(AnnotationProperty::class.java, it, data)
            }
        }

        val accesses = MEMBER_ACCESSES.getOrNull(data.mainData).orEmpty()

        accesses.forEach {
            if (it.owner.`is`(localPart)) {
                val elem = it.newElementToAccess

                if (elem is MethodDeclaration)
                    elem.visitHolder(data, processorManager)
                else if (elem is ConstructorDeclaration)
                    elem.visitHolder(data, processorManager)
                else
                    elem.visitHolder(data, processorManager)
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

        METHOD_DECLARATIONS.remove(data)
        TYPE_DECLARATION.remove(data)
        CLASS_VISITOR.remove(data)
        SwitchOnEnum.MAPPINGS.remove(data)

        BYTECODE_CLASS_LIST.getOrSet(data.mainData, mutableListOf())
            .add(
                at,
                com.github.jonathanxd.kores.bytecode.BytecodeClass(localPart, cw.toByteArray())
            )
        TYPES.getOrNull(data)?.removeAll { it.`is`(localPart) }
        OUTER_TYPE_FIELD.remove(data)
        MEMBER_ACCESSES.getOrNull(data)?.removeAll { it.from.`is`(localPart) }
    }


    fun visitInner(
        innerType: TypeDeclaration,
        classVisitor: ClassVisitor,
        outerClassName: String?
    ) {
        val name = if (innerType !is AnonymousClass) innerType.specifiedName else null
        val outerName = if (name != null) outerClassName else null
        val modifiers = ModifierUtil.innerModifiersToAsm(innerType)
        classVisitor.visitInnerClass(innerType.internalName, outerName, name, modifiers)
    }

    fun visitInners(
        innerTypes: Iterable<TypeDeclaration>,
        classVisitor: ClassVisitor,
        outerClassName: String?
    ) {
        innerTypes.forEach {
            visitInner(it, classVisitor, outerClassName)
        }
    }

    fun visitOuter(
        declaration: TypeDeclaration,
        classVisitor: ClassVisitor,
        location: InnerTypesHolder?
    ) {

        val outer = declaration.outerType

        if (outer != null) {
            val outerClassName = outer.koresType.internalName

            when (location) {
                is FieldDeclaration -> classVisitor.visitOuterClass(outerClassName, null, null)
                is MethodDeclarationBase, is LocalCode -> {
                    val base =
                        (location as? LocalCode)?.declaration ?: location as MethodDeclarationBase
                    val desc = parametersAndReturnToInferredDesc(
                        lazy { declaration },
                        base,
                        base.parameters,
                        base.returnType
                    )
                    classVisitor.visitOuterClass(outerClassName, base.name, desc)
                }
            }

            visitInners(listOf(declaration), classVisitor, outerClassName)
        }


    }

    override fun endProcess(
        part: TypeDeclaration,
        data: TypedData,
        processorManager: ProcessorManager<*>
    ) {
    }

}