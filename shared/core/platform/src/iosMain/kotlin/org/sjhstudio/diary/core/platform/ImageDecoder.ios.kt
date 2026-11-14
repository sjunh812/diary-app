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
import platform.CoreGraphics.CGBitmapContextCreate
import platform.CoreGraphics.CGBitmapContextCreateImage
import platform.CoreGraphics.CGColorSpaceCreateDeviceRGB
import platform.CoreGraphics.CGColorSpaceRelease
import platform.CoreGraphics.CGContextDrawImage
import platform.CoreGraphics.CGContextFillRect
import platform.CoreGraphics.CGContextRelease
import platform.CoreGraphics.CGContextSetRGBFillColor
import platform.CoreGraphics.CGImageAlphaInfo
import platform.CoreGraphics.CGImageGetAlphaInfo
import platform.CoreGraphics.CGImageGetHeight
import platform.CoreGraphics.CGImageGetWidth
import platform.CoreGraphics.CGImageRef
import platform.CoreGraphics.CGImageRelease
import platform.CoreGraphics.CGRectMake
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
    private val OPAQUE =
        setOf(CGImageAlphaInfo.kCGImageAlphaNone, CGImageAlphaInfo.kCGImageAlphaNoneSkipFirst, CGImageAlphaInfo.kCGImageAlphaNoneSkipLast)

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
            val thumbnail = CGImageSourceCreateThumbnailAtIndex(imageSource, 0u, options) ?: return@memScoped false
            // JPEG에는 투명이 없어 그대로 쓰면 투명한 곳이 검게 나와 흰 바탕에 올림(Android와 같음)
            val image = onWhite(thumbnail) ?: thumbnail
            if (image != thumbnail) CGImageRelease(thumbnail)
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

    /** 투명이 있으면 흰 바탕에 그린 불투명 그림. 없으면 null */
    private fun onWhite(image: CGImageRef): CGImageRef? = image.takeIf { hasAlpha(it) }?.let { drawOnWhite(it) }

    private fun hasAlpha(image: CGImageRef): Boolean = CGImageGetAlphaInfo(image) !in OPAQUE

    private fun drawOnWhite(image: CGImageRef): CGImageRef? {
        val width = CGImageGetWidth(image)
        val height = CGImageGetHeight(image)
        val space = CGColorSpaceCreateDeviceRGB()
        val context = CGBitmapContextCreate(null, width, height, 8u, 0u, space, CGImageAlphaInfo.kCGImageAlphaNoneSkipLast.value)
        CGColorSpaceRelease(space)
        return context?.let {
            try {
                val bounds = CGRectMake(0.0, 0.0, width.toDouble(), height.toDouble())
                CGContextSetRGBFillColor(it, 1.0, 1.0, 1.0, 1.0)
                CGContextFillRect(it, bounds)
                CGContextDrawImage(it, bounds, image)
                CGBitmapContextCreateImage(it)
            } finally {
                CGContextRelease(it)
            }
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
