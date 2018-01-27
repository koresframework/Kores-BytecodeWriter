/*
 *      Kores-BytecodeWriter - Translates CodeAPI Structure to JVM Bytecode <https://github.com/JonathanxD/CodeAPI-BytecodeWriter>
 *
 *         The MIT License (MIT)
 *
 *      Copyright (c) 2018 TheRealBuggy/JonathanxD (https://github.com/JonathanxD/) <jonathan.scripter@programmer.net>
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
package com.github.jonathanxd.kores.bytecode.common

import com.github.jonathanxd.kores.Types
import com.github.jonathanxd.kores.type.`is`
import org.objectweb.asm.Label
import java.lang.reflect.Type
import java.util.*

/**
 * Jvm Frame, this frame contains all variables of parent frame and store variables of current frame.
 */
internal class Frame(
    val parent: Frame? = null,
    variables: List<Variable> = emptyList(),
    val endLabel: Label? = null
) {

    private val variables: MutableList<Variable> =
        (parent?.immutableVariableList.orEmpty() + variables).toMutableList()
    val immutableVariableList: List<Variable> = Collections.unmodifiableList(this.variables)

    /**
     * Get variable at stack pos `i`.
     *
     * @param i Index in the stack.
     * @return Variable or [Optional.empty] if not present.
     */
    fun getVar(i: Int): Variable? {
        if (i !in this.variables.indices)
            return null

        if (!this.variables[i].isVisible)
            throw IllegalArgumentException("Cannot access a invisible variable!!!")

        return this.variables[i]
    }

    /**
     * Get a variable by name.
     *
     * @param name Name of the variable.
     * @return Variable or `null` if not present.
     */
    fun getVarByName(name: String): Variable? {
        return this.variables.find { `var` -> `var`.isVisible && `var`.name == name }
    }

    /**
     * Get a variable by name and type.
     *
     * @param name Name of the variable.
     * @param type Type of the variable.
     * @return Variable or `null` if not present.
     */
    fun getVar(name: String, type: Type?): Variable? {
        if (type == null) {
            return this.getVarByName(name)
        }

        return this.variables.find { `var` ->
            `var`.isVisible && `var`.name == name && `var`.type.`is`(
                type
            )
        }
    }

    /**
     * Gets the position of a variable instance
     *
     * @param variable Variable instance
     * @return Position of variable if exists, or [OptionalInt.empty] otherwise.
     */
    fun getVarPos(variable: Variable): OptionalInt {
        for (i in this.variables.indices.reversed()) {
            if (variable.isVisible && this.variables[i] == variable)
                return OptionalInt.of(i)
        }

        return OptionalInt.empty()
    }

    /**
     * Add variable
     */
    fun add(variable: Variable) {
        this.variables.add(variable)
        this.handle(variable)
    }

    /**
     * Add variable
     */
    fun add(pos: Int, variable: Variable) {
        this.variables.add(pos, variable)
        this.handle(variable)
    }

    /**
     * Set variable
     */
    fun set(pos: Int, variable: Variable) {

        if (variable.type.`is`(Types.DOUBLE) || variable.type.`is`(Types.LONG))
            throw IllegalArgumentException("Cannot set variable at pos '$pos' because it is of type Double or Long and it requires a right-move of all other variables.")

        this.variables[pos] = variable
    }

    /**
     * Handle variable addition.
     *
     * Workaround to properly store double and longs in the Local Variable Table.
     */
    private fun handle(variable: Variable) {
        if (variable.type.`is`(Types.DOUBLE) || variable.type.`is`(Types.LONG))
            this.variables.add(
                variable.copy(
                    name = "#${variable.name}ext_",
                    isTemp = true,
                    isVisible = false
                )
            )
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
    fun storeVar(name: String, type: Type, startLabel: Label, endLabel: Label?): OptionalInt {
        // normal var: isVisible = true, isTemp = false

        val variable = Variable(name, type, startLabel, endLabel ?: this.endLabel)

        for (i in this.variables.indices.reversed()) {
            val variable1 = this.variables[i]

            if (variable1.isVisible && variable1 == variable) {
                if (variable1.isTemp) {
                    throw RuntimeException("Cannot store variable named '$name'. Variable already stored!")
                }

                return OptionalInt.of(i)
            }
        }

        this.add(variable)
        // ? Last index with synchronized method is good!!!
        return this.getVarPos(variable)
    }

    /**
     * Store a internal variable. (internal variables doesn't have their names generated in
     * LocalVariableTable).
     *
     * Name generation could also be avoided using '#' symbol in the variable name.
     *
     * Position of internal variables couldn't be retrieved by [storeVar].
     *
     * Internal variables could be freely redefined and has no restrictions about the redefinition.
     *
     * @param name       Name of variable
     * @param type       Type of variable
     * @param startLabel Start label (first occurrence of variable).
     * @param endLabel   End label (last usage of variable).
     * @return [OptionalInt] holding the position, or empty if failed to store.
     */
    fun storeInternalVar(
        name: String,
        type: Type,
        startLabel: Label,
        endLabel: Label?
    ): OptionalInt {
        val variable = Variable(name, type, startLabel, endLabel ?: this.endLabel, true)

        for (i in variables.indices.reversed()) {
            val variable_ = this.variables[i]

            if (variable_.isVisible && variable_ == variable) {
                return OptionalInt.of(i)
            }
        }

        this.add(variable)
        // ? Last index with synchronized method is good!!!
        return this.getVarPos(variable)
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
     * Create a unique name of variable based on [base] name.
     */
    fun getUniqueVariableName(base: String): String {
        if (!hasVar(base))
            return base

        var finalName = base
        var i = 0

        do {
            finalName += i
            ++i
        } while (hasVar(finalName))

        return finalName
    }

    fun hasVar(varName: String) = this.variables.any { it.name == varName }
}