package pt.isel.pc.problemsets.set1
import org.junit.jupiter.api.Assertions.assertFalse
import pt.isel.pc.problemsets.utils.TestHelper
import java.util.concurrent.RejectedExecutionException
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.seconds

class ThreadPoolExecutorTest {


    @Test
    fun testExecute() {
        val executor = ThreadPoolExecutor(5,Duration.ofSeconds(10) )
        val taskCount = 10
        val times=AtomicInteger(0)
        val test=TestHelper(10.seconds)
        test.createAndStartMultiple(taskCount,{idx, isDone ->
            executor.execute(Runnable {
                times.incrementAndGet()

        })}
        )
        test.join()
        assertTrue {  executor.awaitTermination(Duration.ofSeconds(5))}
        assertEquals(taskCount, times.get())
    }


    @Test
    fun testExecuteMultiple() {
        repeat(1000){
            val executor = ThreadPoolExecutor(5,Duration.ofSeconds(10) )
            val taskCount = 10
            val times=AtomicInteger(0)
            val test=TestHelper(10.seconds)
            test.createAndStartMultiple(taskCount,{idx, isDone ->
                executor.execute(Runnable {
                    times.incrementAndGet()

                })}
            )
            test.join()
            assertTrue {  executor.awaitTermination(Duration.ofSeconds(5))}
            assertEquals(taskCount, times.get())
        }}

    @Test
    fun testExecuteTimedOut() {
            val maxThreadPool = 1
            val executor = ThreadPoolExecutor(maxThreadPool, Duration.ofNanos(1))
            val taskCount = 2
            val executedTasks = mutableListOf<Int>()
            val test = TestHelper(10.seconds)
            test.createAndStartMultiple(taskCount, { idx, isDone ->
                executor.execute(Runnable {
                    executedTasks.add(idx)
                    Thread.sleep(100)

                })
            }
            )
            test.join()
            assertTrue { executor.awaitTermination(Duration.ofSeconds(4)) }
            assertEquals(maxThreadPool,executedTasks.size )
    }

    @Test
    fun testShutdown() {
        val executor = ThreadPoolExecutor(5, Duration.ofSeconds(100))

        executor.execute(Runnable {
            Thread.sleep(2000) // Simulate a long-running task
        })

        executor.shutdown()

        // Attempt to submit another task after shutdown
        try {
            executor.execute(Runnable {
                println("This should not be executed.")
            })
        } catch (e: RejectedExecutionException) {
            // Expected exception
            return
        }

        // If RejectedExecutionException is not thrown, fail the test
        assertTrue(false, "Should throw RejectedExecutionException after shutdown")
    }

    @Test
    fun testAwaitTermination() {
        val executor = ThreadPoolExecutor(5, Duration.ofSeconds(10))

        executor.execute(Runnable {
            Thread.sleep(2000) // Simulate a long-running task
        })

        val result = executor.awaitTermination(Duration.ofSeconds(3))

        assertTrue(result)
    }

    @Test
    fun testThrow(){
        val executor = ThreadPoolExecutor(5, Duration.ofSeconds(10))
        val th=Thread{executor.execute(Runnable {
            Thread.sleep(2000)})}
        th.start()
        th.interrupt()
        assertTrue(th.isInterrupted)
    }

    @Test
    fun testAwaitErrorTermination() {
        val executor = ThreadPoolExecutor(5, Duration.ofSeconds(10))

        executor.execute(Runnable {
            Thread.sleep(2000) // Simulate a long-running task
        })

        val result = executor.awaitTermination(Duration.ofSeconds(1))

        assertFalse(result)
    }
}
