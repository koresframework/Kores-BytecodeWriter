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
package com.github.jonathanxd.kores.test.asm;

import com.github.jonathanxd.iutils.annotation.Named;
import com.github.jonathanxd.iutils.collection.Collections3;
import com.github.jonathanxd.kores.Instructions;
import com.github.jonathanxd.kores.Types;
import com.github.jonathanxd.kores.base.Access;
import com.github.jonathanxd.kores.base.Alias;
import com.github.jonathanxd.kores.base.ClassDeclaration;
import com.github.jonathanxd.kores.base.ConstructorDeclaration;
import com.github.jonathanxd.kores.base.FieldDeclaration;
import com.github.jonathanxd.kores.base.KoresModifier;
import com.github.jonathanxd.kores.base.MethodDeclaration;
import com.github.jonathanxd.kores.base.TypeDeclaration;
import com.github.jonathanxd.kores.common.Commons;
import com.github.jonathanxd.kores.factory.Factories;
import com.github.jonathanxd.kores.factory.InvocationFactory;
import com.github.jonathanxd.kores.generic.GenericSignature;
import com.github.jonathanxd.kores.helper.ConcatHelper;
import com.github.jonathanxd.kores.literal.Literals;
import com.github.jonathanxd.kores.type.Generic;
import com.github.jonathanxd.kores.type.TypeRef;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.function.UnaryOperator;

import kotlin.collections.SetsKt;

public class FireEnumTest {

