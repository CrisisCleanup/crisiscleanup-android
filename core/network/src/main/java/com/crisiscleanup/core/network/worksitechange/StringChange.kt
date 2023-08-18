package com.crisiscleanup.core.network.worksitechange

/**
 * Determines if the current value is different from [from]
 *
 * @return NULL if there is no change in value or [this] otherwise.
 * NULL indicates a reference value should be used and non-NULL indicates the returned value should be used.
 */
private fun String.diffFrom(from: String?) =
    if (this.trim() == (from?.trim() ?: "")) {
        null
    } else {
        this
    }

internal fun String.change(from: String, to: String) = to.diffFrom(from) ?: this

internal fun baseChange(base: String?, from: String?, to: String?): String? {
    return if (base == null) {
        to?.diffFrom(from)
    } else {
        val nFrom = from?.trim() ?: ""
        val nTo = to?.trim() ?: ""
        if (nFrom == nTo) {
            return base
        } else {
            to
        }
    }
}
