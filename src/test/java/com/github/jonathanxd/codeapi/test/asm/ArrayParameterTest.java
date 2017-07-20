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
import com.github.jonathanxd.codeapi.base.CodeModifier;
import com.github.jonathanxd.codeapi.bytecode.processor.BytecodeGenerator;
import com.github.jonathanxd.codeapi.factory.VariableFactory;
import com.github.jonathanxd.codeapi.test.PredefinedTest;

import org.junit.Test;

import static com.github.jonathanxd.codeapi.factory.Factories.accessVariable;
import static com.github.jonathanxd.codeapi.factory.Factories.cast;
import static com.github.jonathanxd.codeapi.factory.Factories.parameter;

public class ArrayParameterTest {
    final String name = getClass().getCanonicalName() + "_Generated";

    @Test
    public void arrayTest() {
        PredefinedTest predefinedTest = PredefinedTest.create(name);

        predefinedTest.constructor
                .modifiers(CodeModifier.PUBLIC)
                .parameters(parameter(Text[].class, "par"))
                .body(CodeSource.fromVarArgs(
                        VariableFactory.variable(Object.class, "cf", cast(Text[].class, Object.class, accessVariable(Text[].class, "par"))),
                        VariableFactory.variable(Text[].class, "lt", cast(Object.class, Text[].class, accessVariable(Object.class, "cf")))
                ));


        BytecodeGenerator bytecodeGenerator = new BytecodeGenerator();

        byte[] bytes = bytecodeGenerator.process(predefinedTest.build()).get(0).getBytecode();

        ResultSaver.save(getClass(), bytes);

        Class<?> define = new BCLoader().define(name, bytes);

        try {
            define.getConstructor(Text[].class).newInstance((Object) new Text[]{});
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

    }

    public static class Text {

    }

    private static final class BCLoader extends ClassLoader {

        public Class<?> define(String name, byte[] bytes) {
            return super.defineClass(name, bytes, 0, bytes.length);
        }
    }
}
