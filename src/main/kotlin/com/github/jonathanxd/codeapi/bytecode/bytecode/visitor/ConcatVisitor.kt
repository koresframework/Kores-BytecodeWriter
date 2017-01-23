package com.github.jonathanxd.codeapi.gen.bytecode.visitor

import com.github.jonathanxd.codeapi.CodeAPI
import com.github.jonathanxd.codeapi.common.MVData
import com.github.jonathanxd.codeapi.gen.BytecodeClass
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.gen.visit.VoidVisitor
import com.github.jonathanxd.codeapi.interfaces.Concat
import com.github.jonathanxd.codeapi.interfaces.MethodInvocation
import com.github.jonathanxd.iutils.data.MapData

import com.github.jonathanxd.codeapi.helper.PredefinedTypes.OBJECT
import com.github.jonathanxd.codeapi.helper.PredefinedTypes.STRING
import com.github.jonathanxd.codeapi.helper.PredefinedTypes.STRING_BUILDER


object ConcatVisitor : VoidVisitor<Concat, BytecodeClass, MVData> {

    override fun voidVisit(t: Concat, extraData: MapData, visitorGenerator: VisitorGenerator<BytecodeClass>, additional: MVData) {
        val concatenations = t.concatenations

        val first = if (concatenations.isEmpty()) null else concatenations[0]

        if (first != null) {

            if (concatenations.size == 1) {

                visitorGenerator.generateTo(first.javaClass, first, extraData, additional)

            } else if (concatenations.size == 2) {

                val stringConcat = CodeAPI.invokeVirtual(String::class.java, first, "concat",
                        CodeAPI.typeSpec(String::class.java, String::class.java),
                        CodeAPI.argument(concatenations[1]))

                visitorGenerator.generateTo(MethodInvocation::class.java, stringConcat, extraData, additional)
            } else {

                var strBuilder = CodeAPI.invokeConstructor(StringBuilder::class.java, CodeAPI.argument(first, String::class.java))

                (1..concatenations.size - 1)
                        .map { concatenations[it] }
                        .forEach { strBuilder = CodeAPI.invokeVirtual(STRING_BUILDER, strBuilder, "append", CodeAPI.typeSpec(STRING_BUILDER, STRING), CodeAPI.argument(it)) }

                strBuilder = CodeAPI.invokeVirtual(OBJECT, strBuilder, "toString", CodeAPI.typeSpec(STRING))

                visitorGenerator.generateTo(MethodInvocation::class.java, strBuilder, extraData, additional)
            }
        } else {
            // If the concatenations is empty
            // It is better to CodeAPI (less things to process), and is better to JVM.
            additional.methodVisitor.visitLdcInsn("")
        }

    }

}