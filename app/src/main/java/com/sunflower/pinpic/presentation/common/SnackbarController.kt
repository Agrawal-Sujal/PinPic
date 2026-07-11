package com.sunflower.pinpic.presentation.common

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * Singleton for displaying global snackbars from anywhere in the app.
 * A single source of truth for snackbar messages.
 */
object SnackbarController {
    private val _messages = Channel<String>()
    val messages = _messages.receiveAsFlow()

    suspend fun sendEvent(message: String) {
        _messages.send(message)
    }
}
