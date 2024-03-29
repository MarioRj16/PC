package pt.isel.pc.problemsets.set1

import pt.isel.pc.problemsets.utils.NodeLinkedList
import java.util.*
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.max
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

class ThreadPoolExecutor(
    private val maxThreadPoolSize: Int,
    private val keepAliveTime: Duration,
) {
    private val lock = ReentrantLock()
    private val condition = lock.newCondition()
    private var nOfThreads : Int = 0
    private var workItems = NodeLinkedList<Runnable>()
    private var shuttedDown = false

    init {
        require(maxThreadPoolSize>0)
    }

    @Throws(RejectedExecutionException::class)
    fun execute(runnable: Runnable): Unit {
        lock.withLock {
            if(shuttedDown) throw RejectedExecutionException()
            if(nOfThreads< maxThreadPoolSize) {
                nOfThreads++
                Thread{
                    try {
                        runnable.run()
                    }finally {
                        nOfThreads--
                        processPendingTasks()
                    }
                }.start()
            }else workItems.enqueue(runnable)
        }
    }
    fun shutdown(): Unit {
        lock.withLock {
            shuttedDown=true
            condition.signalAll()
        }
    }
    @OptIn(ExperimentalTime::class)
    @Throws(InterruptedException::class)
    fun awaitTermination(timeout: Duration): Boolean {
        lock.withLock {
            val endTime = System.nanoTime() + timeout.toLongMilliseconds()
            while (nOfThreads > 0) {
                val remainingTime = max(0, endTime - System.nanoTime())
                if (remainingTime <= 0) {
                    return false // Timeout expired
                }
                condition.await(remainingTime, TimeUnit.MILLISECONDS)
            }
            return true // All tasks completed
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun processPendingTasks() {
        lock.withLock {
            while (workItems.notEmpty) {
                val pendingTask = workItems.pull()
                val currentTime = System.nanoTime()
                val y =pendingTask.time
                val elapsedTime = currentTime - pendingTask.time
                val x=keepAliveTime.toLongMilliseconds()
                if (elapsedTime < keepAliveTime.toLongMilliseconds()) {
                    execute(pendingTask.value)
                }
            }
        }
    }






}