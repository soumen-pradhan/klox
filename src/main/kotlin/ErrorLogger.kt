interface ErrorLogger {
    var hadError: Boolean

    var pos: Pos
    var msg: String
    var code: String

    fun log(): Boolean {
        if (!hadError) return false // no log

        eprintln("${pos.line} | $code")
        eprintln("^".padStart(pos.line.digits() + 3 + pos.char) + " $msg")

        hadError = false
        return true // logged
    }
}
