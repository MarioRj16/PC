package pt.isel.pc.problemsets.set1
import org.junit.jupiter.api.Assertions.assertFalse
import pt.isel.pc.problemsets.utils.TestHelper
import java.util.concurrent.RejectedExecutionException
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.INFINITE
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

class ThreadPoolExecutorTest {

    @OptIn(ExperimentalTime::class)
    @Test
    fun testExecute() {
        val executor = ThreadPoolExecutor(5,Duration.INFINITE)
        val taskCount = 10
        val executedTasks = mutableListOf<Int>()
        val test=TestHelper(10.seconds)
        test.createAndStartMultiple(taskCount,{idx, isDone ->
            executor.execute(Runnable {
            executedTasks.add(idx)
        })}
        )

        assertTrue {  executor.awaitTermination(5.seconds)}
        assertEquals(taskCount, executedTasks.size)
        assertTrue { executedTasks.all { it in 0 until taskCount } }
    }


    @Test
    fun testExecuteTimedOut() {
        val maxThreadPool=5
        val executor = ThreadPoolExecutor(maxThreadPool, Duration.ZERO)
        val taskCount = 10
        val executedTasks = mutableListOf<Int>()
        val test=TestHelper(10.seconds)
        test.createAndStartMultiple(taskCount,{idx, isDone ->
            executor.execute(Runnable {
                executedTasks.add(idx)
                Thread.sleep(10)

            })}
        )

        assertTrue {  executor.awaitTermination(5.seconds)}
        assertEquals(maxThreadPool,executedTasks.size )
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun testShutdown() {
        val executor = ThreadPoolExecutor(5, Duration.INFINITE)

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

    @OptIn(ExperimentalTime::class)
    @Test
    fun testAwaitErrorTermination() {
        val executor = ThreadPoolExecutor(5, Duration.INFINITE)

        executor.execute(Runnable {
            Thread.sleep(2000) // Simulate a long-running task
        })

        val result = executor.awaitTermination(3.seconds)

        assertTrue(result)
    }

    @Test
    fun testAwaitTermination() {
        val executor = ThreadPoolExecutor(5, Duration.INFINITE)

        executor.execute(Runnable {
            Thread.sleep(2000) // Simulate a long-running task
        })

        val result = executor.awaitTermination(1.seconds)

        assertFalse(result)
    }
}
