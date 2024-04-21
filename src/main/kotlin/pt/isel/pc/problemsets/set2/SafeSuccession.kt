package pt.isel.pc.problemsets.set2

import java.util.concurrent.atomic.AtomicInteger

class SafeSuccession<T>(
    private val items: Array<T>
) {
    private var index = AtomicInteger(0)
    fun next(): T? {
        while(true){
            val observedIdx = index.get()
            if (observedIdx < items.size ) {
                if(index.compareAndSet(observedIdx, observedIdx + 1))
                    return items[observedIdx]
            }
            else return null
        }
    }
}
