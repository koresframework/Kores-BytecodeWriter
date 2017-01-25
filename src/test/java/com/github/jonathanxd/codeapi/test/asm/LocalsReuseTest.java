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
import com.github.jonathanxd.codeapi.Types;
import com.github.jonathanxd.codeapi.base.ClassDeclaration;
import com.github.jonathanxd.codeapi.builder.ClassDeclarationBuilder;
import com.github.jonathanxd.codeapi.builder.MethodDeclarationBuilder;
import com.github.jonathanxd.codeapi.bytecode.BytecodeClass;
import com.github.jonathanxd.codeapi.bytecode.BytecodeOptions;
import com.github.jonathanxd.codeapi.bytecode.VisitLineType;
import com.github.jonathanxd.codeapi.bytecode.gen.BytecodeGenerator;
import com.github.jonathanxd.codeapi.common.CodeModifier;
import com.github.jonathanxd.codeapi.factory.VariableFactory;
import com.github.jonathanxd.codeapi.literal.Literals;

import org.junit.Assert;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;

import jdk.internal.org.objectweb.asm.Opcodes;

public class LocalsReuseTest {

    @SuppressWarnings("unchecked")
    @Test
    public void test() {
        ClassDeclaration declaration = ClassDeclarationBuilder.builder()
                .withModifiers(CodeModifier.PUBLIC)
                .withQualifiedName("codeapi.LocalsReuse")
                .withBody(CodeAPI.sourceOfParts(
                        MethodDeclarationBuilder.builder()
                                .withModifiers(CodeModifier.PUBLIC)
                                .withName("test")
                                .withReturnType(Types.INT)
                                .withParameters(CodeAPI.parameter(Types.BOOLEAN, "bool"))
                                .withBody(CodeAPI.sourceOfParts(
                                        CodeAPI.ifStatement(CodeAPI.checkTrue(CodeAPI.accessLocalVariable(Types.BOOLEAN, "bool")),
                                                CodeAPI.sourceOfParts(
                                                        VariableFactory.variable(Types.INT, "i", Literals.INT(10)),
                                                        CodeAPI.returnValue(Types.INT, CodeAPI.accessLocalVariable(Types.INT, "i"))
                                                ),
                                                CodeAPI.sourceOfParts(
                                                        VariableFactory.variable(Types.INT, "i", Literals.INT(17)),
                                                        CodeAPI.returnValue(Types.INT, CodeAPI.accessLocalVariable(Types.INT, "i"))
                                                ))
                                ))
                                .build()

                ))
                .build();

        BytecodeGenerator bytecodeGenerator = new BytecodeGenerator();

        bytecodeGenerator.getOptions().set(BytecodeOptions.VISIT_LINES, VisitLineType.FOLLOW_CODE_SOURCE);

        BytecodeClass[] gen = bytecodeGenerator.gen(declaration);

        ResultSaver.save(this.getClass(), gen);

        ClassReader cr = new ClassReader(gen[0].getBytecode());

        ClassNode cn = new ClassNode(Opcodes.ASM5);
        cr.accept(cn, 0);

        MethodNode node = ((List<MethodNode>) cn.methods).stream().filter(methodNode -> methodNode.name.equals("test")).findFirst().orElseThrow(NullPointerException::new);

        Assert.assertEquals("Locals reuse", 3, node.maxLocals);
    }

}
