package pt.isel.pc.problemsets.set1

import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals

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

        val resultsMap = mutableMapOf<String, String>()
        // Create and start threads
        val numberOfThreads = 100
        val threads = List(numberOfThreads) {
            val msg = UUID.randomUUID().toString()
            Thread {
                resultsMap[msg] = exchanger.exchange(msg)
            }
        }

        threads.forEach { it.start() }

        // Wait for all threads to finish
        threads.forEach { it.join() }

        // Ensure all data values were received
        assertEquals(numberOfThreads, resultsMap.size)
        val expected = resultsMap.entries.map { entry -> setOf(entry.key, entry.value) }.toSet().size
        assertEquals(numberOfThreads / 2, expected)
    }
}


