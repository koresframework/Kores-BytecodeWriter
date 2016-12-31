/*
 *      CodeAPI-BytecodeWriter - Framework to generate Java code and Bytecode code. <https://github.com/JonathanxD/CodeAPI-BytecodeWriter>
 *
 *         The MIT License (MIT)
 *
 *      Copyright (c) 2016 TheRealBuggy/JonathanxD (https://github.com/JonathanxD/ & https://github.com/TheRealBuggy/) <jonathan.scripter@programmer.net>
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
package com.github.jonathanxd.codeapi.bytecode.common

import com.github.jonathanxd.codeapi.bytecode.util.CodeTypeUtil
import com.github.jonathanxd.codeapi.interfaces.TagLine
import com.github.jonathanxd.codeapi.types.CodeType
import com.github.jonathanxd.codeapi.types.GenericType
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import java.util.*

/**
 * Internal class that holds a [MethodVisitor] and data about current stored variables (stack)
 * and tag lines.
 *
 * This class doesn't generate bytecode instructions, this class only hold information
 * about variables.
 *
 * @param methodVisitor ASM Method visitor
 * @param variables Variables in stack (including `this`).
 */
class MVData constructor(
        val methodVisitor: MethodVisitor,
        private val variables: MutableList<Variable>) {

    /**
     * Unmodifiable variable list.
     */
    private val unmod: List<Variable>

    /**
     * Tag lines for debug.
     */
    private val tagLines: MutableList<TagLine<*, *>>

    init {
        this.unmod = Collections.unmodifiableList(variables)
        this.tagLines = ArrayList<TagLine<*, *>>()
    }

    /**
     * Get variable at stack pos `i`.
     *
     * @param i Index in the stack.
     * @return Variable or [Optional.empty] if not present.
     */
    fun getVar(i: Int): Optional<Variable> {
        if (i < 0 || i >= this.variables.size)
            return Optional.empty<Variable>()

        return Optional.of(this.variables[i])
    }

    /**
     * Get a variable by name.
     *
     * @param name Name of the variable.
     * @return Variable or [Optional.empty] if not present.
     */
    fun getVarByName(name: String): Optional<Variable> {
        return this.variables.stream().filter { `var` -> `var`.name == name }.findAny()
    }

    /**
     * Get a variable by name and type.
     *
     * @param name Name of the variable.
     * @param type Type of the variable.
     * @return Variable or [Optional.empty] if not present.
     */
    fun getVar(name: String, type: CodeType?): Optional<Variable> {
        if (type == null) {
            return this.getVarByName(name)
        }

        return variables.stream().filter { `var` -> `var`.name == name && `var`.type.compareTo(type) == 0 }.findAny()
    }

    /**
     * Gets the position of a variable instance
     *
     * @param variable Variable instance
     * @return Position of variable if exists, or [OptionalInt.empty] otherwise.
     */
    fun getVarPos(variable: Variable): OptionalInt {
        for (i in variables.indices.reversed()) {
            if (this.variables[i] == variable)
                return OptionalInt.of(i)
        }

        return OptionalInt.empty()
    }

    /**
     * Store a variable in stack "table".
     *
     * @param name       Name of variable
     * @param type       Type of variable
     * @param startLabel Start label (first occurrence of variable).
     * @param endLabel   End label (last usage of variable).
     * @return [OptionalInt] holding the position, or empty if failed to store.
     * @throws RuntimeException if variable is already defined.
     */
    fun storeVar(name: String, type: CodeType, startLabel: Label, endLabel: Label?): OptionalInt {
        val variable = Variable(name, type, startLabel, endLabel)

        for (i in this.variables.indices.reversed()) {
            val variable1 = this.variables[i]

            if (variable1 == variable) {
                if (variable1.isTemp) {
                    throw RuntimeException("Cannot store variable named '$name'. Variable already stored!")
                }

                return OptionalInt.of(i)
            }
        }

        this.variables.add(variable)
        // ? Last index with synchronized method is good!!!
        return this.getVarPos(variable)
    }

    /**
     * Store a internal variable. (internal variables doesn't have their names generated in
     * LocalVariableTable).
     *
     * Name generation could also be avoided using '#' symbol in the variable name.
     *
     * Position of internal variables couldn't be getted by [.storeVar].
     *
     * Internal variables could be freely redefined and has no restrictions about the redefinition.
     *
     * @param name       Name of variable
     * @param type       Type of variable
     * @param startLabel Start label (first occurrence of variable).
     * @param endLabel   End label (last usage of variable).
     * @return [OptionalInt] holding the position, or empty if failed to store.
     */
    fun storeInternalVar(name: String, type: CodeType, startLabel: Label, endLabel: Label?): OptionalInt {
        val variable = Variable(name, type, startLabel, endLabel, true)

        for (i in variables.indices.reversed()) {
            if (this.variables[i] == variable) {
                return OptionalInt.of(i)
            }
        }

        this.variables.add(variable)
        // ? Last index with synchronized method is good!!!
        return this.getVarPos(variable)
    }

    /**
     * Redefine a variable in a `position`.
     *
     * @param pos        Position of variable in stack map.
     * @param name       Name of variable
     * @param type       Type of variable
     * @param startLabel Start label (first occurrence of variable).
     * @param endLabel   End label (last usage of variable).
     */
    fun redefineVar(pos: Int, name: String, type: CodeType, startLabel: Label, endLabel: Label?) {
        val variable = Variable(name, type, startLabel, endLabel)

        if (pos >= this.variables.size) {
            this.variables.add(pos, variable)
        } else {
            if (this.variables[pos].isTemp) {
                throw RuntimeException("Cannot store variable named '$name'. Variable already stored!")
            }

            this.variables[pos] = variable
        }
    }

    /**
     * Visit a tag line.
     *
     * @param line Tag line.
     * @return Position of the tag line.
     */
    fun visitLine(line: TagLine<*, *>): Int {
        this.tagLines.add(line)

        return this.tagLines.size - 1
    }

    /**
     * Return last position in stack map.
     *
     * @return Last position in stack map.
     */
    fun currentPos(): Int {
        return this.variables.size - 1
    }

    /**
     * Gets a immutable list with all variables.
     *
     * @return Immutable list with all variables.
     */
    fun getVariables(): List<Variable> {
        return this.unmod
    }

    /**
     * Generate LocalVariableTable
     *
     * @param start Start of the method.
     * @param end   End of the method.
     */
    fun visitVars(start: Label, end: Label) {
        val variables = this.getVariables()

        for (i in variables.indices) {
            val variable = variables[i]

            if (variable.isTemp || variable.name.contains("#"))
            // Internal variables
                continue

            val varStart = if (variable.startLabel != null) variable.startLabel else start
            val varEnd = if (variable.endLabel != null) variable.endLabel else end

            val type = CodeTypeUtil.codeTypeToFullAsm(variable.type)

            var signature: String? = null

            if (variable.type is GenericType) {
                signature = CodeTypeUtil.toAsm(variable.type)
            }

            methodVisitor.visitLocalVariable(variable.name, type, signature, varStart, varEnd, i)
        }
    }

    /**
     * Generate "find fail" exception to a variable.
     *
     * @param variable Variable failed to find.
     * @return Exception
     */
    fun failFind(variable: Variable): IllegalStateException {
        return IllegalStateException("Cannot find variable '" + variable + "' in stack table: " + this.getVariables())
    }

    /**
     * Generate "store fail" exception to a variable.
     *
     * @param o Object failed to be stored.
     * @return Exception
     */
    fun failStore(o: Any): IllegalStateException {
        return IllegalStateException("Couldn't store '" + o + "' in stack table: " + this.getVariables())
    }
}
