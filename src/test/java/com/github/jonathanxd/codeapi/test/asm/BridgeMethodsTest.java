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
import com.github.jonathanxd.codeapi.base.MethodDeclaration;
import com.github.jonathanxd.codeapi.base.TypeDeclaration;
import com.github.jonathanxd.codeapi.bytecode.BytecodeOptions;
import com.github.jonathanxd.codeapi.bytecode.gen.BytecodeGenerator;
import com.github.jonathanxd.codeapi.common.CodeModifier;
import com.github.jonathanxd.codeapi.factory.VariableFactory;
import com.github.jonathanxd.codeapi.generic.GenericSignature;
import com.github.jonathanxd.codeapi.helper.Predefined;
import com.github.jonathanxd.codeapi.literal.Literals;
import com.github.jonathanxd.codeapi.type.Generic;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import kotlin.collections.CollectionsKt;

public class BridgeMethodsTest {

    @Test
    public void bridgeMethodTest() throws Throwable {

        BCLoader bcLoader = new BCLoader();

        TypeDeclaration itfDeclaration = CodeAPI.anInterfaceBuilder()
                .withModifiers(CodeModifier.PUBLIC)
                .withQualifiedName("com.AB")
                .withGenericSignature(GenericSignature.create(Generic.type("T").extends$(
                        Generic.type(CodeAPI.getJavaType(Iterable.class)).of(Generic.wildcard())
                )))
                .withBody(CodeAPI.sourceOfParts(
                        CodeAPI.methodBuilder()
                                .withModifiers(CodeModifier.PUBLIC, CodeModifier.ABSTRACT)
                                .withName("iterate")
                                .withReturnType(Types.VOID)
                                .withParameters(CodeAPI.parameter(Generic.type("T"), "iter"))
                                .withBody(CodeSource.empty())
                                .build()
                ))
                .build();

        byte[] bts = new BytecodeGenerator().gen(itfDeclaration)[0].getBytecode();

        ResultSaver.save(this.getClass(), "Itf", bts);

        bcLoader.define(itfDeclaration, bts);

        MethodDeclaration method;

        TypeDeclaration typeDeclaration = CodeAPI.aClassBuilder()
                .withModifiers(CodeModifier.PUBLIC)
                .withQualifiedName("com.bridgeTest")
                //.withImplementations(Generic.type(Helper.getJavaType(Iterate.class)).of(PredefinedTypes.LIST))
                .withImplementations(Generic.type(itfDeclaration).of(Types.LIST))
                .withBody(CodeAPI.sourceOfParts(
                        method = CodeAPI.methodBuilder()
                                .withModifiers(CodeModifier.PUBLIC)
                                .withReturnType(Types.VOID)
                                .withName("iterate")
                                .withParameters(CodeAPI.parameter(List.class, "iter"))
                                .withBody(CodeAPI.sourceOfParts(
                                        CodeAPI.invokeInterface(List.class,
                                                CodeAPI.accessLocalVariable(List.class, "iter"),
                                                "get",
                                                CodeAPI.typeSpec(Object.class, Integer.TYPE),
                                                CollectionsKt.listOf(Literals.INT(0))),
                                        CodeAPI.forEachIterable(VariableFactory.variable(Types.OBJECT, "obj"), CodeAPI.accessLocalVariable(List.class, "iter"),
                                                CodeAPI.sourceOfParts(
                                                        Predefined.invokePrintln(
                                                                Predefined.toString(CodeAPI.accessLocalVariable(Object.class, "obj"))
                                                        )
                                                ))
                                ))
                                .build()//,
                        //Helper.bridgeMethod(method, new FullMethodSpec(Iterate.class, Void.TYPE, "iterate", Iterable.class))
                        //Helper.bridgeMethod(method, new FullMethodSpec(itfDeclaration, PredefinedTypes.VOID, "iterate", Helper.getJavaType(Iterable.class)))
                ))
                .build();

        CodeSource codeSource = CodeAPI.sourceOfParts(typeDeclaration);

        BytecodeGenerator bytecodeGenerator = new BytecodeGenerator();

        bytecodeGenerator.getOptions().set(BytecodeOptions.GENERATE_BRIDGE_METHODS, true);

        byte[] gen = bytecodeGenerator.gen(codeSource)[0].getBytecode();

        ResultSaver.save(this.getClass(), gen);

        Class<?> define = bcLoader.define(typeDeclaration, gen);

        //Iterate<List<?>> o = (Iterate<List<?>>) define.newInstance();
        Object o = define.newInstance();

        List<Object> iterable = new ArrayList<>();

        iterable.add("A");

        //o.iterate(iterable);
        define.getDeclaredMethod("iterate", Iterable.class).invoke(o, iterable);

    }

    public interface Iterate<T extends Iterable<?>> {
        void iterate(T iter);
    }

    static class B implements Iterate<List<?>> {
        @Override
        public void iterate(List<?> iter) {
            Iterator<?> iterator = iter.iterator();

            while (iterator.hasNext()) {
                System.out.println(iterator.next());
            }
        }
    }
}
