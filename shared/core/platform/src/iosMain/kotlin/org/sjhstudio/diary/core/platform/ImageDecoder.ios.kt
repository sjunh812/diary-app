package org.sjhstudio.diary.core.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.FloatVar
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.useContents
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFNumberCreate
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFStringCreateWithCString
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.CFURLCreateWithFileSystemPath
import platform.CoreFoundation.CFURLRef
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFNumberFloatType
import platform.CoreFoundation.kCFNumberIntType
import platform.CoreFoundation.kCFStringEncodingUTF8
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.CoreFoundation.kCFURLPOSIXPathStyle
import platform.CoreGraphics.CGImageRelease
import platform.ImageIO.CGImageDestinationAddImage
import platform.ImageIO.CGImageDestinationCreateWithURL
import platform.ImageIO.CGImageDestinationFinalize
import platform.ImageIO.CGImageSourceCreateThumbnailAtIndex
import platform.ImageIO.CGImageSourceCreateWithURL
import platform.ImageIO.kCGImageDestinationLossyCompressionQuality
import platform.ImageIO.kCGImageSourceCreateThumbnailFromImageAlways
import platform.ImageIO.kCGImageSourceCreateThumbnailWithTransform
import platform.ImageIO.kCGImageSourceThumbnailMaxPixelSize
import platform.UIKit.UIImage

// CGImageSource 썸네일은 원본을 다 풀지 않고 줄인 크기로 읽어 큰 사진에서도 메모리를 적게 씀
// 방향(EXIF)은 WithTransform으로 반영
@OptIn(ExperimentalForeignApi::class)
actual object ImageDecoder {
    actual fun writeThumbnail(source: String, target: String, maxSide: Int, quality: Int): Boolean = memScoped {
        val created = mutableListOf<CFTypeRef?>()
        fun <T : CFTypeRef?> keep(ref: T): T = ref.also { created += it }
        try {
            val sourceUrl = keep(fileUrl(source)) ?: return@memScoped false
            val imageSource = keep(CGImageSourceCreateWithURL(sourceUrl, null)) ?: return@memScoped false
            val options = keep(CFDictionaryCreateMutable(null, 3, kCFTypeDictionaryKeyCallBacks.ptr, kCFTypeDictionaryValueCallBacks.ptr))
            val side = alloc<IntVar>().apply { value = maxSide }
            CFDictionaryAddValue(options, kCGImageSourceCreateThumbnailFromImageAlways, kCFBooleanTrue)
            CFDictionaryAddValue(options, kCGImageSourceCreateThumbnailWithTransform, kCFBooleanTrue)
            CFDictionaryAddValue(options, kCGImageSourceThumbnailMaxPixelSize, keep(CFNumberCreate(null, kCFNumberIntType, side.ptr)))
            val image = CGImageSourceCreateThumbnailAtIndex(imageSource, 0u, options) ?: return@memScoped false
            try {
                val targetUrl = keep(fileUrl(target)) ?: return@memScoped false
                val jpeg = keep(cfString("public.jpeg"))
                val destination = keep(CGImageDestinationCreateWithURL(targetUrl, jpeg, 1u, null)) ?: return@memScoped false
                val properties =
                    keep(CFDictionaryCreateMutable(null, 1, kCFTypeDictionaryKeyCallBacks.ptr, kCFTypeDictionaryValueCallBacks.ptr))
                val level = alloc<FloatVar>().apply { value = quality / 100f }
                CFDictionaryAddValue(
                    properties,
                    kCGImageDestinationLossyCompressionQuality,
                    keep(CFNumberCreate(null, kCFNumberFloatType, level.ptr))
                )
                CGImageDestinationAddImage(destination, image, properties)
                CGImageDestinationFinalize(destination)
            } finally {
                CGImageRelease(image)
            }
        } finally {
            created.filterNotNull().forEach(::CFRelease)
        }
    }

    actual fun size(path: String): ImageSize? {
        val image = UIImage.imageWithContentsOfFile(path) ?: return null
        return image.size.useContents { ImageSize(width.toInt(), height.toInt()) }.takeIf { it.width > 0 && it.height > 0 }
    }

    private fun cfString(value: String): CFStringRef? = CFStringCreateWithCString(null, value, kCFStringEncodingUTF8)

    private fun fileUrl(path: String): CFURLRef? {
        val text = cfString(path) ?: return null
        return try {
            CFURLCreateWithFileSystemPath(null, text, kCFURLPOSIXPathStyle, false)
        } finally {
            CFRelease(text)
        }
    }
}
