package org.sjhstudio.diary.core.data

import kotlinx.coroutines.flow.Flow
import org.sjhstudio.diary.core.model.AppSettings
import org.sjhstudio.diary.core.model.BodyTextSize
import org.sjhstudio.diary.core.model.DiaryFont
import org.sjhstudio.diary.core.model.ListStyle
import org.sjhstudio.diary.core.model.MoodLook
import org.sjhstudio.diary.core.model.PreviewLength
import org.sjhstudio.diary.core.model.ThemeChoice
import org.sjhstudio.diary.core.model.ThemeMode

/**
 * 화면이 쓰는 앱 설정
 * 기록 이전은 설정을 한꺼번에 옮기므로 이 저장소 대신 SettingsStore를 바로 씀
 */
interface SettingsRepository {
    val settings: Flow<AppSettings>

    suspend fun setFont(font: DiaryFont)

    suspend fun setBodyTextSize(size: BodyTextSize)

    suspend fun setThemeMode(mode: ThemeMode)

    /** 테마 색 설정을 한 번에 바꿈(정한 색 고르기, 직접 고른 색 쓰기·지우기) */
    suspend fun updateTheme(transform: (ThemeChoice) -> ThemeChoice)

    /** 기분 테마와 클레이 꼴을 한 번에 씀 */
    suspend fun updateMoodLook(transform: (MoodLook) -> MoodLook)

    suspend fun setAskLocationAgain(ask: Boolean)

    /** [minuteOfDay]는 0시부터 센 분(0~1439) */
    suspend fun setReminder(enabled: Boolean, minuteOfDay: Int)

    suspend fun setListStyle(style: ListStyle)

    /**
     * 목록 미리보기 길이(스펙 008)
     * [chosen]은 사람이 고른 값. 참이면 옛 설정을 다시 잇지 않음
     */
    suspend fun setPreviewLength(length: PreviewLength, chosen: Boolean = true)

    /** 목록에서 서식 그대로 보기. 그리는지는 요금제와 함께 봄 */
    suspend fun setShowListFormatting(show: Boolean)

    /** "글꼴이 바뀌었어요"를 한 번 알린 뒤 지움 */
    suspend fun clearFontReplacedNotice()
}
