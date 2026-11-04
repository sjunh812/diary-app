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
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.builtins.ListSerializer
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
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

/** 이전이 읽고 쓰는 자리. Android가 `AppFiles`와 `Context`로 채운다. */
data class MigrationPaths(
    val legacy: LegacyFiles,
    val snapshotDir: Path,
    val migrationDir: Path,
    val mediaDir: Path,
    /** 이전이 끝나면 새 DB가 이 이름이 된다(`databases/diary.db`). */
    val databasePath: Path
) {
    val newDatabase: Path get() = migrationDir / "diary-new.db"
    val photosJson: Path get() = snapshotDir / "photos.json"
}

/** 진행 화면이 0.25초마다 읽는 값. 일기 내용과 사진은 없다. */
data class MigrationProgress(
    val step: MigrationStep,
    val entriesDone: Int = 0,
    val entriesTotal: Int = 0,
    val photosDone: Int = 0,
    val photosTotal: Int = 0
)

sealed interface MigrationResult {
    /** 원본이 없다(새로 설치). 상태 파일을 쓰지 않는다. */
    data object NoLegacy : MigrationResult

    /** 이미 `Done`·`Partial`이다. */
    data class AlreadyFinished(val state: MigrationState) : MigrationResult

    /** [showResult]가 false면 옮길 일기가 0편이라 결과 화면 없이 지나간다. */
    data class Finished(val report: MigrationReport, val showResult: Boolean) : MigrationResult

    data class Failed(
        val step: MigrationStep,
        val kind: MigrationFailureKind,
        /** 공간이 모자랄 때 더 있어야 하는 바이트(필요한 양 - 남은 양). */
        val neededBytes: Long? = null
    ) : MigrationResult
}

/** 사용 통계로 보낼 이전 사건(contracts/migration-flow.md "보내도 되는 값"). 내용은 구간과 열거형뿐이다. */
interface MigrationEvents {
    fun started(entries: Int, photos: Int, attempt: Int) = Unit

    /** 단계(Snapshotting, Copying, Verifying, Committing)에 들어서 `state.json`에 적은 직후. */
    fun stepStarted(step: MigrationStep) = Unit

    fun finished(report: MigrationReport, durationMillis: Long) = Unit

    fun failed(step: MigrationStep, kind: MigrationFailureKind, attempt: Int) = Unit
}

/**
 * 1.7.x 기록을 새 DB로 옮긴다(contracts/migration-flow.md, research R5~R8).
 *
 * 원본은 고치거나 지우지 않는다. 사본을 만들고, 사본을 읽어 임시 DB에 쓰고, 줄마다 지문으로 검증한 뒤에야
 * 임시 DB를 `diary.db`로 이름만 바꾼다. 어느 단계에서 끊겨도 다음 시작에 처음부터 다시 한다.
 *
 * 일기 몇 편이 문제여도 멈추지 않는다. 그 일기만 빼고 나머지를 옮겨 앱을 쓰게 하고, 뺀 일기는 결과에 알린 뒤
 * 앱이 업데이트되면 다시 읽는다(2026-11-04 사용자 결정). 사진 파일이 깨졌으면 일기는 두고 사진만 찾을 수 없는 사진이 된다.
 * 한 편도 옮기지 못할 때만 실패다. 실패하면 임시 DB만 지우고 사본과 옮긴 사진은 남긴다(헌법 I).
 */
