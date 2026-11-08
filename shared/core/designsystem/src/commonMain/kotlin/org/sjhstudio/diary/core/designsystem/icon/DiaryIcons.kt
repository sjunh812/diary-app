package org.sjhstudio.diary.core.designsystem.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * 앱 틀 아이콘.
 * 경로는 Lucide(`lucide-static` 1.66.0, ISC 라이선스)의 SVG를 그대로 옮겼다.
 * 일기 화면 아이콘(찾기부터 날씨까지)은 Figma "화면" 페이지의 아이콘 경로를 그대로 옮겼다.
 * `circle`, `line`, `rect`는 같은 모양의 경로로 바꿨다.
 * 선 굵기는 1.75다(`docs/design.md` 아이콘과 그림).
 * 아이콘 세트를 확정하면 이 파일을 바꾼다.
 */
object DiaryIcons {
    private const val STAR = "M 12 4 L 14.4 9 L 19.8 9.6 L 15.8 13.3 L 16.9 18.6 L 12 16 L 7.1 18.6 L 8.2 13.3 L 4.2 9.6 L 9.6 9 L 12 4 Z"
    private const val GEAR =
        "M12.22 2h-.44a2 2 0 0 0-2 2v.18a2 2 0 0 1-1 1.73l-.43.25a2 2 0 0 1-2 0l-.15-.08a2 2 0 0 0-2.73.73l-.22.38a2 2 0 0 0 .73 2.73l.15.1a2 2 0 0 1 1 1.72v.51a2 2 0 0 1-1 1.74l-.15.09a2 2 0 0 0-.73 2.73l.22.38a2 2 0 0 0 2.73.73l.15-.08a2 2 0 0 1 2 0l.43.25a2 2 0 0 1 1 1.73V20a2 2 0 0 0 2 2h.44a2 2 0 0 0 2-2v-.18a2 2 0 0 1 1-1.73l.43-.25a2 2 0 0 1 2 0l.15.08a2 2 0 0 0 2.73-.73l.22-.39a2 2 0 0 0-.73-2.73l-.15-.08a2 2 0 0 1-1-1.74v-.5a2 2 0 0 1 1-1.74l.15-.09a2 2 0 0 0 .73-2.73l-.22-.38a2 2 0 0 0-2.73-.73l-.15.08a2 2 0 0 1-2 0l-.43-.25a2 2 0 0 1-1-1.73V4a2 2 0 0 0-2-2z"
    private const val CIRCLE = "M2 12a10 10 0 1 0 20 0a10 10 0 1 0 -20 0"

