package pt.isel.pc.problemsets.set2


import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicReference

fun <T> scopedAny(futures: List<CompletableFuture<T>>, onSuccess: (T)->Unit): CompletableFuture<T> {
    val result = AtomicReference<CompletableFuture<T>?>(null)
    val treads : List<Thread> = futures.map { future ->
        Thread {
            try{
                val value = future.get()
                if (result.compareAndSet(null, future)) {
                   onSuccess(value)
                }
            }catch (e:Exception){
               //dont showException in console
            }

        }
    }
    treads.forEach { it.start() }
    treads.forEach { it.join() }
    if(result.get() == null) throw IllegalStateException("No future completed")
    return result.get()!!

}