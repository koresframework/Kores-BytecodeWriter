package com.github.jonathanxd.codeapi.gen.bytecode.visitor

import com.github.jonathanxd.codeapi.CodeAPI
import com.github.jonathanxd.codeapi.gen.BytecodeClass
import com.github.jonathanxd.codeapi.gen.visit.Visitor
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.helper.PredefinedTypes
import com.github.jonathanxd.codeapi.interfaces.EnumDeclaration
import com.github.jonathanxd.codeapi.types.Generic
import com.github.jonathanxd.codeapi.util.gen.EnumUtil
import com.github.jonathanxd.iutils.data.MapData

object EnumVisitor : Visitor<EnumDeclaration, BytecodeClass, Any?> {

    override fun visit(t: EnumDeclaration, extraData: MapData, visitorGenerator: VisitorGenerator<BytecodeClass>, additional: Any?): Array<BytecodeClass> {
        val enumModifiers = EnumUtil.getEnumModifiers(t)
        val source = EnumUtil.generateEnumClassSource(t)

        val typeDeclaration = CodeAPI.aClassBuilder()
                .withModifiers(enumModifiers)
                .withQualifiedName(t.qualifiedName)
                .withSuperClass(Generic.type(PredefinedTypes.ENUM).of(t))
                .withImplementations(t.implementations)
                .withBody(source)
                .build()

        return visitorGenerator.generateTo(typeDeclaration.javaClass, typeDeclaration, extraData, additional)
    }

}