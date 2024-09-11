package com.crisiscleanup.feature.team.util

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.absoluteValue
import kotlin.random.Random

@Singleton
class NameGenerator @Inject constructor() {
    fun generateName() = listOf(
        nameAdjectives.randomItem(),
        nameAnimals.randomItem(),
        nameColors.randomItem(),
    )
        .joinToString(" ") {
            if (it.isBlank()) {
                ""
            } else {
                "${it.first().uppercase()}${it.substring(1..<it.length)}"
            }
        }
        .trim()
}

private fun List<String>.randomItem() = if (isEmpty()) {
    ""
} else {
    get(Random.nextInt().absoluteValue % size)
}
