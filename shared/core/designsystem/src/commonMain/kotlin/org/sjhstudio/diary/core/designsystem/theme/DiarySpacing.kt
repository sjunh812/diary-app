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

/** 홈 목록 모양 크기. */
object DiaryHomeSize {
    /** 카드의 기분 얼굴과 날씨 아이콘. */
    val cardFace: Dp = 28.dp
    val cardIcon: Dp = 16.dp

    /** 사진 격자 칸의 최소 폭. 폰에서 세 칸이 된다. */
    val gridCell: Dp = 112.dp
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

    /** 기분 얼굴. 칸 안 위아래 여백이 4로 같아지는 크기다. */
    val face: Dp = 28.dp
    val faceWide: Dp = 36.dp

    /** 오늘 표시. */
    val todayWidth: Dp = 24.dp
    val todayHeight: Dp = 20.dp

    /** 하루 여러 편일 때 뒤 얼굴이 옆으로 나오는 만큼과 앞 얼굴 둘레. */
    val stackOffset: Dp = 5.dp
    val stackOffsetWide: Dp = 6.dp
    val stackRing: Dp = 2.dp

    /** 넓은 화면의 달력 칸 폭. */
    val pane: Dp = 560.dp
}

/** 온보딩 그림 크기(Figma "시작 (W1~W5)"). */
object DiaryArtSize {
    /** 그림 칸. 글이 길어져도 제목 위치가 흔들리지 않게 높이를 고정한다. */
    val height: Dp = 208.dp
    val width: Dp = 288.dp

    /** 쓰는 그림의 기분 얼굴. */
    val face: Dp = 36.dp

    /** 백업 그림의 휴대폰과 구름. */
    val phoneWidth: Dp = 92.dp
    val phoneHeight: Dp = 156.dp
    val cloud: Dp = 96.dp
    val cloudIcon: Dp = 44.dp

    /** 휴대폰에서 구름으로 날아가는 조각. */
    val tile: Dp = 32.dp
    val tileIcon: Dp = 16.dp
    val arc: Dp = 28.dp

    /** 구름 위 확인 표시. */
    val check: Dp = 32.dp

    /** "이렇게 달라졌어요" 줄의 아이콘 원. */
    val rowBadge: Dp = 44.dp

    /** 휴대폰 안 글줄. */
    val line: Dp = 6.dp
}

/** 쓰기 화면 크기(Figma "쓰기 (S2)"). */
object DiaryWriteSize {
    /** 위 줄 사진 칸과 더하기 칸. */
    val photo: Dp = 72.dp

    /** 기분 칸(얼굴 44 + 이름). */
    val moodCellWidth: Dp = 60.dp
    val moodCellHeight: Dp = 84.dp
    val moodFace: Dp = 44.dp

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
