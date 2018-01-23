/*
 *      CodeAPI-BytecodeWriter - Translates CodeAPI Structure to JVM Bytecode <https://github.com/JonathanxD/CodeAPI-BytecodeWriter>
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
package com.github.jonathanxd.codeapi.test.asm;

import com.github.jonathanxd.codeapi.CodeSource;
import com.github.jonathanxd.codeapi.Types;
import com.github.jonathanxd.codeapi.base.ClassDeclaration;
import com.github.jonathanxd.codeapi.base.CodeModifier;
import com.github.jonathanxd.codeapi.base.MethodDeclaration;
import com.github.jonathanxd.codeapi.base.TypeDeclaration;
import com.github.jonathanxd.codeapi.bytecode.BytecodeClass;
import com.github.jonathanxd.codeapi.bytecode.classloader.CodeClassLoader;
import com.github.jonathanxd.codeapi.bytecode.processor.BytecodeGenerator;
import com.github.jonathanxd.codeapi.helper.Predefined;

import org.junit.Test;

import java.util.Collection;
import java.util.List;

import static com.github.jonathanxd.codeapi.Types.VOID;
import static com.github.jonathanxd.codeapi.factory.Factories.accessVariable;
import static com.github.jonathanxd.codeapi.factory.Factories.parameter;
import static com.github.jonathanxd.codeapi.factory.VariableFactory.variable;
import static com.github.jonathanxd.codeapi.literal.Literals.STRING;

public class DoubleTest {


    @SuppressWarnings("unchecked")
    @Test
    public void wiki() throws Throwable {

        TypeDeclaration decl = ClassDeclaration.Builder.builder()
                .modifiers(CodeModifier.PUBLIC)
                .name("com.MyClass")
                .methods(MethodDeclaration.Builder.builder()
                        .modifiers(CodeModifier.PUBLIC)
                        .name("test")
                        .returnType(VOID)
                        .parameters(parameter(Float.TYPE, "f"),
                                parameter(Boolean.TYPE, "b"),
                                parameter(Short.TYPE, "s"),
                                parameter(Double.TYPE, "d"),
                                parameter(Long.TYPE, "l"))
                        .body(CodeSource.fromVarArgs(
                                variable(Types.STRING, "variable"),
                                Predefined.invokePrintlnStr(STRING("Hello world")),
                                Predefined.invokePrintln(accessVariable(Float.TYPE, "f")),
                                Predefined.invokePrintln(accessVariable(Boolean.TYPE, "b")),
                                Predefined.invokePrintln(accessVariable(Short.TYPE, "s")),
                                Predefined.invokePrintln(accessVariable(Double.TYPE, "d")),
                                Predefined.invokePrintln(accessVariable(Long.TYPE, "l"))
                        ))
                        .build())
                .build();

        BytecodeGenerator bytecodeGenerator = new BytecodeGenerator();

        List<? extends BytecodeClass> gen = bytecodeGenerator.process(decl);

        ResultSaver.save(this.getClass(), gen);

        CodeClassLoader codeClassLoader = new CodeClassLoader();

        Class<?> define = codeClassLoader.define((Collection<BytecodeClass>) gen);

        define.getDeclaredMethod("test", Float.TYPE, Boolean.TYPE, Short.TYPE, Double.TYPE, Long.TYPE).invoke(define.newInstance(), 1.0F, false, (short) 5, 56.8959D, 15656L);


    }
}
