package org.sjhstudio.diary.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.entity.Library
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import org.sjhstudio.diary.core.designsystem.resources.Res as DsRes
import org.sjhstudio.diary.core.model.orElseOnFailure
import org.sjhstudio.diary.core.ui.WhileUiSubscribed
import org.sjhstudio.diary.core.ui.stateInSafely
import org.sjhstudio.diary.feature.settings.resources.Res

/**
 * 라이브러리 줄 하나.
 * [author]는 만든 곳(조직이나 첫 개발자)이고 없으면 비어 있다.
 */
data class LibraryNotice(val id: String, val name: String, val author: String, val licenseName: String)

/**
 * 라이브러리 전문.
 * [details]는 묶음 이름·버전, 만든 곳, 누리집이고 [text]는 라이선스 전문(여럿이면 차례로)이다.
 */
data class LibraryText(val name: String, val licenseName: String, val details: String, val text: String)

/**
 * 오픈소스 라이선스(Figma "오픈소스 라이선스 (I1)").
 * 라이브러리 목록은 빌드 때 만든 `files/aboutlibraries.json`을 읽는다(KotlinConf 앱과 같은 방식).
 * 글꼴 전문은 디자인 시스템 모듈에 둔 원문 파일을 읽는다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LicensesViewModel(
    private val readList: suspend () -> String = { Res.readBytes("files/aboutlibraries.json").decodeToString() },
    private val readFont: suspend (String) -> String = { DsRes.readBytes("files/licenses/$it").decodeToString() },
    // 시험은 시험 디스패처 사용. 다른 스레드에서 늦게 끝난 읽기가 다음 시험을 깨뜨렸음
    private val background: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {
    private var libs: List<Library>? = null
    private val attempt = MutableStateFlow(0)
    private val _failed = MutableStateFlow(false)

    /**
     * 목록을 읽지 못했다.
     * 앱에 든 파일이라 드물지만, 그때는 라이브러리 묶음이 말없이 사라지지 않고 다시 시도를 보인다.
     */
    val failed: StateFlow<Boolean> = _failed.asStateFlow()

    /** 읽는 동안이나 읽지 못하면 null이다. */
    val libraries: StateFlow<List<LibraryNotice>?> =
        attempt.flatMapLatest {
            flow<List<LibraryNotice>?> {
                _failed.value = false
                emit(load().map { it.notice() })
            }.catch {
                if (it !is Exception) throw it
                _failed.value = true
                emit(null)
            }
        }
            .flowOn(background)
            .stateInSafely(viewModelScope, WhileUiSubscribed, null)

    fun retry() = attempt.update { it + 1 }

    // OFL 원문은 70자마다 줄을 바꿔 두어 폰 폭에서 줄이 들쭉날쭉하다.
    // 문단 안의 줄바꿈(다음 줄이 소문자나 여는 괄호, 따옴표로 시작)만 띄어쓰기로 잇고 낱말은 그대로 둔다.
    // 주소로 시작하는 줄은 잇지 않는다.
    // 읽지 못하면 null이다.
    // 화면이 그리는 중에 부르므로 예외를 넘기면 앱이 꺼진다.
    internal suspend fun fontText(notice: FontLicenseNotice): String? =
        orElseOnFailure(null) { readFont(notice.file).replace(SOFT_BREAK, " ") }

    /** 목록에 없거나 읽지 못하면 null이다. */
    suspend fun libraryText(id: String): LibraryText? = orElseOnFailure(null) { findLibraryText(id) }

    private suspend fun findLibraryText(id: String): LibraryText? {
        val library = load().firstOrNull { it.uniqueId == id } ?: return null
        val details =
            listOfNotNull(
                "${library.uniqueId} ${library.artifactVersion.orEmpty()}".trim(),
                library.author().ifEmpty { null },
                library.website
            ).joinToString("\n")
        // 전문이 없는 약관(Play Core 같은)은 약관 주소를 보인다.
        val text = library.licenses.joinToString("\n\n") { it.licenseContent?.takeIf(String::isNotBlank) ?: it.url.orEmpty() }
        return LibraryText(library.name, library.licenseName(), details, text)
    }

    private suspend fun load(): List<Library> = libs ?: Libs.Builder()
        .withJson(readList())
        .build()
        .libraries
        .sortedBy { it.name.lowercase() }
        .also { libs = it }

    private fun Library.notice() = LibraryNotice(uniqueId, name, author(), licenseName())

    private fun Library.author(): String = organization?.name ?: developers.firstOrNull()?.name.orEmpty()

    private fun Library.licenseName(): String = licenses.joinToString(", ") { it.name }

    private companion object {
        val SOFT_BREAK = Regex("(?<=\\S)[ \\t]*\n(?!https?:)(?=[a-z(\"])")
    }
}
