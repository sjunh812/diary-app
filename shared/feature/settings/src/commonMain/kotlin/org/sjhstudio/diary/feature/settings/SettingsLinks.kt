package org.sjhstudio.diary.feature.settings

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.feature.settings.resources.Res
import org.sjhstudio.diary.feature.settings.resources.settings_feedback_body
import org.sjhstudio.diary.feature.settings.resources.settings_feedback_no_mail
import org.sjhstudio.diary.feature.settings.resources.settings_feedback_subject

private const val SUPPORT_EMAIL = "sjunh812@naver.com"
private const val PLAY_LISTING = "https://play.google.com/store/apps/details?id=org.sjhstudio.diary"
private const val PLATFORM_ANDROID = "android"

/** 의견 메일 주소와, 메일 앱이 없을 때 보일 안내. */
data class FeedbackLink(val url: String, val noMail: String)

/**
 * 스토어의 앱 페이지.
 * 인앱 평가 창은 버튼으로 부르면 안 되고 뜨지 않을 수도 있어서, 버튼은 스토어로 보낸다(`docs/DECISIONS.md` 2026-11-07).
 * App Store 번호는 출시 뒤에 정해져서 그때까지 iOS에는 줄을 두지 않는다.
 */
fun storeReviewUrl(platform: String): String? = PLAY_LISTING.takeIf { platform == PLATFORM_ANDROID }

/**
 * 의견 메일.
 * 앱 버전과 기기 종류만 넣고 일기 내용은 넣지 않는다(헌법 II).
 */
@Composable
fun rememberFeedbackLink(version: String, platform: String): FeedbackLink {
    val subject = stringResource(Res.string.settings_feedback_subject)
    val body = stringResource(Res.string.settings_feedback_body, version, platform)
    return FeedbackLink(
        "mailto:$SUPPORT_EMAIL?subject=${subject.percentEncoded()}&body=${body.percentEncoded()}",
        stringResource(Res.string.settings_feedback_no_mail, SUPPORT_EMAIL)
    )
}

/** 메일 주소 뒤의 값은 UTF-8로 낱낱이 바꿔야 어느 메일 앱에서나 깨지지 않는다. */
internal fun String.percentEncoded(): String = encodeToByteArray().joinToString("") { byte ->
    val value = byte.toInt() and BYTE_MASK
    val char = value.toChar()
    if (value < ASCII_END && char.isLetterOrDigit()) char.toString() else "%" + value.toString(HEX).uppercase().padStart(2, '0')
}

private const val BYTE_MASK = 0xFF
private const val ASCII_END = 0x80
private const val HEX = 16
