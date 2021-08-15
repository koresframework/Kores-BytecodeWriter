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
import com.koresframework.kores.base.KoresModifier;
import com.koresframework.kores.base.InterfaceDeclaration;
import com.koresframework.kores.base.MethodDeclaration;
import com.koresframework.kores.base.TypeDeclaration;
import com.koresframework.kores.bytecode.processor.BytecodeGenerator;
import com.koresframework.kores.literal.Literals;
import com.koresframework.kores.operator.Operators;

import org.junit.Test;

import static com.koresframework.kores.Types.INT;
import static com.koresframework.kores.Types.STRING;
import static com.koresframework.kores.factory.Factories.accessVariable;
import static com.koresframework.kores.factory.Factories.operate;
import static com.koresframework.kores.factory.Factories.parameter;
import static com.koresframework.kores.factory.Factories.returnValue;

public class InterfaceTest {

    @Test
    public void test() {

        InterfaceDeclaration interfaceDeclaration = InterfaceDeclaration.Builder.builder()
                .modifiers(KoresModifier.PUBLIC)
                .name("test.Impl")
                .methods(
                        MethodDeclaration.Builder.builder()
                                .modifiers(KoresModifier.PUBLIC)
                                .name("parse")
                                .parameters(parameter(STRING, "string"))
                                .build(),
                        MethodDeclaration.Builder.builder()
                                .modifiers(KoresModifier.PUBLIC)
                                .returnType(INT)
                                .name("getI")
                                .parameters(parameter(INT, "num"))
                                .body(Instructions.fromPart(
                                        returnValue(INT, operate(accessVariable(INT, "num"), Operators.MULTIPLY, Literals.INT(9)))
                                ))
                                .build()
                )
                .build();


        BytecodeGenerator bytecodeGenerator = new BytecodeGenerator();
        byte[] gen = bytecodeGenerator.process(interfaceDeclaration).get(0).getBytecode();

        ResultSaver.save(this.getClass(), gen);


        Class<?> define = new BCLoader().define(interfaceDeclaration, gen);

        /*try {
            //My o = (My) define.newInstance();

            //System.out.println(o.getId());
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RethrowException(e);
        }*/

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
