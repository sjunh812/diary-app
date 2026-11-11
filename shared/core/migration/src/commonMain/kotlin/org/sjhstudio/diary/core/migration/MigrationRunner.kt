package org.sjhstudio.diary.core.migration

import androidx.room3.withWriteTransaction
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import org.sjhstudio.diary.core.data.photo.ThumbnailEncoder
import org.sjhstudio.diary.core.database.DiaryDatabase
import org.sjhstudio.diary.core.database.MediaEntity
import org.sjhstudio.diary.core.database.asEntity
import org.sjhstudio.diary.core.database.asModel
import org.sjhstudio.diary.core.model.Media
import org.sjhstudio.diary.core.model.MediaStatus
import org.sjhstudio.diary.core.model.MigratedSetting
import org.sjhstudio.diary.core.model.MigrationFailure
import org.sjhstudio.diary.core.model.MigrationFailureKind
import org.sjhstudio.diary.core.model.MigrationOutcome
import org.sjhstudio.diary.core.model.MigrationReport
import org.sjhstudio.diary.core.model.MigrationState
import org.sjhstudio.diary.core.model.MigrationStep
import org.sjhstudio.diary.core.model.currentDate
import org.sjhstudio.diary.core.model.orElseOnFailure

/**
 * 이전이 읽고 쓰는 자리
 * Android가 `AppFiles`와 `Context`로 채움
 */
data class MigrationPaths(
    val legacy: LegacyFiles,
    val snapshotDir: Path,
    val migrationDir: Path,
    val mediaDir: Path,
    /** 이전이 끝나면 새 DB가 이 이름이 됨(`databases/diary.db`) */
    val databasePath: Path
) {
    val newDatabase: Path get() = migrationDir / "diary-new.db"
    val photosJson: Path get() = snapshotDir / "photos.json"
}

/**
 * 진행 화면이 0.25초마다 읽는 값
 * 일기 내용과 사진은 없음
 */
data class MigrationProgress(
    val step: MigrationStep,
    val entriesDone: Int = 0,
    val entriesTotal: Int = 0,
    val photosDone: Int = 0,
    val photosTotal: Int = 0
)

sealed interface MigrationResult {
    /**
     * 원본이 없음(새로 설치)
     * 상태 파일을 쓰지 않음
     */
    data object NoLegacy : MigrationResult

    /** 이미 `Done`·`Partial` */
    data class AlreadyFinished(val state: MigrationState) : MigrationResult

    /** [showResult]가 false면 옮길 일기가 0편이라 결과 화면 없이 지나감 */
    data class Finished(val report: MigrationReport, val showResult: Boolean) : MigrationResult

    data class Failed(
        val step: MigrationStep,
        val kind: MigrationFailureKind,
        /** 공간이 부족할 때 더 있어야 하는 바이트(필요한 양 - 남은 양) */
        val neededBytes: Long? = null
    ) : MigrationResult
}

/**
 * 사용 통계로 보낼 이전 사건(contracts/migration-flow.md "보내도 되는 값")
 * 내용은 구간과 열거형뿐
 */
interface MigrationEvents {
    fun started(entries: Int, photos: Int, attempt: Int) = Unit

    /** 단계(Snapshotting, Copying, Verifying, Committing)에 들어서 `state.json`에 적은 직후 */
    fun stepStarted(step: MigrationStep) = Unit

    fun finished(report: MigrationReport, durationMillis: Long) = Unit

    fun failed(step: MigrationStep, kind: MigrationFailureKind, attempt: Int) = Unit
}

/**
 * 1.7.x 기록을 새 DB로 옮김(contracts/migration-flow.md, research R5~R8)
 *
 * 원본은 고치거나 지우지 않음
 * 사본을 만들고, 사본을 읽어 임시 DB에 쓰고, 줄마다 지문으로 검증한 뒤에야
 * 임시 DB를 `diary.db`로 이름만 바꿈
 * 어느 단계에서 끊겨도 다음 시작에 처음부터 다시 함
 *
 * 일기 몇 편이 문제여도 멈추지 않음
 * 그 일기만 빼고 나머지를 옮겨 앱을 쓰게 하고, 뺀 일기는 결과에 알린 뒤
 * 앱이 업데이트되면 다시 읽음
 * 사진 파일이 깨졌으면 일기는 두고 그 사진만 찾을 수 없는 사진으로 둠
 * 한 편도 옮기지 못할 때만 실패
 * 실패하면 임시 DB만 지우고 사본과 옮긴 사진은 남김(헌법 I)
 */
