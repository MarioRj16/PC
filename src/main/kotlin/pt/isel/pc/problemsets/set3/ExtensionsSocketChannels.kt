import kotlinx.coroutines.suspendCancellableCoroutine
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun AsynchronousServerSocketChannel.suspendAccept(): AsynchronousSocketChannel {
    return suspendCancellableCoroutine { cont ->
        this.accept(null, object : CompletionHandler<AsynchronousSocketChannel, Void?> {
            override fun completed(result: AsynchronousSocketChannel, attachment: Void?) {
                cont.resume(result)
            }

            override fun failed(exc: Throwable, attachment: Void?) {
                cont.resumeWithException(exc)
            }
        })

        cont.invokeOnCancellation {
            this.close()
        }
    }
}

suspend fun AsynchronousSocketChannel.suspendRead(buffer: ByteBuffer): Int {
    return suspendCancellableCoroutine { cont ->
        this.read(buffer, null, object : CompletionHandler<Int, Void?> {
            override fun completed(result: Int, attachment: Void?) {
                cont.resume(result)
            }

            override fun failed(exc: Throwable, attachment: Void?) {
                cont.resumeWithException(exc)
            }
        })

        cont.invokeOnCancellation {
            this.close()
        }
    }
}

suspend fun AsynchronousSocketChannel.suspendWrite(buffer: ByteBuffer): Int {
    return suspendCancellableCoroutine { cont ->
        this.write(buffer, null, object : CompletionHandler<Int, Void?> {
            override fun completed(result: Int, attachment: Void?) {
                cont.resume(result)
            }

            override fun failed(exc: Throwable, attachment: Void?) {
                cont.resumeWithException(exc)
            }
        })

        cont.invokeOnCancellation {
            this.close()
        }
    }
}