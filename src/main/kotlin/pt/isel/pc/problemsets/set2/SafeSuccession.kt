package pt.isel.pc.problemsets.set2

import java.util.concurrent.atomic.AtomicInteger

class SafeSuccession<T>(
    private val items: Array<T>
) {
    private val index = AtomicInteger(0)
    fun next(): T? {
        while(true){
            val observedIdx = index.get()
            if (observedIdx < items.size ) {
                //if the observed idx is equal to the observed value we can increment the index and return the current value
                if(index.compareAndSet(observedIdx, observedIdx + 1))
                    return items[observedIdx]
                //if the observed idx isn't equal it means some other thread changed it's value midway
                //through our the time we got the observed idx and the time we compared it in this if
                //so it retries inside the while true
            }
            //if the observed idx is already higher or equal to the items size we can already return null
            //since there isn't a next available
            else return null
        }
    }
}
