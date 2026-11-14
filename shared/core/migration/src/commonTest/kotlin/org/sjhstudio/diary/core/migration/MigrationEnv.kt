package org.sjhstudio.diary.core.migration

import androidx.room3.Room
import androidx.room3.RoomDatabase
import kotlinx.datetime.TimeZone
import okio.Path
import org.sjhstudio.diary.core.database.DiaryDatabase
import org.sjhstudio.diary.core.database.DiaryDatabaseConstructor
import org.sjhstudio.diary.core.database.configured
import org.sjhstudio.diary.core.model.MigrationStep
import org.sjhstudio.diary.core.testing.TestClock

/** 이전 시험의 기기 하나. 옛 파일 자리와 새 파일 자리를 가짐 */
internal class MigrationEnv(val dir: TempDir) {
    val clock = TestClock()
    var ids = 0
    var free = Long.MAX_VALUE
    val legacy =
        LegacyFiles(
            noteDb = dir.root / "databases" / "note.db",
            prefXml = dir.root / "shared_prefs" / "pref.xml",
            pref2Xml = dir.root / "shared_prefs" / "pref2.xml"
        )
    val paths =
        MigrationPaths(
            legacy = legacy,
            snapshotDir = dir.root / "files" / "legacy-snapshot",
            migrationDir = dir.root / "files" / "migration",
            mediaDir = dir.root / "files" / "media",
            databasePath = dir.root / "databases" / "diary.db"
        )

    fun open(path: String): DiaryDatabase = Room
        .databaseBuilder(path, DiaryDatabaseConstructor::initialize)
        .configured()
        .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
        .build()

    fun runner(
        versionCode: Int = 37,
        makeThumbnail: (suspend (Path) -> Boolean)? = null,
        openDb: (String) -> DiaryDatabase = ::open,
        events: MigrationEvents = object : MigrationEvents {}
    ) = MigrationRunner(
        fileSystem = dir.fs,
        paths = paths,
        clock = clock,
        timeZone = TimeZone.of("Asia/Seoul"),
        newId = { "id-${ids++}" },
        freeBytes = { free },
        openDatabase = openDb,
        imageSize = { 100 to 50 },
        versionCode = versionCode,
        makeThumbnail = makeThumbnail,
        events = events
    )

    /**
     * 검증에 들어설 때 [action] 실행
     * 복사와 검증 사이에 무언가 어긋난 상황을 만듦
     */
    fun onVerifying(action: () -> Unit) = object : MigrationEvents {
        override fun stepStarted(step: MigrationStep) {
            if (step == MigrationStep.VERIFYING) action()
        }
    }

    /** 사진 목록의 해시를 바꿔 그 사진이 든 일기의 지문을 어긋나게 함 */
    fun breakFingerprints() {
        val list = paths.photosJson
        val text = dir.fs.read(list) { readUtf8() }
        dir.fs.write(list) { writeUtf8(text.replace(Regex("\"sha256\":\"[0-9a-f]{64}\""), "\"sha256\":\"${"0".repeat(64)}\"")) }
    }

    /** [photo]의 해시만 목록에서 바꿔 그 사진이 든 일기 하나의 지문을 어긋나게 함 */
    fun breakFingerprintOf(photo: Path) {
        val text = dir.fs.read(paths.photosJson) { readUtf8() }
        dir.fs.write(paths.photosJson) { writeUtf8(text.replace(sha(photo), "0".repeat(64))) }
    }

    fun restoreFingerprintOf(photo: Path) {
        val text = dir.fs.read(paths.photosJson) { readUtf8() }
        dir.fs.write(paths.photosJson) { writeUtf8(text.replace("0".repeat(64), sha(photo))) }
    }

    fun photo(name: String, bytes: ByteArray): Path = dir.write("cache/$name", bytes)

    fun sha(path: Path) = LegacySnapshotter(dir.fs, clock).sha256(path)

    /** 검증 단계에서 끊긴 이전. 사진 사본과 목록은 남고 일기는 아직 안 옮겨짐 */
    fun failAtVerifying() = object : MigrationEvents {
        override fun stepStarted(step: MigrationStep) {
            check(step != MigrationStep.VERIFYING) { "끊김" }
        }
    }
}
