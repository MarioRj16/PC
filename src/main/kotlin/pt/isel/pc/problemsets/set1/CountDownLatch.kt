package pt.isel.pc.problemsets.set1

import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class CountDownLatch(private val count: Int) {
    private var currentCount = count
    private val lock = ReentrantLock()
    private val condition = lock.newCondition()

    fun await() {
        lock.withLock {
            while (currentCount > 0) {
                condition.await()
            }
        }
    }

    fun await(timeout: Long, unit: TimeUnit) {
        lock.withLock {
            require(timeout > 0){" timeout has to be higher than 0"}
            println("Started await")
            var remainingTime = unit.toNanos(timeout)
            while (currentCount > 0) {
                remainingTime = condition.awaitNanos(remainingTime)
                println("Thread woke up")
                if(remainingTime <= 0){
                    return
                }
            }
        }
    }



    fun countDown() {
        lock.withLock {
            if (currentCount > 0) {
                currentCount--
                if (currentCount == 0) {
                    condition.signalAll()
                }
            }
        }}


    fun getCount(): Int {
        return lock.withLock {
             currentCount
        }
    }
}

