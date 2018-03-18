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
import com.github.jonathanxd.kores.Types;
import com.github.jonathanxd.kores.base.ClassDeclaration;
import com.github.jonathanxd.kores.base.KoresModifier;
import com.github.jonathanxd.kores.base.ConstructorDeclaration;
import com.github.jonathanxd.kores.base.IfStatement;
import com.github.jonathanxd.kores.base.TypeDeclaration;
import com.github.jonathanxd.kores.base.TypeSpec;
import com.github.jonathanxd.kores.bytecode.extra.Dup;
import com.github.jonathanxd.kores.bytecode.extra.Pop;
import com.github.jonathanxd.kores.common.Stack;
import com.github.jonathanxd.kores.factory.Factories;
import com.github.jonathanxd.kores.factory.InvocationFactory;
import com.github.jonathanxd.kores.helper.Predefined;
import com.github.jonathanxd.kores.literal.Literals;

import org.junit.Test;

import java.util.Collections;
import java.util.function.UnaryOperator;

public class DupTest {

    public static String getNull() {
        return null;
    }

    public static String getNotNull() {
        return "Aa";
    }

    @Test
    public void dupTest() {
        TypeDeclaration getNull = gen("getNull");

        CommonBytecodeTest.test(this.getClass(), getNull, UnaryOperator.identity());

        TypeDeclaration getNotNull = gen("getNotNull");

        CommonBytecodeTest.test(this.getClass(), getNotNull, UnaryOperator.identity());
    }

    private TypeDeclaration gen(String methodName) {
        Dup dup = new Dup(InvocationFactory.invokeStatic(DupTest.class, methodName, new TypeSpec(Types.STRING), Collections.emptyList()));

        return ClassDeclaration.Builder.builder()
                .modifiers(KoresModifier.PUBLIC)
                .qualifiedName("test.DupTest_" + methodName)
                .superClass(Types.OBJECT)
                .constructors(
                        ConstructorDeclaration.Builder.builder()
                                .modifiers(KoresModifier.PUBLIC)
                                .body(Instructions.fromPart(Predefined.invokePrintlnStr(
                                        IfStatement.Builder.builder()
                                                .expressions(Factories.checkNotNull(
                                                        dup
                                                ))
                                                .body(Instructions.fromPart(Stack.INSTANCE))
                                                .elseStatement(Instructions.fromVarArgs(Pop.INSTANCE, Literals.STRING("NULL")))
                                                .build()
                                        ))
                                )
                                .build()
                )
                .build();
    }
}
