package com.chesko.sendfiles.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface Route : NavKey {
    @Serializable
    data object Home : Route
    @Serializable
    data object Files : Route
    @Serializable
    data object Transfers : Route
    @Serializable
    data object Devices : Route
    @Serializable
    data object History : Route
}
