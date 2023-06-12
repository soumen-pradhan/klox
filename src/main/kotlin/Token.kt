enum class Type {
    // Single-character tokens.
    LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE,
    COMMA, DOT, MINUS, PLUS, SEMICOLON, SLASH, STAR,

    // One or two character tokens.
    BANG, BANG_EQUAL,
    EQUAL, EQUAL_EQUAL,
    GREATER, GREATER_EQUAL,
    LESS, LESS_EQUAL,

    // Literals.
    IDENTIFIER, STRING, NUMBER,

    // Keywords.
    AND, CLASS, ELSE, FALSE, FUN, FOR, IF, NIL, OR,
    PRINT, RETURN, SUPER, THIS, TRUE, VAR, WHILE,

    // Ignore
    COMMENT, WHITESPACE, UNKNOWN,

    EOF
}

data class Token(
    val type: Type,
    var pos: Pos,
    val lexeme: String,
    val literal: Any? = null,
) {
    override fun toString(): String = "$type $lexeme ${literal ?: ""}"
}

fun PeekableIterator<Token>.match(vararg tokens: Type) = tokens.any { it == peek()?.type }
fun PeekableIterator<Token>.end() = (peek()?.type ?: throw ParseError("No EOF")) == Type.EOF

class TokenScanner(private val src: PeekableIterator<PosChar>) {
    var hadError = false

    fun scanTokens(): Sequence<Token> = sequence {
        var lastPos = Pos(-1, -1)

        while (src.hasNext()) {
            val (pos, curr) = src.next()
            lastPos = pos

            val token = when (curr) {
                '(' -> Token(Type.LEFT_PAREN, pos, "(")
                ')' -> Token(Type.RIGHT_PAREN, pos, ")")
                '{' -> Token(Type.LEFT_BRACE, pos, "{")
                '}' -> Token(Type.RIGHT_BRACE, pos, "}")
                ',' -> Token(Type.COMMA, pos, ",")
                '.' -> Token(Type.DOT, pos, ".")
                '-' -> Token(Type.MINUS, pos, "-")
                '+' -> Token(Type.PLUS, pos, "+")
                ';' -> Token(Type.SEMICOLON, pos, ";")
                '*' -> Token(Type.STAR, pos, "*")

                '!' -> if (match('=')) Token(Type.BANG_EQUAL, pos, "!=") else Token(Type.BANG, pos, "!")
                '=' -> if (match('=')) Token(Type.EQUAL_EQUAL, pos, "==") else Token(Type.EQUAL, pos, "=")
                '<' -> if (match('=')) Token(Type.LESS_EQUAL, pos, "<=") else Token(Type.LESS, pos, "<")
                '>' -> if (match('=')) Token(Type.GREATER_EQUAL, pos, ">=") else Token(Type.GREATER, pos, ">")

                '/' -> if (match('/')) skipComment(pos) else Token(Type.SLASH, pos, "/")

                in digits -> consumeNum(curr, pos)

                in lowercase, in uppercase, '_' -> consumeIdent(curr, pos)

                ' ', '\r', '\t' -> skipWhitespace(pos)

                '"' -> consumeStr(pos)

                else -> Token(Type.UNKNOWN, pos, curr.toString())
            }

            hadError = token.type == Type.UNKNOWN

            when (token.type) {
                Type.UNKNOWN -> logError(pos, "$curr: unexpected character")
                Type.WHITESPACE, Type.COMMENT -> {}
                else -> yield(token)
            }
        }

        yield(Token(Type.EOF, Pos(lastPos.line, lastPos.char + 1), "<EOF>"))
    }

    private fun match(c: Char) = if (src.hasNext() and (src.peek()?.second == c)) {
        src.next()
        true
    } else {
        false
    }

    private fun skipComment(pos: Pos): Token {
        while (src.hasNext() and (src.peek()?.first?.char != 0)) {
            src.next() // consume comment line
        }
        return Token(Type.COMMENT, pos, "")
    }

    private fun skipWhitespace(pos: Pos): Token {
        while (src.hasNext()) {
            val c = src.peek()?.second ?: break
            when (c) {
                ' ', '\r', '\t' -> src.next()
                else -> break
            }
        }
        return Token(Type.WHITESPACE, pos, "")
    }

    private fun consumeStr(pos: Pos): Token {
        val str = buildString {
            while (src.hasNext()) {
                val (p, char) = src.peek() ?: break
                if (char == '"') {
                    src.next()
                    break
                }

                if (p.char == 0) append('\n') // does not seem to work
                append(src.next().second)
            }
        }

        return if (src.hasNext()) {
            Token(Type.STRING, pos, str, str)
        } else {
            logError(pos, "Unterminated string")
            Token(Type.UNKNOWN, pos, "")
        }

    }

    // TODO add support for method calls on literals
    private fun consumeNum(start: Char, pos: Pos): Token {
        val str = buildString {
            append(start)
            while (src.hasNext()) {
                val (p, char) = src.peek() ?: break

                if (char !in digits) { // check fractional part
                    if (char == '.') {
                        append(src.next().second)

                        while (src.hasNext()) {
                            val (_, c) = src.peek() ?: break
                            if (c !in digits) break
                            append(src.next().second)
                        }
                    }

                    break
                }

                append(src.next().second)
            }
        }

        return Token(Type.NUMBER, pos, str, str.toDouble())
    }

    private fun consumeIdent(start: Char, pos: Pos): Token {
        val str = buildString {
            append(start)

            while (src.hasNext() and (src.peek()?.second in alphaNum)) {
                append(src.next().second)
            }
        }

        val type = idents[str]

        return if (type != null) {
            when (type) {
                Type.FALSE -> Token(type, pos, str, false)
                Type.NIL -> Token(type, pos, str, null)
                Type.TRUE -> Token(type, pos, str, true)
                else -> Token(type, pos, str)
            }
        } else {
            Token(Type.IDENTIFIER, pos, str)
        }
    }

    companion object {
        val digits = '0'..'9'
        val lowercase = 'a'..'z'
        val uppercase = 'A'..'Z'
        val alphaNum = digits + lowercase + uppercase

        val idents = mapOf(
            "and" to Type.AND,
            "class" to Type.CLASS,
            "else" to Type.ELSE,
            "false" to Type.FALSE,
            "for" to Type.FOR,
            "fun" to Type.FUN,
            "if" to Type.IF,
            "nil" to Type.NIL,
            "or" to Type.OR,
            "print" to Type.PRINT,
            "return" to Type.RETURN,
            "super" to Type.SUPER,
            "this" to Type.THIS,
            "true" to Type.TRUE,
            "var" to Type.VAR,
            "while" to Type.WHILE,
        )
    }
}
