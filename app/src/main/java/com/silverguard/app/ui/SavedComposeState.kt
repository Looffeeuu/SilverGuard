package com.silverguard.app.ui

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Compose state that is also written to [SavedStateHandle], so it comes back after Android kills the app
 * in the background (for example while the camera or browser is open on a low-memory phone).
 * Only small values that fit in a Bundle belong here; the analysis itself is reloaded from local history.
 */
internal class SavedComposeState<T>(
    private val handle: SavedStateHandle,
    private val key: String,
    initial: T
) : ReadWriteProperty<Any?, T> {

    @Suppress("UNCHECKED_CAST")
    private val state = mutableStateOf(if (handle.contains(key)) handle.get<Any?>(key) as T else initial)

    override fun getValue(thisRef: Any?, property: KProperty<*>): T = state.value

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        state.value = value
        handle[key] = value
    }
}
