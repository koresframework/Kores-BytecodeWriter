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
import com.koresframework.kores.Types;
import com.koresframework.kores.base.ClassDeclaration;
import com.koresframework.kores.base.KoresModifier;
import com.koresframework.kores.base.MethodDeclaration;
import com.koresframework.kores.base.TypeDeclaration;
import com.koresframework.kores.bytecode.BytecodeClass;
import com.koresframework.kores.bytecode.classloader.CodeClassLoader;
import com.koresframework.kores.bytecode.processor.BytecodeGenerator;
import com.koresframework.kores.helper.Predefined;

import org.junit.Test;

import java.util.Collection;
import java.util.List;

import static com.koresframework.kores.Types.VOID;
import static com.koresframework.kores.factory.Factories.accessVariable;
import static com.koresframework.kores.factory.Factories.parameter;
import static com.koresframework.kores.factory.VariableFactory.variable;
import static com.koresframework.kores.literal.Literals.STRING;

public class DoubleTest {


    @SuppressWarnings("unchecked")
    @Test
    public void wiki() throws Throwable {

        TypeDeclaration decl = ClassDeclaration.Builder.builder()
                .modifiers(KoresModifier.PUBLIC)
                .name("com.MyClass")
                .methods(MethodDeclaration.Builder.builder()
                        .modifiers(KoresModifier.PUBLIC)
                        .name("test")
                        .returnType(VOID)
                        .parameters(parameter(Float.TYPE, "f"),
                                parameter(Boolean.TYPE, "b"),
                                parameter(Short.TYPE, "s"),
                                parameter(Double.TYPE, "d"),
                                parameter(Long.TYPE, "l"))
                        .body(Instructions.fromVarArgs(
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