class MigrationRunner(
    private val fileSystem: FileSystem,
    private val paths: MigrationPaths,
    private val clock: Clock,
    private val timeZone: TimeZone,
    private val newId: () -> String,
    /** 그 폴더가 있는 저장소의 남은 바이트. */
    private val freeBytes: (Path) -> Long,
    /** 이 경로에 Room DB를 연다(임시 DB는 TRUNCATE 저널). */
    private val openDatabase: (String) -> DiaryDatabase,
    imageSize: ImageSizeReader,
    private val events: MigrationEvents = object : MigrationEvents {},
    /**
     * 지금 앱의 versionCode.
     * 처음 옮긴 버전을 적어 두고, 일부만 옮겨진 기기는 이보다 높은 버전에서만 다시 읽는다(FR-011).
     */
    private val versionCode: Int = 0,
    /**
     * 옮긴 사진의 썸네일을 만든다(research R9, FR-037). 실패해도 이전은 계속하고 수만 센다.
     * 앱은 `ThumbnailRepository.ensure`를 넘긴다.
     */
    private val makeThumbnail: (suspend (Path) -> Boolean)? = null,
    private val io: CoroutineDispatcher = Dispatchers.IO
) : LegacyMigration {
    private val store = MigrationStateStore(fileSystem, paths.migrationDir)
    private val snapshotter = LegacySnapshotter(fileSystem, clock)
    private val importer = MediaImporter(fileSystem, paths.mediaDir, imageSize, clock, newId)
    private val _progress = MutableStateFlow(MigrationProgress(MigrationStep.NOT_STARTED))
    override val progress: StateFlow<MigrationProgress> = _progress.asStateFlow()

    override fun state(): MigrationState = store.readState()

    override fun report(): MigrationReport? = store.readReport()

    /** 원본이 있고 아직 끝나지 않았는가(관문이 이전 화면을 띄울지 정한다). */
    override fun needsMigration(): Boolean {
        val step = store.readState().step
        return step != MigrationStep.DONE && step != MigrationStep.PARTIAL && step != MigrationStep.DEFERRED &&
            paths.legacy.exist(fileSystem)
    }

    override suspend fun run(): MigrationResult = withContext(io) {
        val previous = store.readState()
        if (previous.step == MigrationStep.DONE || previous.step == MigrationStep.PARTIAL || previous.step == MigrationStep.DEFERRED) {
            return@withContext MigrationResult.AlreadyFinished(previous)
        }
        if (!paths.legacy.exist(fileSystem)) return@withContext MigrationResult.NoLegacy
        if (previous.step == MigrationStep.COMMITTING && fileSystem.exists(paths.databasePath)) {
            // 이름을 바꾼 뒤 끊겼다. 지문만 다시 보고 맞으면 끝낸다.
            val resumed = runCatching { finishAlreadyCommitted(previous) }.getOrNull()
            if (resumed != null) return@withContext resumed
            deleteDatabase(paths.databasePath)
        }
        migrate(attempt = previous.attempt + 1)
    }

    private suspend fun migrate(attempt: Int): MigrationResult {
        deleteDatabase(paths.newDatabase)
        val startedAt = clock.now()
        var step = MigrationStep.SNAPSHOTTING
        try {
            writeStep(step, attempt, startedAt)
            val legacyBytes = listOf(paths.legacy.noteDb, paths.legacy.noteDbWal, paths.legacy.noteDbShm).sumOf(::sizeOf)
            val snapshotNeed = legacyBytes + sizeOf(paths.legacy.prefXml) + sizeOf(paths.legacy.pref2Xml) + MARGIN_BYTES
            val freeForSnapshot = freeBytes(paths.migrationDir.parent ?: paths.migrationDir)
            if (freeForSnapshot < snapshotNeed) {
                return fail(step, MigrationFailureKind.NO_SPACE, attempt, snapshotNeed - freeForSnapshot)
            }
            try {
                snapshotter.snapshot(paths.legacy, paths.snapshotDir)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                return fail(step, MigrationFailureKind.SNAPSHOT_IO, attempt)
            }

            val (notes, summary) = readSnapshot() ?: return fail(step, MigrationFailureKind.READ_ERROR, attempt)
            if (notes.isEmpty() && summary.unreadable == 0) {
                val report = emptyReport()
                store.writeReport(report)
                writeFinal(MigrationStep.DONE, attempt, startedAt)
                return MigrationResult.Finished(report, showResult = false)
            }

            val fragments = notes.flatMap { PhotoPaths.fragments(it.picture) }
            val photoBytes = fragments.distinct().sumOf { f ->
                PhotoPaths.candidates(f).map { sizeOf(it.toPath()) }.firstOrNull { it > 0 }
                    ?: 0
            }
            val copyNeed = legacyBytes + photoBytes + fragments.size * THUMBNAIL_BYTES + MARGIN_BYTES
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
                // 한 편도 쓰지 못했으면 새 DB 자체가 문제다.
                check(counts.written > 0) { "한 편도 쓰지 못함" }
                step = MigrationStep.VERIFYING
                writeStep(step, attempt, startedAt)
                leftOut = verify(db, notes.size, counts = counts)
                if (leftOut == notes.size) throw MismatchException()
            } finally {
                db.close()
            }

            step = MigrationStep.COMMITTING
            writeStep(step, attempt, startedAt)
            // 닫은 뒤 남는 빈 저널·잠금 파일을 치운다. 옮기는 것은 DB 파일 하나다.
            for (leftover in listOf(
                "diary-new.db-journal",
                "diary-new.db.lck"
            )) {
                fileSystem.delete(paths.migrationDir / leftover, mustExist = false)
            }
            paths.databasePath.parent?.let(fileSystem::createDirectories)
            fileSystem.atomicMove(paths.newDatabase, paths.databasePath)
            val report = counts.toReport(notes.size - leftOut, summary.unreadable + leftOut)
            store.writeReport(report)
            writeFinal(if (report.outcome == MigrationOutcome.PARTIAL) MigrationStep.PARTIAL else MigrationStep.DONE, attempt, startedAt)
            events.finished(report, (clock.now() - startedAt).inWholeMilliseconds)
            return MigrationResult.Finished(report, showResult = true)
        } catch (e: CancellationException) {
            throw e
        } catch (e: MismatchException) {
            return fail(step, MigrationFailureKind.MISMATCH, attempt)
        } catch (e: Throwable) {
            return fail(
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

    /** 사본을 `_id` 순으로 모두 읽는다. 열지 못하면 null. */
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
        val mapper = LegacyEntryMapper(clock.now().toLocalDateTime(timeZone).date, timeZone, newId)
        val records = mutableListOf<PhotoRecord>()
        var done = 0
        var photosDone = 0
        _progress.value = MigrationProgress(MigrationStep.COPYING, 0, notes.size, 0, photosTotal)
        for (chunk in notes.chunked(CHUNK)) {
            val rows = ArrayList<CopiedRow>(chunk.size)
            val copied = ArrayList<Path>()
            for (note in chunk) {
                // 바꾸지 못하는 일기 한 편은 검증에서 빠진 일기로 센다.
                val mapped =
                    try {
                        mapper.map(note)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        continue
                    }
                val photos = importer.import(mapped.entry.id, PhotoPaths.fragments(note.picture))
                for (photo in photos) {
                    records += photo.record
                    photo.media.fileName?.takeIf { photo.found }?.let { copied += paths.mediaDir / it }
                    photosDone++
                }
                rows += CopiedRow(mapped, photos)
            }
            counts.thumbnailsFailed += makeThumbnails(copied)
            write(db, rows).forEach(counts::add)
            done += chunk.size
            _progress.value = MigrationProgress(MigrationStep.COPYING, done, notes.size, photosDone, photosTotal)
        }
        writePhotosJson(records)
    }

    /** 묶음째 쓰고, 실패하면 한 편씩 써서 문제없는 일기는 살린다. 쓴 줄을 돌려준다. */
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

    /** 썸네일을 최대 3개씩 나란히 만든다(contracts/migration-flow.md `Copying`). 못 만든 수를 돌려준다. */
    private suspend fun makeThumbnails(files: List<Path>): Int {
        val make = makeThumbnail ?: return 0
        val permits = Semaphore(THUMBNAIL_PARALLEL)
        return coroutineScope {
            files.distinct()
                .map { file -> async { permits.withPermit { runCatching { make(file) }.getOrDefault(false) } } }
                .awaitAll()
                .count { !it }
        }
    }

    /**
     * 사본을 다시 읽어 줄마다 원본 지문과 새 DB 지문을 견준다(research R6).
     * 지문이 다른 일기는 새 DB에서 뺀다. 사본은 그대로라 앱이 업데이트되면 다시 읽는다.
     * 사진 파일이 없거나 해시가 이름과 다르면 일기는 두고 그 사진만 찾을 수 없는 사진으로 바꾼다.
     * 빠진 일기 수를 돌려준다. [only]가 있으면 그 레거시 번호만 본다(새로 쓴 일기가 있는 다시 읽기).
     */
    private suspend fun verify(db: DiaryDatabase, expectedRows: Int, only: Set<Int>? = null, counts: Counts? = null): Int {
        val photoHashes = readPhotosJson().associate { it.path to it.sha256 }
        val (notes, _) = readSnapshot() ?: throw MismatchException()
        if (notes.size != expectedRows) throw MismatchException()
        val mapper = LegacyEntryMapper(clock.now().toLocalDateTime(timeZone).date, timeZone) { "" }
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
            // 매핑은 이전한 날 같은 대체 값을 다시 만들기 때문에 날짜·시각은 새 DB 값을 원본 규칙으로 다시 견준다.
            val same =
                runCatching {
                    val mapped = mapper.map(note).let { it.copy(entry = it.entry.copy(date = entry.date, time = entry.time)) }
                    Fingerprint.legacyCanonical(note, mapped, photos) == Fingerprint.restoredCanonical(entry, media) &&
                        sameDateRule(note, entry)
                }.getOrDefault(false)
            if (!same) {
                counts?.remove(media)
                db.entryDao().deletePermanently(entry.id)
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
        // 끊겨서 다시 검증할 때 지문이 맞도록 고친 사진을 목록에도 적는다.
        if (brokenPaths.isNotEmpty()) {
            writePhotosJson(readPhotosJson().map { if (it.path in brokenPaths) it.copy(sha256 = null, size = null) else it })
        }
        return leftOut
    }

    /** 원본 날짜·시각을 읽을 수 있었던 줄은 새 DB 값이 그 규칙과 같아야 한다. */
    private fun sameDateRule(note: LegacyNote, entry: org.sjhstudio.diary.core.model.Entry): Boolean {
        val created = note.createDate ?: return true
        val date = LegacyEntryMapper.parseDate(created)
        val time = LegacyEntryMapper.parseTime(created)
        return (date == null || date == entry.date) && (time == null || time == entry.time)
    }

    /** `Committing`에서 끊긴 뒤: 이미 이름을 바꾼 `diary.db`를 지문으로 다시 본다. */
    private suspend fun finishAlreadyCommitted(previous: MigrationState): MigrationResult {
        val (notes, summary) = readSnapshot() ?: error("사본을 읽지 못함")
        val db = openDatabase(paths.databasePath.toString())
        val report =
            try {
                val leftOut = verify(db, notes.size)
                if (leftOut == notes.size) throw MismatchException()
                val records = readPhotosJson()
                Counts(
                    photosFound = records.count { it.sha256 != null },
                    photosMissing = records.count { it.sha256 == null }
                ).toReport(notes.size - leftOut, summary.unreadable + leftOut)
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
     * `Partial`인 기기에서 앱이 업데이트된 뒤 처음 한 번만 못 읽은 일기를 다시 읽는다(FR-011).
     * 새 DB를 버리지 않고, 아직 `entry`에 없는 레거시 줄만 더한다. 그 사이 쓴 일기는 그대로다.
     * 더한 줄 수를 돌려준다. 다시 읽을 때가 아니면 null.
     */
    override suspend fun retryPartial(db: DiaryDatabase): Int? = withContext(io) {
        val state = store.readState()
        if (state.step != MigrationStep.PARTIAL || versionCode <= state.retriedVersionCode) return@withContext null
        store.writeState(state.copy(retriedVersionCode = versionCode, updatedAt = clock.now()))
        val (notes, summary) = readSnapshot() ?: return@withContext 0
        val known = db.entryDao().legacyIds().toHashSet()
        val missing = notes.filter { it.id.toInt() !in known }
        val counts = Counts()
        var leftOut = 0
        if (missing.isNotEmpty()) {
            copy(db, missing, missing.sumOf { PhotoPaths.fragments(it.picture).size }, counts)
            // 사용자가 쓰고 고친 일기는 건드리지 않게 방금 더한 줄만 본다.
            leftOut = verify(db, notes.size, only = missing.map { it.id.toInt() }.toSet(), counts = counts)
        }
        val added = missing.size - leftOut
        val unreadable = summary.unreadable + leftOut
        val old = store.readReport()
        val report =
            (old ?: emptyReport()).copy(
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

    override fun defer() {
        val state = store.readState()
        if (state.step != MigrationStep.FAILED) return
        store.writeState(state.copy(step = MigrationStep.DEFERRED, retriedVersionCode = versionCode, updatedAt = clock.now()))
    }

    /**
     * 미뤄 둔 이전을 지금 일기 DB에 더한다. 그 사이 쓴 일기가 있어 임시 DB로 바꾸지 않고 없는 줄만 넣는다.
     * 실패하면 상태를 그대로 둔다. 앱은 이미 쓰는 중이라 실패 화면으로 돌아가지 않는다.
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
        val known = db.entryDao().legacyIds().toHashSet()
        val missing = notes.filter { it.id.toInt() !in known }
        val fragments = missing.flatMap { PhotoPaths.fragments(it.picture) }
        val need =
            fragments.distinct().sumOf { f -> PhotoPaths.candidates(f).map { sizeOf(it.toPath()) }.firstOrNull { it > 0 } ?: 0 } +
                MARGIN_BYTES
        val free = freeBytes(paths.mediaDir.parent ?: paths.mediaDir)
        if (free < need) return@withContext MigrationResult.Failed(MigrationStep.SNAPSHOTTING, MigrationFailureKind.NO_SPACE, need - free)
        try {
            val counts = Counts()
            var leftOut = 0
            if (missing.isNotEmpty()) {
                copy(db, missing, fragments.size, counts)
                leftOut = verify(db, notes.size, only = missing.map { it.id.toInt() }.toSet(), counts = counts)
                if (leftOut ==
                    missing.size
                ) {
                    return@withContext MigrationResult.Failed(MigrationStep.VERIFYING, MigrationFailureKind.MISMATCH)
                }
            }
            val report = counts.toReport(missing.size - leftOut, summary.unreadable + leftOut)
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
     * 찾을 수 없는 사진 가운데 원래 경로를 아는 것을 하루 한 번 다시 찾는다(FR-011).
     * 있으면 이전과 같은 방법으로 옮기고 `normal`로 바꾼다. 찾은 장 수를 돌려준다. 하루가 안 지났으면 null.
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
        val photos = report?.let { it.photosFound + it.photosMissing } ?: runCatching { readPhotosJson().size }.getOrDefault(0)
        SupportSummary(
            failure = state.failure,
            attempt = state.attempt,
            entriesBucket = org.sjhstudio.diary.core.platform.AnalyticsEvents.countBucket(entries),
            photosBucket = org.sjhstudio.diary.core.platform.AnalyticsEvents.countBucket(photos),
            unreadableBucket = org.sjhstudio.diary.core.platform.AnalyticsEvents.countBucket(report?.entriesUnreadable ?: 0)
        )
    }

    /**
     * 사본에 있는 옛 설정 파일(비밀번호를 뺀 것)에서 옮긴 설정을 센다.
     * 읽지 못하면 비워 둔다. 이 목록은 결과에 남길 뿐이라 이전을 막지 않는다.
     */
    private fun migratedSettings(): List<MigratedSetting> = runCatching {
        fun read(name: String): Map<String, Any> {
            val path = paths.snapshotDir / name
            return if (fileSystem.exists(path)) LegacyPrefs.parse(fileSystem.read(path) { readUtf8() }) else emptyMap()
        }
        LegacySettingsMapper.migrated(read(paths.legacy.prefXml.name), read(paths.legacy.pref2Xml.name))
    }.getOrDefault(emptyList())

    private fun emptyReport() = MigrationReport(
        entriesTotal = 0,
        entriesMigrated = 0,
        entriesUnreadable = 0,
        photosFound = 0,
        photosMissing = 0,
        dateDiffers = 0,
        dateUnparsed = 0,
        thumbnailsFailed = 0,
        settingsMigrated = emptyList(),
        finishedAt = clock.now(),
        outcome = MigrationOutcome.DONE
    )

    private inner class Counts(
        var photosFound: Int = 0,
        var photosMissing: Int = 0,
        var dateDiffers: Int = 0,
        var dateUnparsed: Int = 0,
        var thumbnailsFailed: Int = 0,
        var written: Int = 0
    ) {
        /** 새 DB에 들어간 일기만 센다. */
        fun add(row: CopiedRow) {
            written++
            if (row.mapped.dateUnparsed) dateUnparsed++
            if (row.mapped.dateDiffers) dateDiffers++
            for (photo in row.photos) if (photo.found) photosFound++ else photosMissing++
        }

        /** 검증에서 뺀 일기의 사진을 뺀다. */
        fun remove(media: List<Media>) {
            for (m in media) if (m.status == MediaStatus.NORMAL) photosFound-- else photosMissing--
        }

        fun photoBroke() {
            photosFound--
            photosMissing++
        }

        fun toReport(migrated: Int, unreadable: Int) = MigrationReport(
            entriesTotal = migrated + unreadable,
            entriesMigrated = migrated,
            entriesUnreadable = unreadable,
            photosFound = photosFound,
            photosMissing = photosMissing,
            dateDiffers = dateDiffers,
            dateUnparsed = dateUnparsed,
            thumbnailsFailed = thumbnailsFailed,
            settingsMigrated = migratedSettings(),
            finishedAt = clock.now(),
            outcome = if (unreadable > 0) MigrationOutcome.PARTIAL else MigrationOutcome.DONE
        )
    }

    private fun writeStep(step: MigrationStep, attempt: Int, startedAt: Instant) {
        store.writeState(MigrationState(step = step, attempt = attempt, startedAt = startedAt, updatedAt = clock.now()))
        _progress.value = _progress.value.copy(step = step)
        // 상태를 적은 뒤에 알린다. 개발 빌드의 강제 종료 훅은 여기서 끊어 다시 시작을 시험한다(quickstart §3).
        events.stepStarted(step)
    }

    private fun writeFinal(step: MigrationStep, attempt: Int, startedAt: Instant) {
        // 같은 버전에서는 같은 파일을 같은 방법으로 읽어 결과가 같으므로, 옮긴 버전을 다시 읽은 버전으로 적는다.
        store.writeState(
            MigrationState(step = step, attempt = attempt, startedAt = startedAt, updatedAt = clock.now(), retriedVersionCode = versionCode)
        )
        _progress.value = _progress.value.copy(step = step)
    }

    private fun fail(step: MigrationStep, kind: MigrationFailureKind, attempt: Int, neededBytes: Long? = null): MigrationResult.Failed {
        // 임시 DB만 지운다. 사본(legacy-snapshot)과 옮긴 사진(media)은 남긴다.
        runCatching { deleteDatabase(paths.newDatabase) }
        store.writeState(
            MigrationState(step = MigrationStep.FAILED, attempt = attempt, updatedAt = clock.now(), failure = MigrationFailure(step, kind))
        )
        _progress.value = MigrationProgress(MigrationStep.FAILED)
        events.failed(step, kind, attempt)
        return MigrationResult.Failed(step, kind, neededBytes)
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

    private fun writePhotosJson(records: List<PhotoRecord>) {
        fileSystem.createDirectories(paths.snapshotDir)
        val temp = paths.snapshotDir / "photos.json.tmp"
        fileSystem.write(temp) { writeUtf8(LegacySnapshotter.JSON.encodeToString(ListSerializer(PhotoRecord.serializer()), records)) }
        fileSystem.atomicMove(temp, paths.photosJson)
    }

    private fun readPhotosJson(): List<PhotoRecord> = if (!fileSystem.exists(paths.photosJson)) {
        emptyList()
    } else {
        LegacySnapshotter.JSON.decodeFromString(
            ListSerializer(PhotoRecord.serializer()),
            fileSystem.read(paths.photosJson) {
                readUtf8()
            }
        )
    }

    private class MismatchException : Exception()

    private class CopiedRow(val mapped: MappedEntry, val photos: List<ImportedPhoto>)

    private companion object {
        /** 한 트랜잭션에 쓰는 줄 수(contracts/migration-flow.md Copying). */
        const val CHUNK = 200

        /** 썸네일을 나란히 만드는 수. 큰 사진을 한꺼번에 풀어 메모리가 모자라지 않게 3으로 둔다(R9). */
        const val THUMBNAIL_PARALLEL = 3

        /** 사진 한 장의 썸네일 자리(research R8). */
        const val THUMBNAIL_BYTES = 40L * 1024

        /** 여유 50MB(research R8). */
        const val MARGIN_BYTES = 50L * 1024 * 1024

        val RECHECK_INTERVAL = 24.hours
    }
}
