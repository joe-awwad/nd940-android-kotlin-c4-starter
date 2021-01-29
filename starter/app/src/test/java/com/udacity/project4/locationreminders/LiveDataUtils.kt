package com.udacity.project4.locationreminders

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

fun <T> LiveData<T>.getOrAwaitValue(
    timeoutSeconds: Long = 2L,
    latch: CountDownLatch = CountDownLatch(1)
): T {

    val observer = Observer<T> {
        latch.countDown()
    }

    this.observeForever(observer)

    try {
        if (!latch.await(timeoutSeconds, TimeUnit.SECONDS)) {
            throw TimeoutException("LiveData value await timed out after $timeoutSeconds seconds")
        }

    } finally {
        this.removeObserver(observer)
    }

    @Suppress("UNCHECKED_CAST")
    return value as T
}