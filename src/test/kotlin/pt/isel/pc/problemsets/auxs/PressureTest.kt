package pt.isel.pc.problemsets.auxs

import java.time.LocalTime
import java.time.format.DateTimeFormatter


/**
 * Executes the given test function for the specified duration and checks for failures.
 *
 * @param test The test function to execute.
 * @param time The duration of the test in minutes.
 * @throws AssertionError if the test fails.
 * @throws Exception if any other exception is thrown during the test.
 */
fun pressureTest(test: () -> Unit, time: Long) {
    val endTime = LocalTime.now().plusMinutes(time)
    var iteration = 0
    println("\u001B[31mTest stops at ${endTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"))}")

    while (LocalTime.now() < endTime) {
        println("\u001B[31mIteration ${++iteration}")
        try {
            test()
        } catch (e: Throwable) {
            println("Failed at iteration $iteration")
            throw e
        }
    }
}