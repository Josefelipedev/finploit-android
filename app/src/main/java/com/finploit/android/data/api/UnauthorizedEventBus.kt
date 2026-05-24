package com.finploit.android.data.api

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Global event bus that signals a 401 Unauthorized response was received.
 * UnauthorizedInterceptor emits here; LoginViewModel/AppNavGraph react and redirect to Login.
 */
@Singleton
class UnauthorizedEventBus @Inject constructor() {
    // extraBufferCapacity = 0: events that arrive while the collector is busy are dropped.
    // This prevents stale 401 events (buffered from a previous expired session) from
    // firing again after the user successfully re-logs in, which would cause a logout loop.
    private val _events = MutableSharedFlow<Unit>(extraBufferCapacity = 0)
    val events: SharedFlow<Unit> = _events.asSharedFlow()

    /** Called from the OkHttp interceptor thread — tryEmit is thread-safe. */
    fun send() {
        _events.tryEmit(Unit)
    }
}
