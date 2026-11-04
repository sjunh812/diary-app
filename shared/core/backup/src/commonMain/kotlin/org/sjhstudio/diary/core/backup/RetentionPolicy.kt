package org.sjhstudio.diary.core.backup

/** 정리할지 고를 백업 하나. [month]는 기기 현지 시각의 `yyyy-MM`이다. */
data class RetainedBackup(val name: String, val createdAt: Long, val month: String)

/** 이 기기의 끝까지 된 백업 중 최근 5개와, 그보다 옛것은 달마다 가장 늦은 1개를 남긴다(FR-010). */
object RetentionPolicy {
    private const val RECENT = 5

    /** 지울 백업 이름. 다른 기기의 백업은 넘기지 않는다. */
    fun toDelete(backups: List<RetainedBackup>): Set<String> {
        val sorted = backups.sortedByDescending { it.createdAt }
        val keep = sorted.take(RECENT).map { it.name }.toMutableSet()
        sorted.drop(RECENT).groupBy { it.month }.values.forEach { keep += it.first().name }
        return backups.map { it.name }.toSet() - keep
    }
}
