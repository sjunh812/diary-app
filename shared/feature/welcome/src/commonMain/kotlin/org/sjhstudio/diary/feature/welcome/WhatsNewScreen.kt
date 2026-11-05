package org.sjhstudio.diary.feature.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.sjhstudio.diary.core.designsystem.component.DiarySettingsHeader
import org.sjhstudio.diary.core.designsystem.component.DiarySettingsRow
import org.sjhstudio.diary.core.designsystem.component.DiaryTopBar
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.DiaryWriteSize
import org.sjhstudio.diary.feature.welcome.resources.Res
import org.sjhstudio.diary.feature.welcome.resources.letter_title
import org.sjhstudio.diary.feature.welcome.resources.news_again
import org.sjhstudio.diary.feature.welcome.resources.news_group
import org.sjhstudio.diary.feature.welcome.resources.news_title
import org.sjhstudio.diary.feature.welcome.resources.news_v2_body

/** 설정 > 새 소식(Figma W4). 내용은 앱에 넣고 버전으로 고른다(FR-009). */
@Composable
fun WhatsNewRoute(onBack: () -> Unit, onLetter: () -> Unit, onIntro: () -> Unit, modifier: Modifier = Modifier) {
    koinViewModel<WhatsNewViewModel>()
    WhatsNewScreen(onBack, onLetter, onIntro, modifier)
}

@Composable
internal fun WhatsNewScreen(onBack: () -> Unit, onLetter: () -> Unit, onIntro: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().background(DiaryTheme.colors.canvas).statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        DiaryTopBar(stringResource(Res.string.news_title), onBack = onBack)
        Column(Modifier.widthIn(max = DiaryWriteSize.settingsMax).fillMaxWidth().verticalScroll(rememberScrollState())) {
            DiarySettingsHeader(stringResource(Res.string.news_group))
            DiarySettingsRow(
                stringResource(Res.string.letter_title),
                onClick = onLetter,
                description = stringResource(Res.string.news_v2_body)
            )
            DiarySettingsRow(stringResource(Res.string.news_again), onClick = onIntro)
        }
    }
}
