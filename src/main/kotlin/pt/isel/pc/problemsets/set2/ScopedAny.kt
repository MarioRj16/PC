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
                    //if it was successfull the first to reach this if will run success and change the value
                    if (success.compareAndSet(null, value)) {
                        onSuccess(value)
                    }
                } else {
                    //if it wasn't successfull it adds the throwable to our exception
                    exceptions.addSuppressed(throwable)
                    logger.info("Future completed with exception: $throwable")
                }
            } finally {
                //before the handle finishes it decrements the counter of the futures still running
                logger.info("Decrementing counter")
                if (counter.decrementAndGet() == 0) {
                    //if the future is the last one it completes the returned CompletableFuture
                    logger.info("All futures completed")
                    val observedSuccess = success.get()
                    if(observedSuccess != null){
                        //if the success is diferent than null it means there was a succesfull one so
                        //we complete it with the value of that first one
                        logger.info("Completed with result ${observedSuccess}")
                        result.complete(observedSuccess)
                    } else {
                        //if the success is null it means all the futures didn't complete successfully so
                        //we complete the returned completable future with the exception with all the suppressed
                        //exceptions inside
                        logger.info("Completed with exception ${exceptions}")
                        result.completeExceptionally(exceptions)
                    }
                }
            }
        }
    }
    //returns the completable future immediately
    return result
}