package pt.isel.pc.problemsets.set3.ex3.utils

import java.io.Writer

fun Writer.sendLine(line: String) {
    appendLine(line)
    flush()
}
