package pt.isel.pc.problemsets.set2
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.concurrent.thread


class ScopedAnyTest {

    @Test
    fun testScopedAny() {
        val futures = listOf(
            CompletableFuture.failedFuture(IllegalStateException()),
            CompletableFuture.completedFuture(1),
            CompletableFuture.completedFuture(2),
            CompletableFuture.completedFuture(3)
        )

        val result = scopedAny(futures) { value ->
            println("Completed with value: $value")
            assertTrue{ value in 1..3}
        }
        println("result")
        assertTrue{ result.join() in 1..3}
    }

    @Test
    fun testScopedAnyMultiple() {
        val results = ConcurrentLinkedQueue<Int>()
        repeat(100){
            val futures = listOf(
                CompletableFuture.failedFuture(IllegalStateException()),
                CompletableFuture.completedFuture(1),
                CompletableFuture.completedFuture(2),
                CompletableFuture.completedFuture(3)
            )
            val result = scopedAny(futures) { value ->
                println("Completed with value: $value")
                assertTrue{ value in 1..3}
            }
            println("result")
            assertTrue{ result.join() in 1..3}
            results.add(result.join())
        }
        println(results)
    }
    @Test
    fun testScopedExceptions() {
        val futures = listOf<CompletableFuture<Any>>(
            CompletableFuture.failedFuture(IllegalStateException()),
            CompletableFuture.failedFuture(IllegalArgumentException()),
            CompletableFuture.failedFuture(NumberFormatException()),
            CompletableFuture.failedFuture(NullPointerException())
        )
        try{
            val result = scopedAny(futures) { value ->
            println("Completed with value: $value")
            }
            result.join()
        }catch(e:Exception){
            return assertEquals( e.cause?.suppressed?.size,4)
        }
        assertEquals(0,1)
    }

    @Test
    fun testSecondFutureCompletesFirst() {
        val future1 = CompletableFuture<Int>()
        val future2 = CompletableFuture<Int>()

        thread {
            Thread.sleep(500)
            future1.complete(1)
        }

        future2.complete(2)

        val resultFuture = scopedAny(listOf(future1, future2)) { value ->
            println("Completed with value: $value")
        }

        val result = resultFuture.join()

        assertEquals(2, result)
    }
}