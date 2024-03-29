package pt.isel.pc.problemsets.set1

import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.locks.Condition
import kotlin.concurrent.withLock


class Exchanger<T> {

    data class message<T>(var data:T, val locked:Condition)
    private val lock = ReentrantLock()
    private val condition = lock.newCondition()
    private var waiter: message<T>? = null
    private var occupied:Boolean=false

    fun exchange(myData: T): T? {
        lock.withLock {
            while(occupied) condition.await()
            var previous: T? = null
            if (waiter == null) {
                waiter = message(myData,lock.newCondition())
                condition.signal()
                waiter!!.locked.await()
                    //wait for data update


                previous = waiter!!.data
                waiter = null
                occupied=false
                condition.signal()
                } else {
                    occupied=true
                    previous = waiter!!.data

                    waiter!!.data=myData
                    waiter!!.locked.signal()

                    //must signal so that first thread knows data has been changed

                }
                return previous
        }
    }
}
