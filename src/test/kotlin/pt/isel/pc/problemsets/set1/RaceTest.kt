package pt.isel.pc.problemsets.set1

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.seconds

class RaceTest {
    @Test
    fun <T> testRaceTimesOut(){
        val suppliers =
            listOf(
                { Thread.sleep(400); (1 as T) },
                { Thread.sleep(600); (2 as T) }
            )
        val timeout= 0.2.seconds
        assertNull(race(suppliers,timeout),"testRaceTimesOut Successful")
    }

    @Test
    fun <T> testRaceDoesNotTimeOut(){
        val suppliers: List<()->T> =
            listOf(
                { Thread.sleep(400); 1 as T},
                { Thread.sleep(600);2 as T}
            )
        val timeout= 3.seconds
       assertNotNull(race(suppliers,timeout),"testRaceDoesNotTimeOut Successful")
    }
}