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
package com.github.jonathanxd.kores.test.asm;

import com.github.jonathanxd.iutils.annotation.Named;
import com.github.jonathanxd.kores.Instructions;
import com.github.jonathanxd.kores.base.Access;
import com.github.jonathanxd.kores.base.ClassDeclaration;
import com.github.jonathanxd.kores.base.KoresModifier;
import com.github.jonathanxd.kores.base.KoresParameter;
import com.github.jonathanxd.kores.base.TypeDeclaration;
import com.github.jonathanxd.kores.bytecode.BytecodeOptions;
import com.github.jonathanxd.kores.factory.Factories;
import com.github.jonathanxd.kores.factory.PartFactory;
import com.github.jonathanxd.kores.literal.Literals;
import com.github.jonathanxd.kores.type.Generic;
import com.github.jonathanxd.kores.type.GenericType;

import org.junit.Test;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.UnaryOperator;

public class RecursiveGenericTypeTest {


    @SuppressWarnings("unchecked")
    @Test
    public void selfPrivateAccessBugTest() throws Throwable {
        ClassDeclaration decl = ClassDeclaration.Builder.builder()
                .modifiers(KoresModifier.PUBLIC)
                .specifiedName("com.MyClass")
                .fields(new ArrayList<>())
                .constructors(new ArrayList<>())
                .methods(new ArrayList<>())
                .build();

        decl.getFields().add(PartFactory.fieldDec()
                .modifiers(KoresModifier.PRIVATE, KoresModifier.STATIC, KoresModifier.FINAL)
                .type(String.class)
                .name("f1")
                .value(Literals.STRING("Test"))
                .build());

        GenericType recursive =
                Generic.type("M").extends$(Generic.type(Map.class).of(Generic.type("E"), Generic.type("M")));

        decl.getFields().add(PartFactory.fieldDec()
                .modifiers(KoresModifier.PRIVATE, KoresModifier.STATIC, KoresModifier.FINAL)
                .type(recursive.getBounds()[0].getType())
                .name("f2")
                .value(Literals.NULL)
                .build());

        decl.getConstructors().add(PartFactory.constructorDec()
                .modifiers(KoresModifier.PUBLIC)
                .parameters(
                        Factories.parameter(Type.class, "type"),
                        Factories.parameter(Generic.type("E"), "x"),
                        Factories.parameter(recursive, "test")
                )
                .name("get")
                .body(Instructions.fromPart(
                        Factories.accessStaticField(decl, String.class, "f1")
                ))
                .build());

        @Named("Instance") Object test = CommonBytecodeTest.test(this.getClass(), decl, UnaryOperator.identity(), aClass ->
                aClass.getConstructor(Type.class, Object.class, Map.class).newInstance(new Object[]{null, null, null}), bytecodeGenerator -> bytecodeGenerator.getOptions().set(BytecodeOptions.CHECK, true));
    }
}
