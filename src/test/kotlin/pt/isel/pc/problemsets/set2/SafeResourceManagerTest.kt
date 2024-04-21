import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import pt.isel.pc.problemsets.set2.SafeResourceManager
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

class SafeResourceManagerTest {

    @Test
    fun testReleaseSingleUsage() {
        val outputStream = ByteArrayOutputStream()
        val resourceManager = SafeResourceManager(OutputStreamWriter(outputStream), 1)

        // Release the resource
        resourceManager.release()

        // Verify that the resource was closed
        assertTrue(outputStream.isClosed)
    }

    @Test
    fun testReleaseMultipleUsages() {
        val outputStream = ByteArrayOutputStream()
        val resourceManager = SafeResourceManager(OutputStreamWriter(outputStream), 3)

        // Release the resource three times

        resourceManager.release()
        resourceManager.release()
        resourceManager.release()

        // Verify that the resource was closed after the last release
        assertTrue(outputStream.isClosed)
    }

    @Test
    fun testReleaseMultipleUsagesWithThrow() {
        val outputStream = ByteArrayOutputStream()
        val resourceManager = SafeResourceManager(OutputStreamWriter(outputStream), 3)

        // Release the resource three times

        resourceManager.release()
        resourceManager.release()
        resourceManager.release()

        assertThrows(IllegalStateException::class.java) {
            resourceManager.release()
        }
        // Verify that the resource was closed after the last release
        assertTrue(outputStream.isClosed)
    }

    @Test
    fun testReleaseZeroUsages() {
        val outputStream = ByteArrayOutputStream()
        val resourceManager = SafeResourceManager(OutputStreamWriter(outputStream), 0)

        assertThrows(IllegalStateException::class.java) {
            resourceManager.release()
        }
    }

    @Test
    fun testConcurrentResourceRelease() {
        val outputStream = ByteArrayOutputStream()
        val resourceManager = SafeResourceManager(OutputStreamWriter(outputStream), 5)

        val numThreads = 10
        val threads = mutableListOf<Thread>()
        val throwCounter = AtomicInteger(0)

        // Create and start multiple threads to release the resource concurrently
        repeat(numThreads) {
            threads.add(thread {
                try {
                    resourceManager.release()
                }catch(e:Exception){
                    throwCounter.incrementAndGet()
                }
            })
        }

        // Wait for all threads to complete
        threads.forEach { it.join() }
        assertTrue(throwCounter.get() == 5)
        // Verify that the resource was closed after all releases
        assertTrue(outputStream.isClosed)
    }

    private val OutputStream.isClosed: Boolean
        get() = try {
            this.close()
            true
        } catch (e: Exception) {
            false
        }
}
