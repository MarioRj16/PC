package pt.isel.pc.problemsets.set1

import java.util.*
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.max
import kotlin.time.Duration

class ThreadPoolExecutor(
    private val maxThreadPoolSize: Int,
    private val keepAliveTime: Duration,
) {
    private val lock=ReentrantLock()
    private var nOfThreads: Int = 0
    private var workItems = LinkedList<Runnable>()

    init {
        require(maxThreadPoolSize>0)
    }
    @Throws(RejectedExecutionException::class)
    fun execute(runnable: Runnable): Unit {
        lock.withLock {

            if(nOfThreads< maxThreadPoolSize) Thread.
        }
    }
    fun shutdown(): Unit {

    }
    @Throws(InterruptedException::class)
    fun awaitTermination(timeout: Duration): Boolean {

        TODO()
    }

    private sealed interface
}