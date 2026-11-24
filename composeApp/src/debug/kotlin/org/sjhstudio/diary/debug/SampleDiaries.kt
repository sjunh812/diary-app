package org.sjhstudio.diary.debug

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.core.graphics.createBitmap
import java.io.File
import kotlin.random.Random
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import okio.FileSystem
import okio.Path.Companion.toPath
import org.sjhstudio.diary.core.database.DiaryDatabase
import org.sjhstudio.diary.core.database.EntryEntity
import org.sjhstudio.diary.core.database.MediaEntity
import org.sjhstudio.diary.core.model.Checklist
import org.sjhstudio.diary.core.model.MediaStatus
import org.sjhstudio.diary.core.model.richtext.RichTextSanitizer
import org.sjhstudio.diary.core.model.richtext.toPlainText
import org.sjhstudio.diary.core.platform.sha256Hex

// 개발자 모드 "일기 1만 편 넣기"의 견본 일기(스펙 001 T083, 스펙 008 SC-003)
// 20%는 에디터 서식을 모두 쓰고, 5%는 5천 자 넘는 긴 일기, 짝수 번째마다 사진 한 장

private const val ENTRIES = 10_000

/**
 * 일기 1만 편과 사진 5천 장을 [db]와 [photos]에 넣음. 이미 넣었으면 그대로 둠
 * 백업 파일로 실기기 측정에 옮김
 */
internal suspend fun writeSample(db: DiaryDatabase, photos: File): Boolean {
    // 같은 ID로 다시 넣으면 DB가 거절해 두 번째 누름은 건너뜀
    if (db.entryDao().get("sample-0") != null) return false
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
    }
    return true
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
    val bitmap = createBitmap(480, 360)
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
