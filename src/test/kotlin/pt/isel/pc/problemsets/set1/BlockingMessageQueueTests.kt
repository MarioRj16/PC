package pt.isel.pc.problemsets.set1.blockingMessageQueue

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.concurrent.thread

class BlockingMessageQueueTest {
    private val duration: Duration = Duration.ofMillis(100)

    @Test
    fun `enqueue and dequeue messages`() {
        val queue = BlockingMessageQueue<Int>(capacity = 3)
        queue.tryEnqueue(listOf(10), duration)
        queue.tryEnqueue(listOf(20), duration)
        queue.tryEnqueue(listOf(30), duration)

        assertEquals(10, queue.tryDequeue(duration))
        assertEquals(20, queue.tryDequeue(duration))
        assertEquals(30, queue.tryDequeue(duration))
    }

    @Test
    fun `enqueue with timeout`() {
        val queue = BlockingMessageQueue<Int>(capacity = 1)

        assertTrue(queue.tryEnqueue(listOf(1), timeout = Duration.ofMillis(1000)))
        assertFalse(queue.tryEnqueue(listOf(2), timeout = Duration.ofMillis(100))) // Queue is full, should return false
    }

    @Test
    fun `dequeue with timeout`() {
        val queue = BlockingMessageQueue<Int>(capacity = 1)

        assertNull(queue.tryDequeue(timeout = Duration.ofMillis(100))) // Queue is empty, should return null

        thread {
            Thread.sleep(50)
            queue.tryEnqueue(listOf(1),  Duration.ofMillis(200))
        }

        assertEquals(1, queue.tryDequeue(timeout = Duration.ofMillis(200))) // Should dequeue successfully
    }

    @Test
    fun `enqueue with multiple items and timeout`() {
        val queue = BlockingMessageQueue<Int>(capacity = 2)

        assertTrue(queue.tryEnqueue(listOf(1, 2), timeout = Duration.ofMillis(100)))
        assertFalse(queue.tryEnqueue(listOf(3, 4), timeout = Duration.ofMillis(100))) // Queue is full, should return false
    }
}

