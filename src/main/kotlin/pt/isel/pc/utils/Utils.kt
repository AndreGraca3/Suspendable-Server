package pt.isel.pc.utils


/**
 * Extracts the timeout value from the input string.
 *
 * @param input The input string from which to extract the timeout value.
 * @return The timeout value as a Long if found, or null if no numeric value is present.
 */
fun extractTimeout(input: String): Long? {
    val timeoutString = input.findFirstNumeric()
    return timeoutString?.toLongOrNull()
}

/**
 * Finds the first numeric value in the string.
 *
 * @return The first numeric value found in the string, or null if no numeric value is present.
 */
fun String.findFirstNumeric(): String? {
    val regex = Regex("\\d+")
    return regex.find(this)?.value
}
