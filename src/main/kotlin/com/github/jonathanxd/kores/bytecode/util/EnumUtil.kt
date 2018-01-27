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
package com.github.jonathanxd.kores.bytecode.util

import com.github.jonathanxd.kores.Instruction
import com.github.jonathanxd.kores.Instructions
import com.github.jonathanxd.kores.Types
import com.github.jonathanxd.kores.base.*
import com.github.jonathanxd.kores.builder.build
import com.github.jonathanxd.kores.common.getNewInnerName
import com.github.jonathanxd.kores.common.getNewName
import com.github.jonathanxd.kores.factory.*
import com.github.jonathanxd.kores.literal.Literals
import com.github.jonathanxd.kores.type.Generic
import com.github.jonathanxd.kores.util.conversion.access
import java.util.*
import java.util.function.Predicate

object EnumUtil {

    /**
     * Returns whether the [EnumEntry] requires a inner type.
     */
    val EnumEntry.requiresInnerType
        get() = this.staticBlock.body.isNotEmpty
                || this.fields.isNotEmpty()
                || this.methods.isNotEmpty()
                || this.constructorSpec != null
                || this.innerTypes.isNotEmpty()

    fun getEnumModifiers(enumDeclaration: EnumDeclaration): Set<KoresModifier> {
        val modifiers = TreeSet(enumDeclaration.modifiers)

        modifiers.add(KoresModifier.ENUM)

        if (enumDeclaration.entries.any { it.requiresInnerType }) {
            modifiers.add(KoresModifier.ABSTRACT)
        } else {
            modifiers.add(KoresModifier.FINAL)
        }

        return modifiers
    }

    fun generateEnumClass(enumDeclaration: EnumDeclaration): TypeDeclaration {
        val fields = mutableListOf<FieldDeclaration>()
        val entries = enumDeclaration.entries
        val innerTypes = mutableListOf<TypeDeclaration>()

        for (i in entries.indices) {
            val enumEntry = entries[i]
            val value: Instruction

            if (enumEntry.requiresInnerType) {

                val typeDeclaration = EnumUtil.genEnumInnerClass(enumDeclaration, enumEntry)

                innerTypes.add(typeDeclaration)

                value = EnumUtil.callConstructor(typeDeclaration, enumEntry, i)
            } else {
                value = EnumUtil.callConstructor(enumDeclaration, enumEntry, i)
            }

            fields.add(
                FieldDeclaration.Builder.builder()
                    .modifiers(
                        EnumSet.of(
                            KoresModifier.PUBLIC,
                            KoresModifier.STATIC,
                            KoresModifier.FINAL,
                            KoresModifier.ENUM
                        )
                    )
                    .type(enumDeclaration)
                    .name(enumEntry.name)
                    .value(value)
                    .build()
            )
        }

        // Constant enum values field unique name
        val valuesFieldName = getNewName("\$VALUES", fields)

        // Size of arary
        val fieldSize = fields.size

        // Creates array
        val arrayType = enumDeclaration.toArray(1)

        val arrayArguments = fields.map { accessStaticField(type = it.type, name = it.name) }

        val valuesField = FieldDeclaration.Builder.builder()
            .modifiers(
                EnumSet.of(
                    KoresModifier.PRIVATE,
                    KoresModifier.STATIC,
                    KoresModifier.FINAL,
                    KoresModifier.SYNTHETIC
                )
            )
            .type(arrayType)
            .name(valuesFieldName)
            .value(createArray(arrayType, listOf(Literals.INT(fieldSize)), arrayArguments))
            .build()

        fields += valuesField

        return ClassDeclaration.Builder.builder()
            .modifiers(getEnumModifiers(enumDeclaration))
            .qualifiedName(enumDeclaration.qualifiedName)
            .superClass(Generic.type(Types.ENUM).of(enumDeclaration))
            .implementations(enumDeclaration.implementations)
            .constructors(createConstructors(enumDeclaration))
            .fields(fields)
            .innerTypes(innerTypes)
            .methods(
                // Enum.values() method.
                MethodDeclaration.Builder.builder()
                    .modifiers(KoresModifier.PUBLIC, KoresModifier.STATIC)
                    .name("values")
                    .returnType(arrayType)
                    .body(
                        Instructions.fromPart(
                            returnValue(
                                arrayType,
                                cast(
                                    Types.OBJECT,
                                    arrayType,
                                    invokeVirtual(
                                        arrayType,
                                        accessStaticField(
                                            type = valuesField.type,
                                            name = valuesField.name
                                        ),
                                        "clone",
                                        typeSpec(Types.OBJECT),
                                        emptyList()
                                    )
                                )
                            )

                        )
                    )
                    .build(),
                // Enum.valueOf(String) method.
                MethodDeclaration.Builder.builder()
                    .modifiers(KoresModifier.PUBLIC, KoresModifier.STATIC)
                    .name("valueOf")
                    .parameters(parameter(type = Types.STRING, name = "name"))
                    .returnType(enumDeclaration)
                    .body(
                        Instructions.fromPart(
                            returnValue(
                                Types.ENUM, cast(
                                    Types.ENUM, enumDeclaration,
                                    Types.ENUM.invokeStatic(
                                        "valueOf", typeSpec(
                                            Types.ENUM,
                                            Types.CLASS,
                                            Types.STRING
                                        ),
                                        listOf(
                                            Literals.CLASS(enumDeclaration),
                                            accessVariable(Types.STRING, "name")
                                        )
                                    )
                                )
                            )

                        )
                    )
                    .build()
            )
            .build()
    }

