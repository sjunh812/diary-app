package org.sjhstudio.diary.core.platform

import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeoutOrNull
import platform.Foundation.NSBundleResourceRequest
import platform.Foundation.NSBundleResourceRequestLoadingPriorityUrgent
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSURLErrorDomain

/**
 * 글꼴마다 하나인 On-Demand Resources 태그(글꼴 ID)를 받아 `<id>.ttf`를 앱 파일 자리로 복사
 * 태그는 `iosApp/project.yml`이 정함
 * 개발 빌드는 시험할 수 있게 자료를 앱 안에 담음(EMBED_ASSET_PACKS_IN_PRODUCT_BUNDLE)
 */
@OptIn(ExperimentalForeignApi::class, ExperimentalAtomicApi::class)
class IosFontInstaller(private val files: AppFiles) : FontInstaller {
    private val manager = NSFileManager.defaultManager

    // 화면을 그릴 때도 불러 파일 확인은 글꼴마다 한 번만. 시작 뒤 뒤에서 먼저 채움(Startup)
    private val known = AtomicReference<Map<String, String>>(emptyMap())

    override fun installedPath(id: String): String? = (known.load()[id] ?: check(id)).ifEmpty { null }

    override fun knownPath(id: String): String? = known.load()[id]?.ifEmpty { null }

    private val _changes = MutableStateFlow(0)
    override val changes: StateFlow<Int> = _changes.asStateFlow()

    /** 없으면 빈 글자로 기억. 받으면 [copy]가 고쳐 둠 */
    private fun check(id: String): String = (target(id).takeIf { manager.fileExistsAtPath(it) } ?: "").also { remember(id, it) }

    private fun remember(id: String, path: String) {
        while (true) {
            val now = known.load()
            if (known.compareAndSet(now, now + (id to path))) return
        }
    }

    /**
     * 진행률과 결과를 한 코루틴에서 차례로 내보냄
     * 완료 알림은 다른 스레드에서 와서 그대로 보내면 "받는 중"이 "받았어요" 뒤에 끼어 받는 중으로 남음
     */
    override fun install(id: String): Flow<FontInstallState> = flow {
        installedPath(id)?.let {
            emit(FontInstallState.Installed(it))
            return@flow
        }
        val request = NSBundleResourceRequest(tags = setOf(id))
        request.loadingPriority = NSBundleResourceRequestLoadingPriorityUrgent
        val done = CompletableDeferred<NSError?>()
        request.beginAccessingResourcesWithCompletionHandler { error -> done.complete(error) }
        try {
            // 진행을 따로 알려 주지 않아 잠깐마다 읽음
            while (!done.isCompleted) {
                emit(FontInstallState.Downloading(request.progress.fractionCompleted.toFloat()))
                withTimeoutOrNull(PROGRESS_MILLIS) { done.await() }
            }
        } catch (cancelled: CancellationException) {
            // 화면을 떠나면 받기도 멈춤
            request.progress.cancel()
            throw cancelled
        }
        val error = done.await()
        if (error != null) {
            emit(FontInstallState.Failed(offline = error.domain == NSURLErrorDomain))
            return@flow
        }
        val source = request.bundle.pathForResource(id, ofType = "ttf")
        val installed = source?.let { copy(it, id) }
        request.endAccessingResources()
        emit(installed?.let { FontInstallState.Installed(it) } ?: FontInstallState.Failed(offline = false))
    }.flowOn(Dispatchers.IO)

    /** 반쯤 쓴 파일이 남지 않게 임시 이름으로 복사한 뒤 옮김 */
    private fun copy(source: String, id: String): String? {
        val target = target(id)
        val partial = "$target.part"
        manager.createDirectoryAtPath(files.fontDirectory, withIntermediateDirectories = true, attributes = null, error = null)
        manager.removeItemAtPath(partial, error = null)
        if (!manager.copyItemAtPath(source, toPath = partial, error = null)) return null
        manager.removeItemAtPath(target, error = null)
        return target.takeIf { manager.moveItemAtPath(partial, toPath = it, error = null) }?.also {
            remember(id, it)
            _changes.update { count -> count + 1 }
        }
    }

    override fun removeExcept(ids: Set<String>) {
        val names = manager.contentsOfDirectoryAtPath(files.fontDirectory, error = null).orEmpty().filterIsInstance<String>()
        for (name in names) {
            val id = name.substringBefore('.')
            if (id !in ids && manager.removeItemAtPath("${files.fontDirectory}/$name", error = null)) {
                remember(id, "")
                _changes.update { it + 1 }
            }
        }
    }

    private fun target(id: String) = "${files.fontDirectory}/$id.ttf"

    private companion object {
        const val PROGRESS_MILLIS = 200L
    }
}
