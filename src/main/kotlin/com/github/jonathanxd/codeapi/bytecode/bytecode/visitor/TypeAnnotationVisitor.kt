package com.github.jonathanxd.codeapi.gen.bytecode.visitor

import com.github.jonathanxd.codeapi.MutableCodeSource
import com.github.jonathanxd.codeapi.builder.InterfaceBuilder
import com.github.jonathanxd.codeapi.common.CodeModifier
import com.github.jonathanxd.codeapi.gen.BytecodeClass
import com.github.jonathanxd.codeapi.gen.visit.Visitor
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.helper.PredefinedTypes
import com.github.jonathanxd.codeapi.interfaces.AnnotationDeclaration
import com.github.jonathanxd.iutils.data.MapData

object TypeAnnotationVisitor : Visitor<AnnotationDeclaration, BytecodeClass, Any?> {

    override fun visit(annotationDeclaration: AnnotationDeclaration, extraData: MapData, visitorGenerator: VisitorGenerator<BytecodeClass>, additional: Any?): Array<BytecodeClass> {
        val modifiers = ArrayList(annotationDeclaration.modifiers)

        modifiers.add(CodeModifier.ANNOTATION)

        val source = MutableCodeSource()

        source.addAll(annotationDeclaration.properties)

        val body = annotationDeclaration.body

        if(body.isPresent) {
            source.addAll(body.get())
        }

        val typeDeclaration = InterfaceBuilder.builder()
                .withModifiers(modifiers)
                .withQualifiedName(annotationDeclaration.qualifiedName)
                .withImplementations(PredefinedTypes.ANNOTATION)
                .withBody(source)
                .build()

        return visitorGenerator.generateTo(typeDeclaration.javaClass, typeDeclaration, extraData, additional)
    }

}