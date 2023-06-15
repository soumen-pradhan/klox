import Stmt.*
import TokenType.IDENTIFIER

sealed class Stmt {
    data class Expression(val expr: Expr) : Stmt()
    data class Print(val expr: Expr) : Stmt()
    data class Var(val name: IDENTIFIER, val init: Expr) : Stmt()
    data class Block(val stmts: List<Stmt>) : Stmt()
    data class If(val cond: Expr, val thenBranch: Stmt, val elseBranch: Stmt?) : Stmt()
}

fun Stmt.ast(): String = when (this) {
    is Expression -> expr.ast()
    is Print -> "(print ${expr.ast()})"
    is Var -> "(decl ${name.repr()} ${init.ast()})"
    is Block -> "(block ${stmts.joinToString(" ") { it.ast() }})"
    is If -> "(if ${thenBranch.ast()}" + if (elseBranch != null) " ${elseBranch.ast()})" else ")"
}
