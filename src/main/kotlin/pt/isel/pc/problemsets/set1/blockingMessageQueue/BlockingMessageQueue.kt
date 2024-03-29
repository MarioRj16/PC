package pt.isel.pc.problemsets.set1.blockingMessageQueue

import pt.isel.pc.problemsets.utils.NodeLinkedList
import java.time.Duration
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class BlockingMessageQueue<T>(private val capacity: Int) {
    data class EnqueueRequest<T>(
        val condition: Condition,
        var message: List<T>,
        var isDone: Boolean
    )

    data class DequeueRequest<T>(
        val condition: Condition,
        var message: T?
    ){
        val isDone: Boolean
            get() = message != null
    }

    private val lock = ReentrantLock()

    private fun availableSpaces(): Int = capacity + dequeueRequests.count - messageQueue.count

    private val messageQueue = NodeLinkedList<T>()
    private val enqueueRequests = NodeLinkedList<EnqueueRequest<T>>()
    private val dequeueRequests = NodeLinkedList<DequeueRequest<T>>()

    @Throws(InterruptedException::class)
    fun tryEnqueue(messages: List<T>, timeout: Duration): Boolean {
        lock.withLock {
        fun addToDequeueAndMessageQueue(){
            for ((idx, message) in messages.withIndex()){
                if(dequeueRequests.notEmpty){
                    val dequeueRequest = dequeueRequests.pull()
                    dequeueRequest.value.message = message
                    dequeueRequest.value.condition.signal()
                } else {
                    for (element in messages.drop(idx)){
                        messageQueue.enqueue(element)
                    }
                    break
                }
            }
        }

            require(messages.isNotEmpty()){" Cannot enqueue empty list of messages"}
            // fast-path
            if(messages.size <= availableSpaces()){
                // If there's consumer threads waiting, then queue is empty.
                addToDequeueAndMessageQueue()
                return true
            }
            // wait-path

            var timeoutInNanos = timeout.toNanos()
            val selfNode = enqueueRequests.enqueue(EnqueueRequest(lock.newCondition(), messages, false))
            while(true){
                try {
                    timeoutInNanos = selfNode.value.condition.awaitNanos(timeoutInNanos)
                } catch (e: InterruptedException){
                    if(selfNode.value.isDone){
                        Thread.currentThread().interrupt()
                        return true
                    }
                    //enqueueRequests.remove(selfNode)
                    // A cancellation does not create conditions to complete other requests
                    throw e
                }
                // check for space in message queue
                if(messages.size <= availableSpaces()){
                    enqueueRequests.remove(selfNode)
                    addToDequeueAndMessageQueue()
                    return true
                }

                if(selfNode.value.isDone){
                    return true
                }
                // check for timeout
                if(timeoutInNanos <= 0){
                    enqueueRequests.remove(selfNode)
                    // A cancellation does not create conditions to complete other requests
                    return false
                }
            }
        }
    }

    @Throws(InterruptedException::class)
    fun tryDequeue(timeout: Duration): T? {
        lock.withLock {
            // fast-path
            if(messageQueue.notEmpty){
                if(enqueueRequests.notEmpty)
                    enqueueRequests.first().condition.signal()
                val msg = messageQueue.pull()
                print(msg.value)
                return msg.value
            }

            // wait-path
            val selfNode = dequeueRequests.enqueue(DequeueRequest(lock.newCondition(), null))
            var timeoutInNanos = timeout.toNanos()
            while(true){
                try {
                    if(enqueueRequests.notEmpty)
                        enqueueRequests.first().condition.signal()
                    timeoutInNanos = selfNode.value.condition.awaitNanos(timeoutInNanos)
                } catch (e: InterruptedException){
                    if(selfNode.value.isDone){
                        Thread.currentThread().interrupt()
                        return selfNode.value.message
                    }
                    dequeueRequests.remove(selfNode)
                    // A cancellation does not create conditions to complete other requests
                    throw e
                }
                // check for success
                if(selfNode.value.isDone){
                    return selfNode.value.message
                }
                // check for timeout
                if(timeoutInNanos <= 0){
                    dequeueRequests.remove(selfNode)
                    // A cancellation does not create conditions to complete other requests
                    return null
                }
            }
        }
    }
}