    val ChevronLeft: ImageVector by lazy { lucide("chevron-left", "m15 18-6-6 6-6") }
    val ChevronRight: ImageVector by lazy { lucide("chevron-right", "m9 18 6-6-6-6") }
    val Check: ImageVector by lazy { lucide("check", "M20 6 9 17l-5-5") }
    val Close: ImageVector by lazy { lucide("x", "M18 6 6 18", "m6 6 12 12") }
    val CircleCheck: ImageVector by lazy { lucide("circle-check", CIRCLE, "m16 9-5.5 5.5L8 12") }
    val CircleAlert: ImageVector by lazy { lucide("circle-alert", CIRCLE, "M12 8L12 12", "M12 16L12.01 16") }
    val Info: ImageVector by lazy { lucide("info", CIRCLE, "M12 16v-4", "M12 8h.01") }
    val Bell: ImageVector by lazy {
        lucide(
            "bell",
            "M10.268 21a2 2 0 0 0 3.464 0",
            "M3.262 15.326A1 1 0 0 0 4 17h16a1 1 0 0 0 .74-1.673C19.41 13.956 18 12.499 18 8A6 6 0 0 0 6 8c0 4.499-1.411 5.956-2.738 7.326"
        )
    }
    val BookOpen: ImageVector by lazy {
        lucide(
            "book-open",
            "M12 5v16",
            "M20.001 19A2 2 0 0022 17V5a2 2 0 00-1.999-2L16 3.002A5 5 0 0012 5a5 5 0 00-4-2H4a2 2 0 00-2 2v12a2 2 0 001.999 2H8a5 5 0 014 2 5 5 0 014-2z"
        )
    }
    val ImageOff: ImageVector by lazy {
        lucide(
            "image-off",
            "M2 2L22 22",
            "M10.41 10.41a2 2 0 1 1-2.83-2.83",
            "M13.5 13.5L6 21",
            "M18 12L21 15",
            "M3.59 3.59A1.99 1.99 0 0 0 3 5v14a2 2 0 0 0 2 2h14c.55 0 1.052-.22 1.41-.59",
            "M21 15V5a2 2 0 0 0-2-2H9"
        )
    }
    val CalendarCheck: ImageVector by lazy {
        lucide(
            "calendar-check",
            "M8 2v3",
            "M16 2v3",
            "M5 3h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z",
            "M3 9h18",
            "m9 15 2 2 4-4"
        )
    }
    val Download: ImageVector by lazy { lucide("download", "M12 15V3", "M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4", "m7 10 5 5 5-5") }
    val ShieldCheck: ImageVector by lazy {
        lucide(
            "shield-check",
            "M20 13c0 5-3.5 7.5-7.66 8.95a1 1 0 0 1-.67-.01C7.5 20.5 4 18 4 13V6a1 1 0 0 1 1-1c2 0 4.5-1.2 6.24-2.72a1.17 1.17 0 0 1 1.52 0C14.51 3.81 17 5 19 5a1 1 0 0 1 1 1z",
            "m9 12 2 2 4-4"
        )
    }
    val Copy: ImageVector by lazy {
        lucide(
            "copy",
            "M10 8h10a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H10a2 2 0 0 1-2-2V10a2 2 0 0 1 2-2z",
            "M4 16c-1.1 0-2-.9-2-2V4c0-1.1.9-2 2-2h10c1.1 0 2 .9 2 2"
        )
    }
    val Trash: ImageVector by lazy {
        lucide(
            "trash",
            "M10 11v6",
            "M14 11v6",
            "M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6",
            "M3 6h18",
            "M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"
        )
    }
    val History: ImageVector by lazy {
        lucide("history", "M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8", "M3 3v5h5", "M12 7v5l4 2")
    }
    val Mail: ImageVector by lazy {
        lucide(
            "mail",
            "m22 7-8.991 5.727a2 2 0 0 1-2.009 0L2 7",
            "M4 4h16a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z"
        )
    }

    val Lock: ImageVector by lazy {
        lucide("lock", "M5 11h14a2 2 0 0 1 2 2v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2z", "M7 11V7a5 5 0 0 1 10 0v4")
    }
    val Fingerprint: ImageVector by lazy {
        lucide(
            "fingerprint",
            "M12 10a2 2 0 0 0-2 2c0 1.02-.1 2.51-.26 4",
            "M14 13.12c0 2.38 0 6.38-1 8.88",
            "M17.29 21.02c.12-.6.43-2.3.5-3.02",
            "M2 12a10 10 0 0 1 18-6",
            "M2 16h.01",
            "M21.8 16c.2-2 .131-5.354 0-6",
            "M5 19.5C5.5 18 6 15 6 12a6 6 0 0 1 .34-2",
            "M8.65 22c.21-.66.45-1.32.57-2",
            "M9 6.8a6 6 0 0 1 9 5.2v2"
        )
    }
    val Delete: ImageVector by lazy {
        lucide(
            "delete",
            "M10 5a2 2 0 0 0-1.344.519l-6.328 5.74a1 1 0 0 0 0 1.481l6.328 5.741A2 2 0 0 0 10 19h10a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2z",
            "m12 9 6 6",
            "m18 9-6 6"
        )
    }

