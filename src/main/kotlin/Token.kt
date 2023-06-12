import Token.*

sealed class Token {

    // Single-character tokens.
    object LEFT_PAREN : Token()
    object RIGHT_PAREN : Token()
    object LEFT_BRACE : Token()
    object RIGHT_BRACE : Token()

    object COMMA : Token()
    object DOT : Token()
    object MINUS : Token()
    object PLUS : Token()
    object SEMICOLON : Token()
    object SLASH : Token()
    object STAR : Token()

    // One or two character tokens.
    object BANG : Token()
    object BANG_EQUAL : Token()

    object EQUAL : Token()
    object EQUAL_EQUAL : Token()

    object GREATER : Token()
    object GREATER_EQUAL : Token()

    object LESS : Token()
    object LESS_EQUAL : Token()

    // Literals.
    data class IDENTIFIER(val value: String) : Token()
    data class STRING(val value: String) : Token()
    data class NUMBER(val value: Double) : Token()

    // Keywords.
    object AND : Token()
    object CLASS : Token()
    object ELSE : Token()

    object FALSE : Token() {
        const val value = false
    }

    object FUN : Token()
    object FOR : Token()
    object IF : Token()

    object NIL : Token() {
        val value = null
    }

    object OR : Token()
    object PRINT : Token()
    object RETURN : Token()
    object SUPER : Token()
    object THIS : Token()

    object TRUE : Token() {
        const val value = true
    }

    object VAR : Token()
    object WHILE : Token()

    // Ignore
    object COMMENT : Token()
    object WHITESPACE : Token()
    object UNKNOWN : Token()

    object EOF : Token()
}

data class TokenPos(val token: Token, val pos: Pos)

operator fun Token.plus(pos: Pos) = TokenPos(this, pos)

fun PeekableIterator<TokenPos>.match(vararg tokens: Token) = tokens.any { it == peek()?.token }
fun PeekableIterator<TokenPos>.end() = (peek()?.token ?: throw ParseError("No EOF")) == EOF

class TokenScanner(val src: SrcReader) {
    val chars by lazy { src.chars().peekable() }

    fun scanTokens(): Sequence<TokenPos> = sequence {
        var lastPos = Pos(0, 0)

        while (chars.hasNext()) {
            val (curr, pos) = chars.next()
            lastPos = pos

            val token = when (curr) {
                '(' -> LEFT_PAREN
                ')' -> RIGHT_PAREN
                '{' -> LEFT_BRACE
                '}' -> RIGHT_BRACE
                ',' -> COMMA
                '.' -> DOT
                '-' -> MINUS
                '+' -> PLUS
                ';' -> SEMICOLON
                '*' -> STAR

                '!' -> if (match('=')) BANG_EQUAL else BANG
                '=' -> if (match('=')) EQUAL_EQUAL else EQUAL
                '<' -> if (match('=')) LESS_EQUAL else LESS
                '>' -> if (match('=')) GREATER_EQUAL else GREATER

                '/' -> if (match('/')) skipComment() else SLASH

                in digits -> scanNum(curr)
                in lowercase, in uppercase, '_' -> scanIdent(curr)
                '"' -> scanStr()

                ' ', '\r', '\t' -> skipWhitespace()

                else -> UNKNOWN
            }

            scanErr.apply { this.pos = pos }.log()

            when (token) {
                WHITESPACE, COMMENT -> {}

                UNKNOWN -> scanErr.apply {
                    hadError = true
                    this.pos = pos
                    msg = "Unexpected character"
                    code = src.currLine
                }.log()

                else -> yield(token + pos)
            }
        }

        yield(EOF + Pos(lastPos.line, lastPos.char + 1))
    }

    private fun match(c: Char): Boolean =
        if (chars.hasNext() && chars.peek()?.first == c) {
            chars.next()
            true
        } else {
            false
        }

    private fun skipComment(): Token {
        while (chars.hasNext() && chars.peek()?.second?.char != 0) {
            chars.next()
        }
        return COMMENT
    }

    // TODO add suport for method calls on literal. Add mulitple peeks.
    private fun scanNum(start: Char): Token {
        val num = buildString {
            append(start)

            while (chars.hasNext()) {
                val (char, _) = chars.peek() ?: break

                // check if fractional part exists
                if (char !in digits) {
                    if (char == '.') {
                        append(chars.next().first) // decimal point ??

                        while (chars.hasNext()) {
                            val (c, _) = chars.peek() ?: break
                            if (c !in digits) break

                            append(chars.next().first)
                        }
                    }

                    break
                }

                append(chars.next().first)
            }
        }

        return NUMBER(num.toDouble())
    }

    private fun scanIdent(start: Char): Token {
        val ident = buildString {
            append(start)

            while (chars.hasNext() && chars.peek()?.first in alphaNum) {
                append(chars.next().first)
            }
        }

        return keywords[ident] ?: IDENTIFIER(ident)
    }

    private fun scanStr(): Token {
        val str = buildString {
            while (chars.hasNext()) {
                val (char, pos) = chars.peek() ?: break

                if (char == '"') {
                    chars.next()
                    break
                }

                if (pos.char == 0) append('\n')
                append(chars.next().first)

            }
        }

        return if (chars.hasNext()) {
            STRING(str)
        } else {
            scanErr.apply {
                hadError = true
                msg = "Unterminated String"
                code = src.currLine
            }
            UNKNOWN
        }
    }

    private fun skipWhitespace(): Token {
        // check till new line pos.char will reset
        while (chars.hasNext()) {
            val c = chars.peek()?.first ?: break
            when (c) {
                ' ', '\r', '\t' -> chars.next()
                else -> break
            }
        }

        return WHITESPACE
    }

    companion object {
        val digits = '0'..'9'
        val lowercase = 'a'..'z'
        val uppercase = 'A'..'Z'
        val alphaNum = digits + lowercase + uppercase

        val keywords = mapOf(
            "and" to AND,
            "class" to CLASS,
            "else" to ELSE,
            "false" to FALSE,
            "for" to FOR,
            "fun" to FUN,
            "if" to IF,
            "nil" to NIL,
            "or" to OR,
            "print" to PRINT,
            "return" to RETURN,
            "super" to SUPER,
            "this" to THIS,
            "true" to TRUE,
            "var" to VAR,
            "while" to WHILE,
        )

        val scanErr = object : ErrorLogger {
            override var hadError: Boolean = false
            override var pos: Pos = Pos(0, 0)
            override var msg: String = ""
            override var code: String = ""
        }
    }
}

/*
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
*/
