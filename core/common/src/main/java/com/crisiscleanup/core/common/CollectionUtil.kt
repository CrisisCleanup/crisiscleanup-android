package com.crisiscleanup.core.common

/**
 * @return (trueys, falseys)
 */
fun <T> Collection<T>.split(predicate: (t: T) -> Boolean): Pair<Collection<T>, Collection<T>> {
    val trueBucket = mutableListOf<T>()
    val falseBucket = mutableListOf<T>()
    forEach {
        if (predicate(it)) {
            trueBucket.add(it)
        } else {
            falseBucket.add(it)
        }
    }
    return Pair(trueBucket, falseBucket)
}