/*
 *      Kores-BytecodeWriter - Translates Kores Structure to JVM Bytecode <https://github.com/JonathanxD/CodeAPI-BytecodeWriter>
 *
 *         The MIT License (MIT)
 *
 *      Copyright (c) 2021 TheRealBuggy/JonathanxD (https://github.com/JonathanxD/) <jonathan.scripter@programmer.net>
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
package com.koresframework.kores.bytecode.util

import com.koresframework.kores.Instructions
import com.koresframework.kores.Types
import com.koresframework.kores.base.*
import com.koresframework.kores.base.comment.Comments
import com.koresframework.kores.bytecode.processor.TYPE_DECLARATION
import com.koresframework.kores.factory.*
import com.koresframework.kores.literal.Literals
import com.koresframework.kores.safeForComparison
import com.koresframework.kores.type
import com.koresframework.kores.type.KoresType
import com.koresframework.kores.type.koresType
import com.github.jonathanxd.iutils.data.TypedData
import com.github.jonathanxd.iutils.kt.add
import com.github.jonathanxd.iutils.kt.typedKeyOf

/**
 * Switch on enum helper.
 */
object SwitchOnEnum {

    // SwitchOnEnum.Mapping
    val MAPPINGS = typedKeyOf<MutableList<SwitchOnEnum.Mapping>>("SWITCH_ON_ENUM_MAPPING")

    fun mappings(switchStatement: SwitchStatement, data: TypedData): SwitchStatement {

        val typeDeclaration: TypeDeclaration = TYPE_DECLARATION.getOrNull(data)!!

        val enumType = switchStatement.value.type.koresType

        val name = "${typeDeclaration.canonicalName}\$${enumType.canonicalName.replace(
            ".",
            "_"
        )}\$SwitchOnEnum\$Mappings"

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
            .switchType(SwitchType.NUMERIC)
            .value(access)
            .cases(
                switchStatement.cases.map {
                    return@map if (it.isDefault) {
                        it
                    } else {
                        val index = mapping.getIndex((it.value.safeForComparison as EnumValue).name)
                        it.builder().value(Literals.INT(index)).build()
                    }
                }
            )
            .build()
    }

    class Mapping(val name: String, val enumType: KoresType) {
        //
        val declaration = ClassDeclaration.Builder.builder()
            .modifiers(KoresModifier.PACKAGE_PRIVATE, KoresModifier.SYNTHETIC)
            .specifiedName(name)
            .build()

        val fieldName = "ENUM_MAP"
        private val mappings = mutableListOf<String>()
        private var evaluated: Boolean = false

        fun getIndex(entry: String): Int {
            return mappings.indexOfFirst { it == entry }.let {
                return@let if (it == -1) {
                    mappings.add(entry)
                    mappings.lastIndex + 1
                } else it + 1
            }
        }


        fun buildClass(): TypeDeclaration {

            if (evaluated)
                throw IllegalStateException("SwitchOnEnum mapping already builded")

            evaluated = true
            val typeDeclarationBuilder = declaration.builder()

            val accessValuesLength = arrayLength(
                enumType.toArray(1), enumType.invokeStatic(
                    "values",
                    typeSpec(enumType.toArray(1)),
                    listOf()
                )
            )

            val field = FieldDeclaration.Builder.builder()
                .modifiers(KoresModifier.PACKAGE_PRIVATE, KoresModifier.STATIC)
                .type(Types.INT.toArray(1))
                .name(fieldName)
                .value(
                    createArray(
                        Types.INT.toArray(1),
                        listOf(accessValuesLength),
                        listOf()
                    )
                ).build()

            typeDeclarationBuilder.fields(field)

            val catch = CatchStatement(
                exceptionTypes = listOf(NoSuchFieldError::class.java.koresType),
                variable = variable(NoSuchFieldError::class.java.koresType, "ex"),
                body = Instructions.empty() // Ignore
            )

            val staticBlock = StaticBlock(
                Comments.Absent,
                emptyList(),
                Instructions.fromIterable(mappings.mapIndexed { i, enumName ->

                    tryStatement(
                        Instructions.fromPart(
                            setArrayValue(
                                Types.INT.toArray(1),
                                accessField(
                                    field.localization,
                                    field.target,
                                    field.type,
                                    field.name
                                ),
                                invokeVirtual(
                                    Types.ENUM,
                                    accessStaticField(enumType, enumType, enumName),
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

                })
            )

            typeDeclarationBuilder.staticBlock(staticBlock)

            return typeDeclarationBuilder.build()
        }
    }
}