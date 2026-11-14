package org.sjhstudio.diary.app.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.elementDescriptors
import org.sjhstudio.diary.core.platform.AnalyticsEvents

// 화면 키 이름은 사용 통계와 비정상 종료 보고로 나감. 일기 번호 같은 값이 섞이면 안 됨(헌법 II)
class ScreenKeysPrivacyTest {
    private val secret = "비밀-일기-번호"

    private val keysWithValues =
        listOf(
            SearchKey(listOf(secret), secret, secret),
            EntryDetailKey(secret),
            PhotoViewerKey(secret, 3),
            WriteKey(secret, secret),
            FontLicenseKey(secret),
            LibraryLicenseKey(secret),
            RestorePreviewKey(secret)
        )

    /** `Screen` 아래 모든 키의 이름. 키를 추가하면 저절로 여기 들어옴 */
    @OptIn(ExperimentalSerializationApi::class)
    private fun screenNames(): Set<String> =
        Screen.serializer().descriptor.elementDescriptors.last().elementDescriptors.map { it.serialName.substringAfterLast('.') }.toSet()

    @Test
    fun a_key_never_shows_what_it_carries() {
        for (key in keysWithValues) {
            assertFalse(secret in key.toString(), key.toString())
            assertEquals(key::class.simpleName, key.toString())
        }
    }

    /** 표에 없는 화면 이름은 출시 빌드에서 버려지고 개발 빌드에서는 앱이 꺼짐 */
    @Test
    fun every_screen_is_on_the_analytics_table() {
        assertEquals(AnalyticsEvents.SCREENS, screenNames())
    }
}
