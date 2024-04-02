package pt.isel.pc.problemsets.set1

import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExchangerTest {


    @Test
    fun testExchange(){
        val exchanger = Exchanger<Int>()
        val dataA = 111
        val dataB = 222
        val threadA = Thread {
            val receivedData = exchanger.exchange(dataA)
            assertEquals(receivedData,dataB)
        }

        val threadB = Thread {
            val receivedData = exchanger.exchange(dataB)
            assertEquals(receivedData,dataA)
        }

        threadA.start()
        threadB.start()

        threadA.join()
        threadB.join()


    }

    @Test
    fun testMultipleThreadsExchange() {
        val exchanger = Exchanger<String>()

        val resultsMap = ConcurrentHashMap<String, String>()
        val numberOfThreads = 100_000
        val threads = List(numberOfThreads) {
            val msg = UUID.randomUUID().toString()
            Thread {
                resultsMap[msg] = exchanger.exchange(msg)
            }
        }

        threads.forEach { it.start() }

        threads.forEach { it.join() }

        assertEquals(numberOfThreads, resultsMap.size)
        val expected = resultsMap.entries.map { entry -> setOf(entry.key, entry.value) }.toSet().size
        assertEquals(numberOfThreads / 2, expected)
    }

    @Test
    fun testExchangeTimeout(){
        val exchanger = Exchanger<Int>()
        val timeout = Duration.ofMillis(100)
        val data = 1
        var result: Int? = null

        val th = thread {
            result = exchanger.exchange(data, timeout)
        }

        th.join(timeout.toMillis())
        assertNull(result)
    }

    @Test
    fun testExchangeInterruption(){
        val exchanger = Exchanger<Int>()
        val timeout = Duration.ofMillis(100)
        val data = 1

        val th = thread {
            exchanger.exchange(data, timeout)
        }

        th.interrupt()

        assertTrue(th.isInterrupted)
    }
}


