package pt.isel.pc.problemsets.set1

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import pt.isel.pc.problemsets.set1.blockingMessageQueue.BlockingMessageQueue
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock
import kotlin.math.min
import kotlin.random.Random
import kotlin.test.fail

class BlockingMessageQueueTests {

    @Test
    fun `test one consumer and one producer multiple values`() {
        val queue = BlockingMessageQueue<Int>(capacity = 5)
        val timeout = Duration.ofSeconds(1)

        var consumerFail = false

        val producer = thread {
            repeat(100) {
                queue.tryEnqueue(listOf(it), timeout)
            }
        }

        val consumer = thread {
            repeat(100) {
                val v= queue.tryDequeue(timeout)

                if ( v != it) {
                    consumerFail = true
                }
            }
        }

        producer.join(5000)
        consumer.join(5000)


        assertFalse(consumerFail)
    }


    /**
     * this simple test presents most of the considered best practices
     * used on concurrency tests construction
     *
     * - first, create the needed artifacts
     * - second, launch the thread(s) that executes the (potentially blocking) code to test
     * - start building simple tests that test single operations on a simple context
     * - the created thread(s) should not execute any assertion,
     *   they just write results on selected artifacts
     * - the test thread collects the  results, after joining the created threads(s)
     *   with an appropriate timeout to avoid running tests with undefinable times
     * - finally the test thread runs the necessary assertions to check the
     *   test success
     */
    @Test
    fun `blocking message single dequeue on an initial empty queue`() {
        // build the needed artifacts
        val capacity = 1
        val msgQueue = BlockingMessageQueue<String>(capacity)
        val expectedResult = "Ok"
        var result : String? = null

        // create a thread that run the code to test and produce the necessary results
        val consumer = thread {
            result = msgQueue.tryDequeue(Duration.ofSeconds(1))
        }

        // create a thread that run the code to test and produce the necessary results
        val producer = thread {
            msgQueue.tryEnqueue(listOf(expectedResult), Duration.ZERO)
        }

        // join the created threads e«with a timeout
        producer.join(5000)
        consumer.join(5000)

        // do the necessary assertions
        assertEquals(expectedResult, result)
    }

    /**
     * A more complicated test that should be done after the simple tests
     * have succeeded
     * In this case we have an arbitrary number of enqueuers and dequeuers
     * The test parametrizes the number of each.
     * Note that this code could be refactored to an auxiliary method that
     * could be used to build tests for different scenarios.
     */
    @Test
    fun blocking_message_queue_with_multiple_senders_receivers() {
        // general config
        val NWRITERS = 4
        val NREADERS = 1
        val CAPACITY = 4

        // the queue to test
        val msgQueue = BlockingMessageQueue<Int>(CAPACITY)

        // test scenario
        val numbers_range = (1..1000)
        val numbers_size = numbers_range.count()

        val expectedSet = (1..numbers_size).toSet()
        val resultSet = ConcurrentHashMap.newKeySet<Int>()

        // this index use is protected by a mutex
        // in oprder to support multiple writers
        var writeIndex = 1
        val mutex = ReentrantLock()

        val readersDone = CountDownLatch(NREADERS)
        val writersDone = CountDownLatch(NWRITERS)

        val writerThreads = (1..NWRITERS)
            .map {
                thread {
                    val random = Random(it)
                    while (true) {
                        // produce random sized lists with
                        // numbers from the expected range
                        var size = 0
                        var localIndex : Int
                        mutex.withLock {
                            localIndex = writeIndex
                            if (writeIndex <= numbers_size) {
                                size = min(numbers_size - writeIndex + 1,
                                    random.nextInt(1, 4))
                                writeIndex += size
                            }
                        }
                        if (size == 0) {
                            break
                        }
                        val value = (localIndex until (localIndex+size)).toList()
                        //if (msgQueue.tryEnqueue(value, Duration.ofHours(9999999999999999)))

                    }
                    writersDone.countDown()
                }

            }

        val readerThreads =  (1..NREADERS).map {
            thread {
                while(true) {
                    // assume that all emitting values have been consumed
                    // if operation exit by timeout
                    val value =
                        msgQueue.tryDequeue(Duration.ofSeconds(3)) ?: break
                    resultSet.add(value)
                }
                readersDone.countDown()
            }
        }


        // wait for writers termination (with a timeout)
        val writersExitedOk = writersDone.await(5000, TimeUnit.MILLISECONDS)

        for(wt in writerThreads) {
            if (!writersExitedOk) wt.interrupt()
            wt.join()
        }

        // wait for readers termination (with a timeout)
        val readersExitedOk = readersDone.await(5000, TimeUnit.MILLISECONDS)

        for(rt in readerThreads) {
            if (!readersExitedOk) rt.interrupt()
            rt.join()
        }

        // final assertions
        if (!writersExitedOk) {
            fail("too much execution time for writers")
        }
        if (!readersExitedOk) {
            fail("too much execution time for readers")
        }
        assertEquals(numbers_size, resultSet.size)
        assertEquals(expectedSet, resultSet)
    }



}

