package com.crisiscleanup.core.common

object PhoneNumberUtil {
    private val bracketsDashRegex = """[()-]""".toRegex()
    private val letterRegex = """[a-zA-Z]""".toRegex()
    private val twoPlusSpacesRegex = """\s{2,}""".toRegex()
    private val nonNumberRegex = """\D""".toRegex()

    private val straightDigitsRegex = """^\d{10,11}$""".toRegex()
    private val threeThreeFourDigitsRegex = """^\d{3} \d{3} \d{4}$""".toRegex()
    private val areaCodeNumberRegex = """^\d{3} \d{7}$""".toRegex()
    private val twoPhoneNumbersRegex = """^(\d{10,11})\D+(\d{10,11})$""".toRegex()

    fun getPhoneNumbers(possiblePhoneNumbers: List<String?>) = possiblePhoneNumbers
        .filter { s -> s?.isNotBlank() == true }
        .map { s -> s!! }
        .map { phoneIn ->
            val filtered = phoneIn.trim()
                .trim()
            val cleaned = filtered.replace(bracketsDashRegex, "")
                .replace(letterRegex, " ")
                .replace(twoPlusSpacesRegex, "  ")
                .trim()

            if (cleaned.isBlank()) {
                return@map ParsedPhoneNumber(phoneIn, emptyList())
            }

            if (straightDigitsRegex.matches(cleaned)) {
                return@map ParsedPhoneNumber(phoneIn, listOf(cleaned))
            }

            if (threeThreeFourDigitsRegex.matches(cleaned) ||
                areaCodeNumberRegex.matches(cleaned)
            ) {
                val parsedNumber = cleaned.replace(" ", "")
                return@map ParsedPhoneNumber(phoneIn, listOf(parsedNumber))
            }

            twoPhoneNumbersRegex.matchEntire(cleaned)?.let {
                val parsedNumbers = listOf(it.groupValues[1], it.groupValues[2])
                return@map ParsedPhoneNumber(phoneIn, parsedNumbers)
            }

            val onlyNumbers = cleaned.replace(nonNumberRegex, "")
            if (straightDigitsRegex.matches(onlyNumbers)) {
                return@map ParsedPhoneNumber(phoneIn, listOf(onlyNumbers))
            }

            return@map ParsedPhoneNumber(phoneIn, emptyList())
        }
}

data class ParsedPhoneNumber(
    val source: String,
    val parsedNumbers: List<String>,
)