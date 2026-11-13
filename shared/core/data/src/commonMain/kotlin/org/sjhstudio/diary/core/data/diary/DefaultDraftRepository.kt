package org.sjhstudio.diary.core.data.diary

import androidx.room3.withWriteTransaction
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.sjhstudio.diary.core.data.photo.PhotoStore
import org.sjhstudio.diary.core.database.DiaryDatabase
import org.sjhstudio.diary.core.database.DraftEntity
import org.sjhstudio.diary.core.database.DraftMediaEntity
import org.sjhstudio.diary.core.database.EntryEntity
import org.sjhstudio.diary.core.database.MediaEntity
import org.sjhstudio.diary.core.database.MergeMarkEntity
import org.sjhstudio.diary.core.database.asEntity
import org.sjhstudio.diary.core.database.asModel
import org.sjhstudio.diary.core.database.isEditable
import org.sjhstudio.diary.core.model.AddPhotosResult
import org.sjhstudio.diary.core.model.Draft
import org.sjhstudio.diary.core.model.DraftKey
import org.sjhstudio.diary.core.model.DraftPhoto
import org.sjhstudio.diary.core.model.MediaStatus
import org.sjhstudio.diary.core.model.PhotoLimit
import org.sjhstudio.diary.core.model.PickedImage
import org.sjhstudio.diary.core.model.richtext.RichTextSanitizer
import org.sjhstudio.diary.core.model.richtext.toPlainText

/**
 * 저장 조건(FR-002): 글, 사진, 기분 가운데 하나는 있어야 함
 * [hasText]는 본문을 아직 HTML로 꺼내지 않았을 때 넘김
 */
fun Draft.canSave(hasText: Boolean = toPlainText(bodyHtml).isNotBlank()): Boolean = hasText || photos.isNotEmpty() || mood != null

