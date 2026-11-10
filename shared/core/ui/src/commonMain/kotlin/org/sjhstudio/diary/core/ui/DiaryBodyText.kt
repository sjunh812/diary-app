package org.sjhstudio.diary.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.Font
import org.sjhstudio.diary.core.data.SettingsRepository
import org.sjhstudio.diary.core.designsystem.resources.Res
import org.sjhstudio.diary.core.designsystem.resources.changwon_round_regular
import org.sjhstudio.diary.core.designsystem.resources.maru_buri_regular
import org.sjhstudio.diary.core.designsystem.resources.nanum_cheer_regular
import org.sjhstudio.diary.core.designsystem.resources.nanum_pen_regular
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.pretendardFamily
import org.sjhstudio.diary.core.model.AppSettings
import org.sjhstudio.diary.core.model.BodyTextSize
import org.sjhstudio.diary.core.model.DiaryFont

/**
 * 앱에 넣은 본문 글꼴(3.5MB 이하, `docs/design.md` 일기 본문 글꼴 10종).
 * 나머지는 고를 때 받는다.
 */
val BUNDLED_DIARY_FONTS: Set<DiaryFont> =
    setOf(
        DiaryFont.PRETENDARD,
        DiaryFont.SYSTEM,
        DiaryFont.NANUM_CHEER,
        DiaryFont.NANUM_PEN,
        DiaryFont.CHANGWON_ROUND,
        DiaryFont.MARU_BURI
    )

/**
 * 사용자가 고른 글꼴.
 * 앱에 넣은 글꼴은 리소스에서 읽고, 받는 글꼴은 [installed]가 돌려주는 받은 파일로 만든다.
 * 받기 전이거나 읽지 못하면 Pretendard로 보인다(글꼴을 받는 동안에도 일기는 읽힌다).
 * 고른 글꼴 하나만 읽는다.
 * 앱 전체 글자에 쓴다(헌법 V, 2026-11-04 사용자 결정).
 */
@Composable
fun diaryBodyFontFamily(font: DiaryFont, installed: (DiaryFont) -> FontFamily? = { null }): FontFamily = key(font) {
    // iOS의 Font()는 화면 상태 자리를 쓴다.
    // 글꼴마다 자리를 나누지 않으면 글꼴을 바꿀 때 자리가 어긋나 앱이 멈춘다.
    fontFamilyOf(font, installed)
}

@Composable
private fun fontFamilyOf(font: DiaryFont, installed: (DiaryFont) -> FontFamily?): FontFamily = when (font) {
    DiaryFont.SYSTEM -> FontFamily.Default
    DiaryFont.PRETENDARD -> pretendardFamily()
    DiaryFont.NANUM_CHEER -> FontFamily(Font(Res.font.nanum_cheer_regular))
    DiaryFont.NANUM_PEN -> FontFamily(Font(Res.font.nanum_pen_regular))
    DiaryFont.CHANGWON_ROUND -> FontFamily(Font(Res.font.changwon_round_regular))
    DiaryFont.MARU_BURI -> FontFamily(Font(Res.font.maru_buri_regular))
    else -> installed(font) ?: pretendardFamily()
}

/**
 * 일기 본문 글자 스타일: 고른 글꼴과 본문 글자 크기 단계(research R17).
 * 크기는 sp라 기기 글자 크기 위에 곱해진다.
 * 글자 크기 단계는 일기 본문에만 걸려서 메뉴와 버튼은 이 스타일을 쓰지 않는다.
 */
@Composable
fun diaryBodyTextStyle(font: DiaryFont, size: BodyTextSize, installed: (DiaryFont) -> FontFamily? = { null }): TextStyle =
    DiaryTheme.typography.reading.copy(
        fontFamily = diaryBodyFontFamily(font, installed),
        fontSize = size.fontSizeSp.sp,
        lineHeight = size.lineHeightSp.sp
    )

/**
 * 목록 미리보기.
 * 고른 글꼴을 쓰고, 크기는 기본 단계에서 `body`가 되도록 같은 비율로 줄인다.
 * 본문보다 한 단계 작아야 목록이 빽빽하지 않다.
 */
@Composable
fun diaryPreviewTextStyle(font: DiaryFont, size: BodyTextSize, installed: (DiaryFont) -> FontFamily? = { null }): TextStyle {
    val body = DiaryTheme.typography.body
    val ratio = size.fontSizeSp.toFloat() / BodyTextSize.DEFAULT.fontSizeSp
    return body.copy(
        fontFamily = diaryBodyFontFamily(font, installed),
        fontSize = body.fontSize * ratio,
        lineHeight = body.lineHeight * ratio
    )
}

/**
 * 사용자가 고른 글꼴과 본문 크기.
 * 목록 미리보기, 상세, 쓰기가 같이 쓴다.
 */
data class BodyLook(val font: DiaryFont = DiaryFont.DEFAULT, val size: BodyTextSize = BodyTextSize.DEFAULT, val fontPath: String? = null)

/**
 * 설정에서 본문 모양을 꺼낸다.
 * [fontPath]는 글꼴 ID로 받아 둔 파일을 찾는다.
 */
fun AppSettings.bodyLook(fontPath: (String) -> String?): BodyLook = BodyLook(font, bodyTextSize, fontPath(font.id))

/**
 * 화면 모델이 쓰는 본문 모양.
 * 홈, 상세, 달력, 찾기가 같이 쓴다.
 * 글꼴을 바꾸면 바로 따라오도록 화면 모델이 살아 있는 동안 늘 본다.
 */
fun SettingsRepository.bodyLookIn(scope: CoroutineScope, fontPath: (String) -> String?): StateFlow<BodyLook> =
    settings.map { it.bodyLook(fontPath) }.stateInSafely(scope, SharingStarted.Eagerly, BodyLook())
