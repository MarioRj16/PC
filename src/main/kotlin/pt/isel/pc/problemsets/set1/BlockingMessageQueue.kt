package pt.isel.pc.problemsets.set1

import org.slf4j.LoggerFactory
import pt.isel.pc.problemsets.utils.NodeLinkedList
import java.time.Duration
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * FIFO blocking message queue, with N capacity, N-ary insertion and unary retrieval.
 * Uses kernel-style.
 * @param capacity The maximum capacity of the message queue.
 * @param T The type of messages stored in the queue.
 */
class BlockingMessageQueue<T>(private val capacity: Int) {

    private val logger = LoggerFactory.getLogger(BlockingMessageQueue::class.java)

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
    private val messageQueue = NodeLinkedList<T>()
    private val producersQueue = NodeLinkedList<EnqueueRequest<T>>()
    private val consumersQueue = NodeLinkedList<DequeueRequest<T>>()

    /**
     * Checks if there are enough available spaces in the message queue to accommodate the given messages.
     * @param messages The list of messages to be checked for available spaces.
     * @return True if there are enough available spaces in the message queue, false otherwise.
     */
    private fun availableSpaces(messages: List<T>): Boolean =
        messages.size <= (capacity - messageQueue.count)

    /**
     * Enqueues a list of messages to the message queue for further processing.
     * @param messages The list of messages to be enqueued.
     */
    private fun sendToMQ(messages: List<T>){
        for (message in messages){
            messageQueue.enqueue(message)
        }
    }

    /**
     * Notifies waiting producers about the availability of space in the message queue.
     * Iterates through the producers queue and signals the condition of each producer waiting for space.
     * Once a producer is notified, its associated messages are sent to the message queue for processing.
     */
    private fun notifyProducers(){
        while (producersQueue.notEmpty && availableSpaces(producersQueue.headValue!!.message)){
            val producer = producersQueue.pull().value
            producer.isDone = true
            producer.condition.signal()
            sendToMQ(producer.message)
        }
    }

    /**
     * Notifies the consumer about the availability of messages.
     * Updates the consumer's message with the first message from the list and signals its condition to wake it up.
     * @param messages The list of messages available for consumption.
     * @return The dequeue request associated with the consumer.
     */
    private fun notifyConsumer(messages: List<T>): DequeueRequest<T> {
        val consumer = consumersQueue.headValue!!
        consumer.message = messages.first()
        consumer.condition.signal()
        return consumer
    }

    /**
     * Executes the fast path for producer, where it delivers a message directly to the consumer if the queue is empty
     * and there are consumers waiting; otherwise, it sends messages to the message queue.
     * @param messages The list of messages to be processed.
     */
    private fun producerFastPath(messages: List<T>){
        logger.debug("Producer is going to fast Path")
        // If MQ is empty then we can deliver one message directly to the consumer
        if(messageQueue.empty && consumersQueue.notEmpty && !(consumersQueue.headValue!!.isDone)){
            val consumer = notifyConsumer(messages)
            val messagesToSendToMQ = messages.drop(1)
            sendToMQ(messagesToSendToMQ)
            logger.debug(
                "Producer delivered {} to consumer and sent {} to MQ",
                consumer.message,
                messagesToSendToMQ
            )
        } else {
            logger.debug("Producer sent {} to MQ", messages)
            sendToMQ(messages)
        }
    }

    /**
     * Executes the await path for producer, where it waits until space becomes available in the queue or a timeout occurs.
     * @param messages The list of messages to be enqueued.
     * @param timeout The maximum time to wait for space in the queue.
     * @return True if the messages were successfully enqueued, false if the operation times out.
     * @throws InterruptedException If the current thread is interrupted while waiting.
     */
    private fun producerAwaitPath(messages: List<T>, timeout: Duration): Boolean {
        logger.debug("Producer is going to wait-path")
        var timeoutInNanos = timeout.toNanos()
        val producer = producersQueue.enqueue(EnqueueRequest(lock.newCondition(), messages, false))
        while(true){
            try {
                timeoutInNanos = producer.value.condition.awaitNanos(timeoutInNanos)
            } catch (e: InterruptedException){
                if(producer.value.isDone){
                    Thread.currentThread().interrupt()
                    return true
                }
                producersQueue.remove(producer)
                // A cancellation does not create conditions to complete other requests
                throw e
            }

            if(producer.value.isDone){
                return true
            }
            // check for timeout
            if(timeoutInNanos <= 0){
                producersQueue.remove(producer)
                // A cancellation does not create conditions to complete other requests
                return false
            }
        }
    }

