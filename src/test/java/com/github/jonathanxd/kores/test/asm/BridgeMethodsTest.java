/*
 *      Kores-BytecodeWriter - Translates CodeAPI Structure to JVM Bytecode <https://github.com/JonathanxD/CodeAPI-BytecodeWriter>
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
import com.github.jonathanxd.kores.base.ForEachStatement;
import com.github.jonathanxd.kores.base.InterfaceDeclaration;
import com.github.jonathanxd.kores.base.MethodDeclaration;
import com.github.jonathanxd.kores.base.TypeDeclaration;
import com.github.jonathanxd.kores.bytecode.BytecodeClass;
import com.github.jonathanxd.kores.bytecode.BytecodeOptions;
import com.github.jonathanxd.kores.bytecode.processor.BytecodeGenerator;
import com.github.jonathanxd.kores.factory.InvocationFactory;
import com.github.jonathanxd.kores.factory.VariableFactory;
import com.github.jonathanxd.kores.generic.GenericSignature;
import com.github.jonathanxd.kores.helper.Predefined;
import com.github.jonathanxd.kores.literal.Literals;
import com.github.jonathanxd.kores.type.Generic;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import kotlin.collections.CollectionsKt;

import static com.github.jonathanxd.kores.factory.Factories.accessVariable;
import static com.github.jonathanxd.kores.factory.Factories.forEachIterable;
import static com.github.jonathanxd.kores.factory.Factories.parameter;
import static com.github.jonathanxd.kores.factory.Factories.typeSpec;

public class BridgeMethodsTest {

    @Test
    public void bridgeMethodTest() throws Throwable {

        BCLoader bcLoader = new BCLoader();

        TypeDeclaration itfDeclaration = InterfaceDeclaration.Builder.builder()
                .modifiers(KoresModifier.PUBLIC)
                .qualifiedName("com.AB")
                .genericSignature(GenericSignature.create(Generic.type("T").extends$(
                        Generic.type(Iterable.class).of(Generic.wildcard())
                )))
                .methods(
                        MethodDeclaration.Builder.builder()
                                .modifiers(KoresModifier.PUBLIC, KoresModifier.ABSTRACT)
                                .name("iterate")
                                .returnType(Types.VOID)
                                .parameters(parameter(Generic.type("T"), "iter"))
                                .body(Instructions.empty())
                                .build()
                )
                .build();

        BytecodeClass bytecodeClass = new BytecodeGenerator().process(itfDeclaration).get(0);

        byte[] bts = bytecodeClass.getBytecode();

        ResultSaver.save(this.getClass(), "Itf", bytecodeClass);

        bcLoader.define(itfDeclaration, bts);

        MethodDeclaration method;

        ForEachStatement forEachIterable = forEachIterable(VariableFactory.variable(Types.OBJECT, "obj"), accessVariable(List.class, "iter"),
                Instructions.fromPart(
                        Predefined.invokePrintln(
                                Predefined.invokeToString(accessVariable(Object.class, "obj"))
                        )
                ));

        TypeDeclaration typeDeclaration = ClassDeclaration.Builder.builder()
                .modifiers(KoresModifier.PUBLIC)
                .qualifiedName("com.bridgeTest")
                //.withImplementations(Generic.type(Helper.getJavaType(Iterate.class)).of(PredefinedTypes.LIST))
                .implementations(Generic.type(itfDeclaration).of(Types.LIST))
                .methods(
                        method = MethodDeclaration.Builder.builder()
                                .modifiers(KoresModifier.PUBLIC)
                                .returnType(Types.VOID)
                                .name("iterate")
                                .parameters(parameter(List.class, "iter"))
                                .body(Instructions.fromVarArgs(
                                        InvocationFactory.invokeInterface(List.class,
                                                accessVariable(List.class, "iter"),
                                                "get",
                                                typeSpec(Object.class, Integer.TYPE),
                                                CollectionsKt.listOf(Literals.INT(0))),
                                        forEachIterable
                                ))
                                .build()//,
                        //Helper.bridgeMethod(method, new FullMethodSpec(Iterate.class, Void.TYPE, "iterate", Iterable.class))
                        //Helper.bridgeMethod(method, new FullMethodSpec(itfDeclaration, PredefinedTypes.VOID, "iterate", Helper.getJavaType(Iterable.class)))
                )
                .build();

        BytecodeGenerator bytecodeGenerator = new BytecodeGenerator();

        bytecodeGenerator.getOptions().set(BytecodeOptions.GENERATE_BRIDGE_METHODS, true);

        byte[] gen = bytecodeGenerator.process(typeDeclaration).get(0).getBytecode();

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
