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

import com.github.jonathanxd.iutils.exception.RethrowException;
import com.github.jonathanxd.kores.Types;
import com.github.jonathanxd.kores.base.Access;
import com.github.jonathanxd.kores.base.ClassDeclaration;
import com.github.jonathanxd.kores.literal.Literals;

import org.junit.Test;

import java.util.function.UnaryOperator;

import static com.github.jonathanxd.kores.base.KoresModifier.FINAL;
import static com.github.jonathanxd.kores.base.KoresModifier.PRIVATE;
import static com.github.jonathanxd.kores.base.KoresModifier.PUBLIC;
import static com.github.jonathanxd.kores.factory.Factories.constructorTypeSpec;
import static com.github.jonathanxd.kores.factory.InvocationFactory.invokeConstructor;
import static com.github.jonathanxd.kores.factory.InvocationFactory.invokeSuperConstructor;
import static com.github.jonathanxd.kores.factory.PartFactory.classDec;
import static com.github.jonathanxd.kores.factory.PartFactory.constructorDec;
import static com.github.jonathanxd.kores.factory.PartFactory.fieldDec;
import static com.github.jonathanxd.kores.factory.PartFactory.source;
import static kotlin.collections.CollectionsKt.listOf;

public class FinalFieldWithThis {

    @Test
    public void finalFieldWithThis() {
        ClassDeclaration testField = classDec().modifiers(PUBLIC).name("finalfieldwiththis.Test").superClass(TestBox.class)
                .fields(
                        fieldDec().modifiers(PRIVATE, FINAL).type(TestBox.class).name("testField")
                                .value(invokeConstructor(TestBox.class, constructorTypeSpec(Types.OBJECT), listOf(Access.THIS)))
                                .build()
                )
                .constructors(
                        constructorDec().modifiers(PUBLIC).body(source(
                                invokeSuperConstructor(TestBox.class, constructorTypeSpec(Types.OBJECT), listOf(Literals.NULL))
                        )).build()
                )
                .build();

        CommonBytecodeTest.test(this.getClass(), testField, UnaryOperator.identity(), aClass -> {
            try {
                return aClass.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RethrowException(e, e.getCause());
            }
        });
    }

    public static class TestBox {
        private final Object o;

        public TestBox(Object o) {
            this.o = o;
        }
    }
}
