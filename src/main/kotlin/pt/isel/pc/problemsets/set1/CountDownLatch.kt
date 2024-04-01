package pt.isel.pc.problemsets.set1

import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * A synchronization aid that allows one or more threads to wait until a set of operations
 * being performed in other threads completes.
 * Uses kernel-style
 * @param count the number of times `countDown` must be invoked before threads can pass
 * through `await`
 */
class CountDownLatch(private val count: Int) {

    private val logger=LoggerFactory.getLogger(CountDownLatch::class.java)
    init {
        require(count > 0){
            "count must be higher than 0"
        }
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
                logger.debug("Thread wants to wait")
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
     * @return True if the count reached zero and false if the waiting time elapsed
     * before the count reached zero
     * @throws IllegalArgumentException if the timeout value is not positive
     */
    @Throws(InterruptedException::class)
    fun await(timeout: Long, unit: TimeUnit): Boolean {
        lock.withLock {
            require(timeout > 0) { "Timeout has to be higher than 0" }
            var remainingTime = unit.toNanos(timeout)
            while (currentCount > 0) {
                logger.debug("Thread wants to wait")
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
                logger.debug("Thread counted down")
                currentCount--
                if (currentCount == 0) {
                    logger.debug("Finished")
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
