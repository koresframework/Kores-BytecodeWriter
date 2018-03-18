/*
 *      Kores-BytecodeWriter - Translates Kores Structure to JVM Bytecode <https://github.com/JonathanxD/CodeAPI-BytecodeWriter>
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
import com.github.jonathanxd.kores.base.ClassDeclaration;
import com.github.jonathanxd.kores.base.KoresModifier;
import com.github.jonathanxd.kores.base.ConstructorDeclaration;
import com.github.jonathanxd.kores.base.TryStatement;
import com.github.jonathanxd.kores.base.TypeDeclaration;
import com.github.jonathanxd.kores.base.TypeSpec;
import com.github.jonathanxd.kores.factory.Factories;
import com.github.jonathanxd.kores.factory.InvocationFactory;
import com.github.jonathanxd.kores.factory.VariableFactory;
import com.github.jonathanxd.kores.test.TryTest_;
import com.github.jonathanxd.iutils.annotation.Named;
import com.github.jonathanxd.iutils.collection.Collections3;

import org.junit.Test;

import java.util.Collections;
import java.util.function.UnaryOperator;

public class TryTest {

    public static String x() {
        return null;
    }

    @Test
    public void tryTest() {
        TypeDeclaration $ = TryTest_.$();
        @Named("Instance") Object test = CommonBytecodeTest.test(this.getClass(), $, UnaryOperator.identity());
    }

    @Test
    public void tryTest2() {
        TryStatement tryStatement = Factories.tryStatement(
                Instructions.fromVarArgs(
                        InvocationFactory.invokeStatic(
                                TryTest_.class,
                                "boom",
                                new TypeSpec(Void.TYPE),
                                Collections.emptyList()
                        )/*,
                        InvocationFactory.invokeStatic(
                                TryTest.class,
                                "x",
                                new TypeSpec(String.class),
                                Collections.emptyList()
                        )*/
                ),
                Collections3.listOf(
                        Factories.catchStatement(Exception.class,
                                VariableFactory.variable(Exception.class, "e"),
                                Instructions.fromVarArgs(
                                        InvocationFactory.invokeVirtual(
                                                Exception.class,
                                                Factories.accessVariable(Exception.class, "e"),
                                                "printStackTrace",
                                                new TypeSpec(Void.TYPE),
                                                Collections.emptyList()
                                        ),
                                        InvocationFactory.invokeStatic(
                                                TryTest.class,
                                                "x",
                                                new TypeSpec(String.class),
                                                Collections.emptyList()
                                        )
                                        //Predefined.invokePrintlnStr(Literals.STRING("a"))
                                        //Pop.INSTANCE
                                ))
                ),
                Instructions.empty()//Instructions.fromPart(Predefined.invokePrintln(Literals.STRING("Finally")))
        );

        TypeDeclaration $ = ClassDeclaration.Builder.builder()
                .modifiers(KoresModifier.PUBLIC)
                .specifiedName("test.TryCatchFinally")
                .constructors(ConstructorDeclaration.Builder.builder()
                        .body(Instructions.fromVarArgs(
                                tryStatement
                                /*InstructionCodePart.Companion.create((o, data, processorManager) -> {
                                    MethodVisitor mv = KeysKt.getMETHOD_VISITOR().getOrNull(data).getMethodVisitor();
                                    mv.visitVarInsn(Opcodes.ASTORE, 2);
                                    return Unit.INSTANCE;
                                })*/
                        ))
                        .build())
                .build();

        @Named("Instance") Object test = CommonBytecodeTest.test(this.getClass(), $, UnaryOperator.identity());
    }
}
