package org.sjhstudio.diary.feature.settings

import org.jetbrains.compose.resources.StringResource
import org.sjhstudio.diary.core.model.DiaryFont
import org.sjhstudio.diary.feature.settings.resources.Res
import org.sjhstudio.diary.feature.settings.resources.license_changwon_round
import org.sjhstudio.diary.feature.settings.resources.license_hallym
import org.sjhstudio.diary.feature.settings.resources.license_kyobo
import org.sjhstudio.diary.feature.settings.resources.license_naver
import org.sjhstudio.diary.feature.settings.resources.license_ofl
import org.sjhstudio.diary.feature.settings.resources.license_sejong
import org.sjhstudio.diary.feature.settings.resources.license_watermelon

/**
 * 글꼴 라이선스 갈래
 * 줄 아래에 이름을 보임
 */
internal enum class FontLicense(val label: StringResource) {
    OFL(Res.string.license_ofl),
    NAVER(Res.string.license_naver),
    CHANGWON_ROUND(Res.string.license_changwon_round),
    HALLYM(Res.string.license_hallym),
    WATERMELON(Res.string.license_watermelon),
    SEJONG(Res.string.license_sejong),
    KYOBO(Res.string.license_kyobo)
}

/**
 * 글꼴 하나의 고지(`docs/release.md` 글꼴 라이선스)
 * 전문은 디자인 시스템 모듈의 `files/licenses/<file>`에 받은 원문 그대로 있음
 */
internal data class FontLicenseNotice(val font: DiaryFont, val license: FontLicense, val file: String)

/**
 * 고지 화면의 글꼴 순서(`docs/design.md` 일기 본문 글꼴 표)
 * 시스템 글꼴은 앱에 넣지 않아 없음
 */
internal val FONT_NOTICES: List<FontLicenseNotice> =
    listOf(
        FontLicenseNotice(DiaryFont.PRETENDARD, FontLicense.OFL, "pretendard.txt"),
        FontLicenseNotice(DiaryFont.HAKGYOANSIM_BADASSEUGI, FontLicense.OFL, "hakgyoansim_badasseugi.txt"),
        FontLicenseNotice(DiaryFont.CHANGWON_ROUND, FontLicense.CHANGWON_ROUND, "changwon_round.txt"),
        FontLicenseNotice(DiaryFont.HALLYM_MYUNGJO, FontLicense.HALLYM, "hallym_myungjo.txt"),
        FontLicenseNotice(DiaryFont.GOWUN_BATANG, FontLicense.OFL, "gowun_batang.txt"),
        FontLicenseNotice(DiaryFont.KYOBO_2020, FontLicense.KYOBO, "kyobo_2020.txt"),
        FontLicenseNotice(DiaryFont.NANUM_JEOMGGOL, FontLicense.NAVER, "nanum_jeomggol.txt"),
        FontLicenseNotice(DiaryFont.NANUM_CHEER, FontLicense.NAVER, "nanum_cheer.txt"),
        FontLicenseNotice(DiaryFont.RIX_WATERMELON, FontLicense.WATERMELON, "rix_watermelon.txt"),
        FontLicenseNotice(DiaryFont.SEJONG_GEULGGOT, FontLicense.SEJONG, "sejong_geulggot.txt")
    )
