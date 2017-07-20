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

import com.github.jonathanxd.codeapi.CodeInstruction;
import com.github.jonathanxd.codeapi.CodeSource;
import com.github.jonathanxd.codeapi.MutableCodeSource;
import com.github.jonathanxd.codeapi.Types;
import com.github.jonathanxd.codeapi.base.Access;
import com.github.jonathanxd.codeapi.base.CatchStatement;
import com.github.jonathanxd.codeapi.base.ClassDeclaration;
import com.github.jonathanxd.codeapi.base.CodeModifier;
import com.github.jonathanxd.codeapi.base.IfStatement;
import com.github.jonathanxd.codeapi.base.InvokeType;
import com.github.jonathanxd.codeapi.base.MethodDeclaration;
import com.github.jonathanxd.codeapi.base.TypeSpec;
import com.github.jonathanxd.codeapi.base.VariableDeclaration;
import com.github.jonathanxd.codeapi.bytecode.processor.BytecodeGenerator;
import com.github.jonathanxd.codeapi.factory.Factories;
import com.github.jonathanxd.codeapi.factory.InvocationFactory;
import com.github.jonathanxd.codeapi.factory.VariableFactory;
import com.github.jonathanxd.codeapi.helper.Predefined;
import com.github.jonathanxd.codeapi.literal.Literals;
import com.github.jonathanxd.codeapi.operator.Operators;
import com.github.jonathanxd.codeapi.type.CodeType;
import com.github.jonathanxd.codeapi.type.LoadedCodeType;
import com.github.jonathanxd.codeapi.util.CodeTypes;

import org.junit.Test;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import kotlin.collections.CollectionsKt;

@SuppressWarnings("Duplicates")
public class CodeAPITestBytecode {

    public final String b = "9";

    private static CodeSource rethrow(String variable) {
        MutableCodeSource source = MutableCodeSource.create();

        source.add(Predefined.invokePrintln(Literals.STRING("Rethrow from var '" + variable + "'!")));

        source.add(InvocationFactory.invoke(InvokeType.INVOKE_VIRTUAL, Throwable.class, Factories.accessVariable(Throwable.class, variable),
                "printStackTrace",
                new TypeSpec(Types.VOID),
                Collections.emptyList()));

        /*source.add(Helper.throwException(Helper.getJavaType(RuntimeException.class), new CodeArgument[]{
                new CodeArgument(Helper.accessLocalVariable(variable, Helper.getJavaType(Throwable.class)), false, Helper.getJavaType(Throwable.class))
        }));*/

        //throw new RuntimeException(e);

        return source;
    }

    private static CodeInstruction invokePrintlnMethod(CodeInstruction varToPrint) {
        return InvocationFactory.invoke(
                InvokeType.INVOKE_VIRTUAL,
                PrintStream.class,
                Factories.accessField(System.class, Access.STATIC, PrintStream.class, "out"),
                "println",
                new TypeSpec(Types.VOID, CollectionsKt.listOf(Types.OBJECT)),
                CollectionsKt.listOf(varToPrint));
    }

