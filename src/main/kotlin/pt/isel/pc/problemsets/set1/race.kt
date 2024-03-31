package pt.isel.pc.problemsets.set1
import java.time.Duration

@Throws(InterruptedException::class)
fun <T> race(suppliers: List<()->T>, timeout: Duration): T?{
    var x:T? = null
    suppliers.forEach {
        Thread {
            val result= it();
            Thread.currentThread().threadGroup.interrupt()
            x = result
        }.start()
    }
    val z= Thread.getAllStackTraces()
    for(t in Thread.getAllStackTraces().keys){
        if(t!= Thread.currentThread()){
            t.join(timeout.toNanos())
        }
    }
    return x
}
