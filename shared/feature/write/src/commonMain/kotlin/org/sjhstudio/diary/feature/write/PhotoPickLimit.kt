package org.sjhstudio.diary.feature.write

import androidx.compose.runtime.Composable

/**
 * 고르기 화면이 고를 장수를 막을 수 있는지
 * 못 막는 화면에 장수를 넘기면 FileKit이 넘친 사진을 말없이 잘라 저장소가 한도 안내를 못 띄움
 */
@Composable
internal expect fun rememberPickerLimitsCount(): Boolean

/** 고르기 화면에 넘길 최대 장수. 못 막는 화면이면 null로 다 받아 저장소가 넘침을 알림 */
internal fun pickMaxItems(limit: Int, attached: Int, pickerLimitsCount: Boolean): Int? =
    (limit - attached).coerceAtLeast(1).takeIf { pickerLimitsCount }
