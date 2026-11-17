package org.sjhstudio.diary.feature.write

import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/** 포토피커가 없는 기기는 문서 고르기 화면으로 넘어가 장수를 막지 못함 */
@Composable
internal actual fun rememberPickerLimitsCount(): Boolean {
    val context = LocalContext.current
    return remember(context) { PickVisualMedia.isPhotoPickerAvailable(context) }
}
