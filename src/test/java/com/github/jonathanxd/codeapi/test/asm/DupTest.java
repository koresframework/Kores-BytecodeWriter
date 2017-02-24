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
import com.github.jonathanxd.codeapi.base.TypeDeclaration;
import com.github.jonathanxd.codeapi.builder.ClassDeclarationBuilder;
import com.github.jonathanxd.codeapi.builder.ConstructorDeclarationBuilder;
import com.github.jonathanxd.codeapi.builder.IfStatementBuilder;
import com.github.jonathanxd.codeapi.bytecode.extra.Dup;
import com.github.jonathanxd.codeapi.bytecode.extra.Pop;
import com.github.jonathanxd.codeapi.common.CodeModifier;
import com.github.jonathanxd.codeapi.common.TypeSpec;
import com.github.jonathanxd.codeapi.helper.Predefined;
import com.github.jonathanxd.codeapi.literal.Literals;
import com.github.jonathanxd.codeapi.util.Stack;

import org.junit.Test;

import java.util.Collections;

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

        CommonBytecodeTest.test(this.getClass(), getNull, CodeAPI.source(getNull));

        TypeDeclaration getNotNull = gen("getNotNull");

        CommonBytecodeTest.test(this.getClass(), getNotNull, CodeAPI.source(getNotNull));
    }

    private TypeDeclaration gen(String methodName) {
        return ClassDeclarationBuilder.builder()
                .withModifiers(CodeModifier.PUBLIC)
                .withQualifiedName("test.DupTest_" + methodName)
                .withSuperClass(Types.OBJECT)
                .withBody(CodeAPI.source(
                        ConstructorDeclarationBuilder.builder()
                                .withModifiers(CodeModifier.PUBLIC)
                                .withBody(
                                        CodeAPI.source(Predefined.invokePrintlnStr(
                                                IfStatementBuilder.builder()
                                                        .withExpressions(CodeAPI.checkNotNull(
                                                                new Dup(CodeAPI.invokeStatic(DupTest.class, methodName, new TypeSpec(Types.STRING), Collections.emptyList()))
                                                        ))
                                                        .withBody(CodeAPI.source(Stack.INSTANCE))
                                                        .withElseStatement(CodeAPI.source(Pop.INSTANCE, Literals.STRING("NULL")))
                                                        .build()
                                        ))
                                )
                                .build()
                ))
                .build();
    }
}
