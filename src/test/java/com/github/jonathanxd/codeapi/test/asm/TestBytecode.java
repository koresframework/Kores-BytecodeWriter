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
import com.github.jonathanxd.codeapi.CodePart;
import com.github.jonathanxd.codeapi.MutableCodeSource;
import com.github.jonathanxd.codeapi.Types;
import com.github.jonathanxd.codeapi.base.ClassDeclaration;
import com.github.jonathanxd.codeapi.base.ConstructorDeclaration;
import com.github.jonathanxd.codeapi.base.FieldDeclaration;
import com.github.jonathanxd.codeapi.base.MethodDeclaration;
import com.github.jonathanxd.codeapi.builder.ClassDeclarationBuilder;
import com.github.jonathanxd.codeapi.builder.ConstructorDeclarationBuilder;
import com.github.jonathanxd.codeapi.builder.MethodDeclarationBuilder;
import com.github.jonathanxd.codeapi.bytecode.gen.BytecodeGenerator;
import com.github.jonathanxd.codeapi.common.CodeArgument;
import com.github.jonathanxd.codeapi.common.CodeModifier;
import com.github.jonathanxd.codeapi.common.CodeParameter;
import com.github.jonathanxd.codeapi.common.InvokeType;
import com.github.jonathanxd.codeapi.factory.FieldFactory;
import com.github.jonathanxd.codeapi.factory.VariableFactory;
import com.github.jonathanxd.codeapi.helper.Predefined;
import com.github.jonathanxd.codeapi.literal.Literals;
import com.github.jonathanxd.codeapi.operator.Operators;

import org.junit.Test;

import java.io.PrintStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.EnumSet;

import static java.util.Collections.singletonList;

/**
 * Created by jonathan on 03/06/16.
 */
@SuppressWarnings("Duplicates")
public class TestBytecode {
    public static CodePart invokePrintln(CodeArgument toPrint) {
        return CodeAPI.invoke(InvokeType.INVOKE_VIRTUAL, CodeAPI.getJavaType(PrintStream.class),
                CodeAPI.accessStaticField(CodeAPI.getJavaType(System.class), CodeAPI.getJavaType(PrintStream.class), "out"),
                "println",
                CodeAPI.typeSpec(Types.VOID, Types.OBJECT),
                singletonList(toPrint));
    }

