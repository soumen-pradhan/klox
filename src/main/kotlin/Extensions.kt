/** Boolean to Nullable Boolean */
fun Boolean.orNull(): Boolean? = if (this) true else null

fun eprintln(string: String) = System.err.println(string)

fun logError(pos: Pos, msg: String) {
    eprintln("Error [$pos]: $msg")
}

fun logError(token: Token, msg: String) {
    eprintln("Error [${token.pos}]: $msg. Found ${token.lexeme}")
}

data class Pos(val line: Int, val char: Int) {
    override fun toString(): String = "$line:$char"
}

typealias PosChar = Pair<Pos, Char>

/** Peek into a Iterator without consuming it */
class PeekableIterator<T : Any>(private var iter: Iterator<T>) : Iterator<T> {
    private var peekedValue: T? = null

    init {
        if (iter.hasNext()) {
            peekedValue = iter.next()
        }
    }

    override fun hasNext(): Boolean =
        peekedValue != null || iter.hasNext()

    override fun next(): T {
        val ret = peekedValue ?: throw NoSuchElementException()
        peekedValue = if (iter.hasNext()) iter.next() else null

        return ret
    }

    fun peek(): T? = peekedValue
}

fun <T : Any> Iterator<T>.peekable() = PeekableIterator(this)
fun <T : Any> Sequence<T>.peekable() = iterator().peekable()
