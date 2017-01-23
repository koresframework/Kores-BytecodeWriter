package com.github.jonathanxd.codeapi.gen.bytecode.visitor

import com.github.jonathanxd.codeapi.common.Flow
import com.github.jonathanxd.codeapi.common.MemberInfos
import com.github.jonathanxd.iutils.type.TypeInfo

object ConstantDatas {

    val FLOW_TYPE_INFO = TypeInfo.aUnique(Flow::class.java)
    val MEMBER_INFOS = TypeInfo.aUnique(MemberInfos::class.java)

}