    private static MethodDeclaration createMethod() {

        MutableCodeSource methodSource = MutableCodeSource.create();

        // Declare 'println' method
        MethodDeclaration codeMethod = MethodDeclaration.Builder.builder()
                .name("println")
                // Add parameter 'Object msg'
                .parameters(Collections.singletonList(Factories.parameter(Types.OBJECT, "msg")))
                // Add 'public static' modifier
                .modifiers(CodeModifier.PUBLIC, CodeModifier.STATIC)
                // Set 'void' return type
                .returnType(Types.VOID)
                // Set method source
                .body(methodSource)
                .build();

        LoadedCodeType<CodeAPITestBytecode> javaType = CodeTypes.getCodeType(CodeAPITestBytecode.class);

        methodSource.add(VariableFactory.variable(javaType, "test", InvocationFactory.invokeConstructor(javaType)));
        methodSource.add(Predefined.invokePrintln(
                Factories.accessField(javaType, Factories.accessVariable(javaType, "test"), Types.STRING, "b")
        ));

        // Create method body source
        MutableCodeSource source = MutableCodeSource.create();

        IfStatement ifBlock = Factories.ifStatement(
                Factories.check(Factories.accessVariable(Types.OBJECT, "msg"), Operators.NOT_EQUAL_TO, Literals.NULL),
                CodeSource.fromPart(invokePrintlnMethod(Factories.accessVariable(Types.OBJECT, "msg")))
        );

        source.add(ifBlock);

        // 'Localization' of the field
        CodeType localization = CodeTypes.getCodeType(System.class);

        // Access field in 'localization'
        CodeInstruction variable = Factories.accessField(localization, Access.STATIC, PrintStream.class, "out");

        // ref local field
        VariableDeclaration cf = VariableDeclaration.Builder.builder()
                .name("ref")
                // Type is Object
                .type(Types.OBJECT)
                // Value = variable (System.out)
                .value(variable)
                .build();

        source.add(cf);

        LoadedCodeType<IllegalStateException> exceptionType = CodeTypes.getCodeType(IllegalStateException.class);

        source.add(Factories.throwException(
                InvocationFactory.invokeConstructor(
                        exceptionType,
                        Factories.constructorTypeSpec(Types.STRING),
                        CollectionsKt.listOf(Literals.STRING("Error"))
                )
        ));

        // Access Local Variable 'msg'
        CodeInstruction msgVar = Factories.accessVariable(Types.OBJECT, "msg");

        // Add Invocation of println method declared in 'System.out' ('variable')
        source.add(InvocationFactory.invoke(InvokeType.INVOKE_VIRTUAL, PrintStream.class, variable,
                "println",
                new TypeSpec(Types.VOID, CollectionsKt.listOf(Types.OBJECT)),
                // with argument 'msgVar' (Method msg parameter)
                CollectionsKt.listOf(msgVar))
        );


        List<Type> catchExceptions = Arrays.asList(IllegalArgumentException.class, IllegalStateException.class);

        List<Type> catchExceptions2 = Arrays.asList(IOException.class, ClassNotFoundException.class);

        // Finally block

        CodeSource finallySource = CodeSource.fromPart(Predefined.invokePrintln(Literals.STRING("Finally!")));

        CatchStatement catchStatement = Factories.catchStatement(catchExceptions, VariableFactory.variable(Types.THROWABLE, "thr"), rethrow("thr"));

        CatchStatement catchStatement2 = Factories.catchStatement(catchExceptions2, VariableFactory.variable(Types.THROWABLE, "tlr"), rethrow("tlr"));

        // Surround 'source' with 'try-catch'
        CodeInstruction surround = Factories.tryStatement(source, Arrays.asList(catchStatement,
                catchStatement2),
                finallySource);

        // Add body to method source
        methodSource.add(surround);

        return codeMethod;
    }

    @Test
    public void codeAPITest() {


        // Define a class
        ClassDeclaration codeClass = ClassDeclaration.Builder.builder()
                .qualifiedName("github.com." + this.getClass().getSimpleName())
                // Add 'public' modifier
                .modifiers(CodeModifier.PUBLIC)
                // Declare methods
                .methods(createMethod())
                .build();

        BytecodeGenerator generator = new BytecodeGenerator();

        byte[] bytes = generator.process(codeClass).get(0).getBytecode();

        ResultSaver.save(this.getClass(), bytes);

        Class<?> define = new BCLoader().define("github.com." + this.getClass().getSimpleName(), bytes);

        try {
            define.newInstance();

            MethodHandles.Lookup lookup = MethodHandles.publicLookup();

            MethodHandle println = lookup.findStatic(define, "println", java.lang.invoke.MethodType.methodType(Void.TYPE, Object.class));

            println.invoke((Object) "nano");
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    private static final class BCLoader extends ClassLoader {

        public Class<?> define(String name, byte[] bytes) {
            return super.defineClass(name, bytes, 0, bytes.length);
        }
    }
    // TODO: LOW LEVEL TASK -> UPDATE

}
