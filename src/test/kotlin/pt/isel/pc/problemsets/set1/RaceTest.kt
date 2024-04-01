package pt.isel.pc.problemsets.set1

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import java.time.Duration.*

import kotlin.test.assertTrue


class RaceTest {

    fun test():Long{
        Thread.sleep(1000)
        return 1L
    }
    fun test1():Long{
        val timeToSleep = System.currentTimeMillis() % 10 + 1
        Thread.sleep(1100 * timeToSleep)
        return 2L
    }

    @Test
    fun testRace(){
        val suppliers:List<() -> Long> =
            listOf(
                ::test,
                ::test1
            )
        val timeout= ofMillis(20000)
        val result= race(suppliers,timeout)
        assertTrue(result== test())
    }


    @Test
    fun testMultipleRace(){
        val suppliers:List<() -> Long> =
            listOf(
                ::test1,
                ::test,
                ::test1,
                ::test1,
                ::test1,
                ::test1,
            )
        val timeout= ofMillis(20000)
        val result= race(suppliers,timeout)
        assertTrue(result== test())
    }

    @Test
    fun testRaceTimesOut(){
        val suppliers:List<() -> Long> =
            listOf(
                ::test,
                ::test1
            )
        val timeout= ofMillis(100)
        assertTrue(race(suppliers,timeout)==null)
    }

    @Test
    fun testRaceThrow(){
        val suppliers:List<() -> Long> =
            listOf(
                ::test,
                ::test1
            )
        val timeout= ofMillis(10000)
        val th = Thread{
            race(suppliers,timeout)
        }
        th.start()

        th.interrupt()
        assertTrue(th.isInterrupted)
        
    }




}
