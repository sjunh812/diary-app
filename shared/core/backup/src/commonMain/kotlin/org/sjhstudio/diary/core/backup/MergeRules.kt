package org.sjhstudio.diary.core.backup

import org.sjhstudio.diary.core.database.EntryEntity
import org.sjhstudio.diary.core.database.MediaEntity

/** 일기 하나와 그 사진 줄. */
data class MergeEntry(val entry: EntryEntity, val media: List<MediaEntity>)

/** 합치기 결과(contracts/merge-rules.md 표의 결과 칸). */
enum class MergeAction { ADD, KEEP, REPLACE, KEEP_BOTH }

/**
 * 드라이브·묶음 파일 합치기 규칙.
 * 쓰기 없이 돌려 미리보기 숫자도 여기서 낸다.
 * 지금 기기의 일기는 지우지 않는다(FR-022).
 */
object MergeRules {
    /**
     * [base]는 이 일기의 `sync_base` 값이다.
     * 기준값이 없으면 어느 쪽이 먼저인지 알 수 없다.
     * 이 기기가 만든 백업이면 한 줄기 기록이라 늦은 쪽이 맞고,
     * 다른 기기 백업([fromOtherDevice])이면 한쪽 수정을 잃지 않게 둘 다 남긴다(헌법 I, 2026-11-06 사용자 결정).
     */
    fun decide(backup: MergeEntry, local: MergeEntry?, base: Long?, fromOtherDevice: Boolean = false): MergeAction {
        if (local == null) return MergeAction.ADD
        val b = backup.entry.updatedAt
        val l = local.entry.updatedAt
        if (l == b) return if (sameContent(backup, local)) MergeAction.KEEP else MergeAction.KEEP_BOTH
        if (base == null) {
            return when {
                !fromOtherDevice -> if (b > l) MergeAction.REPLACE else MergeAction.KEEP
                sameContent(backup, local) -> MergeAction.KEEP
                else -> MergeAction.KEEP_BOTH
            }
        }
        return when {
            b <= base -> MergeAction.KEEP

            // 시계가 뒤로 간 뒤에 고치면 L이 S보다 작다.
            // S와 다르면 이 기기에서 고친 것이다.
            l == base -> MergeAction.REPLACE

            sameContent(backup, local) -> MergeAction.KEEP

            else -> MergeAction.KEEP_BOTH
        }
    }

    /**
     * 둘 다 휴지통이면 날짜는 견주지 않는다.
     * 복원이 휴지통 날짜를 늦추기도 해서, 날짜까지 견주면 같은 백업을 다시 합칠 때 일기가 하나 더 생긴다.
     */
    fun sameContent(a: MergeEntry, b: MergeEntry): Boolean {
        val x = a.entry
        val y = b.entry
        return x.date == y.date && x.time == y.time && x.bodyHtml == y.bodyHtml && x.mood == y.mood && x.weather == y.weather &&
            x.placeName == y.placeName && x.isFavorite == y.isFavorite && (x.deletedAt == null) == (y.deletedAt == null) &&
            a.mediaKey() == b.mediaKey()
    }

    /**
     * 사진을 받는 중이어도 같은 사진이다.
     * 그래서 상태는 견주지 않고, 해시가 없는 이전 사진 줄은 옛 경로로 견준다.
     */
    private fun MergeEntry.mediaKey() = media.sortedBy { it.sortOrder }.map { it.sortOrder to (it.sha256 ?: it.legacyPath) }
}
