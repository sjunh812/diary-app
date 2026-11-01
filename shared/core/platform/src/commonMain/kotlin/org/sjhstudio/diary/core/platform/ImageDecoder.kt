package org.sjhstudio.diary.core.platform

/**
 * 사진을 줄여 JPEG로 쓴다(썸네일, 스펙 001 research R9, FR-036·FR-037).
 * 원본은 읽기만 한다. 사진의 방향(EXIF)을 반영해 똑바로 선 그림으로 쓴다.
 */
expect object ImageDecoder {
    /**
     * [source]를 긴 변이 [maxSide]px 이하가 되게 줄여 [target]에 JPEG 품질 [quality]로 쓴다.
     * 원본이 더 작으면 키우지 않는다. 읽을 수 없는 그림이면 false다.
     */
    fun writeThumbnail(source: String, target: String, maxSide: Int, quality: Int): Boolean
}
