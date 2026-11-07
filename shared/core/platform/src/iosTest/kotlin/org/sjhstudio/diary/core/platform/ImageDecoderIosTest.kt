package org.sjhstudio.diary.core.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUUID
import platform.Foundation.writeToFile
import platform.UIKit.UIColor
import platform.UIKit.UIGraphicsImageRenderer
import platform.UIKit.UIGraphicsImageRendererFormat
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImageOrientation

@OptIn(ExperimentalForeignApi::class)
class ImageDecoderIosTest {
    private fun path(name: String) = NSTemporaryDirectory() + NSUUID().UUIDString + "-" + name

    /** 1200×800 그림을 만들어 [orientation] 방향 표시를 붙여 JPEG로 쓴다. */
    private fun photo(orientation: UIImageOrientation): String {
        val format = UIGraphicsImageRendererFormat.defaultFormat().apply { setScale(1.0) }
        val drawn =
            UIGraphicsImageRenderer(size = CGSizeMake(1200.0, 800.0), format = format).imageWithActions { context ->
                UIColor.orangeColor.setFill()
                context?.fillRect(CGRectMake(0.0, 0.0, 1200.0, 800.0))
            }
        val oriented = UIImage(cGImage = drawn.CGImage, scale = 1.0, orientation = orientation)
        val target = path("photo.jpg")
        UIImageJPEGRepresentation(oriented, 0.9)!!.writeToFile(target, atomically = true)
        return target
    }

    private fun size(path: String): Pair<Int, Int> =
        UIImage.imageWithContentsOfFile(path)!!.size.useContents { width.toInt() to height.toInt() }

    @Test
    fun long_side_becomes_480() {
        val target = path("thumb.jpg")
        assertTrue(ImageDecoder.writeThumbnail(photo(UIImageOrientation.UIImageOrientationUp), target, 480, 80))
        assertEquals(480 to 320, size(target))
    }

    /** 옆으로 찍어 방향 표시만 붙은 사진은 세워서 줄인다. */
    @Test
    fun exif_orientation_is_applied() {
        val target = path("thumb.jpg")
        assertTrue(ImageDecoder.writeThumbnail(photo(UIImageOrientation.UIImageOrientationRight), target, 480, 80))
        assertEquals(320 to 480, size(target))
    }

    @Test
    fun broken_file_is_refused() {
        val broken = path("broken.jpg")
        NSFileManager.defaultManager.createFileAtPath(broken, contents = null, attributes = null)
        assertFalse(ImageDecoder.writeThumbnail(broken, path("thumb.jpg"), 480, 80))
    }
}
