package org.sjhstudio.diary.app.ads

import androidx.compose.runtime.Composable

/** iOS 광고는 R2에 붙인다. */
@Composable
internal actual fun NativeAdCard(key: String, shape: AdShape, frame: @Composable (ad: @Composable () -> Unit) -> Unit) = Unit
