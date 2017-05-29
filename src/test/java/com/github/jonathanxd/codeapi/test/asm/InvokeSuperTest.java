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
package com.github.jonathanxd.codeapi.test.asm;

import com.github.jonathanxd.codeapi.CodeSource;
import com.github.jonathanxd.codeapi.base.ClassDeclaration;
import com.github.jonathanxd.codeapi.base.CodeModifier;
import com.github.jonathanxd.codeapi.base.ConstructorDeclaration;
import com.github.jonathanxd.codeapi.base.TypeDeclaration;
import com.github.jonathanxd.codeapi.bytecode.processor.BytecodeProcessor;
import com.github.jonathanxd.codeapi.factory.Factories;
import com.github.jonathanxd.codeapi.factory.InvocationFactory;
import com.github.jonathanxd.codeapi.factory.VariableFactory;
import com.github.jonathanxd.codeapi.literal.Literals;
import com.github.jonathanxd.iutils.exception.RethrowException;

import org.junit.Test;

import kotlin.collections.CollectionsKt;

import static com.github.jonathanxd.codeapi.Types.STRING;

public class InvokeSuperTest {

    @Test
    public void test() {

        TypeDeclaration codeClass = ClassDeclaration.Builder.builder()
                .modifiers(CodeModifier.PUBLIC)
                .name("test.Impl")
                .superClass(My.class)
                .constructors(ConstructorDeclaration.Builder.builder()
                        .modifiers(CodeModifier.PUBLIC)
                        .body(CodeSource.fromVarArgs(
                                VariableFactory.variable(STRING, "blc", Literals.STRING("099")),

                                InvocationFactory.invokeSuperConstructor(
                                        My.class,
                                        Factories.constructorTypeSpec(STRING),
                                        CollectionsKt.listOf(Factories.accessVariable(STRING, "blc"))
                                )
                        ))
                        .build())
                .build();


        BytecodeProcessor bytecodeProcessor = new BytecodeProcessor();

        byte[] gen = bytecodeProcessor.process(codeClass).get(0).getBytecode();

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

}
