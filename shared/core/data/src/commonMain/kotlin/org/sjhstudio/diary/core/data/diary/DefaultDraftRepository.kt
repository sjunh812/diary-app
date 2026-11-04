package org.sjhstudio.diary.core.data.diary

import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.sjhstudio.diary.core.data.photo.PhotoStore
import org.sjhstudio.diary.core.data.richtext.RichTextSanitizer
import org.sjhstudio.diary.core.data.richtext.toPlainText
import org.sjhstudio.diary.core.database.DiaryDatabase
import org.sjhstudio.diary.core.database.DraftEntity
import org.sjhstudio.diary.core.database.DraftMediaEntity
import org.sjhstudio.diary.core.database.EntryEntity
import org.sjhstudio.diary.core.database.MediaEntity
import org.sjhstudio.diary.core.database.asEntity
import org.sjhstudio.diary.core.database.asModel
import org.sjhstudio.diary.core.model.AddPhotosResult
import org.sjhstudio.diary.core.model.Draft
import org.sjhstudio.diary.core.model.DraftKey
import org.sjhstudio.diary.core.model.MediaStatus
import org.sjhstudio.diary.core.model.PhotoLimit
import org.sjhstudio.diary.core.model.PickedImage

/** 저장 조건(FR-002): 글, 사진, 기분 가운데 하나는 있어야 한다. */
fun Draft.canSave(): Boolean = toPlainText(bodyHtml).isNotBlank() || photos.isNotEmpty() || mood != null

class DefaultDraftRepository(
    private val db: DiaryDatabase,
    private val photos: PhotoStore,
    private val clock: Clock = Clock.System,
    /** 한 편에 새로 붙일 수 있는 사진 수. Plus가 생기면 결제 상태로 바꾼다. */
    private val photoLimit: () -> Int = { PhotoLimit.FREE }
) : DraftRepository {
    private val drafts get() = db.draftDao()
    private val entries get() = db.entryDao()
    private val media get() = db.mediaDao()

    override fun draft(key: DraftKey): Flow<Draft?> =
        combine(drafts.observe(key.value), drafts.observeMedia(key.value)) { draft, rows -> draft?.asModel(rows) }

    override suspend fun recentPlaces(): List<String> = db.entryDao().recentPlaces(RECENT_PLACES)

    override suspend fun latestNew(): Draft? = drafts.latestNew()?.let { it.asModel(drafts.media(it.key)) }

    override suspend fun openEdit(entryId: String): Draft? {
        val existing = drafts.get(entryId)
        if (existing != null) return existing.asModel(drafts.media(entryId))
        val entry = entries.get(entryId)?.takeIf { it.deletedAt == null } ?: return null
        val draft =
            DraftEntity(
                key = entryId,
                date = entry.date,
                time = entry.time,
                bodyHtml = entry.bodyHtml,
                mood = entry.mood,
                weather = entry.weather,
                placeName = entry.placeName,
                isFavorite = entry.isFavorite,
                weatherAutoFilled = false,
                updatedAt = clock.now().toEpochMilliseconds()
            )
        drafts.upsert(draft)
        // 찾을 수 없는 사진은 고칠 수 없으니 임시 저장본에 싣지 않고 저장할 때 그대로 둔다.
        val rows =
            media.forEntry(entryId).filter { it.status == MediaStatus.NORMAL.id && it.fileName != null && it.sha256 != null }
                .mapIndexed { index, row ->
                    DraftMediaEntity(entryId, row.fileName!!, row.sha256!!, row.width, row.height, row.byteSize, index)
                }
        if (rows.isNotEmpty()) drafts.insertMedia(rows)
        return draft.asModel(rows)
    }

    override suspend fun save(draft: Draft) = drafts.upsert(draft.asEntity())

    override suspend fun addPhotos(key: DraftKey, picked: List<PickedImage>): AddPhotosResult {
        val limit = photoLimit()
        val fixed = (key as? DraftKey.Edit)?.let { edit -> media.forEntry(edit.entryId).count { it.status != MediaStatus.NORMAL.id } } ?: 0
        val current = drafts.media(key.value)
        val room = (limit - current.size - fixed).coerceAtLeast(0)
        var added = 0
        var failed = 0
        for (image in picked.take(room)) {
            val stored =
                try {
                    photos.store(image.read())
                } catch (_: okio.IOException) {
                    return AddPhotosResult.NoSpace(added)
                }
            if (stored == null) {
                failed++
                continue
            }
            val order = current.size + added
            drafts.insertMedia(
                listOf(DraftMediaEntity(key.value, stored.fileName, stored.sha256, stored.width, stored.height, stored.byteSize, order))
            )
            added++
        }
        return if (picked.size > room) {
            AddPhotosResult.LimitReached(limit, canUpgrade = limit < PhotoLimit.PLUS, added = added)
        } else {
            AddPhotosResult.Added(added, failed)
        }
    }

    override suspend fun removePhoto(key: DraftKey, index: Int) {
        val rows = drafts.media(key.value)
        if (index !in rows.indices) return
        drafts.replaceMedia(key.value, rows.filterIndexed { i, _ -> i != index }.mapIndexed { i, row -> row.copy(sortOrder = i) })
    }

    override suspend fun reorderPhotos(key: DraftKey, order: List<Int>) {
        val rows = drafts.media(key.value)
        require(order.sorted() == rows.indices.toList()) { "순서는 지금 사진을 빠짐없이 나열해야 한다" }
        drafts.replaceMedia(key.value, order.mapIndexed { i, from -> rows[from].copy(sortOrder = i) })
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun commit(key: DraftKey): String {
        val draft = requireNotNull(drafts.get(key.value)) { "임시 저장본이 없다" }
        val rows = drafts.media(key.value)
        val now = clock.now().toEpochMilliseconds()
        val old = (key as? DraftKey.Edit)?.let { entries.get(it.entryId) }
        check(key !is DraftKey.Edit || old != null) { "고치는 일기가 없다" }
        val id = old?.id ?: Uuid.random().toString()
        val entry = draft.toEntry(id, old, now)
        val oldMedia = old?.let { media.forEntry(it.id) }.orEmpty()
        drafts.commit(key.value, entry, mediaRows(id, rows, oldMedia, now), entries, media)
        return id
    }

    private fun DraftEntity.toEntry(id: String, old: EntryEntity?, now: Long): EntryEntity {
        // 고치지 않은 본문은 그대로 둔다. 레거시 원문과 서식 없는 글이 바뀌지 않게 하려고다.
        val body = if (old != null && bodyHtml == old.bodyHtml) bodyHtml else RichTextSanitizer.sanitize(bodyHtml)
        val plain = if (old != null && bodyHtml == old.bodyHtml) old.plainText else toPlainText(body)
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
                isFavorite = isFavorite,
                createdAt = old?.createdAt ?: now,
                updatedAt = old?.updatedAt ?: now,
                deletedAt = old?.deletedAt,
                legacyId = old?.legacyId,
                legacyCreateDate = old?.legacyCreateDate,
                legacyLocation = old?.legacyLocation
            )
        // 보이는 값이 바뀔 때만 고친 시각을 올린다(스펙 003 합치기가 쓴다).
        return if (old == null || entry == old) entry else entry.copy(updatedAt = now)
    }

    /**
     * 같은 사진은 원래 줄(ID, 만든 시각, 옛 경로)을 잇고 순서만 바꾼다.
     * 찾을 수 없는 사진 줄은 사용자가 뺄 수 없으니 그대로 뒤에 남긴다(헌법 I).
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

    override suspend fun discard(key: DraftKey) = drafts.delete(key.value)
}

private const val RECENT_PLACES = 6
