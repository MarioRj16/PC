package pt.isel.pc.problemsets.set1

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import java.time.Duration.*


class RaceTest {
    @Test
    fun <T> testRaceTimesOut(){
        val suppliers =
            listOf(
                { Thread.sleep(400)},
                { Thread.sleep(600)}
            )
        val timeout= ofMillis(200)
        assertNull(race(suppliers,timeout),"testRaceTimesOut Successful")
    }

    @Test
    fun <T> testRaceDoesNotTimeOut(){
        val suppliers: List<()->T> =
            listOf(
                { Thread.sleep(400); 1 as T},
                { Thread.sleep(600);2 as T}
            )
        val timeout= ofSeconds(2)
       assertNotNull(race(suppliers,timeout),"testRaceDoesNotTimeOut Successful")
    }
}