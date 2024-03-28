package pt.isel.pc.problemsets.set1.blockingMessageQueue

import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.concurrent.thread
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BlockingMessageQueueTest {

    @Test
    fun `test enqueue and dequeue with multiple items`() {
        val queue = BlockingMessageQueue<Int>(capacity = 3)

        queue.enqueue(listOf(1, 2, 3))
        assertEquals(1, queue.dequeue())
        assertEquals(2, queue.dequeue())
        assertEquals(3, queue.dequeue())
    }

    @Test
    fun `test enqueue with timeout`() {
        val queue = BlockingMessageQueue<Int>(capacity = 1)

        assertTrue(queue.enqueue(listOf(1), timeout = Duration.ofMillis(100)))
        assertFalse(queue.enqueue(listOf(2), timeout = Duration.ofMillis(100))) // Queue is full, should return false
    }

    @Test
    fun `test dequeue with timeout`() {
        val queue = BlockingMessageQueue<Int>(capacity = 1)

        assertNull(queue.dequeue(timeout = Duration.ofMillis(100))) // Queue is empty, should return null

        thread {
            Thread.sleep(50)
            queue.enqueue(1)
        }

        assertEquals(1, queue.dequeue(timeout = Duration.ofMillis(200))) // Should dequeue successfully
    }
}