    // 하단 탭·레일 아이콘(Figma "설정 (S6)"의 tab-bar 경로 그대로).
    // 고른 탭은 선이 굵다(docs/design.md 하단 탭).
    val TabHome: ImageVector by lazy {
        tab(
            "tab-home",
            1.75f,
            "M 4 10.5 L 12 4 L 20 10.5 L 20 20 C 20 20.265 19.895 20.52 19.707 20.707 C 19.52 20.895 19.265 21 19 21 L 14.5 21 L 14.5 15 L 9.5 15 L 9.5 21 L 5 21 C 4.735 21 4.48 20.895 4.293 20.707 C 4.105 20.52 4 20.265 4 20 L 4 10.5 Z"
        )
    }
    val TabHomeSelected: ImageVector by lazy {
        tab(
            "tab-home-selected",
            2.25f,
            "M 4 10.5 L 12 4 L 20 10.5 L 20 20 C 20 20.265 19.895 20.52 19.707 20.707 C 19.52 20.895 19.265 21 19 21 L 14.5 21 L 14.5 15 L 9.5 15 L 9.5 21 L 5 21 C 4.735 21 4.48 20.895 4.293 20.707 C 4.105 20.52 4 20.265 4 20 L 4 10.5 Z"
        )
    }
    val TabCalendar: ImageVector by lazy {
        tab(
            "tab-calendar",
            1.75f,
            "M 6.5 5.5 L 17.5 5.5 C 18.881 5.5 20 6.619 20 8 L 20 18 C 20 19.381 18.881 20.5 17.5 20.5 L 6.5 20.5 C 5.119 20.5 4 19.381 4 18 L 4 8 C 4 6.619 5.119 5.5 6.5 5.5 Z",
            "M 4 10 L 20 10 M 8.5 3.5 L 8.5 7.5 M 15.5 3.5 L 15.5 7.5"
        )
    }
    val TabCalendarSelected: ImageVector by lazy {
        tab(
            "tab-calendar-selected",
            2.25f,
            "M 6.5 5.5 L 17.5 5.5 C 18.881 5.5 20 6.619 20 8 L 20 18 C 20 19.381 18.881 20.5 17.5 20.5 L 6.5 20.5 C 5.119 20.5 4 19.381 4 18 L 4 8 C 4 6.619 5.119 5.5 6.5 5.5 Z",
            "M 4 10 L 20 10 M 8.5 3.5 L 8.5 7.5 M 15.5 3.5 L 15.5 7.5"
        )
    }
    val TabInsight: ImageVector by lazy { tab("tab-insight", 1.75f, "M 5 20 L 5 11 M 12 20 L 12 5 M 19 20 L 19 14") }
    val TabInsightSelected: ImageVector by lazy { tab("tab-insight-selected", 2.25f, "M 5 20 L 5 11 M 12 20 L 12 5 M 19 20 L 19 14") }
    val TabSettings: ImageVector by lazy {
        tab(
            "tab-settings",
            1.75f,
            "M 15 12 C 15 13.657 13.657 15 12 15 C 10.343 15 9 13.657 9 12 C 9 10.343 10.343 9 12 9 C 13.657 9 15 10.343 15 12 Z",
            GEAR
        )
    }
    val TabSettingsSelected: ImageVector by lazy {
        tab(
            "tab-settings-selected",
            2.25f,
            "M 15 12 C 15 13.657 13.657 15 12 15 C 10.343 15 9 13.657 9 12 C 9 10.343 10.343 9 12 9 C 13.657 9 15 10.343 15 12 Z",
            GEAR
        )
    }

