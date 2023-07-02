interface ErrorLogger {
    var start: Pos
    var end: Pos

    var msg: String

    fun err(message: String) = eprintln(message)
    fun err(block: ErrorLogger.() -> Unit)
}

/** Marker Error Class */
class ParseError(msg: String) : Exception(msg)
class TypeError(msg: String) : Exception(msg)
class InterpreterError(msg: String) : Exception(msg)

// should not be thrown anywhere
object AbruptEndError : Exception("End without ${TokenType.EOF}")
