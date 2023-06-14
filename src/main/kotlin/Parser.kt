import TokenType.*

/**
 * program    → decl* EOF
 * decl       → varDecl | statement
 * varDecl    → "var" IDENT ( "=" expression )? ";"
 * statement  → exprStmt | printStmt
 * exprStmt   → expression ";"
 * printStmt  → "print" expression ";"
 * expression → assignment
 * assignment → IDENT "=" assignment | equality
 * equality   → comparison ( ( "!=" | "==" ) comparison )*
 * comparison → term ( ( ">" | ">=" | "<" | "<=" ) term )*
 * term       → factor ( ( "-" | "+" ) factor )*
 * factor     → unary ( ( "/" | "*" ) unary )*
 * unary      → ( "!" | "-" ) unary | primary
 * primary    → NUMBER | STRING | "true" | "false" | "nil" | "(" expression ")" | IDENT
 */

class Parser(private val tokens: PeekableIterator<Token>) {

    fun parse(): Iterator<Stmt> = iterator {
        while (!tokens.end()) {
            val decl = declaration() ?: continue
            yield(decl)
        }
    }

    private fun declaration(): Stmt? {
        val beginToken = tokens.peek() ?: throw AbruptEndError

        return try {
            if (beginToken.type == VAR) {
                tokens.next()
                valDecl()
            } else {
                statement()
            }
        } catch (_: ParseError) {
            sync()
            null
        }
    }

    private fun valDecl(): Stmt {
        val (peek, pos) = tokens.peek() ?: throw AbruptEndError

        val name = if (peek is IDENTIFIER) {
            tokens.next()
            peek
        } else {
            Log.err {
                position = pos
                msg = "Expected variable name"
            }
            throw ParseError("Expected variable name")
        }

        val init = if (tokens.peek()?.type == EQUAL) {
            tokens.next()
            expression()
        } else {
            Expr.Literal.Nothing
        }

        consume(SEMICOLON, "Expected `${SEMICOLON.repr()}` after variable declaration")
        return Stmt.Var(name, init)
    }

    private fun statement(): Stmt {
        val beginToken = tokens.peek() ?: throw AbruptEndError

        return if (beginToken.type == PRINT) {
            tokens.next()
            val expr = expression()
            consume(SEMICOLON, "Expected `;` after statement")
            Stmt.Print(expr)
        } else {
            val expr = expression()
            consume(SEMICOLON, "Expected `;` after statement")
            Stmt.Expression(expr)
        }
    }

    private fun expression(): Expr = assignment()

    private fun assignment(): Expr {
        val expr = equality()

        if (!tokens.end() && tokens.peek()?.type == EQUAL) {
            val equals = tokens.next()
            val value = assignment()

            if (expr is Expr.Variable) {
                val name = expr.name
                return Expr.Assign(name, value)
            }

            Log.err {
                position = equals.pos
                msg = "Invalid assignment target"
            }
        }

        return expr
    }

    private fun equality(): Expr {
        var expr = comparison()

        while (!tokens.end() && tokens.match(BANG_EQUAL, EQUAL_EQUAL)) {
            val operator = tokens.next()
            val right = comparison()
            expr = Expr.Binary(expr, operator, right)
        }

        return expr
    }

    private fun comparison(): Expr {
        var expr = term()

        while (!tokens.end() && tokens.match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            val operator = tokens.next()
            val right = term()
            expr = Expr.Binary(expr, operator, right)
        }

        return expr
    }

    private fun term(): Expr {
        var expr = factor()

        while (!tokens.end() && tokens.match(MINUS, PLUS)) {
            val operator = tokens.next()
            val right = factor()
            expr = Expr.Binary(expr, operator, right)
        }

        return expr
    }

    private fun factor(): Expr {
        var expr = unary()

        while (!tokens.end() && tokens.match(SLASH, STAR)) {
            val operator = tokens.next()
            val right = unary()
            expr = Expr.Binary(expr, operator, right)
        }

        return expr
    }

    private fun unary(): Expr {
        if (!tokens.end() && tokens.match(BANG, MINUS)) {
            val operator = tokens.next()
            val right = unary()
            return Expr.Unary(operator, right)
        }

        return primary()
    }

    private fun primary(): Expr {
        val (token, pos) = tokens.peek() ?: throw AbruptEndError

        return when (token) {
            is BOOL -> {
                tokens.next()
                Expr.Literal.Bool(token.value)
            }

            NIL -> {
                tokens.next()
                Expr.Literal.Nothing
            }

            is NUMBER -> {
                tokens.next()
                Expr.Literal.Num(token.value)
            }

            is STRING -> {
                tokens.next()
                Expr.Literal.Str(token.value)
            }

            is IDENTIFIER -> {
                tokens.next()
                Expr.Variable(token)
            }

            LEFT_PAREN -> {
                tokens.next()
                val expr = expression()
                consume(RIGHT_PAREN, "Expected `)` after expression")
                Expr.Grouping(expr)
            }

            else -> {
                Log.err {
                    position = pos
                    msg = "Expected expression. Found `${token.repr()}`"
                }
                throw ParseError("not primary token")
            }
        }
    }

    // only use for TokenType that are objects
    private fun consume(type: TokenType, message: String): Token {
        val (peek, pos) = tokens.peek() ?: throw AbruptEndError

        return if (peek == type) {
            tokens.next()
        } else {
            Log.err {
                position = pos
                msg = message
            }
            throw ParseError(message)
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
