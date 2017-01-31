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
import com.github.jonathanxd.codeapi.base.TypeDeclaration;
import com.github.jonathanxd.codeapi.bytecode.BytecodeClass;
import com.github.jonathanxd.codeapi.bytecode.classloader.CodeClassLoader;
import com.github.jonathanxd.codeapi.bytecode.gen.BytecodeGenerator;
import com.github.jonathanxd.codeapi.common.CodeParameter;
import com.github.jonathanxd.codeapi.helper.Predefined;

import org.junit.Test;

import java.util.EnumSet;

import static com.github.jonathanxd.codeapi.CodeAPI.parameter;
import static com.github.jonathanxd.codeapi.CodeAPI.sourceOfParts;
import static com.github.jonathanxd.codeapi.Types.VOID;
import static com.github.jonathanxd.codeapi.common.CodeModifier.PUBLIC;
import static com.github.jonathanxd.codeapi.factory.ClassFactory.aClass;
import static com.github.jonathanxd.codeapi.factory.MethodFactory.method;
import static com.github.jonathanxd.codeapi.factory.VariableFactory.variable;
import static com.github.jonathanxd.codeapi.literal.Literals.STRING;

public class DoubleTest {


    @Test
    public void wiki() throws Throwable {
        CodeSource source = sourceOfParts(
                method(EnumSet.of(PUBLIC), "test", VOID, new CodeParameter[]{
                                parameter(Float.TYPE, "f"),
                                parameter(Boolean.TYPE, "b"),
                                parameter(Short.TYPE, "s"),
                                parameter(Double.TYPE, "d"),
                                parameter(Long.TYPE, "l"),
                        },
                        sourceOfParts(
                                variable(Types.STRING, "variable"),
                                Predefined.invokePrintlnStr(STRING("Hello world")),
                                Predefined.invokePrintln(CodeAPI.accessLocalVariable(Float.TYPE, "f")),
                                Predefined.invokePrintln(CodeAPI.accessLocalVariable(Boolean.TYPE, "b")),
                                Predefined.invokePrintln(CodeAPI.accessLocalVariable(Short.TYPE, "s")),
                                Predefined.invokePrintln(CodeAPI.accessLocalVariable(Double.TYPE, "d")),
                                Predefined.invokePrintln(CodeAPI.accessLocalVariable(Long.TYPE, "l"))
                        )
                )
        );

        TypeDeclaration decl = aClass(EnumSet.of(PUBLIC), "com.MyClass", source);

        BytecodeGenerator bytecodeGenerator = new BytecodeGenerator();

        BytecodeClass[] gen = bytecodeGenerator.gen(decl);

        ResultSaver.save(this.getClass(), gen);

        CodeClassLoader codeClassLoader = new CodeClassLoader();

        Class<?> define = codeClassLoader.define(gen);

        define.getDeclaredMethod("test", Float.TYPE, Boolean.TYPE, Short.TYPE, Double.TYPE, Long.TYPE).invoke(define.newInstance(), 1.0F, false, (short) 5, 56.8959D, 15656L);


    }
}
