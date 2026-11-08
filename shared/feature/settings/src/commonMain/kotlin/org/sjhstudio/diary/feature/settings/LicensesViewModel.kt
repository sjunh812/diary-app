package org.sjhstudio.diary.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.entity.Library
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import org.sjhstudio.diary.core.designsystem.resources.Res as DsRes
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
class LicensesViewModel : ViewModel() {
    private var libs: List<Library>? = null

    /** 읽는 동안은 null이다. */
    val libraries: StateFlow<List<LibraryNotice>?> =
        flow { emit(load().map { it.notice() }) }
            .flowOn(Dispatchers.Default)
            .stateInSafely(viewModelScope, SharingStarted.WhileSubscribed(STOP_MILLIS), null)

    // OFL 원문은 70자마다 줄을 바꿔 두어 폰 폭에서 줄이 들쭉날쭉하다.
    // 문단 안의 줄바꿈(다음 줄이 소문자나 여는 괄호, 따옴표로 시작)만 띄어쓰기로 잇고 낱말은 그대로 둔다.
    // 주소로 시작하는 줄은 잇지 않는다.
    internal suspend fun fontText(notice: FontLicenseNotice): String =
        DsRes.readBytes("files/licenses/${notice.file}").decodeToString().replace(SOFT_BREAK, " ")

    suspend fun libraryText(id: String): LibraryText? {
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
        .withJson(Res.readBytes("files/aboutlibraries.json").decodeToString())
        .build()
        .libraries
        .sortedBy { it.name.lowercase() }
        .also { libs = it }

    private fun Library.notice() = LibraryNotice(uniqueId, name, author(), licenseName())

    private fun Library.author(): String = organization?.name ?: developers.firstOrNull()?.name.orEmpty()

    private fun Library.licenseName(): String = licenses.joinToString(", ") { it.name }

    private companion object {
        const val STOP_MILLIS = 5_000L
        val SOFT_BREAK = Regex("(?<=\\S)[ \\t]*\n(?!https?:)(?=[a-z(\"])")
    }
}
