interface ErrorLogger {
    var hadError: Boolean

    var position: Pos
    var msg: String
    var code: String

    fun err(): Boolean {
        if (!hadError) return false // no log

        val exceptionError = position.line == 0

        if (exceptionError) {
            eprintln(msg)
        } else {
            eprintln("${position.line} | $code")
            eprintln("^".padStart(position.line.digits() + 3 + position.char) + " $msg")
        }

        hadError = false
        return true // logged
    }

    fun reset() = this.apply {
        hadError = false
        position = Pos(0, 0)
        msg = ""
        code = ""
    }
}

/** Marker Error Class */
class ParseError(msg: String) : Exception(msg)
class TypeError(msg: String) : Exception(msg)
class InterpreterError(msg: String) : Exception(msg)

// should not be thrown anywhere
object AbruptEndError : Exception("End without ${TokenType.EOF.repr()}")
