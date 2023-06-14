import java.io.File
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
        } catch (e: Exception) {
            eprintln(e.message ?: "Something went wrong")
            continue
        }
    }
}

object Log : ErrorLogger {
    override var hadError: Boolean = false
    override var position: Pos = Pos(0, 0)
        set(value) {
            field = value
            code = lines.getOrNull(value.line - 1) ?: ""
        }

    override var msg: String = ""
    override var code: String = ""

    var lines: List<String> = listOf()

    fun err(block: ErrorLogger.() -> Unit): Boolean {
        this.block()
        hadError = true
        return err()
    }
}
