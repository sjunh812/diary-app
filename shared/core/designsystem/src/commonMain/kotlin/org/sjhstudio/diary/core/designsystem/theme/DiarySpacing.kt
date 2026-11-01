package org.sjhstudio.diary.core.designsystem.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 간격 단계(`docs/design.md` 간격, SEED dimension 이름).
 * 화면 배치에는 이 단계만 쓴다.
 * 10·14·28 같은 컴포넌트 안 값은 그 컴포넌트 코드 안에만 둔다.
 */
object DiarySpacing {
    /** 2. 아이콘과 글자 사이 같은 좁은 틈에만. */
    val x0_5: Dp = 2.dp
    val x1: Dp = 4.dp

    /** 6. 아이콘과 글자 사이 같은 좁은 틈에만. */
    val x1_5: Dp = 6.dp
    val x2: Dp = 8.dp
    val x3: Dp = 12.dp
    val x4: Dp = 16.dp
    val x5: Dp = 20.dp
    val x6: Dp = 24.dp
    val x8: Dp = 32.dp
    val x10: Dp = 40.dp
    val x14: Dp = 56.dp

    /** 폰 화면 좌우(TDS ListRow 20, HIG 예시 20). */
    val screenCompact: Dp = x5

    /** 600dp부터 화면 좌우(M3 medium·expanded 24). */
    val screenWide: Dp = x6

    /** 누를 곳 최소 크기(M3 48, HIG 44). 보이는 크기가 작으면 누르는 영역을 이만큼 넓힌다. */
    val minTouchTarget: Dp = 48.dp
}

/** 아이콘 크기 네 단계(`docs/design.md` 아이콘과 그림, 2026-11-01 점검). */
object DiaryIconSize {
    /** 칩, 메타 덧붙임, 안내 상자, 상태 상자 머리, 한 줄 안내, 검색 지우기. */
    val i16: Dp = 16.dp

    /** 줄 끝 화살표, 돋보기, 작은 버튼, 큰 제목 옆 펼침. */
    val i20: Dp = 20.dp

    /** 상단 바, 탭, 줄 앞, 체크, 숫자판 지우기, 날씨 칸. */
    val i24: Dp = 24.dp

    /** 결과 배지, 빈 상태, 생체 인증. */
    val i32: Dp = 32.dp
}
