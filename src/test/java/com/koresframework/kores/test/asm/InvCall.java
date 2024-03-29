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
import com.koresframework.kores.base.ConstructorDeclaration;
import com.koresframework.kores.base.TypeDeclaration;
import com.koresframework.kores.bytecode.processor.BytecodeGenerator;
import com.koresframework.kores.factory.Factories;
import com.koresframework.kores.factory.InvocationFactory;
import com.koresframework.kores.factory.VariableFactory;
import com.koresframework.kores.literal.Literals;
import com.github.jonathanxd.iutils.exception.RethrowException;

import org.junit.Test;

import kotlin.collections.CollectionsKt;

import static com.koresframework.kores.Types.STRING;

public class InvCall {

    @Test
    public void test() {

        ClassDeclaration codeClass;

        codeClass = ClassDeclaration.Builder.builder()
                .modifiers(KoresModifier.PUBLIC)
                .specifiedName("test.Impl")
                .superClass(My.class)
                .constructors(ConstructorDeclaration.Builder.builder()
                        .modifiers(KoresModifier.PUBLIC)
                        .body(Instructions.fromVarArgs(
                                VariableFactory.variable(STRING, "blc", Literals.STRING("099")),

                                InvocationFactory.invokeSuperConstructor(
                                        My.class,
                                        Factories.constructorTypeSpec(STRING),
                                        CollectionsKt.listOf(Factories.accessVariable(STRING, "blc")))
                        ))
                        .build())
                .build();


        BytecodeGenerator bytecodeGenerator = new BytecodeGenerator();

        byte[] gen = bytecodeGenerator.process(codeClass).get(0).getBytecode();

        ResultSaver.save(this.getClass(), gen);


        Class<?> define = new BCLoader().define(codeClass, gen);

        try {
            My o = (My) define.newInstance();

            System.out.println(o.getId());
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RethrowException(e);
        }

    }


    public static class My {
        private final String id;

        public My(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }

    private static final class BCLoader extends ClassLoader {

        public Class<?> define(TypeDeclaration typeDeclaration, byte[] bytes) {
            return super.defineClass(typeDeclaration.getType(), bytes, 0, bytes.length);
        }
    }

}
