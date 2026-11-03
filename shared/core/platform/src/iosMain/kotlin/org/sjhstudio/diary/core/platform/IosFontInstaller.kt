package org.sjhstudio.diary.core.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import platform.Foundation.NSBundleResourceRequest
import platform.Foundation.NSBundleResourceRequestLoadingPriorityUrgent
import platform.Foundation.NSFileManager
import platform.Foundation.NSURLErrorDomain

/**
 * 글꼴마다 하나인 On-Demand Resources 태그(글꼴 ID)를 받아 `<id>.ttf`를 앱 파일 자리로 복사한다.
 * 태그는 `iosApp/project.yml`이 정한다. 개발 빌드는 시험할 수 있게 자료를 앱 안에 담는다(EMBED_ASSET_PACKS_IN_PRODUCT_BUNDLE).
 */
@OptIn(ExperimentalForeignApi::class)
class IosFontInstaller(private val files: AppFiles) : FontInstaller {
    private val manager = NSFileManager.defaultManager

    override fun installedPath(id: String): String? = target(id).takeIf { manager.fileExistsAtPath(it) }

    override fun install(id: String): Flow<FontInstallState> = callbackFlow {
        installedPath(id)?.let {
            send(FontInstallState.Installed(it))
            close()
            return@callbackFlow
        }
        val request = NSBundleResourceRequest(tags = setOf(id))
        request.loadingPriority = NSBundleResourceRequestLoadingPriorityUrgent
        // 진행은 따로 알려 주지 않아 잠깐마다 읽는다.
        val progress = launch {
            while (isActive) {
                send(FontInstallState.Downloading(request.progress.fractionCompleted.toFloat()))
                delay(PROGRESS_MILLIS)
            }
        }
        request.beginAccessingResourcesWithCompletionHandler { error ->
            progress.cancel()
            if (error != null) {
                trySend(FontInstallState.Failed(offline = error.domain == NSURLErrorDomain))
            } else {
                val source = request.bundle.pathForResource(id, ofType = "ttf")
                trySend(source?.let { copy(it, id) }?.let { FontInstallState.Installed(it) } ?: FontInstallState.Failed(offline = false))
                request.endAccessingResources()
            }
            close()
        }
        awaitClose { progress.cancel() }
    }

    /** 반쯤 쓴 파일을 남기지 않게 임시 이름으로 복사한 뒤 옮긴다. */
    private fun copy(source: String, id: String): String? {
        val target = target(id)
        val partial = "$target.part"
        manager.createDirectoryAtPath(files.fontDirectory, withIntermediateDirectories = true, attributes = null, error = null)
        manager.removeItemAtPath(partial, error = null)
        if (!manager.copyItemAtPath(source, toPath = partial, error = null)) return null
        manager.removeItemAtPath(target, error = null)
        return target.takeIf { manager.moveItemAtPath(partial, toPath = it, error = null) }
    }

    private fun target(id: String) = "${files.fontDirectory}/$id.ttf"

    private companion object {
        const val PROGRESS_MILLIS = 200L
    }
}
