import TokenType.IDENTIFIER

sealed class Stmt {
    data class Expression(val expr: Expr) : Stmt()
    data class Print(val expr: Expr) : Stmt()
    data class Var(val name: IDENTIFIER, val init: Expr) : Stmt()
    data class Block(val stmts: List<Stmt>) : Stmt()
}
