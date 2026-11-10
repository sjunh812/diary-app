package org.sjhstudio.diary.core.backup

import androidx.room3.withWriteTransaction
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import org.sjhstudio.diary.core.database.DiaryDatabase
import org.sjhstudio.diary.core.database.MediaEntity
import org.sjhstudio.diary.core.database.MergeMarkEntity
import org.sjhstudio.diary.core.database.SyncBaseEntity
import org.sjhstudio.diary.core.model.MediaStatus
import org.sjhstudio.diary.core.model.TRASH_KEEP_DAYS

/**
 * 합치기 계획
 * 미리보기 숫자는 이 값이고, 확인하면 같은 값대로 씀
 */
data class MergePlan(val items: List<Pair<MergeEntry, MergeAction>>, val local: Map<String, MergeEntry>) {
    fun count(action: MergeAction): Int = items.count { it.second == action }
}

/** 합친 결과 숫자 */
data class MergeCounts(val added: Int, val updated: Int, val keptBoth: Int)

/**
 * 합치기를 계획하고 한 트랜잭션으로 씀
 * 중간에 실패하면 아무것도 바뀌지 않음(contracts/merge-rules.md)
 */
@OptIn(ExperimentalUuidApi::class)
class MergeApplier(
    private val db: DiaryDatabase,
    private val fileExists: (fileName: String) -> Boolean,
    private val clock: Clock = Clock.System,
    private val newId: () -> String = { Uuid.random().toString() }
) {
    suspend fun plan(backup: List<MergeEntry>, fromOtherDevice: Boolean = false): MergePlan {
        val dao = db.backupDao()
        val media = dao.allMedia().groupBy { it.entryId }
        val local = dao.allEntries().associate { it.id to MergeEntry(it, media[it.id].orEmpty()) }
        val bases = dao.bases().associate { it.entryId to it.baseUpdatedAt }
        val sameTime = local.values.groupBy { it.entry.updatedAt }
        return MergePlan(
            backup.map { item ->
                val action = MergeRules.decide(item, local[item.entry.id], bases[item.entry.id], fromOtherDevice)
                // 둘 다 남긴 일기는 기준값을 그대로 두어 같은 백업을 또 합치면 또 둘 다로 나옴
                // 백업과 같은 사본이 이미 있으면 그 일은 끝난 것
                val copied =
                    action == MergeAction.KEEP_BOTH && sameTime[item.entry.updatedAt].orEmpty().any { MergeRules.sameContent(item, it) }
                item to if (copied) MergeAction.KEEP else action
            },
            local
        )
    }

    suspend fun apply(plan: MergePlan, source: String): MergeCounts {
        val dao = db.backupDao()
        val entries = db.entryDao()
        val media = db.mediaDao()
        val now = clock.now().toEpochMilliseconds()
        // 다른 기기에서 이전한 일기면 같은 옛 번호가 다른 일기
        // 번호는 표시에 쓰지 않아 겹치면 비움
        val taken = plan.local.values.mapNotNull { row -> row.entry.legacyId?.let { it to row.entry.id } }.toMap(HashMap())
        db.withWriteTransaction {
            for ((item, action) in plan.items) {
                val legacyId = item.entry.legacyId?.takeIf { taken[it] == null || taken[it] == item.entry.id }
                val incoming = item.entry.copy(deletedAt = keepInTrash(item.entry.deletedAt, now), legacyId = legacyId)
                when (action) {
                    MergeAction.KEEP -> Unit

                    MergeAction.ADD -> {
                        entries.insertAll(listOf(incoming))
                        media.insertAll(item.media.map { it.settled() })
                        dao.setBase(listOf(SyncBaseEntity(incoming.id, incoming.updatedAt)))
                    }

                    MergeAction.REPLACE -> {
                        dao.updateEntry(incoming)
                        media.deleteForEntry(incoming.id)
                        media.insertAll(item.media.map { it.settled() })
                        dao.setBase(listOf(SyncBaseEntity(incoming.id, incoming.updatedAt)))
                        // 수정하다 남겨 둔 글이 합쳐 온 일기를 덮어쓰지 않게 함
                        db.draftDao().markEntryChanged(incoming.id)
                    }

                    MergeAction.KEEP_BOTH -> {
                        val id = newId()
                        entries.insertAll(listOf(incoming.copy(id = id, legacyId = null)))
                        media.insertAll(item.media.map { it.settled().copy(id = newId(), entryId = id) })
                        dao.setBase(listOf(SyncBaseEntity(id, incoming.updatedAt)))
                        dao.mark(
                            listOf(
                                MergeMarkEntity(incoming.id, "kept_both", id, source, now),
                                MergeMarkEntity(id, "kept_both", incoming.id, source, now)
                            )
                        )
                    }
                }
            }
        }
        return MergeCounts(plan.count(MergeAction.ADD), plan.count(MergeAction.REPLACE), plan.count(MergeAction.KEEP_BOTH))
    }

    /**
     * 이번 백업에 든 사진이면 찾을 수 없다고 적어 둔 줄도 다시 받음
     * 복원이 끊기거나 깨진 파일을 읽었을 때 적힌 표시가 영영 남지 않게 함
     */
    suspend fun revive(inBackup: Set<String>) {
        val dao = db.backupDao()
        for (photo in dao.missingFiles().filter { it.sha256 in inBackup }) {
            val name = photo.fileName ?: continue
            dao.reviveMissing(photo.sha256, name, (if (fileExists(name)) MediaStatus.NORMAL else MediaStatus.PENDING).id)
        }
    }

    /**
     * 사진 파일이 이 기기에 있으면 바로 쓰고, 없으면 받을 때까지 대기
     * 찾을 수 없던 줄은 그대로 둠
     */
    private fun MediaEntity.settled(): MediaEntity {
        if (status == MediaStatus.MISSING.id) return this
        return copy(status = (if (fileName?.let(fileExists) == true) MediaStatus.NORMAL else MediaStatus.PENDING).id)
    }

    /** 복원 직후 자동 비움으로 다시 잃지 않게 휴지통 남은 날이 하루보다 적으면 7일을 줌 */
    private fun keepInTrash(deletedAt: Long?, now: Long): Long? {
        if (deletedAt == null) return null
        val left = deletedAt + TRASH_KEEP_DAYS.days.inWholeMilliseconds - now
        return if (left < 1.days.inWholeMilliseconds) now - (TRASH_KEEP_DAYS - GRACE_DAYS).days.inWholeMilliseconds else deletedAt
    }

    private companion object {
        const val GRACE_DAYS = 7
    }
}
