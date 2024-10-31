package com.crisiscleanup.core.common

import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

fun Collection<String?>.filterNotBlankTrim() = filter { it?.isNotBlank() == true }
    .filterNotNull()
    .map(String::trim)

fun Collection<String?>.combineTrimText(separator: String = ", ") =
    filterNotBlankTrim().joinToString(separator)

fun String.urlEncode(): String = URLEncoder.encode(this, StandardCharsets.UTF_8.toString())
fun String.urlDecode(): String = URLDecoder.decode(this, StandardCharsets.UTF_8.toString())
