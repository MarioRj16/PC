package pt.isel.pc.problemsets.set1
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import pt.isel.pc.problemsets.utils.TestHelper
import java.util.concurrent.RejectedExecutionException
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.seconds

class ThreadPoolExecutorTest {


    @Test
    fun testThreadPoolExecutorExecute() {
       repeat(1){
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
        assertTrue {  executor.awaitTermination(Duration.ofSeconds(3))}
        assertEquals(taskCount, times.get())}
    }


    @Test
    fun testThreadPoolExecutorExecuteMultiple() {
        repeat(100){
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
    fun testThreadPoolExecutorExecuteTimedOut() {
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
    fun testThreadPoolExecutorShutdown() {
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
    fun testThreadPoolExecutorAwaitTermination() {
        val executor = ThreadPoolExecutor(5, Duration.ofSeconds(10))

        executor.execute(Runnable {
            Thread.sleep(2000) // Simulate a long-running task
        })

        val result = executor.awaitTermination(Duration.ofSeconds(3))

        assertTrue(result)
    }

    @Test
    fun testThreadPoolExecutorInterruption(){
        val executor = ThreadPoolExecutor(5, Duration.ofSeconds(10))
        val th=Thread{executor.execute(Runnable {
            Thread.sleep(2000)})}
        th.start()
        th.interrupt()
        assertTrue(th.isInterrupted)
    }

    @Test
    fun testCountDownLatchInvalidArgument(){
        assertThrows<IllegalArgumentException> {ThreadPoolExecutor(0,Duration.ZERO)}
    }


    @Test
    fun testThreadPoolExecutorAwaitErrorTermination() {
        val executor = ThreadPoolExecutor(5, Duration.ofSeconds(10))

        executor.execute(Runnable {
            Thread.sleep(2000) // Simulate a long-running task
        })

        val result = executor.awaitTermination(Duration.ofSeconds(1))

        assertFalse(result)
    }
}
