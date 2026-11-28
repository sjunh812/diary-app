package org.sjhstudio.diary.core.designsystem.theme

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.designsystem.resources.Res
import org.sjhstudio.diary.core.designsystem.resources.ds_language

/**
 * 화면 문구의 언어 태그("en", "ko", "ja", "zh-Hant", "es", "pt", "de", "fr", `docs/design.md` 여러 언어)
 * 문자열 리소스가 기기 언어에 맞춰 고른 폴더의 값이라, 프랑스어처럼 우리가 없는 언어면 영어 화면과 같이 "en"
 * 날짜 형식과 글꼴도 이 값을 따라 화면 문구와 같은 언어를 씀
 */
@Composable
fun diaryLanguage(): String = stringResource(Res.string.ds_language)

/** 한자를 Pretendard에 없는 글자로 쓰는 언어. 한 낱말 안에서 글꼴이 바뀌지 않게 시스템 글꼴을 씀 */
fun usesSystemFont(language: String): Boolean = language == "ja" || language.startsWith("zh")

/** 연·월과 오전·오후를 앞에 두는 언어(플랫폼 날짜·시각 형식과 같은 순서) */
fun putsLargerUnitFirst(language: String): Boolean = language == "ko" || usesSystemFont(language)
