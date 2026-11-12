package org.sjhstudio.diary.core.migration

import androidx.sqlite.SQLITE_DATA_FLOAT
import androidx.sqlite.SQLITE_DATA_INTEGER
import androidx.sqlite.SQLITE_DATA_NULL
import androidx.sqlite.SQLITE_DATA_TEXT
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL

/** 읽은 줄 수와 깨져서 읽지 못한 줄 수(추정) */
data class LegacyReadSummary(val read: Int, val unreadable: Int)

/**
 * `legacy-snapshot/note.db`(사본)를 번들 드라이버의 날 SQL로 읽음(research R5)
 * 레거시 스키마의 `TIMESTAMP` 열 때문에 Room으로는 못 엶
 * 원본이 아닌 사본만 엶
 * 사본의 WAL은 사본에 합쳐져도 괜찮음
 */
class LegacyDbReader private constructor(private val connection: SQLiteConnection) : AutoCloseable {
    // 스키마를 못 읽으면 예외를 그대로 올림. 삼키면 깨진 파일이 "표 없음"으로 보여 옮길 일기가 없다고 끝남
    private val columns: Set<String> by lazy {
        connection.prepare("PRAGMA table_info(Note)").use { s -> buildSet { while (s.step()) add(s.getText(1)) } }
    }

    /**
     * `Note` 표가 있는가
     * 없으면 옮길 일기가 없음
     * 파일이 깨져 스키마를 못 읽으면 예외
     */
    val hasNoteTable: Boolean get() = "_id" in columns

    fun integrityOk(): Boolean =
        runCatching { connection.prepare("PRAGMA integrity_check").use { it.step() && it.getText(0) == "ok" } }.getOrDefault(false)

    /**
     * 줄 수
     * 표가 없으면 0, 깨져서 셀 수 없으면 null.
     */
    fun count(): Long? = when (runCatching { hasNoteTable }.getOrNull()) {
        null -> null

        false -> 0

        true -> runCatching {
            connection.prepare("SELECT COUNT(*) FROM Note").use {
                it.step()
                it.getLong(0)
            }
        }.getOrNull()
    }

    /**
     * `_id` 순으로 [onRow]에 넘김
     * 한 번에 [PAGE]줄씩 읽고, 깨진 범위를 만나면 반씩 나눠 읽히는 줄만 건짐
     * 못 읽은 범위의 `_id`는 있는지조차 모를 수 있어 "못 읽은 줄"은 추정
     */
    fun readAll(onRow: (LegacyNote) -> Unit): LegacyReadSummary {
        if (!hasNoteTable) return LegacyReadSummary(0, 0)
        var read = 0
        var unreadable = 0
        var lastId = Long.MIN_VALUE
        val maxId = lastIdOrNull()
        while (true) {
            val page = runCatching { query("WHERE _id > ? ORDER BY _id LIMIT $PAGE", lastId) }
            val rows = page.getOrNull()
            if (rows != null) {
                if (rows.isEmpty()) break
                rows.forEach(onRow)
                read += rows.size
                lastId = rows.last().id
                continue
            }
            // 이 쪽은 깨짐
            // 다음 읽히는 줄까지 반씩 나눠 건짐
            // 끝을 모르면 몇 줄이 빠졌는지 셀 수 없음. 다 읽었다고 하지 않게 한 줄로 셈
            if (maxId == null) {
                unreadable += 1
                break
            }
            val end: Long = maxId
            val start = if (lastId == Long.MIN_VALUE) minIdOrOne() else lastId + 1
            if (start > end) break
            val salvaged = salvage(start, end, onRow)
            read += salvaged.read
            unreadable += salvaged.unreadable
            break
        }
        return LegacyReadSummary(read, unreadable)
    }

    /**
     * 예전 일기 보기(contracts/migration-flow.md 실패 화면): 일기 날짜 최근 순(`create_date` 역순, 같으면 `_id` 역순)으로 [limit]줄
     * 사용자가 지난 날짜를 골라 쓸 수 있어 `_id`가 아닌 날짜로 줄을 세움
     * [after]로 준 줄 다음부터
     * 깨진 사본이면 읽히는 줄만 모아 같은 차례로
     */
    fun readNewestFirst(after: LegacyNote?, limit: Int): List<LegacyNote> {
        if (runCatching { hasNoteTable }.getOrDefault(false).not()) return emptyList()
        return runCatching {
            if (after == null) {
                queryText("ORDER BY ifnull(create_date, '') DESC, _id DESC LIMIT $limit")
            } else {
                queryText(
                    "WHERE ifnull(create_date, '') < ? OR (ifnull(create_date, '') = ? AND _id < ?) " +
                        "ORDER BY ifnull(create_date, '') DESC, _id DESC LIMIT $limit",
                    after.createDate.orEmpty(),
                    after.createDate.orEmpty(),
                    after.id
                )
            }
        }.getOrElse {
            val all = mutableListOf<LegacyNote>()
            readAll { all += it }
            val sorted = all.sortedWith(compareByDescending<LegacyNote> { it.createDate.orEmpty() }.thenByDescending { it.id })
            val start = if (after == null) 0 else sorted.indexOfFirst { it.id == after.id } + 1
            sorted.drop(start).take(limit)
        }
    }

