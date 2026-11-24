# 조사: 앱 테마 색

스펙 010의 근거다. 공식 문서·공식 저장소·공식 블로그·스토어에서 확인한 것만 적고, 확인하지 못한 것은 그렇다고 적었다(2026-11-22). 조사 원문(앱 38개 표, 디자인 시스템 수치, 미리보기 모양, 공개 팔레트 hex, 기술 검토)은 세션 노트 `theme-ref-*.md`, `theme-tech.md`에 있고 핵심만 옮긴다.

## 1. 테마 색을 주는 앱

| 앱 | 가짓수 | 직접 고르기 | 무료/유료 | 미리보기 | 출처 |
| --- | --- | --- | --- | --- | --- |
| Day One | 저널 테마 9, 글자색 11("일부러 묶음") | 없음 | 테마 2 무료 / 7 유료 | 테마 카드에 고정 견본 글을 그 테마로 그림 | [Journal Themes](https://dayoneapp.com/guides/tips-and-tutorials/journal-themes/), [Text and Highlight Colors](https://dayoneapp.com/guides/tips-and-tutorials/text-and-highlight-colors/), [Dark Mode](https://dayoneapp.com/guides/settings/dark-mode/) |
| Bear | 무료 3 + Pro 30여 | 없음 | 기본·다크·고대비만 무료 | 라이트용·다크용을 따로 기억 | [Free and Pro themes](https://bear.app/faq/about-free-and-pro-themes-in-bear/), [Bear iOS 13](https://blog.bear.app/2019/10/bear-is-ready-for-ios-13-ipados-and-catalina/) |
| Todoist | 7 + 숨은 1, 짧은 자연·음식 이름 | 없음 | 무료 3 / Pro 4 | 막대·원으로 줄인 카드 | [Change your color theme](https://www.todoist.com/help/articles/change-color-themes-zD0N5K) |
| Daylio | 확인 못 함 | "custom colors" | 추가 테마·직접 색 Premium | 확인 못 함 | [Premium features](https://daylio.net/faq/docs/daylio-faq/about/daylio-premium-features/) |
| Telegram | 이름 색 7 등 | 색상환·편집기 | 일부 Premium | 목록은 말풍선 그림, 미리보기는 가짜 대화 | [Telegram colors](https://core.telegram.org/api/colors), [ThemePreviewActivity](https://raw.githubusercontent.com/DrKLO/Telegram/master/TMessagesProj/src/main/java/org/telegram/ui/ThemePreviewActivity.java) |
| Discord | 무료 4 + Nitro 28 + 직접 | 색 5개까지 섞기 | 직접 만들기 Nitro, 미리보기는 누구나 | 바로 적용 | [Nitro themes](https://discord.com/blog/bring-your-vibe-to-discord-with-new-themes-in-nitro) |
| Linear | 테마 + 직접 | base·accent·contrast 3개로 LCH 생성 | 무료 | 바로 적용 | [How we redesigned the Linear UI](https://linear.app/blog/how-we-redesigned-the-linear-ui) |
| Things 3, Notion, 토스 | 색 고르기 없음(화면 모드만) | - | - | - | [Things blog](https://culturedcode.com/things/blog/2024/09/things-for-the-latest-os-releases/), [Notion appearance](https://Notion.com/help/appearance-settings) |
| iOS 디스플레이 및 밝기 | 라이트·다크 | - | - | 그림 두 장이 곧 모드 고르기, 그림은 추상 | [HIG Color](https://developer.apple.com/design/human-interface-guidelines/color) |

정리: 강조색만 바꾸는 앱은 5~12가지가 흔하다. 직접 고르는 색은 유료 쪽에 둔다. 다크는 고른 색에서 따로 만든다. 여러 개를 비교하는 작은 썸네일은 추상 그림이고, 크게 보이는 미리보기는 실제 꼴에 견본 내용을 넣는다. 미리보기에 사용자의 실제 일기를 쓰는 일기 앱은 찾지 못했다. 당근·카카오뱅크·Reflectly·Stoic의 색 테마, X의 강조색 공식 자료는 찾지 못했다.

## 2. 디자인 시스템과 대비 기준

| 출처 | 확인한 것 | 출처 |
| --- | --- | --- |
| Material 3 | TonalSpot: 고른 색의 hue에 chroma 36, primary 톤 40/80, onPrimary 100/20, primaryContainer 90/30. 흰 글자는 톤 49 이하. 탁한 노랑-초록(hue 90~111, chroma>16, tone<65)은 tone 70으로 | [material-color-utilities](https://github.com/material-foundation/material-color-utilities), [dislike_analyzer.ts](https://github.com/material-foundation/material-color-utilities/blob/main/typescript/dislike/dislike_analyzer.ts) |
| Apple HIG | 최소 대비 17pt 이하 4.5:1, 18pt 이상 3:1, 굵은 글자 3:1. 색만으로 알리지 않는다. 대비 높이기에 더 진한 판을 둔다 | [HIG Accessibility](https://developer.apple.com/design/human-interface-guidelines/accessibility), [HIG Color](https://developer.apple.com/design/human-interface-guidelines/color) |
| Radix Colors | 9 solid, 11·12 글자, 4·5 고른 바탕. 사용자 색 생성기는 OKLCH, 흰 글자가 약하면 어두운 글자 | [generate-radix-colors.tsx](https://github.com/radix-ui/website/blob/main/components/generate-radix-colors.tsx) |
| 토스 TDS | OKLCH로 같은 단계 명도를 맞추고, sRGB 밖이면 hue·명도는 두고 채도만 줄임 | 세션 노트 `theme-ref-design-systems.md` 4-1 |
| 당근 SEED | brand solid `#ff6600`, 흰 글자 약 2.9:1(APCA 기준) | [seed-design color.yaml](https://github.com/daangn/seed-design/blob/dev/packages/rootage/color.yaml) |
| Atlassian 브랜드 테마 | 고른 색이 흰색 대비 4.5 이상이면 그대로, 아니면 단계 내림 | 세션 노트 5-2 |

토스·당근 목표 대비 숫자, m3.material.io 본문, macOS 강조색 이름 목록은 확인하지 못했다. HIG의 "굵은 글자"가 SemiBold(600)를 포함하는지 확인하지 못했다.

## 3. 공개 팔레트(정한 색의 출발점)

Day One 저널 테마에 쓰인 공개 팔레트의 원문 값이다: Nord nord7 `#8fbcbb`, nord10 `#5e81ac`, nord12 `#d08770`, nord13 `#ebcb8b`, nord15 `#b48ead`([nord.css](https://raw.githubusercontent.com/nordtheme/nord/develop/src/nord.css)), Rosé Pine dawn love `#b4637a`([palette.json](https://raw.githubusercontent.com/rose-pine/palette/main/palette.json)), Everforest green·aqua([palette.md](https://raw.githubusercontent.com/sainnhe/everforest/master/palette.md)). Apple HIG Brown `rgb(172,127,94)`.

## 4. 계산 규칙(OKLCH)

고른 색(sRGB) → OKLCH(L, C, H). 대비는 WCAG 2.x 상대 휘도 식. 바탕: 라이트 `canvas #FFFFFF`, 다크 `canvas #16171B`, 글자 `#1A1C20`.

| 토큰 | 규칙 |
| --- | --- |
| 정리 | C < 0.03이면 회색: H = 262(SEED 회색 색조), C = 0.012, L < 0.5면 검정 값. 아니면 계산하는 면(고른 칸, 카드 면, 편지 면, 글자)에만 C = min(C, 0.19). 라이트 버튼 면은 고른 색 그대로. sRGB 밖이면 C만 줄임 |
| `accent` 라이트 | 고른 그대로. 위 글자는 흰색·검은색 중 APCA |Lc|가 큰 쪽(갈림은 화면 밝기 Y≈0.36, 그때 둘 다 Lc 52 안팎). 흰 바탕과 1.3:1 미만이면 1.3:1이 되게 진하게(2026-11-22: 2.4~3:1을 진하게 하던 것과 L 0.38로 올리던 것을 뺌) |
| `onAccent` | 위 결과대로 `#FFFFFF` 또는 `#1A1C20` |
| `selected` 라이트 | L 0.932, C = min(0.21C, 0.045) (회색 테마는 L 0.90) |
| `accentIcon` | 라이트는 `accent`가 `sunken`(`#F3F4F5`)·`selected` 위 3:1 이상이면 그대로, 아니면 같은 색상·채도로 3:1이 되는 가장 밝은 L(별·고리·커서가 묻히지 않게). 다크는 다크 `accent`가 카드 면(`#1D2025`)·`sunken`(`#2B2E35`)·`selected` 위 3:1이 되게 밝힌 값 |
| `accentText` 라이트 | C 0.9배, `selected` 위 5.0:1·흰 바탕 4.5:1을 넘는 가장 밝은 L(≤ min(accent L, 0.62)). 노랑(H 70~105)은 H−25(최소 70), 연두(105~125)는 H+20 |
| `surfaceCard` 라이트 | L 0.981, C = min(0.05C, 0.010) |
| `lilac` | L 0.789, C = min(0.71C, 0.13) |
| `toastAction` 라이트 | C 0.7배, 토스트 `#2A3038` 위 6.0:1이 되는 가장 어두운 L |
| `accent` 다크 | 라이트 값이 다크 바탕과 3:1 이상이면 그대로, 아니면 3:1이 되는 가장 어두운 L(그 위 글자는 위 규칙을 다시 적용) |
| `accentText`·`toastAction` 다크 | C 0.7배, 다크 바탕 위 8.6:1이 되는 가장 어두운 L |
| `selected` 다크 | L 0.271, C = min(0.36C, 0.065) (회색 0.36) |
| `paper`·`envelopeFlap` 다크 | L 0.377·0.345, C = min(0.30C, 0.054)·min(0.49C, 0.088) |
| `toneGood` | `accentText`와 같음 |

라벤더 씨앗 `#6E56CF`로 돌리면 accent `#6E56CF`, accentText `#6450BA`, 다크 accentText `#B3AAFF`로 지금 값과 거의 같다. 라벤더는 지금 값을 그대로 고정한다.

## 5. 정한 7색

정한 색은 화면에 칠해 견주며 고른 고정 값이다. 11가지까지 넓혔다가 구독을 이끌려고 색 계열마다 하나씩 7가지로 줄였고(자두·코코아·연두·청록 뺌), 같은 날 노을·꿀이 둘 다 흐린 따뜻한 색이라 헷갈려 다시 골랐다. 자연색(로즈·귤·올리브·청록·남색)은 라벤더 옆에서 흐려 보였고, 또렷한 인기 강조색(macOS 강조색 차례: 분홍·빨강·주황·초록·파랑)은 빨강이 삭제 빨강과 겹쳤다. 울트라에서 견준 끝에 지금 앱의 라일락·기분 파스텔과 어울리는 파스텔 묶음에 검정을 더하고, 진하게 맞춘 색에 옅은 것 이름(라일락·복숭아·세이지·하늘)이 붙어 어긋나 자두·단풍·숲·바다로 이름을 고쳤다(견주기 판은 지우고 고른 파스텔 묶음 판만 Figma "시안 · 테마 색"에 남김). 1절처럼 무료 색을 적게 두는 앱(Todoist 무료 3, Bear 무료 3)과 같은 방향이다. 버튼 위 흰 글자를 4.5:1 이상으로 맞추고, 4절 규칙은 직접 고른 색에 쓴다.

| 색 | 출발 | accent / 위 글자 | accentText | selected | 다크 accent / 위 글자 | 다크 accentText | 다크 selected | 대비(위 글자 · 브랜드 글자 · 다크 바탕 · 다크 위 글자 · 다크 글자) |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 라벤더 | 지금 값 | `#6E56CF` / `#FFFFFF` | `#6550B9` | `#EBE4FF` | `#6E56CF` / `#FFFFFF` | `#BAA7FF` | `#291F43` | 5.4 · 5.0 · 3.3 · 5.4 · 7.3 |
| 자두 | 파스텔 라일락 `#B07CC6`을 흰 글자 5.4:1까지 | `#88569D` / `#FFFFFF` | `#7B4F8E` | `#EFE4F4` | `#88569D` / `#FFFFFF` | `#CCA7DC` | `#2F2035` | 5.4 · 5.1 · 3.3 · 5.4 · 7.3 |
| 로즈 | 파스텔 로즈 `#C9667B`를 흰 글자 5.4:1까지 | `#AA4A60` / `#FFFFFF` | `#9A4558` | `#FAE2E5` | `#AA4A60` / `#FFFFFF` | `#E9A0AC` | `#3A1D22` | 5.4 · 5.1 · 3.3 · 5.4 · 7.3 |
| 단풍 | 파스텔 복숭아 `#E8956B`를 흰 글자 5.4:1까지 | `#A2552C` / `#FFFFFF` | `#924E2A` | `#F7E4DB` | `#A2552C` / `#FFFFFF` | `#E2A78A` | `#372014` | 5.4 · 5.1 · 3.3 · 5.4 · 7.3 |
| 숲 | 파스텔 세이지 `#7FA68A`를 흰 글자 5.4:1까지 | `#407552` / `#FFFFFF` | `#3C6A4B` | `#E1ECE3` | `#407552` / `#FFFFFF` | `#97BDA2` | `#1C2B20` | 5.4 · 5.2 · 3.3 · 5.4 · 7.1 |
| 바다 | 파스텔 하늘 `#6B9AC4`를 흰 글자 5.4:1까지 | `#406E96` / `#FFFFFF` | `#3C6487` | `#E0EAF4` | `#406E96` / `#FFFFFF` | `#97B8D6` | `#1B2834` | 5.4 · 5.1 · 3.3 · 5.4 · 7.2 |
| 검정 | SEED gray 계열 | `#2B2D33` / `#FFFFFF` | `#2B2D33` | `#DCDEE2` | `#E6E8EB` / `#16171B` | `#E6E8EB` | `#3A3D44` | 13.8 · 10.2 · 14.6 · 14.6 · 8.9 |

나머지 토큰(`surfaceCard`, `lilac`, `toastAction`, `paper`, `envelopeFlap`)은 4절 규칙의 계산 값을 개발 때 고정 값으로 넣는다.

## 6. 기술 검토

- **가능 여부**: 가능하다. 색 고르기 화면과 토큰 계산 모두 `commonMain` 하나로 Android·iOS에 같이 된다(Kotlin 2.4.21, CMP 1.12.2).
- **색 계산**: OKLCH 변환(Björn Ottosson 공식, CSS Color 4)과 대비 식을 `shared/core/designsystem`에 직접 둔다(100줄 안팎). 토스·Radix가 OKLCH로 단계를 맞추는 것과 같은 축이고, 규칙이 "대비를 넘는 가장 밝은/어두운 명도 찾기"라 HCT 라이브러리가 필요 없다. 비교안 `com.materialkolor:material-color-utilities:6.0.0`(Apache-2.0, KMP)은 HCT·DislikeAnalyzer가 있지만 우리 규칙에 쓰는 것은 변환뿐이라 의존을 늘리지 않는다.
- **색 고르기**: 직접 그린다(Canvas + pointerInput + semantics). `skydoves/colorpicker-compose:1.3.0`은 iOS를 지원하지만 접근성 semantics가 없어 TalkBack·VoiceOver로 고를 수 없다([HsvColorPicker.kt](https://github.com/skydoves/colorpicker-compose/blob/main/colorpicker-compose/src/commonMain/kotlin/com/github/skydoves/colorpicker/compose/HsvColorPicker.kt)). godaddy 피커는 iOS가 없다.
- **바탕화면 색 따르기**: 넣지 않는다. Android 12 이상에서만 되고(minSdk 24), iOS에는 공개 API가 없다([Dynamic colors](https://developer.android.com/develop/ui/views/theming/dynamic-colors)). 헌법 VI.
- **성능**: `LocalDiaryColors`는 static이라 색을 바꾸면 앱 전체가 다시 그려지지만 고를 때 한 번뿐이다. 정한 색은 고정 값이라 계산이 없다. 시작 때는 지금처럼 설정을 먼저 읽고 바탕만 그려 다른 색이 스치지 않는다.
- **영향 범위**: 화면은 모두 토큰을 거친다. 고칠 곳은 `DiaryColors`·`DiaryTheme`·`AppSettings`/`SettingsStore`·`DiaryApp`·설정 화면·백업 설정 묶음·Plus 혜택·결제 화면 로띠다. 위젯은 아직 없다.

## 7. 열린 결정

- 대비 높이기(Android `getContrast`, iOS 대비 높이기)에 목표 대비를 올린 판을 둘지: 이번 범위 밖. 사용자가 원하면 다음 스펙.
- HIG "굵은 글자"에 SemiBold(600)가 드는지: 확인 못 함. 버튼 글자는 지금 굵기를 쓰기로 했다(사용자 결정).
