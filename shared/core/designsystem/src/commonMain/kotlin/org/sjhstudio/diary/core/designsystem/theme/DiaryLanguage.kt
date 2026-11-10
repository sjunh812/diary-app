package org.sjhstudio.diary.core.designsystem.theme

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.designsystem.resources.Res
import org.sjhstudio.diary.core.designsystem.resources.ds_language

/**
 * 화면 문구의 언어 태그("en", "ko", "ja", `docs/design.md` 여러 언어).
 * 문자열 리소스가 기기 언어에 맞춰 고른 폴더의 값이라, 프랑스어처럼 우리가 없는 언어면 영어 화면과 같이 "en".
 * 날짜 형식과 글꼴도 이 값을 따라 화면 문구와 같은 언어를 씀.
 */
@Composable
fun diaryLanguage(): String = stringResource(Res.string.ds_language)
