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
import com.github.jonathanxd.codeapi.base.ClassDeclaration;
import com.github.jonathanxd.codeapi.base.CodeModifier;
import com.github.jonathanxd.codeapi.base.MethodDeclaration;
import com.github.jonathanxd.codeapi.base.TypeDeclaration;
import com.github.jonathanxd.codeapi.bytecode.BytecodeOptions;
import com.github.jonathanxd.codeapi.factory.Factories;
import com.github.jonathanxd.codeapi.literal.Literals;

import org.junit.Assert;
import org.junit.Test;

import java.util.function.UnaryOperator;

public class BridgeMethodsTest2 {

    @Test
    public void bridgeMethodTest2() throws Throwable {

        BCLoader bcLoader = new BCLoader();

        TypeDeclaration typeDeclaration = ClassDeclaration.Builder.builder()
                .modifiers(CodeModifier.PUBLIC)
                .qualifiedName("com.BridgeMethodTest2")
                .implementations(BaseString.class)
                .methods(MethodDeclaration.Builder.builder()
                        .modifiers(CodeModifier.PUBLIC)
                        .returnType(String.class)
                        .name("getValue")
                        .body(CodeSource.fromPart(
                                Factories.returnValue(String.class, Literals.STRING("works"))
                        ))
                        .build()
                )
                .build();

        Object o = CommonBytecodeTest.test(this.getClass(), typeDeclaration, UnaryOperator.identity(), Class::newInstance,
                bytecodeGenerator -> {
                    bytecodeGenerator.getOptions().set(BytecodeOptions.GENERATE_BRIDGE_METHODS, true);
                });

        Base base = (Base) o;

        Assert.assertEquals("works", base.getValue());

    }

    @Test
    public void bridgeMethodTest3() {

        TypeDeclaration typeDeclaration = ClassDeclaration.Builder.builder()
                .modifiers(CodeModifier.PUBLIC)
                .qualifiedName("com.BridgeMethodTest3")
                .implementations(BaseGInteger.class)
                .methods(MethodDeclaration.Builder.builder()
                        .modifiers(CodeModifier.PUBLIC)
                        .returnType(Integer.class)
                        .name("getValue")
                        .body(CodeSource.fromPart(
                                Factories.returnValue(Integer.class,
                                        Factories.cast(Integer.TYPE, Integer.class, Literals.INT(14)))
                        ))
                        .build()
                )
                .build();

        Object o = CommonBytecodeTest.test(this.getClass(), typeDeclaration, UnaryOperator.identity(), Class::newInstance,
                bytecodeGenerator -> {
                    bytecodeGenerator.getOptions().set(BytecodeOptions.GENERATE_BRIDGE_METHODS, true);
                });

        BaseGNumber base = (BaseGNumber) o;
        BaseGGeneric baseg = (BaseGGeneric) o;
        BaseGNumber basen = (BaseGNumber) o;
        BaseGInteger basegInteger = (BaseGInteger) o;

        Assert.assertEquals(14, base.getValue());
        Assert.assertEquals(14, baseg.getValue());
        Assert.assertEquals(14, basen.getValue());
        Assert.assertEquals((Object) 14, basegInteger.getValue());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void bridgeMethodTest4() {

        TypeDeclaration typeDeclaration = ClassDeclaration.Builder.builder()
                .modifiers(CodeModifier.PUBLIC)
                .qualifiedName("com.BridgeMethodTest4")
                .implementations(BaseGIntegerG.class)
                .methods(MethodDeclaration.Builder.builder()
                        .modifiers(CodeModifier.PUBLIC)
                        .returnType(Integer.class)
                        .name("getValue")
                        .body(CodeSource.fromPart(
                                Factories.returnValue(Integer.class,
                                        Factories.cast(Integer.TYPE, Integer.class, Literals.INT(14)))
                        ))
                        .build()
                )
                .build();

        Object o = CommonBytecodeTest.test(this.getClass(), typeDeclaration, UnaryOperator.identity(), Class::newInstance,
                bytecodeGenerator -> {
                    bytecodeGenerator.getOptions().set(BytecodeOptions.GENERATE_BRIDGE_METHODS, true);
                });

        BaseGNumberG<Number> base = (BaseGNumberG<Number>) o;
        BaseGNumberG<Integer> basegn = (BaseGNumberG<Integer>) o;
        BaseGGeneric baseg = (BaseGGeneric) o;
        BaseGIntegerG basen = (BaseGIntegerG) o;

        Assert.assertEquals(14, base.getValue());
        Assert.assertEquals(14, baseg.getValue());
        Assert.assertEquals((Object) 14, basegn.getValue());
        Assert.assertEquals((Object) 14, basen.getValue());
    }
}
