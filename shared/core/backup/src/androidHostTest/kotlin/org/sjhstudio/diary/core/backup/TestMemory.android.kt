package org.sjhstudio.diary.core.backup

internal actual fun retainedBytes(): Long {
    val runtime = Runtime.getRuntime()
    System.gc()
    return runtime.totalMemory() - runtime.freeMemory()
}
