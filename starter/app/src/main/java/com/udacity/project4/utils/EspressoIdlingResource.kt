package com.udacity.project4.utils

import androidx.test.espresso.idling.CountingIdlingResource

/**
 * Code taken from udacity's com.example.android.architecture.blueprints.reactive app source code
 */
object EspressoIdlingResource {

    private const val RESOURCE = "GLOBAL"

    @JvmField
    val countingIdlingResource = CountingIdlingResource(RESOURCE)

    fun increment() {
        countingIdlingResource.increment()
    }

    fun decrement() {
        if (!countingIdlingResource.isIdleNow) {
            countingIdlingResource.decrement()
        }
    }
}

inline fun <T> wrapEspressoIdlingResource(block: () -> T): T {
    EspressoIdlingResource.increment()

    return try {
        block()

    } finally {
        EspressoIdlingResource.decrement()
    }
}