package pt.isel.pc.problemsets.set1

import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * A synchronization aid that allows one or more threads to wait until a set of operations
 * being performed in other threads completes.
 *
 * @param count the number of times `countDown` must be invoked before threads can pass
 * through `await`
 */

/**
 * USED KERNEL STYLE
 */
class CountDownLatch(private val count: Int) {
    init {
        require(count>0){"count must be higher than 0"}
    }
    private var currentCount = count
    private val lock = ReentrantLock()
    private val condition = lock.newCondition()

    /**
     * Causes the current thread to wait until the latch has counted down to zero, unless
     * the thread is interrupted.
     */
    fun await() {
        lock.withLock {
            while (currentCount > 0) {
                condition.await()
            }
        }
    }

    /**
     * Causes the current thread to wait until the latch has counted down to zero, unless
     * the thread is interrupted, or the specified waiting time elapses.
     *
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @return `true` if the count reached zero and `false` if the waiting time elapsed
     * before the count reached zero
     * @throws IllegalArgumentException if the timeout value is not positive
     */
    @Throws(InterruptedException::class)
    fun await(timeout: Long, unit: TimeUnit): Boolean {
        lock.withLock {
            require(timeout > 0) { "Timeout has to be higher than 0" }
            var remainingTime = unit.toNanos(timeout)
            while (currentCount > 0) {
                remainingTime = condition.awaitNanos(remainingTime)
                if (remainingTime <= 0) {
                    return false
                }
            }
            return true
        }
    }

    /**
     * Decrements the count of the latch, releasing all waiting threads if the count reaches zero.
     */
    fun countDown() {
        lock.withLock {
            if (currentCount > 0) {
                currentCount--
                if (currentCount == 0) {
                    condition.signalAll()
                }
            }
        }
    }

    /**
     * Returns the current count of the latch.
     *
     * @return the current count
     */
    fun getCount(): Int {
        return lock.withLock {
            currentCount
        }
    }
}
