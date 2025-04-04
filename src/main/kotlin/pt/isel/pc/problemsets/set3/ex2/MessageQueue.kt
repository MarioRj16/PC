package pt.isel.pc.problemsets.set3.ex2

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import pt.isel.pc.problemsets.utils.NodeLinkedList
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.jvm.Throws

class MessageQueue<T> {

    private data class Request<T>(
        var value: T?,
        val continuation: CancellableContinuation<T>,
        var isDone: Boolean = false
    )

    private val messages = NodeLinkedList<T>()
    private val requests = NodeLinkedList<Request<T>>()
    private val lock = ReentrantLock()

    fun enqueue(item: T) {
        val continuation: CancellableContinuation<T>? = lock.withLock {
            if (requests.empty) {
                messages.enqueue(item)
                return
            }
            val requestNode = requests.pull()
            requestNode.value.value = item
            requestNode.value.isDone = true
            requestNode.value.continuation
        }
        continuation?.resume(item)
    }

    suspend fun dequeue(): T {
        var message: T? = null

        return suspendCancellableCoroutine { continuation ->
                lock.withLock {
                    if (!messages.empty) {
                        message = messages.pull().value
                        continuation.resume(message!!)
                    } else {
                        val requestNode = requests.enqueue(Request(null, continuation))
                        continuation.invokeOnCancellation {
                            lock.withLock {
                                if (!requestNode.value.isDone) {
                                    requests.remove(requestNode)
                                    continuation.resumeWithException(CancellationException("Request was cancelled"))
                                }
                            }
                        }
                    }
                }
            }
        }
}