class MigrationRunner(
    private val fileSystem: FileSystem,
    private val paths: MigrationPaths,
    private val clock: Clock,
    private val timeZone: TimeZone,
    private val newId: () -> String,
    /** 그 폴더가 있는 저장소의 남은 바이트 */
    private val freeBytes: (Path) -> Long,
    /** 이 경로에 Room DB를 엶(임시 DB는 TRUNCATE 저널) */
    private val openDatabase: (String) -> DiaryDatabase,
    imageSize: ImageSizeReader,
    private val events: MigrationEvents = object : MigrationEvents {},
    /**
     * 지금 앱의 versionCode.
     * 처음 옮긴 버전을 적어 두고, 일부만 옮겨진 기기는 이보다 높은 버전에서만 다시 읽음(FR-011)
     */
    private val versionCode: Int = 0,
    /**
     * 옮긴 사진의 썸네일 생성(research R9, FR-037)
     * 실패해도 이전은 계속하고 수만 셈
     * 앱은 `ThumbnailRepository.ensure`를 넘김
     */
    private val makeThumbnail: (suspend (Path) -> Boolean)? = null,
    /** 큰 사진을 줄이는 인코더. 앱은 새로 붙인 사진과 같은 것을 넘김 */
    shrink: ThumbnailEncoder? = null,
    private val io: CoroutineDispatcher = Dispatchers.IO
) : LegacyMigration {
    private val store = MigrationStateStore(fileSystem, paths.migrationDir)
    private val moved = MovedNumbers(fileSystem, paths.migrationDir)
    private val photoRecords = PhotoRecordFile(fileSystem, paths)
    private val snapshotter = LegacySnapshotter(fileSystem, clock)
    private val importer = MediaImporter(fileSystem, paths.mediaDir, imageSize, clock, newId, shrink)
    private val _progress = MutableStateFlow(MigrationProgress(MigrationStep.NOT_STARTED))
    override val progress: StateFlow<MigrationProgress> = _progress.asStateFlow()

    override suspend fun state(): MigrationState = withContext(io) { store.readState() }

    override suspend fun report(): MigrationReport? = withContext(io) { store.readReport() }

    override suspend fun hasLegacyCopy(): Boolean = withContext(io) { fileSystem.exists(paths.snapshotDir / paths.legacy.noteDb.name) }

    /** 원본이 있고 아직 끝나지 않았는가(관문이 이전 화면을 띄울지 정함) */
    override suspend fun needsMigration(): Boolean = withContext(io) {
        val step = store.readState().step
        step != MigrationStep.DONE && step != MigrationStep.PARTIAL && step != MigrationStep.DEFERRED &&
            paths.legacy.exist(fileSystem)
    }

    override suspend fun keepsPhotoCopies(): Boolean = withContext(io) {
        val step = store.readState().step
        step == MigrationStep.DEFERRED || step == MigrationStep.PARTIAL
    }

    /**
     * 어떤 실패도 앱을 끄지 않게 결과로 바꿈
     * 공간이 꽉 찬 기기는 상태 파일 쓰기부터 실패함
     * 꺼지면 다시 켜도 같은 자리에서 또 꺼짐
     */
    override suspend fun run(): MigrationResult = withContext(io) {
        val previous = store.readState()
        try {
            resume(previous)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            failOrNoSpace(MigrationStep.SNAPSHOTTING, MigrationFailureKind.UNKNOWN, previous.attempt + 1)
        }
    }

    private suspend fun resume(previous: MigrationState): MigrationResult = when {
        previous.step == MigrationStep.DONE || previous.step == MigrationStep.PARTIAL || previous.step == MigrationStep.DEFERRED ->
            MigrationResult.AlreadyFinished(previous)

        !paths.legacy.exist(fileSystem) -> MigrationResult.NoLegacy

        else -> afterCommit(previous) ?: deferIfInUse(previous) ?: migrate(attempt = previous.attempt + 1)
    }

    /**
     * 공간이 차 미뤘다고 적지 못해도 앱은 열림
     * 그사이 쓴 일기가 있으면 새 DB로 덮지 않고 미룬 이전으로 돌림
     * 미룬 이전은 없는 일기만 더함
     */
    private suspend fun deferIfInUse(previous: MigrationState): MigrationResult? {
        if (!hasWrittenEntries()) return null
        val deferred = previous.copy(step = MigrationStep.DEFERRED, retriedVersionCode = versionCode, updatedAt = clock.now())
        store.writeState(deferred)
        return MigrationResult.AlreadyFinished(deferred)
    }

    private suspend fun hasWrittenEntries(): Boolean {
        if (!fileSystem.exists(paths.databasePath)) return false
        val db = openDatabase(paths.databasePath.toString())
        return try {
            db.entryDao().countAll() > 0
        } finally {
            db.close()
        }
    }

    /**
     * 이름을 바꾼 뒤 끊겼으면 지문만 다시 보고 맞으면 끝냄
     * 맞지 않아도 일기가 있으면 지우지 않고 미룬 이전으로 돌림
     * 그 사이 사용자가 쓰거나 고쳤을 수 있음(헌법 I)
     */
    private suspend fun afterCommit(previous: MigrationState): MigrationResult? {
        if (previous.step != MigrationStep.COMMITTING || !fileSystem.exists(paths.databasePath)) return null
        val resumed = orElseOnFailure(null) { finishAlreadyCommitted(previous) }
        if (resumed == null && !orElseOnFailure(false) { hasWrittenEntries() }) deleteDatabase(paths.databasePath)
        return resumed
    }

    private suspend fun migrate(attempt: Int): MigrationResult {
        val startedAt = clock.now()
        var step = MigrationStep.SNAPSHOTTING
        try {
            deleteDatabase(paths.newDatabase)
            // 무엇을 쓰기 전에 공간부터 봄
            // 상태 파일조차 못 쓰는 기기에서도 얼마나 더 필요한지 알림
            val legacyBytes = listOf(paths.legacy.noteDb, paths.legacy.noteDbWal, paths.legacy.noteDbShm).sumOf(::sizeOf)
            val snapshotNeed = legacyBytes + sizeOf(paths.legacy.prefXml) + sizeOf(paths.legacy.pref2Xml) + MARGIN_BYTES
            val freeForSnapshot = freeBytes(paths.migrationDir.parent ?: paths.migrationDir)
            if (freeForSnapshot < snapshotNeed) {
                return fail(step, MigrationFailureKind.NO_SPACE, attempt, snapshotNeed - freeForSnapshot)
            }
            writeStep(step, attempt, startedAt)
            try {
                snapshotter.snapshot(paths.legacy, paths.snapshotDir)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                return failOrNoSpace(step, MigrationFailureKind.SNAPSHOT_IO, attempt)
            }

            val (notes, summary) = readSnapshot() ?: return fail(step, MigrationFailureKind.READ_ERROR, attempt)
            if (notes.isEmpty() && summary.unreadable == 0) {
                val report = emptyReport(clock.now())
                store.writeReport(report)
                writeFinal(MigrationStep.DONE, attempt, startedAt)
                return MigrationResult.Finished(report, showResult = false)
            }

            val fragments = notes.flatMap { PhotoPaths.fragments(it.picture) }
            val photoBytes = fragments.distinct().sumOf { f ->
                PhotoPaths.candidates(f).map { sizeOf(it.toPath()) }.firstOrNull { it > 0 }
                    ?: 0
            }
            val copyNeed = legacyBytes + photoBytes + fragments.size * THUMBNAIL_BYTES + MARGIN_BYTES + cacheGuard(photoBytes)
            val freeForCopy = freeBytes(paths.mediaDir.parent ?: paths.mediaDir)
            if (freeForCopy < copyNeed) {
                return fail(step, MigrationFailureKind.NO_SPACE, attempt, copyNeed - freeForCopy)
            }

            step = MigrationStep.COPYING
            writeStep(step, attempt, startedAt)
            events.started(notes.size, fragments.size, attempt)
            val counts = Counts()
            var leftOut = 0
            fileSystem.createDirectories(paths.migrationDir)
            val db = openDatabase(paths.newDatabase.toString())
            try {
                copy(db, notes, fragments.size, counts)
                // 한 편도 쓰지 못했으면 새 DB 자체가 문제
                check(counts.written > 0) { "한 편도 쓰지 못함" }
                step = MigrationStep.VERIFYING
                writeStep(step, attempt, startedAt)
                leftOut = verify(db, notes.size, counts = counts)
                if (leftOut == notes.size) throw MismatchException()
                if (leftOut + summary.unreadable > 0) rememberMoved(db)
            } finally {
                db.close()
            }

            step = MigrationStep.COMMITTING
            writeStep(step, attempt, startedAt)
            // 닫은 뒤 남는 빈 저널·잠금 파일 정리
            // 옮기는 것은 DB 파일 하나
            for (leftover in listOf(
                "diary-new.db-journal",
                "diary-new.db.lck"
            )) {
                fileSystem.delete(paths.migrationDir / leftover, mustExist = false)
            }
            paths.databasePath.parent?.let(fileSystem::createDirectories)
            fileSystem.atomicMove(paths.newDatabase, paths.databasePath)
            val report = counts.report(notes.size - leftOut, summary.unreadable + leftOut)
            store.writeReport(report)
            writeFinal(if (report.outcome == MigrationOutcome.PARTIAL) MigrationStep.PARTIAL else MigrationStep.DONE, attempt, startedAt)
            events.finished(report, (clock.now() - startedAt).inWholeMilliseconds)
            return MigrationResult.Finished(report, showResult = true)
        } catch (e: CancellationException) {
            throw e
        } catch (e: MismatchException) {
            return fail(step, MigrationFailureKind.MISMATCH, attempt)
        } catch (e: Throwable) {
            return failOrNoSpace(
                step,
                if (step ==
                    MigrationStep.COPYING
                ) {
                    MigrationFailureKind.WRITE_ERROR
                } else {
                    MigrationFailureKind.UNKNOWN
                },
                attempt
            )
        }
    }

    /**
     * 사본을 `_id` 순으로 모두 읽음
     * 열지 못하면 null.
     */
    private fun readSnapshot(): Pair<List<LegacyNote>, LegacyReadSummary>? {
        val path = paths.snapshotDir / paths.legacy.noteDb.name
        if (!fileSystem.exists(path)) return emptyList<LegacyNote>() to LegacyReadSummary(0, 0)
        return runCatching {
            LegacyDbReader.open(path.toString()).use { reader ->
                val notes = mutableListOf<LegacyNote>()
                val summary = reader.readAll { notes += it }
                notes to summary
            }
        }.getOrNull()
    }

    private suspend fun copy(db: DiaryDatabase, notes: List<LegacyNote>, photosTotal: Int, counts: Counts) {
        val mapper = LegacyEntryMapper(currentDate(clock, timeZone), timeZone, newId)
        // 앞선 시도의 목록을 이음. 원본이 사라진 사진은 이 목록으로 사본을 찾고, 다시 읽기가 앞선 사진을 지우지 않음
        val records = runCatching { photoRecords.read() }.getOrDefault(emptyList()).associateByTo(LinkedHashMap()) { it.path }
        var done = 0
        var photosDone = 0
        _progress.value = MigrationProgress(MigrationStep.COPYING, 0, notes.size, 0, photosTotal)
        for (chunk in notes.chunked(CHUNK)) {
            val rows = ArrayList<CopiedRow>(chunk.size)
            val copied = ArrayList<Path>()
            for (note in chunk) {
                // 바꾸지 못하는 일기 한 편은 검증에서 빠진 일기로 셈
                val mapped =
                    try {
                        mapper.map(note)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        continue
                    }
                val photos = importer.import(mapped.entry.id, PhotoPaths.fragments(note.picture), records)
                for (photo in photos) {
                    records[photo.record.path] = photo.record
                    photo.media.fileName?.takeIf { photo.found }?.let { copied += paths.mediaDir / it }
                    photosDone++
                }
                rows += CopiedRow(mapped, photos)
            }
            counts.thumbnailsFailed += makeThumbnails(copied)
            write(db, rows).forEach(counts::add)
            // 묶음마다 적음. 복사 중에 앱이 죽어도 어느 사진을 옮겼는지 남음
            photoRecords.write(records.values.toList())
            done += chunk.size
            _progress.value = MigrationProgress(MigrationStep.COPYING, done, notes.size, photosDone, photosTotal)
        }
    }

    /**
     * 묶음째 쓰고, 실패하면 한 편씩 써서 문제없는 일기는 살림
     * 쓴 줄을 돌려줌
     */
    private suspend fun write(db: DiaryDatabase, rows: List<CopiedRow>): List<CopiedRow> {
        try {
            db.withWriteTransaction { insert(db, rows) }
            return rows
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return rows.filter { row ->
                try {
                    db.withWriteTransaction { insert(db, listOf(row)) }
                    true
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    false
                }
            }
        }
    }

    private suspend fun insert(db: DiaryDatabase, rows: List<CopiedRow>) {
        db.entryDao().insertAll(rows.map { it.mapped.entry.asEntity() })
        db.mediaDao().insertAll(rows.flatMap { row -> row.photos.map { it.media.asEntity() } })
    }

    /**
     * 썸네일을 최대 3개씩 나란히 만듦(contracts/migration-flow.md `Copying`)
     * 못 만든 수를 돌려줌
     */
    private suspend fun makeThumbnails(files: List<Path>): Int {
        val make = makeThumbnail ?: return 0
        val permits = Semaphore(THUMBNAIL_PARALLEL)
        return coroutineScope {
            files.distinct()
                .map { file -> async { permits.withPermit { orElseOnFailure(false) { make(file) } } } }
                .awaitAll()
                .count { !it }
        }
    }

    /**
     * 사본을 다시 읽어 줄마다 원본 지문과 새 DB 지문을 견줌(research R6)
     * 지문이 다른 일기는 새 DB에서 뺌
     * 사본은 그대로라 앱이 업데이트되면 다시 읽음
     * 사진 파일이 없거나 해시가 이름과 다르면 일기는 두고 그 사진만 찾을 수 없는 사진으로 바꿈
     * 빠진 일기 수를 돌려줌
     * [only]가 있으면 그 레거시 번호만 봄(새로 쓴 일기가 있는 다시 읽기)
     */
    private suspend fun verify(
        db: DiaryDatabase,
        expectedRows: Int,
        only: Set<Int>? = null,
        counts: Counts? = null,
        keep: Boolean = false
    ): Int {
        val photoHashes = photoRecords.read().associate { it.path to it.sha256 }
        val (notes, _) = readSnapshot() ?: throw MismatchException()
        if (notes.size != expectedRows) throw MismatchException()
        val mapper = LegacyEntryMapper(currentDate(clock, timeZone), timeZone) { "" }
        val goodFiles = HashSet<String>()
        val brokenFiles = HashSet<String>()
        val brokenPaths = HashSet<String>()
        var leftOut = 0
        _progress.value = _progress.value.copy(step = MigrationStep.VERIFYING)
        for (note in notes) {
            if (only != null && note.id.toInt() !in only) continue
            val entity = db.entryDao().getByLegacyId(note.id.toInt())
            if (entity == null) {
                leftOut++
                continue
            }
            val entry = entity.asModel()
            val media = db.mediaDao().forEntry(entry.id).map { it.asModel() }
            val photos = PhotoPaths.fragments(note.picture).map { Fingerprint.LegacyPhoto(it, photoHashes[it]) }
            // 매핑이 이전한 날 같은 대체 값을 다시 만들어, 날짜·시각은 새 DB 값을 원본 규칙으로 다시 견줌
            val same =
                runCatching {
                    val mapped = mapper.map(note).let { it.copy(entry = it.entry.copy(date = entry.date, time = entry.time)) }
                    Fingerprint.legacyCanonical(note, mapped, photos) == Fingerprint.restoredCanonical(entry, media) &&
                        sameDateRule(note, entry)
                }.getOrDefault(false)
            if (!same) {
                counts?.remove(media)
                if (!keep) db.entryDao().deletePermanently(entry.id)
                leftOut++
                continue
            }
            for (m in media) {
                val name = m.fileName ?: continue
                if (m.status != MediaStatus.NORMAL || name in goodFiles) continue
                if (name !in brokenFiles && importer.verify(name)) {
                    goodFiles += name
                    continue
                }
                brokenFiles += name
                m.legacyPath?.let(brokenPaths::add)
                db.mediaDao().update(
                    m.copy(
                        status = MediaStatus.MISSING,
                        fileName = null,
                        sha256 = null,
                        width = null,
                        height = null,
                        byteSize = null
                    ).asEntity()
                )
                counts?.photoBroke()
            }
        }
        // 끊겨서 다시 검증할 때 지문이 맞도록 고친 사진을 목록에도 적음
        if (brokenPaths.isNotEmpty()) {
            photoRecords.write(photoRecords.read().map { if (it.path in brokenPaths) it.copy(sha256 = null, size = null) else it })
        }
        return leftOut
    }

    /** 원본 날짜·시각을 읽을 수 있었던 줄은 새 DB 값이 그 규칙과 같아야 함 */
    private fun sameDateRule(note: LegacyNote, entry: org.sjhstudio.diary.core.model.Entry): Boolean {
        val created = note.createDate ?: return true
        val date = LegacyEntryMapper.parseDate(created)
        val time = LegacyEntryMapper.parseTime(created)
        return (date == null || date == entry.date) && (time == null || time == entry.time)
    }

    /** `Committing`에서 끊긴 뒤: 이미 이름을 바꾼 `diary.db`를 지문으로 다시 봄 */
    private suspend fun finishAlreadyCommitted(previous: MigrationState): MigrationResult {
        val (notes, summary) = readSnapshot() ?: error("사본을 읽지 못함")
        val db = openDatabase(paths.databasePath.toString())
        val report =
            try {
                // 앱이 이미 열렸던 길이라 다른 일기는 사용자가 고친 글일 수 있음
                val leftOut = verify(db, notes.size, keep = true)
                if (leftOut == notes.size) throw MismatchException()
                if (leftOut + summary.unreadable > 0) rememberMoved(db)
                val records = photoRecords.read()
                Counts(
                    photosFound = records.count { it.sha256 != null },
                    photosMissing = records.count { it.sha256 == null }
                ).report(notes.size - leftOut, summary.unreadable + leftOut)
            } finally {
                db.close()
            }
        store.writeReport(report)
        writeFinal(
            if (report.outcome == MigrationOutcome.PARTIAL) MigrationStep.PARTIAL else MigrationStep.DONE,
            previous.attempt,
            previous.startedAt ?: clock.now()
        )
        return MigrationResult.Finished(report, showResult = true)
    }

    /**
     * `Partial`인 기기에서 앱 업데이트 뒤 처음 한 번만 못 읽은 일기를 다시 읽음(FR-011)
     * 새 DB를 버리지 않고, 아직 `entry`에 없는 레거시 줄만 더함
     * 그 사이 쓴 일기는 그대로
     * 더한 줄 수를 돌려줌
     * 다시 읽을 때가 아니면 null.
     */
    override suspend fun retryPartial(db: DiaryDatabase): Int? = withContext(io) {
        val state = store.readState()
        if (state.step != MigrationStep.PARTIAL || versionCode <= state.retriedVersionCode) return@withContext null
        store.writeState(state.copy(retriedVersionCode = versionCode, updatedAt = clock.now()))
        val (notes, summary) = readSnapshot() ?: return@withContext 0
        // 이미 옮긴 일기는 사용자가 고쳤을 수 있어 글은 견주지 않음
        // 옮긴 뒤 완전히 삭제한 일기는 표에 없어도 못 옮긴 일기가 아님
        val known = moved.read()
        val missing = notYetMoved(db, notes, editsAreOurs = true).filterNot { it.id.toInt() in known }
        val counts = Counts()
        var leftOut = 0
        if (missing.isNotEmpty()) {
            releaseNumbers(db, missing)
            copy(db, missing, missing.sumOf { PhotoPaths.fragments(it.picture).size }, counts)
            // 사용자가 쓰고 고친 일기는 건드리지 않게 방금 더한 줄만 봄
            leftOut = verify(db, notes.size, only = missing.map { it.id.toInt() }.toSet(), counts = counts)
            rememberMoved(db)
        }
        val added = missing.size - leftOut
        val unreadable = summary.unreadable + leftOut
        val old = store.readReport()
        val report =
            (old ?: emptyReport(clock.now())).copy(
                entriesTotal = notes.size + summary.unreadable,
                entriesMigrated = (old?.entriesMigrated ?: 0) + added,
                entriesUnreadable = unreadable,
                photosFound = (old?.photosFound ?: 0) + counts.photosFound,
                photosMissing = (old?.photosMissing ?: 0) + counts.photosMissing,
                dateDiffers = (old?.dateDiffers ?: 0) + counts.dateDiffers,
                dateUnparsed = (old?.dateUnparsed ?: 0) + counts.dateUnparsed,
                thumbnailsFailed = (old?.thumbnailsFailed ?: 0) + counts.thumbnailsFailed,
                finishedAt = clock.now(),
                outcome = if (unreadable > 0) MigrationOutcome.PARTIAL else MigrationOutcome.DONE
            )
        store.writeReport(report)
        if (report.outcome == MigrationOutcome.DONE) {
            store.writeState(store.readState().copy(step = MigrationStep.DONE, updatedAt = clock.now()))
        }
        added
    }

    /**
     * 아직 옮기지 않은 옛 일기를 고름
     * 옛 번호는 기기 안에서만 유일해서 다른 기기에서 합쳐 온 일기와 겹침
     * 번호만 보면 그 일기를 옮긴 것으로 치고 건너뛰어 이 기기의 옛 일기가 영영 안 옮겨짐
     * 옛 작성 시각은 초 단위까지 기기 안에서 유일하니 이 값과 글로 같은 일기를 찾음
     * [editsAreOurs]가 참이면 번호와 작성 시각이 같은 일기는 글이 달라도 이미 옮겨 고친 일기로 봄
     */
    private suspend fun notYetMoved(db: DiaryDatabase, notes: List<LegacyNote>, editsAreOurs: Boolean): List<LegacyNote> {
        val rows = db.entryDao().legacyRows()
        val byNumber = rows.filter { it.legacyId != null }.associateBy { it.legacyId }
        val written = rows.filter { it.legacyCreateDate != null }.mapTo(HashSet()) { it.legacyCreateDate to it.plainText }
        return notes.filterNot { note ->
            val text = note.contents.orEmpty()
            val row = byNumber[note.id.toInt()]?.takeIf { it.legacyCreateDate == note.createDate }
            (note.createDate to text) in written || (row != null && (editsAreOurs || row.plainText == text))
        }
    }

    /**
     * 옮길 일기의 번호를 쥔 일기가 있으면 번호만 비움(`docs/migration.md` "미룬 이전과 합친 일기")
     * 번호는 겹칠 수 없고 검증은 번호로 일기를 찾음
     * 비우지 않으면 넣지 못하고, 검증이 다른 기기 일기를 틀린 일기로 보아 지움
     */
    private suspend fun releaseNumbers(db: DiaryDatabase, notes: List<LegacyNote>) {
        notes.map { it.id.toInt() }.chunked(SQL_ARGS).forEach { db.entryDao().clearLegacyIds(it) }
    }

    override suspend fun defer() = withContext(io) {
        val state = store.readState()
        if (state.step != MigrationStep.FAILED) return@withContext
        store.writeState(state.copy(step = MigrationStep.DEFERRED, retriedVersionCode = versionCode, updatedAt = clock.now()))
    }

    /**
     * 미뤄 둔 이전을 지금 일기 DB에 더함
     * 그 사이 쓴 일기가 있어 임시 DB로 바꾸지 않고 없는 줄만 넣음
     * 실패하면 상태를 그대로 둠
     * 앱은 이미 쓰는 중이라 실패 화면으로 돌아가지 않음
     */
    override suspend fun retryDeferred(db: DiaryDatabase, onlyAfterUpdate: Boolean): MigrationResult = withContext(io) {
        val state = store.readState()
        if (state.step != MigrationStep.DEFERRED || (onlyAfterUpdate && versionCode <= state.retriedVersionCode)) {
            return@withContext MigrationResult.AlreadyFinished(state)
        }
        store.writeState(state.copy(attempt = state.attempt + 1, retriedVersionCode = versionCode, updatedAt = clock.now()))
        val startedAt = clock.now()
        try {
            if (paths.legacy.exist(fileSystem)) snapshotter.snapshot(paths.legacy, paths.snapshotDir)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            return@withContext MigrationResult.Failed(MigrationStep.SNAPSHOTTING, MigrationFailureKind.SNAPSHOT_IO)
        }
        val (notes, summary) =
            readSnapshot() ?: return@withContext MigrationResult.Failed(MigrationStep.SNAPSHOTTING, MigrationFailureKind.READ_ERROR)
        // 미룬 이전은 아직 한 편도 옮기지 않아 같은 시각의 일기는 다른 곳에서 온 것
        // 글까지 같아야 같은 일기
        val missing = notYetMoved(db, notes, editsAreOurs = false)
        val fragments = missing.flatMap { PhotoPaths.fragments(it.picture) }
        val need =
            fragments.distinct().sumOf { f -> PhotoPaths.candidates(f).map { sizeOf(it.toPath()) }.firstOrNull { it > 0 } ?: 0 }.let {
                it + MARGIN_BYTES + cacheGuard(it)
            }
        val free = freeBytes(paths.mediaDir.parent ?: paths.mediaDir)
        if (free < need) return@withContext MigrationResult.Failed(MigrationStep.SNAPSHOTTING, MigrationFailureKind.NO_SPACE, need - free)
        try {
            val counts = Counts()
            var leftOut = 0
            if (missing.isNotEmpty()) {
                releaseNumbers(db, missing)
                copy(db, missing, fragments.size, counts)
                leftOut = verify(db, notes.size, only = missing.map { it.id.toInt() }.toSet(), counts = counts)
                if (leftOut ==
                    missing.size
                ) {
                    return@withContext MigrationResult.Failed(MigrationStep.VERIFYING, MigrationFailureKind.MISMATCH)
                }
            }
            if (leftOut + summary.unreadable > 0) rememberMoved(db)
            val report = counts.report(missing.size - leftOut, summary.unreadable + leftOut)
            store.writeReport(report)
            writeFinal(
                if (report.outcome ==
                    MigrationOutcome.PARTIAL
                ) {
                    MigrationStep.PARTIAL
                } else {
                    MigrationStep.DONE
                },
                state.attempt + 1,
                startedAt
            )
            events.finished(report, (clock.now() - startedAt).inWholeMilliseconds)
            MigrationResult.Finished(report, showResult = false)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            MigrationResult.Failed(MigrationStep.COPYING, MigrationFailureKind.WRITE_ERROR)
        }
    }

    /**
     * 찾을 수 없는 사진 중 원래 경로를 아는 것을 하루 한 번 다시 찾음(FR-011)
     * 있으면 이전과 같은 방법으로 옮기고 `normal`로 바꿈
     * 찾은 장 수를 돌려줌
     * 하루가 안 지났으면 null.
     */
    override suspend fun recheckMissingPhotos(db: DiaryDatabase): Int? = withContext(io) {
        val state = store.readState()
        if (state.step != MigrationStep.DONE && state.step != MigrationStep.PARTIAL) return@withContext null
        val now = clock.now()
        val last = state.photoRecheckAt
        if (last != null && now - last < RECHECK_INTERVAL) return@withContext null
        store.writeState(state.copy(photoRecheckAt = now, updatedAt = now))
        var found = 0
        for (row in db.mediaDao().missingWithLegacyPath()) {
            val photo = importer.importOne(row.entryId, row.sortOrder, row.legacyPath ?: continue)
            if (!photo.found) continue
            db.mediaDao().update(photo.media.copy(id = row.id, createdAt = Instant.fromEpochMilliseconds(row.createdAt)).asEntity())
            photo.media.fileName?.let { makeThumbnails(listOf(paths.mediaDir / it)) }
            found++
        }
        if (found > 0) {
            store.readReport()?.let { r ->
                store.writeReport(
                    r.copy(
                        photosFound = r.photosFound + found,
                        photosMissing = (
                            r.photosMissing -
                                found
                            ).coerceAtLeast(0)
                    )
                )
            }
        }
        found
    }

    override suspend fun cleanUpSupportFiles(): Boolean = withContext(io) {
        SupportFiles(fileSystem, paths.migrationDir, clock).deleteExpired()
    }

    override suspend fun supportSummary(): SupportSummary = withContext(io) {
        val state = store.readState()
        val report = store.readReport()
        val entries =
            report?.entriesTotal
                ?: runCatching {
                    val snapshot = paths.snapshotDir / paths.legacy.noteDb.name
                    if (fileSystem.exists(snapshot)) LegacyDbReader.open(snapshot.toString()).use { it.count()?.toInt() } else null
                }.getOrNull()
                ?: 0
        val photos = report?.let { it.photosFound + it.photosMissing } ?: runCatching { photoRecords.read().size }.getOrDefault(0)
        SupportSummary(
            failure = state.failure,
            attempt = state.attempt,
            entriesBucket = org.sjhstudio.diary.core.platform.AnalyticsEvents.countBucket(entries),
            photosBucket = org.sjhstudio.diary.core.platform.AnalyticsEvents.countBucket(photos),
            unreadableBucket = org.sjhstudio.diary.core.platform.AnalyticsEvents.countBucket(report?.entriesUnreadable ?: 0)
        )
    }

    /**
     * 사본에 있는 옛 설정 파일(비밀번호를 뺀 것)에서 옮긴 설정을 셈
     * 읽지 못하면 비워 둠
     * 이 목록은 결과에 남길 뿐이라 이전을 막지 않음
     */
    private fun migratedSettings(): List<MigratedSetting> = runCatching {
        fun read(name: String): Map<String, Any> {
            val path = paths.snapshotDir / name
            return if (fileSystem.exists(path)) LegacyPrefs.parse(fileSystem.read(path) { readUtf8() }) else emptyMap()
        }
        LegacySettingsMapper.migrated(read(paths.legacy.prefXml.name), read(paths.legacy.pref2Xml.name))
    }.getOrDefault(emptyList())

    private fun Counts.report(migrated: Int, unreadable: Int) = toReport(migrated, unreadable, migratedSettings(), clock.now())

    private fun writeStep(step: MigrationStep, attempt: Int, startedAt: Instant) {
        store.writeState(MigrationState(step = step, attempt = attempt, startedAt = startedAt, updatedAt = clock.now()))
        _progress.value = _progress.value.copy(step = step)
        // 상태를 적은 뒤에 알림
        // 개발 빌드의 강제 종료 훅이 여기서 끊어 다시 시작을 시험함(quickstart §3)
        events.stepStarted(step)
    }

    private fun writeFinal(step: MigrationStep, attempt: Int, startedAt: Instant) {
        // 같은 버전에서는 같은 파일을 같은 방법으로 읽어 결과가 같으므로, 옮긴 버전을 다시 읽은 버전으로 적음
        store.writeState(
            MigrationState(step = step, attempt = attempt, startedAt = startedAt, updatedAt = clock.now(), retriedVersionCode = versionCode)
        )
        _progress.value = _progress.value.copy(step = step)
    }

    private fun fail(step: MigrationStep, kind: MigrationFailureKind, attempt: Int, neededBytes: Long? = null): MigrationResult.Failed {
        // 임시 DB만 지움
        // 사본(legacy-snapshot)과 옮긴 사진(media)은 남김
        runCatching { deleteDatabase(paths.newDatabase) }
        // 공간이 꽉 차면 실패 기록도 못 씀
        // 기록이 없어도 실패는 화면까지 감
        runCatching {
            store.writeState(
                MigrationState(
                    step = MigrationStep.FAILED,
                    attempt = attempt,
                    updatedAt = clock.now(),
                    failure = MigrationFailure(step, kind)
                )
            )
        }
        _progress.value = MigrationProgress(MigrationStep.FAILED)
        events.failed(step, kind, attempt)
        return MigrationResult.Failed(step, kind, neededBytes)
    }

    /**
     * 쓰다가 실패했는데 여유가 바닥났으면 [kind]보다 공간 부족이 까닭
     * 그래야 사용자가 무엇을 하면 되는지 앎
     */
    private fun failOrNoSpace(step: MigrationStep, kind: MigrationFailureKind, attempt: Int): MigrationResult.Failed {
        val free = runCatching { freeBytes(paths.migrationDir.parent ?: paths.migrationDir) }.getOrDefault(Long.MAX_VALUE)
        return if (free <
            MARGIN_BYTES
        ) {
            fail(step, MigrationFailureKind.NO_SPACE, attempt, MARGIN_BYTES - free)
        } else {
            fail(step, kind, attempt)
        }
    }

    private fun deleteDatabase(path: Path) {
        for (suffix in listOf(
            "",
            "-journal",
            "-wal",
            "-shm",
            ".lck"
        )) {
            fileSystem.delete((path.toString() + suffix).toPath(), mustExist = false)
        }
    }

    private fun sizeOf(path: Path): Long = fileSystem.metadataOrNull(path)?.size ?: 0

    /** 옮긴 옛 번호를 남김. 적지 못해도 이전은 끝낼 수 있음 */
    private suspend fun rememberMoved(db: DiaryDatabase) {
        moved.add(db.entryDao().legacyRows().mapNotNull { it.legacyId })
    }

    private class MismatchException : Exception()

    /**
     * 옛 사진은 캐시에 있음
     * 공간이 빠듯해지면 OS가 복사 도중에도 캐시를 지움(docs/migration.md)
     * 복사한 뒤에도 그 선 위에 남을 때만 시작하고, 아니면 사용자가 공간을 비우고 다시 하게 멈춤
     */
    private fun cacheGuard(photoBytes: Long): Long = if (photoBytes > 0) CACHE_GUARD_BYTES else 0

    private companion object {
        const val CACHE_GUARD_BYTES = 1024L * 1024 * 1024

        /** 한 트랜잭션에 쓰는 줄 수(contracts/migration-flow.md Copying) */
        const val CHUNK = 200

        /** SQLite에 한 번에 넘기는 값 수 */
        const val SQL_ARGS = 500

        /**
         * 썸네일을 나란히 만드는 수
         * 큰 사진을 한꺼번에 풀어 메모리가 부족하지 않게 3으로 둠(R9)
         */
        const val THUMBNAIL_PARALLEL = 3

        /** 사진 한 장의 썸네일 자리(research R8) */
        const val THUMBNAIL_BYTES = 40L * 1024

        /** 여유 50MB(research R8) */
        const val MARGIN_BYTES = 50L * 1024 * 1024

        val RECHECK_INTERVAL = 24.hours
    }
}

