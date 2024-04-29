package pt.isel.pc.problemsets.set2


import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.logging.Logger


fun <T> scopedAny(futures: List<CompletableFuture<T>>, onSuccess: (T)->Unit): CompletableFuture<T> {
    val logger= Logger.getLogger("ScopedAny")
    val result = AtomicReference<CompletableFuture<T>?>(null)
    val counter = AtomicInteger(futures.size)
    val isFinished= CompletableFuture<Any>()
    futures.forEach { future ->
        future.handleAsync{ value, throwable ->
            logger.info("Future completed with value: $value")
            try {
                if (throwable == null) {
                    if (result.compareAndSet(null, future)) {
                        onSuccess(value)
                    }
                } else {
                    logger.info("Future completed with exception: $throwable")
                }
            } finally {
                logger.info("Decrementing counter")
                if (counter.decrementAndGet() == 0) {
                    logger.info("All futures completed")
                    result.get()?.complete(value)
                    isFinished.complete(true)
                }
            }
        }
    }
    isFinished.join()
    return result.get() ?: throw IllegalStateException("No future completed")
}