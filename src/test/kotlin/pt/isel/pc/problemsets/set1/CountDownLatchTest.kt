package pt.isel.pc.problemsets.set1

import org.junit.jupiter.api.Test
import pt.isel.pc.problemsets.utils.TestHelper
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class CountDownLatchTest {

    @Test
    fun testCountDownLatch() {
        val latch = CountDownLatch(5)
        val helper=TestHelper(5.seconds)
        helper.createAndStartMultiple(5) { ix, isDone ->
            helper.thread {
                Thread.sleep(ix*100L)
                println("thread $ix done")
                latch.countDown()
            }
        }
        latch.await()
        assertEquals(0, latch.getCount(), "Latch count should be zero")
    }

    @Test
    fun testCountDownLatchNotEnoughThreads() {
        val latch = CountDownLatch(6)
        val helper=TestHelper(5.seconds)
        helper.createAndStartMultiple(5) { ix, isDone ->
            helper.thread {
                Thread.sleep(ix*100L)
                println("thread $ix done")
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
