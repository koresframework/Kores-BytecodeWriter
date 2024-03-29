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

import com.github.jonathanxd.iutils.annotation.Named;
import com.koresframework.kores.Instruction;
import com.koresframework.kores.Instructions;
import com.koresframework.kores.base.ClassDeclaration;
import com.koresframework.kores.base.KoresModifier;
import com.koresframework.kores.base.MethodDeclaration;
import com.koresframework.kores.base.TypeDeclaration;
import com.koresframework.kores.base.TypeSpec;
import com.koresframework.kores.bytecode.BytecodeOptions;
import com.koresframework.kores.bytecode.VisitLineType;
import com.koresframework.kores.factory.Factories;
import com.koresframework.kores.factory.InvocationFactory;
import com.koresframework.kores.factory.VariableFactory;
import com.koresframework.kores.helper.Predefined;
import com.koresframework.kores.literal.Literals;

import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;

public class TryCatchFinallyTest {

    @Test(expected = InvocationTargetException.class)
    public void tryCatchFinallyTest() throws Exception {
        TypeDeclaration decl = ClassDeclaration.Builder.builder()
                .modifiers(KoresModifier.PUBLIC)
                .specifiedName("com.MyClass")
                .methods(MethodDeclaration.Builder.builder()
                        .modifiers(KoresModifier.PUBLIC)
                        .name("test")
                        .parameters(Factories.parameter(Boolean.TYPE, "a"))
                        .body(Instructions.fromVarArgs(
                                Factories.tryStatement(
                                        Instructions.fromVarArgs(
                                                Factories.ifStatement(Factories.checkTrue(
                                                        Factories.accessVariable(Boolean.TYPE, "a")
                                                ), Instructions.fromPart(
                                                        Factories.returnVoid()
                                                ), Instructions.fromPart(
                                                        Predefined.invokePrintlnStr(Literals.STRING("X"))
                                                )),
                                                invokeCFName(Literals.STRING("f"))
                                        ),
                                        Collections.singletonList(
                                                Factories.catchStatement(
                                                        ClassNotFoundException.class,
                                                        VariableFactory.variable(ClassNotFoundException.class, "ex"),
                                                        Instructions.fromPart(
                                                                Factories.throwException(
                                                                        Factories.accessVariable(ClassNotFoundException.class, "ex")
                                                                )
                                                        )
                                                )
                                        ),
                                        Instructions.fromPart(Predefined.invokePrintlnStr(Literals.STRING("Finally")))
                                )
                        ))
                        .build())
                .build();

        @Named("Instance") Object test = CommonBytecodeTest.test(this.getClass(), decl, typeDeclaration -> typeDeclaration,
                aClass -> aClass.getConstructor().newInstance(),
                bytecodeGenerator -> {
                    bytecodeGenerator.getOptions().set(BytecodeOptions.VISIT_LINES, VisitLineType.GEN_LINE_INSTRUCTION);
                });

        test.getClass().getDeclaredMethod("test", Boolean.TYPE).invoke(test, false);
    }

    private Instruction invokeCFName(Instruction name) {
        return InvocationFactory.invokeStatic(Class.class,
                "forName",
                new TypeSpec(Class.class, Collections.singletonList(String.class)),
                Collections.singletonList(name)
        );
    }

}
