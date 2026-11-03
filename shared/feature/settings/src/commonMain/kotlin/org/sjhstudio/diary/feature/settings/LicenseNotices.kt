package org.sjhstudio.diary.feature.settings

import org.jetbrains.compose.resources.StringResource
import org.sjhstudio.diary.core.model.DiaryFont
import org.sjhstudio.diary.feature.settings.resources.Res
import org.sjhstudio.diary.feature.settings.resources.license_kogl
import org.sjhstudio.diary.feature.settings.resources.license_naver
import org.sjhstudio.diary.feature.settings.resources.license_ofl

/** 글꼴 라이선스 갈래. 줄 아래에 이름을 보인다. */
internal enum class FontLicense(val label: StringResource) {
    OFL(Res.string.license_ofl),
    NAVER(Res.string.license_naver),
    KOGL(Res.string.license_kogl)
}

/**
 * 글꼴 하나의 고지(`docs/release.md` 글꼴 라이선스).
 * 전문은 디자인 시스템 모듈의 `files/licenses/<file>`에 받은 원문 그대로 있다.
 */
internal data class FontLicenseNotice(val font: DiaryFont, val license: FontLicense, val file: String)

/** 고지 화면의 글꼴 순서(`docs/design.md` 일기 본문 글꼴 표). 시스템 글꼴은 앱에 넣지 않아 없다. */
internal val FONT_NOTICES: List<FontLicenseNotice> =
    listOf(
        FontLicenseNotice(DiaryFont.PRETENDARD, FontLicense.OFL, "pretendard.txt"),
        FontLicenseNotice(DiaryFont.GOWUN_DODUM, FontLicense.OFL, "gowun_dodum.txt"),
        FontLicenseNotice(DiaryFont.CAFE24_SURROUND, FontLicense.OFL, "cafe24_surround.txt"),
        FontLicenseNotice(DiaryFont.MARU_BURI, FontLicense.NAVER, "maru_buri.txt"),
        FontLicenseNotice(DiaryFont.GOWUN_BATANG, FontLicense.OFL, "gowun_batang.txt"),
        FontLicenseNotice(DiaryFont.NANUM_PEN, FontLicense.OFL, "nanum_pen.txt"),
        FontLicenseNotice(DiaryFont.NANUM_JEOMGGOL, FontLicense.NAVER, "nanum_jeomggol.txt"),
        FontLicenseNotice(DiaryFont.KKOMI, FontLicense.KOGL, "kkomi.txt"),
        FontLicenseNotice(DiaryFont.GAMJA_FLOWER, FontLicense.OFL, "gamja_flower.txt"),
        FontLicenseNotice(DiaryFont.HI_MELODY, FontLicense.OFL, "hi_melody.txt")
    )
