/**
 * program    → decl* EOF
 * decl       → funcDecl | varDecl | statement
 * funcDecl   → "fun" IDENT "(" param? ")" block
 * param      → IDENT ( "," IDENT )*
 * varDecl    → "var" IDENT ( "=" expression )? ";"
 * statement  → exprStmt | ifStmt | printStmt | forStmt | whileStmt | block | retStmt
 * exprStmt   → expression ";"
 * ifStmt     → "if" expression "{" statement "}" ( "else" "{" statement "}" )?
 * printStmt  → "print" expression ";"
 * forStmt    → "for" "(" ( varDecl | exprStmt | ";" ) expression? ";" expression? ")" statement
 * whileStmt  → "while" "(" expression ")" statement
 * block      → "(" decl* ")"
 * retStmt    → "return" expression? ";"
 * expression → assignment
 * assignment → IDENT "=" assignment | logic_or
 * logic_or   → logic_and ( "or" logic_and )*
 * logic_and  → equality ( "and" equality )*
 * equality   → comparison ( ( "!=" | "==" ) comparison )*
 * comparison → term ( ( ">" | ">=" | "<" | "<=" ) term )*
 * term       → factor ( ( "-" | "+" ) factor )*
 * factor     → unary ( ( "/" | "*" ) unary )*
 * unary      → ( "!" | "-" ) unary | call
 * call       → primary ( "(" arguments? ")" )*
 * arguments → expression ( "," expression )*
 * primary    → NUMBER | STRING | "true" | "false" | "nil" | "(" expression ")" | IDENT
 */

import TokenType.*


class Parser(private val tokens: PeekableIterator<Token>) {

    fun parse(): Iterator<Stmt> = iterator {
        while (!tokens.end()) {
            val decl = declaration() ?: continue
            yield(decl)
        }
    }

    private fun declaration(): Stmt? {
        val beginToken = tokens.peekSafe()

        return try {
            when (beginToken.type) {
                is VAR -> {
                    tokens.next()
                    varDecl()
                }

                is FUN -> {
                    tokens.next()
                    funcDecl(FuncType.FUNCTION)
                }

                else -> statement()
            }
        } catch (_: ParseError) {
            // Log.err(e.message ?: "")
            sync()
            null
        }
    }

    enum class FuncType {
        FUNCTION, METHOD
    }

    private fun consumeIdent(message: String): IDENT {
        val (peek, pos) = tokens.peekSafe()

        return if (peek is IDENT) {
            tokens.next()
            peek
        } else {
            Log.errAndThrow {
                start = pos
                end = pos + Pos(0, peek.toString().length)
                msg = message
            }
        }
    }

    private fun funcDecl(kind: FuncType): Stmt {
        val name = consumeIdent("Expected $kind name")

        consume(LEFT_PAREN, "Expected `${LEFT_PAREN}` after $kind name")

        val params = mutableListOf<IDENT>()
        if (!tokens.match(RIGHT_PAREN)) {
            do {
                if (params.size >= 255) {
                    val (peek, pos) = tokens.peekSafe()
                    Log.errAndThrow {
                        start = pos
                        end = pos + Pos(0, peek.toString().length)
                        msg = "Can't have more than 255 parameters"
                    }
                }

                val param = consumeIdent("Expected parameter name")
                params.add(param)

            } while (tokens.matchAndConsume(COMMA))
        }

        consume(RIGHT_PAREN, "Expected `${RIGHT_PAREN}` after parameters")

        consume(LEFT_BRACE, "Expected `${LEFT_BRACE}` before $kind body")
        val body = block()

        return Stmt.Function(name, params, Stmt.Block(body))
    }

    private fun varDecl(): Stmt {
        val name = consumeIdent("Expected a variable")

        val init = if (tokens.matchAndConsume(EQUAL)) {
            expression()
        } else {
            Expr.Literal.Nil
        }

        // TODO Bug - When semicolon is missing at end, the next line is logged
        consume(SEMICOLON, "Expected `${SEMICOLON}` after variable `${name}` declaration")

        return Stmt.Var(name, init)
    }

    private fun statement(): Stmt {
        val beginToken = tokens.peekSafe()

        return when (beginToken.type) {
            PRINT -> {
                tokens.next()
                val expr = expression()
                consume(SEMICOLON, "Expected `${SEMICOLON}` after statement")
                Stmt.Print(expr)
            }

            RETURN -> {
                val keyword = tokens.next()
                val expr = if (!tokens.match(SEMICOLON)) expression() else null
                consume(SEMICOLON, "Expected $SEMICOLON after return")
                Stmt.Return(keyword, expr)
            }

            WHILE -> {
                tokens.next()

                consume(LEFT_PAREN, "Expected `${LEFT_PAREN}` after while")
                val cond = expression()
                consume(RIGHT_PAREN, "Expected `${RIGHT_PAREN}` after condition")

                val body = statement()

                Stmt.While(cond, body)
            }

            FOR -> {
                tokens.next()
                forStatement()
            }

            IF -> {
                tokens.next()
                ifStatement()
            }

            LEFT_BRACE -> {
                tokens.next()
                Stmt.Block(block())
            }

            else -> exprStatement()

        }
    }

    private fun exprStatement(): Stmt {
        val expr = expression()
        consume(SEMICOLON, "Expected `${SEMICOLON}` after statement")
        return Stmt.Expression(expr)
    }

