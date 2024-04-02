package pt.isel.pc.problemsets.set1

import org.slf4j.LoggerFactory
import pt.isel.pc.problemsets.utils.NodeLinkedList
import java.time.Duration
import java.util.*
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * A thread pool executor that manages a pool of worker threads for executing submitted tasks.
 *
 *
 * @property maxThreadPoolSize The maximum number of threads allowed in the thread pool.
 * @property keepAliveTime The maximum time that excess idle threads will wait for new tasks before terminating.
 *
 * MONITOR STYLE USED
 */
class ThreadPoolExecutor(
    private val maxThreadPoolSize: Int,
    private val keepAliveTime: Duration,
) {
    private val logger = LoggerFactory.getLogger(ThreadPoolExecutor::class.java)
    private val lock = ReentrantLock()
    private val condition = lock.newCondition()
    private var nOfThreads = AtomicInteger(0)
    private var shuttedDown = false

    /**
     * A linked list to store pending tasks.
     * Each node contains a `Runnable` task and a timestamp indicating its arrival time.
     */
    private val workItems = NodeLinkedList<Runnable>()

    /**
     * Initializes the thread pool executor.
     *
     * @throws IllegalArgumentException if [maxThreadPoolSize] is not positive.
     */
    init {
        require(maxThreadPoolSize > 0) { "maxThreadPoolSize must be positive" }
        require(keepAliveTime > Duration.ZERO) { "Duration must be higher than 0" }
    }

    /**
     * Executes the given task.
     *
     * @param runnable The task to execute.
     * @throws RejectedExecutionException if the executor has been shut down.
     */
    @Throws(RejectedExecutionException::class)
    fun execute(runnable: Runnable): Unit {
        if (shuttedDown) {
            logger.warn("Rejected task: Executor is shut down")
            throw RejectedExecutionException()
        }
        lock.lock()
        if (nOfThreads.get() < maxThreadPoolSize) {
            nOfThreads.incrementAndGet()
            lock.unlock()
            logger.debug("Created new Thread")
            Thread {
                try {
                    var x: Runnable? = runnable
                    while (x != null) {
                        if (shuttedDown) {
                            logger.warn("Rejected task: Executor is shut down")
                            throw RejectedExecutionException()
                        }
                        x.run()
                        x = processPendingTasks()
                    }
                } finally {
                    endThread()
                }
            }.start()
        } else {
            logger.debug("Added runnable to queue")
            workItems.enqueue(runnable)
            condition.signalAll()
            lock.unlock()
        }
    }

    /**
     * Initiates an orderly shutdown in which previously submitted tasks are executed, but no new tasks will be accepted.
     */
    fun shutdown(): Unit {
        logger.debug("Shutting down executor")
        shuttedDown = true
    }

    /**
     * Blocks until all tasks have completed execution after a shutdown request, or the timeout occurs,
     * or the current thread is interrupted, whichever happens first.
     *
     * @param timeout The maximum time to wait.
     * @return `true` if this executor terminated and `false` if the timeout elapsed before termination.
     * @throws InterruptedException if interrupted while waiting.
     */
    @Throws(InterruptedException::class)
    fun awaitTermination(timeout: Duration): Boolean {
        lock.withLock {
            var remainingTime = timeout.toNanos()
            while (true) {
                try {
                    remainingTime = condition.awaitNanos(remainingTime)
                } catch (e: InterruptedException) {
                    throw e
                }
                if (nOfThreads.get() == 0){
                    logger.debug("Executor terminated")
                    return true
                }
                if (remainingTime <= 0) {
                    logger.debug("Timeout elapsed before termination")
                    return false
                }
            }
        }
    }

    /**
     * Process pending tasks and returns the next task to execute, if any.
     *
     * @return The next task to execute, or `null` if no tasks are pending or the keep-alive time expired.
     */
    private fun processPendingTasks(): Runnable? {
        lock.withLock {
            var remainingTime = keepAliveTime.toNanos()
            while (true) {
                if (workItems.notEmpty) {
                    logger.debug("Retrieving task from queue")
                    return workItems.pull().value
                }
                if (remainingTime <= 0) {
                    logger.debug("Keep-alive time expired")
                    return null
                }
                remainingTime = condition.awaitNanos(remainingTime)
            }
        }
    }

    /**
     * Decrements the thread count upon thread termination and signals waiting threads if necessary.
     */
    private fun endThread() {
        lock.withLock {
            nOfThreads.decrementAndGet()
            if (nOfThreads.get() == 0) {
                logger.debug("All threads terminated, signaling waiting threads")
                condition.signalAll()
            }
        }
    }
}
