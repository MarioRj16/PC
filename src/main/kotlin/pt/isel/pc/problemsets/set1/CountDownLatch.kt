package pt.isel.pc.problemsets.set1

import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.locks.Condition
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

