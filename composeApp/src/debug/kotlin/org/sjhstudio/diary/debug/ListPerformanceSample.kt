package org.sjhstudio.diary.debug

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import java.io.File
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import okio.FileSystem
import okio.Path.Companion.toPath
import org.koin.core.context.GlobalContext
import org.sjhstudio.diary.core.data.diary.DefaultEntryRepository
import org.sjhstudio.diary.core.database.DatabaseFactory
import org.sjhstudio.diary.core.database.DiaryDatabase
import org.sjhstudio.diary.core.database.EntryEntity
import org.sjhstudio.diary.core.database.MediaEntity
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.Checklist
import org.sjhstudio.diary.core.model.MediaStatus
import org.sjhstudio.diary.core.model.PreviewLength
import org.sjhstudio.diary.core.model.PreviewMode
import org.sjhstudio.diary.core.model.richtext.RichTextSanitizer
import org.sjhstudio.diary.core.model.richtext.toPlainText
import org.sjhstudio.diary.core.platform.sha256Hex
import org.sjhstudio.diary.core.ui.DiaryEntryLine
import org.sjhstudio.diary.core.ui.ProvideDiaryImageLoader

// 목록 성능 견본(스펙 001 T083, SC-010·SC-011, 스펙 008 SC-003)
// 홈과 같은 저장소(`DefaultEntryRepository.timeline`)와 일기 줄로 그리고, 위에서 미리보기 방식을 바꿈
// 일기의 20%는 에디터 서식을 모두 쓰고(굵게·밑줄·글자색·형광펜·체크리스트·목록), 5%는 5천 자 넘는 긴 일기
// 일기 1만 편·사진 5천 장을 따로 둔 견본 DB(`no_backup/perf-sample-v3/`)에 한 번 만들고, 실제 일기 DB는 건드리지 않음
// 60편씩 읽고(Paging 3), 줄 키는 일기 ID, 줄 모델은 불변, 사진 칸은 썸네일 크기 JPEG만 읽음

private const val ENTRIES = 10_000
private const val PHOTOS = 5_000
private const val PAGE = 60

/** 견본 DB와 사진 폴더 */
private class Sample(val db: DiaryDatabase, val photoDir: File)

@Composable
internal fun ListPerformanceSample() {
    val context = LocalContext.current
    var sample by remember { mutableStateOf<Sample?>(null) }
    var made by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { sample = withContext(Dispatchers.IO) { openSample(context) { made = it } } }
    // 견본 사진은 이미 썸네일 크기라 그 파일을 그대로 읽음
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
    var mode by remember { mutableStateOf(MODES.first()) }
    val modes = remember { MutableStateFlow(mode) }
    val rows =
        remember(sample) {
            DefaultEntryRepository(sample.db, sample.photoDir.path.toPath()).timeline(false, modes.map { it.second })
        }.collectAsLazyPagingItems()
    val colors = DiaryTheme.colors
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for ((name, value) in MODES) {
                BasicText(
                    name,
                    Modifier
                        .background(if (mode.second == value) colors.selected else colors.sunken, RoundedCornerShape(8.dp))
                        .clickable {
                            mode = name to value
                            modes.value = name to value
                        }.padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("mode-$name"),
                    style = DiaryTheme.typography.caption.copy(color = colors.textPrimary)
                )
            }
        }
        LazyColumn(Modifier.fillMaxSize().testTag("perf-list")) {
            items(count = rows.itemCount, key = rows.itemKey { it.id }) { index ->
                val row = rows[index] ?: return@items
                DiaryEntryLine(row, showDate = true, dividerAbove = index > 0)
            }
        }
    }
}

/** 3줄 / 전체 / 전체+서식 */
private val MODES =
    listOf(
        "3줄" to PreviewMode(),
        "전체" to PreviewMode(PreviewLength.FULL),
        "전체+서식" to PreviewMode(PreviewLength.FULL, formatting = true)
    )

// 견본 자료라 한국어 요일만 둠
// 실제 홈은 플랫폼 날짜 형식을 씀
private val WEEKDAYS = listOf("월", "화", "수", "목", "금", "토", "일")

