package com.chesko.sendfiles.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface Route : NavKey {
    @Serializable
    data object Home : Route
    @Serializable
    data object Files : Route
    @Serializable
    data class Transfers(val isReceiveMode: Boolean = false) : Route
    @Serializable
    data object Devices : Route
    @Serializable
    data object History : Route
    @Serializable
    data object Settings : Route
}
