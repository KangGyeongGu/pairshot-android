package com.pairshot.core.ads.controller

import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FullscreenAdState
@Inject
constructor() {
    private val showing = AtomicBoolean(false)

    fun isShowing(): Boolean = showing.get()

    fun markShown(): Boolean = showing.compareAndSet(false, true)

    fun markDismissed() {
        showing.set(false)
    }
}
