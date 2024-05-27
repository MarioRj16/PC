package pt.isel.pc.problemsets.set3

import kotlinx.coroutines.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import suspendAccept
import suspendRead
import suspendWrite
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel


class AsyncSocketChannelExtensionsTest {

    @Test
    fun `test accept connection`() = runBlocking {
        val server = AsynchronousServerSocketChannel.open().bind(InetSocketAddress(0))
        val port = (server.localAddress as InetSocketAddress).port

        val serverJob = launch {
            val clientChannel = server.suspendAccept()
            clientChannel.close()
        }

        val clientChannel = AsynchronousSocketChannel.open()
        clientChannel.connect(InetSocketAddress("localhost", port)).get()
        clientChannel.close()

        serverJob.join()
        server.close()
    }

    @Test
    fun `test read from socket`() = runBlocking {
        val server = AsynchronousServerSocketChannel.open().bind(InetSocketAddress(0))
        val port = (server.localAddress as InetSocketAddress).port

        val message = "Hello, world!"
        val buffer = ByteBuffer.allocate(1024)

        val serverJob = launch {
            val clientChannel = server.suspendAccept()
            clientChannel.write(ByteBuffer.wrap(message.toByteArray())).get()
            clientChannel.close()
        }

        val clientChannel = AsynchronousSocketChannel.open()
        clientChannel.connect(InetSocketAddress("localhost", port)).get()
        clientChannel.suspendRead(buffer)
        buffer.flip()

        val receivedMessage = String(buffer.array(), buffer.position(), buffer.remaining())
        assertEquals(message, receivedMessage)

        clientChannel.close()
        serverJob.join()
        server.close()
    }

    @Test
    fun `test write to socket`() = runBlocking {
        val server = AsynchronousServerSocketChannel.open().bind(InetSocketAddress(0))
        val port = (server.localAddress as InetSocketAddress).port

        val message = "Hello, world!"
        val buffer = ByteBuffer.wrap(message.toByteArray())

        val serverJob = launch {
            val clientChannel = server.suspendAccept()
            val readBuffer = ByteBuffer.allocate(1024)
            clientChannel.read(readBuffer).get()
            readBuffer.flip()

            val receivedMessage = String(readBuffer.array(), readBuffer.position(), readBuffer.remaining())
            assertEquals(message, receivedMessage)

            clientChannel.close()
        }

        val clientChannel = AsynchronousSocketChannel.open()
        clientChannel.connect(InetSocketAddress("localhost", port)).get()
        clientChannel.suspendWrite(buffer)
        clientChannel.close()

        serverJob.join()
        server.close()
    }

    @Test
    fun `test cancellation during accept`() = runBlocking {
        val server = AsynchronousServerSocketChannel.open().bind(InetSocketAddress(0))

        val job = launch {
            try{
                    server.suspendAccept()
            }catch (e: CancellationException){
                assertEquals(1, 1)
            }catch (e: Exception){
                //ONLY CANCELLATION EXCEPTION SHOULD BE THROWN
                assertEquals(0, 1)
            }

        }

        delay(100) // Ensure the accept call is suspended
        job.cancelAndJoin() // Cancel and wait for completion
        server.close()
    }

    @Test
    fun `test cancellation during read`() = runBlocking {
        val server = AsynchronousServerSocketChannel.open().bind(InetSocketAddress(0))
        val port = (server.localAddress as InetSocketAddress).port

        val serverJob = launch {
            val clientChannel = server.suspendAccept()
            delay(100) // Simulate delay before reading
            clientChannel.close()
        }

        val clientChannel = AsynchronousSocketChannel.open()
        clientChannel.connect(InetSocketAddress("localhost", port)).get()

        val readJob = launch {
            val buffer = ByteBuffer.allocate(1024)
            try{
                clientChannel.suspendRead(buffer)
            }catch (e: CancellationException){
                assertEquals(1, 1)
            }catch (e: Exception){
                //ONLY CANCELLATION EXCEPTION SHOULD BE THROWN
                assertEquals(0, 1)
            }
        }

        delay(100) // Ensure the read call is suspended
        readJob.cancelAndJoin() // Cancel and wait for completion
        clientChannel.close()
        serverJob.join()
        server.close()
    }

    @Test
    fun `test cancellation during write`() = runBlocking {
        val server = AsynchronousServerSocketChannel.open().bind(InetSocketAddress(0))
        val port = (server.localAddress as InetSocketAddress).port

        val serverJob = launch {
            val clientChannel = server.suspendAccept()
            delay(100) // Simulate delay before writing
            clientChannel.close()
        }

        val clientChannel = AsynchronousSocketChannel.open()
        clientChannel.connect(InetSocketAddress("localhost", port)).get()

        val writeJob = launch {
            val buffer = ByteBuffer.allocate(1024)
            try{
                clientChannel.suspendWrite(buffer)
            }catch (e: CancellationException){
                assertEquals(1, 1)
            }catch (e: Exception){
                //ONLY CANCELLATION EXCEPTION SHOULD BE THROWN
                assertEquals(0, 1)
            }

        }

        delay(100) // Ensure the write call is suspended
        writeJob.cancelAndJoin() // Cancel and wait for completion
        clientChannel.close()
        serverJob.join()
        server.close()
    }
}
