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

    private fun availableSpaces(messages: List<T>): Boolean = messages.size <= capacity - messageQueue.count

    private val messageQueue = NodeLinkedList<T>()
    private val producersQueue = NodeLinkedList<EnqueueRequest<T>>()
    private val consumersQueue = NodeLinkedList<DequeueRequest<T>>()

    private fun sendToMQ(messages: List<T>){
        for (message in messages){
            messageQueue.enqueue(message)
        }
    }

    private fun notifyProducer(){
        while (producersQueue.notEmpty && availableSpaces(producersQueue.headValue!!.message)){
            val producer = producersQueue.pull().value
            producer.isDone = true
            producer.condition.signal()
            sendToMQ(producer.message)
        }
    }

    @Throws(InterruptedException::class)
    fun tryEnqueue(messages: List<T>, timeout: Duration): Boolean {
        lock.withLock {
            require(messages.size <= capacity)
            require(messages.isNotEmpty()){" Cannot enqueue empty list of messages"}
            println("Producer wants to send $messages")
            println("Producer found ${messageQueue.count} messages in MQ, ${producersQueue.count} producers and ${consumersQueue.count} consumers")
            // fast-path
            if(availableSpaces(messages)){
                // If MQ is empty then we can deliver one message directly to the consumer
                println("Producer is going to fast Path")
                if(messageQueue.empty && consumersQueue.notEmpty && !(consumersQueue.headValue!!.isDone)){
                    val consumer = consumersQueue.headValue!!
                    consumer.message = messages.first()
                    consumer.condition.signal()
                    sendToMQ(messages.drop(1))
                    println("Producer delivered ${consumer.message} to consumer and sent ${messages.drop(1)} to MQ")
                } else {
                    println("Producer sent ${messages} to MQ")
                    sendToMQ(messages)
                }
                return true
            }
            // wait-path

            println("Producer is going to wait-path")
            var timeoutInNanos = timeout.toNanos()
            val myRequest = producersQueue.enqueue(EnqueueRequest(lock.newCondition(), messages, false))
            while(true){
                try {
                    timeoutInNanos = myRequest.value.condition.awaitNanos(timeoutInNanos)
                } catch (e: InterruptedException){
                    if(myRequest.value.isDone){
                        Thread.currentThread().interrupt()
                        return true
                    }
                    producersQueue.remove(myRequest)
                    // A cancellation does not create conditions to complete other requests
                    throw e
                }

                if(myRequest.value.isDone){
                    return true
                }
                // check for timeout
                if(timeoutInNanos <= 0){
                    producersQueue.remove(myRequest)
                    // A cancellation does not create conditions to complete other requests
                    return false
                }
            }
        }
    }

    @Throws(InterruptedException::class)
    fun tryDequeue(timeout: Duration): T? {
        lock.withLock {
            println("consumer found ${messageQueue.count} messages in MQ, ${producersQueue.count} producers and ${consumersQueue.count} consumers")
            // fast-path
            if(messageQueue.notEmpty && consumersQueue.empty){
                println("Consumer is going to fast-path")
                val message = messageQueue.pull().value
                println("Consumer pulled $message from the MQ")
                val shouldNotifyProducer = producersQueue.notEmpty && availableSpaces(producersQueue.headValue!!.message)
                if(messageQueue.empty && shouldNotifyProducer){
                    println("Consumer will notify producer(s)")
                    notifyProducer()
                }
                return message
            }
            // wait-path
            println("Consumer is going to wait-path")
            val myRequest = consumersQueue.enqueue(DequeueRequest(lock.newCondition(), null))
            var timeoutInNanos = timeout.toNanos()
            while(true){
                try {
                    timeoutInNanos = myRequest.value.condition.awaitNanos(timeoutInNanos)
                } catch (e: InterruptedException){
                    if(myRequest.value.isDone){
                        Thread.currentThread().interrupt()
                        return myRequest.value.message
                    }
                    consumersQueue.remove(myRequest)
                    // A cancellation does not create conditions to complete other requests
                    throw e
                }
                println("Consumer woke up! Message: ${myRequest.value.message}")
                // check for success
                consumersQueue.remove(myRequest)
                if(myRequest.value.isDone){
                    return myRequest.value.message
                }
                // check for timeout
                if(timeoutInNanos <= 0){
                    // A cancellation does not create conditions to complete other requests
                    return null
                }
            }
        }
    }
}