    val Search: ImageVector by lazy {
        lucide(
            "search",
            "M 17.5 11 C 17.5 14.59 14.59 17.5 11 17.5 C 7.41 17.5 4.5 14.59 4.5 11 C 4.5 7.41 7.41 4.5 11 4.5 C 14.59 4.5 17.5 7.41 17.5 11 Z",
            "M 16 16 L 20 20"
        )
    }
    val ChevronDown: ImageVector by lazy {
        lucide(
            "chevron-down",
            "M 5 9 L 12 16 L 19 9"
        )
    }
    val Cloud: ImageVector by lazy { lucide("cloud", "M17.5 19H9a7 7 0 1 1 6.71-9h1.79a4.5 4.5 0 1 1 0 9Z") }
    val CloudUpload: ImageVector by lazy {
        lucide("cloud-upload", "M12 13v8", "M4 14.899A7 7 0 1 1 15.71 8h1.79a4.5 4.5 0 0 1 2.5 8.242", "m8 17 4-4 4 4")
    }
    val CloudOff: ImageVector by lazy {
        lucide(
            "cloud-off",
            "M10.94 5.274A7 7 0 0 1 15.71 10h1.79a4.5 4.5 0 0 1 4.222 6.057",
            "M18.796 18.81A4.5 4.5 0 0 1 17.5 19H9A7 7 0 0 1 5.79 5.78",
            "m2 2 20 20"
        )
    }
    val LinkOff: ImageVector by lazy { lucide("link-2-off", "M9 17H7A5 5 0 0 1 7 7", "M15 7h2a5 5 0 0 1 4 8", "M8 12L12 12", "M2 2L22 22") }
    val Sparkles: ImageVector by lazy {
        lucide(
            "sparkles",
            "M9.937 15.5A2 2 0 0 0 8.5 14.063l-6.135-1.582a.5.5 0 0 1 0-.962L8.5 9.936A2 2 0 0 0 9.937 8.5l1.582-6.135a.5.5 0 0 1 .963 0L14.063 8.5A2 2 0 0 0 15.5 9.937l6.135 1.581a.5.5 0 0 1 0 .964L15.5 14.063a2 2 0 0 0-1.437 1.437l-1.582 6.135a.5.5 0 0 1-.963 0z",
            "M20 3v4",
            "M22 5h-4"
        )
    }
    val SparklesSolid: ImageVector by lazy {
        lucide(
            "sparkles-solid",
            "M10.5 4 Q11.9 11.6 19.5 13 Q11.9 14.4 10.5 22 Q9.1 14.4 1.5 13 Q9.1 11.6 10.5 4 Z",
            "M19 1.8 Q19.5 4.5 22.2 5 Q19.5 5.5 19 8.2 Q18.5 5.5 15.8 5 Q18.5 4.5 19 1.8 Z",
            filled = true,
            strokeWidth = 0.6f
        )
    }
    val Tablet: ImageVector by lazy {
        lucide("tablet", "M6 2h12a2 2 0 0 1 2 2v16a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2Z", "M12 18h.01")
    }
    val Pen: ImageVector by lazy {
        lucide(
            "pen",
            "M 14.5 5.5 L 18.5 9.5 L 9 19 L 5 19 L 5 15 L 14.5 5.5 Z",
            "M 13 7 L 17 11"
        )
    }

