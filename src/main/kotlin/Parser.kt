/*import Type.*

/**
expression → equality ;
equality   → comparison ( ( "!=" | "==" ) comparison )* ;
comparison → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
term       → factor ( ( "-" | "+" ) factor )* ;
factor     → unary ( ( "/" | "*" ) unary )* ;
unary      → ( "!" | "-" ) unary | primary ;
primary    → NUMBER | STRING | "true" | "false" | "nil" | "(" expression ")" ;
 */

class Parser(val tokens: PeekableIterator<Token>) {

    fun parse(): Expr? = try {
        expression()
    } catch (e: ParseError) {
        null
    }

    fun expression(): Expr = equality()

    fun equality(): Expr {
        var expr = comparison()

        while (!tokens.end() && tokens.match(BANG_EQUAL, EQUAL_EQUAL)) {
            val operator = tokens.next()
            val right = comparison()
            expr = Expr.Binary(expr, operator, right)
        }

        return expr
    }

    fun comparison(): Expr {
        var expr = term()

        while (!tokens.end() && tokens.match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            val operator = tokens.next()
            val right = term()
            expr = Expr.Binary(expr, operator, right)
        }

        return expr
    }

    fun term(): Expr {
        var expr = factor()

        while (!tokens.end() && tokens.match(MINUS, PLUS)) {
            val operator = tokens.next()
            val right = factor()
            expr = Expr.Binary(expr, operator, right)
        }

        return expr
    }

    fun factor(): Expr {
        var expr = unary()

        while (!tokens.end() && tokens.match(SLASH, STAR)) {
            val operator = tokens.next()
            val right = unary()
            expr = Expr.Binary(expr, operator, right)
        }

        return expr
    }

    fun unary(): Expr {
        if (!tokens.end() && tokens.match(BANG, MINUS)) {
            val operator = tokens.next()
            val right = unary()
            return Expr.Unary(operator, right)
        }

        return primary()
    }

    fun primary(): Expr {
        // TODO handle no more tokens case
        val token = tokens.peek() ?: throw ParseError("incomplete tokens")

        return when (token.type) {
            FALSE -> {
                tokens.next()
                Expr.Literal(false)
            }

            TRUE -> {
                tokens.next()
                Expr.Literal(true)
            }

            NIL -> {
                tokens.next()
                Expr.Literal(null)
            }

            NUMBER, STRING -> Expr.Literal(tokens.next().literal)

            LEFT_PAREN -> {
                tokens.next()
                val expr = expression()
                consume(RIGHT_PAREN, "Expected ')' after expression")
                Expr.Grouping(expr)
            }

            else -> {
                logError(token, "Expected expression")
                throw ParseError("not primary token")
            }
        }
    }

    fun consume(type: Type, msg: String): Token {
        val peek = tokens.peek() ?: throw ParseError("incomplete tokens")

        return if (peek.type == type) {
            tokens.next()
        } else {
            logError(peek, msg)
            throw ParseError("expected $peek")
        }
    }

    fun sync() {
        while (!tokens.end()) {
            if (tokens.next().type == SEMICOLON) return
            if (tokens.match(CLASS, FUN, VAR, FOR, IF, WHILE, PRINT, RETURN)) return

            tokens.next()
        }
    }

}

*/
