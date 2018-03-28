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

import com.github.jonathanxd.kores.Instruction;
import com.github.jonathanxd.kores.Instructions;
import com.github.jonathanxd.kores.MutableInstructions;
import com.github.jonathanxd.kores.Types;
import com.github.jonathanxd.kores.base.Access;
import com.github.jonathanxd.kores.base.CatchStatement;
import com.github.jonathanxd.kores.base.ClassDeclaration;
import com.github.jonathanxd.kores.base.KoresModifier;
import com.github.jonathanxd.kores.base.IfStatement;
import com.github.jonathanxd.kores.base.InvokeType;
import com.github.jonathanxd.kores.base.MethodDeclaration;
import com.github.jonathanxd.kores.base.TypeSpec;
import com.github.jonathanxd.kores.base.VariableDeclaration;
import com.github.jonathanxd.kores.bytecode.processor.BytecodeGenerator;
import com.github.jonathanxd.kores.factory.Factories;
import com.github.jonathanxd.kores.factory.InvocationFactory;
import com.github.jonathanxd.kores.factory.VariableFactory;
import com.github.jonathanxd.kores.helper.Predefined;
import com.github.jonathanxd.kores.literal.Literals;
import com.github.jonathanxd.kores.operator.Operators;
import com.github.jonathanxd.kores.type.KoresType;
import com.github.jonathanxd.kores.type.KoresTypes;
import com.github.jonathanxd.kores.type.LoadedKoresType;

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
public class KoresTestBytecode {

    public final String b = "9";

    private static Instructions rethrow(String variable) {
        MutableInstructions source = MutableInstructions.create();

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

    private static Instruction invokePrintlnMethod(Instruction varToPrint) {
        return InvocationFactory.invoke(
                InvokeType.INVOKE_VIRTUAL,
                PrintStream.class,
                Factories.accessField(System.class, Access.STATIC, PrintStream.class, "out"),
                "println",
                new TypeSpec(Types.VOID, CollectionsKt.listOf(Types.OBJECT)),
                CollectionsKt.listOf(varToPrint));
    }

    private static MethodDeclaration createMethod() {

        MutableInstructions methodSource = MutableInstructions.create();

        // Declare 'println' method
        MethodDeclaration codeMethod = MethodDeclaration.Builder.builder()
                .name("println")
                // Add parameter 'Object msg'
                .parameters(Collections.singletonList(Factories.parameter(Types.OBJECT, "msg")))
                // Add 'public static' modifier
                .modifiers(KoresModifier.PUBLIC, KoresModifier.STATIC)
                // Set 'void' return type
                .returnType(Types.VOID)
                // Set method source
                .body(methodSource)
                .build();

        LoadedKoresType<KoresTestBytecode> javaType = KoresTypes.getKoresType(KoresTestBytecode.class);

        methodSource.add(VariableFactory.variable(javaType, "test", InvocationFactory.invokeConstructor(javaType)));
        methodSource.add(Predefined.invokePrintln(
                Factories.accessField(javaType, Factories.accessVariable(javaType, "test"), Types.STRING, "b")
        ));

        // Create method body source
        MutableInstructions source = MutableInstructions.create();

        IfStatement ifBlock = Factories.ifStatement(
                Factories.check(Factories.accessVariable(Types.OBJECT, "msg"), Operators.NOT_EQUAL_TO, Literals.NULL),
                Instructions.fromPart(invokePrintlnMethod(Factories.accessVariable(Types.OBJECT, "msg")))
        );

        source.add(ifBlock);

        // 'Localization' of the field
        KoresType localization = KoresTypes.getKoresType(System.class);

        // Access field in 'localization'
        Instruction variable = Factories.accessField(localization, Access.STATIC, PrintStream.class, "out");

        // ref local field
        VariableDeclaration cf = VariableDeclaration.Builder.builder()
                .name("ref")
                // Type is Object
                .type(Types.OBJECT)
                // Value = variable (System.out)
                .value(variable)
                .build();

        source.add(cf);

        LoadedKoresType<IllegalStateException> exceptionType = KoresTypes.getKoresType(IllegalStateException.class);

        source.add(Factories.throwException(
                InvocationFactory.invokeConstructor(
                        exceptionType,
                        Factories.constructorTypeSpec(Types.STRING),
                        CollectionsKt.listOf(Literals.STRING("Error"))
                )
        ));

        // Access Local Variable 'msg'
        Instruction msgVar = Factories.accessVariable(Types.OBJECT, "msg");

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

        Instructions finallySource = Instructions.fromPart(Predefined.invokePrintln(Literals.STRING("Finally!")));

        CatchStatement catchStatement = Factories.catchStatement(catchExceptions, VariableFactory.variable(Types.THROWABLE, "thr"), rethrow("thr"));

        CatchStatement catchStatement2 = Factories.catchStatement(catchExceptions2, VariableFactory.variable(Types.THROWABLE, "tlr"), rethrow("tlr"));

        // Surround 'source' with 'try-catch'
        Instruction surround = Factories.tryStatement(source, Arrays.asList(catchStatement,
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
                .modifiers(KoresModifier.PUBLIC)
                // Declare methods
                .methods(createMethod())
                .build();

        BytecodeGenerator generator = new BytecodeGenerator();

        //generator.getOptions().set(BytecodeOptions.CHECK, Boolean.FALSE);
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
