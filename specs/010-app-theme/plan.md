# 구현 계획: 앱 테마 색

**브랜치**: `feat/app-theme` | **작성**: 2026-11-22 | **명세**: [spec.md](spec.md) | **프로토타입**: [prototype.md](prototype.md)(2026-11-22 확정) | **조사**: [research.md](research.md)

## 요약

설정 > 테마에서 정한 7색(무료)이나 직접 고른 색(Plus)을 고르면 브랜드색 자리 토큰만 바뀌어 앱 전체가 그 색이 된다. 화면 모드도 같은 화면에서 작은 홈 그림으로 고른다.
색 값과 계산은 디자인 시스템 모듈이 갖고(`DiaryBrand`), 앱 뿌리가 설정과 요금제로 어떤 색을 쓸지 정해 `DiaryTheme`에 넘긴다. 일기 DB 스키마는 바꾸지 않고, 설정 값 둘과 백업 설정 칸 둘이 더해진다.

## 기술 배경

- 축은 스펙 001과 같다(Kotlin, CMP, Koin, DataStore). 새 라이브러리는 없다.
- 색 계산: OKLCH 변환과 WCAG 대비를 `shared/core/designsystem`에 직접 둔다(`research.md` 6절). 정한 색은 고정 값이고, 직접 고른 색만 고를 때 한 번 계산한다.
- 색 고르기: Canvas + `pointerInput` + semantics로 직접 그린다(접근성 때문에 라이브러리를 쓰지 않음).
- 결제 화면 그림: `scripts/lottie/make_onboarding.py`의 `plus()`를 고쳐 `plus_benefits.json`을 다시 만든다.
- 시험: `commonTest`(색 계산·대비·정한 색 값, 설정 저장·모르는 값 지키기, 백업 왕복, 혜택 순서), `iosTest`(테마 화면).

## 헌법 점검

| 원칙 | 이 계획에서 지키는 자리 |
| --- | --- |
| I 기록은 잃지 않는다 | 일기 DB는 그대로. 설정 칸 둘은 비어 있으면 라벤더. 모르는 색 이름·깨진 값은 라벤더로 보이고 저장값은 사용자가 다시 고를 때까지 지우지 않는다 |
| II 일기는 사용자 것 | 통계에는 설정 이름과 정한 색 id만(직접 고른 hex는 보내지 않음) |
| III 무료 기능·광고 | 정한 7색은 무료. 지금 쓰던 화면 모드도 무료. 광고 자리 그대로. Plus가 끝나면 직접 고른 색만 지운다(정한 색은 그대로) |
| IV 돈 드는 서버 없음 | 서버 없음 |
| V 심플 | 2026-11-22 예외: 브랜드색 자리 토큰만 바뀐다. 디자인 시스템은 하나 그대로. 견본·그림은 토큰과 기존 부품으로 |
| VI 어떤 기기에서도 | 공통 코드 하나. 동적 색은 넣지 않음. 태블릿은 설정 오른쪽 칸 최대 폭 안 |
| VII 근거 | `research.md` 1~7절 |

## 구조

```text
shared/core/designsystem/.../theme/OkLch.kt          sRGB↔OKLCH, 대비, 명도 찾기(internal)
shared/core/designsystem/.../theme/DiaryBrand.kt     BrandTokens(라이트·다크 10개), 정한 7색 고정 값, custom(seed) 계산
shared/core/designsystem/.../theme/DiaryColors.kt    withBrand(BrandTokens), diaryColors(dark, brand) 공개
shared/core/designsystem/.../theme/DiaryTheme.kt     DiaryTheme(darkTheme, fontFamily, brand = Lavender)
shared/core/designsystem/.../icon/DiaryIcons.kt      Palette(Lucide)
shared/core/model/.../ThemeColor.kt                  정한 7색 + CUSTOM, id
shared/core/model/.../AppSettings.kt                 themeColorId, customThemeColor(원문 문자열) + 풀어 쓰는 값
shared/core/model/.../Plan.kt                        customTheme
shared/core/datastore/.../SettingsStore.kt           theme_color, custom_theme_color
shared/core/data/.../SettingsRepository.kt           setThemeColor, setCustomThemeColor
shared/core/ui/.../ThemeBrand.kt                     ThemeColor → DiaryBrand, 설정·요금제로 쓸 색 정하기
shared/core/backup/.../ManifestCodec.kt              ManifestSettings.themeColor, customThemeColor
shared/app/.../backup/BackupSettingsBridge.kt        담기·적용
shared/app/.../DiaryApp.kt                           요금제와 설정으로 brand를 정해 DiaryTheme에
shared/app/.../settings/SettingsEntries.kt           테마 화면 연결, Plus 결제 화면으로
shared/feature/settings/.../ThemeSettingsScreen.kt   화면 모드 그림 + 스위치 + 견본 8칸
shared/feature/settings/.../ThemeModePicture.kt      작은 홈 그림(목록 보기 셋)
shared/feature/settings/.../ThemeSwatch.kt           견본 칸, 직접 고르기 칸, Plus 칩
shared/feature/settings/.../ColorPickerSheet.kt      색 판·색상 띠·hex·버튼
shared/feature/settings/.../SettingsScreen.kt        "테마" 줄 값
shared/feature/settings/.../SettingsViewModel.kt     canCustomTheme, setThemeColor, setCustomThemeColor
shared/feature/settings/.../preview/ThemePreviews.kt 한국어 먼저, 라이트·다크, 상태마다
shared/feature/plus/.../PlusBenefit.kt               THEME(core) 셋째
shared/feature/plus/.../PlusScreen.kt                아이콘·문구
scripts/lottie/make_onboarding.py                    plus() 다시 짬 → plus_benefits.json
```

## 데이터 모델

| 저장 | 키 | 값 | 없을 때 |
| --- | --- | --- | --- |
| 설정 | `theme_color` | `lavender`·`rose`·`sunset`·`honey`·`forest`·`sea`·`black`·`custom` | 라벤더 |
| 설정 | `custom_theme_color` | `#RRGGBB` | 직접 고르기 칸 비어 있음 |
| 백업 `ManifestSettings` | `themeColor`, `customThemeColor` | 같은 값 | 복원 기기 값 그대로 |

쓸 색: `custom`이고 Plus이며 직접 고른 색이 바르면 그 색, 아니면 정한 색(모르는 이름·깨진 값·Plus 아님은 라벤더).

## 위험과 막는 방법

- **라벤더가 지금과 달라짐**: 라벤더 토큰을 지금 값과 하나하나 비교하는 시험.
- **직접 고른 색의 대비 부족**: 색상 0~360°를 15° 간격으로, 명도·채도 경계 색까지 돌며 FR-010 대비를 시험.
- **설정 쓰기가 모르는 값을 지움**: 원문 문자열을 그대로 다시 쓰는 시험.
- **시작 때 색이 스침**: 설정과 요금제는 시작 단계에서 이미 읽어 첫 프레임부터 같은 색.
- **색을 바꿀 때 앱 전체 다시 그리기**: 고를 때 한 번뿐. 견본을 연타해도 저장은 마지막 값.
