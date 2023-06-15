/**
 * program    → decl* EOF
 * decl       → varDecl | statement
 * varDecl    → "var" IDENT ( "=" expression )? ";"
 * statement  → exprStmt | ifStmt | printStmt | block
 * exprStmt   → expression ";"
 * ifStmt     → "if" expression "{" statement "}" ( "else" "{" statement "}" )?
 * printStmt  → "print" expression ";"
 * block      → "(" decl* ")"
 * expression → assignment
 * assignment → IDENT "=" assignment | equality
 * equality   → comparison ( ( "!=" | "==" ) comparison )*
 * comparison → term ( ( ">" | ">=" | "<" | "<=" ) term )*
 * term       → factor ( ( "-" | "+" ) factor )*
 * factor     → unary ( ( "/" | "*" ) unary )*
 * unary      → ( "!" | "-" ) unary | primary
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
        val beginToken = tokens.peek() ?: throw AbruptEndError

        return try {
            if (beginToken.type == VAR) {
                tokens.next()
                varDecl()
            } else {
                statement()
            }
        } catch (_: ParseError) {
            // Log.err(e.message ?: "")
            sync()
            null
        }
    }

    private fun varDecl(): Stmt {
        val (peek, pos) = tokens.peek() ?: throw AbruptEndError

        val name = if (peek is IDENTIFIER) {
            tokens.next()
            peek
        } else {
            Log.err {
                start = pos
                end = pos + Pos(0, peek.repr().length)
                msg = "Expected a variable"
            }
            throw ParseError("Expected a variable")
        }

        val init = if (tokens.peek()?.type == EQUAL) {
            tokens.next()
            expression()
        } else {
            Expr.Literal.Nothing
        }

        // TODO Bug - When semicolon is missing at end, the next line is logged
        consume(SEMICOLON, "Expected `${SEMICOLON.repr()}` after variable `${name.value}` declaration")

        return Stmt.Var(name, init)
    }

    private fun statement(): Stmt {
        val beginToken = tokens.peek() ?: throw AbruptEndError

        return when (beginToken.type) {
            PRINT -> {
                tokens.next()
                val expr = expression()
                consume(SEMICOLON, "Expected `;` after statement")
                Stmt.Print(expr)
            }

            IF -> {
                tokens.next()
                ifStatement()
            }

            LEFT_BRACE -> {
                tokens.next()
                Stmt.Block(block())
            }

            else -> {
                val expr = expression()
                consume(SEMICOLON, "Expected `;` after statement")
                Stmt.Expression(expr)
            }
        }
    }

    private fun ifStatement(): Stmt {
        consume(LEFT_PAREN, "Expected `${LEFT_PAREN.repr()}` after condition")
        val cond = expression()
        consume(RIGHT_PAREN, "Expected `${RIGHT_PAREN.repr()}` after block")

        val thenBranch = statement()
        val elseBranch =
            if (!tokens.end() && tokens.peek()?.type == ELSE) {
                tokens.next()
                statement()
            } else null

        return Stmt.If(cond, thenBranch, elseBranch)
    }

    private fun block(): List<Stmt> {
        val statements = buildList {
            while (!tokens.end() && tokens.peek()?.type != RIGHT_BRACE) {
                val decl = declaration() ?: continue
                add(decl)
            }
        }

        consume(RIGHT_BRACE, "Expected ${RIGHT_BRACE.repr()} after block")
        return statements
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
                start = equals.pos
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
                    start = pos
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
                start = pos
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
