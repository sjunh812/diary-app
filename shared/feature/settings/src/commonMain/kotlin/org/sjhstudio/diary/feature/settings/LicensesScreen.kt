package org.sjhstudio.diary.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.sjhstudio.diary.core.designsystem.component.DiarySettingsHeader
import org.sjhstudio.diary.core.designsystem.component.DiarySettingsRow
import org.sjhstudio.diary.core.designsystem.component.DiaryTopBar
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.DiaryWriteSize
import org.sjhstudio.diary.core.model.DiaryFont
import org.sjhstudio.diary.feature.settings.resources.Res
import org.sjhstudio.diary.feature.settings.resources.licenses_group_fonts
import org.sjhstudio.diary.feature.settings.resources.licenses_group_libraries
import org.sjhstudio.diary.feature.settings.resources.settings_licenses

/**
 * 오픈소스 라이선스(Figma "오픈소스 라이선스 (I1)").
 * 설정 > 정보 > 오픈소스 라이선스.
 * 글꼴 10종을 `docs/design.md` 표 순서로, 앱에 들어간 라이브러리를 이름순으로 보인다.
 * 누르면 전문(I2)으로 간다.
 * 라이브러리가 150개 넘어 지연 목록에 담는다.
 */
@Composable
fun LicensesScreen(
    libraries: List<LibraryNotice>?,
    onFont: (DiaryFont) -> Unit,
    onLibrary: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DiaryTheme.colors
    Column(modifier.fillMaxSize().background(colors.canvas).systemBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        DiaryTopBar(title = stringResource(Res.string.settings_licenses), onBack = onBack)
        LazyColumn(Modifier.widthIn(max = DiaryWriteSize.settingsMax).fillMaxWidth()) {
            item(key = "fonts") { DiarySettingsHeader(stringResource(Res.string.licenses_group_fonts)) }
            items(FONT_NOTICES, key = { "font-${it.font.id}" }) { notice ->
                DiarySettingsRow(
                    stringResource(notice.font.label()),
                    onClick = { onFont(notice.font) },
                    description = stringResource(notice.license.label)
                )
            }
            if (!libraries.isNullOrEmpty()) {
                item(key = "libraries") { DiarySettingsHeader(stringResource(Res.string.licenses_group_libraries), gapAbove = true) }
                items(libraries, key = { "lib-${it.id}" }) { library ->
                    DiarySettingsRow(
                        library.name,
                        onClick = { onLibrary(library.id) },
                        description = listOf(library.author, library.licenseName).filter(String::isNotEmpty).joinToString(" · ")
                    )
                }
            }
        }
    }
}

/**
 * 라이선스 전문(Figma "오픈소스 라이선스 상태" I2).
 * 상단 바에 글꼴이나 라이브러리 이름, 맨 위에 라이선스 이름을 둔다.
 * 전문은 받은 원문 그대로 보이고 번역하지 않는다.
 * 읽는 동안 [text]는 null이다.
 */
@Composable
fun LicenseTextScreen(
    title: String,
    licenseName: String,
    text: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    details: String? = null
) {
    val colors = DiaryTheme.colors
    Column(modifier.fillMaxSize().background(colors.canvas).systemBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        DiaryTopBar(title = title, onBack = onBack)
        Column(
            Modifier
                .widthIn(max = DiaryWriteSize.settingsMax)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = DiarySpacing.screenCompact, vertical = DiarySpacing.x2)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(DiarySpacing.x4)
        ) {
            BasicText(licenseName, style = DiaryTheme.typography.bodyStrong.copy(color = colors.textPrimary))
            if (details != null) BasicText(details, style = DiaryTheme.typography.caption.copy(color = colors.textSecondary))
            if (text != null) BasicText(text, style = DiaryTheme.typography.caption.copy(color = colors.textSecondary))
        }
    }
}

/**
 * 오픈소스 라이선스 목록을 잇는다.
 * 앱 틀은 이것만 부른다.
 */
@Composable
fun LicensesRoute(onFont: (DiaryFont) -> Unit, onLibrary: (String) -> Unit, onBack: () -> Unit) {
    val viewModel = koinViewModel<LicensesViewModel>()
    val libraries by viewModel.libraries.collectAsStateWithLifecycle()
    LicensesScreen(libraries, onFont = onFont, onLibrary = onLibrary, onBack = onBack)
}

/**
 * 글꼴 전문.
 * [fontId]가 고지가 없는 글꼴이면 바로 돌아간다.
 */
@Composable
fun FontLicenseRoute(fontId: String, onBack: () -> Unit) {
    val viewModel = koinViewModel<LicensesViewModel>()
    val notice = FONT_NOTICES.firstOrNull { it.font.id == fontId }
    if (notice == null) {
        LaunchedEffect(fontId) { onBack() }
        return
    }
    val text by produceState<String?>(null, notice) { value = viewModel.fontText(notice) }
    LicenseTextScreen(stringResource(notice.font.label()), stringResource(notice.license.label), text, onBack = onBack)
}

/**
 * 라이브러리 전문.
 * 목록에 없는 [libraryId]면 이름 없이 빈 화면이다.
 */
@Composable
fun LibraryLicenseRoute(libraryId: String, onBack: () -> Unit) {
    val viewModel = koinViewModel<LicensesViewModel>()
    val library by produceState<LibraryText?>(null, libraryId) { value = viewModel.libraryText(libraryId) }
    val current = library
    LicenseTextScreen(current?.name.orEmpty(), current?.licenseName.orEmpty(), current?.text, onBack = onBack, details = current?.details)
}
