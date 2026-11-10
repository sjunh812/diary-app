package org.sjhstudio.diary.core.ui

import android.content.Context
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.createFontFamilyResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import org.sjhstudio.diary.core.designsystem.resources.Res
import org.sjhstudio.diary.core.designsystem.resources.ds_language
import org.sjhstudio.diary.core.model.DiaryFont
import org.sjhstudio.diary.core.model.orElseOnFailure

/**
 * 안드로이드 글꼴 미리 읽기.
 * `createFontFamilyResolver`로 만든 것은 화면이 쓰는 것과 글꼴 캐시를 함께 쓴다(Android 공식 문서).
 * 화면의 글꼴과 같은 글꼴로 만들어야 캐시가 맞는다.
 * 앱에 넣은 글꼴은 Compose 리소스와 같은 자산 경로로, 받은 글꼴은 화면과 같은 객체([cachedFontFamily])로 만든다.
 */
class AndroidFontWarmup(context: Context, private val installedPath: (DiaryFont) -> String?) : FontWarmup {
    private val app = context.applicationContext
    private val resolver by lazy { createFontFamilyResolver(app) }

    override suspend fun warm(font: DiaryFont) {
        withContext(Dispatchers.Default) {
            orElseOnFailure(Unit) { familyFor(font)?.let { resolver.preload(it) } }
        }
    }

    private suspend fun familyFor(font: DiaryFont): FontFamily? = when {
        font == DiaryFont.SYSTEM -> null

        // 기본 글꼴이면 테마가 Pretendard를 쓴다. 일본어 화면은 시스템 글꼴이다.
        font == DiaryFont.PRETENDARD -> if (getString(Res.string.ds_language) == "ja") null else pretendard()

        font in BUNDLED_FONT_FILES -> FontFamily(Font(asset(BUNDLED_FONT_FILES.getValue(font)), app.assets))

        else -> installedPath(font)?.let(::cachedFontFamily)
    }

    // pretendardFamily()와 같은 파일, 같은 굵기다.
    private fun pretendard() = FontFamily(
        Font(asset("pretendard_regular.otf"), app.assets, FontWeight.Normal),
        Font(asset("pretendard_medium.otf"), app.assets, FontWeight.Medium),
        Font(asset("pretendard_semibold.otf"), app.assets, FontWeight.SemiBold),
        Font(asset("pretendard_bold.otf"), app.assets, FontWeight.Bold)
    )

    // Compose 리소스의 Font()가 쓰는 자산 경로와 같다.
    private fun asset(file: String) = Res.getUri("font/$file").removePrefix(ANDROID_ASSET)

    private companion object {
        const val ANDROID_ASSET = "file:///android_asset/"
    }
}
