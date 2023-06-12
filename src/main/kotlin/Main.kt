import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import kotlin.system.exitProcess

fun main() {
    // only 2GB files
    val file = File("src/test/resources/sample.lox")
    val lines = file.checkAndRead {
        canRead()
        exists()
        isFile()
    } ?: exitProcess(64)

    val tokeniser = TokenScanner(lines)

    for ((token, pos) in tokeniser.scanTokens()) {
//        println("[$pos] $token")
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

}

//fun File.checkAndRead(): SrcReader? {
//    exists().orNull() ?: return null
//    canRead().orNull() ?: return null
//
//    return SrcReader(this)
//}

class SrcReader(val file: File) {
    private val reader = BufferedReader(FileReader(file))
    var currLine: String = ""

    fun chars(): Sequence<CharPos> = reader.lineSequence().withIndex()
        .flatMap { (lineIdx, line) ->
            line.also { currLine = it }.withIndex()
                .map { (charIdx, char) ->
                    char to Pos(lineIdx + 1, charIdx + 1)
                }
        }

    fun close() {
        reader.close()
    }
}
