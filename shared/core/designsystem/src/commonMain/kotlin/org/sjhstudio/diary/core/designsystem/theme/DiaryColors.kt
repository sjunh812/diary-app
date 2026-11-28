package org.sjhstudio.diary.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * 색 토큰(`docs/design.md` 색 표, Figma 변수 "색 · 라이트"·"색 · 다크"와 같은 이름)
 * 화면 코드는 원시 색 대신 `DiaryTheme.colors`만 씀(헌법 V)
 */
@Immutable
data class DiaryColors(
    /** 화면 바탕, 탭 바 */
    val canvas: Color,
    /** 카드형 목록과 고급 통계의 카드. 흰 바탕 위에 옅은 보라 면으로 깔림 */
    val surfaceCard: Color,
    /** 시트, 대화상자 */
    val surface: Color,
    /** 편지지와 봉투. 라벤더 바탕에서 뜨지 않게 다크는 보랏빛 면 */
    val paper: Color,
    /** 봉투 덮개. 면([paper])보다 보랏빛이 짙음 */
    val envelopeFlap: Color,
    /** 입력칸, 보조 버튼, 칩, 진행 막대 트랙 */
    val sunken: Color,
    /** [sunken] 위에 떠오른 고른 칸(탭 한 줄). 다크도 바탕보다 밝게 */
    val raised: Color,
    /** 고른 칸 바탕 */
    val selected: Color,
    /** 구분선 */
    val line: Color,
    /** 시트 손잡이 */
    val lineStrong: Color,
    /** 꾸밈(그림, 장식). 글자·버튼에는 쓰지 않음 */
    val lilac: Color,
    /** 주요 버튼, 쓰기 버튼 같은 면 */
    val accent: Color,
    /** 바탕 위 브랜드색 아이콘과 선(즐겨찾기 별, 고름 고리·테두리, 켜진 스위치, 입력 커서). 바탕과 3:1 */
    val accentIcon: Color,
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
    /** 입력 안내. 꼭 필요한 정보를 싣지 않음 */
    val placeholder: Color,
    /**
     * 고급 통계의 긍정적인 기분 막대와 범례 점. 브랜드 보라(`accentText`)와 같은 값
     * 기분 색이 아니라 이 앱의 강조색이라 긍정 쪽 한 곳에만 씀
     */
    val toneGood: Color,
    /**
     * 고급 통계의 부정적인 기분 막대와 범례 점. 푸른 회색
     * 옅은 회색은 "세지 않는 기분"(`lineStrong`)과 비슷해 부정적인 기분이 무시되는 듯 보임
     * 차가운 쪽이 부정인 Apple 마음 상태 색과도 맞음
     */
    val toneHard: Color,
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

    /** 사진 위 글자와 지우기 버튼의 바탕. 흰 사진에서도 글자가 4.5:1을 넘게 [overlay]보다 진함 */
    val onPhoto: Color,
    /** 쓰기 버튼 그림자 */
    val shadow: Color,
    /** 사진 크게 보기 바탕. 두 테마 모두 검정 */
    val photoBackdrop: Color = Color(0xFF000000),
    /** [onPhoto]·[photoBackdrop] 위 글자와 아이콘. 테마 색과 상관없이 흰색 */
    val onPhotoContent: Color = Color.White,
    /** 기분 9색. 면과 점으로만 쓰고 글자색으로 쓰지 않음 */
    val mood: DiaryMoodColors,
    /** 일기 본문 글자색(스펙 002 contracts/rich-text-html.md) */
    val ink: DiaryInkColors,
    /** 일기 본문 형광펜 */
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
    val tired: Color,
    val excited: Color,
    val proud: Color,
    val calm: Color,
    val bored: Color,
    val anxious: Color,
    val annoyed: Color
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
        envelopeFlap = Color(0xFFEBE4FF),
        sunken = Color(0xFFF3F4F5),
        raised = Color(0xFFFFFFFF),
        selected = Color(0xFFEBE4FF),
        line = Color(0xFFEEEFF1),
        lineStrong = Color(0xFFDCDEE3),
        lilac = Color(0xFFCAA6FE),
        accent = Color(0xFF6E56CF),
        accentIcon = Color(0xFF6E56CF),
        accentText = Color(0xFF6550B9),
        toneGood = Color(0xFF6550B9),
        toneHard = Color(0xFF8F9BB3),
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
        onPhoto = Color(0x99000000),
        shadow = Color(0x29000000),
        mood =
            DiaryMoodColors(
                angry = Color(0xFFDC776D),
                cool = Color(0xFF90DCDB),
                sad = Color(0xFF7EA7DD),
                sick = Color(0xFFC8E5A5),
                laugh = Color(0xFFFCE694),
                meh = Color(0xFFD6CEBF),
                bad = Color(0xFF717A8D),
                good = Color(0xFFFBD4B6),
                tired = Color(0xFFC4E6F4),
                excited = Color(0xFFF9B6CF),
                proud = Color(0xFF8ED5AF),
                calm = Color(0xFFCDC0F0),
                bored = Color(0xFFBBC0C5),
                anxious = Color(0xFFCEA0D4),
                annoyed = Color(0xFFF7AA83)
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
        paper = Color(0xFF433C5C),
        envelopeFlap = Color(0xFF3A2F63),
        sunken = Color(0xFF2B2E35),
        raised = Color(0xFF393D46),
        selected = Color(0xFF291F43),
        line = Color(0xFF2B2E35),
        lineStrong = Color(0xFF393D46),
        lilac = Color(0xFFCAA6FE),
        accent = Color(0xFF6E56CF),
        accentIcon = Color(0xFF6E56CF),
        accentText = Color(0xFFBAA7FF),
        toneGood = Color(0xFFBAA7FF),
        toneHard = Color(0xFF7D879C),
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
        onPhoto = Color(0x99000000),
        shadow = Color(0x66000000),
        mood =
            DiaryMoodColors(
                angry = Color(0xFFDC776D),
                cool = Color(0xFF90DCDB),
                sad = Color(0xFF7EA7DD),
                sick = Color(0xFFC8E5A5),
                laugh = Color(0xFFFCE694),
                meh = Color(0xFFD6CEBF),
                bad = Color(0xFF717A8D),
                good = Color(0xFFFBD4B6),
                tired = Color(0xFFC4E6F4),
                excited = Color(0xFFF9B6CF),
                proud = Color(0xFF8ED5AF),
                calm = Color(0xFFCDC0F0),
                bored = Color(0xFFBBC0C5),
                anxious = Color(0xFFCEA0D4),
                annoyed = Color(0xFFF7AA83)
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
        // 형광펜 위 색 글자도 4.5:1 넘게 어둡게
        mark =
            DiaryMarkColors(
                yellow = Color(0xFF4D2B0C),
                green = Color(0xFF0E3C20),
                blue = Color(0xFF182E6E),
                purple = Color(0xFF411980),
                pink = Color(0xFF641233)
            )
    )

/** 브랜드색 자리만 [brand] 값으로 바꿈. `toneGood`은 브랜드 글자와 같은 값 */
internal fun DiaryColors.withBrand(brand: BrandTokens): DiaryColors = copy(
    accent = brand.accent,
    accentIcon = brand.accentIcon,
    onAccent = brand.onAccent,
    accentText = brand.accentText,
    toneGood = brand.accentText,
    selected = brand.selected,
    surfaceCard = brand.surfaceCard,
    paper = brand.paper,
    envelopeFlap = brand.envelopeFlap,
    lilac = brand.lilac,
    toastAction = brand.toastAction
)

/** 화면 모드와 테마 색으로 정한 색 토큰. 테마 화면의 작은 홈 그림처럼 지금 모드와 다른 색이 필요할 때 씀 */
fun diaryColors(dark: Boolean, brand: DiaryBrand = DiaryBrand.Lavender): DiaryColors =
    (if (dark) DarkDiaryColors else LightDiaryColors).withBrand(brand.tokens(dark))

/**
 * 클레이 얼굴 피부톤 여섯 단계(Figma "9-옵션" 피부톤, 밝은 쪽부터)
 * 얼굴과 코 색을 함께 둠. 라이트·다크 같은 값
 */
object DiaryClaySkin {
    val faces: List<Color> =
        listOf(Color(0xFFFBE5D6), Color(0xFFF5CFB3), Color(0xFFE8B690), Color(0xFFD09A72), Color(0xFFA56C4B), Color(0xFF6C432E))
    val noses: List<Color> =
        listOf(Color(0xFFFCBCB7), Color(0xFFF9ADA0), Color(0xFFF09D8A), Color(0xFFE08B76), Color(0xFFC56D5D), Color(0xFF9F524A))
}
