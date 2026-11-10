package org.sjhstudio.diary.core.migration

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import okio.FileSystem
import okio.Path

/**
 * 시험용 옛 앱 `note.db` 생성(스펙 001 T031, docs/migration.md "시험")
 * 표 정의는 레거시 `NoteDatabase.java`의 SQL 그대로
 * 저장소에 파일로 두지 않고 시험마다 만들어, Android 호스트와 iOS 시뮬레이터에서 같은 자료로 돎
 */
object LegacyFixtures {
    const val CREATE_V2 =
        "CREATE TABLE IF NOT EXISTS Note (" +
            "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "weather INTEGER DEFAULT -1, " +
            "address TEXT DEFAULT '', " +
            "location_x TEXT DEFAULT '', " +
            "location_y TEXT DEFAULT '', " +
            "contents TEXT DEFAULT '', " +
            "mood INTEGER DEFAULT -1, " +
            "picture TEXT DEFAULT '', " +
            "create_date TIMESTAMP DEFAULT (datetime('now','localtime')), " +
            "modify_date TIMESTAMP DEFAULT (datetime('now','localtime')), " +
            "year INTEGER DEFAULT 1900, " +
            "month INTEGER DEFAULT 1," +
            "star INTEGER DEFAULT 0" +
            ");"

    /** 즐겨찾기가 생기기 전(버전 1) 표 */
    val CREATE_V1 = CREATE_V2.replace(",star INTEGER DEFAULT 0", "")

    const val CREATE_INDEX = "CREATE UNIQUE INDEX IF NOT EXISTS Note_Index ON Note(create_date);"

    /** 버전 1에서 2로 올릴 때 옛 앱이 쓴 SQL. 옛 줄의 `star`는 NULL */
    const val UPGRADE_V1_TO_V2 = "ALTER TABLE Note ADD COLUMN star INTAGER;"

    data class Row(
        val createDate: String,
        val contents: String = "오늘",
        val mood: Any? = -1,
        val weather: Any? = -1,
        val address: String = "",
        val picture: String = "",
        val modifyDate: String = createDate,
        val locationX: String = "",
        val locationY: String = "",
        val star: Int? = 0
    )

    fun open(path: Path): SQLiteConnection = BundledSQLiteDriver().open(path.toString())

    fun create(path: Path, rows: List<Row>, version: Int = 2, wal: Boolean = false): SQLiteConnection {
        FileSystem.SYSTEM.createDirectories(path.parent!!)
        val c = open(path)
        if (wal) {
            c.execSQL("PRAGMA journal_mode = WAL")
            c.execSQL("PRAGMA wal_autocheckpoint = 0")
        }
        c.execSQL(if (version == 1) CREATE_V1 else CREATE_V2)
        c.execSQL(CREATE_INDEX)
        insert(c, rows, withStar = version == 2)
        return c
    }

    fun insert(c: SQLiteConnection, rows: List<Row>, withStar: Boolean = true) {
        c.execSQL("BEGIN")
        val sql =
            "INSERT INTO Note(create_date, modify_date, contents, mood, weather, address, picture, location_x, location_y" +
                (if (withStar) ", star" else "") + ") VALUES(?,?,?,?,?,?,?,?,?" + (if (withStar) ",?" else "") + ")"
        c.prepare(sql).use { s ->
            for (r in rows) {
                s.reset()
                s.clearBindings()
                s.bindText(1, r.createDate)
                s.bindText(2, r.modifyDate)
                s.bindText(3, r.contents)
                bindAny(s, 4, r.mood)
                bindAny(s, 5, r.weather)
                s.bindText(6, r.address)
                s.bindText(7, r.picture)
                s.bindText(8, r.locationX)
                s.bindText(9, r.locationY)
                if (withStar) bindAny(s, 10, r.star)
                s.step()
            }
        }
        c.execSQL("COMMIT")
    }

    private fun bindAny(s: androidx.sqlite.SQLiteStatement, index: Int, value: Any?) = when (value) {
        null -> s.bindNull(index)
        is Int -> s.bindLong(index, value.toLong())
        is Long -> s.bindLong(index, value)
        else -> s.bindText(index, value.toString())
    }

    /**
     * `count`줄
     * 날짜는 하루씩, 글 길이는 [contentLength]
     */
    fun manyRows(count: Int, contentLength: Int = 20): List<Row> = (0 until count).map { i ->
        val day = i % 28 + 1
        val month = i / 28 % 12 + 1
        val year = 2000 + i / (28 * 12)
        val date = "$year-${month.toString().padStart(
            2,
            '0'
        )}-${day.toString().padStart(2, '0')} 21:${(i % 60).toString().padStart(2, '0')}:00"
        Row(createDate = date, contents = "일기 $i " + "가".repeat(contentLength), mood = i % 9, weather = i % 7)
    }

    /**
     * 가운데 페이지 몇 장을 망가뜨림
     * 첫 페이지(머리)는 그대로 둠
     */
    fun corruptMiddlePages(path: Path, pages: Int = 3) {
        val fs = FileSystem.SYSTEM
        val bytes = fs.read(path) { readByteArray() }
        val pageSize = ((bytes[16].toInt() and 0xFF) shl 8) or (bytes[17].toInt() and 0xFF)
        val total = bytes.size / pageSize
        val start = total / 2
        for (p in start until (start + pages).coerceAtMost(total)) {
            for (i in 0 until pageSize) bytes[p * pageSize + i] = (i * 7 + 13).toByte()
        }
        fs.write(path) { write(bytes) }
    }
}
