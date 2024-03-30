package pt.isel.pc.problemsets.set1

import pt.isel.pc.problemsets.utils.NodeLinkedList
import java.util.*
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
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
    private var nOfThreads = AtomicInteger(0)
    private var workItems = NodeLinkedList<Runnable>()
    private var shuttedDown = false

    init {
        require(maxThreadPoolSize>0)
    }

    @Throws(RejectedExecutionException::class)
    fun execute(runnable: Runnable): Unit {
        lock.withLock {
            if(shuttedDown) throw RejectedExecutionException()
            if(nOfThreads.get()< maxThreadPoolSize) {
                nOfThreads.incrementAndGet()
                Thread{
                    try {
                        runnable.run()
                    }finally {
                        nOfThreads.decrementAndGet()
                        if(nOfThreads.get()==0) condition.signal()
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
            var remainingTime = timeout.toLongMilliseconds()

            while ( nOfThreads.get() > 0 && remainingTime > 0) {
                val startTime = System.currentTimeMillis()
                condition.await(remainingTime, TimeUnit.MILLISECONDS)
                val elapsedTime = System.currentTimeMillis() - startTime
                remainingTime -= elapsedTime
            }
            return nOfThreads.get() == 0
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
                val x=keepAliveTime.inWholeNanoseconds
                if (elapsedTime < keepAliveTime.inWholeNanoseconds) {
                    execute(pendingTask.value)
                }
            }
        }
    }






}