/** 견본 DB를 열고, 비었으면 일기 1만 편과 사진 5천 장(480px JPEG 80)을 만듦 */
private suspend fun openSample(context: Context, progress: (Int) -> Unit): Sample {
    val dir = context.noBackupFilesDir.resolve("perf-sample-v3").apply { mkdirs() }
    val photos = dir.resolve("photos").apply { mkdirs() }
    val db = GlobalContext.get().get<DatabaseFactory>().createAt(dir.resolve("sample.db").path)
    if (db.entryDao().countAll() < ENTRIES) writeSample(db, photos, progress)
    return Sample(db, photos)
}

/**
 * 일기 1만 편과 사진 5천 장을 [db]와 [photos]에 넣음
 * 개발자 모드가 실제 일기 DB에도 넣어 백업 파일로 실기기 측정에 옮김
 */
internal suspend fun writeSample(db: DiaryDatabase, photos: File, progress: (Int) -> Unit = {}) {
    val random = Random(7)
    val today = LocalDate(2026, 11, 4)
    val now = System.currentTimeMillis()
    val chunk = 500
    for (start in 0 until ENTRIES step chunk) {
        val entries = (start until start + chunk).map { i ->
            val date = today.minus(DatePeriod(days = i))
            val body = sampleBody(i, random)
            EntryEntity(
                id = "sample-$i",
                date = date.toString(),
                time = "%02d:%02d".format(random.nextInt(24), random.nextInt(60)),
                bodyHtml = body.html,
                plainText = body.plain,
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
        // 짝수 번째 일기마다 사진 한 장
        // 사진마다 다른 파일이라 메모리 캐시에 기대지 않음
        val media = entries.filterIndexed { k, _ -> (start + k) % 2 == 0 }.map { entry ->
            val n = entry.id.removePrefix("sample-").toInt() / 2
            // 복원은 사진 이름이 지문과 같은지 봄. 실제 앱처럼 지문으로 이름 붙임
            val drawn = photos.resolve("p$n.jpg").also { if (!it.exists()) writePhoto(it, random) }
            val sha256 = FileSystem.SYSTEM.sha256Hex(drawn.path.toPath())
            val file = photos.resolve("$sha256.jpg")
            if (file.exists()) drawn.delete() else drawn.renameTo(file)
            val name = file.name
            MediaEntity(
                id = "m-$n",
                entryId = entry.id,
                sortOrder = 0,
                status = MediaStatus.NORMAL.id,
                fileName = name,
                sha256 = sha256,
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
    check(db.mediaDao().count() >= PHOTOS) { "견본 사진 수가 맞지 않음" }
}

private class SampleBody(val html: String, val plain: String)

/**
 * 줄마다 쓴 글. 20번째마다 5천 자 넘는 긴 일기
 * 다섯 편 가운데 하나는 에디터 서식을 모두 씀: 굵게, 밑줄, 글자색, 형광펜, 체크리스트, 글머리표 목록
 * 저장 형식처럼 빈 줄로 나뉜 덩어리는 `p`, 덩어리 안 줄은 `br`, 목록은 `ul`·`li`
 */
private fun sampleBody(index: Int, random: Random): SampleBody {
    val count = if (index % LONG_EVERY == 0) LONG_SENTENCES else 2 + random.nextInt(5)
    val lines = List(count) { SENTENCES[random.nextInt(SENTENCES.size)] }
    val blocks = lines.chunked(3).map { block -> block.joinToString("<br>", "<p>", "</p>") }
    if (index % FORMATTED_EVERY != 1) {
        val html = blocks.joinToString("")
        return SampleBody(html, toPlainText(html))
    }
    val pick = { SENTENCES[random.nextInt(SENTENCES.size)] }
    val ink = INK[index % INK.size]
    val mark = MARK[index % MARK.size]
    val styled =
        "<p><b>${pick()}</b><br><u>${pick()}</u><br><span style=\"color:$ink\">${pick()}</span>" +
            "<br><span style=\"background-color:$mark\">${pick()}</span></p>"
    val checklist = "<p>${Checklist.DONE} ${pick()}<br>${Checklist.OPEN} ${pick()}<br>${Checklist.OPEN} ${pick()}</p>"
    val bullets = "<ul><li>${pick()}</li><li><b>${pick()}</b></li><li>${pick()}</li></ul>"
    val html = (listOf(styled, checklist, bullets) + blocks).joinToString("")
    return SampleBody(html, toPlainText(html))
}

private val INK = RichTextSanitizer.INK.toList()
private val MARK = RichTextSanitizer.MARK.toList()

private const val LONG_EVERY = 20
private const val LONG_SENTENCES = 300
private const val FORMATTED_EVERY = 5

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
