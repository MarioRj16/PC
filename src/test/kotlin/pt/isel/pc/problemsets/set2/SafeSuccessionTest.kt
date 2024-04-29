import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import pt.isel.pc.problemsets.set2.SafeSuccession
import pt.isel.pc.problemsets.utils.TestHelper
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread
import kotlin.time.Duration.Companion.seconds


class SafeSuccessionTest {

    @Test
    fun testNextSequential() {
        val items = arrayOf(1, 2, 3, 4, 5)
        val succession = SafeSuccession(items)

        assertEquals(1, succession.next())
        assertEquals(2, succession.next())
        assertEquals(3, succession.next())
        assertEquals(4, succession.next())
        assertEquals(5, succession.next())
        assertNull(succession.next())
    }

    @Test
    fun testNextConcurrent() {
        val items = arrayOf(1, 2, 3, 4, 5)
        val succession = SafeSuccession(items)

        val numThreads = 10
        val latch = CountDownLatch(numThreads)
        val results = ConcurrentLinkedQueue<Int>()
        repeat(numThreads) {
            thread {
                while (true) {
                    val nextItem = succession.next()
                    if (nextItem != null) {
                        results.add(nextItem)
                    } else {
                        break
                    }
                }
                latch.countDown()
            }
        }
        latch.await()
        assertEquals(items.toList(), results.toList())
        assertNull(succession.next())
    }



    @Test
    fun next(){
        val NOF_THREADS = 200
        val array = arrayOf(0,1,2,3,4,5,6,7,8,9)
        val retArray = ConcurrentLinkedQueue<Int>()
        val sut = SafeSuccession(array)
        val testHelper = TestHelper(3.seconds)

        testHelper.createAndStartMultiple(NOF_THREADS){ _, _ ->
            val item = sut.next()
            if(item != null) retArray.add(item)
        }
        testHelper.join()
        assertEquals(array.toList(), retArray.sorted())
    }
}

