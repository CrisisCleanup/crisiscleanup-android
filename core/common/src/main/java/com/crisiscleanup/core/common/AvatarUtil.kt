package com.crisiscleanup.core.common

private val whiteSpaceRegex = "\\s+".toRegex()

val String.svgAvatarUrl: String
    get() {
        val seed = replace(whiteSpaceRegex, "-")
        return "https://api.dicebear.com/9.x/pixel-art/svg?seed=$seed"
    }
