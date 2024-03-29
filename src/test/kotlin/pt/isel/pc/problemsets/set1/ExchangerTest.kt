package pt.isel.pc.problemsets.set1

import org.junit.jupiter.api.Test
import java.util.concurrent.Exchanger
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals

class ExchangerTest {

    @Test
    fun testExchangerStress() {
        val exchanger = Exchanger<String>()
        val numThreads = 10
        val executor = Executors.newFixedThreadPool(numThreads)
        val successCounter = AtomicInteger()

        val latch = java.util.concurrent.CountDownLatch(numThreads)

        repeat(numThreads) {
            executor.submit {
                try {
                    val data = "Data from Thread ${Thread.currentThread().id}"
                    val exchangedData = exchanger.exchange(data)
                    if (exchangedData == data) {
                        successCounter.incrementAndGet()
                    }
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                } finally {
                    latch.countDown()
                }
            }
        }

        executor.shutdown()
        executor.awaitTermination(10, TimeUnit.SECONDS)

        assertEquals(numThreads, successCounter.get())
    }
}


