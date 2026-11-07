package org.sjhstudio.diary.core.backup

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.runtime.GC
import kotlin.native.runtime.NativeRuntimeApi

@OptIn(NativeRuntimeApi::class, ExperimentalNativeApi::class, ExperimentalStdlibApi::class)
internal actual fun retainedBytes(): Long {
    GC.collect()
    return GC.lastGCInfo?.memoryUsageAfter?.values?.sumOf { it.totalObjectsSizeBytes } ?: 0
}
