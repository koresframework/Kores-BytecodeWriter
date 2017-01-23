package com.github.jonathanxd.codeapi.gen.bytecode.visitor

import com.github.jonathanxd.codeapi.CodePart
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.gen.visit.VoidVisitor
import com.github.jonathanxd.codeapi.sugar.SugarSyntax
import com.github.jonathanxd.iutils.data.MapData

open class SugarSyntaxVisitor<T: CodePart, R: CodePart, V>(val sugarSyntax: SugarSyntax<T, R>) : VoidVisitor<T, V, Any?> {

    override fun voidVisit(t: T, extraData: MapData, visitorGenerator: VisitorGenerator<V>, additional: Any?) {
        val generated = this.sugarSyntax.generator.generate(t)

        visitorGenerator.generateTo(generated.javaClass, generated, extraData, null)
    }


}