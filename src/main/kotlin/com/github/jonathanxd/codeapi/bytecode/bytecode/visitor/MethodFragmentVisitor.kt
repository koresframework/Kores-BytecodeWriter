package com.github.jonathanxd.codeapi.gen.bytecode.visitor

import com.github.jonathanxd.codeapi.common.MVData
import com.github.jonathanxd.codeapi.gen.BytecodeClass
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.gen.visit.VoidVisitor
import com.github.jonathanxd.codeapi.interfaces.MethodDeclaration
import com.github.jonathanxd.codeapi.interfaces.MethodFragment
import com.github.jonathanxd.codeapi.interfaces.MethodInvocation
import com.github.jonathanxd.iutils.data.MapData
import com.github.jonathanxd.iutils.type.TypeInfo

object MethodFragmentVisitor : VoidVisitor<MethodFragment, BytecodeClass, Any?> {

    @JvmStatic
    val FRAGMENT_TYPE_INFO = TypeInfo.aUnique(MethodFragment::class.java)

    @JvmStatic
    fun visitFragmentsGeneration(visitorGenerator: VisitorGenerator<*>, extraData: MapData) {
        val all = extraData.getAll(MethodFragmentVisitor.FRAGMENT_TYPE_INFO)

        if (!all.isEmpty()) {
            for (methodFragment in all) {
                visitorGenerator.generateTo(MethodFragment::class.java, methodFragment, extraData, null, null)
            }
        }
    }

    override fun voidVisit(t: MethodFragment, extraData: MapData, visitorGenerator: VisitorGenerator<BytecodeClass>, additional: Any?) {
        if (additional != null && additional is MVData) {
            extraData.registerData(MethodFragmentVisitor.FRAGMENT_TYPE_INFO, t)
            visitorGenerator.generateTo(MethodInvocation::class.java, t, extraData, null, additional)
        } else {
            visitorGenerator.generateTo(MethodDeclaration::class.java, t.method, extraData, null, null)
        }
    }


}