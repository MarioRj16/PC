package pt.isel.pc.problemsets.set2

import java.util.concurrent.atomic.AtomicInteger

class SafeResourceManager(private val obj: AutoCloseable, usages: Int) {

    private val currentUsages = AtomicInteger(usages)
    fun release() {
        while(true){
            val observedUsages = currentUsages.get()
            if (observedUsages == 0) {
                //if the observed usages is already zero we can throw it instantly
                throw IllegalStateException("usage count is already zero")
            }
            if (currentUsages.compareAndSet(observedUsages, observedUsages - 1)) {
                //we decrement the usages here and in case that was the last use we close the obj
                if(observedUsages == 1)  obj.close()
                return
            }
            //if the observed usages isn't equal it means some other thread changed it's value midway
            //through our the time we got the observed usages and the time we compared it in this if
            //so it retries inside the while true
        }
    }
}