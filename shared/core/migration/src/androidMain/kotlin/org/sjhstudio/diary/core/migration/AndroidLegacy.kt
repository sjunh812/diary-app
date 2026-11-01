package org.sjhstudio.diary.core.migration

import android.content.Context
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.os.StatFs
import java.io.File
import okio.Path
import okio.Path.Companion.toOkioPath

/** 옛 앱(1.7.x)이 같은 패키지에 남긴 원본 자리. 읽기만 한다. */
fun legacyFiles(context: Context): LegacyFiles {
    val prefs = File(context.applicationInfo.dataDir, "shared_prefs")
    return LegacyFiles(
        noteDb = context.getDatabasePath("note.db").toOkioPath(),
        prefXml = File(prefs, "pref.xml").toOkioPath(),
        pref2Xml = File(prefs, "pref2.xml").toOkioPath()
    )
}

/** 그 폴더가 있는 저장소의 남은 바이트. */
fun freeBytes(path: Path): Long {
    var dir: File? = path.toFile()
    while (dir != null && !dir.exists()) dir = dir.parentFile
    return runCatching { StatFs((dir ?: File("/")).path).availableBytes }.getOrDefault(0L)
}

/**
 * 사진 가로세로를 머리만 읽어 잰다. EXIF가 90°·270° 돌림이면 가로세로를 바꾼다(research R8).
 * 사진 전체를 디코딩하지 않는다.
 */
object AndroidImageSizeReader : ImageSizeReader {
    override fun read(path: String): Pair<Int, Int>? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, options)
        if (options.outWidth <= 0 || options.outHeight <= 0) return null
        val orientation =
            runCatching { ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }
                .getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        val rotated =
            orientation == ExifInterface.ORIENTATION_ROTATE_90 ||
                orientation == ExifInterface.ORIENTATION_ROTATE_270 ||
                orientation == ExifInterface.ORIENTATION_TRANSPOSE ||
                orientation == ExifInterface.ORIENTATION_TRANSVERSE
        return if (rotated) options.outHeight to options.outWidth else options.outWidth to options.outHeight
    }
}
