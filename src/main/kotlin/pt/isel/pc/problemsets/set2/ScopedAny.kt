package pt.isel.pc.problemsets.set2


import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.logging.Logger


fun <T> scopedAny(futures: List<CompletableFuture<T>>, onSuccess: (T)->Unit): CompletableFuture<T> {
    val logger= Logger.getLogger("ScopedAny")
    val success = AtomicReference<T?>(null)
    val counter = AtomicInteger(futures.size)
    val exceptions = Exception()
    val result = CompletableFuture<T>()
    futures.forEach { future ->
        future.handleAsync{ value, throwable ->
            logger.info("Future completed with value: $value")
            try {
                if (throwable == null) {
                    if (success.compareAndSet(null, value)) {
                        onSuccess(value)
                    }
                } else {
                    exceptions.addSuppressed(throwable)
                    logger.info("Future completed with exception: $throwable")
                }
            } finally {
                logger.info("Decrementing counter")
                if (counter.decrementAndGet() == 0) {
                    logger.info("All futures completed")
                    val observedSuccess = success.get()
                    if(observedSuccess != null){
                        logger.info("Completed with result ${observedSuccess}")
                        result.complete(observedSuccess)
                    } else {
                        logger.info("Completed with exception ${exceptions}")
                        result.completeExceptionally(exceptions)
                    }
                }
            }
        }
    }
    return result
}