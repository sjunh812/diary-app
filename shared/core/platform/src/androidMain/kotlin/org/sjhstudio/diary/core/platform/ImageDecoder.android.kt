package org.sjhstudio.diary.core.platform

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.media.ExifInterface
import java.io.File
import kotlin.math.max

actual object ImageDecoder {
    /**
     * 메모리가 모자라면 false를 돌려줘 "읽지 못한 사진" 안내로 이어짐.
     * 큰 사진(4800만 화소 등)은 줄이는 동안 수십 MB를 써 저사양 기기에서 메모리 부족이 날 수 있음.
     * 메모리 부족은 Exception이 아닌 Error라 바깥 안전망이 못 잡고 앱이 꺼지므로 여기서 잡음.
     */
    actual fun writeThumbnail(source: String, target: String, maxSide: Int, quality: Int): Boolean = try {
        shrink(source, target, maxSide, quality)
    } catch (_: OutOfMemoryError) {
        File(target).delete()
        false
    }

    private fun shrink(source: String, target: String, maxSide: Int, quality: Int): Boolean {
        // 크기만 먼저 읽고, 긴 변이 maxSide 아래로 내려가지 않는 가장 큰 2의 거듭제곱으로 다운샘플링
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(source, bounds)
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
        val readable = bounds.outWidth > 0 && bounds.outHeight > 0
        val decoded =
            (if (readable) BitmapFactory.decodeFile(source, BitmapFactory.Options().apply { inSampleSize = sample }) else null)
                ?: return false
        // 정확한 크기로 줄이기와 방향 돌리기를 한 번에
        val scale = minOf(1f, maxSide.toFloat() / max(decoded.width, decoded.height))
        val matrix = orientation(source).apply { postScale(scale, scale) }
        val result = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        // JPEG에는 투명이 없어 그대로 쓰면 투명한 곳이 검게 나와 흰 바탕에 올림
        val opaque = if (result.hasAlpha()) onWhite(result) else result
        return try {
            File(target).outputStream().use { opaque.compress(Bitmap.CompressFormat.JPEG, quality, it) }
        } finally {
            if (opaque !== result) opaque.recycle()
            if (result !== decoded) result.recycle()
            decoded.recycle()
        }
    }

    private fun onWhite(image: Bitmap): Bitmap {
        val flat = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888)
        Canvas(flat).apply {
            drawColor(Color.WHITE)
            drawBitmap(image, 0f, 0f, null)
        }
        return flat
    }

    actual fun size(path: String): ImageSize? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        return if (bounds.outWidth > 0 && bounds.outHeight > 0) ImageSize(bounds.outWidth, bounds.outHeight) else null
    }

    /** EXIF 방향을 행렬로 바꿈. EXIF가 없는 그림(PNG 등)은 그대로 */
    private fun orientation(source: String): Matrix {
        val matrix = Matrix()
        val value =
            runCatching { ExifInterface(source).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }
                .getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        when (value) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)

            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)

            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)

            ExifInterface.ORIENTATION_TRANSPOSE -> matrix.apply {
                postRotate(90f)
                postScale(-1f, 1f)
            }

            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)

            ExifInterface.ORIENTATION_TRANSVERSE -> matrix.apply {
                postRotate(-90f)
                postScale(-1f, 1f)
            }

            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(-90f)
        }
        return matrix
    }
}
