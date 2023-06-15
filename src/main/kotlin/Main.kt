import java.io.File
import kotlin.math.max
import kotlin.system.exitProcess

fun main() {
    // only < 2GB files
    val file = File("src/test/resources/expr.lox")

    val lines = file.checkAndRead {
        exists() or "$this does not exist"
        isFile or "$this is not a file"
        canRead() or "$this cannot be read"
    } ?: exitProcess(64)

    Log.lines = lines

    val interpreter = Interpreter()

    val tokeniser = TokenScanner(lines)
    val tokens = tokeniser.scanTokens().peekable()

    val stmts = Parser(tokens).parse()

    for (s in stmts) {
        try {
            interpreter.interpret(s)
            // println("> ${s.ast()}")
        } catch (e: Exception) {
            eprintln(e.message ?: "Something went wrong")
            continue
        }
    }
}

object Log : ErrorLogger {

    override var start: Pos = Pos(0, 0)
        set(value) {
            field = value
            end = value
        }

    override var end: Pos = Pos(0, 0)
        set(value) {
            field = max(start, value)
        }

    override var msg: String = ""

    var lines: List<String> = listOf()

    override fun err(block: ErrorLogger.() -> Unit) {
        this.block()

        val width = end.line.digits()
        val linePad = " ".repeat(width) + " | "

        // single line
        if (start.line == end.line) {
            val padding = " ".repeat(start.char - 1) +
                    "^".repeat(max(end.char - start.char, 1))

            eprintln("${start.line} | ${lines.getOrNull(start.line - 1) ?: ""}")
            eprintln("$linePad$padding $msg")

            return
        }

        // multiple lines
        run {
            val firstLine = lines.getOrNull(start.line - 1) ?: ""
            val padding = " ".repeat(start.char - 1) +
                    "^".repeat(firstLine.length - start.char + 1)

            eprintln("${start.line fmt width} | $firstLine")
            eprintln("$linePad$padding")
        }

        for (line in (start.line + 1) until end.line) {
            val codeLine = lines.getOrNull(line - 1) ?: ""

            eprintln("${line fmt width} | $codeLine")
            eprintln(linePad + "^".repeat(codeLine.length))
        }

        eprintln("${end.line fmt width} | ${lines.getOrNull(end.line - 1) ?: ""}")
        eprintln(linePad + "^".repeat(end.char) + " $msg")
    }
}
