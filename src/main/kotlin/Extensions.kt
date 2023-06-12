import kotlin.math.abs

/** Boolean to Nullable Boolean */
fun Boolean.orNull(): Boolean? = if (this) true else null

/** Print double without fractional part */
fun Double.str() = if (this == toInt().toDouble()) toInt().toString() else toString()

/** Count digits of an Int */
fun Int.digits(): Int {
    if (this == 0) return 1

    var l = 0
    var n = abs(this)

    while (n > 0) {
        n /= 10
        ++l
    }

    return l
}

fun eprintln(string: String) = System.err.println(string)

fun logError(pos: Pos, msg: String, code: String = "") {
    eprintln("${pos.line} | $code")
    eprintln("^".padStart(pos.line.digits() + 3 + pos.char) + " $msg")
}

fun logError(tokenPos: TokenPos, msg: String, code: String = "") {
    val (token, pos) = tokenPos
    eprintln("${pos.line} | $code")
    eprintln("^".padStart(pos.line.digits() + 3 + pos.char) + " $msg. Found $token")
}

data class Pos(val line: Int, val char: Int) {
    override fun toString(): String = "$line:$char"
}

typealias CharPos = Pair<Char, Pos>

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

/** Marker Error Class */
class ParseError(msg: String) : RuntimeException(msg)