private fun emptyReport(finishedAt: Instant) = MigrationReport(
    entriesTotal = 0,
    entriesMigrated = 0,
    entriesUnreadable = 0,
    photosFound = 0,
    photosMissing = 0,
    dateDiffers = 0,
    dateUnparsed = 0,
    thumbnailsFailed = 0,
    settingsMigrated = emptyList(),
    finishedAt = finishedAt,
    outcome = MigrationOutcome.DONE
)

private class CopiedRow(val mapped: MappedEntry, val photos: List<ImportedPhoto>)

private class Counts(
    var photosFound: Int = 0,
    var photosMissing: Int = 0,
    var dateDiffers: Int = 0,
    var dateUnparsed: Int = 0,
    var thumbnailsFailed: Int = 0,
    var written: Int = 0
) {
    /** 새 DB에 들어간 일기만 셈 */
    fun add(row: CopiedRow) {
        written++
        if (row.mapped.dateUnparsed) dateUnparsed++
        if (row.mapped.dateDiffers) dateDiffers++
        for (photo in row.photos) if (photo.found) photosFound++ else photosMissing++
    }

    /** 검증에서 뺀 일기의 사진을 뺌 */
    fun remove(media: List<Media>) {
        for (m in media) if (m.status == MediaStatus.NORMAL) photosFound-- else photosMissing--
    }

    fun photoBroke() {
        photosFound--
        photosMissing++
    }

    fun toReport(migrated: Int, unreadable: Int, settings: List<MigratedSetting>, finishedAt: Instant) = MigrationReport(
        entriesTotal = migrated + unreadable,
        entriesMigrated = migrated,
        entriesUnreadable = unreadable,
        photosFound = photosFound,
        photosMissing = photosMissing,
        dateDiffers = dateDiffers,
        dateUnparsed = dateUnparsed,
        thumbnailsFailed = thumbnailsFailed,
        settingsMigrated = settings,
        finishedAt = finishedAt,
        outcome = if (unreadable > 0) MigrationOutcome.PARTIAL else MigrationOutcome.DONE
    )
}
