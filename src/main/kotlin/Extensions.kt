import java.io.File
import kotlin.math.abs

/** Boolean to Nullable Boolean */
fun Boolean.orNull(): Boolean? = if (this) true else null

/** Print double without fractional part */
fun Double.str() = if (this == toInt().toDouble()) toInt().toString() else toString()

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
typealias CharPos = Pair<Char, Pos>

data class Pos(val line: Int, val char: Int) {
    override fun toString(): String = "$line:$char"
}

/** Check file */
class FileChecker(val file: File) {
    private var canRead = true
    fun canRead() {
        canRead = file.canRead()
    }

    private var exists = true
    fun exists() {
        exists = file.exists()
    }

    private var isFile = true
    fun isFile() {
        isFile = file.isFile
    }

    fun valid() {
        if (!canRead) throw FileException("File $file cannot be read")
        if (!exists) throw FileException("File $file does not exist")
        if (!isFile) throw FileException("$file is not a valid file")
    }
}

class FileException(msg: String) : Exception(msg)

fun File.checkAndRead(block: FileChecker.() -> Unit): List<String>? {
    try {
        FileChecker(this).apply {
            block()
            valid()
        }
    } catch (e: FileException) {
        eprintln(e.message ?: "")
        return null
    }

    return this.readLines()
}

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
