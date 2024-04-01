package pt.isel.pc.problemsets.set1

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock
import kotlin.math.min
import kotlin.properties.Delegates
import kotlin.random.Random
import kotlin.test.fail

class BlockingMessageQueueTests {

    @Test
    fun `test one consumer and one producer with multiple values`() {
        val blockingMQ = BlockingMessageQueue<Int>(capacity = 5)
        val timeout = Duration.ofSeconds(5)
        val joinTimeout = 5000L

        var consumerFail = false

        val producer = thread {
            repeat(1000) {
                blockingMQ.tryEnqueue(listOf(it), timeout)
            }
        }

        val consumer = thread {
            repeat(1000) {
                val v= blockingMQ.tryDequeue(timeout)

                if ( v != it) {
                    consumerFail = true
                }
            }
        }

        producer.join(joinTimeout)
        consumer.join(joinTimeout)

        assertFalse(consumerFail)
    }

    @Test
    fun `test producer interruptions`(){
        val blockingMQ = BlockingMessageQueue<Int>(capacity = 1)
        val timeout = Duration.ofSeconds(5)

        val th = thread {
            repeat(2){
                blockingMQ.tryEnqueue(listOf(it), timeout)
            }
        }

        th.interrupt()
        assertTrue(th.isInterrupted)
    }

    @Test
    fun `test consumer interruptions`(){
        val blockingMQ = BlockingMessageQueue<Int>(capacity = 1)
        val timeout = Duration.ofSeconds(5)

        val th = thread{
            blockingMQ.tryDequeue(timeout)
        }

        th.interrupt()
        assertTrue(th.isInterrupted)
    }

    @Test
    fun `test producer timeouts`(){
        val blockingMQ = BlockingMessageQueue<Int>(capacity = 1)
        val timeout = Duration.ofSeconds(5)
        var result by Delegates.notNull<Boolean>()

        val th = thread {
            repeat(2){
                result = blockingMQ.tryEnqueue(listOf(it), timeout)
            }
        }
        th.join(timeout.toNanos())
        assertFalse(result)
    }

    @Test
    fun `test consumer timeouts`(){
        val blockingMQ = BlockingMessageQueue<Int>(capacity = 1)
        val timeout = Duration.ofSeconds(5)
        var result: Int? = null

        val th = thread {
            result = blockingMQ.tryDequeue(timeout)
        }
        th.join(timeout.toNanos())

        val th2 = thread {
            blockingMQ.tryEnqueue(listOf(1), timeout)
        }

        th2.join()

        assertNull(result)
    }

    @Test
    fun `blocking message single dequeue on an initial empty queue`() {
        val capacity = 1
        val blockingMQ = BlockingMessageQueue<String>(capacity)
        val expectedResult = "Ok"
        var result : String? = null
        val joinTimeout = 5000L

        val consumer = thread {
            result = blockingMQ.tryDequeue(Duration.ofSeconds(1))
        }

        val producer = thread {
            blockingMQ.tryEnqueue(listOf(expectedResult), Duration.ZERO)
        }

        consumer.join(joinTimeout)
        producer.join(joinTimeout)

        assertEquals(expectedResult, result)
    }

    @Test
    fun blocking_message_queue_with_multiple_senders_receivers() {
        val nWriters = 4
        val nReaders = 1
        val capacity = 4

        val blockingMQ = BlockingMessageQueue<Int>(capacity)

        val numbersRange = (1..1000)
        val numbersSize = numbersRange.count()

        val expectedSet = (1..numbersSize).toSet()
        val resultSet = ConcurrentHashMap.newKeySet<Int>()

        var writeIndex = 1
        val mutex = ReentrantLock()

        val readersDone = CountDownLatch(nReaders)
        val writersDone = CountDownLatch(nWriters)

        val writerThreads = (1..nWriters)
            .map {
                thread {
                    val random = Random(it)
                    while (true) {
                        var size = 0
                        var localIndex : Int
                        mutex.withLock {
                            localIndex = writeIndex
                            if (writeIndex <= numbersSize) {
                                size = min(
                                    numbersSize - writeIndex + 1,
                                    random.nextInt(1, 4)
                                )
                                writeIndex += size
                            }
                        }
                        if (size == 0) {
                            break
                        }
                        val value = (localIndex until (localIndex+size)).toList()
                        blockingMQ.tryEnqueue(value, Duration.ofHours(10))

                    }
                    writersDone.countDown()
                }

            }

        val readerThreads =  (1..nReaders).map {
            thread {
                while(true) {
                    val value =
                        blockingMQ.tryDequeue(Duration.ofSeconds(3)) ?: break
                    resultSet.add(value)
                }
                readersDone.countDown()
            }
        }


        val writersExitedOk = writersDone.await(5000, TimeUnit.MILLISECONDS)

        for(wt in writerThreads) {
            if (!writersExitedOk) wt.interrupt()
            wt.join()
        }

        val readersExitedOk = readersDone.await(5000, TimeUnit.MILLISECONDS)

        for(rt in readerThreads) {
            if (!readersExitedOk) rt.interrupt()
            rt.join()
        }

        if (!writersExitedOk) {
            fail("too much execution time for writers")
        }
        if (!readersExitedOk) {
            fail("too much execution time for readers")
        }
        assertEquals(numbersSize, resultSet.size)
        assertEquals(expectedSet, resultSet)
    }
}

