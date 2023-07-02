/**
 * expression → literal | unary | binary | grouping
 * literal    → NUMBER | STRING | "true" | "false" | "nil"
 * grouping   → "(" expression ")"
 * unary      → ( "-" | "!" ) expression
 * binary     → expression operator expression
 * operator   → "==" | "!=" | "<" | "<=" | ">" | ">=" | "+" | "-" | "*" | "/"
 */

import TokenType.IDENT

sealed interface Expr {

    class Binary(val left: Expr, val op: Token, val right: Expr) : Expr {
        override fun toString() = "(${op.type} $left $right)"
    }

    class Logical(val left: Expr, val op: Token, val right: Expr) : Expr {
        override fun toString() = "(${op.type} $left $right)"
    }

    // paren is for error logging
    class Call(val callee: Expr, val paren: Token, val args: List<Expr>) : Expr {
        override fun toString() = "(call $callee ${args.joinToString(" ") { "$it" }})"
    }

    class Grouping(val expr: Expr) : Expr {
        override fun toString() = "(group $expr)"
    }

    class Unary(val op: Token, val right: Expr) : Expr {
        override fun toString() = "${op.type} $right)"
    }

    // refer to a variable
    class Variable(val name: IDENT) : Expr {
        override fun toString() = "(var $name)"
    }

    // assign to a variable (not declare)
    class Assign(val name: IDENT, val expr: Expr) : Expr {
        override fun toString() = "(let $name $expr)"
    }

    sealed interface Literal : Expr {

        class Str(val value: String) : Literal {
            override fun toString() = value
        }

        class Num(val value: Double) : Literal {
            override fun toString() = value.stringify()
        }

        class Bool(val value: Boolean) : Literal {
            override fun toString() = value.toString()
        }

        object Nil : Literal {
            override fun toString() = "nil"
        }

        object Nothing : Literal {
            override fun toString() = "Nothing"
        }

        sealed interface LoxObj : Literal {

            interface LoxCallable : LoxObj {
                fun arity(): Int
                fun call(context: Interpreter, args: List<Literal>): Literal
            }

            class LoxFunction(val decl: Stmt.Function) : LoxCallable {
                override fun toString() = "(fun ${decl.name} [${decl.params.joinToString(", ") { "$it" }}])"

                override fun arity() = decl.params.size

                override fun call(context: Interpreter, args: List<Literal>): Literal {
                    val localEnv = Environment(Interpreter.globals)

                    // bind parameter names to values
                    for ((name, expr) in decl.params.zip(args)) {
                        localEnv.define(name, expr)
                    }

                    try {
                        context.interpret(decl.body, localEnv)
                    } catch (returnVal: Interpreter.ReturnToParentCall) {
                        return returnVal.value ?: Nothing
                    }
                    return Nothing
                }
            }
        }
    }
}
