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
import com.github.jonathanxd.kores.base.Alias;
import com.github.jonathanxd.kores.base.ClassDeclaration;
import com.github.jonathanxd.kores.base.ConstructorDeclaration;
import com.github.jonathanxd.kores.base.KoresModifier;
import com.github.jonathanxd.kores.factory.Factories;
import com.github.jonathanxd.kores.factory.InvocationFactory;
import com.github.jonathanxd.kores.literal.Literals;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;

public class InvokeSuperConstructorBugTest {


    @SuppressWarnings("unchecked")
    @Test
    public void invokeSuperConstructorBugTest() {
        ClassDeclaration decl = ClassDeclaration.Builder.builder()
                .modifiers(KoresModifier.PUBLIC)
                .specifiedName("com.InvokeSuperConstructor")
                .superClass(ClassWithConstructor.class)
                .fields(new ArrayList<>())
                .constructors(new ArrayList<>())
                .methods(new ArrayList<>())
                .build();

        decl.getConstructors().add(ConstructorDeclaration.Builder.builder()
                .body(Instructions.fromPart(InvocationFactory.invokeSuperConstructor(
                        Alias.SUPER.INSTANCE,
                        Factories.constructorTypeSpec(String.class),
                        Collections.singletonList(Literals.STRING("Hello"))
                )))
                .build()
        );

        @Named("Instance") Object test = CommonBytecodeTest.test(this.getClass(), decl);
    }

    public static class ClassWithConstructor {
        private final String s;

        public ClassWithConstructor(String s) {
            this.s = s;
        }
    }

}
