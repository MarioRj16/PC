package pt.isel.pc.problemsets.set1

import pt.isel.pc.problemsets.utils.NodeLinkedList
import java.time.Duration
import java.util.*
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock


class ThreadPoolExecutor(
    private val maxThreadPoolSize: Int,
    private val keepAliveTime: Duration,
) {
    private val lock = ReentrantLock()

    private val condition = lock.newCondition()
    private var nOfThreads = AtomicInteger(0)
    private val workItems = NodeLinkedList<Runnable>()
    private var shuttedDown = false

    init {
        require(maxThreadPoolSize>0)
    }

    @Throws(RejectedExecutionException::class)
    fun execute(runnable: Runnable): Unit {
        if(shuttedDown) throw RejectedExecutionException()
        lock.lock()
        if(nOfThreads.get()< maxThreadPoolSize) {
            nOfThreads.incrementAndGet()
            lock.unlock()
            Thread{
                try {
                   // println("entry")
                    var x:Runnable?=runnable
                    while(x!=null){
                        if(shuttedDown) throw RejectedExecutionException()
                        x.run()
                        println("here")
                        x=processPendingTasks()
                    }
                  //  println("left")
                }
                finally {
                    lock.withLock {
                    nOfThreads.decrementAndGet()
                    if(nOfThreads.get()==0) condition.signalAll()}
                }
            }.start()
        }else {
            workItems.enqueue(runnable)
            lock.unlock()
        }

    }
    fun shutdown(): Unit {
        shuttedDown=true
    }

    @Throws(InterruptedException::class)
    fun awaitTermination(timeout: Duration): Boolean {
        lock.withLock {
            var remainingTime = timeout.toNanos()
            while ( true) {
                try {
                    remainingTime=condition.awaitNanos(remainingTime)
                }catch(e:InterruptedException){
                    throw e
                }
                if(nOfThreads.get() == 0) return true
                if(remainingTime <= 0) return false
            }
        }
    }


    private fun processPendingTasks() :Runnable?{
        lock.withLock {
            while (workItems.notEmpty) {
                val pendingTask = workItems.pull()
                val currentTime = System.nanoTime()
                val elapsedTime = currentTime - pendingTask.time
                if (elapsedTime <= keepAliveTime.toNanos()) {
                   // println("returned")
                    return pendingTask.value
                }
            }
            return null
        }
    }






}