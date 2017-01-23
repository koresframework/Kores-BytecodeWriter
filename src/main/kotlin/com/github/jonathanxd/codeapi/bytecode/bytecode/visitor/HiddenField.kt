package com.github.jonathanxd.codeapi.gen.bytecode.visitor

import com.github.jonathanxd.codeapi.CodePart
import com.github.jonathanxd.codeapi.annotation.GenerateTo
import com.github.jonathanxd.codeapi.common.CodeModifier
import com.github.jonathanxd.codeapi.impl.CodeField
import com.github.jonathanxd.codeapi.interfaces.Annotation
import com.github.jonathanxd.codeapi.interfaces.FieldDeclaration
import com.github.jonathanxd.codeapi.types.CodeType

@GenerateTo(FieldDeclaration::class)
class HiddenField(name: String, type: CodeType, value: CodePart?, modifiers: Collection<CodeModifier>, annotations: List<Annotation>) :
        CodeField(name, type, value, modifiers, annotations) {


    constructor(name: String, type: CodeType, modifiers: Collection<CodeModifier>, annotations: List<Annotation>)
            : this(name, type, null, modifiers, annotations)

    constructor(name: String, type: CodeType, modifiers: Collection<CodeModifier>)
            : this(name, type, null, modifiers, emptyList())

    constructor(name: String, type: CodeType)
            : this(name, type, null, emptyList(), emptyList())

    constructor(name: String, type: CodeType, value: CodePart, modifiers: Collection<CodeModifier>)
            : this(name, type, value, modifiers, emptyList())

    constructor(name: String, type: CodeType, value: CodePart)
            : this(name, type, value, emptyList(), emptyList())
}