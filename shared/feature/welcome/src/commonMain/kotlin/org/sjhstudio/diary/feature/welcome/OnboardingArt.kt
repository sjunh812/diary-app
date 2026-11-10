package org.sjhstudio.diary.feature.welcome

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.sjhstudio.diary.core.designsystem.theme.DiaryArtSize
import org.sjhstudio.diary.core.ui.DiaryLottie
import org.sjhstudio.diary.feature.welcome.resources.Res

// 온보딩 그림. 글자가 없어 언어를 타지 않고, 색은 토큰을 따라 다크도 맞음
// 재생 시간은 로띠 프레임 수 / 30fps(scripts/lottie/make_onboarding.py)

/** 기분 고르기, 글쓰기, 저장 */
@Composable
internal fun WriteArt(active: Boolean, modifier: Modifier = Modifier) = OnboardingLottie("onboarding_write", 4_000, active, modifier)

/** 휴대폰의 글과 사진이 내 드라이브로. 휴대폰 쪽 기록은 그대로 */
@Composable
internal fun BackupArt(active: Boolean, modifier: Modifier = Modifier) = OnboardingLottie("onboarding_backup", 3_200, active, modifier)

/** 도구 막대의 굵게·글자색·형광펜·목록이 차례로 눌리며 줄이 바뀜 */
@Composable
internal fun StyleArt(active: Boolean, modifier: Modifier = Modifier) = OnboardingLottie("onboarding_style", 3_333, active, modifier)

/** 기분·날씨 칩을 고르면 목록이 걸러지고 기분 막대가 올라옴 */
@Composable
internal fun FindArt(active: Boolean, modifier: Modifier = Modifier) = OnboardingLottie("onboarding_find", 3_333, active, modifier)

/** 휴지통에 들어간 일기가 제자리로 돌아옴 */
@Composable
internal fun TrashArt(active: Boolean, modifier: Modifier = Modifier) = OnboardingLottie("onboarding_trash", 3_467, active, modifier)

/** 폰 화면이 넓어지고 목록 옆에 일기가 열림 */
@Composable
internal fun TabletArt(active: Boolean, modifier: Modifier = Modifier) = OnboardingLottie("onboarding_tablet", 3_333, active, modifier)

@Composable
private fun OnboardingLottie(name: String, millis: Int, active: Boolean, modifier: Modifier = Modifier) =
    DiaryLottie({ Res.readBytes("files/$name.json") }, millis, active, modifier.size(DiaryArtSize.width, DiaryArtSize.height))
