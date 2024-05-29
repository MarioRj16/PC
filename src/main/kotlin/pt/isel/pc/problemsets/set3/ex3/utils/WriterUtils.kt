package pt.isel.pc.problemsets.set3.ex3.utils

import suspendWrite
import java.io.Writer
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel

fun Writer.sendLine(line: String) {
    appendLine(line)
    flush()
}

suspend fun AsynchronousSocketChannel.suspendWriteLn(buff:ByteBuffer){
    this.suspendWrite(buff)
    this.suspendWrite(ByteBuffer.wrap("\n".toByteArray()))
}