    private fun queryText(clause: String, vararg args: Any): List<LegacyNote> {
        val sql = "SELECT ${SELECT.joinToString { if (it in columns) it else "NULL" }} FROM Note $clause"
        return connection.prepare(sql).use { s ->
            args.forEachIndexed { i, v -> if (v is Long) s.bindLong(i + 1, v) else s.bindText(i + 1, v.toString()) }
            buildList { while (s.step()) add(s.toNote()) }
        }
    }

    private fun singleLongOrNull(sql: String, vararg args: Long): Long? = connection.prepare(sql).use { s ->
        args.forEachIndexed { i, v -> s.bindLong(i + 1, v) }
        s.step()
        if (s.isNull(0)) null else s.getLong(0)
    }

    /** 마지막 `_id`. 표 끝이 깨져 못 구하면 번호표(`sqlite_sequence`)에서 찾음 */
    private fun lastIdOrNull(): Long? = runCatching { singleLongOrNull("SELECT MAX(_id) FROM Note") }.getOrNull()
        ?: runCatching { singleLongOrNull("SELECT seq FROM sqlite_sequence WHERE name = 'Note'") }.getOrNull()

    private fun minIdOrOne(): Long = runCatching {
        connection.prepare("SELECT MIN(_id) FROM Note").use {
            it.step()
            it.getLong(0)
        }
    }.getOrDefault(1L)

    private fun salvage(lo: Long, hi: Long, onRow: (LegacyNote) -> Unit): LegacyReadSummary {
        val rows = runCatching { query("WHERE _id BETWEEN ? AND ? ORDER BY _id", lo, hi) }.getOrNull()
        if (rows != null) {
            rows.forEach(onRow)
            return LegacyReadSummary(rows.size, 0)
        }
        if (lo == hi) {
            // 줄 수를 셀 수 있고 없다고 나오면 못 읽은 줄이 아님
            val exists = runCatching { singleLongOrNull("SELECT COUNT(*) FROM Note WHERE _id = ?", lo) }.getOrNull()
            return LegacyReadSummary(0, if (exists == 0L) 0 else 1)
        }
        val mid = lo + (hi - lo) / 2
        val left = salvage(lo, mid, onRow)
        val right = salvage(mid + 1, hi, onRow)
        return LegacyReadSummary(left.read + right.read, left.unreadable + right.unreadable)
    }

    private fun query(where: String, vararg args: Long): List<LegacyNote> {
        val sql = "SELECT ${SELECT.joinToString { if (it in columns) it else "NULL" }} FROM Note $where"
        return connection.prepare(sql).use { s ->
            args.forEachIndexed { i, v -> s.bindLong(i + 1, v) }
            buildList { while (s.step()) add(s.toNote()) }
        }
    }

    private fun SQLiteStatement.toNote() = LegacyNote(
        id = getLong(0),
        createDate = textOrNull(1),
        modifyDate = textOrNull(2),
        contents = textOrNull(3),
        mood = longOrNull(4),
        weather = longOrNull(5),
        address = textOrNull(6),
        star = longOrNull(7),
        locationX = textOrNull(8),
        locationY = textOrNull(9),
        picture = textOrNull(10)
    )

    private fun SQLiteStatement.textOrNull(i: Int): String? = if (getColumnType(i) == SQLITE_DATA_NULL) null else getText(i)

    /** 정수 열에 글이 들어 있어도 숫자로 읽히는 것만 숫자로 봄(SQLite는 열 타입을 강제하지 않음) */
    private fun SQLiteStatement.longOrNull(i: Int): Long? = when (getColumnType(i)) {
        SQLITE_DATA_INTEGER -> getLong(i)
        SQLITE_DATA_FLOAT -> getDouble(i).toLong()
        SQLITE_DATA_TEXT -> getText(i).trim().toLongOrNull()
        else -> null
    }

    override fun close() = connection.close()

    companion object {
        private const val PAGE = 200
        private val SELECT =
            listOf(
                "_id", "create_date", "modify_date", "contents", "mood", "weather",
                "address", "star", "location_x", "location_y", "picture"
            )

        /**
         * 사본을 엶
         * 사본의 WAL을 읽으려면 쓰기 모드로 엶(원본은 절대 열지 않음)
         */
        fun open(snapshotDbPath: String): LegacyDbReader {
            val connection = BundledSQLiteDriver().open(snapshotDbPath)
            // 깨진 파일에서 쓸데없이 기다리지 않음
            runCatching { connection.execSQL("PRAGMA busy_timeout = 1000") }
            return LegacyDbReader(connection)
        }
    }
}
