package org.sjhstudio.diary.core.ui

import androidx.compose.runtime.Composable
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.key.Keyer
import coil3.request.Options
import okio.FileSystem
import okio.Path.Companion.toPath

/**
 * 사진 칸이 Coil에 넘기는 모델
 * 원본 경로를 주면 썸네일을 읽음(스펙 001 FR-036)
 */
data class Thumbnail(val mediaPath: String)

/**
 * 썸네일 경로. 없으면 만듦
 * 맡지 않는 사진이거나 만들지 못하면 null(`ThumbnailRepository`)
 */
fun interface ThumbnailSource {
    suspend fun thumbnailPath(mediaPath: String): String?
}

/**
 * 썸네일을 읽는 Coil Fetcher(research R9)
 * 읽고 줄이는 일은 Coil이 화면 밖 스레드에서 함
 * 썸네일이 없는 사진(예전 일기 보기의 옛 경로)은 원본을 Coil이 칸 크기로 줄여 읽음
 */
internal class ThumbnailFetcher(private val data: Thumbnail, private val source: ThumbnailSource) : Fetcher {
    override suspend fun fetch(): FetchResult {
        val path = source.thumbnailPath(data.mediaPath) ?: data.mediaPath
        return SourceFetchResult(
            source = ImageSource(file = path.toPath(), fileSystem = FileSystem.SYSTEM),
            mimeType = null,
            dataSource = DataSource.DISK
        )
    }

    class Factory(private val source: ThumbnailSource) : Fetcher.Factory<Thumbnail> {
        override fun create(data: Thumbnail, options: Options, imageLoader: ImageLoader): Fetcher = ThumbnailFetcher(data, source)
    }
}

/**
 * 메모리 캐시 키
 * 같은 사진이면 같은 키
 */
internal object ThumbnailKeyer : Keyer<Thumbnail> {
    override fun key(data: Thumbnail, options: Options): String = "thumbnail:${data.mediaPath}"
}

/**
 * 앱 뿌리에서 한 번 부름
 * 사진 칸이 썸네일을 읽게 Coil 기본 이미지 로더를 정함
 */
@Composable
fun ProvideDiaryImageLoader(source: ThumbnailSource) {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components {
                add(ThumbnailKeyer)
                add(ThumbnailFetcher.Factory(source))
            }.build()
    }
}
