# 구현 계획: 새 버전 안내

**브랜치**: `feat/app-update-notice` | **작성**: 2026-11-21 | **명세**: [spec.md](spec.md) | **프로토타입**: [prototype.md](prototype.md)(2026-11-21 확정)

## 요약

스토어가 이 기기에 줄 수 있는 새 버전이 있으면 홈 맨 위 카드와 설정 > 앱 정보 > 앱 버전 줄로 알린다. Android는 Play 유연 업데이트로 앱 안에서 받고 [다시 시작하기]로 바꾼다. iOS는 앱 위에 App Store 상품 화면을 띄운다.
판단은 공통 코드의 `AppUpdateRepository` 하나가 맡고, 받기 상태는 앱에 하나라 홈 카드와 설정 줄이 같은 상태를 본다. 일기 DB 스키마와 백업 형식은 바꾸지 않는다.

## 기술 배경

- 축은 스펙 001과 같다(Kotlin, CMP, Koin, DataStore). 새로 들이는 라이브러리는 Play 앱 안 업데이트 하나다.
- Android: `com.google.android.play:app-update-ktx:2.1.0`(무료, `research.md` R1). `AppUpdateManager`로 확인·받기·설치. 동의 창은 Play가 그린다.
- iOS: 새 버전은 App Store 공개 조회(`itunes.apple.com/lookup?id=6835116731&country=<기기 지역>`)를 Ktor(Darwin, 이미 `core/drive`가 씀)로 하루 한 번. App Store 화면은 StoreKit `SKStoreProductViewController`를 Swift 다리(`AppUpdateBridge.swift`)로 띄운다. 리뷰 창 다리(`StoreReviewBridge.swift`)와 같은 방식이다.
- 원격 값: Firebase Remote Config(이미 전면 광고 스위치로 씀). 키 `update_min_version`, `update_note_version`, `update_note_ko`·`_en`·`_ja`(모두 문자열, 플랫폼 조건으로 나눔). JSON을 풀 필요가 없게 언어마다 키를 둔다. 기본값은 모두 빈 문자열. 받는 횟수는 지금처럼 12시간에 한 번이라 늘지 않는다(`research.md` R5).
- 시험: `commonTest`(저장소 판단·버전 비교·조회 응답 해석), `iosTest`(카드 화면), Android 플랫폼 코드는 공통 인터페이스를 가짜로 바꿔 시험한다.

## 헌법 점검

| 원칙 | 이 계획에서 지키는 자리 |
| --- | --- |
| I 기록은 잃지 않는다 | 저장 형식을 건드리지 않는다. 백업 권하기 카드가 새 버전 카드보다 먼저다. 다시 시작은 사용자가 누를 때만, 쓰다 만 글이 없는 화면(홈, 설정)에서만 |
| II 일기는 사용자 것 | 스토어 조회에 앱 번호와 기기 지역만 보낸다. 일기·기기 식별자·계정은 보내지 않는다 |
| III 무료 기능·광고 규칙 | 광고 자리와 빈도를 바꾸지 않는다. 카드는 홈 목록에만 있고 광고가 아니다. 상세·작성 화면에는 없다 |
| IV 돈 드는 서버 없음 | 서버 없음. Play 라이브러리, App Store 조회, StoreKit, Remote Config(Spark) 모두 무료 |
| V 심플 | 새 부품 없이 `DiaryStatusBox`, `DiaryProgressBar`, `DiarySettingsRow`를 쓴다. 한 번에 카드 한 장 |
| VI 어떤 기기에서도 | 폰·태블릿 두 칸 같은 규칙. 진행 표시만 Android에 있다(iOS는 App Store가 진행을 알려 주지 않음, 스펙 Clarifications) |
| VII 근거 | `research.md` R1~R5에 공식 문서 |

## 구조

```text
shared/core/model/.../AppUpdate.kt                 StoreCheck, UpdateNotice, ReleaseNote, AppVersion(이름 비교)
shared/core/datastore/.../PromptStore.kt           닫은 새 버전, 마지막으로 안 새 버전·확인 시각(기기 것, 백업 제외)
shared/core/data/.../AppUpdateRepository.kt        판단과 기록. StoreUpdates·UpdateConfig 인터페이스
shared/app/src/androidMain/.../update/PlayStoreUpdates.kt   AppUpdateManager 감쌈
shared/app/src/androidMain/.../ads/FirebaseAdSwitch.kt      → FirebaseRemote.kt: 광고 스위치와 업데이트 원격 값을 한 번에
shared/app/src/iosMain/.../update/IosStoreUpdates.kt        공개 조회(Ktor) + Swift 다리
shared/app/src/iosMain/.../IosStartup.kt           다리 받기, FirebaseReporting.remoteString
iosApp/iosApp/AppUpdateBridge.swift                App Store 상품 화면
iosApp/iosApp/FirebaseReporter.swift               remoteString, 기본값
shared/app/.../AppRootViewModel.kt                 앱으로 돌아올 때 확인
shared/app/.../scaffold/FeedbackCardViewModel.kt   HomeTop.Update 추가(백업 > 새 버전 > 의견)
shared/app/.../scaffold/UpdateCardViewModel.kt     카드에서 누르는 일
shared/feature/home/.../UpdateCard.kt              카드 화면(+ preview/)
shared/core/designsystem/.../DiarySettingsRow.kt   값 뒤 브랜드색 글자(valueAction)
shared/feature/settings/.../SettingsScreen.kt      앱 버전 줄
shared/app/.../settings/SettingsEntries.kt         앱 버전 줄 연결
shared/feature/backup/.../RestoreProgressScreens.kt  NEEDS_UPDATE 버튼 [스토어에서 업데이트]
composeApp/src/debug/.../DevStoreUpdates.kt        개발자 모드에서 새 버전·받기 흉내
shared/app/src/iosMain/.../IosDebugPlan.kt         -debugUpdate 인자로 흉내
```

