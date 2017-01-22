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
package com.github.jonathanxd.codeapi.bytecode.util

import com.github.jonathanxd.codeapi.*
import com.github.jonathanxd.codeapi.base.*
import com.github.jonathanxd.codeapi.builder.ConstructorDeclarationBuilder
import com.github.jonathanxd.codeapi.builder.build
import com.github.jonathanxd.codeapi.common.CodeModifier
import com.github.jonathanxd.codeapi.common.CodeParameter
import com.github.jonathanxd.codeapi.common.TypeSpec
import com.github.jonathanxd.codeapi.factory.constructor
import com.github.jonathanxd.codeapi.factory.field
import com.github.jonathanxd.codeapi.inspect.SourceInspect
import com.github.jonathanxd.codeapi.literal.Literals
import com.github.jonathanxd.codeapi.util.source.CodeArgumentUtil
import com.github.jonathanxd.codeapi.util.source.CodeSourceUtil
import java.util.*

object EnumUtil {

    fun getEnumModifiers(enumDeclaration: EnumDeclaration): Set<CodeModifier> {
        val modifiers = TreeSet(enumDeclaration.modifiers)

        modifiers.add(CodeModifier.ENUM)

        if (enumDeclaration.entries.any { it.body.isNotEmpty }) {
            modifiers.add(CodeModifier.ABSTRACT)
        } else {
            modifiers.add(CodeModifier.FINAL)
        }

        return modifiers
    }

    fun generateEnumClassSource(enumDeclaration: EnumDeclaration): CodeSource {
        val fields = mutableListOf<FieldDeclaration>()
        val entries = enumDeclaration.entries
        val innerClasses = mutableListOf<TypeDeclaration>()

        val codeSource = MutableCodeSource()

        for (i in entries.indices) {
            val enumEntry = entries[i]
            val value: CodePart

            if (enumEntry.body.isNotEmpty) {

                val typeDeclaration = EnumUtil.genEnumInnerClass(enumDeclaration, enumEntry)

                innerClasses.add(typeDeclaration)

                value = EnumUtil.callConstructor(typeDeclaration, enumEntry, i)
            } else {
                value = EnumUtil.callConstructor(enumDeclaration, enumEntry, i)
            }

            fields.add(field(
                    modifiers = EnumSet.of(CodeModifier.PUBLIC, CodeModifier.STATIC, CodeModifier.FINAL, CodeModifier.ENUM),
                    type = enumDeclaration,
                    name = enumEntry.name,
                    value = value

            ))
        }

        codeSource.addAll(fields)

        val valuesFieldName = CodeSourceUtil.getNewFieldName("\$VALUES", codeSource)

        val fieldSize = fields.size

        val arrayType = enumDeclaration.toArray(1)

        val arrayArguments = fields
                .map { fieldDeclaration ->
                    CodeAPI.accessStaticField(fieldDeclaration.type, fieldDeclaration.name)
                }

        val valuesField = field(
                modifiers = EnumSet.of(CodeModifier.PRIVATE, CodeModifier.STATIC, CodeModifier.FINAL, CodeModifier.SYNTHETIC),
                type = arrayType,
                name = valuesFieldName,
                value = CodeAPI.arrayConstruct(arrayType, arrayOf(Literals.INT(fieldSize)), arrayArguments))

        codeSource.add(valuesField)

        val source = enumDeclaration.body.toMutable()

        // Gen methods
        fixConstructor(source)

        codeSource.addAll(source)

        // Enum.values() method.
        codeSource.add(CodeAPI.methodBuilder()
                .withModifiers(CodeModifier.PUBLIC, CodeModifier.STATIC)
                .withName("values")
                .withReturnType(arrayType)
                .withBody(CodeAPI.sourceOfParts(
                        CodeAPI.returnValue(
                                arrayType,
                                CodeAPI.cast(
                                        Types.OBJECT,
                                        arrayType,
                                        CodeAPI.invokeVirtual(
                                                arrayType,
                                                CodeAPI.accessStaticField(valuesField.type, valuesField.name),
                                                "clone",
                                                CodeAPI.typeSpec(Types.OBJECT),
                                                emptyList()
                                        )
                                )
                        )

                ))
                .build())

        // Enum.valueOf(String) method.
        codeSource.add(CodeAPI.methodBuilder()
                .withModifiers(CodeModifier.PUBLIC, CodeModifier.STATIC)
                .withName("valueOf")
                .withParameters(CodeAPI.parameter(Types.STRING, "name"))
                .withReturnType(enumDeclaration)
                .withBody(CodeAPI.sourceOfParts(
                        CodeAPI.returnValue(Types.ENUM, CodeAPI.cast(Types.ENUM, enumDeclaration,
                                CodeAPI.invokeStatic(Types.ENUM, "valueOf", CodeAPI.typeSpec(
                                        Types.ENUM,
                                        Types.CLASS,
                                        Types.STRING),
                                        listOf(
                                                Literals.CLASS(enumDeclaration),
                                                CodeAPI.accessLocalVariable(Types.STRING, "name")
                                        )
                                ))
                        )

                ))
                .build())

        for (innerClass in innerClasses) {
            codeSource.add(innerClass)
        }

        return codeSource
    }