    // 그림이 틀을 채우는 다른 아이콘과 크기가 맞게 폭을 넓혔다.
    // 좁으면 옆 글자와의 틈이 혼자 넓어 보인다.
    val Pin: ImageVector by lazy {
        lucide(
            "pin",
            "M 20 10 C 20 16 12 22 12 22 C 12 22 4 16 4 10 C 4 5.58 7.58 2 12 2 C 16.42 2 20 5.58 20 10 Z",
            "M 15 10 C 15 11.66 13.66 13 12 13 C 10.34 13 9 11.66 9 10 C 9 8.34 10.34 7 12 7 C 13.66 7 15 8.34 15 10 Z"
        )
    }
    val Locate: ImageVector by lazy {
        lucide(
            "locate",
            "M2 12h3",
            "M19 12h3",
            "M12 2v3",
            "M12 19v3",
            "M5 12a7 7 0 1 0 14 0a7 7 0 1 0 -14 0",
            "M11 12a1 1 0 1 0 2 0a1 1 0 1 0 -2 0"
        )
    }
    val More: ImageVector by lazy {
        lucide(
            "more",
            "M 7.2 12 C 7.2 12.66 6.66 13.2 6 13.2 C 5.34 13.2 4.8 12.66 4.8 12 C 4.8 11.34 5.34 10.8 6 10.8 C 6.66 10.8 7.2 11.34 7.2 12 Z",
            "M 13.2 12 C 13.2 12.66 12.66 13.2 12 13.2 C 11.34 13.2 10.8 12.66 10.8 12 C 10.8 11.34 11.34 10.8 12 10.8 C 12.66 10.8 13.2 11.34 13.2 12 Z",
            "M 19.2 12 C 19.2 12.66 18.66 13.2 18 13.2 C 17.34 13.2 16.8 12.66 16.8 12 C 16.8 11.34 17.34 10.8 18 10.8 C 18.66 10.8 19.2 11.34 19.2 12 Z"
        )
    }
    val Share: ImageVector by lazy {
        lucide(
            "share",
            "M 12 4 L 12 15 M 12 4 L 8 8 M 12 4 L 16 8 M 6 13 L 6 18.5 C 6 18.9 6.16 19.28 6.44 19.56 C 6.72 19.84 7.1 20 7.5 20 L 16.5 20 C 16.9 20 17.28 19.84 17.56 19.56 C 17.84 19.28 18 18.9 18 18.5 L 18 13"
        )
    }
    val Image: ImageVector by lazy {
        lucide(
            "image",
            "M 6.5 5 L 17.5 5 C 18.88 5 20 6.12 20 7.5 L 20 16.5 C 20 17.88 18.88 19 17.5 19 L 6.5 19 C 5.12 19 4 17.88 4 16.5 L 4 7.5 C 4 6.12 5.12 5 6.5 5 Z",
            "M 10.6 10 C 10.6 10.88 9.88 11.6 9 11.6 C 8.12 11.6 7.4 10.88 7.4 10 C 7.4 9.12 8.12 8.4 9 8.4 C 9.88 8.4 10.6 9.12 10.6 10 Z",
            "M 5 17 L 9.5 12.5 L 12.5 15.5 L 15 13 L 19 16.6"
        )
    }
    val Camera: ImageVector by lazy {
        lucide(
            "camera",
            "M 4 8.5 C 4 8.1 4.16 7.72 4.44 7.44 C 4.72 7.16 5.1 7 5.5 7 L 7.5 7 L 9 5 L 15 5 L 16.5 7 L 18.5 7 C 18.9 7 19.28 7.16 19.56 7.44 C 19.84 7.72 20 8.1 20 8.5 L 20 18 C 20 18.4 19.84 18.78 19.56 19.06 C 19.28 19.34 18.9 19.5 18.5 19.5 L 5.5 19.5 C 5.1 19.5 4.72 19.34 4.44 19.06 C 4.16 18.78 4 18.4 4 18 L 4 8.5 Z",
            "M 15.3 13 C 15.3 14.82 13.82 16.3 12 16.3 C 10.18 16.3 8.7 14.82 8.7 13 C 8.7 11.18 10.18 9.7 12 9.7 C 13.82 9.7 15.3 11.18 15.3 13 Z"
        )
    }
    val Bold: ImageVector by lazy {
        lucide(
            "bold",
            "M 7 5 L 13 5 C 13.93 5 14.82 5.37 15.47 6.03 C 16.13 6.68 16.5 7.57 16.5 8.5 C 16.5 9.43 16.13 10.32 15.47 10.97 C 14.82 11.63 13.93 12 13 12 L 7 12 L 7 5 Z M 7 12 L 14 12 C 14.93 12 15.82 12.37 16.47 13.03 C 17.13 13.68 17.5 14.57 17.5 15.5 C 17.5 16.43 17.13 17.32 16.47 17.97 C 15.82 18.63 14.93 19 14 19 L 7 19 L 7 12 Z"
        )
    }
    val Underline: ImageVector by lazy {
        lucide(
            "underline",
            "M 7 4 L 7 11 C 7 12.33 7.53 13.6 8.46 14.54 C 9.4 15.47 10.67 16 12 16 C 13.33 16 14.6 15.47 15.54 14.54 C 16.47 13.6 17 12.33 17 11 L 17 4 M 6 20 L 18 20"
        )
    }
    val TextColor: ImageVector by lazy {
        lucide("text-color", "M 4 20 L 20 20", "M 6 16 L 12 4 L 18 16", "M 8 12 L 16 12")
    }
    val Highlight: ImageVector by lazy {
        lucide(
            "highlight",
            "M 9 11 L 3 17 L 3 20 L 12 20 L 15 17",
            "M 22 12 L 17.4 16.6 C 16.63 17.37 15.37 17.37 14.6 16.6 L 9.4 11.4 C 8.63 10.63 8.63 9.37 9.4 8.6 L 14 4"
        )
    }
    val Checklist: ImageVector by lazy {
        lucide("checklist", "M 3 7 L 5 9 L 9 5", "M 3 17 L 5 19 L 9 15", "M 13 6 L 21 6", "M 13 12 L 21 12", "M 13 18 L 21 18")
    }

