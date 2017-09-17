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
import com.github.jonathanxd.codeapi.base.ClassDeclaration;
import com.github.jonathanxd.codeapi.base.CodeModifier;
import com.github.jonathanxd.codeapi.base.ForEachStatement;
import com.github.jonathanxd.codeapi.base.InterfaceDeclaration;
import com.github.jonathanxd.codeapi.base.MethodDeclaration;
import com.github.jonathanxd.codeapi.base.TypeDeclaration;
import com.github.jonathanxd.codeapi.bytecode.BytecodeClass;
import com.github.jonathanxd.codeapi.bytecode.BytecodeOptions;
import com.github.jonathanxd.codeapi.bytecode.processor.BytecodeGenerator;
import com.github.jonathanxd.codeapi.factory.Factories;
import com.github.jonathanxd.codeapi.factory.InvocationFactory;
import com.github.jonathanxd.codeapi.factory.VariableFactory;
import com.github.jonathanxd.codeapi.generic.GenericSignature;
import com.github.jonathanxd.codeapi.helper.Predefined;
import com.github.jonathanxd.codeapi.literal.Literals;
import com.github.jonathanxd.codeapi.type.Generic;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.UnaryOperator;

import kotlin.collections.CollectionsKt;

import static com.github.jonathanxd.codeapi.factory.Factories.accessVariable;
import static com.github.jonathanxd.codeapi.factory.Factories.forEachIterable;
import static com.github.jonathanxd.codeapi.factory.Factories.parameter;
import static com.github.jonathanxd.codeapi.factory.Factories.typeSpec;

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
    public void bridgeMethodTest3() throws Throwable {

        BCLoader bcLoader = new BCLoader();

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
}