    public static void main(String[] args) {
        new FireEnumTest().test();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test() {
        TypeRef eType = new TypeRef("Option");
        TypeRef someType = new TypeRef(eType, "Some");
        TypeRef noneType = new TypeRef(eType, "None");
        Generic someOfT = Generic.type(someType).of("T");
        Generic typeT = Generic.type("T");

        TypeDeclaration $ = ClassDeclaration.Builder.builder()
                .modifiers(SetsKt.setOf(KoresModifier.PUBLIC, KoresModifier.ENUM))
                .qualifiedName("Option")
                .genericSignature(GenericSignature.create(typeT))
                .superClass(Generic.type(Enum.class).of(eType))
                .innerTypes(
                        ClassDeclaration.Builder.builder()
                                .outerType(eType)
                                .modifiers(SetsKt.setOf(KoresModifier.PUBLIC, KoresModifier.STATIC))
                                .qualifiedName("Some")
                                .genericSignature(GenericSignature.create(typeT))
                                .superClass(Generic.type(eType).of("T"))
                                .fields(FieldDeclaration.Builder.builder()
                                        .modifiers(KoresModifier.PRIVATE, KoresModifier.FINAL)
                                        .type(typeT)
                                        .name("value")
                                        .build())
                                .constructors(ConstructorDeclaration.Builder.builder()
                                        .modifiers(KoresModifier.PACKAGE_PRIVATE)
                                        .parameters(Factories.parameter(typeT, "value"))
                                        .body(Instructions.fromVarArgs(
                                                InvocationFactory.invokeSuperConstructor(
                                                        Factories.constructorTypeSpec(Types.STRING,
                                                                Types.INT),
                                                        Collections3.listOf(
                                                                Literals.STRING("Some"),
                                                                Literals.INT(0)
                                                        )
                                                ),
                                                Factories.setFieldValue(
                                                        Alias.THIS.INSTANCE,
                                                        Access.THIS,
                                                        typeT,
                                                        "value",
                                                        Factories.accessVariable(typeT, "value")
                                                )))
                                        .build()
                                )
                                .methods(MethodDeclaration.Builder.builder()
                                                .modifiers(KoresModifier.PUBLIC)
                                                .returnType(typeT)
                                                .name("getValue")
                                                .body(Instructions.fromPart(
                                                        Factories.returnValue(typeT, Factories.accessField(
                                                                Alias.THIS.INSTANCE,
                                                                Access.THIS,
                                                                typeT,
                                                                "value"
                                                        ))))
                                                .build(),
                                        MethodDeclaration.Builder.builder()
                                                .modifiers(KoresModifier.PUBLIC)
                                                .returnType(Types.STRING)
                                                .name("toString")
                                                .body(Instructions.fromPart(
                                                        Factories.returnValue(Types.STRING,
                                                                ConcatHelper.builder("Some")
                                                                        .concat("(")
                                                                        .concat(Commons.invokeObjectsToString(
                                                                                Factories.accessField(
                                                                                        Alias.THIS.INSTANCE,
                                                                                        Access.THIS,
                                                                                        typeT,
                                                                                        "value"
                                                                                )))
                                                                        .concat(")")
                                                                        .build()
                                                        )
                                                ))
                                                .build()

                                )
                                .build(),
                        ClassDeclaration.Builder.builder()
                                .outerType(eType)
                                .modifiers(SetsKt.setOf(KoresModifier.PUBLIC, KoresModifier.STATIC))
                                .qualifiedName("None")
                                .superClass(eType)
                                .constructors(ConstructorDeclaration.Builder.builder()
                                        .modifiers(KoresModifier.PACKAGE_PRIVATE)
                                        .body(Instructions.fromVarArgs(
                                                InvocationFactory.invokeSuperConstructor(
                                                        Factories.constructorTypeSpec(Types.STRING,
                                                                Types.INT),
                                                        Collections3.listOf(
                                                                Literals.STRING("None"),
                                                                Literals.INT(1)
                                                        )
                                                )
                                        ))
                                        .build())
                                .build()

                )
                .fields(
                        FieldDeclaration.Builder.builder()
                                .modifiers(KoresModifier.PUBLIC, KoresModifier.STATIC,
                                        KoresModifier.FINAL, KoresModifier.ENUM)
                                .type(noneType)
                                .name("None")
                                .value(InvocationFactory.invokeConstructor(noneType))
                                .build(),
                        FieldDeclaration.Builder.builder()
                                .modifiers(KoresModifier.PUBLIC, KoresModifier.STATIC,
                                        KoresModifier.FINAL, KoresModifier.ENUM)
                                .type(Generic.type(someType).of(noneType))
                                .name("Some")
                                .value(InvocationFactory.invokeConstructor(someType,
                                        Factories.constructorTypeSpec(typeT),
                                        Collections.singletonList(
                                                Factories.accessStaticField(noneType, "None")
                                        ))
                                )
                                .build(),
                        FieldDeclaration.Builder.builder()
                                .modifiers(KoresModifier.PRIVATE, KoresModifier.STATIC,
                                        KoresModifier.FINAL, KoresModifier.SYNTHETIC)
                                .type(eType.toArray(1))
                                .name("$VALUES")
                                .value(Factories.createArray(
                                        eType.toArray(1),
                                        Collections3.listOf(Literals.INT(2)),
                                        Collections3.listOf(
                                                Factories.accessStaticField(someType, "Some"),
                                                Factories.accessStaticField(noneType, "None")
                                        )
                                ))
                                .build()
                )
                .constructors(ConstructorDeclaration.Builder.builder()
                        .modifiers(KoresModifier.PACKAGE_PRIVATE)
                        .parameters(Factories.parameter(Types.STRING, "name"),
                                Factories.parameter(Types.INT, "ordinal"))
                        .body(Instructions.fromPart(
                                InvocationFactory.invokeSuperConstructor(
                                        Factories.constructorTypeSpec(Types.STRING, Types.INT),
                                        Collections3.listOf(
                                                Factories.accessVariable(Types.STRING,
                                                        "name"),
                                                Factories.accessVariable(Types.INT,
                                                        "ordinal")
                                        )
                                )
                        ))
                        .build()
                )
                .methods(MethodDeclaration.Builder.builder()
                                .modifiers(KoresModifier.PUBLIC, KoresModifier.STATIC)
                                .genericSignature(GenericSignature.create(typeT))
                                .returnType(someOfT)
                                .name("some")
                                .parameters(Factories.parameter(typeT, "value"))
                                .body(Instructions.fromPart(
                                        Factories.returnValue(someOfT,
                                                InvocationFactory.invokeConstructor(someType,
                                                        Factories.constructorTypeSpec(typeT),
                                                        Collections.singletonList(
                                                                Factories.accessVariable(typeT, "value")
                                                        ))
                                        )
                                ))
                                .build(),
                        MethodDeclaration.Builder.builder()
                                .modifiers(KoresModifier.PUBLIC, KoresModifier.STATIC)
                                .returnType(noneType)
                                .name("none")
                                .body(Instructions.fromPart(
                                        Factories.returnValue(noneType,
                                                Factories.accessStaticField(noneType, "None")
                                        )
                                ))
                                .build(),
                        MethodDeclaration.Builder.builder()
                                .modifiers(KoresModifier.PUBLIC, KoresModifier.STATIC)
                                .returnType(eType.toArray(1))
                                .name("values")
                                .parameters()
                                .body(Instructions.fromPart(
                                        Factories.returnValue(eType.toArray(1),
                                                InvocationFactory.invokeVirtual(
                                                        eType.toArray(1),
                                                        Factories.accessStaticField(
                                                                eType.toArray(1),
                                                                "$VALUES"
                                                        ),
                                                        "clone",
                                                        Factories.typeSpec(Types.OBJECT),
                                                        Collections.emptyList()
                                                )
                                        )
                                ))
                                .build(),
                        MethodDeclaration.Builder.builder()
                                .modifiers(KoresModifier.PUBLIC, KoresModifier.STATIC)
                                .returnType(eType)
                                .name("valueOf")
                                .parameters(Factories.parameter(Types.STRING, "name"))
                                .body(Instructions.fromPart(
                                        Factories.returnValue(
                                                eType,
                                                InvocationFactory.invokeStatic(Types.ENUM,
                                                        "valueOf", Factories.typeSpec(
                                                                Types.ENUM,
                                                                Types.CLASS,
                                                                Types.STRING
                                                        ),
                                                        Collections3.listOf(
                                                                Literals.CLASS(eType),
                                                                Factories.accessVariable(
                                                                        Types.STRING,
                                                                        "name"
                                                                )
                                                        )
                                                )
                                        )

                                ))
                                .build()
                )
                .build();

        @Named("Instance") Class<Enum> test = (Class<Enum>) CommonBytecodeTest.test(this.getClass(),
                $, UnaryOperator.identity(), aClass -> aClass);

        Enum some = Enum.valueOf(test, "Some");
        Enum none = Enum.valueOf(test, "None");

        Assert.assertEquals(0, some.ordinal());
        Assert.assertEquals("Some", some.name());
        Assert.assertEquals("Some(None)", some.toString());
        Assert.assertEquals(1, none.ordinal());
        Assert.assertEquals("None", none.name());
        Assert.assertEquals("None", none.toString());

    }

}

