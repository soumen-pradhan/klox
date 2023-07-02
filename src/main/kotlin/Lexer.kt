import TokenType.*

sealed class TokenType(private val repr: String) {

    // Single-character tokens.
    object LEFT_PAREN : TokenType("(")
    object RIGHT_PAREN : TokenType(")")
    object LEFT_BRACE : TokenType("{")
    object RIGHT_BRACE : TokenType("}")

    object COMMA : TokenType(",")
    object DOT : TokenType(".")
    object MINUS : TokenType("-")
    object PLUS : TokenType("+")
    object SEMICOLON : TokenType(";")
    object SLASH : TokenType("/")
    object STAR : TokenType("*")

    // One or two character tokens.
    object BANG : TokenType("!")
    object BANG_EQUAL : TokenType("!=")
    object EQUAL : TokenType("=")
    object EQUAL_EQUAL : TokenType("==")
    object GREATER : TokenType(">")
    object GREATER_EQUAL : TokenType(">=")
    object LESS : TokenType("<")
    object LESS_EQUAL : TokenType("<=")

    // Literals.
    class IDENT(val value: String) : TokenType(value)
    class STRING(val value: String) : TokenType(value)
    class NUMBER(val value: Double) : TokenType(value.stringify())

    // Keywords.
    object AND : TokenType("and")
    object CLASS : TokenType("class")
    object ELSE : TokenType("else")
    object FUN : TokenType("fun")
    object FOR : TokenType("for")
    object IF : TokenType("if")
    object NIL : TokenType("nil")
    object OR : TokenType("or")
    object PRINT : TokenType("print")
    object RETURN : TokenType("return")
    object SUPER : TokenType("super")
    object THIS : TokenType("this")
    object VAR : TokenType("var")
    object WHILE : TokenType("while")

    class BOOL(val value: Boolean) : TokenType(value.toString())

    // Ignore
    object COMMENT : TokenType("")
    object WHITESPACE : TokenType("")
    object UNKNOWN : TokenType("")

    object EOF : TokenType("<EOF>")

    override fun toString() = repr

    companion object {
        val keywords = mapOf(
            AND to "and",
            CLASS to "class",
            ELSE to "else",
            BOOL(false) to "false",
            FOR to "for",
            FUN to "fun",
            IF to "if",
            NIL to "nil",
            OR to "or",
            PRINT to "print",
            RETURN to "return",
            SUPER to "super",
            THIS to "this",
            BOOL(true) to "true",
            VAR to "var",
            WHILE to "while",
        )

        val specials = mapOf(
            // Single-character tokens.
            LEFT_PAREN to "(", RIGHT_PAREN to ")", LEFT_BRACE to "{", RIGHT_BRACE to "}",
            COMMA to ",", DOT to ".", MINUS to "-", PLUS to "+", SEMICOLON to ";", SLASH to "/", STAR to "*",

            // One or two character tokens.
            BANG to "!", BANG_EQUAL to "!=", EQUAL to "=", EQUAL_EQUAL to "==",
            GREATER to ">", GREATER_EQUAL to ">=", LESS to "<", LESS_EQUAL to "<=",

            EOF to "<EOF>"
        )
    }
}

data class Token(val type: TokenType, val pos: Pos)

operator fun TokenType.plus(pos: Pos) = Token(this, pos)

class TokenScanner(lines: List<String>) {
    @Suppress("unused")
    var scanError = false

    private val chars: PeekableIterator<CharPos> = lines.asSequence().withIndex()
        .flatMap { (lineIdx, line) ->
            line.withIndex()
                .map { (charIdx, char) ->
                    CharPos(char, Pos(lineIdx + 1, charIdx + 1))
                }
        }.peekable()

    fun scanTokens(): Iterator<Token> = iterator {
        var lastPos = Pos(0, 0)

        while (chars.hasNext()) {
            val (curr, pos) = chars.next()

            Log.start = pos
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

                '!' -> if (matchAndConsume('=')) BANG_EQUAL else BANG
                '=' -> if (matchAndConsume('=')) EQUAL_EQUAL else EQUAL
                '<' -> if (matchAndConsume('=')) LESS_EQUAL else LESS
                '>' -> if (matchAndConsume('=')) GREATER_EQUAL else GREATER

                '/' -> if (matchAndConsume('/')) skipComment() else SLASH

                in digits -> scanNum(curr)
                in lowercase, in uppercase, '_' -> scanIdent(curr)
                '"' -> scanStr()

                ' ', '\r', '\t', '\n' -> skipWhitespace()

                else -> UNKNOWN
            }

            when (token) {
                WHITESPACE, COMMENT -> {}

                UNKNOWN -> Log.err {
                    start = pos
                    msg = "Unexpected character"
                }

                else -> yield(token + pos)
            }
        }

        yield(EOF + Pos(lastPos.line, lastPos.char + 1))
    }

    private fun matchAndConsume(c: Char): Boolean =
        if (chars.hasNext() && chars.peek()?.char == c) {
            chars.next()
            true
        } else {
            false
        }

    private fun skipComment(): TokenType {
        while (chars.hasNext() && chars.peek()?.pos?.char != 1) {
            chars.next()
        }
        return COMMENT
    }

    // TODO add support for method calls on literal. Add multiple peeks.
    private fun scanNum(start: Char): TokenType {
        val num = buildString {
            append(start)

            while (chars.hasNext()) {
                val (char, _) = chars.peek() ?: break

                // check if fractional part exists
                if (char !in digits) {
                    if (char == '.') {
                        append(chars.next().char) // decimal point ??

                        while (chars.hasNext()) {
                            val (c, _) = chars.peek() ?: break
                            if (c !in digits) break

                            append(chars.next().char)
                        }
                    }

                    break
                }

                append(chars.next().char)
            }
        }

        return NUMBER(num.toDouble())
    }

    private fun scanIdent(start: Char): TokenType {
        val ident = buildString {
            append(start)

            while (chars.hasNext() && chars.peek()?.char in alphaNum) {
                append(chars.next().char)
            }
        }

        return keywords[ident] ?: IDENT(ident)
    }

    private fun scanStr(): TokenType {
        val str = buildString {
            while (chars.hasNext()) {
                val (char, pos) = chars.peek() ?: break

                Log.end = pos

                if (char == '"') {
                    chars.next()
                    break
                }

                if (pos.char == 1) append("\n")
                append(chars.next().char)
            }
        }

        return if (chars.hasNext()) {
            STRING(str)
        } else {
            Log.err { msg = "Unterminated String" }
            UNKNOWN
        }
    }

    private fun skipWhitespace(): TokenType {
        // check till new line pos.char will reset
        while (chars.hasNext()) {
            val c = chars.peek()?.char ?: break
            when (c) {
                ' ', '\r', '\t', '\n' -> chars.next()
                else -> break
            }
        }

        return WHITESPACE
    }

    companion object {
        val digits = '0'..'9'
        val lowercase = 'a'..'z'
        val uppercase = 'A'..'Z'
        val alphaNum = digits + lowercase + uppercase + '_'

        val keywords = TokenType.keywords.invert()
    }
}
