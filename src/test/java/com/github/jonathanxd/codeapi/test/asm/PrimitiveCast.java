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

import com.github.jonathanxd.codeapi.CodeAPI;
import com.github.jonathanxd.codeapi.CodeSource;
import com.github.jonathanxd.codeapi.Types;
import com.github.jonathanxd.codeapi.base.ClassDeclaration;
import com.github.jonathanxd.codeapi.bytecode.gen.BytecodeGenerator;
import com.github.jonathanxd.codeapi.common.CodeParameter;
import com.github.jonathanxd.codeapi.helper.Predefined;
import com.github.jonathanxd.codeapi.literal.Literals;

import org.junit.Test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.EnumSet;

import static com.github.jonathanxd.codeapi.CodeAPI.accessLocalVariable;
import static com.github.jonathanxd.codeapi.CodeAPI.argument;
import static com.github.jonathanxd.codeapi.CodeAPI.invokeConstructor;
import static com.github.jonathanxd.codeapi.CodeAPI.parameter;
import static com.github.jonathanxd.codeapi.CodeAPI.returnValue;
import static com.github.jonathanxd.codeapi.CodeAPI.source;
import static com.github.jonathanxd.codeapi.Types.BYTE;
import static com.github.jonathanxd.codeapi.Types.INT;
import static com.github.jonathanxd.codeapi.Types.LONG;
import static com.github.jonathanxd.codeapi.Types.OBJECT;
import static com.github.jonathanxd.codeapi.Types.STRING;
import static com.github.jonathanxd.codeapi.common.CodeModifier.PUBLIC;
import static com.github.jonathanxd.codeapi.common.CodeModifier.STATIC;
import static com.github.jonathanxd.codeapi.factory.ClassFactory.aClass;
import static com.github.jonathanxd.codeapi.factory.MethodFactory.method;
import static com.github.jonathanxd.codeapi.factory.VariableFactory.variable;


/**
 * Created by jonathan on 02/05/16.
 */
@SuppressWarnings("Duplicates")
public class PrimitiveCast {

    @Test
    public void codeAPITest() {


        String name = this.getClass().getCanonicalName() + "_Generated";

        ClassDeclaration codeClass = aClass(EnumSet.of(PUBLIC), name, source(
                method(EnumSet.of(PUBLIC, STATIC), "printString", INT, new CodeParameter[]{parameter(STRING, "string")},
                        source(
                                Predefined.invokePrintln(argument(accessLocalVariable(STRING, "string"))),

                                variable(OBJECT, "objectF", CodeAPI.cast(INT, OBJECT, Literals.INT(9))),

                                variable(OBJECT, "iF", CodeAPI.cast(INT, OBJECT, Literals.INT(9))),

                                variable(INT, "IntegerBoxed", CodeAPI.cast(OBJECT, INT, CodeAPI.accessLocalVariable(OBJECT, "iF"))),

                                variable(INT, "IntegerBoxed2", CodeAPI.cast(OBJECT, INT, CodeAPI.accessLocalVariable(OBJECT, "iF"))),

                                variable(INT, "int", Literals.INT(9)),

                                variable(INT, "IntToInt", CodeAPI.cast(INT, INT, CodeAPI.accessLocalVariable(INT, "int"))),

                                variable(LONG, "Long", Literals.LONG(59855246879798L)),
                                variable(BYTE, "LongToByte", CodeAPI.cast(LONG, BYTE, CodeAPI.accessLocalVariable(LONG, "Long"))),


                                // Cast Integer to Int
                                returnValue(int.class, CodeAPI.cast(Types.INTEGER_WRAPPER, Types.INT,
                                        invokeConstructor(
                                                Types.INTEGER_WRAPPER,
                                                CodeAPI.constructorTypeSpec(int.class),
                                                Collections.singletonList(argument(Literals.INT(9))))))
                        ))
        ));

        CodeSource mySource = CodeAPI.sourceOfParts(codeClass);


        byte[] bytes = generate(mySource);

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


    public byte[] generate(CodeSource source) {
        BytecodeGenerator generator = new BytecodeGenerator();

        byte[] bytes = generator.gen(source)[0].getBytecode();

        ResultSaver.save(this.getClass(), bytes);

        return bytes;
    }

    private static final class BCLoader extends ClassLoader {

        public Class<?> define(String name, byte[] bytes) {
            return super.defineClass(name, bytes, 0, bytes.length);
        }
    }

}
