package pt.isel.pc.problemsets.set1

import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.test.assertEquals

class CountDownLatchTest {

    @Test
    fun testCountDownLatch() {
        val latch = CountDownLatch(1)

        val thread1 = thread {
            Thread.sleep(2000)
            println("Thread 1 completed")
            latch.countDown()
        }

        val thread2 = thread {
            Thread.sleep(3000)
            println("Thread 2 completed")
            latch.countDown()
        }

        val thread3 = thread {
            Thread.sleep(4000)
            println("Thread 3 completed")
            latch.countDown()
        }
        val thread4 = thread {
            Thread.sleep(4000)
            println("Thread 3 completed")
            latch.countDown()
        }

        thread1.join()
        thread2.join()
        thread3.join()
        thread4.join()

        latch.await()

        assertEquals(0, latch.getCount(), "Latch count should be zero")

    }


    @Test
    fun testWithJavaCountDownLatch() {
        val latch = java.util.concurrent.CountDownLatch(4)

        val thread1 = thread {
            Thread.sleep(2000)
            println("Thread 1 completed")
            latch.countDown()
        }

        val thread2 = thread {
            Thread.sleep(3000)
            println("Thread 2 completed")
            latch.countDown()
        }

        val thread3 = thread {
            Thread.sleep(4000)
            println("Thread 3 completed")
            latch.countDown()
        }
        val thread4 = thread {
            Thread.sleep(4000)
            println("Thread 3 completed")
            latch.countDown()
        }

        thread1.join()
        thread2.join()
        thread3.join()
        thread4.join()

        latch.await()

        assertEquals(0, latch.count, "Latch count should be zero")

    }
}
