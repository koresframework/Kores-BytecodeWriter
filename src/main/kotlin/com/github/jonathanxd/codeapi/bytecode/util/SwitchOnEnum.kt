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

import com.github.jonathanxd.codeapi.CodeAPI
import com.github.jonathanxd.codeapi.CodeSource
import com.github.jonathanxd.codeapi.MutableCodeSource
import com.github.jonathanxd.codeapi.Types
import com.github.jonathanxd.codeapi.base.EnumValue
import com.github.jonathanxd.codeapi.base.SwitchStatement
import com.github.jonathanxd.codeapi.base.TypeDeclaration
import com.github.jonathanxd.codeapi.base.impl.CatchStatementImpl
import com.github.jonathanxd.codeapi.base.impl.StaticBlockImpl
import com.github.jonathanxd.codeapi.builder.SwitchStatementBuilder
import com.github.jonathanxd.codeapi.bytecode.gen.visitor.TypeVisitor
import com.github.jonathanxd.codeapi.common.CodeModifier
import com.github.jonathanxd.codeapi.common.Data
import com.github.jonathanxd.codeapi.common.SwitchTypes
import com.github.jonathanxd.codeapi.factory.aClass
import com.github.jonathanxd.codeapi.factory.field
import com.github.jonathanxd.codeapi.factory.variable
import com.github.jonathanxd.codeapi.literal.Literals
import com.github.jonathanxd.codeapi.type.CodeType
import com.github.jonathanxd.codeapi.util.codeType
import java.util.*

object SwitchOnEnum {

    // SwitchOnEnum.Mapping
    val MAPPINGS = "SWITCH_ON_ENUM_MAPPING"

    fun mappings(switchStatement: SwitchStatement, data: Data): SwitchStatement {

        val typeDeclaration: TypeDeclaration = data.getRequired(TypeVisitor.TYPE_DECLARATION_REPRESENTATION)

        val enumType = switchStatement.value.type

        val name = "${typeDeclaration.canonicalName}\$${enumType.canonicalName.replace(".", "_")}\$SwitchOnEnum\$Mappings"

        val allAsList = data.getAllAsList<Mapping>(MAPPINGS)

        val mapping = allAsList.firstOrNull { it.enumType == enumType }.let {
            return@let if (it == null) {
                val mapping = Mapping(name, enumType)
                data.registerData(MAPPINGS, mapping)
                mapping
            } else it
        }

        val value = switchStatement.value

        val access = CodeAPI.getArrayValue(
                Types.INT,
                CodeAPI.accessStaticField(mapping.declaration, Types.INT.toArray(1), mapping.fieldName),
                CodeAPI.invokeVirtual(
                        Types.ENUM,
                        value,
                        "ordinal",
                        CodeAPI.typeSpec(Types.INT),
                        listOf()
                )
        )


        return SwitchStatementBuilder.builder()
                .withSwitchType(SwitchTypes.NUMERIC)
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
        val declaration = aClass(modifiers = EnumSet.of(CodeModifier.PACKAGE_PRIVATE, CodeModifier.SYNTHETIC),
                qualifiedName = name,
                source = MutableCodeSource())

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
            val body = declaration.body as MutableCodeSource

            if (body.isNotEmpty) {
                body.clear()
            }

            val accessValuesLength = CodeAPI.getArrayLength(CodeAPI.invokeStatic(
                    enumType,
                    "values",
                    CodeAPI.typeSpec(enumType.toArray(1)),
                    listOf()
            ))

            val field = field(
                    modifiers = EnumSet.of(CodeModifier.PACKAGE_PRIVATE, CodeModifier.STATIC),
                    type = Types.INT.toArray(1),
                    name = fieldName,
                    value = CodeAPI.arrayConstruct(Types.INT.toArray(1), arrayOf(accessValuesLength))
            )

            body.add(field)

            val source = MutableCodeSource()
            val staticBlock = StaticBlockImpl(source)

            val catch = CatchStatementImpl(
                    exceptionTypes = listOf(NoSuchFieldError::class.java.codeType),
                    variable = variable(NoSuchFieldError::class.java.codeType, "ex"),
                    body = CodeSource.empty() // Ignore
            )

            mappings.forEachIndexed { i, name ->
                source.add(
                        CodeAPI.tryStatement(
                                CodeAPI.sourceOfParts(
                                        CodeAPI.setArrayValue(
                                                CodeAPI.accessField(field),
                                                CodeAPI.invokeVirtual(
                                                        Types.ENUM,
                                                        CodeAPI.accessStaticField(enumType, enumType, name),
                                                        "ordinal",
                                                        CodeAPI.typeSpec(Types.INT),
                                                        listOf()
                                                ),
                                                Literals.INT(i + 1)

                                        )
                                ),
                                catch
                        )
                )
            }

            body.add(staticBlock)

            return declaration
        }
    }
}