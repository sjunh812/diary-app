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
import org.sjhstudio.diary.core.designsystem.resources.hakgyoansim_badasseugi_light
import org.sjhstudio.diary.core.designsystem.resources.hallym_myungjo_bold
import org.sjhstudio.diary.core.designsystem.resources.hallym_myungjo_regular
import org.sjhstudio.diary.core.designsystem.resources.kyobo_2020_regular
import org.sjhstudio.diary.core.designsystem.resources.nanum_cheer_regular
import org.sjhstudio.diary.core.designsystem.resources.rix_watermelon_regular
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.pretendardFamily
import org.sjhstudio.diary.core.model.AppSettings
import org.sjhstudio.diary.core.model.BodyTextSize
import org.sjhstudio.diary.core.model.DiaryFont

/**
 * 앱에 넣은 본문 글꼴(3.5MB 이하, `docs/design.md` 일기 본문 글꼴 10종)
 * 나머지는 고를 때 받음
 */
val BUNDLED_DIARY_FONTS: Set<DiaryFont> =
    setOf(
        DiaryFont.PRETENDARD,
        DiaryFont.SYSTEM,
        DiaryFont.NANUM_CHEER,
        DiaryFont.CHANGWON_ROUND,
        DiaryFont.HAKGYOANSIM_BADASSEUGI,
        DiaryFont.HALLYM_MYUNGJO,
        DiaryFont.RIX_WATERMELON,
        DiaryFont.KYOBO_2020
    )

/**
 * 사용자가 고른 글꼴
 * 앱에 넣은 글꼴은 리소스에서 읽고, 받는 글꼴은 [installed]의 받은 파일로 만듦
 * 받기 전이거나 읽지 못하면 Pretendard로 보임(글꼴을 받는 동안에도 일기는 읽힘)
 * 고른 글꼴 하나만 읽음
 * 앱 전체 글자에 씀(헌법 V)
 * [installed]는 기본값이 없음. 빠뜨리면 받은 글꼴이 Pretendard로 보임
 */
@Composable
fun diaryBodyFontFamily(font: DiaryFont, installed: (DiaryFont) -> FontFamily?): FontFamily = key(font) {
    // iOS의 Font()는 화면 상태 자리를 씀
    // 글꼴마다 자리를 나누지 않으면 글꼴을 바꿀 때 자리가 어긋나 앱이 멈춤
    fontFamilyOf(font, installed)
}

@Composable
private fun fontFamilyOf(font: DiaryFont, installed: (DiaryFont) -> FontFamily?): FontFamily = when (font) {
    DiaryFont.SYSTEM -> FontFamily.Default

    DiaryFont.PRETENDARD -> pretendardFamily()

    DiaryFont.NANUM_CHEER -> FontFamily(Font(Res.font.nanum_cheer_regular))

    DiaryFont.CHANGWON_ROUND -> FontFamily(Font(Res.font.changwon_round_regular))

    DiaryFont.HAKGYOANSIM_BADASSEUGI -> FontFamily(Font(Res.font.hakgyoansim_badasseugi_light))

    DiaryFont.HALLYM_MYUNGJO ->
        FontFamily(Font(Res.font.hallym_myungjo_regular), Font(Res.font.hallym_myungjo_bold, FontWeight.Bold))

    DiaryFont.RIX_WATERMELON -> FontFamily(Font(Res.font.rix_watermelon_regular))

    DiaryFont.KYOBO_2020 -> FontFamily(Font(Res.font.kyobo_2020_regular))

    else -> installed(font) ?: pretendardFamily()
}

/**
 * 앱에 넣은 글꼴의 리소스 파일 이름(`composeResources/.../font/`)
 * 시작할 때 미리 읽는 쪽이 씀
 * [fontFamilyOf]의 리소스와 같은 파일이어야 미리 읽은 것이 쓰임
 */
val BUNDLED_FONT_FILES: Map<DiaryFont, String> =
    mapOf(
        DiaryFont.NANUM_CHEER to "nanum_cheer_regular.ttf",
        DiaryFont.CHANGWON_ROUND to "changwon_round_regular.otf",
        DiaryFont.HAKGYOANSIM_BADASSEUGI to "hakgyoansim_badasseugi_light.otf",
        DiaryFont.HALLYM_MYUNGJO to "hallym_myungjo_regular.otf",
        DiaryFont.RIX_WATERMELON to "rix_watermelon_regular.otf",
        DiaryFont.KYOBO_2020 to "kyobo_2020_regular.otf"
    )

/**
 * 일기 본문 글자 스타일: 고른 글꼴과 본문 글자 크기 단계(research R17)
 * 크기는 sp라 기기 글자 크기 위에 곱해짐
 * 글자 크기 단계는 일기 본문에만 걸려서 메뉴와 버튼은 이 스타일을 쓰지 않음
 */
@Composable
fun diaryBodyTextStyle(font: DiaryFont, size: BodyTextSize, installed: (DiaryFont) -> FontFamily?): TextStyle =
    DiaryTheme.typography.reading.copy(
        fontFamily = diaryBodyFontFamily(font, installed),
        fontSize = size.fontSizeSp.sp,
        lineHeight = size.lineHeightSp.sp
    )

/**
 * 목록 미리보기
 * 고른 글꼴을 쓰고, 크기는 기본 단계에서 `body`가 되게 같은 비율로 줄임
 * 본문보다 한 단계 작아야 목록이 빽빽하지 않음
 */
@Composable
fun diaryPreviewTextStyle(font: DiaryFont, size: BodyTextSize, installed: (DiaryFont) -> FontFamily?): TextStyle {
    val body = DiaryTheme.typography.body
    val ratio = size.fontSizeSp.toFloat() / BodyTextSize.DEFAULT.fontSizeSp
    return body.copy(
        fontFamily = diaryBodyFontFamily(font, installed),
        fontSize = body.fontSize * ratio,
        lineHeight = body.lineHeight * ratio
    )
}

/**
 * 사용자가 고른 글꼴과 본문 크기
 * 목록 미리보기, 상세, 쓰기가 같이 씀
 */
data class BodyLook(val font: DiaryFont = DiaryFont.DEFAULT, val size: BodyTextSize = BodyTextSize.DEFAULT, val fontPath: String? = null)

/**
 * 설정에서 꺼낸 본문 모양
 * [fontPath]는 글꼴 ID로 받아 둔 파일을 찾음
 */
fun AppSettings.bodyLook(fontPath: (String) -> String?): BodyLook = BodyLook(font, bodyTextSize, fontPath(font.id))

/**
 * 화면 모델이 쓰는 본문 모양
 * 홈, 상세, 달력, 찾기가 같이 씀
 * 글꼴을 바꾸면 바로 따라오게 화면 모델이 살아 있는 동안 늘 봄
 */
fun SettingsRepository.bodyLookIn(scope: CoroutineScope, fontPath: (String) -> String?): StateFlow<BodyLook> =
    settings.map { it.bodyLook(fontPath) }.stateInSafely(scope, SharingStarted.Eagerly, BodyLook())
