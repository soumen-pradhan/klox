import TokenType.IDENT

sealed class Stmt {
    class Expression(val expr: Expr) : Stmt() {
        override fun toString(): String = expr.toString()
    }

    class Print(val expr: Expr) : Stmt() {
        override fun toString(): String = "(print $expr)"
    }

    class Return(val keyword: Token, val expr: Expr?) : Stmt() {
        override fun toString(): String = "(return ${(expr ?: Expr.Literal.Nothing)}"
    }

    class Function(val name: IDENT, val params: List<IDENT>, val body: Block) : Stmt() {
        override fun toString(): String = "(fun $name [${params.joinToString { "$it" }}] (...))"
    }

    class Var(val name: IDENT, val init: Expr) : Stmt() {
        override fun toString(): String = "(var $name $init)"
    }

    class Block(val stmts: List<Stmt>) : Stmt() {
        override fun toString(): String = "(block ${stmts.joinToString(" ") { "$it" }})"
    }

    class If(val cond: Expr, val thenBranch: Stmt, val elseBranch: Stmt?) : Stmt() {
        override fun toString(): String = "(if $thenBranch" + if (elseBranch != null) " $elseBranch)" else ")"
    }

    class While(val cond: Expr, val body: Stmt) : Stmt() {
        override fun toString(): String = "(while $cond $body)"
    }

}