    val BulletList: ImageVector by lazy {
        lucide(
            "bullet-list",
            "M 9 7 L 20 7 M 9 12 L 20 12 M 9 17 L 20 17",
            "M 5.9 7 C 5.9 7.5 5.5 7.9 5 7.9 C 4.5 7.9 4.1 7.5 4.1 7 C 4.1 6.5 4.5 6.1 5 6.1 C 5.5 6.1 5.9 6.5 5.9 7 Z",
            "M 5.9 12 C 5.9 12.5 5.5 12.9 5 12.9 C 4.5 12.9 4.1 12.5 4.1 12 C 4.1 11.5 4.5 11.1 5 11.1 C 5.5 11.1 5.9 11.5 5.9 12 Z",
            "M 5.9 17 C 5.9 17.5 5.5 17.9 5 17.9 C 4.5 17.9 4.1 17.5 4.1 17 C 4.1 16.5 4.5 16.1 5 16.1 C 5.5 16.1 5.9 16.5 5.9 17 Z"
        )
    }
    val Plus: ImageVector by lazy {
        lucide(
            "plus",
            "M 12 5 L 12 19 M 5 12 L 19 12"
        )
    }
    val Smile: ImageVector by lazy {
        lucide(
            "smile",
            "M 20 12 C 20 16.42 16.42 20 12 20 C 7.58 20 4 16.42 4 12 C 4 7.58 7.58 4 12 4 C 16.42 4 20 7.58 20 12 Z",
            "M 8.5 14 C 8.85 14.63 9.35 15.15 9.97 15.51 C 10.58 15.87 11.29 16.06 12 16.06 C 12.71 16.06 13.42 15.87 14.03 15.51 C 14.65 15.15 15.15 14.63 15.5 14",
            "M 9.5 9.5 L 9.5 10 M 14.5 9.5 L 14.5 10"
        )
    }
    val Restore: ImageVector by lazy {
        lucide(
            "restore",
            "M 5 12 C 5 13.62 5.56 15.18 6.58 16.43 C 7.61 17.68 9.03 18.54 10.62 18.86 C 12.2 19.18 13.85 18.94 15.28 18.18 C 16.71 17.43 17.83 16.2 18.45 14.71 C 19.08 13.22 19.17 11.56 18.71 10.01 C 18.25 8.46 17.27 7.12 15.93 6.21 C 14.6 5.3 12.98 4.88 11.37 5.03 C 9.76 5.17 8.25 5.87 7.1 7",
            "M 5 4 L 5 8 L 9 8"
        )
    }
    val WeatherSunny: ImageVector by lazy {
        lucide(
            "weather-sunny",
            "M 15.8 12 C 15.8 14.1 14.1 15.8 12 15.8 C 9.9 15.8 8.2 14.1 8.2 12 C 8.2 9.9 9.9 8.2 12 8.2 C 14.1 8.2 15.8 9.9 15.8 12 Z",
            "M 12 3 L 12 5 M 12 19 L 12 21 M 3 12 L 5 12 M 19 12 L 21 12 M 5.6 5.6 L 7 7 M 17 17 L 18.4 18.4 M 5.6 18.4 L 7 17 M 17 7 L 18.4 5.6"
        )
    }
    val WeatherPartlyCloudy: ImageVector by lazy {
        lucide(
            "weather-partly-cloudy",
            "M 12 9 C 12 10.66 10.66 12 9 12 C 7.34 12 6 10.66 6 9 C 6 7.34 7.34 6 9 6 C 10.66 6 12 7.34 12 9 Z",
            "M 9 3.5 L 9 4.7 M 3.5 9 L 4.7 9 M 5.1 5.1 L 6 6 M 12.9 5.1 L 12 6",
            "M 8 19 L 17 19 C 17.93 19 18.82 18.63 19.47 17.97 C 20.13 17.32 20.5 16.43 20.5 15.5 C 20.5 14.57 20.13 13.68 19.47 13.03 C 18.82 12.37 17.93 12 17 12 C 16.6 11.09 15.9 10.33 15.03 9.86 C 14.15 9.38 13.14 9.21 12.16 9.37 C 11.17 9.53 10.27 10.01 9.59 10.74 C 8.91 11.47 8.49 12.41 8.4 13.4 C 7.82 13.57 7.3 13.92 6.92 14.4 C 6.53 14.87 6.31 15.45 6.26 16.06 C 6.22 16.67 6.36 17.28 6.67 17.8 C 6.98 18.33 7.45 18.74 8 19 Z"
        )
    }
    val WeatherMostlyCloudy: ImageVector by lazy {
        lucide(
            "weather-mostly-cloudy",
            "M 7 18 L 17 18 C 18.06 18 19.08 17.58 19.83 16.83 C 20.58 16.08 21 15.06 21 14 C 21 12.94 20.58 11.92 19.83 11.17 C 19.08 10.42 18.06 10 17 10 C 16.54 8.84 15.71 7.87 14.63 7.25 C 13.55 6.63 12.29 6.39 11.06 6.58 C 9.83 6.77 8.7 7.36 7.85 8.28 C 7.01 9.19 6.49 10.36 6.4 11.6 C 5.69 11.84 5.09 12.3 4.68 12.93 C 4.27 13.55 4.09 14.3 4.16 15.04 C 4.23 15.78 4.55 16.48 5.06 17.01 C 5.58 17.55 6.26 17.9 7 18 Z"
        )
    }

