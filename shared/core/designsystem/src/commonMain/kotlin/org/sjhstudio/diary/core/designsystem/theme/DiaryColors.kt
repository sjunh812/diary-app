package org.sjhstudio.diary.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * 색 토큰(`docs/design.md` 색 표, Figma 변수 "색 · 라이트"·"색 · 다크"와 같은 이름).
 * 화면 코드는 원시 색 대신 `DiaryTheme.colors`만 쓴다(헌법 V).
 */
@Immutable
data class DiaryColors(
    /** 화면 바탕, 탭 바 */
    val canvas: Color,
    /**
     * 카드형 목록의 카드.
     * 흰 바탕 위에 면으로 깔려서 입력칸 회색보다 한 단계 연하다.
     */
    val surfaceCard: Color,
    /** 시트, 대화상자 */
    val surface: Color,
    /**
     * 편지지와 봉투.
     * 라벤더 바탕에서 뜨지 않게 다크는 보랏빛 면이다
     */
    val paper: Color,
    /** 입력칸, 보조 버튼, 칩, 진행 막대 트랙 */
    val sunken: Color,
    /** 고른 칸 바탕 */
    val selected: Color,
    /** 구분선 */
    val line: Color,
    /** 시트 손잡이 */
    val lineStrong: Color,
    /**
     * 꾸밈(그림, 장식).
     * 글자·버튼에는 쓰지 않는다
     */
    val lilac: Color,
    /** 주요 버튼, 쓰기 버튼, 즐겨찾기 별 */
    val accent: Color,
    /** 링크, 고른 글자, 체크 */
    val accentText: Color,
    /** accent 위 글자 */
    val onAccent: Color,
    /** 본문, 제목 */
    val textPrimary: Color,
    /** 시각, 장소, 요일, 설명, 안내 글 */
    val textSecondary: Color,
    /** 아이콘과 짝인 덧붙임, 비활성 탭만(라이트 3.4:1) */
    val textTertiary: Color,
    /**
     * 입력 안내.
     * 꼭 필요한 정보를 싣지 않는다
     */
    val placeholder: Color,
    /** 지우기, 실패 */
    val danger: Color,
    /** danger 위 글자 */
    val onDanger: Color,
    /** 토스트 바탕 */
    val toast: Color,
    /** 토스트 글자 */
    val onToast: Color,
    /** 토스트 버튼 글자 */
    val toastAction: Color,
    /** 시트·대화상자 뒤 */
    val scrim: Color,
    /** 사진 위 +N 뱃지 */
    val overlay: Color,
    /**
     * 사진 크게 보기 바탕.
     * 두 테마 모두 검다
     */
    val photoBackdrop: Color = Color(0xFF000000),
    /**
     * 기분 9색.
     * 면과 점으로만 쓰고 글자색으로 쓰지 않는다.
     */
    val mood: DiaryMoodColors,
    /** 일기 본문 글자색(스펙 002 contracts/rich-text-html.md). */
    val ink: DiaryInkColors,
    /** 일기 본문 형광펜. */
    val mark: DiaryMarkColors
)

@Immutable
data class DiaryMoodColors(
    val angry: Color,
    val cool: Color,
    val sad: Color,
    val sick: Color,
    val laugh: Color,
    val meh: Color,
    val bad: Color,
    val good: Color,
    val tired: Color
)

@Immutable
data class DiaryInkColors(
    val red: Color,
    val orange: Color,
    val green: Color,
    val blue: Color,
    val gray: Color,
    val purple: Color,
    val pink: Color
)

@Immutable
data class DiaryMarkColors(val yellow: Color, val green: Color, val blue: Color, val purple: Color, val pink: Color)

