package org.sjhstudio.diary.core.backup

/**
 * 정리할지 고를 백업 하나
 * [month]는 기기 현지 시각의 `yyyy-MM`
 */
data class RetainedBackup(val name: String, val createdAt: Long, val month: String)

/**
 * 이 기기의 끝까지 된 백업 중 최근 5개와, 그보다 옛것은 백업이 있는 최근 12개 달마다 가장 늦은 1개를 남김(FR-010)
 * 한 기기에 많아야 17개라 일기가 많아도 드라이브에 끝없이 쌓이지 않음
 * 달 수는 달력이 아니라 백업이 있는 달로 셈. 한동안 쉬었다 다시 써도 되돌아갈 시점이 남음
 */
object RetentionPolicy {
    private const val RECENT = 5
    private const val MONTHS = 12

    /**
     * 지울 백업 이름
     * 다른 기기의 백업은 넘기지 않음
     */
    fun toDelete(backups: List<RetainedBackup>): Set<String> {
        val sorted = backups.sortedByDescending { it.createdAt }
        val keep = sorted.take(RECENT).map { it.name }.toMutableSet()
        // 늦은 백업부터 묶여 앞의 달이 최근 달
        sorted.drop(RECENT).groupBy { it.month }.values.take(MONTHS).forEach { keep += it.first().name }
        return backups.map { it.name }.toSet() - keep
    }
}