    // 흐림, 비, 비/눈, 눈은 구름 자리를 같게 두고 아래 표시만 다르게 한다.
    val WeatherCloudy: ImageVector by lazy {
        lucide(
            "weather-cloudy",
            "M 7 15 L 17 15 C 18.06 15 19.08 14.58 19.83 13.83 C 20.58 13.08 21 12.06 21 11 C 21 9.94 20.58 8.92 19.83 8.17 C 19.08 7.42 18.06 7 17 7 C 16.54 5.84 15.71 4.87 14.63 4.25 C 13.55 3.63 12.29 3.39 11.06 3.58 C 9.83 3.77 8.7 4.36 7.85 5.28 C 7.01 6.19 6.49 7.36 6.4 8.6 C 5.69 8.84 5.09 9.3 4.68 9.93 C 4.27 10.55 4.09 11.3 4.16 12.04 C 4.23 12.78 4.55 13.48 5.06 14.01 C 5.58 14.55 6.26 14.9 7 15 Z",
            "M 9 18.5 L 17 18.5"
        )
    }
    val WeatherRain: ImageVector by lazy {
        lucide(
            "weather-rain",
            "M 7 15 L 17 15 C 18.06 15 19.08 14.58 19.83 13.83 C 20.58 13.08 21 12.06 21 11 C 21 9.94 20.58 8.92 19.83 8.17 C 19.08 7.42 18.06 7 17 7 C 16.54 5.84 15.71 4.87 14.63 4.25 C 13.55 3.63 12.29 3.39 11.06 3.58 C 9.83 3.77 8.7 4.36 7.85 5.28 C 7.01 6.19 6.49 7.36 6.4 8.6 C 5.69 8.84 5.09 9.3 4.68 9.93 C 4.27 10.55 4.09 11.3 4.16 12.04 C 4.23 12.78 4.55 13.48 5.06 14.01 C 5.58 14.55 6.26 14.9 7 15 Z",
            "M 9 18 L 8 20.5 M 13 18 L 12 20.5 M 17 18 L 16 20.5"
        )
    }
    val WeatherSleet: ImageVector by lazy {
        lucide(
            "weather-sleet",
            "M 7 15 L 17 15 C 18.06 15 19.08 14.58 19.83 13.83 C 20.58 13.08 21 12.06 21 11 C 21 9.94 20.58 8.92 19.83 8.17 C 19.08 7.42 18.06 7 17 7 C 16.54 5.84 15.71 4.87 14.63 4.25 C 13.55 3.63 12.29 3.39 11.06 3.58 C 9.83 3.77 8.7 4.36 7.85 5.28 C 7.01 6.19 6.49 7.36 6.4 8.6 C 5.69 8.84 5.09 9.3 4.68 9.93 C 4.27 10.55 4.09 11.3 4.16 12.04 C 4.23 12.78 4.55 13.48 5.06 14.01 C 5.58 14.55 6.26 14.9 7 15 Z",
            "M 9 18 L 8 20.5 M 15.5 19.5 L 15.5 19.51 M 12.5 21.5 L 12.5 21.51"
        )
    }
    val WeatherSnow: ImageVector by lazy {
        lucide(
            "weather-snow",
            "M 7 15 L 17 15 C 18.06 15 19.08 14.58 19.83 13.83 C 20.58 13.08 21 12.06 21 11 C 21 9.94 20.58 8.92 19.83 8.17 C 19.08 7.42 18.06 7 17 7 C 16.54 5.84 15.71 4.87 14.63 4.25 C 13.55 3.63 12.29 3.39 11.06 3.58 C 9.83 3.77 8.7 4.36 7.85 5.28 C 7.01 6.19 6.49 7.36 6.4 8.6 C 5.69 8.84 5.09 9.3 4.68 9.93 C 4.27 10.55 4.09 11.3 4.16 12.04 C 4.23 12.78 4.55 13.48 5.06 14.01 C 5.58 14.55 6.26 14.9 7 15 Z",
            "M 9 19 L 9 19.01 M 12 21 L 12 21.01 M 15 19 L 15 19.01 M 12 18 L 12 18.01"
        )
    }

    /** 즐겨찾기 빈 별과 찬 별(Figma 별 경로). */
    val StarOutline: ImageVector by lazy { lucide("star-outline", STAR) }
    val StarFilled: ImageVector by lazy { lucide("star-filled", STAR, filled = true) }

    private fun tab(name: String, stroke: Float, vararg paths: String): ImageVector = lucide(name, *paths, strokeWidth = stroke)

    private fun lucide(name: String, vararg paths: String, filled: Boolean = false, strokeWidth: Float = 1.75f): ImageVector {
        val builder = ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        for (path in paths) {
            builder.addPath(
                pathData = addPathNodes(path),
                fill = if (filled) SolidColor(Color.Black) else null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = strokeWidth,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }
        return builder.build()
    }
}
