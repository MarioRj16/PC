package pt.isel.pc.problemsets.set1
import java.time.Duration
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Executes a race between multiple suppliers concurrently and returns the result of the first supplier
 * to complete successfully.
 * Uses kernel-style.
 * @param suppliers A list of supplier functions to be executed concurrently.
 * @param timeout The maximum duration to wait for the race to complete. If no supplier completes
 * within this duration, the race is considered timed out.
 * @return The result of the first supplier to complete successfully, or null if the race times out
 * or if all suppliers throw exceptions.
 * @throws InterruptedException if the current thread is interrupted while waiting for the race to complete.
 */
@Throws(InterruptedException::class)
fun <T> race(suppliers: List<()->T>, timeout: Duration): T?{
    val lock=ReentrantLock()
    val condition=lock.newCondition()
    val ans = AtomicReference<T?>(null)
    val threads = mutableListOf<Thread>()
    // Create and start threads for each supplier
    suppliers.forEachIndexed {key, value ->
        threads += Thread {
            try{
                val result = value()
                // Update the result if it's not already set
                lock.withLock {
                    if (ans.get() == null) {
                        ans.set(result)
                        condition.signalAll()
                    }
                }
            } catch (e: InterruptedException){
                //println("Thread $key caught interruption")
            }
        }
    }
    // Start all threads
    threads.forEach{ it.start() }
    var timeoutInNanos = timeout.toNanos()

    // Wait for the race to complete or timeout
    while (true){
        try{
            lock.withLock {
                timeoutInNanos = condition.awaitNanos(timeoutInNanos)
            }
        } catch (e: InterruptedException){
            threads.forEach{
                it.interrupt()
                it.join()
            }
            throw e
        }

        if(timeoutInNanos <= 0){
            threads.forEach{
                it.interrupt()
            }
            return null
        }
        // If a result is found, return it
        val result = ans.get()
        if(result != null)
            return result
    }
}