## 데이터 모델

`PromptPrefs`에 더하는 값(기기에만, `prompt.preferences_pb`)

| 값 | 뜻 |
| --- | --- |
| `updateDismissed: String?` | [나중에]를 누른 새 버전 id |
| `updateKnown: String?` | 마지막으로 안 새 버전 id(iOS 하루 한 번 조회의 결과) |
| `updateCheckedAt: Instant?` | 마지막으로 스토어에 물은 시각 |

새 버전 id는 Android는 Play가 알려 준 버전 코드, iOS는 App Store 버전 이름이다. 둘 다 지금 앱보다 높을 때만 쓴다.

`UpdateNotice`(화면이 보는 상태)

| 상태 | 홈 카드 | 설정 줄 값 |
| --- | --- | --- |
| `None` | 없음 | 버전만, 누를 수 없음 |
| `Available(id, required, note)` | 닫지 않았거나 `required`면 보임 | "버전 · 업데이트" |
| `Downloading(progress)` | 보임(닫았어도) | "버전 · 받는 중 42%" |
| `Downloaded` | 보임(닫았어도) | "버전 · 다시 시작하기" |
| `Failed(id, required, note)` | 닫지 않았거나 `required`면 보임, 설명이 실패 문구 | "버전 · 업데이트" |

- `required`: 원격 `update_min_version`이 비어 있지 않고 지금 앱 버전 이름보다 높다.
- `note`: 원격 `update_note_version`이 지금 앱 버전보다 높을 때 언어별 문장(`update_note_ko` 등). 화면이 앱 언어(없으면 영어)를 고른다. 두 줄을 넘는지는 출시 때 사람이 본다(`docs/release.md`).
- 확인 실패·오프라인·원격 값 실패는 마지막으로 안 값으로, 아는 값이 없으면 `None`.
- 홈 카드는 처음 확인을 마친 뒤부터 알린다. 그 전에는 홈 맨 위 칸이 백업 안내처럼 읽는 중이라 다른 카드가 먼저 그려졌다 바뀌지 않는다. iOS는 기억한 값으로 먼저 정하고 조회는 5초에서 끊는다.
- 받은 뒤 [나중에]는 이번 실행에서만 숨긴다.

## 흐름

1. 앱으로 돌아올 때(`AppRootViewModel.resumed`, 잠금·이전 관문 뒤) `refresh()`. Android는 매번 Play에 묻는다(기기 안 호출, 받은 상태 확인이 필요). iOS는 지난 확인에서 24시간이 지났을 때만 조회한다.
2. [업데이트](카드·설정 줄) → `start()`. Android는 지금 화면 위에 Play 동의 창을 띄우고, 받기를 고르면 진행을 듣는다. iOS는 App Store 상품 화면을 띄운다.
3. 받기 끝 → `Downloaded`. [다시 시작하기] → `restart()`(`completeUpdate`).
4. [나중에] → `updateDismissed`에 id를 적는다. `required`면 버튼이 없다.
5. 복원 NEEDS_UPDATE → `openStorePage()`. Android는 Play 스토어 앱의 이 앱 화면, iOS는 상품 화면. 진행은 복원 화면에 두지 않는다.

## 문서

- `docs/design.md`: 홈 맨 위 카드 차례(백업 > 새 버전 > 의견), 새 버전 카드와 앱 버전 줄 규칙.
- `docs/release.md`: 출시 때 Remote Config `update_note_*`·`update_min_version` 적는 순서.
- `docs/DECISIONS.md`: 2026-11-21 새 버전 안내 결정(카드 안 A, 강제 안 B, [다시 시작하기], 복원은 스토어로).
- `/release-prep` 스킬: Remote Config 단계 한 줄.

## 열린 결정

- 없음. 전면 광고 원격 스위치를 지울지는 이 기능과 따로 사용자가 정한다(Remote Config 받는 횟수는 키 수와 무관).