    @Test
    public void testBytecode() {

        MutableCodeSource codeSource = new MutableCodeSource();

        MutableCodeSource clSource = new MutableCodeSource();

        ClassDeclaration codeClass = ClassDeclarationBuilder.builder()
                .withModifiers(CodeModifier.PUBLIC)
                .withQualifiedName("fullName." + this.getClass().getSimpleName())
                .withSuperClass(Types.OBJECT)
                .withBody(clSource)
                .build();
        // TODO
        FieldDeclaration codeField = FieldFactory.field(
                EnumSet.of(CodeModifier.PUBLIC, CodeModifier.FINAL),
                Types.STRING,
                "FIELD",
                Literals.STRING("AVD")
        );

        FieldDeclaration codeField2 = FieldFactory.field(
                EnumSet.of(CodeModifier.PUBLIC, CodeModifier.FINAL),
                Types.INT,
                "n",
                Literals.INT(15)
        );

        clSource.add(codeField);
        clSource.add(codeField2);

        CodePart invokeTest = CodeAPI.invoke(InvokeType.INVOKE_VIRTUAL, CodeAPI.getJavaType(PrintStream.class),
                CodeAPI.accessStaticField(CodeAPI.getJavaType(System.class), CodeAPI.getJavaType(PrintStream.class), "out"),
                "println",
                CodeAPI.typeSpec(Types.VOID, Types.OBJECT),
                Collections.singletonList(new CodeArgument(Literals.STRING("Hello"))));

        CodePart invokeTest2 = CodeAPI.invoke(InvokeType.INVOKE_VIRTUAL, codeClass,
                CodeAPI.accessThis(),
                "printIt",
                CodeAPI.typeSpec(Types.VOID, Types.OBJECT),

                Collections.singletonList(new CodeArgument(Literals.STRING("Oi"))));

        ConstructorDeclaration codeConstructor = ConstructorDeclarationBuilder.builder()
                .withModifiers(CodeModifier.PUBLIC)
                .withBody(CodeAPI.source(invokeTest, invokeTest2))
                .build();

        clSource.add(codeConstructor);

        clSource.add(makeCM());
        clSource.add(makeCM2());

        codeSource.add(codeClass);

        BytecodeGenerator bytecodeGenerator = new BytecodeGenerator();

        byte[] gen = bytecodeGenerator.gen(codeSource)[0].getBytecode();

        ResultSaver.save(this.getClass(), gen);

        BCLoader bcLoader = new BCLoader();

        Class<?> define = bcLoader.define("fullName." + this.getClass().getSimpleName(), gen);

        System.out.println("Class -> " + Modifier.toString(define.getModifiers()) + " " + define);

        Object o;
        try {
            o = define.newInstance();
            MethodHandles.Lookup lookup = MethodHandles.publicLookup();

            MethodHandle printIt = lookup.findVirtual(define, "printIt", MethodType.methodType(Void.TYPE, Object.class)).bindTo(o);

            try {
                System.out.println("NAO DEVE FALAR HELLO");
                printIt.invoke((Object) null);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }

            MethodHandle check = lookup.findVirtual(define, "check", MethodType.methodType(Boolean.TYPE, Integer.TYPE)).bindTo(o);

            try {
                System.out.println("CHECK NINE");
                boolean invoke = (boolean) check.invoke(9);

                System.out.println("Invoke = " + invoke);
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

    public static MethodDeclaration makeCM() {
        MutableCodeSource methodSource = new MutableCodeSource();

        MethodDeclaration codeMethod = MethodDeclarationBuilder.builder()
                .withModifiers(CodeModifier.PUBLIC)
                .withName("printIt")
                .withParameters(new CodeParameter(Types.OBJECT, "n"))
                .withReturnType(Types.VOID)
                .withBody(methodSource)
                .build();

        methodSource.add(CodeAPI.ifStatement(
                CodeAPI.checkNotNull(CodeAPI.accessLocalVariable(Object.class, "n")),
                CodeAPI.source(invokePrintln(new CodeArgument(Literals.STRING("Hello :D"))))
                )
        );

        methodSource.add(VariableFactory.variable(Types.STRING, "dingdong", Literals.STRING("DingDong")));

        methodSource.add(Predefined.invokePrintln(new CodeArgument(CodeAPI.accessLocalVariable(String.class, "dingdong"))));

        methodSource.add(CodeAPI.invoke(InvokeType.INVOKE_VIRTUAL, PrintStream.class,
                CodeAPI.accessStaticField(System.class, PrintStream.class, "out"),
                "println",
                CodeAPI.typeSpec(Types.VOID, Types.OBJECT),
                singletonList(new CodeArgument(CodeAPI.accessLocalVariable(Types.OBJECT, "n")))));


        return codeMethod;
    }

    public MethodDeclaration makeCM2() {
        MutableCodeSource methodSource = new MutableCodeSource();

        MethodDeclaration codeMethod = MethodDeclarationBuilder.builder()
                .withName("check")
                .withModifiers(CodeModifier.PUBLIC)
                .withParameters(new CodeParameter(Types.INT, "x"))
                .withReturnType(Types.BOOLEAN)
                .withBody(methodSource)
                .build();

        methodSource.add(CodeAPI.ifStatement(
                CodeAPI.ifExprs(
                        CodeAPI.check(CodeAPI.accessLocalVariable(Types.INT, "x"), Operators.EQUAL_TO, Literals.INT(9)),
                        Operators.OR,
                        CodeAPI.check(CodeAPI.accessLocalVariable(Types.INT, "x"), Operators.EQUAL_TO, Literals.INT(7))
                ),
                CodeAPI.source(
                        CodeAPI.returnValue(Types.INT, Literals.INT(0))
                )));

        methodSource.add(Predefined.invokePrintln(
                new CodeArgument(CodeAPI.accessLocalVariable(Types.INT, "x"))
        ));

        methodSource.add(CodeAPI.returnValue(Types.INT, Literals.INT(1)));

        return codeMethod;
    }

    private static final class BCLoader extends ClassLoader {

        public Class<?> define(String name, byte[] bytes) {
            return super.defineClass(name, bytes, 0, bytes.length);
        }
    }
}
