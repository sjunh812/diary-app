package org.sjhstudio.diary.core.platform

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner

actual object AppForeground {
    actual fun observe(onBackground: () -> Unit, onForeground: () -> Unit): () -> Unit {
        val observer =
            object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) = onForeground()

                override fun onStop(owner: LifecycleOwner) = onBackground()
            }
        val lifecycle = ProcessLifecycleOwner.get().lifecycle
        lifecycle.addObserver(observer)
        return { lifecycle.removeObserver(observer) }
    }
}
