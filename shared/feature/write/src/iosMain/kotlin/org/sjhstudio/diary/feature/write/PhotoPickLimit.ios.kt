package org.sjhstudio.diary.feature.write

import androidx.compose.runtime.Composable

/** PHPicker는 selectionLimit로 장수를 막음 */
@Composable
internal actual fun rememberPickerLimitsCount(): Boolean = true