    private fun callConstructor(
        location: TypeDeclaration,
        enumEntry: EnumEntry,
        ordinal: Int
    ): Instruction {
        val constructorSpec = enumEntry.constructorSpec

        val arguments = mutableListOf<Instruction>()

        arguments.add(Literals.STRING(enumEntry.name))
        arguments.add(Literals.INT(ordinal))

        var spec = TypeSpec(Types.VOID, listOf(Types.STRING, Types.INT))

        if (constructorSpec != null) {

            val parameterTypes = spec.parameterTypes.toMutableList()

            parameterTypes.addAll(constructorSpec.parameterTypes)

            spec = spec.copy(parameterTypes = parameterTypes)

            arguments.addAll(enumEntry.arguments)
        }

        return location.invokeConstructor(spec, arguments)
    }

    private fun createConstructors(enumDeclaration: EnumDeclaration): List<ConstructorDeclaration> {

        val constructors = enumDeclaration.constructors.toMutableList()

        if (constructors.isEmpty()) {
            constructors.add(
                ConstructorDeclaration.Builder.builder()
                    .modifiers(EnumSet.of(KoresModifier.PROTECTED, KoresModifier.SYNTHETIC))
                    .build()
            )
        }

        return constructors.map {
            val parameters = it.parameters.toMutableList()

            val name = getNewName("\$name", parameters)
            val ordinal = getNewName("\$ordinal", parameters)

            parameters.addAll(
                0, listOf(
                    parameter(type = Types.STRING, name = name),
                    parameter(type = Types.INT, name = ordinal)
                )
            )

            val source = it.body.toMutable()


            // super invocation in enum constructor is invalid
            source.removeIf(Predicate {
                it is MethodInvocation && it.target is Alias.SUPER && it.invokeType.isSpecial()
            })

            source.add(
                0, invokeSuperConstructor(
                    Types.ENUM,
                    TypeSpec(Types.VOID, listOf(Types.STRING, Types.INT)),
                    listOf(
                        accessVariable(Types.STRING, name),
                        accessVariable(Types.INT, ordinal)
                    )
                )
            )

            ConstructorDeclaration.Builder.builder(it).build {
                this.body = source
                this.parameters = parameters
            }
        }

    }

    private fun genEnumInnerClass(
        enumDeclaration: EnumDeclaration,
        enumEntry: EnumEntry
    ): TypeDeclaration {
        val ctrTypeSpec = enumEntry.constructorSpec

        val baseParameters = EnumUtil.getEnumBaseParameters("\$name", "\$ordinal")

        if (ctrTypeSpec != null) {

            val parameterTypes = ctrTypeSpec.parameterTypes

            parameterTypes.indices.mapTo(baseParameters) {
                parameter(
                    type = parameterTypes[it],
                    name = "$" + it
                )
            }
        }

        val arguments = baseParameters.access

        val constructorSpec = constructorTypeSpec(
            *baseParameters
                .map { it.type }
                .toTypedArray()
        )

        val enumEntryName = getNewInnerName(enumEntry.name + "\$Inner", enumDeclaration)

        return ClassDeclaration.Builder.builder()
            .outerType(enumDeclaration)
            .modifiers(KoresModifier.STATIC, KoresModifier.ENUM)
            .qualifiedName(enumEntryName)
            .superClass(enumDeclaration)
            // Inner
            .staticBlock(enumEntry.staticBlock)
            .fields(enumEntry.fields)
            .methods(enumEntry.methods)
            .innerTypes(enumEntry.innerTypes)
            // Generated
            .constructors(
                ConstructorDeclaration.Builder.builder()
                    .modifiers(KoresModifier.PROTECTED)
                    .parameters(baseParameters)
                    .body(
                        Instructions.fromPart(
                            invokeSuperConstructor(constructorSpec, arguments)
                        )
                    )
                    .build()
            )
            .build()
    }

    private fun getEnumBaseParameters(name: String, ordinal: String): MutableList<KoresParameter> {
        return mutableListOf(
            parameter(type = Types.STRING, name = name),
            parameter(type = Types.INT, name = ordinal)
        )
    }

}

