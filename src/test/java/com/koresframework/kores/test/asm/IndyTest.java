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
import com.koresframework.kores.*;
import com.koresframework.kores.base.*;
import com.koresframework.kores.common.*;
import com.koresframework.kores.factory.DynamicInvocationFactory;
import com.koresframework.kores.factory.Factories;
import com.koresframework.kores.factory.InvocationFactory;
import com.koresframework.kores.factory.VariableFactory;
import com.koresframework.kores.helper.Predefined;
import com.koresframework.kores.literal.Literals;
import com.koresframework.kores.operator.Operators;
import com.koresframework.kores.type.KoresType;
import com.koresframework.kores.type.KoresTypes;
import com.koresframework.kores.type.TypeRef;
import org.junit.Assert;
import org.junit.Test;

import java.io.PrintStream;
import java.lang.invoke.*;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.UnaryOperator;

import static java.util.Collections.singletonList;

public class IndyTest {

    private static final AtomicInteger BOOTSTRAP_CALLS = new AtomicInteger();
    private static final AtomicInteger CONST_BOOTSTRAP_CALLS = new AtomicInteger();

    @Test
    public void indyTest() {
        TypeDeclaration $ = IndyTest.$();

        @Named("Instance") Class<?> define = CommonBytecodeTest.test(this.getClass(), $, UnaryOperator.identity(), aClass -> aClass);

        Object o;
        try {
            o = define.newInstance();
            MethodHandles.Lookup lookup = MethodHandles.publicLookup();

            MethodHandle check = lookup.findVirtual(define, "check", MethodType.methodType(String.class, Integer.TYPE)).bindTo(o);

            try {
                String invoke = (String) check.invoke(9);
                String invoke2 = (String) check.invoke(9);

                Assert.assertEquals("MAGIC", invoke);
                Assert.assertEquals("MAGIC", invoke2);
                Assert.assertEquals(2, BOOTSTRAP_CALLS.get());
                Assert.assertEquals(1, CONST_BOOTSTRAP_CALLS.get());
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }

        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

        for (Field field : define.getDeclaredFields()) {
            try {
                System.out.println("Field -> " + field + " = " + field.get(o));
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static final IndyTest INSTANCE = new IndyTest();
    public static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    public static final MethodTypeSpec BOOTSTRAP_SPEC = new MethodTypeSpec(
            KoresTypes.getKoresType(IndyTest.class),
            "myBootstrap",
            Factories.typeSpec(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, Object[].class)
    );
    public static final MethodTypeSpec CONSTANT_BOOTSTRAP_SPEC = new MethodTypeSpec(
            KoresTypes.getKoresType(IndyTest.class),
            "constantBootstrap",
            Factories.typeSpec(String.class, MethodHandles.Lookup.class, String.class, Class.class, Object[].class)
    );

    public static final MethodHandle FALLBACK;
    public static final MethodHandle CONST_FALLBACK;

    static {
        try {
            FALLBACK = LOOKUP.findStatic(
                    IndyTest.class,
                    "fallback",
                    MethodType.methodType(Object.class, IndyTest.MyCallSite.class, Object[].class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        try {
            CONST_FALLBACK = LOOKUP.findStatic(
                    IndyTest.class,
                    "constFallback", MethodType.methodType(String.class, String.class, Object[].class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static KoresPart invokePrintln(Instruction toPrint) {
        return InvocationFactory.invoke(InvokeType.INVOKE_VIRTUAL, KoresTypes.getKoresType(PrintStream.class),
                Factories.accessStaticField(KoresTypes.getKoresType(System.class), KoresTypes.getKoresType(PrintStream.class), "out"),
                "println",
                Factories.voidTypeSpec(Types.OBJECT),
                Collections.singletonList(toPrint));
    }

    public static void bmp(String a, String b) {
        System.out.println("A = " + a + ", B = " + b);
    }

    public static CallSite myBootstrap(MethodHandles.Lookup caller, String name,
                                       MethodType type, Object... parameters) throws Throwable {
        BOOTSTRAP_CALLS.incrementAndGet();
        IndyTest.MyCallSite myCallSite = new IndyTest.MyCallSite(caller, name, parameters);

        MethodHandle methodHandle = FALLBACK.bindTo(myCallSite)
                .asCollector(Object[].class, type.parameterCount())
                .asType(type);

        return new ConstantCallSite(methodHandle);
    }

    public static String constantBootstrap(MethodHandles.Lookup caller, String name,
                                       Class<?> type, Object... parameters) throws Throwable {
        CONST_BOOTSTRAP_CALLS.incrementAndGet();
        if ("some".equals(name)) {
            return "MAGIC";
        }
        return "NO_MAGIC";
    }

    public static Object fallback(IndyTest.MyCallSite callSite, Object[] args) throws Throwable {
        return callSite.parameters[0];
    }

    public static String constFallback(String constantName, Object[] args) throws Throwable {
        if ("some".equals(constantName)) {
            return "MAGIC";
        }
        return "NO_MAGIC";
    }

    public static TypeDeclaration $() {
        MutableInstructions codeSource = MutableInstructions.create();

        TypeRef typeRef = new TypeRef("fullName." + IndyTest.class.getSimpleName() + "_Generated");


        ClassDeclaration codeClass = ClassDeclaration.Builder.Companion.builder()
                .modifiers(KoresModifier.PUBLIC)
                .base(typeRef)
                .superClass(Types.OBJECT)
                .fields(FieldDeclaration.Builder.builder()
                                .modifiers(KoresModifier.PUBLIC, KoresModifier.FINAL)
                                .type(Types.STRING)
                                .name("FIELD")
                                .value(Literals.STRING("AVD"))
                                .build(),
                        FieldDeclaration.Builder.builder()
                                .modifiers(KoresModifier.PUBLIC, KoresModifier.FINAL)
                                .type(Types.INT)
                                .name("n")
                                .value(Literals.INT(15))
                                .build()
                )
                .constructors(ConstructorDeclaration.Builder.Companion.builder()
                        .modifiers(KoresModifier.PUBLIC)
                        .body(Instructions.empty())
                        .build())
                .methods(makeCM2(typeRef))
                .build();

        return codeClass;
    }

    public static MethodDeclaration makeCM2(TypeRef typeDeclaration) {
        MutableInstructions methodSource = MutableInstructions.create();

        MethodDeclaration codeMethod = MethodDeclaration.Builder.builder()
                .modifiers(KoresModifier.PUBLIC)
                .name("check")
                .returnType(Types.STRING)
                .parameters(Factories.parameter(Types.INT, "x"))
                .body(methodSource)
                .build();

        methodSource.add(Predefined.invokePrintln(Literals.STRING("Invoke Dynamic Bootstrap ->")));
        DynamicConstantSpec someConst = new DynamicConstantSpec(
                "some",
                Types.STRING,
                new MethodInvokeHandleSpec(DynamicInvokeType.INVOKE_STATIC, CONSTANT_BOOTSTRAP_SPEC),
                Collections.singletonList("value")
        );

        InvokeDynamic bootstrapInvocation = DynamicInvocationFactory.invokeDynamic(
                new MethodInvokeSpec(InvokeType.INVOKE_STATIC, BOOTSTRAP_SPEC),
                new DynamicMethodSpec("helloWorld", Factories.typeSpec(Types.STRING, Types.STRING),
                        singletonList(Literals.STRING("World"))),
                Collections.singletonList(
                        someConst
                )
        );


        methodSource.add(bootstrapInvocation);
        methodSource.add(Factories.returnValue(Types.STRING, bootstrapInvocation));

        return codeMethod;
    }

    public void helloWorld(String name) {
        System.out.println("Hello, " + name);
    }

    @Test
    public void test() {
        $();
    }

    /*public static class MyCallSite extends ConstantCallSite {

        final MethodHandles.Lookup callerLookup;
        final String name;
        final Object[] parameters;

        MyCallSite(MethodHandles.Lookup callerLookup, MethodHandle target, String name, Object[] parameters) {
            super(target);
            this.callerLookup = callerLookup;
            this.name = name;
            this.parameters = parameters;
        }


    }
*/
    public static class MyCallSite {

        final MethodHandles.Lookup callerLookup;
        final String name;
        final Object[] parameters;

        MyCallSite(MethodHandles.Lookup callerLookup, String name, Object[] parameters) {
            this.callerLookup = callerLookup;
            this.name = name;
            this.parameters = parameters;
        }


    }



    private static final class BCLoader extends ClassLoader {

        protected BCLoader(ClassLoader parent) {
            super(parent);
        }

        public Class<?> define(String name, byte[] bytes) {
            return super.defineClass(name, bytes, 0, bytes.length);
        }
    }
}