    /**
     * Executes the fast path for consumer, where it directly pulls a message from the queue and notifies producers if necessary.
     * @return The message pulled from the queue.
     */
    private fun consumerFastPath(): T {
        logger.debug("Consumer is going to fast-path")
        val message = messageQueue.pull().value
        logger.debug("Consumer pulled {} from the MQ", message)
        val shouldNotifyProducer = producersQueue.notEmpty && availableSpaces(producersQueue.headValue!!.message)
        if(messageQueue.empty && shouldNotifyProducer){
            logger.debug("Consumer will notify producer(s)")
            notifyProducers()
        }
        return message
    }

    /**
     * Executes the wait path for consumer, where it waits until a message is available in the queue or a timeout occurs.
     * @param timeout The maximum time to wait for a message.
     * @return The message dequeued from the queue, or null if the operation times out.
     * @throws InterruptedException If the current thread is interrupted while waiting.
     */
    private fun consumerWaitPath(timeout: Duration): T? {
        logger.debug("Consumer is going to wait-path")
        val consumer = consumersQueue.enqueue(DequeueRequest(lock.newCondition(), null))
        var timeoutInNanos = timeout.toNanos()
        while(true){
            try {
                timeoutInNanos = consumer.value.condition.awaitNanos(timeoutInNanos)
            } catch (e: InterruptedException){
                if(consumer.value.isDone){
                    Thread.currentThread().interrupt()
                    return consumer.value.message
                }
                consumersQueue.remove(consumer)
                // A cancellation does not create conditions to complete other requests
                throw e
            }

            logger.debug("Consumer woke up! Message: {}", consumer.value.message)
            consumersQueue.remove(consumer)

            // check for success
            if(consumer.value.isDone){
                return consumer.value.message
            }

            // check for timeout
            if(timeoutInNanos <= 0){
                // A cancellation does not create conditions to complete other requests
                return null
            }
        }
    }

    /**
     * Tries to enqueue a list of messages into the queue, blocking until space becomes available or a timeout occurs.
     * @param messages The list of messages to enqueue.
     * @param timeout The maximum time to wait for space in the queue.
     * @return True if the messages were successfully enqueued, false if the operation times out.
     * @throws InterruptedException If the current thread is interrupted while waiting.
     */
    @Throws(InterruptedException::class)
    fun tryEnqueue(messages: List<T>, timeout: Duration): Boolean {
        lock.withLock {
            require(messages.size <= capacity){"The number of messages must not exceed the capacity of the queue"}
            require(messages.isNotEmpty()){"Cannot enqueue empty list of messages"}
            logger.debug("Producer wants to send {}", messages)
            logger.debug("Producer found ${messageQueue.count} messages in MQ, ${producersQueue.count} producers and ${consumersQueue.count} consumers")

            return if(availableSpaces(messages)){
                producerFastPath(messages)
                true
            } else {
                producerAwaitPath(messages, timeout)
            }
        }
    }

    /**
     * Tries to dequeue a message from the queue, blocking until a message is available or a timeout occurs.
     * @param timeout The maximum time to wait for a message.
     * @return The dequeued message, or null if the operation times out.
     * @throws InterruptedException If the current thread is interrupted while waiting.
     */
    @Throws(InterruptedException::class)
    fun tryDequeue(timeout: Duration): T? {
        lock.withLock {
            logger.debug("consumer found ${messageQueue.count} messages in MQ, ${producersQueue.count} producers and ${consumersQueue.count} consumers")
            return if(messageQueue.notEmpty && consumersQueue.empty) {
                consumerFastPath()
            } else {
                consumerWaitPath(timeout)
            }
        }
    }
}
