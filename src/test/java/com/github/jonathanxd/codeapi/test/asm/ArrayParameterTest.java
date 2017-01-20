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
import com.github.jonathanxd.codeapi.Types;
import com.github.jonathanxd.codeapi.base.ClassDeclaration;
import com.github.jonathanxd.codeapi.bytecode.gen.BytecodeGenerator;
import com.github.jonathanxd.codeapi.common.CodeParameter;
import com.github.jonathanxd.codeapi.factory.ClassFactory;
import com.github.jonathanxd.codeapi.factory.VariableFactory;

import org.junit.Test;

import java.util.EnumSet;

import static com.github.jonathanxd.codeapi.CodeAPI.source;
import static com.github.jonathanxd.codeapi.CodeAPI.sourceOfParts;
import static com.github.jonathanxd.codeapi.common.CodeModifier.PUBLIC;
import static com.github.jonathanxd.codeapi.factory.ConstructorFactory.constructor;

public class ArrayParameterTest {
    final String name = getClass().getCanonicalName() + "_Generated";

    @Test
    public void arrayTest() {


        ClassDeclaration codeClass = ClassFactory.aClass(EnumSet.of(PUBLIC), name, source(
                constructor(EnumSet.of(PUBLIC), new CodeParameter[]{new CodeParameter(CodeAPI.getJavaType(Text[].class), "par")}, source(
                        VariableFactory.variable(Types.OBJECT, "cf", CodeAPI.cast(CodeAPI.getJavaType(Text[].class), Types.OBJECT, CodeAPI.accessLocalVariable(Text[].class, "par"))),
                        VariableFactory.variable(CodeAPI.getJavaType(Text[].class), "lt", CodeAPI.cast(Types.OBJECT, CodeAPI.getJavaType(Text[].class), CodeAPI.accessLocalVariable(Types.OBJECT, "cf")))
                ))
        ));

        BytecodeGenerator bytecodeGenerator = new BytecodeGenerator();

        byte[] bytes = bytecodeGenerator.gen(sourceOfParts(codeClass))[0].getBytecode();

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
