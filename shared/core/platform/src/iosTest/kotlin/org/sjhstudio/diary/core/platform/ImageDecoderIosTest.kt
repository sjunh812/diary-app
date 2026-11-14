package org.sjhstudio.diary.core.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGBitmapContextCreate
import platform.CoreGraphics.CGColorSpaceCreateDeviceRGB
import platform.CoreGraphics.CGColorSpaceRelease
import platform.CoreGraphics.CGContextDrawImage
import platform.CoreGraphics.CGContextRelease
import platform.CoreGraphics.CGImageAlphaInfo
import platform.CoreGraphics.CGImageGetHeight
import platform.CoreGraphics.CGImageGetWidth
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
import platform.UIKit.UIImagePNGRepresentation

@OptIn(ExperimentalForeignApi::class)
class ImageDecoderIosTest {
    private fun path(name: String) = NSTemporaryDirectory() + NSUUID().UUIDString + "-" + name

    /** 1200×800 그림을 만들어 [orientation] 방향 표시를 붙여 JPEG로 씀 */
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

    /** 옆으로 찍어 방향 표시만 붙은 사진은 세워서 줄임 */
    @Test
    fun exif_orientation_is_applied() {
        val target = path("thumb.jpg")
        assertTrue(ImageDecoder.writeThumbnail(photo(UIImageOrientation.UIImageOrientationRight), target, 480, 80))
        assertEquals(320 to 480, size(target))
    }

    /** 투명 PNG는 흰 바탕으로. 그대로 JPEG로 쓰면 투명한 곳이 검게 나옴 */
    @Test
    fun transparent_parts_become_white() {
        val format = UIGraphicsImageRendererFormat.defaultFormat().apply {
            setScale(1.0)
            setOpaque(false)
        }
        val clear = UIGraphicsImageRenderer(size = CGSizeMake(600.0, 400.0), format = format).imageWithActions { }
        val png = path("clear.png")
        UIImagePNGRepresentation(clear)!!.writeToFile(png, atomically = true)
        val target = path("thumb.jpg")

        assertTrue(ImageDecoder.writeThumbnail(png, target, 480, 80))

        assertTrue(centerBrightness(target) > 240, "투명한 곳이 희게 나와야 함")
    }

    /** 가운데 한 점의 밝기(0~255) */
    private fun centerBrightness(path: String): Int = memScoped {
        val image = UIImage.imageWithContentsOfFile(path)!!.CGImage
        val pixel = allocArray<UByteVar>(4)
        val space = CGColorSpaceCreateDeviceRGB()
        val context = CGBitmapContextCreate(pixel, 1u, 1u, 8u, 4u, space, CGImageAlphaInfo.kCGImageAlphaNoneSkipLast.value)
        CGColorSpaceRelease(space)
        val width = CGImageGetWidth(image).toDouble()
        val height = CGImageGetHeight(image).toDouble()
        CGContextDrawImage(context, CGRectMake(-width / 2, -height / 2, width, height), image)
        CGContextRelease(context)
        (pixel[0].toInt() + pixel[1].toInt() + pixel[2].toInt()) / 3
    }

    @Test
    fun broken_file_is_refused() {
        val broken = path("broken.jpg")
        NSFileManager.defaultManager.createFileAtPath(broken, contents = null, attributes = null)
        assertFalse(ImageDecoder.writeThumbnail(broken, path("thumb.jpg"), 480, 80))
    }
}
