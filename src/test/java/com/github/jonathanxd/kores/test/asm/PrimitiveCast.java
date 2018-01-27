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

import com.github.jonathanxd.kores.Instructions;
import com.github.jonathanxd.kores.Types;
import com.github.jonathanxd.kores.base.ClassDeclaration;
import com.github.jonathanxd.kores.base.MethodDeclaration;
import com.github.jonathanxd.kores.base.TypeDeclaration;
import com.github.jonathanxd.kores.bytecode.processor.BytecodeGenerator;
import com.github.jonathanxd.kores.factory.InvocationFactory;
import com.github.jonathanxd.kores.helper.Predefined;
import com.github.jonathanxd.kores.literal.Literals;

import org.junit.Test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.Collections;

import static com.github.jonathanxd.kores.Types.BYTE;
import static com.github.jonathanxd.kores.Types.INT;
import static com.github.jonathanxd.kores.Types.LONG;
import static com.github.jonathanxd.kores.Types.OBJECT;
import static com.github.jonathanxd.kores.Types.STRING;
import static com.github.jonathanxd.kores.base.KoresModifier.PUBLIC;
import static com.github.jonathanxd.kores.base.KoresModifier.STATIC;
import static com.github.jonathanxd.kores.factory.Factories.accessVariable;
import static com.github.jonathanxd.kores.factory.Factories.cast;
import static com.github.jonathanxd.kores.factory.Factories.constructorTypeSpec;
import static com.github.jonathanxd.kores.factory.Factories.parameter;
import static com.github.jonathanxd.kores.factory.Factories.returnValue;
import static com.github.jonathanxd.kores.factory.VariableFactory.variable;


@SuppressWarnings("Duplicates")
public class PrimitiveCast {

    @Test
    public void codeAPITest() {


        String name = this.getClass().getCanonicalName() + "_Generated";

        ClassDeclaration codeClass = ClassDeclaration.Builder.builder()
                .modifiers(PUBLIC)
                .name(name)
                .methods(
                        MethodDeclaration.Builder.builder()
                                .modifiers(PUBLIC, STATIC)
                                .name("printString")
                                .returnType(INT)
                                .parameters(parameter(STRING, "string"))
                                .body(Instructions.fromVarArgs(
                                        Predefined.invokePrintln(accessVariable(STRING, "string")),

                                        variable(OBJECT, "objectF", cast(INT, OBJECT, Literals.INT(9))),

                                        variable(OBJECT, "iF", cast(INT, OBJECT, Literals.INT(9))),

                                        variable(INT, "IntegerBoxed", cast(OBJECT, INT, accessVariable(OBJECT, "iF"))),

                                        variable(INT, "IntegerBoxed2", cast(OBJECT, INT, accessVariable(OBJECT, "iF"))),

                                        variable(INT, "int", Literals.INT(9)),

                                        variable(INT, "IntToInt", cast(INT, INT, accessVariable(INT, "int"))),

                                        variable(LONG, "Long", Literals.LONG(59855246879798L)),
                                        variable(BYTE, "LongToByte", cast(LONG, BYTE, accessVariable(LONG, "Long"))),


                                        // Cast Integer to Int
                                        returnValue(int.class, cast(Types.INTEGER_WRAPPER, Types.INT,
                                                InvocationFactory.invokeConstructor(
                                                        Types.INTEGER_WRAPPER,
                                                        constructorTypeSpec(int.class),
                                                        Collections.singletonList(Literals.INT(9)))))
                                ))
                                .build()
                )
                .build();

        byte[] bytes = generate(codeClass);

        ResultSaver.save(this.getClass(), bytes);

        Class<?> define = new BCLoader().define(name, bytes);
        try {
            define.newInstance();

            MethodHandles.Lookup lookup = MethodHandles.publicLookup();

            MethodHandle println = lookup.findStatic(define, "printString", java.lang.invoke.MethodType.methodType(Integer.TYPE, String.class));

            int x = (int) println.invoke("Test");

            System.out.println(x);

        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }


    public byte[] generate(TypeDeclaration declaration) {
        BytecodeGenerator generator = new BytecodeGenerator();

        byte[] bytes = generator.process(declaration).get(0).getBytecode();

        ResultSaver.save(this.getClass(), bytes);

        return bytes;
    }

    private static final class BCLoader extends ClassLoader {

        public Class<?> define(String name, byte[] bytes) {
            return super.defineClass(name, bytes, 0, bytes.length);
        }
    }

}
