import java.io.File
import kotlin.math.abs

/** Boolean to Nullable Boolean */
// fun Boolean.orNull(): Boolean? = if (this) true else null

/** Print double without fractional part */
fun Double.repr() = if (this == toInt().toDouble()) toInt().toString() else toString()

/** Count digits of an Int */
fun Int.digits(): Int {
    if (this == 0) return 1

    var count = 0
    var n = abs(this)

    while (n > 0) {
        n /= 10
        ++count
    }

    return count
}

fun eprintln(string: String) = System.err.println(string)

/** Position at file source code */
data class CharPos(val char: Char, val pos: Pos)

data class Pos(val line: Int, val char: Int) {
    override fun toString(): String = "$line:$char"
}

/** Check file */
class FileException(msg: String) : Exception(msg)

infix fun Boolean.or(msg: String) = if (this) this else throw FileException(msg)

fun File.checkAndRead(block: File.() -> Unit): List<String>? {
    try {
        block()
    } catch (e: FileException) {
        eprintln(e.message ?: "")
        return null
    }

    return readLines()
}

/** Peek into an Iterator without consuming it */
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

fun PeekableIterator<Token>.match(vararg tokens: TokenType) = tokens.any { it == peek()?.type }
fun PeekableIterator<Token>.end() = (peek()?.type ?: throw AbruptEndError) == TokenType.EOF

/** Invert a map */
fun <K, V> Map<K, V>.invert() =
    entries.associate { (key, value) -> value to key }

/**  */
fun <T> List<T>.getOrNull(index: Int): T? = try {
    this[index]
} catch (e: IndexOutOfBoundsException) {
    null
}
