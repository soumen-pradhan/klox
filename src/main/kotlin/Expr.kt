/**
 * expression → literal | unary | binary | grouping
 * literal    → NUMBER | STRING | "true" | "false" | "nil"
 * grouping   → "(" expression ")"
 * unary      → ( "-" | "!" ) expression
 * binary     → expression operator expression
 * operator   → "==" | "!=" | "<" | "<=" | ">" | ">=" | "+" | "-" | "*" | "/"
 */

import Expr.*
import TokenType.IDENTIFIER

sealed class Expr {
    data class Binary(val left: Expr, val operator: Token, val right: Expr) : Expr()
    data class Logical(val left: Expr, val operator: Token, val right: Expr) : Expr()

    data class Grouping(val expr: Expr) : Expr()
    data class Unary(val operator: Token, val right: Expr) : Expr()

    // refer to a variable
    data class Variable(val name: IDENTIFIER) : Expr()

    // assign to a variable (not declare)
    data class Assign(val name: IDENTIFIER, val expr: Expr) : Expr()

    sealed class Literal : Expr() {
        class Str(val value: String) : Literal()
        class Num(val value: Double) : Literal()
        class Bool(val value: Boolean) : Literal()
        object Nothing : Literal()
    }
}

fun Expr.ast(): String = when (this) {
    is Binary -> "(${operator.type.repr()} ${left.ast()} ${right.ast()})"
    is Logical -> "(${operator.type.repr()} ${left.ast()} ${right.ast()})"

    is Grouping -> "(group ${expr.ast()})"
    is Unary -> "(${operator.type.repr()} ${right.ast()})"

    is Variable -> "(var ${name.repr()})"
    is Assign -> "(let ${name.repr()} ${expr.ast()})"

    is Literal.Str -> value
    is Literal.Num -> value.repr()
    is Literal.Bool -> value.toString()
    is Literal.Nothing -> "nil"
}
