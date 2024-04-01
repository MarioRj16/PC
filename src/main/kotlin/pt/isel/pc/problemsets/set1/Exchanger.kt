package pt.isel.pc.problemsets.set1

import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.TimeoutException
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.locks.Condition
import kotlin.concurrent.withLock

/**
 * A synchronization point at which threads can pair and swap elements within pairs.
 * Uses kernel-style.
 * @param T The type of elements to be exchanged.
 */
class Exchanger<T> {
    private val logger = LoggerFactory.getLogger(Exchanger::class.java)

    private val lock = ReentrantLock()

    private data class Message<T>(
        val condition: Condition,
        var message: T,
        var isDone: Boolean = false
    )

    private var sender: Message<T>? = null

    /**
     * Executes the fast path of the exchange operation, where a waiting thread immediately pairs and swaps elements.
     * @param data The data to be exchanged.
     * @return The data received from the other thread.
     */
    private fun fastPath(data: T): T {
        logger.debug("Node is going to fast path")
        val node = sender!!
        sender = null
        val prevMessage = node.message
        node.message = data
        node.isDone = true
        node.condition.signal()
        logger.debug("Node received {} and sent {}", prevMessage, data)
        return prevMessage
    }

    /**
     * Executes the wait path of the exchange operation, where a thread waits until another thread pairs and swaps elements.
     * @param data The data to be exchanged.
     * @return The data received from the other thread.
     */
    private fun waitPath(data: T, timeout: Duration = Duration.ZERO): T{
        logger.debug("Thread is going to wait-path")
        sender = Message(lock.newCondition(), data)
        val node = sender!!
        var timeoutInNanos = timeout.toNanos()
        while (true){
            try {
                if(timeout != Duration.ZERO){
                    timeoutInNanos = node.condition.awaitNanos(timeoutInNanos)
                } else {
                    node.condition.await()
                }
            } catch (e: InterruptedException){
                if(sender != null){
                    val message = sender!!.message
                    sender = null
                    Thread.currentThread().interrupt()
                    return message
                }
                sender = null
                throw e
            }

            if(node.isDone){
                logger.debug("Thread sent {} and received {}", data, node.message)
                return node.message
            }

            if(timeout != Duration.ZERO && timeoutInNanos <= 0){
                throw TimeoutException()
            }
        }
    }

    /**
     * Exchanges the specified data with another thread, either immediately or by waiting for pairing.
     * @param data The data to be exchanged.
     * @return The data received from the other thread.
     * @throws InterruptedException If the current thread is interrupted while waiting.
     */
    @Throws(InterruptedException::class)
    fun exchange(data: T): T {
        lock.withLock {
            logger.debug("Thread wants to send {}", data)
            return if (sender != null)
                fastPath(data)
            else
                waitPath(data)
        }
    }

    /**
     * Exchanges the specified data with another thread, either immediately or by waiting for pairing with a timeout.
     * If the exchange does not complete within the specified timeout, a [TimeoutException] is thrown.
     * @param data The data to be exchanged.
     * @param timeout The maximum time to wait for pairing with another thread.
     * @return The data received from the other thread.
     * @throws TimeoutException If the exchange operation times out.
     * @throws InterruptedException If the current thread is interrupted while waiting.
     */
    @Throws(InterruptedException::class)
    fun exchange(data: T, timeout: Duration): T {
        lock.withLock {
            logger.debug("Thread wants to send {}", data)
            return if(sender != null)
                fastPath(data)
            else
                waitPath(data, timeout)
        }
    }
}