    private fun callConstructor(location: TypeDeclaration, enumEntry: EnumEntry, ordinal: Int): CodePart {
        val constructorSpec = enumEntry.constructorSpec

        val arguments = ArrayList<CodePart>()

        arguments.add(Literals.STRING(enumEntry.name))
        arguments.add(Literals.INT(ordinal))

        var spec = TypeSpec(Types.VOID, listOf(Types.STRING, Types.INT))

        if (constructorSpec != null) {

            val parameterTypes = spec.parameterTypes.toMutableList()

            parameterTypes.addAll(constructorSpec.parameterTypes)

            spec = spec.copy(parameterTypes = parameterTypes)

            arguments.addAll(enumEntry.arguments)
        }

        return CodeAPI.invokeConstructor(location, spec, arguments)
    }

    private fun fixConstructor(originalSource: MutableCodeSource) {
        val inspect = SourceInspect.find { codePart -> codePart is ConstructorDeclaration }
                .include { bodied -> bodied is CodeSource }
                .includeSource(true)
                .mapTo { codePart -> codePart as ConstructorDeclaration }
                .inspect(originalSource)

        if (inspect.isEmpty()) {

            originalSource.add(
                    constructor(
                            EnumSet.of(CodeModifier.PROTECTED, CodeModifier.SYNTHETIC),
                            arrayOf(CodeAPI.parameter(Types.STRING, "name"), CodeAPI.parameter(Types.INT, "ordinal")),
                            CodeSource.fromPart(
                                    CodeAPI.invokeSuperConstructor(CodeAPI.constructorTypeSpec(Types.STRING, Types.INT),
                                            listOf(
                                                    CodeAPI.accessLocalVariable(Types.STRING, "name"),
                                                    CodeAPI.accessLocalVariable(Types.INT, "ordinal")
                                            ))
                            )
                    ))
            // generate
        } else {
            // modify
            for (constructorDeclaration in inspect) {
                val parameters = constructorDeclaration.parameters.toMutableList()

                val name = CodeSourceUtil.getNewName("\$name", parameters)
                val ordinal = CodeSourceUtil.getNewName("\$ordinal", parameters)

                parameters.addAll(0, Arrays.asList(
                        CodeAPI.parameter(Types.STRING, name),
                        CodeAPI.parameter(Types.INT, ordinal)))

                val source = constructorDeclaration.body.toMutable()

                source.add(0, CodeAPI.invokeSuperConstructor(
                        Types.ENUM,
                        TypeSpec(Types.VOID, listOf(Types.STRING, Types.INT)),
                        listOf(CodeAPI.accessLocalVariable(Types.STRING, name),
                                CodeAPI.accessLocalVariable(Types.INT, ordinal))
                ))

                originalSource.remove(constructorDeclaration)


                originalSource.add(ConstructorDeclarationBuilder(constructorDeclaration).build {
                    this.body = source
                    this.parameters = parameters
                })
            }
        }
    }

    private fun genEnumInnerClass(enumDeclaration: EnumDeclaration, enumEntry: EnumEntry): TypeDeclaration {
        val ctrTypeSpec = enumEntry.constructorSpec

        val baseParameters = EnumUtil.getEnumBaseParameters("\$name", "\$ordinal")

        if (ctrTypeSpec != null) {

            val parameterTypes = ctrTypeSpec.parameterTypes

            for (i in parameterTypes.indices) {
                baseParameters.add(CodeAPI.parameter(parameterTypes[i], "$" + i))
            }
        }

        val arguments = CodeArgumentUtil.argumentsFromParameters(baseParameters)

        val constructorSpec = CodeAPI.constructorTypeSpec(
                *baseParameters
                        .map { it.type }
                        .toTypedArray()
        )

        val enumEntryName = CodeSourceUtil.getNewInnerName(enumEntry.name + "\$Inner", enumDeclaration)

        val body = enumEntry.body.toMutable()

        body.add(CodeAPI.constructorBuilder()
                .withParameters(baseParameters)
                .withBody(CodeAPI.sourceOfParts(
                        CodeAPI.invokeSuperConstructor(constructorSpec, arguments)
                ))
                .build())

        return CodeAPI.aClassBuilder()
                .withOuterClass(enumDeclaration)
                .withModifiers(CodeModifier.STATIC, CodeModifier.ENUM)
                .withQualifiedName(enumEntryName)
                .withSuperClass(enumDeclaration)
                .withBody(body)
                .build()
    }

    private fun getEnumBaseParameters(name: String, ordinal: String): MutableList<CodeParameter> {
        return ArrayList(Arrays.asList(
                CodeAPI.parameter(Types.STRING, name),
                CodeAPI.parameter(Types.INT, ordinal)))
    }

}