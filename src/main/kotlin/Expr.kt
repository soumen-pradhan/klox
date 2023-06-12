/**
expression → literal | unary | binary | grouping ;
literal    → NUMBER | STRING | "true" | "false" | "nil" ;
grouping   → "(" expression ")" ;
unary      → ( "-" | "!" ) expression ;
binary     → expression operator expression ;
operator   → "==" | "!=" | "<" | "<=" | ">" | ">=" | "+" | "-" | "*" | "/" ;
 */

sealed class Expr {
    data class Binary(val left: Expr, val operator: Token, val right: Expr) : Expr()
    data class Grouping(val expr: Expr) : Expr()
    data class Literal(val value: Any?) : Expr()
    data class Unary(val operator: Token, val right: Expr) : Expr()
}

fun Expr.print(): String = when (this) {
    is Expr.Binary -> paren(operator.toString(), left, right)
    is Expr.Grouping -> paren("group", expr)
    is Expr.Literal -> value?.toString() ?: "nil"
    is Expr.Unary -> paren(operator.toString(), right)
}

fun paren(name: String, vararg exprs: Expr) = buildString {
    append("($name")

    for (expr in exprs) {
        append(" ")
        append(expr.print())
    }

    append(")")
}
