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

        resourceManager.release()
        assertTrue(outputStream.isClosed)
    }

    @Test
    fun testReleaseMultipleUsages() {
        val outputStream = ByteArrayOutputStream()
        val resourceManager = SafeResourceManager(OutputStreamWriter(outputStream), 3)


        resourceManager.release()
        resourceManager.release()
        resourceManager.release()

        assertTrue(outputStream.isClosed)
    }

    @Test
    fun testReleaseMultipleUsagesWithThrow() {
        val outputStream = ByteArrayOutputStream()
        val resourceManager = SafeResourceManager(OutputStreamWriter(outputStream), 3)

        resourceManager.release()
        resourceManager.release()
        resourceManager.release()

        assertThrows(IllegalStateException::class.java) {
            resourceManager.release()
        }
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

        repeat(numThreads) {
            threads.add(thread {
                try {
                    resourceManager.release()
                }catch(e:Exception){
                    throwCounter.incrementAndGet()
                }
            })
        }

        threads.forEach { it.join() }
        assertTrue(throwCounter.get() == 5)
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
