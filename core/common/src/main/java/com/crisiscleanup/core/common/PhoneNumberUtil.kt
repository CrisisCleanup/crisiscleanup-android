package com.crisiscleanup.core.common

import androidx.annotation.VisibleForTesting
import kotlin.math.abs

object PhoneNumberUtil {
    private val noNumbersPattern = """^\D+$""".toRegex()

    private val isTenDigitNumberPattern = """^\s*\d{10}\s*$""".toRegex()
    private val isOneTenDigitNumberPattern = """^\s*1\d{10}\s*$""".toRegex()

    private val commonPhoneFormat1Pattern =
        """^\+?1?\s?\((\d{3})\)\s*(\d{3})[. -](\d{4})\s*$""".toRegex()

    private val inParenthesisPattern = """\((\d{3})\)""".toRegex()
    private val leadingOnePattern = """(?:^|\b)\+?1\s""".toRegex()
    private val compact334DelimiterPattern =
        """(?:^|\b)1?(\d{3})(.)(\d{3})\2(\d{4})(?:$|\b)""".toRegex()
    private val compact334SpacePattern =
        """(?:^|\b)1?(\d{3})\s+(\d{3})\s+(\d{4})(?:$|\b)""".toRegex()
    private val digitDashDigitPattern = """(\d)-(\d)""".toRegex()
    private val digitEndParenthesisDigitPattern = """(\d)\) (\d)""".toRegex()
    private val compact37Pattern = """(?:^|\b)1?(\d{3})[.-]?\s(\d{7})(?:$|\b)""".toRegex()
    private val compact64Pattern = """(?:^|\b)1?(\d{6})[.-]?\s(\d{4})(?:$|\b)""".toRegex()
    private val leadingOnePostfixPattern = """(?:^|\b)1(?:- )?(\d{10})(?:$|\b)""".toRegex()
    private val nonNumericEndsPattern = """^\D*\b(\d{10})\b\D*$""".toRegex()
    private val repeatingNumbersPattern = """\b(\d)\1{4,}\b""".toRegex()
    private val shortWordsPattern = """\b[a-zA-Z]{3,}\b""".toRegex()

    private val digitSequencePattern = """(?:^|\b)1?(\d{9,16})(?:$|\D+)""".toRegex()
    private val separated37Pattern = """(?:^|\b)(\d{3})[. -](\d{7})(?:$|\b)""".toRegex()
    private val nonDigitPattern = """\D""".toRegex(RegexOption.MULTILINE)

    private val String.exactTenDigits: String?
        get() {
            if (isBlank()) {
                return ""
            }
            if (isOneTenDigitNumberPattern.matches(this)) {
                return this.trim().substring(1)
            }
            if (isTenDigitNumberPattern.matches(this)) {
                return this.trim()
            }
            return null
        }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun parsePhoneNumbers(raw: String): ParsedPhoneNumber? {
        if (raw.trim().length < 6) {
            return null
        }

        fun singleParsedNumber(result: String) =
            ParsedPhoneNumber(raw, listOf(result.trim()))

        raw.exactTenDigits?.let {
            return singleParsedNumber(it)
        }

        if (noNumbersPattern.matches(raw)) {
            return null
        }

        commonPhoneFormat1Pattern.matchEntire(raw)?.let {
            with(it.groupValues) {
                return singleParsedNumber("${get(1)}${get(2)}${get(3)}")
            }
        }

        val unparenthesized = raw.replace(inParenthesisPattern, "$1")
        val leadingOneTrimmed = unparenthesized.replace(leadingOnePattern, "")
        val threeThreeFourUndelimited =
            leadingOneTrimmed.replace(compact334DelimiterPattern, " $1$3$4 ")
                .also {
                    it.exactTenDigits?.let { tenDigits ->
                        return singleParsedNumber(tenDigits)
                    }
                }

        val threeThreeFourUnspaced =
            threeThreeFourUndelimited.replace(compact334SpacePattern, " $1$2$3 ")
        val dashesRemoved = threeThreeFourUnspaced.replace(digitDashDigitPattern, "$1$2")
        val endParenthesisRemoved = dashesRemoved.replace(digitEndParenthesisDigitPattern, "$1$2")
            .also {
                it.exactTenDigits?.let { tenDigits ->
                    return singleParsedNumber(tenDigits)
                }
            }

        val threeSevenCompacted = endParenthesisRemoved.replace(compact37Pattern, " $1$2 ")
            .also {
                it.exactTenDigits?.let { tenDigits ->
                    return singleParsedNumber(tenDigits)
                }
            }

        val sixFourCompacted = threeSevenCompacted.replace(compact64Pattern, " $1$2 ")
        val onePopped = sixFourCompacted.replace(leadingOnePostfixPattern, "$1")
        val nonNumericEnds = onePopped.replace(nonNumericEndsPattern, "$1")
            .also {
                it.exactTenDigits?.let { tenDigits ->
                    return singleParsedNumber(tenDigits)
                }
            }

        val repeatNumbersRemoved = nonNumericEnds.replace(repeatingNumbersPattern, "")
        val noShortWords = repeatNumbersRemoved.replace(shortWordsPattern, " ")

        fun getMatches(
            pattern: Regex,
            s: String,
            matchMaker: (MatchResult) -> String = { it.groupValues[1] },
        ): ParsedPhoneNumber? {
            val patternMatches = pattern.findAll(s)
            if (patternMatches.any()) {
                val matches = patternMatches.map(matchMaker)
                val matchList = matches.toList()
                return if (matchList.size > 1) {
                    ParsedPhoneNumber(raw, matches.toList())
                } else {
                    singleParsedNumber(matches.first())
                }
            }
            return null
        }

        getMatches(digitSequencePattern, noShortWords)?.let {
            return it
        }

        val trimmedLength = noShortWords.trim().length
        if (trimmedLength in 10..15) {
            if (trimmedLength <= 12) {
                separated37Pattern.find(noShortWords)?.let {
                    return singleParsedNumber("${it.groupValues[1]}${it.groupValues[2]}")
                }
            }

            val onlyNumbers = noShortWords.replace(nonDigitPattern, "")
            if (onlyNumbers.length in 9..11) {
                return singleParsedNumber(onlyNumbers)
            }
        }

        return null
    }

    fun getPhoneNumbers(possiblePhoneNumbers: List<String?>) = possiblePhoneNumbers
        .asSequence()
        .mapNotNull { it }
        .mapNotNull(::parsePhoneNumbers)
        .toList()

    fun searchablePhoneNumbers(phone1: String, phone2: String?): String =
        getPhoneNumbers(listOf(phone1, phone2))
            .asSequence()
            .map(ParsedPhoneNumber::parsedNumbers)
            .flatten()
            .filter(String::isNotBlank)
            .map {
                if (it.startsWith("1") && it.length == 11) {
                    it.substring(1)
                } else {
                    it
                }
            }
            .flatMap {
                if (it.length == 10) {
                    listOf(it, it.substring(3))
                } else {
                    listOf(it)
                }
            }
            .toList()
            .sortedWith(
                { a, b ->
                    if (a.length == 10) {
                        return@sortedWith -1
                    }
                    if (b.length == 10) {
                        return@sortedWith 1
                    }
                    val closestToTen = abs(a.length - 10) - abs(b.length - 10)
                    if (closestToTen <= 0) {
                        -1
                    } else {
                        1
                    }
                },
            )
            .joinToString(" ")
}

data class ParsedPhoneNumber(
    val source: String,
    val parsedNumbers: List<String>,
)
