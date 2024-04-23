package pt.isel.pc.problemsets.set2


import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock
import java.util.logging.Logger
import kotlin.concurrent.withLock

fun <T> scopedAny(futures: List<CompletableFuture<T>>, onSuccess: (T)->Unit): CompletableFuture<T> {
    //usar handle para dar manage dos que acabaram para depois retornar
    //meter counter para que o handler saiba se é o ultimo
    //ter um handler para cada completable future
    //handle function (t,Trowable -> Unit)

    //countDownLatch ???
    val logger= Logger.getLogger("ScopedAny")
    val result = AtomicReference<CompletableFuture<T>?>(null)
    val counter = AtomicInteger(futures.size)
    val lock = ReentrantLock()
    val condition = lock.newCondition()
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
                    lock.withLock {
                        logger.info("All futures completed")
                        result.get()?.complete(value)
                        condition.signal()
                    }
                }
            }
        }
    }
    lock.withLock {
        condition.await()
        val x = result.get() ?: throw IllegalStateException("No future completed")
        return x
    }
}