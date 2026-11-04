package org.sjhstudio.diary.debug

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import androidx.paging.map
import java.io.File
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import org.koin.core.context.GlobalContext
import org.sjhstudio.diary.core.database.DatabaseFactory
import org.sjhstudio.diary.core.database.DiaryDatabase
import org.sjhstudio.diary.core.database.EntryEntity
import org.sjhstudio.diary.core.database.MediaEntity
import org.sjhstudio.diary.core.designsystem.component.DiaryEntryRow
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.ui.DiaryPhotoCell
import org.sjhstudio.diary.core.ui.ProvideDiaryImageLoader

// 목록 성능 견본(스펙 001 T083, SC-010·SC-011). 스펙 002 홈이 같은 방식을 쓴다.
// 일기 1만 편·사진 5천 장을 따로 둔 견본 DB(`no_backup/perf-sample/`)에 한 번 만들고, 실제 일기 DB는 건드리지 않는다.
// 60편씩 읽고(Paging 3), 줄 키는 일기 ID, 줄 모델은 불변이고, 사진 칸은 썸네일 크기 JPEG만 읽는다.

private const val ENTRIES = 10_000
private const val PHOTOS = 5_000
private const val PAGE = 60

/** 견본 DB와 사진 폴더. */
private class Sample(val db: DiaryDatabase, val photoDir: File)

/** 목록 한 줄. 바뀌지 않는 값만 들어 있어 Compose가 같은 줄을 다시 그리지 않는다. */
@Immutable
private data class SampleRow(val id: String, val weekday: String, val day: String, val preview: String, val photos: List<String?>)

@Composable
internal fun ListPerformanceSample() {
    val context = LocalContext.current
    var sample by remember { mutableStateOf<Sample?>(null) }
    var made by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { sample = withContext(Dispatchers.IO) { openSample(context) { made = it } } }
    // 견본 사진은 이미 썸네일 크기라 그 파일을 그대로 읽는다.
    ProvideDiaryImageLoader { it }
    val colors = DiaryTheme.colors
    Box(Modifier.fillMaxSize().background(colors.canvas).systemBarsPadding()) {
        val opened = sample
        if (opened == null) {
            BasicText(
                "$made / $ENTRIES",
                Modifier.align(Alignment.Center),
                style = DiaryTheme.typography.body.copy(color = colors.textSecondary)
            )
        } else {
            SampleList(opened)
        }
    }
}

@Composable
private fun SampleList(sample: Sample) {
    val db = sample.db
    val rows =
        remember(sample) {
            Pager(PagingConfig(pageSize = PAGE, initialLoadSize = PAGE, prefetchDistance = PAGE / 2)) { db.entryDao().visibleEntries() }
                .flow
                .map { page ->
                    page.map { entry ->
                        val media = db.mediaDao().forEntry(entry.id)
                        val date = LocalDate.parse(entry.date)
                        SampleRow(
                            id = entry.id,
                            weekday = WEEKDAYS[date.dayOfWeek.ordinal],
                            day = date.day.toString(),
                            preview = entry.plainText,
                            photos = media.map { m -> m.fileName?.let { sample.photoDir.resolve(it).path } }
                        )
                    }
                }
        }.collectAsLazyPagingItems()
    LazyColumn(Modifier.fillMaxSize().testTag("perf-list")) {
        items(count = rows.itemCount, key = rows.itemKey { it.id }) { index ->
            val row = rows[index] ?: return@items
            DiaryEntryRow(
                weekday = row.weekday,
                day = row.day,
                meta = emptyList(),
                preview = row.preview,
                dividerAbove = index > 0,
                photo = if (row.photos.isEmpty()) null else ({ DiaryPhotoCell(row.photos) })
            )
        }
    }
}

// 견본 자료라 한국어 요일만 둔다. 실제 홈은 플랫폼 날짜 형식을 쓴다.
private val WEEKDAYS = listOf("월", "화", "수", "목", "금", "토", "일")

/** 견본 DB를 열고, 비었으면 일기 1만 편과 사진 5천 장(480px JPEG 80)을 만든다. */
private suspend fun openSample(context: Context, progress: (Int) -> Unit): Sample {
    val dir = context.noBackupFilesDir.resolve("perf-sample").apply { mkdirs() }
    val photos = dir.resolve("photos").apply { mkdirs() }
    val db = GlobalContext.get().get<DatabaseFactory>().createAt(dir.resolve("sample.db").path)
    if (db.entryDao().countAll() >= ENTRIES) return Sample(db, photos)
    val random = Random(7)
    val today = LocalDate(2026, 11, 4)
    val now = System.currentTimeMillis()
    val chunk = 500
    for (start in 0 until ENTRIES step chunk) {
        val entries = (start until start + chunk).map { i ->
            val date = today.minus(DatePeriod(days = i))
            EntryEntity(
                id = "sample-$i",
                date = date.toString(),
                time = "%02d:%02d".format(random.nextInt(24), random.nextInt(60)),
                bodyHtml = "",
                plainText = SENTENCES.shuffled(random).take(2 + random.nextInt(4)).joinToString(" "),
                mood = null,
                weather = null,
                placeName = null,
                isFavorite = false,
                createdAt = now - i * 86_400_000L,
                updatedAt = now - i * 86_400_000L,
                deletedAt = null,
                legacyId = null,
                legacyCreateDate = null,
                legacyLocation = null
            )
        }
        db.entryDao().insertAll(entries)
        // 짝수 번째 일기마다 사진 한 장. 사진마다 다른 파일이라 메모리 캐시에 기대지 않는다.
        val media = entries.filterIndexed { k, _ -> (start + k) % 2 == 0 }.map { entry ->
            val n = entry.id.removePrefix("sample-").toInt() / 2
            val name = "p$n.jpg"
            val file = photos.resolve(name)
            if (!file.exists()) writePhoto(file, random)
            MediaEntity(
                id = "m-$n",
                entryId = entry.id,
                sortOrder = 0,
                status = "normal",
                fileName = name,
                sha256 = null,
                width = 480,
                height = 360,
                byteSize = file.length(),
                legacyPath = null,
                createdAt = now
            )
        }
        db.mediaDao().insertAll(media)
        progress(start + chunk)
    }
    check(db.mediaDao().count() == PHOTOS) { "견본 사진 수가 맞지 않음" }
    return Sample(db, photos)
}

private fun writePhoto(file: File, random: Random) {
    val bitmap = Bitmap.createBitmap(480, 360, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint().apply {
        shader = LinearGradient(
            0f,
            0f,
            480f,
            360f,
            Color.HSVToColor(floatArrayOf(random.nextFloat() * 360f, 0.35f, 0.95f)),
            Color.HSVToColor(floatArrayOf(random.nextFloat() * 360f, 0.5f, 0.7f)),
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawRect(0f, 0f, 480f, 360f, paint)
    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 80, it) }
    bitmap.recycle()
}

private val SENTENCES =
    listOf(
        "퇴근길에 한강까지 걸었다.",
        "바람이 차가워졌는데 그래서 더 좋았다.",
        "아침 회의가 길었다.",
        "커피 두 잔째인데도 눈이 감긴다.",
        "편의점에서 따뜻한 두유를 하나 샀다.",
        "오랜만에 친구에게 전화를 걸었다.",
        "비가 와서 우산을 들고 나갔다.",
        "저녁은 김치찌개였다.",
        "책을 스무 쪽 읽고 잠들었다.",
        "내일은 조금 일찍 일어나야지."
    )
