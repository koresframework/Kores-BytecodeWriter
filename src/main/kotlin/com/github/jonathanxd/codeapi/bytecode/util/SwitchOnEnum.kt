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

import com.github.jonathanxd.codeapi.CodeSource
import com.github.jonathanxd.codeapi.Types
import com.github.jonathanxd.codeapi.base.*
import com.github.jonathanxd.codeapi.base.comment.Comments
import com.github.jonathanxd.codeapi.bytecode.processor.TYPE_DECLARATION
import com.github.jonathanxd.codeapi.bytecode.processor.add
import com.github.jonathanxd.codeapi.factory.*
import com.github.jonathanxd.codeapi.literal.Literals
import com.github.jonathanxd.codeapi.type.CodeType
import com.github.jonathanxd.codeapi.util.codeType
import com.github.jonathanxd.codeapi.util.typedKeyOf
import com.github.jonathanxd.iutils.data.TypedData

/**
 * Switch on enum helper.
 */
object SwitchOnEnum {

    // SwitchOnEnum.Mapping
    val MAPPINGS = typedKeyOf<MutableList<SwitchOnEnum.Mapping>>("SWITCH_ON_ENUM_MAPPING")

    fun mappings(switchStatement: SwitchStatement, data: TypedData): SwitchStatement {

        val typeDeclaration: TypeDeclaration = TYPE_DECLARATION.getOrNull(data)!!

        val enumType = switchStatement.value.type.codeType

        val name = "${typeDeclaration.canonicalName}\$${enumType.canonicalName.replace(".", "_")}\$SwitchOnEnum\$Mappings"

        val allAsList = MAPPINGS.getOrSet(data, mutableListOf())

        val mapping = allAsList.firstOrNull { it.enumType == enumType }.let {
            return@let if (it == null) {
                val mapping = Mapping(name, enumType)
                MAPPINGS.add(data, mapping)
                mapping
            } else it
        }

        val value = switchStatement.value

        val access = accessArrayValue(
                Types.INT.toArray(1),
                accessStaticField(mapping.declaration, Types.INT.toArray(1), mapping.fieldName),
                invokeVirtual(
                        Types.ENUM,
                        value,
                        "ordinal",
                        typeSpec(Types.INT),
                        listOf()
                ),
                Types.INT
        )


        // Requires update

        return SwitchStatement.Builder.builder()
                .withSwitchType(SwitchType.NUMERIC)
                .withValue(access)
                .withCases(
                        switchStatement.cases.map {
                            return@map if (it.isDefault) {
                                it
                            } else {
                                val index = mapping.getIndex((it.value as EnumValue).name)
                                it.builder().withValue(Literals.INT(index)).build()
                            }
                        }
                )
                .build()
    }

    class Mapping(val name: String, val enumType: CodeType) {
        //
        val declaration = ClassDeclaration.Builder.builder()
                .withModifiers(CodeModifier.PACKAGE_PRIVATE, CodeModifier.SYNTHETIC)
                .withSpecifiedName(name)
                .build()

        val fieldName = "ENUM_MAP"
        private val mappings = mutableListOf<String>()

        fun getIndex(entry: String): Int {
            return mappings.indexOfFirst { it == entry }.let {
                return@let if (it == -1) {
                    mappings.add(entry)
                    mappings.lastIndex + 1
                } else it + 1
            }
        }


        fun buildClass(): TypeDeclaration {
            val typeDeclarationBuilder = declaration.builder()

            val accessValuesLength = arrayLength(enumType.toArray(1), invokeStatic(
                    enumType,
                    "values",
                    typeSpec(enumType.toArray(1)),
                    listOf()
            ))

            val field = FieldDeclaration.Builder.builder()
                    .withModifiers(CodeModifier.PACKAGE_PRIVATE, CodeModifier.STATIC)
                    .withType(Types.INT.toArray(1))
                    .withName(fieldName)
                    .withValue(createArray(
                            Types.INT.toArray(1),
                            listOf(accessValuesLength),
                            listOf()
                    )).build()

            typeDeclarationBuilder.withFields(field)

            val catch = CatchStatement(
                    exceptionTypes = listOf(NoSuchFieldError::class.java.codeType),
                    variable = variable(NoSuchFieldError::class.java.codeType, "ex"),
                    body = CodeSource.empty() // Ignore
            )

            val staticBlock = StaticBlock(Comments.Absent, emptyList(), CodeSource.fromIterable(mappings.mapIndexed { i, _ ->

                tryStatement(
                        CodeSource.fromPart(
                                setArrayValue(
                                        Types.INT.toArray(1),
                                        accessField(field.localization, field.target, field.type, field.name),
                                        invokeVirtual(
                                                Types.ENUM,
                                                accessStaticField(enumType, enumType, name),
                                                "ordinal",
                                                typeSpec(Types.INT),
                                                listOf()
                                        ),
                                        Types.INT,
                                        Literals.INT(i + 1)

                                )
                        ),
                        listOf(catch)
                )

            }))

            typeDeclarationBuilder.withStaticBlock(staticBlock)

            return declaration
        }
    }
}