package org.sjhstudio.diary.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.sjhstudio.diary.core.migration.LegacyMigration
import org.sjhstudio.diary.core.model.MigrationReport
import org.sjhstudio.diary.core.model.MigrationStep

/** 기록 이전 결과(M5) 화면 상태. 결과 파일이 없으면 [report]가 null이다. */
data class MigrationReportUiState(val report: MigrationReport? = null, val migratedOn: LocalDate? = null)

/**
 * 설정 > 기록 이전 결과(FR-010, FR-011). 이전 직후 결과 화면과 같은 숫자를 보인다.
 * 다시 시도 버튼은 없다. 앱이 업데이트된 뒤 이전 관문이 못 읽은 일기를 한 번 더 읽는다.
 */
class MigrationReportViewModel(
    private val migration: LegacyMigration,
    private val timeZone: TimeZone,
    /** 결과 파일을 읽는 곳. 시험에서는 시험 디스패처로 바꾼다. */
    private val background: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {
    private val _state = MutableStateFlow(MigrationReportUiState())
    val state: StateFlow<MigrationReportUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val report = withContext(background) { migration.report() }
            _state.value = MigrationReportUiState(report, report?.finishedAt?.toLocalDateTime(timeZone)?.date)
        }
    }
}

/** 설정에 "기록 이전 결과" 줄을 둘지. 일부만 옮겨진 기기에서만 둔다. 다 옮긴 기기에는 줄이 없다(M5 설명). */
fun showsMigrationReport(migration: LegacyMigration): Boolean = migration.state().step == MigrationStep.PARTIAL
