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
import com.github.jonathanxd.codeapi.base.Access;
import com.github.jonathanxd.codeapi.base.ClassDeclaration;
import com.github.jonathanxd.codeapi.base.CodeModifier;
import com.github.jonathanxd.codeapi.base.InvokeType;
import com.github.jonathanxd.codeapi.base.MethodDeclaration;
import com.github.jonathanxd.codeapi.base.MethodInvocation;
import com.github.jonathanxd.codeapi.base.TypeDeclaration;
import com.github.jonathanxd.codeapi.base.TypeSpec;
import com.github.jonathanxd.codeapi.bytecode.BytecodeClass;
import com.github.jonathanxd.codeapi.bytecode.classloader.CodeClassLoader;
import com.github.jonathanxd.codeapi.bytecode.exception.ClassCheckException;
import com.github.jonathanxd.codeapi.bytecode.processor.BytecodeGenerator;
import com.github.jonathanxd.codeapi.common.DynamicMethodSpec;
import com.github.jonathanxd.codeapi.common.MethodInvokeSpec;
import com.github.jonathanxd.codeapi.common.MethodTypeSpec;
import com.github.jonathanxd.codeapi.factory.DynamicInvocationFactory;
import com.github.jonathanxd.codeapi.factory.Factories;
import com.github.jonathanxd.codeapi.factory.InvocationFactory;
import com.github.jonathanxd.codeapi.factory.VariableFactory;
import com.github.jonathanxd.codeapi.helper.ConcatHelper;
import com.github.jonathanxd.codeapi.literal.Literals;
import com.github.jonathanxd.codeapi.type.Generic;
import com.github.jonathanxd.codeapi.util.Alias;
import com.github.jonathanxd.iutils.annotation.Named;
import com.github.jonathanxd.iutils.collection.Collections3;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public class LocalLambdaBugTest {


    @SuppressWarnings("unchecked")
    @Test
    public void localLambdaBugTest() throws Throwable {
        MethodInvokeSpec ref = new MethodInvokeSpec(
                InvokeType.INVOKE_VIRTUAL,
                new MethodTypeSpec(
                        Alias.THIS.INSTANCE,
                        "lambda",
                        new TypeSpec(String.class, Collections3.listOf(Short.TYPE, String.class))
                )
        );

        TypeDeclaration decl = ClassDeclaration.Builder.builder()
                .modifiers(CodeModifier.PUBLIC)
                .name("com.MyClass")
                .methods(
                        MethodDeclaration.Builder.builder()
                                .modifiers(CodeModifier.PUBLIC)
                                .name("test")
                                .returnType(String.class)
                                .parameters(Factories.parameter(Short.TYPE, "s"))
                                .body(CodeSource.fromVarArgs(
                                        VariableFactory.variable(Function.class, "func",
                                                DynamicInvocationFactory.invokeDynamicLambda(
                                                        ref,
                                                        Access.THIS,
                                                        Collections3.listOf(Factories.accessVariable(Short.TYPE, "s")),
                                                        new MethodTypeSpec(Generic.type(Function.class).of(String.class),
                                                                "apply",
                                                                Factories.typeSpec(Object.class, Object.class)),
                                                        Factories.typeSpec(String.class, String.class)
                                                )),
                                        Factories.returnValue(String.class,
                                                Factories.cast(Object.class, String.class,
                                                InvocationFactory.invokeInterface(Function.class,
                                                Factories.accessVariable(Function.class, "func"),
                                                "apply",
                                                Factories.typeSpec(Object.class, Object.class),
                                                Collections3.listOf(Literals.STRING("A")))
                                        ))
                                ))
                                .build(),

                        MethodDeclaration.Builder.builder()
                                .modifiers(CodeModifier.PUBLIC)
                                .name("lambda")
                                .parameters(
                                        Factories.parameter(Short.TYPE, "s"),
                                        Factories.parameter(String.class, "value")
                                )
                                .returnType(String.class)
                                .body(CodeSource.fromVarArgs(
                                        Factories.returnValue(String.class,
                                                ConcatHelper.builder("Short: ").concat(InvocationFactory.invokeStatic(
                                                        String.class,
                                                        "valueOf",
                                                        Factories.typeSpec(String.class, Integer.TYPE),
                                                        Collections3.listOf(
                                                                Factories.cast(Short.TYPE, Integer.TYPE,
                                                                Factories.accessVariable(Short.TYPE, "s"))))

                                                ).concat(". S: ").concat(
                                                        Factories.accessVariable(String.class, "value")).build()
                                        )
                                ))
                                .build()
                )
                .build();
        //@Named("Instance") Class<?> define = CommonBytecodeTest.test(this.getClass(), decl, t -> t, v -> v, g -> {});
        BytecodeGenerator bytecodeGenerator = new BytecodeGenerator();

        List<? extends BytecodeClass> gen;
        try {
            gen = bytecodeGenerator.process(decl);
        } catch (ClassCheckException e) {
            gen = e.getBytecodeClasses();
        }

        ResultSaver.save(this.getClass(), gen);

        CodeClassLoader codeClassLoader = new CodeClassLoader();

        Class<?> define = codeClassLoader.define((Collection<BytecodeClass>) gen);

        Object test = define.getDeclaredMethod("test", Short.TYPE).invoke(define.newInstance(), (short) 55);

        Assert.assertEquals("Short: 55. S: A", test);

    }
}
