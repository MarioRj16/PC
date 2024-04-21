import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import pt.isel.pc.problemsets.set2.SafeSuccession
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread


class SafeSuccessionTest {

    @Test
    fun testNextSequential() {
        val items = arrayOf(1, 2, 3, 4, 5)
        val succession = SafeSuccession(items)

        // Test sequential access
        assertEquals(1, succession.next())
        assertEquals(2, succession.next())
        assertEquals(3, succession.next())
        assertEquals(4, succession.next())
        assertEquals(5, succession.next())
        assertNull(succession.next()) // Beyond the end of array
    }

    @Test
    fun testNextConcurrent() {

        // Create an array of integers for testing
        val items = arrayOf(1, 2, 3, 4, 5)
        val succession = SafeSuccession(items)

        val numThreads = 10
        val latch = CountDownLatch(numThreads)
        val results = mutableSetOf<Int?>()

        // Create and start multiple threads
        repeat(numThreads) {
            thread {
                while (true) {
                    val nextItem = succession.next()
                    if (nextItem != null) {
                        results.add(nextItem)
                    } else {
                        break
                    }
                }
                latch.countDown() // Signal that this thread has finished
            }
        }

        // Wait for all threads to finish
        latch.await()

        // Ensure all items from the array are accessed exactly once
        assertEquals(items.toSet(), results)
        assertNull(succession.next()) // Ensure no more items after exhaustion
    }
}
