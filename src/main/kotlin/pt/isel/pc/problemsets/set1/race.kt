package pt.isel.pc.problemsets.set1

import kotlin.time.Duration

@Throws(InterruptedException::class)
fun <T> race(suppliers: List<()->T>, timeout: Duration): T?{
    var x:T?=null
    var endTime= System.nanoTime() + timeout.inWholeNanoseconds
    suppliers.forEach {
        try {
            val thread = Thread {
                x = it()
                if(endTime - System.nanoTime() <= 0) throw RuntimeException()
                throw InterruptedException()
            }
            thread.start()
        }catch (e: InterruptedException){
            Thread.currentThread().threadGroup.interrupt()
            return x

        } catch (e: RuntimeException){
            Thread.currentThread().threadGroup.interrupt()
            return null
        }

    }
    return null
}