    private fun forStatement(): Stmt {
        consume(LEFT_PAREN, "Expected `${LEFT_PAREN}` after `for`")

        val init = if (tokens.matchAndConsume(SEMICOLON)) {
            null
        } else if (tokens.matchAndConsume(VAR)) {
            varDecl()
        } else {
            exprStatement()
        }

        val cond = if (!tokens.match(SEMICOLON)) expression() else Expr.Literal.Bool(true)
        consume(SEMICOLON, "Expected `${SEMICOLON}` after loop condition")

        val postBody = if (!tokens.match(RIGHT_PAREN)) expression() else null
        consume(RIGHT_PAREN, "Expected `${RIGHT_PAREN}` after the clauses")

        var body = statement()

        if (postBody != null) {
            body = Stmt.Block(listOf(body, Stmt.Expression(postBody)))
        }

        body = Stmt.While(cond, body)

        if (init != null) {
            body = Stmt.Block(listOf(init, body))
        }

        return body
    }

    private fun ifStatement(): Stmt {
        consume(LEFT_PAREN, "Expected `${LEFT_PAREN}` after condition")
        val cond = expression()
        consume(RIGHT_PAREN, "Expected `${RIGHT_PAREN}` after block")

        val thenBranch = statement()
        val elseBranch = if (tokens.matchAndConsume(ELSE)) statement() else null

        return Stmt.If(cond, thenBranch, elseBranch)
    }

    private fun block(): List<Stmt> {
        val statements = buildList {
            while (tokens.peekSafe().type != RIGHT_BRACE) {
                val decl = declaration() ?: continue
                add(decl)
            }
        }

        consume(RIGHT_BRACE, "Expected `${RIGHT_BRACE}` after block")
        return statements
    }

    private fun expression(): Expr = assignment()

    private fun assignment(): Expr {
        val expr = logicalOr()

        if (tokens.match(EQUAL)) {
            val equals = tokens.next()
            val value = assignment()

            if (expr is Expr.Variable) {
                val name = expr.name
                return Expr.Assign(name, value)
            }

            Log.err {
                start = equals.pos
                msg = "Invalid assignment target"
            }
        }

        return expr
    }

    private fun logicalOr(): Expr {
        var expr = logicalAnd()

        while (tokens.match(OR)) {
            val operator = tokens.next()
            val right = logicalAnd()
            expr = Expr.Logical(expr, operator, right)
        }

        return expr
    }

    private fun logicalAnd(): Expr {
        var expr = equality()

        while (tokens.match(AND)) {
            val operator = tokens.next()
            val right = equality()
            expr = Expr.Logical(expr, operator, right)
        }

        return expr
    }

    private fun equality(): Expr {
        var expr = comparison()

        while (tokens.match(BANG_EQUAL, EQUAL_EQUAL)) {
            val operator = tokens.next()
            val right = comparison()
            expr = Expr.Binary(expr, operator, right)
        }

        return expr
    }

    private fun comparison(): Expr {
        var expr = term()

        while (tokens.match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            val operator = tokens.next()
            val right = term()
            expr = Expr.Binary(expr, operator, right)
        }

        return expr
    }

    private fun term(): Expr {
        var expr = factor()

        while (tokens.match(MINUS, PLUS)) {
            val operator = tokens.next()
            val right = factor()
            expr = Expr.Binary(expr, operator, right)
        }

        return expr
    }

    private fun factor(): Expr {
        var expr = unary()

        while (tokens.match(SLASH, STAR)) {
            val operator = tokens.next()
            val right = unary()
            expr = Expr.Binary(expr, operator, right)
        }

        return expr
    }

    private fun unary(): Expr {
        if (tokens.match(BANG, MINUS)) {
            val operator = tokens.next()
            val right = unary()
            return Expr.Unary(operator, right)
        }

        return call()
    }

    private fun call(): Expr {
        var expr = primary()

        while (true) {
            if (tokens.matchAndConsume(LEFT_PAREN)) {
                expr = finishCall(expr)
            } else {
                break
            }
        }

        return expr
    }

    private fun finishCall(callee: Expr): Expr {
        val args =
            if (tokens.match(RIGHT_PAREN)) {
                listOf()
            } else {
                buildList {
                    var i = 0
                    do {
                        if (++i >= 255) {
                            Log.err { msg = "Cannot have more than 255 arguments" }
                            throw ParseError("Cannot have more than 255 arguments")
                        }
                        add(expression())

                    } while (tokens.matchAndConsume(COMMA))
                }
            }

        val paren = consume(RIGHT_PAREN, "Expected `${RIGHT_PAREN}` after arguments")
        return Expr.Call(callee, paren, args)
    }

    private fun primary(): Expr {
        val (token, pos) = tokens.peekSafe()

        return when (token) {
            is BOOL -> {
                tokens.next()
                Expr.Literal.Bool(token.value)
            }

            NIL -> {
                tokens.next()
                Expr.Literal.Nil
            }

            is NUMBER -> {
                tokens.next()
                Expr.Literal.Num(token.value)
            }

            is STRING -> {
                tokens.next()
                Expr.Literal.Str(token.value)
            }

            is IDENT -> {
                tokens.next()
                Expr.Variable(token)
            }

            LEFT_PAREN -> {
                tokens.next()
                val expr = expression()
                consume(RIGHT_PAREN, "Expected `${RIGHT_PAREN}` after expression")
                Expr.Grouping(expr)
            }

            else -> {
                Log.errAndThrow {
                    start = pos
                    msg = "Expected expression. Found `${token}`"
                }
            }
        }
    }

    // only use for TokenType that are objects
    private fun consume(type: TokenType, message: String): Token {
        val (peek, pos) = tokens.peekSafe()

        return if (peek == type) {
            tokens.next()
        } else {
            Log.errAndThrow {
                start = pos
                msg = message
            }
        }
    }

    private fun sync() {
        while (!tokens.end()) {
            if (tokens.next().type == SEMICOLON) return
            if (tokens.match(CLASS, FUN, VAR, FOR, IF, WHILE, PRINT, RETURN)) return

            tokens.next()
        }
    }

}
