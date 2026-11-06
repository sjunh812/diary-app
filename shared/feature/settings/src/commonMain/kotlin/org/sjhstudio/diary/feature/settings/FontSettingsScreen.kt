package org.sjhstudio.diary.feature.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.sjhstudio.diary.core.designsystem.component.DiarySettingsChoiceRow
import org.sjhstudio.diary.core.designsystem.component.DiaryToastHost
import org.sjhstudio.diary.core.designsystem.component.DiaryToastState
import org.sjhstudio.diary.core.designsystem.component.DiaryTopBar
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.DiaryWriteSize
import org.sjhstudio.diary.core.designsystem.theme.diaryLanguage
import org.sjhstudio.diary.core.model.DiaryFont
import org.sjhstudio.diary.core.ui.diaryBodyFontFamily
import org.sjhstudio.diary.core.ui.diaryBodyTextStyle
import org.sjhstudio.diary.core.ui.rememberFileFontFamily
import org.sjhstudio.diary.feature.settings.resources.Res
import org.sjhstudio.diary.feature.settings.resources.font_download
import org.sjhstudio.diary.feature.settings.resources.font_downloading
import org.sjhstudio.diary.feature.settings.resources.font_failed
import org.sjhstudio.diary.feature.settings.resources.font_offline
import org.sjhstudio.diary.feature.settings.resources.font_preview_label
import org.sjhstudio.diary.feature.settings.resources.font_preview_sample
import org.sjhstudio.diary.feature.settings.resources.settings_font

/**
 * 글꼴(Figma "글꼴 (G1)", "글꼴 상태").
 * 설정 > 글꼴.
 * 맨 위 미리보기 상자는 고른 글꼴과 본문 글자 크기로 일기 문장을 보인다.
 * 줄 이름은 그 글꼴로 그리고, 받기 전 글꼴은 이름 그림과 "받기 · 크기"를 보인다.
 * 누르면 받고, 다 받으면 바로 바뀐다.
 */
@Composable
fun FontSettingsScreen(state: FontUiState, onSelect: (DiaryFont) -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val colors = DiaryTheme.colors
    val selected = state.settings.font
    Column(modifier.fillMaxSize().background(colors.canvas).systemBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        DiaryTopBar(title = stringResource(Res.string.settings_font), onBack = onBack)
        Column(Modifier.widthIn(max = DiaryWriteSize.settingsMax).fillMaxWidth().verticalScroll(rememberScrollState())) {
            Column(
                Modifier
                    .padding(horizontal = DiarySpacing.screenCompact, vertical = DiarySpacing.x2)
                    .fillMaxWidth()
                    .clip(DiaryShapes.r16)
                    .background(colors.sunken)
                    .padding(DiarySpacing.x5),
                verticalArrangement = Arrangement.spacedBy(DiarySpacing.x1)
            ) {
                BasicText(
                    stringResource(Res.string.font_preview_label),
                    style = DiaryTheme.typography.caption.copy(color = colors.textSecondary)
                )
                val installedFamily = rememberFileFontFamily(state.installedPaths[selected])
                BasicText(
                    stringResource(Res.string.font_preview_sample),
                    style = diaryBodyTextStyle(selected, state.settings.bodyTextSize) { installedFamily }.copy(color = colors.textPrimary)
                )
            }
            for (font in FONT_ORDER) FontRow(font, state, selected == font) { onSelect(font) }
        }
    }
}

@Composable
private fun FontRow(font: DiaryFont, state: FontUiState, selected: Boolean, onClick: () -> Unit) {
    val colors = DiaryTheme.colors
    val name = stringResource(font.label())
    val ready = state.isReady(font)
    val downloading = font in state.downloading
    DiarySettingsChoiceRow(
        label = name,
        selected = selected,
        onClick = onClick,
        title = { FontName(font, name, ready, state.installedPaths[font]) },
        trailing =
            when {
                selected -> null

                downloading -> {
                    {
                        val percent = ((state.downloading[font] ?: 0f) * 100).roundToInt()
                        BasicText(
                            stringResource(Res.string.font_downloading, "$percent%"),
                            style = DiaryTheme.typography.caption.copy(color = colors.textSecondary)
                        )
                    }
                }

                !ready -> {
                    {
                        val size = FONT_DOWNLOAD_MB[font]?.let { "${it}MB" }.orEmpty()
                        BasicText(
                            stringResource(Res.string.font_download, size),
                            style = DiaryTheme.typography.caption.copy(color = colors.accentText)
                        )
                    }
                }

                else -> {
                    {}
                }
            }
    )
}

/**
 * 줄 이름.
 * 쓸 수 있는 글꼴은 그 글꼴로 쓰고, 받기 전 글꼴은 그 글꼴로 그린 이름 그림을 글자색으로 물들여 보인다.
 */
@Composable
private fun FontName(font: DiaryFont, name: String, ready: Boolean, installedPath: String?) {
    val colors = DiaryTheme.colors
    val style = DiaryTheme.typography.reading
    val preview = font.namePreview(korean = diaryLanguage() == "ko")
    if (!ready && preview != null) {
        // 그림은 줄 높이 28에 맞춰 그렸다(글자 17).
        // 기기 글자 크기를 따라 같이 커진다.
        val height = with(LocalDensity.current) { style.lineHeight.toDp() }
        Image(
            painterResource(preview),
            contentDescription = null,
            modifier = Modifier.height(height),
            contentScale = ContentScale.FillHeight,
            colorFilter = ColorFilter.tint(colors.textPrimary),
            alignment = Alignment.CenterStart
        )
    } else {
        val installedFamily = rememberFileFontFamily(installedPath)
        val family: FontFamily = diaryBodyFontFamily(font) { installedFamily }
        BasicText(name, style = style.copy(color = colors.textPrimary, fontFamily = family), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * 글꼴 화면과 받기 실패 알림을 잇는다.
 * 앱 틀은 이것만 부른다.
 */
@Composable
fun FontSettingsRoute(onBack: () -> Unit) {
    val viewModel = koinViewModel<FontSettingsViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val toast = remember { DiaryToastState() }
    LaunchedEffect(viewModel) {
        viewModel.notice.collect { notice ->
            toast.show(getString(if (notice == FontNotice.Offline) Res.string.font_offline else Res.string.font_failed))
        }
    }
    Box(Modifier.fillMaxSize()) {
        FontSettingsScreen(state, onSelect = viewModel::select, onBack = onBack)
        DiaryToastHost(toast, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = DiarySpacing.x4))
    }
}
