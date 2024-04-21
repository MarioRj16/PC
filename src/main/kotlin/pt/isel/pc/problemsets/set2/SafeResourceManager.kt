package pt.isel.pc.problemsets.set2

import java.util.concurrent.atomic.AtomicInteger

class SafeResourceManager(private val obj: AutoCloseable, usages: Int) {

    private val currentUsages = AtomicInteger(usages)
    fun release() {
        val observedUsages = currentUsages.get()
        if (currentUsages.get() == 0) {
            throw IllegalStateException("usage count is already zero")
        }
        if (currentUsages.compareAndSet(observedUsages, observedUsages - 1) && observedUsages == 1) {
            obj.close()
        }
    }
}