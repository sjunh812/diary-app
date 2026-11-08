package org.sjhstudio.diary.core.testing

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import org.sjhstudio.diary.core.data.diary.DraftRepository
import org.sjhstudio.diary.core.data.diary.OpenedEdit
import org.sjhstudio.diary.core.model.AddPhotosResult
import org.sjhstudio.diary.core.model.Draft
import org.sjhstudio.diary.core.model.DraftKey
import org.sjhstudio.diary.core.model.DraftPhoto
import org.sjhstudio.diary.core.model.PickedImage

/**
 * 쓰기 화면 시험용 임시 저장소.
 * 메모리에만 둔다.
 */
class FakeDraftRepository(
    initial: List<Draft> = emptyList(),
    /** 수정 화면으로 열면 돌려줄 임시 저장본. */
    private val editable: Map<String, Draft> = emptyMap()
) : DraftRepository {
    private val drafts = MutableStateFlow(initial.associateBy { it.key })

    /** [commit]이 실패하는 상황을 만든다. */
    var failCommit = false

    /** 임시 저장본 쓰기가 실패하는 상황을 만든다(저장 공간이 찼을 때처럼). */
    var failWrites = false

    /** 쓰기 화면을 열 때 읽기가 실패하는 상황을 만든다(DB를 읽지 못할 때처럼). */
    var failReads = false

    /**
     * 다음 [addPhotos]가 돌려줄 값.
     * 없으면 받은 수만큼 더한다.
     */
    var nextAddResult: AddPhotosResult? = null

    /** 있으면 사진을 더하기 전에 기다린다. 사진을 넣는 중인 상황을 만든다. */
    var addGate: CompletableDeferred<Unit>? = null

    val saved: List<Draft> get() = drafts.value.values.toList()
    val committed = mutableListOf<DraftKey>()
    val discarded = mutableListOf<DraftKey>()

    override fun draft(key: DraftKey): Flow<Draft?> = drafts.map { it[key] }

    var places: List<String> = emptyList()

    /** 복원이 바꾼 일기 ID. */
    val changedByRestore = mutableSetOf<String>()

    override suspend fun recentPlaces(): List<String> {
        check(!failReads) { "읽을 수 없음" }
        return places
    }

    override suspend fun latestNew(): Draft? {
        check(!failReads) { "읽을 수 없음" }
        return drafts.value.values.filter { it.key is DraftKey.New }.maxByOrNull { it.updatedAt }
    }

    override suspend fun openEdit(entryId: String): OpenedEdit? {
        check(!failReads) { "읽을 수 없음" }
        val key = DraftKey.Edit(entryId)
        drafts.value[key]?.let { return OpenedEdit(it, resumed = true, entryChanged = entryId in changedByRestore) }
        val draft = editable[entryId] ?: return null
        drafts.update { it + (key to draft) }
        return OpenedEdit(draft, resumed = false)
    }

    override suspend fun matchesEntry(key: DraftKey.Edit): Boolean {
        val draft = drafts.value[key]
        val entry = editable[key.entryId]
        return draft == null ||
            draft.copy(updatedAt = entry?.updatedAt ?: draft.updatedAt, placeName = draft.placeName?.trim()?.ifEmpty { null }) == entry
    }

    override suspend fun save(draft: Draft) {
        check(!failWrites) { "쓰기 실패" }
        drafts.update { all -> all + (draft.key to draft.copy(photos = all[draft.key]?.photos ?: draft.photos)) }
    }

    override suspend fun addPhotos(key: DraftKey, picked: List<PickedImage>): AddPhotosResult {
        addGate?.await()
        check(!failWrites) { "쓰기 실패" }
        nextAddResult?.let { return it }
        drafts.update { all ->
            val draft = all[key] ?: return@update all
            val added = picked.indices.map { DraftPhoto("p${draft.photos.size + it}.jpg", "p${draft.photos.size + it}", 10, 10, 1) }
            all + (key to draft.copy(photos = draft.photos + added))
        }
        return AddPhotosResult.Added(picked.size)
    }

    override suspend fun removePhoto(key: DraftKey, index: Int) {
        check(!failWrites) { "쓰기 실패" }
        drafts.update { all ->
            val draft = all[key] ?: return@update all
            all + (key to draft.copy(photos = draft.photos.filterIndexed { i, _ -> i != index }))
        }
    }

    override suspend fun reorderPhotos(key: DraftKey, order: List<Int>) {
        check(!failWrites) { "쓰기 실패" }
        drafts.update { all ->
            val draft = all[key] ?: return@update all
            all + (key to draft.copy(photos = order.map { draft.photos[it] }))
        }
    }

    override suspend fun commit(key: DraftKey): String {
        check(!failCommit) { "저장 실패" }
        drafts.update { it - key }
        committed += key
        return (key as? DraftKey.Edit)?.entryId ?: "saved-id"
    }

    override suspend fun discard(key: DraftKey) {
        check(!failWrites) { "쓰기 실패" }
        drafts.update { it - key }
        discarded += key
    }
}
