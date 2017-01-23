package com.github.jonathanxd.codeapi.gen.bytecode.visitor

import com.github.jonathanxd.codeapi.CodeAPI
import com.github.jonathanxd.codeapi.CodeSource
import com.github.jonathanxd.codeapi.common.CodeParameter
import com.github.jonathanxd.codeapi.gen.BytecodeClass
import com.github.jonathanxd.codeapi.gen.visit.Visitor
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.interfaces.*
import com.github.jonathanxd.codeapi.util.gen.ConstructorUtil
import com.github.jonathanxd.codeapi.util.source.CodeSourceUtil
import com.github.jonathanxd.iutils.data.MapData

object ConstructorVisitor : Visitor<ConstructorDeclaration, BytecodeClass, Any?> {

    override fun visit(t: ConstructorDeclaration, extraData: MapData, visitorGenerator: VisitorGenerator<BytecodeClass>, additional: Any?): Array<BytecodeClass> {
        var constructorDeclaration: ConstructorDeclaration = t

        val outerFields = extraData.getAllAsList(TypeVisitor.OUTER_FIELD_REPRESENTATION)

        if (!outerFields.isEmpty()) {
            val typeDeclaration = extraData.getRequired(TypeVisitor.CODE_TYPE_REPRESENTATION, "Cannot find CodeClass. Register 'TypeVisitor.CODE_TYPE_REPRESENTATION'!")

            val parameters = ArrayList<CodeParameter>(constructorDeclaration.parameters)
            var source = CodeSource.fromIterable(constructorDeclaration.body.orElse(CodeSource.empty()))

            for (outerField in outerFields) {
                parameters.add(0, CodeParameter(outerField.name, outerField.variableType))

                source = CodeSourceUtil.insertAfterOrEnd(
                        { part -> part is MethodInvocation && ConstructorUtil.isInitForThat(typeDeclaration, part) },
                        CodeAPI.sourceOfParts(
                                CodeAPI.setThisField(outerField.variableType, outerField.name,
                                        CodeAPI.accessLocalVariable(outerField.variableType, outerField.name))
                        ),
                        source)
            }

            constructorDeclaration = constructorDeclaration.setParameters(parameters).setBody(source)
        }

        visitorGenerator.generateTo(MethodDeclaration::class.java, constructorDeclaration, extraData, null, null)

        return emptyArray()
    }

}