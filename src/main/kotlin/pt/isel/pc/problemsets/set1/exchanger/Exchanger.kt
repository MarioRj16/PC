package pt.isel.pc.problemsets.set1.exchanger

import java.util.concurrent.locks.ReentrantLock


class Exchanger<T> {
    private val lock = ReentrantLock()
    private val condition = lock.newCondition()
    private var data: T? = null

    fun exchange(myData: T): T? {
        lock.lock()
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
}


fun main() {
    val exchanger = Exchanger<Int>()

    val threadA = Thread {
        val myData = 123
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

    threadA.join()
    threadB.join()
    threadC.join()
    threadD.join()
}
