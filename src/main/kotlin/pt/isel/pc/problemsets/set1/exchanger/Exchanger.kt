package pt.isel.pc.problemsets.set1.exchanger

import java.util.Stack
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.Semaphore
import kotlin.concurrent.withLock


class Exchanger<T> {
    private val lock = ReentrantLock()
    private val condition = lock.newCondition()
    private var data: T? = null
    private var occupied:Boolean=false

    fun exchange(myData: T): T? {
        lock.withLock {
                while (occupied) condition.await()
                var previous: T? = null
                if (data == null) {
                    data = myData
                    condition.signalAll()
                    //wait for data update
                    while(data==myData) condition.await();

                    previous = data
                    data = null

                    occupied=false
                    condition.signalAll()

                } else {
                    occupied=true
                    previous = data
                    data = myData

                    condition.signalAll()

                    //must signal so that first thread knows data has been changed

                }

                return previous

        }
    }
}
/*
        private val lock = ReentrantLock()
        private val condition = lock.newCondition()
        private var first: T? = null
        private var second: T? = null
        private var count=AtomicInteger(0)


        fun exchange(item: T): T {
            lock.withLock {
                while(count.get()>1) condition.await()
               if(first==null){
                   count.incrementAndGet()
                   first=item
                   condition.await()
                   count.decrementAndGet()
                   condition.signalAll()
                   return second!!
               }else{ count.incrementAndGet()
                   second=item
                   condition.signal()
                   count.decrementAndGet()
                   return first!!
               }
            }}
            }
 */

      /*  fun exchange(item: T): T {
            lock.lock()
            try {
                count.incrementAndGet()
                while(count.get()%2==1) condition.await()
                if (first == null ) {
                    first = item
                    while (second == null) {
                        condition.await()
                    }
                    val temp = second
                    second = null
                    count.decrementAndGet()
                    condition.signal()
                    return temp!!
                } else if (second==null){
                    second = item
                    val temp = first
                    first = null
                    count.decrementAndGet()
                    condition.signal()
                    return temp!!
                }
                return item
            } finally {
                lock.unlock()
            }}

    }

       */

        /*lock.lock()
        try {
            val previousData = data
            data = myData
            condition.signalAll()
            while (data === myData) {
                condition.await()
            }

            return previousData
        } finally {
            lock.unlock()
        }
    }

         */



fun main() {
    val exchanger = Exchanger<Int>()
    val threadA = Thread {
        val myData = 123
        val receivedData = exchanger.exchange(myData)
        println("Thread A sent $myData and received $receivedData")
    }

    val threadF = Thread {
        val myData = 432
        val receivedData = exchanger.exchange(myData)
        println("Thread A sent $myData and received $receivedData")
    }

    val threadB = Thread {
        val myData = 458
        val receivedData = exchanger.exchange(myData)
        println("Thread B sent $myData and received $receivedData")
    }

    val threadC = Thread {
        val myData = 500
        val receivedData = exchanger.exchange(myData)
        println("Thread C sent $myData and received $receivedData")
    }

    val threadD = Thread {
        val myData = 505
        val receivedData = exchanger.exchange(myData)
        println("Thread D sent $myData and received $receivedData")
    }

    threadA.start()
    threadB.start()
    threadC.start()
    threadD.start()
   // threadF.start()

    threadA.join()
    threadB.join()
    threadC.join()
    threadD.join()
   // threadF.join()
}