class DefaultDraftRepository(
    private val db: DiaryDatabase,
    private val photos: PhotoStore,
    private val clock: Clock = Clock.System,
    /**
     * 한 편에 새로 붙일 수 있는 사진 수
     * 지금 요금제를 따름
     * 재설치 직후처럼 요금제를 아직 모르면 알 때까지 잠깐 기다림
     */
    private val photoLimit: suspend () -> Int = { PhotoLimit.FREE }
) : DraftRepository {
    private val drafts get() = db.draftDao()
    private val entries get() = db.entryDao()
    private val media get() = db.mediaDao()

    /**
     * 임시 저장본마다 사진 바꾸는 일을 한 줄로 세움
     * 그림을 읽는 동안 다른 일이 끼어들지 않게
     */
    private val locks = mutableMapOf<String, Mutex>()
    private val locksGuard = Mutex()

    private suspend fun <T> inOrder(key: DraftKey, block: suspend () -> T): T {
        val lock = locksGuard.withLock { locks.getOrPut(key.value) { Mutex() } }
        return lock.withLock { block() }
    }

    override fun draft(key: DraftKey): Flow<Draft?> =
        combine(drafts.observe(key.value), drafts.observeMedia(key.value)) { draft, rows -> draft?.asModel(rows) }

    override suspend fun recentPlaces(): List<String> = db.entryDao().recentPlaces(RECENT_PLACES)

    override suspend fun latestNew(): Draft? = drafts.latestNew()?.let { it.asModel(drafts.media(it.key)) }

    override suspend fun openEdit(entryId: String): OpenedEdit? =
        drafts.openEdit(entryId, clock.now().toEpochMilliseconds(), entries, media)?.let { opened ->
            OpenedEdit(opened.draft.asModel(opened.media), opened.resumed, opened.draft.entryChanged)
        }

    override suspend fun matchesEntry(key: DraftKey.Edit): Boolean = inOrder(key) {
        val draft = drafts.get(key.value) ?: return@inOrder true
        val old = entries.get(key.entryId) ?: return@inOrder false
        val photosChanged = photosChanged(key, media.forEntry(old.id))
        !photosChanged && draft.toEntry(old.id, old, clock.now().toEpochMilliseconds(), photosChanged = false) == old
    }

    private suspend fun photosChanged(key: DraftKey, oldMedia: List<MediaEntity>): Boolean =
        drafts.media(key.value).map { it.sha256 } != oldMedia.filter { it.isEditable }.map { it.sha256 }

    // 저장과 지우기 사이에 끼어 저장한 일기의 임시 저장본이 되살아나지 않게 같은 줄에 세움
    override suspend fun save(draft: Draft) = inOrder(draft.key) { drafts.upsert(draft.asEntity()) }

    override suspend fun addPhotos(key: DraftKey, picked: List<PickedImage>): AddPhotosResult = inOrder(key) {
        // 화면이 더하는 사이 저장이나 버리기로 임시 저장본이 지워졌으면 붙일 곳이 없음
        if (drafts.get(key.value) == null) return@inOrder AddPhotosResult.Added(0, failed = picked.size)
        val limit = photoLimit()
        val fixed = (key as? DraftKey.Edit)?.let { edit -> media.forEntry(edit.entryId).count { it.status != MediaStatus.NORMAL.id } } ?: 0
        val room = (limit - drafts.media(key.value).size - fixed).coerceAtLeast(0)
        var added = 0
        var failed = 0
        for (image in picked.take(room)) {
            // 읽지 못한 그림은 공간 문제가 아니라 나머지를 계속 더함
            val stored =
                try {
                    photos.store(image)
                } catch (_: okio.IOException) {
                    return@inOrder AddPhotosResult.NoSpace(added)
                }
            if (stored == null) {
                failed++
                continue
            }
            drafts.appendMedia(DraftMediaEntity(key.value, stored.fileName, stored.sha256, stored.width, stored.height, stored.byteSize, 0))
            added++
        }
        if (picked.size > room) {
            AddPhotosResult.LimitReached(limit, canUpgrade = limit < PhotoLimit.PLUS, added = added)
        } else {
            AddPhotosResult.Added(added, failed)
        }
    }

    override suspend fun removePhoto(key: DraftKey, index: Int): DraftPhoto? = inOrder(key) {
        drafts.removeMediaAt(key.value, index)?.let { DraftPhoto(it.fileName, it.sha256, it.width, it.height, it.byteSize) }
    }

    override suspend fun restorePhoto(key: DraftKey, photo: DraftPhoto, index: Int): Boolean = inOrder(key) {
        if (drafts.get(key.value) == null) return@inOrder false
        val fixed = (key as? DraftKey.Edit)?.let { edit -> media.forEntry(edit.entryId).count { it.status != MediaStatus.NORMAL.id } } ?: 0
        if (drafts.media(key.value).size + fixed >= photoLimit()) return@inOrder false
        val row = DraftMediaEntity(key.value, photo.fileName, photo.sha256, photo.width, photo.height, photo.byteSize, sortOrder = 0)
        drafts.insertMediaAt(key.value, row, index)
        true
    }

    override suspend fun reorderPhotos(key: DraftKey, order: List<Int>) = inOrder(key) { drafts.reorderMedia(key.value, order) }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun commit(key: DraftKey): String = inOrder(key) {
        val draft = requireNotNull(drafts.get(key.value)) { "임시 저장본이 없다" }
        val rows = drafts.media(key.value)
        val now = clock.now().toEpochMilliseconds()
        // 복원으로 일기가 사라졌어도 쓴 글은 새 일기로 남김
        val old = (key as? DraftKey.Edit)?.let { entries.get(it.entryId) }
        val oldMedia = old?.let { media.forEntry(it.id) }.orEmpty()
        val photosChanged = photosChanged(key, oldMedia)
        val id = old?.id ?: Uuid.random().toString()
        val entry = draft.toEntry(id, old, now, photosChanged)
        val edited = entry != old || photosChanged
        if (old != null && draft.entryChanged && edited) return@inOrder keepBoth(key, draft, rows, old, now)
        drafts.commit(key.value, entry, mediaRows(id, rows, oldMedia, now), entries, media)
        id
    }

    /**
     * 복원이 바꾼 일기를 남겨 둔 글로 덮어쓰지 않음
     * 합치기의 둘 다 남기기처럼 표시해 사용자가 상세에서 고르게 함
     */
    @OptIn(ExperimentalUuidApi::class)
    private suspend fun keepBoth(key: DraftKey, draft: DraftEntity, rows: List<DraftMediaEntity>, old: EntryEntity, now: Long): String {
        val id = Uuid.random().toString()
        val copy = draft.toEntry(id, null, now, photosChanged = true)
        db.withWriteTransaction {
            drafts.commit(key.value, copy, mediaRows(id, rows, emptyList(), now), entries, media)
            db.backupDao().mark(
                listOf(
                    MergeMarkEntity(old.id, KEPT_BOTH, id, SOURCE_DRAFT, now),
                    MergeMarkEntity(id, KEPT_BOTH, old.id, SOURCE_DRAFT, now)
                )
            )
        }
        return id
    }

    private fun DraftEntity.toEntry(id: String, old: EntryEntity?, now: Long, photosChanged: Boolean): EntryEntity {
        // 고치지 않은 본문은 그대로 둠
        // 레거시 원문과 서식 없는 글이 바뀌지 않게
        val kept = old != null && bodyHtml == old.bodyHtml
        // 공백과 빈 줄뿐인 본문은 빈 본문(기분만 남긴 일기)
        val body = if (kept) bodyHtml else RichTextSanitizer.sanitize(bodyHtml).takeUnless { toPlainText(it).isBlank() }.orEmpty()
        val plain = if (kept) old.plainText else toPlainText(body)
        val entry =
            EntryEntity(
                id = id,
                date = date,
                time = time,
                bodyHtml = body,
                plainText = plain,
                mood = mood,
                weather = weather,
                placeName = placeName?.trim()?.takeIf { it.isNotEmpty() },
                // 즐겨찾기는 수정 화면에서 바꾸지 않음
                // 남겨 둔 글을 저장해도 그사이 누른 별이 되돌아가지 않게
                isFavorite = old?.isFavorite ?: isFavorite,
                createdAt = old?.createdAt ?: now,
                updatedAt = old?.updatedAt ?: now,
                deletedAt = old?.deletedAt,
                legacyId = old?.legacyId,
                legacyCreateDate = old?.legacyCreateDate,
                legacyLocation = old?.legacyLocation
            )
        // 보이는 값이 바뀔 때만 고친 시각을 올림(스펙 003 합치기가 씀)
        // 사진의 더하기, 빼기, 순서도 보이는 값
        return if (old == null || (entry == old && !photosChanged)) entry else entry.copy(updatedAt = now)
    }

    /**
     * 같은 사진은 원래 줄(ID, 만든 시각, 옛 경로)을 잇고 순서만 바꿈
     * 찾을 수 없는 사진 줄은 사용자가 뺄 수 없으니 그대로 뒤에 남김(헌법 I)
     */
    @OptIn(ExperimentalUuidApi::class)
    private fun mediaRows(entryId: String, rows: List<DraftMediaEntity>, old: List<MediaEntity>, now: Long): List<MediaEntity> {
        val reusable = old.filter { it.status == MediaStatus.NORMAL.id }.groupBy { it.sha256 }.mapValues { it.value.toMutableList() }
        val kept =
            rows.mapIndexed { index, row ->
                reusable[row.sha256]?.removeFirstOrNull()?.copy(sortOrder = index)
                    ?: MediaEntity(
                        id = Uuid.random().toString(),
                        entryId = entryId,
                        sortOrder = index,
                        status = MediaStatus.NORMAL.id,
                        fileName = row.fileName,
                        sha256 = row.sha256,
                        width = row.width,
                        height = row.height,
                        byteSize = row.byteSize,
                        legacyPath = null,
                        createdAt = now
                    )
            }
        val fixed = old.filter { it.status != MediaStatus.NORMAL.id }.mapIndexed { index, row -> row.copy(sortOrder = rows.size + index) }
        return kept + fixed
    }

    override suspend fun discard(key: DraftKey) = inOrder(key) { drafts.delete(key.value) }
}

private const val RECENT_PLACES = 6
private const val KEPT_BOTH = "kept_both"
private const val SOURCE_DRAFT = "draft"
