package pt.isel.pc.problemsets.set1

import org.slf4j.LoggerFactory
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.locks.Condition
import kotlin.concurrent.withLock


class Exchanger<T> {
    private val logger = LoggerFactory.getLogger(Exchanger::class.java)

    private val lock = ReentrantLock()

    private data class Message<T>(
        val condition: Condition,
        var message: T,
        var isDone: Boolean = false
    )

    private var sender: Message<T>? = null

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

    private fun waitPath(data: T): T{
        logger.debug("Thread is going to wait-path")
        sender = Message(lock.newCondition(), data)
        val node = sender!!
        while(true){
            node.condition.await()
            if(node.isDone){
                logger.debug("Thread sent {} and received {}", data, node.message)
                return node.message
            }
        }
    }

    fun exchange(data: T): T {
        lock.withLock {
            logger.debug("Thread wants to send {}", data)
            return if (sender != null) fastPath(data) else waitPath(data)
        }
    }
}
