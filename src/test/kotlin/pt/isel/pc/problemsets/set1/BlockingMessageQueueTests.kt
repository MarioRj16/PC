package pt.isel.pc.problemsets.set1.blockingMessageQueue
/*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class BlockingMessageQueueTest {

    @Test
    fun testEnqueueDequeue() {
        val queue = BlockingMessageQueue<Int>(capacity = 5)
        queue.tryEnqueue(1)
        assertEquals(1, queue.dequeue())
    }

    @Test
    fun testEnqueueMultiple() {
        val queue = BlockingMessageQueue<Int>(capacity = 5)
        val messages = listOf(1, 2, 3)
        assertTrue(queue.tryEnqueue(messages))
        assertEquals(1, queue.dequeue())
        assertEquals(2, queue.dequeue())
        assertEquals(3, queue.dequeue())
    }

    @Test
    fun testEnqueueFullQueue() {
        val queue = BlockingMessageQueue<Int>(capacity = 3)
        queue.tryEnqueue(1)
        queue.tryEnqueue(2)
        queue.tryEnqueue(3)
        assertFalse(queue.tryEnqueue(4, timeout = 100, unit = TimeUnit.MILLISECONDS))
    }

    @Test
    fun testDequeueEmptyQueue() {
        val queue = BlockingMessageQueue<Int>(capacity = 5)
        assertNull(queue.dequeue(timeout = 100, unit = TimeUnit.MILLISECONDS))
    }

    @Test
    fun testEnqueueTimeout() {
        val queue = BlockingMessageQueue<Int>(capacity = 2)
        val executor = Executors.newSingleThreadExecutor()
        executor.submit {
            Thread.sleep(500) // Sleep for 500 milliseconds
            assertTrue(queue.tryEnqueue(1, timeout = 100, unit = TimeUnit.MILLISECONDS))
        }
        assertFalse(queue.tryEnqueue(2, timeout = 200, unit = TimeUnit.MILLISECONDS))
        executor.shutdown()
        executor.awaitTermination(1, TimeUnit.SECONDS)
    }

    @Test
    fun testDequeueTimeout() {
        val queue = BlockingMessageQueue<Int>(capacity = 2)
        val executor = Executors.newSingleThreadExecutor()
        executor.submit {
            Thread.sleep(500) // Sleep for 500 milliseconds
            assertNull(queue.dequeue(timeout = 100, unit = TimeUnit.MILLISECONDS))
        }
        queue.tryEnqueue(1)
        assertNotNull(queue.dequeue(timeout = 200, unit = TimeUnit.MILLISECONDS))
        executor.shutdown()
        executor.awaitTermination(1, TimeUnit.SECONDS)
    }
}


 */
