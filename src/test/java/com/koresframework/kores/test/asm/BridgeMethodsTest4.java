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
package com.koresframework.kores.test.asm;

import com.koresframework.kores.Instructions;
import com.koresframework.kores.base.ClassDeclaration;
import com.koresframework.kores.base.KoresModifier;
import com.koresframework.kores.base.MethodDeclaration;
import com.koresframework.kores.base.TypeDeclaration;
import com.koresframework.kores.bytecode.BytecodeOptions;
import com.koresframework.kores.factory.Factories;
import com.koresframework.kores.generic.GenericSignature;
import com.koresframework.kores.helper.Predefined;
import com.koresframework.kores.literal.Literals;
import com.koresframework.kores.type.Generic;
import org.junit.Assert;
import org.junit.Test;

import java.util.function.UnaryOperator;

public class BridgeMethodsTest4 {

    @Test
    public void bridgeMethodTest4() {

        TypeDeclaration typeDeclaration = ClassDeclaration.Builder.builder()
                .modifiers(KoresModifier.PUBLIC)
                .qualifiedName("com.BridgeMethodTest4")
                .genericSignature(GenericSignature.create(Generic.type("V").extends$(Generic.type(MyEvent2.class).of("V"))))
                .implementations(MyEvent2.class)
                .methods(MethodDeclaration.Builder.builder()
                        .modifiers(KoresModifier.PUBLIC)
                        .returnType(Generic.type("V").extends$(Event.class))
                        .parameters()
                        .name("self")
                        .body(Instructions.fromPart(
                                Factories.returnValue(Generic.type("V").extends$(Event.class),
                                        Literals.NULL
                                )
                        ))
                        .build()
                )
                .build();

        Object o = CommonBytecodeTest.test(this.getClass(), typeDeclaration, UnaryOperator.identity(), Class::newInstance,
                bytecodeGenerator -> {
                    bytecodeGenerator.getOptions().set(BytecodeOptions.GENERATE_BRIDGE_METHODS, true);
                });

        MyEvent2<?> s = (MyEvent2<?>) o;

        Assert.assertNull(s.self());
    }

    public interface Event {}

    public interface MyEvent<V extends MyEvent<V>> extends Event {
        V self();
    }

    public interface MyEvent2<V extends MyEvent2<V>> extends MyEvent<V> {
        V self();
    }

}
