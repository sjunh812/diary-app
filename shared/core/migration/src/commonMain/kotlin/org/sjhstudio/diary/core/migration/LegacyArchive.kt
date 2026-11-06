package org.sjhstudio.diary.core.migration

import kotlin.time.Clock
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.serialization.builtins.ListSerializer
import okio.FileSystem
import okio.Path.Companion.toPath
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.core.model.Weather
import org.sjhstudio.diary.core.model.currentDate

/**
 * 예전 일기 보기의 한 줄.
 * 사진은 앱 저장소 파일 경로이고 찾을 수 없는 사진은 null이다.
 */
data class LegacyEntryView(
    val id: Long,
    /**
     * 다음 쪽을 읽을 때 쓰는 원래 줄.
     * 화면은 쓰지 않는다.
     */
    val source: LegacyNote,
    val date: LocalDate,
    val time: LocalTime,
    val text: String,
    val mood: Mood?,
    val weather: Weather?,
    val placeName: String?,
    val favorite: Boolean,
    val photos: List<String?>
)

/**
 * 이전에 실패한 기기에서 옛 일기를 읽기 전용으로 보여 준다(FR-009, contracts/migration-flow.md "예전 일기 보기").
 * 원본이 아니라 사본(`legacy-snapshot/note.db`)을 연다.
 * 고치기·지우기는 없다.
 * 일기 날짜 최근 순이다.
 */
interface LegacyArchive {
    /**
     * [after] 다음부터 [limit]편.
     * 처음은 null.
     */
    suspend fun page(after: LegacyEntryView?, limit: Int = PAGE): List<LegacyEntryView>

    companion object {
        /** 한 번에 60편(contracts/migration-flow.md). */
        const val PAGE = 60
    }
}

/** 옛 앱이 없는 플랫폼(iOS). */
object EmptyLegacyArchive : LegacyArchive {
    override suspend fun page(after: LegacyEntryView?, limit: Int): List<LegacyEntryView> = emptyList()
}

class SnapshotLegacyArchive(
    private val fileSystem: FileSystem,
    private val paths: MigrationPaths,
    private val clock: Clock,
    private val timeZone: TimeZone,
    private val io: CoroutineDispatcher = Dispatchers.IO
) : LegacyArchive {
    override suspend fun page(after: LegacyEntryView?, limit: Int): List<LegacyEntryView> = withContext(io) {
        val snapshot = paths.snapshotDir / paths.legacy.noteDb.name
        if (!fileSystem.exists(snapshot)) return@withContext emptyList()
        val notes = LegacyDbReader.open(snapshot.toString()).use { it.readNewestFirst(after?.source, limit) }
        val hashes = photoHashes()
        val files = mediaFiles()
        val mapper = LegacyEntryMapper(currentDate(clock, timeZone), timeZone) { "" }
        notes.map { note ->
            val entry = mapper.map(note).entry
            LegacyEntryView(
                id = note.id,
                source = note,
                date = entry.date,
                time = entry.time,
                text = entry.plainText,
                mood = entry.mood,
                weather = entry.weather,
                placeName = entry.placeName?.trim(),
                favorite = entry.isFavorite,
                photos = PhotoPaths.fragments(note.picture).map { fragment -> photoPath(fragment, hashes, files) }
            )
        }
    }

    /** 옮겨 둔 사본이 있으면 그것, 없으면 원래 자리에 아직 있는 파일(읽기만 한다). */
    private fun photoPath(fragment: String, hashes: Map<String, String?>, files: Map<String, String>): String? {
        hashes[fragment]?.let { sha -> files[sha]?.let { return (paths.mediaDir / it).toString() } }
        return PhotoPaths.candidates(fragment).firstOrNull { fileSystem.metadataOrNull(it.toPath())?.isRegularFile == true }
    }

    private fun photoHashes(): Map<String, String?> = runCatching {
        if (!fileSystem.exists(paths.photosJson)) return emptyMap()
        LegacySnapshotter.JSON
            .decodeFromString(ListSerializer(PhotoRecord.serializer()), fileSystem.read(paths.photosJson) { readUtf8() })
            .associate { it.path to it.sha256 }
    }.getOrDefault(emptyMap())

    private fun mediaFiles(): Map<String, String> =
        runCatching { fileSystem.list(paths.mediaDir).associate { it.name.substringBefore('.') to it.name } }.getOrDefault(emptyMap())
}
