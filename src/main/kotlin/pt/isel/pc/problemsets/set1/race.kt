package pt.isel.pc.problemsets.set1
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock


@Throws(InterruptedException::class)
fun <T> race(suppliers: List<()->T>, timeout: Duration): T?{
    val lock=ReentrantLock()
    val condition=lock.newCondition()
    val ans = AtomicReference<T?>(null)
    val threads = mutableListOf<Thread>()
    suppliers.forEachIndexed {key, value ->
        threads += Thread {
            try{
                println("Thread $key started running")
                val result = value();
                println("Thread $key finished with value $result")
                //println("Thread $key found answer to be $ans")
                lock.withLock {
                    if (ans.get() == null) {
                        ans.set(result)
                        println("SETTED")
                        condition.signalAll()
                    }
                }
            } catch (e: InterruptedException){
                println("Thread $key caught interruption")
            }


        }
    }

    threads.forEach{ it.start() }
    var timeoutInNanos = timeout.toNanos()
    while (true){
        try{
            lock.withLock {
                timeoutInNanos = condition.awaitNanos(timeoutInNanos)
            }
        } catch (e: InterruptedException){
            println("passei aqui")
            threads.forEach{
                it.interrupt()
                it.join()
            }
            println("Main thread is about to throw")
            throw e
        }
        println("Main thread woke up")

        if(timeoutInNanos <= 0){
            println("Main thread time out")
            threads.forEach{
                it.interrupt()
            }
            return null
        }

        if(ans.get() != null)
            return ans.get()
    }
}
