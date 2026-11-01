package org.sjhstudio.diary.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import org.sjhstudio.diary.core.designsystem.resources.Res
import org.sjhstudio.diary.core.designsystem.resources.pretendard_bold
import org.sjhstudio.diary.core.designsystem.resources.pretendard_medium
import org.sjhstudio.diary.core.designsystem.resources.pretendard_regular
import org.sjhstudio.diary.core.designsystem.resources.pretendard_semibold

/**
 * 글자 토큰 13단계(`docs/design.md` 글자 표, Figma 글자 스타일과 같은 이름).
 * 자간은 모두 0이고 줄 높이도 sp라서 기기 글자 크기를 따라 함께 커진다.
 * 일기 본문은 사용자가 고른 글꼴과 크기를 따른다(스펙 001 T068).
 */
@Immutable
data class DiaryTypography(
    /** 28/38 Bold. 홈·탭 첫 화면 큰 제목. */
    val display: TextStyle,
    /** 22/30 Bold. 화면 제목, 상세 날짜. */
    val title1: TextStyle,
    /** 20/28 Bold. 시트·대화상자 제목. */
    val title2: TextStyle,
    /** 22/28 Medium. 목록 날짜 숫자, 통계 숫자. */
    val numeral: TextStyle,
    /** 28/34 Regular. PIN 숫자판 숫자에만. */
    val keypad: TextStyle,
    /** 17/24 SemiBold. 큰 버튼, 상단 바 제목. */
    val headline: TextStyle,
    /** 17/28 Regular. 일기 본문 기본값, 설정 줄 이름. */
    val reading: TextStyle,
    /** 15/22 Regular. 목록 미리보기, 설명. */
    val body: TextStyle,
    /** 15/22 SemiBold. 중간 버튼, 토스트. */
    val bodyStrong: TextStyle,
    /** 14/20 Medium. 칩, 입력 라벨, 설정 묶음 머리. */
    val label: TextStyle,
    /** 13/18 Regular. 장소, 보조 글. */
    val caption: TextStyle,
    /** 13/18 Medium. 시각, 요일, 작은 버튼. */
    val captionStrong: TextStyle,
    /** 12/16 Medium. 탭 이름, 날씨 이름, 사진 수. */
    val micro: TextStyle
)

/** UI 서체 Pretendard 네 굵기(SIL OFL 1.1). */
@Composable
fun pretendardFamily(): FontFamily = FontFamily(
    Font(Res.font.pretendard_regular, FontWeight.Normal),
    Font(Res.font.pretendard_medium, FontWeight.Medium),
    Font(Res.font.pretendard_semibold, FontWeight.SemiBold),
    Font(Res.font.pretendard_bold, FontWeight.Bold)
)

@Composable
internal fun diaryTypography(): DiaryTypography {
    val family = pretendardFamily()
    fun style(size: TextUnit, lineHeight: TextUnit, weight: FontWeight) = TextStyle(
        fontFamily = family,
        fontSize = size,
        lineHeight = lineHeight,
        fontWeight = weight,
        letterSpacing = 0.sp,
        // Figma처럼 줄 높이 안에서 글자를 가운데 둔다.
        lineHeightStyle = LineHeightStyle(alignment = LineHeightStyle.Alignment.Center, trim = LineHeightStyle.Trim.None)
    )
    return DiaryTypography(
        display = style(28.sp, 38.sp, FontWeight.Bold),
        title1 = style(22.sp, 30.sp, FontWeight.Bold),
        title2 = style(20.sp, 28.sp, FontWeight.Bold),
        numeral = style(22.sp, 28.sp, FontWeight.Medium),
        keypad = style(28.sp, 34.sp, FontWeight.Normal),
        headline = style(17.sp, 24.sp, FontWeight.SemiBold),
        reading = style(17.sp, 28.sp, FontWeight.Normal),
        body = style(15.sp, 22.sp, FontWeight.Normal),
        bodyStrong = style(15.sp, 22.sp, FontWeight.SemiBold),
        label = style(14.sp, 20.sp, FontWeight.Medium),
        caption = style(13.sp, 18.sp, FontWeight.Normal),
        captionStrong = style(13.sp, 18.sp, FontWeight.Medium),
        micro = style(12.sp, 16.sp, FontWeight.Medium)
    )
}
