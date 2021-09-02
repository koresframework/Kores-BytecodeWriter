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
package com.koresframework.kores.test.asm

import com.koresframework.kores.Instructions
import com.koresframework.kores.MutableInstructions
import com.koresframework.kores.Types
import com.koresframework.kores.base.*
import com.koresframework.kores.factory.variable
import com.koresframework.kores.generic.GenericSignature
import com.koresframework.kores.literal.Literals
import com.koresframework.kores.type.Generic
import com.koresframework.kores.type.bindedDefaultResolver
import com.koresframework.kores.type.koresType
import com.koresframework.kores.type.typeOf
import org.junit.Test
import java.lang.reflect.Type
import java.util.function.Supplier

/**
 * Created by jonathan on 05/07/16.
 */
class GenericVariableTest {
    @Test
    fun test() {
        val `$`: TypeDeclaration = ClassDeclaration.Builder.builder()
            .specifiedName("com.Generic")
            .constructors(ConstructorDeclaration.Builder.builder()
                .body(MutableInstructions.create(listOf(
                    variable(Generic.type(Types.INT), "hell", Literals.INT(9))
                )))
                .build()
            )
            .build()
        CommonBytecodeTest.test(this.javaClass, `$`)
    }


}