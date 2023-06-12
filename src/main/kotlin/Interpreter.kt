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
