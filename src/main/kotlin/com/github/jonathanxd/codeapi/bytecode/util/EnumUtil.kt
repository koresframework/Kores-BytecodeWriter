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
package com.github.jonathanxd.codeapi.bytecode.util

import com.github.jonathanxd.codeapi.CodeAPI
import com.github.jonathanxd.codeapi.CodePart
import com.github.jonathanxd.codeapi.CodeSource
import com.github.jonathanxd.codeapi.MutableCodeSource
import com.github.jonathanxd.codeapi.common.CodeArgument
import com.github.jonathanxd.codeapi.common.CodeModifier
import com.github.jonathanxd.codeapi.common.CodeParameter
import com.github.jonathanxd.codeapi.common.TypeSpec
import com.github.jonathanxd.codeapi.helper.Helper
import com.github.jonathanxd.codeapi.helper.PredefinedTypes
import com.github.jonathanxd.codeapi.inspect.SourceInspect
import com.github.jonathanxd.codeapi.interfaces.*
import com.github.jonathanxd.codeapi.literals.Literals
import com.github.jonathanxd.codeapi.types.CodeType
import com.github.jonathanxd.codeapi.util.source.CodeArgumentUtil
import com.github.jonathanxd.codeapi.util.source.CodeSourceUtil
import java.lang.reflect.Modifier
import java.util.*

object EnumUtil {

    fun getEnumModifiers(enumDeclaration: EnumDeclaration): Collection<CodeModifier> {
        val modifiers = TreeSet(enumDeclaration.modifiers)

        modifiers.add(CodeModifier.ENUM)

        if (enumDeclaration.entries.any { it.hasBody() }) {
            modifiers.add(CodeModifier.ABSTRACT)
        } else {
            modifiers.add(CodeModifier.FINAL)
        }

        return modifiers
    }

    fun generateEnumClassSource(enumDeclaration: EnumDeclaration): CodeSource {
        val fields = ArrayList<FieldDeclaration>()
        val entries = enumDeclaration.entries
        val innerClasses = ArrayList<TypeDeclaration>()

        val codeSource = MutableCodeSource()

        for (i in entries.indices) {
            val enumEntry = entries[i]
            val value: CodePart

            if (enumEntry.hasBody()) {

                val typeDeclaration = EnumUtil.genEnumInnerClass(enumDeclaration, enumEntry)

                innerClasses.add(typeDeclaration)

                value = EnumUtil.callConstructor(typeDeclaration, enumEntry, i)
            } else {
                value = EnumUtil.callConstructor(enumDeclaration, enumEntry, i)
            }

            fields.add(CodeAPI.field(Modifier.PUBLIC or Modifier.STATIC or Modifier.FINAL or CodeModifier.Internal.ENUM, enumDeclaration, enumEntry.name,
                    value))
        }

        codeSource.addAll(fields)

        val valuesFieldName = CodeSourceUtil.getNewFieldName("\$VALUES", codeSource)

        val fieldSize = fields.size

        val arrayType = enumDeclaration.toArray(1)

        val arrayArguments = fields
                .map { fieldDeclaration -> CodeAPI.argument(CodeAPI.accessStaticField(fieldDeclaration.variableType, fieldDeclaration.name), fieldDeclaration.variableType) }
                .toTypedArray()

        val valuesField = CodeAPI.field(Modifier.PRIVATE or Modifier.STATIC or Modifier.FINAL or CodeModifier.Internal.SYNTHETIC, arrayType, valuesFieldName,
                CodeAPI.arrayConstruct(enumDeclaration, arrayOf<CodePart>(Literals.INT(fieldSize)), *arrayArguments))

        codeSource.add(valuesField)

        val source = enumDeclaration.body.map(::MutableCodeSource).orElse(MutableCodeSource())

        // Gen methods
        fixConstructor(source)

        codeSource.addAll(source)

        // Enum.values() method.
        codeSource.add(CodeAPI.methodBuilder()
                .withModifiers(Modifier.PUBLIC or Modifier.STATIC)
                .withName("values")
                .withReturnType(arrayType)
                .withBody(CodeAPI.sourceOfParts(
                        CodeAPI.returnValue(arrayType, CodeAPI.cast(PredefinedTypes.OBJECT, arrayType, CodeAPI.invokeVirtual(arrayType, CodeAPI.accessStaticField(valuesField.variableType, valuesField.name),
                                "clone", CodeAPI.typeSpec(PredefinedTypes.OBJECT))))

                ))
                .build())

        // Enum.valueOf(String) method.
        codeSource.add(CodeAPI.methodBuilder()
                .withModifiers(Modifier.PUBLIC or Modifier.STATIC)
                .withName("valueOf")
                .withParameters(CodeAPI.parameter(PredefinedTypes.STRING, "name"))
                .withReturnType(enumDeclaration)
                .withBody(CodeAPI.sourceOfParts(
                        CodeAPI.returnValue(PredefinedTypes.ENUM, CodeAPI.cast(PredefinedTypes.ENUM, enumDeclaration,
                                CodeAPI.invokeStatic(PredefinedTypes.ENUM, "valueOf", CodeAPI.typeSpec(
                                        PredefinedTypes.ENUM,
                                        PredefinedTypes.CLASS,
                                        PredefinedTypes.STRING),
                                        CodeAPI.argument(Literals.CLASS(enumDeclaration)),
                                        CodeAPI.argument(CodeAPI.accessLocalVariable(PredefinedTypes.STRING, "name")))))

                ))
                .build())

        for (innerClass in innerClasses) {
            codeSource.add(innerClass)
        }

        return codeSource
    }

