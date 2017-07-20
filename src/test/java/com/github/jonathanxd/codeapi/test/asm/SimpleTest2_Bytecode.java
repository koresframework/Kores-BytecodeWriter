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
import com.github.jonathanxd.codeapi.Types;
import com.github.jonathanxd.codeapi.base.Access;
import com.github.jonathanxd.codeapi.base.ClassDeclaration;
import com.github.jonathanxd.codeapi.base.CodeModifier;
import com.github.jonathanxd.codeapi.base.ConstructorDeclaration;
import com.github.jonathanxd.codeapi.base.FieldDeclaration;
import com.github.jonathanxd.codeapi.base.InvokeType;
import com.github.jonathanxd.codeapi.bytecode.BytecodeClass;
import com.github.jonathanxd.codeapi.bytecode.processor.BytecodeGenerator;
import com.github.jonathanxd.codeapi.helper.Predefined;
import com.github.jonathanxd.codeapi.literal.Literals;
import com.github.jonathanxd.codeapi.operator.Operators;
import com.github.jonathanxd.codeapi.util.Alias;

import org.junit.Test;

import java.io.PrintStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.Collections;

import static com.github.jonathanxd.codeapi.factory.Factories.accessStaticField;
import static com.github.jonathanxd.codeapi.factory.Factories.accessVariable;
import static com.github.jonathanxd.codeapi.factory.Factories.cast;
import static com.github.jonathanxd.codeapi.factory.Factories.check;
import static com.github.jonathanxd.codeapi.factory.Factories.ifExprs;
import static com.github.jonathanxd.codeapi.factory.Factories.ifStatement;
import static com.github.jonathanxd.codeapi.factory.Factories.parameter;
import static com.github.jonathanxd.codeapi.factory.Factories.setFieldValue;
import static com.github.jonathanxd.codeapi.factory.Factories.typeSpec;
import static com.github.jonathanxd.codeapi.factory.InvocationFactory.invoke;

public class SimpleTest2_Bytecode {

    // antes da refatoração: ~890ms
    @Test
    public void simpleTest() throws NoSuchFieldException {
        // Crio uma classe com nome de SimpleTest2_bytecode
        ClassDeclaration codeClass = ClassDeclaration.Builder.builder()
                // Adiciona o modifier publico
                .modifiers(CodeModifier.PUBLIC)
                // Defino o nome
                .name("me.jonathanscripter.codeapi.test.SimpleTest2_bytecode")
                // Defino o super tipo
                .superClass(Types.OBJECT)
                // Define as fields
                .fields(
                        // Cria uma field (campo)
                        FieldDeclaration.Builder.builder()
                                // Adiciona os modificadores public final
                                .modifiers(CodeModifier.PUBLIC, CodeModifier.FINAL)
                                // Define o tipo da field como String
                                .type(String.class)
                                // Defino o nome
                                .name("myField")
                                .build()
                )
                .constructors(
                        // Cria um construtor para a classe 'codeClass' que criamos. CodeConstructor recebe CodeType
                        // como parametro
                        ConstructorDeclaration.Builder.builder()
                                // Adiciona o modificador publico
                                .modifiers(CodeModifier.PUBLIC)
                                // Adiciona um parametro 'myField' do tipo String ao construtor
                                .parameters(parameter(String.class, "myField"))
                                .body(
                                        // Define o corpo (codigo fonte) do metodo
                                        // Classe Factories é usada pelo menos em 70% do código, ela ajuda em tarefas comuns.
                                        CodeSource.fromVarArgs(
                                                setFieldValue(Alias.THIS.INSTANCE, Access.THIS, String.class, "myField", accessVariable(String.class, "myField")),
                                                ifStatement(
                                                        check(accessVariable(String.class, "myField"), Operators.NOT_EQUAL_TO, Literals.NULL),
                                                        CodeSource.fromVarArgs(
                                                                invoke(InvokeType.INVOKE_VIRTUAL, PrintStream.class,
                                                                        accessStaticField(System.class, PrintStream.class, "out"),
                                                                        "println",
                                                                        typeSpec(Types.VOID, Types.STRING),
                                                                        Collections.singletonList(accessVariable(String.class, "myField"))
                                                                )), CodeSource.fromVarArgs(
                                                                invoke(
                                                                        InvokeType.INVOKE_VIRTUAL,
                                                                        PrintStream.class,
                                                                        accessStaticField(System.class, PrintStream.class, "out"),
                                                                        "println",
                                                                        typeSpec(Types.VOID, Types.STRING),
                                                                        Collections.singletonList(
                                                                                cast(String.class, String.class, Literals.STRING("NULL VALUE"))
                                                                        ))
                                                        )),
                                                ifStatement(
                                                        ifExprs(check(Literals.LONG(5894567987L), Operators.LESS_THAN, Literals.LONG(89859845678798L))),
                                                        CodeSource.fromPart(Predefined.invokePrintlnStr(Literals.STRING("First < Second"))),
                                                        CodeSource.fromPart(Predefined.invokePrintlnStr(Literals.STRING("First >= Second")))
                                                )
                                        ))
                                .build()
                )
                // Construo uma instancia
                .build();


        BytecodeGenerator bytecodeGenerator = new BytecodeGenerator();
        BytecodeClass bytecodeClass = bytecodeGenerator.process(codeClass).get(0);

        byte[] bytes = bytecodeClass.getBytecode();

        ResultSaver.save(this.getClass(), bytecodeClass);

        BCLoader bcLoader = new BCLoader();

        Class<?> define = bcLoader.define("me.jonathanscripter.codeapi.test.SimpleTest2_bytecode", bytes);

        try {
            Object o = define.getConstructor(String.class).newInstance((String) null);

            MethodHandle getMyField = MethodHandles.publicLookup().findGetter(define, "myField", String.class).bindTo(o);

            System.out.println("Field value = " + (String) getMyField.invoke());
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }

    }

    private static final class BCLoader extends ClassLoader {

        public Class<?> define(String name, byte[] bytes) {
            return super.defineClass(name, bytes, 0, bytes.length);
        }
    }

}
