package pt.isel.pc.problemsets.set3

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.test.Test


class CoroutinesDemoTests {

    private val tid get() = Thread.currentThread().id

    @Test
    fun example() {
        runBlocking {
            println("[T$tid] :: LAUNCHED COROUTINE 2 ::")
            launch {
                println("[T$tid] :: RUNNING COROUTINE 2 ::") // This starts before launching all if multi threads
                delay(2000)
                println("[T$tid] :: COROUTINE 2 ENDED ::")
            }

            println("[T$tid] :: LAUNCHED COROUTINE 1 ::")
            launch {
                println("[T$tid] :: RUNNING COROUTINE 1 ::")
                println("[T$tid] :: LAUNCHED SUB-COROUTINE ::")
                launch {
                    println("[T$tid] :: RUNNING SUB-COROUTINE ::")
                    delay(1000)
                    println("[T$tid] :: SUB-COROUTINE ENDED ::")
                }
                println("[T$tid] :: COROUTINE 1 ENDED ::")
            }

            println("[T$tid] :: LAUNCHED ALL COROUTINES ::")
        }

        println("[T$tid] :: TEST ENDS ::")
    }
}