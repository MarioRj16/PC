package pt.isel.pc.problemsets.set3

import kotlinx.coroutines.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class MessageQueueTest {

    @Test
    fun `test basic enqueue and dequeue`() = runBlocking {
        val queue = MessageQueue<Int>()
        queue.enqueue(1)
        queue.enqueue(2)

        val first = queue.dequeue()
        val second = queue.dequeue()

        assertEquals(1, first)
        assertEquals(2, second)
    }

    @Test
    fun `test dequeue with no enqueued messages`() = runBlocking {
        val queue = MessageQueue<Int>()
        val job = launch {
            val result = queue.dequeue()
            assertEquals(42, result)
        }

        delay(100) // Ensure the dequeue call is suspended
        queue.enqueue(42)
        job.join() // Wait for the coroutine to finish
    }

    @Test
    fun `test cancellation handling`() = runBlocking {
        val queue = MessageQueue<Int>()
        val job = launch {
            assertThrows<CancellationException> {
                runBlocking {
                    queue.dequeue()
                }
            }
        }

        delay(100) // Ensure the dequeue call is suspended
        job.cancelAndJoin() // Cancel and wait for completion
    }

    @Test
    fun `test cancellation does not affect other dequeues`() = runBlocking {
        val queue = MessageQueue<Int>()

        val job1 = launch {
            queue.dequeue()
            //Throw Error if not throws
            assertEquals(0,1)
        }

        delay(100) // Ensure the first dequeue call is suspended

        val job2 = launch {
            val result = queue.dequeue()
            assertEquals(84, result)
        }

        job1.cancelAndJoin()
        queue.enqueue(84)
        job2.join() // Wait for the second coroutine to finish
    }


    @Test
    fun `test multiple enqueues and dequeues`() = runBlocking {
        val queue = MessageQueue<Int>()
        val enqueuer = launch {
            for (i in 1..10) {
                queue.enqueue(i)
            }
        }

        val dequeuer = launch {
            for (i in 1..10) {
                val value = queue.dequeue()
                assertEquals(i, value)
            }
        }

        enqueuer.join()
        dequeuer.join()
    }

    @Test
    fun `test interleaved enqueue and dequeue`() = runBlocking {
        val queue = MessageQueue<Int>()

        val job1 = launch {
            assertEquals(1, queue.dequeue())
        }

        val job2 = launch {
            assertEquals(2, queue.dequeue())
        }

        queue.enqueue(1)
        queue.enqueue(2)

        job1.join()
        job2.join()
    }
}
