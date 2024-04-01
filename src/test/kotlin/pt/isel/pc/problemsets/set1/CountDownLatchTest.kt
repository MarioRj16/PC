package pt.isel.pc.problemsets.set1

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import pt.isel.pc.problemsets.utils.TestHelper
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class CountDownLatchTest {

    @Test
    fun testCountDownLatch() {
        val latch = CountDownLatch(5)
        val helper=TestHelper(5.seconds)
        helper.createAndStartMultiple(5) { ix, isDone ->
            helper.thread {
                Thread.sleep(ix*100L)
                //println("thread $ix done")
                latch.countDown()
            }
        }
        latch.await()
        assertEquals(0, latch.getCount(), "Latch count should be zero")
    }
    @Test
    fun testCountDownLatchWithTime() {
        val latch = CountDownLatch(5)
        val helper=TestHelper(5.seconds)
        helper.createAndStartMultiple(5) { ix, isDone ->
            helper.thread {
                Thread.sleep(ix*100L)
                //println("thread $ix done")
                latch.countDown()
            }
        }
        latch.await(1,TimeUnit.SECONDS)
        assertEquals(0, latch.getCount(), "Latch count should be zero")
    }


    @Test
    fun testCountDownLatchTimedOut() {
        val latch = CountDownLatch(5)
        val helper=TestHelper(5.seconds)
        helper.createAndStartMultiple(5) { ix, isDone ->
            helper.thread {
                Thread.sleep(ix*100L)
                //println("thread $ix done")
                latch.countDown()
            }
        }
        assertFalse(latch.await(1,TimeUnit.NANOSECONDS))
    }

    @Test
    fun testCountDownLatchInvalidParameter() {
        val latch = CountDownLatch(5)
        val helper=TestHelper(5.seconds)
        helper.createAndStartMultiple(5) { ix, isDone ->
            helper.thread {
                Thread.sleep(ix*100L)
                //println("thread $ix done")
                latch.countDown()
            }
        }
        assertThrows<IllegalArgumentException>{latch.await(0,TimeUnit.NANOSECONDS)}
    }

    @Test
    fun testCountDownLatchNotEnoughThreads() {
        val latch = CountDownLatch(6)
        val helper=TestHelper(5.seconds)
        helper.createAndStartMultiple(5) { ix, isDone ->
            helper.thread {
                Thread.sleep(ix*100L)
                //println("thread $ix done")
                latch.countDown()
            }
        }
        while(!helper.isDone()) {
            if (latch.getCount()==0)
            break
        }
        assertEquals(1, latch.getCount(), "Latch count should be one")
        assertTrue(helper.isDone(),"Since latch count didn't reach zero the latch awaits indefinitely")

    }

}
