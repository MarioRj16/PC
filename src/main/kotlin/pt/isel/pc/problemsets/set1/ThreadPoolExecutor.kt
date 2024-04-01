package pt.isel.pc.problemsets.set1

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
 * @property maxThreadPoolSize The maximum number of threads allowed in the thread pool.
 * @property keepAliveTime The maximum time that excess idle threads will wait for new tasks before terminating.
 */

/**
 * MONITOR STYLE USED
 */
class ThreadPoolExecutor(
    private val maxThreadPoolSize: Int,
    private val keepAliveTime: Duration,
) {
    private val lock = ReentrantLock()
    private val condition = lock.newCondition()
    private var nOfThreads = AtomicInteger(0)
    private var shuttedDown = false


    /**
     * Added time to each link so that we can save the time of input for each node
     */
    private val workItems = NodeLinkedList<Runnable>()

    /**
     * Initializes the thread pool executor.
     *
     * @throws IllegalArgumentException if [maxThreadPoolSize] is not positive.
     */
    init {
        require(maxThreadPoolSize > 0) { "maxThreadPoolSize must be positive" }
    }

    /**
     * Executes the given task.
     *
     * @param runnable The task to execute.
     * @throws RejectedExecutionException if the executor has been shut down.
     */
    @Throws(RejectedExecutionException::class)
    fun execute(runnable: Runnable): Unit {
        if (shuttedDown) throw RejectedExecutionException()
        lock.lock()
        if (nOfThreads.get() < maxThreadPoolSize) {
            nOfThreads.incrementAndGet()
            lock.unlock()
            Thread {
                try {
                    var x: Runnable? = runnable
                    while (x != null) {
                        if (shuttedDown) throw RejectedExecutionException()
                        x.run()
                        x = processPendingTasks()
                    }
                } finally {
                    lock.withLock {
                        nOfThreads.decrementAndGet()
                        if (nOfThreads.get() == 0) condition.signalAll()
                    }
                }
            }.start()
        } else {
            workItems.enqueue(runnable)
            lock.unlock()
        }
    }

    /**
     * Initiates an orderly shutdown in which previously submitted tasks are executed, but no new tasks will be accepted.
     */
    fun shutdown(): Unit {
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
                    return true
                }
                if (remainingTime <= 0) return false
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
            while (workItems.notEmpty) {
                val pendingTask = workItems.pull()
                val elapsedTime = System.nanoTime() - pendingTask.time
                if (elapsedTime <= keepAliveTime.toNanos()) {
                    return pendingTask.value
                }
            }
            return null
        }
    }
}