    private fun callConstructor(location: TypeDeclaration, enumEntry: EnumEntry, ordinal: Int): CodePart {
        val constructorSpecOpt = enumEntry.constructorSpec

        val arguments = ArrayList<CodeArgument>()

        arguments.add(CodeAPI.argument(Literals.STRING(enumEntry.name)))
        arguments.add(CodeAPI.argument(Literals.INT(ordinal)))

        var spec = TypeSpec(PredefinedTypes.VOID, PredefinedTypes.STRING, PredefinedTypes.INT)

        if (constructorSpecOpt.isPresent) {
            val typeSpec = constructorSpecOpt.get()

            val parameterTypes = ArrayList(spec.parameterTypes)

            parameterTypes.addAll(typeSpec.parameterTypes)

            spec = spec.setParameterTypes(parameterTypes)

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
            originalSource.add(CodeAPI.constructor(Modifier.PROTECTED or CodeModifier.Internal.SYNTHETIC, CodeAPI.parameters(CodeAPI.parameter(PredefinedTypes.STRING, "name"),
                    CodeAPI.parameter(PredefinedTypes.INT, "ordinal"))
            ) { _ ->
                CodeAPI.sourceOfParts(
                        CodeAPI.invokeSuperConstructor(CodeAPI.constructorTypeSpec(PredefinedTypes.STRING, PredefinedTypes.INT),
                                CodeAPI.argument(CodeAPI.accessLocalVariable(PredefinedTypes.STRING, "name")),
                                CodeAPI.argument(CodeAPI.accessLocalVariable(PredefinedTypes.INT, "ordinal")))
                )
            })
            // generate
        } else {
            // modify
            for (constructorDeclaration in inspect) {
                val parameters = ArrayList(constructorDeclaration.parameters)

                val name = CodeSourceUtil.getNewName("\$name", parameters)
                val ordinal = CodeSourceUtil.getNewName("\$ordinal", parameters)

                parameters.addAll(0, Arrays.asList(
                        CodeAPI.parameter(PredefinedTypes.STRING, name),
                        CodeAPI.parameter(PredefinedTypes.INT, ordinal)))

                val source = constructorDeclaration.body.map(::MutableCodeSource).orElse(MutableCodeSource())

                source.add(0, Helper.invokeSuperInit(PredefinedTypes.ENUM,
                        CodeAPI.argument(CodeAPI.accessLocalVariable(PredefinedTypes.STRING, name), PredefinedTypes.STRING),
                        CodeAPI.argument(CodeAPI.accessLocalVariable(PredefinedTypes.INT, ordinal), PredefinedTypes.INT)))

                originalSource.remove(constructorDeclaration)

                originalSource.add(constructorDeclaration.setBody(source).setParameters(parameters))
            }
        }
    }

    private fun genEnumInnerClass(enumDeclaration: EnumDeclaration, enumEntry: EnumEntry): TypeDeclaration {
        val typeSpecOpt = enumEntry.constructorSpec

        val baseParameters = EnumUtil.getEnumBaseParameters("\$name", "\$ordinal")
        val arguments: Array<CodeArgument>

        if (typeSpecOpt.isPresent) {
            val typeSpec = typeSpecOpt.get()

            val parameterTypes = typeSpec.parameterTypes

            for (i in parameterTypes.indices) {
                baseParameters.add(CodeAPI.parameter(parameterTypes[i], "$" + i))
            }
        }

        arguments = CodeArgumentUtil.argumentsFromParameters(baseParameters).toTypedArray()
        val constructorSpec = CodeAPI.constructorTypeSpec(
                *baseParameters
                        .map { it.requiredType }
                        .toTypedArray()
        )

        val enumEntryName = CodeSourceUtil.getNewInnerName(enumEntry.name + "\$Inner", enumDeclaration)

        val body = enumEntry.body.orElseThrow(::NullPointerException).toMutable()

        body.add(CodeAPI.constructorBuilder()
                .withParameters(baseParameters)
                .withBody(CodeAPI.sourceOfParts(
                        CodeAPI.invokeSuperConstructor(constructorSpec, *arguments)
                ))
                .build())

        return CodeAPI.aClassBuilder()
                .withOuterClass(enumDeclaration)
                .withModifiers(Modifier.STATIC or CodeModifier.Internal.ENUM)
                .withQualifiedName(enumEntryName)
                .withSuperClass(enumDeclaration)
                .withBody(body)
                .build()
    }

    private fun getEnumBaseParameters(name: String, ordinal: String): MutableList<CodeParameter> {
        return ArrayList(Arrays.asList(
                CodeAPI.parameter(PredefinedTypes.STRING, name),
                CodeAPI.parameter(PredefinedTypes.INT, ordinal)))
    }

}