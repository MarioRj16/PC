package pt.isel.pc.problemsets.set3

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import pt.isel.pc.problemsets.utils.NodeLinkedList
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

class MessageQueue<T> {


    private data class Request<T>(
        var value : T?,
        val continuation: CancellableContinuation<T>,
        var isDone: Boolean
    )

    private val messages = NodeLinkedList<Request<T>>()

    private val lock = ReentrantLock()


    fun enqueue(item:T){
        val continuation:CancellableContinuation<T>? = lock.withLock {
            if(messages.empty){
                return
            }
            val requestNode = messages.pull()
            requestNode.value.value = item
            requestNode.value.isDone = true
            requestNode.value.continuation
        }
        continuation?.resume(item)
    }

    suspend fun dequeue():T{
        var isFastPath = false
        var requestNode: NodeLinkedList.Node<Request<T>>? = null
        var value : MessageQueue. Request<T>?= null
        try{
            return suspendCancellableCoroutine<T> { continuation ->
                lock.withLock {
                    if(messages.headValue != null) {
                        isFastPath = true
                        value = messages.pull().value
                        continuation.resume(value!!.value!!)
                    }else{
                        requestNode = messages.enqueue(Request(null,continuation,false))
                    }
                }
            }
        } catch (e : CancellationException){
            if (isFastPath) {
                return value!!.value!!
            }
            val observedNode = requestNode ?: throw e
            lock.withLock {
                if (observedNode.value.isDone) {
                    return value!!.value!!
                }
                messages.remove(observedNode)
                throw e
            }
        }
    }
}