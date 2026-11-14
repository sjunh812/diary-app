package org.sjhstudio.diary.core.platform

/**
 * 사진을 줄여 JPEG로 씀(썸네일, 스펙 001 research R9, FR-036·FR-037)
 * 원본은 읽기만 하고, 사진 방향(EXIF)을 반영해 똑바로 선 그림으로 씀
 */
expect object ImageDecoder {
    /**
     * [source]를 긴 변이 [maxSide]px 이하가 되게 줄여 [target]에 JPEG 품질 [quality]로 씀
     * 원본이 더 작으면 키우지 않음. 읽을 수 없는 그림이면 false.
     */
    fun writeThumbnail(source: String, target: String, maxSide: Int, quality: Int): Boolean

    /** 그림 파일의 픽셀 크기. 읽을 수 없으면 null. 방금 쓴 JPEG에 씀 */
    fun size(path: String): ImageSize?
}

data class ImageSize(val width: Int, val height: Int)
