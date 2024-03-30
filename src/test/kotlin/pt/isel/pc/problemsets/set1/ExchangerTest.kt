package pt.isel.pc.problemsets.set1

import org.junit.jupiter.api.Test
import pt.isel.pc.problemsets.utils.TestFunction
import pt.isel.pc.problemsets.utils.TestHelper
import java.util.concurrent.Exchanger
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.time.Duration

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
        val exchanger = Exchanger<Int>()

        // Define data values for each thread
        val data = listOf(111, 222, 333, 444, 555, 666)

        // List to hold received data from each thread
        val receivedData = mutableListOf<Int>()

        // Create and start threads
        val threads = List(6) { index ->
            Thread {
                val received = exchanger.exchange(data[index])
                synchronized(receivedData) {
                    receivedData.add(received)
                }
            }
        }

        threads.forEach { it.start() }

        // Wait for all threads to finish
        threads.forEach { it.join() }

        // Ensure all data values were received
        assertEquals(6, receivedData.size)
        var count=0
        for(i in receivedData){
            val idx=data.indexOf(i)
            assertEquals(receivedData[idx],data[count])
            assertEquals(i,data[idx])
            count++
        }


        // Ensure each thread received the correct data value
        data.forEach {
            assert(receivedData.contains(it))
        }
    }
}


