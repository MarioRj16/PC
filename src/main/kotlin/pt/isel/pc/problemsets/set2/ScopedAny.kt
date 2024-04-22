package pt.isel.pc.problemsets.set2


import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.logging.Logger

fun <T> scopedAny(futures: List<CompletableFuture<T>>, onSuccess: (T)->Unit): CompletableFuture<T> {
    //usar handle para dar manage dos que acabaram para depois retornar
    //meter counter para que o handler saiba se é o ultimo
    //ter um handler para cada completable future
    //handle function (t,Trowable -> Unit)
    val logger= Logger.getLogger("ScopedAny")
    val result = AtomicReference<CompletableFuture<T>?>(null)
    val counter = AtomicInteger(futures.size)
    futures.forEach { future ->
        future.handle { value, throwable ->
            logger.info("Future completed with value: $value")
            try {
                if (throwable == null) {
                    if (result.compareAndSet(null, future)) {
                        onSuccess(value)
                    }
                } else {
                //dont showException in console
                }
            } finally {
                logger.info("Decrementing counter")
                if (counter.decrementAndGet() == 0) {
                    logger.info("All futures completed")
                    result.get()?.complete(value)
                }
            }
        }
    }
    val x = result.get() ?: throw IllegalStateException("No future completed")
    return x
}