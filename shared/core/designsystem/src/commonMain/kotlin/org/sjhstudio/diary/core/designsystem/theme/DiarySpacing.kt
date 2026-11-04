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

    /** 터치 영역 최소 크기(M3 48, HIG 44). 보이는 크기가 작으면 누르는 영역을 이만큼 넓힌다. */
    val minTouchTarget: Dp = 48.dp

    /** 구분선 두께. */
    val hairline: Dp = 1.dp
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

/** 돌아보기 크기(Figma "돌아보기 (S5)"). */
object DiaryInsightSize {
    /** 기분 막대 줄. */
    val barRow: Dp = 36.dp
    val barHeight: Dp = 10.dp
    val barFace: Dp = 24.dp
    val barName: Dp = 36.dp
    val barCount: Dp = 56.dp

    /** 기분 흐름 얼굴. */
    val flowFace: Dp = 32.dp

    /** 연간 지도. */
    val mapGap: Dp = 2.dp
    val mapRow: Dp = 11.dp
    val mapRadius: Dp = 2.dp
    val mapLabel: Dp = 20.dp
}

/** 달력 크기(Figma "달력 (S4)"). */
object DiaryCalendarSize {
    /** 날짜 칸 높이. */
    val cell: Dp = 60.dp
    val cellWide: Dp = 76.dp

    /** 기분 얼굴. */
    val face: Dp = 30.dp
    val faceWide: Dp = 36.dp

    /** 오늘 표시. */
    val todayWidth: Dp = 24.dp
    val todayHeight: Dp = 20.dp

    /** 하루 여러 편 뱃지. */
    val badge: Dp = 18.dp
    val badgeBorder: Dp = 2.dp

    /** 넓은 화면의 달력 칸 폭. */
    val pane: Dp = 560.dp
}

/** 쓰기 화면 크기(Figma "쓰기 (S2)"). */
object DiaryWriteSize {
    /** 위 줄 사진 칸과 더하기 칸. */
    val photo: Dp = 72.dp

    /** 기분 칸(얼굴 44 + 이름). */
    val moodCellWidth: Dp = 60.dp
    val moodCellHeight: Dp = 84.dp
    val moodFace: Dp = 44.dp

    /** 날씨 칸. */
    val weatherCellHeight: Dp = 60.dp

    /** 서식 도구 줄. */
    val toolbar: Dp = 52.dp

    /** 글자색 점과 형광펜 칸. */
    val swatch: Dp = 28.dp

    /** 사진 지우기 표시. */
    val removeBadge: Dp = 20.dp

    /** 넓은 화면 옆 칸. */
    val sidePanel: Dp = 360.dp

    /** 넓은 화면 본문 폭. */
    val bodyMax: Dp = 680.dp

    /** 달력 고르기 칸. */
    val dayCell: Dp = 40.dp

    /** 본문 칸 최소 높이. */
    val editorMin: Dp = 240.dp

    /** 두 칸의 왼쪽 목록 폭(M3 고정 판). */
    val listPane: Dp = 360.dp

    /** 두 칸으로 나누는 폭. */
    val twoPaneMin: Dp = 840.dp
}