internal val LightDiaryColors =
    DiaryColors(
        canvas = Color(0xFFFFFFFF),
        surfaceCard = Color(0xFFF8F8FF),
        surface = Color(0xFFFFFFFF),
        paper = Color(0xFFFFFFFF),
        sunken = Color(0xFFF3F4F5),
        selected = Color(0xFFEBE4FF),
        line = Color(0xFFEEEFF1),
        lineStrong = Color(0xFFDCDEE3),
        lilac = Color(0xFFCAA6FE),
        accent = Color(0xFF6E56CF),
        accentText = Color(0xFF6550B9),
        onAccent = Color(0xFFFFFFFF),
        textPrimary = Color(0xFF1A1C20),
        textSecondary = Color(0xFF555D6D),
        textTertiary = Color(0xFF868B94),
        placeholder = Color(0xFFB0B3BA),
        danger = Color(0xFFCE2C31),
        onDanger = Color(0xFFFFFFFF),
        toast = Color(0xFF2A3038),
        onToast = Color(0xFFFFFFFF),
        toastAction = Color(0xFFCAA6FE),
        scrim = Color(0x66000000),
        overlay = Color(0x80000000),
        mood =
            DiaryMoodColors(
                angry = Color(0xFFFFAFB0),
                cool = Color(0xFFAEE4FF),
                sad = Color(0xFFC4F4FE),
                sick = Color(0xFFBEE9B4),
                laugh = Color(0xFFFDFA87),
                meh = Color(0xFFDFD4E4),
                bad = Color(0xFF808080),
                good = Color(0xFFFFD4A8),
                tired = Color(0xFFFCC6F7)
            ),
        ink =
            DiaryInkColors(
                red = Color(0xFFB91C1C),
                orange = Color(0xFFC2410C),
                green = Color(0xFF15803D),
                blue = Color(0xFF1D4ED8),
                gray = Color(0xFF6B7280),
                purple = Color(0xFF6D28D9),
                pink = Color(0xFFBE185D)
            ),
        mark =
            DiaryMarkColors(
                yellow = Color(0xFFFEF08A),
                green = Color(0xFFBBF7D0),
                blue = Color(0xFFBFDBFE),
                purple = Color(0xFFDDD6FE),
                pink = Color(0xFFFBCFE8)
            )
    )

internal val DarkDiaryColors =
    DiaryColors(
        canvas = Color(0xFF16171B),
        surfaceCard = Color(0xFF1D2025),
        surface = Color(0xFF1D2025),
        paper = Color(0xFF2B2640),
        sunken = Color(0xFF2B2E35),
        selected = Color(0xFF291F43),
        line = Color(0xFF2B2E35),
        lineStrong = Color(0xFF393D46),
        lilac = Color(0xFFCAA6FE),
        accent = Color(0xFF6E56CF),
        accentText = Color(0xFFBAA7FF),
        onAccent = Color(0xFFFFFFFF),
        textPrimary = Color(0xFFF3F4F5),
        textSecondary = Color(0xFFDCDEE3),
        textTertiary = Color(0xFFB0B3BA),
        placeholder = Color(0xFF868B94),
        danger = Color(0xFFFF9592),
        onDanger = Color(0xFF16171B),
        toast = Color(0xFF393D46),
        onToast = Color(0xFFF3F4F5),
        toastAction = Color(0xFFBAA7FF),
        scrim = Color(0x99000000),
        overlay = Color(0x80000000),
        mood =
            DiaryMoodColors(
                angry = Color(0xFFFFAFB0),
                cool = Color(0xFFAEE4FF),
                sad = Color(0xFFC4F4FE),
                sick = Color(0xFFBEE9B4),
                laugh = Color(0xFFFDFA87),
                meh = Color(0xFFDFD4E4),
                bad = Color(0xFF808080),
                good = Color(0xFFFFD4A8),
                tired = Color(0xFFFCC6F7)
            ),
        ink =
            DiaryInkColors(
                red = Color(0xFFF87171),
                orange = Color(0xFFFB923C),
                green = Color(0xFF4ADE80),
                blue = Color(0xFF60A5FA),
                gray = Color(0xFF9CA3AF),
                purple = Color(0xFFA78BFA),
                pink = Color(0xFFF472B6)
            ),
        mark =
            DiaryMarkColors(
                yellow = Color(0xFF713F12),
                green = Color(0xFF14532D),
                blue = Color(0xFF1E3A8A),
                purple = Color(0xFF4C1D95),
                pink = Color(0xFF831843)
            )
    )
