/*import Type.*

fun Expr.interpret() = when (this) {
    is Expr.Binary -> {
        val leftVal = eval(left)
        val rightVal = eval(right)

        when (operator.type) {
            GREATER -> (leftVal as Double) > (rightVal as Double)
            GREATER_EQUAL -> (leftVal as Double) >= (rightVal as Double)
            LESS -> (leftVal as Double) < (rightVal as Double)
            LESS_EQUAL -> (leftVal as Double) <= (rightVal as Double)

            BANG_EQUAL -> !isEqual(left, right)
            EQUAL_EQUAL -> isEqual(left, right)

            MINUS -> (leftVal as Double) - (rightVal as Double)

            PLUS -> {
                if (leftVal is Double && rightVal is Double) {
                    leftVal + rightVal
                } else if (leftVal is String && rightVal is String) {
                    leftVal + rightVal
                } else {
                    Object()
                }
            }

            SLASH -> (leftVal as Double) / (rightVal as Double)
            STAR -> (leftVal as Double) * (rightVal as Double)

            else -> Object()
        }
    }

    is Expr.Grouping -> eval(expr)
    is Expr.Literal -> value?.toString() ?: "nil"

    is Expr.Unary -> {
        val rightVal = eval(right)
        when (operator.type) {
            MINUS -> -(rightVal as Double)
            BANG -> !isTruthy(rightVal)
            else -> Object()
        }
    }
}

fun eval(expr: Expr): Any = Object()

fun isTruthy(value: Any?) = when (value) {
    null -> false
    is Boolean -> value
    else -> true
}

fun isEqual(a: Any?, b: Any?): Boolean {
    if (a == null && b == null) return true
    if (a == null) return false

    return a == b
}
*/

import Expr.*
import Stmt.*
import TokenType.*

class Interpreter {
    private var env = Environment()

    fun interpret(stmt: Stmt): Unit {
        when (stmt) {
            is Expression -> stmt.expr.eval()
            is Print -> stmt.expr.eval().also { println(it.ast()) }

            is Var -> {
                val initVal = stmt.init.eval()
                env.define(stmt.name, initVal)
            }

            is Block -> {
                val parent = env
                var lastVal: Literal = Literal.Nothing

                try {
                    env = Environment(parent) // create new env
                    stmt.stmts.forEach { interpret(it) }
                } finally {
                    env = parent
                }
            }

            is If -> {
                if (stmt.cond.eval().isTruthy()) interpret(stmt.thenBranch)
                else if (stmt.elseBranch != null) interpret(stmt.elseBranch)
            }
        }
    }

    private fun Expr.eval(): Literal = when (this) {
        is Binary -> {
            val leftVal = left.eval()
            val rightVal = right.eval()

            val errMsg = "Unknown operator `${operator.type.repr()}`"
            Log.apply {
                start = operator.pos
                msg = errMsg
            }

            when (operator.type) {
                BANG_EQUAL -> Literal.Bool(!isEqual(leftVal, rightVal))
                EQUAL_EQUAL -> Literal.Bool(isEqual(leftVal, rightVal))

                else -> if (leftVal is Literal.Num && rightVal is Literal.Num) {
                    when (operator.type) {
                        GREATER -> Literal.Bool(leftVal.value > rightVal.value)
                        GREATER_EQUAL -> Literal.Bool(leftVal.value >= rightVal.value)
                        LESS -> Literal.Bool(leftVal.value < rightVal.value)
                        LESS_EQUAL -> Literal.Bool(leftVal.value <= rightVal.value)

                        PLUS -> Literal.Num(leftVal.value + rightVal.value)
                        MINUS -> Literal.Num(leftVal.value - rightVal.value)
                        SLASH -> Literal.Num(leftVal.value / rightVal.value)
                        STAR -> Literal.Num(leftVal.value * rightVal.value)

                        else -> {
                            Log.err {}
                            throw TypeError(errMsg)
                        }
                    }
                } else if (leftVal is Literal.Str && rightVal is Literal.Str) {
                    when (operator.type) {
                        PLUS -> Literal.Str(leftVal.value + rightVal.value)
                        else -> {
                            Log.err {}
                            throw TypeError(errMsg)
                        }
                    }
                } else {
                    Log.err { msg = "Required 2 number or 2 string" }
                    throw TypeError("Required 2 number or 2 string")
                }
            }
        }

        is Grouping -> expr.eval()

        is Unary -> {
            val rightVal = right.eval()

            when (operator.type) {
                MINUS -> if (rightVal is Literal.Num) {
                    Literal.Num(-rightVal.value)
                } else {
                    Log.err {
                        start = operator.pos
                        msg = "Expected number"
                    }
                    throw TypeError("Expected number")
                }

                BANG -> Literal.Bool(!rightVal.isTruthy())
                else -> throw TypeError("Unknown Operation")
            }
        }

        is Variable -> env.get(name)

        is Assign -> expr.eval().also { env.assign(name, it) }

        is Literal -> this
    }
}

class Environment(
    private val parent: Environment? = null,
    private val map: MutableMap<String, Literal> = mutableMapOf(),
) {
    // error logging responsibility on caller code
    fun get(name: IDENTIFIER): Literal = map[name.value]
        ?: parent?.get(name)
        ?: throw InterpreterError("Undefined variable `${name.value}`")

    fun define(name: IDENTIFIER, value: Literal) {
        map[name.value] = value
    }

    fun assign(name: IDENTIFIER, value: Literal) {
        if (map.containsKey(name.value)) {
            map[name.value] = value
        } else if (parent != null) {
            parent.assign(name, value)
        } else {
            Log.err { msg = "Undefined variable ${name.value}" }
            throw InterpreterError("Undefined variable `${name.value}`")
        }
    }
}

fun Literal.isTruthy() = when (this) {
    is Literal.Nothing -> false
    is Literal.Bool -> value
    else -> true
}

fun isEqual(a: Literal, b: Literal) =
    if (a is Literal.Nothing && b is Literal.Nothing) true
    else if (a is Literal.Nothing) false
    else a == b
