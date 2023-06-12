import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.util.*
import kotlin.system.exitProcess

fun main() {

    val file = File("src/test/resources/sample.lox")
    val src = file.checkAndRead() ?: exitProcess(64)

    val tokeniser = TokenScanner(src)

    for ((token, pos) in tokeniser.scanTokens()) {
        println("[$pos] $token")
    }

//    for ((token, pos) in tokeniser.scanTokens()) {
//        println("[$pos] $token")
//    }
//
//    val expr = Parser(tokeniser.scanTokens().peekable()).parse() ?: return
//
//    if (tokeniser.hadError) return
//
//    println(expr.print())

    src.close()
}

class SrcReader(file: File) {
    private val reader = BufferedReader(FileReader(file))
    var currLine: String = ""

    fun chars(): Sequence<CharPos> = reader.lineSequence().withIndex()
        .flatMap { (lineIdx, line) ->
            currLine = line // save it for error

            line.asSequence().withIndex()
                .map { (charIdx, char) ->
                    char to Pos(lineIdx + 1, charIdx + 1)
                }
        }

    fun close() {
        reader.close()
    }
}

fun File.checkAndRead(): SrcReader? {
    exists().orNull() ?: return null
    canRead().orNull() ?: return null

    return SrcReader(this)
}

fun runFile(path: String) {
    val bytes = File(path).readBytes() //limited 2GB
    run(String(bytes, Charset.defaultCharset()))
}

fun runPrompt() {
    val input = InputStreamReader(System.`in`)
    val reader = BufferedReader(input)

    while (true) {
        print("> ")
        val line = reader.readLine() ?: break
        run(line)
    }
}

fun run(src: String) {
    val tokens = Scanner(src).tokens() ?: return

    for (token in tokens) {
        println(token)
    }
}