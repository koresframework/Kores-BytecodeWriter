package com.github.jonathanxd.codeapi.gen.bytecode.visitor

import com.github.jonathanxd.codeapi.gen.BytecodeClass
import com.github.jonathanxd.codeapi.gen.visit.Visitor
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.interfaces.PackageDeclaration
import com.github.jonathanxd.iutils.data.MapData
import com.github.jonathanxd.iutils.type.TypeInfo

object PackageVisitor : Visitor<PackageDeclaration, BytecodeClass, Any?> {

    val PACKAGE_REPRESENTATION = TypeInfo.a(PackageDeclaration::class.java).setUnique(true).build()

    override fun visit(t: PackageDeclaration, extraData: MapData, visitorGenerator: VisitorGenerator<BytecodeClass>, additional: Any?): Array<BytecodeClass> {

        extraData.registerData(PACKAGE_REPRESENTATION, t)

        return emptyArray()

    }

}