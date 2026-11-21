# 조사: 새 버전 안내

2026-11-21. 공식 문서에서 확인한 것만 적고, 확인하지 못한 것은 못 했다고 쓴다.

## R1 Android: Play 앱 안 업데이트

출처: [개요](https://developer.android.com/guide/playcore/in-app-updates), [Kotlin](https://developer.android.com/guide/playcore/in-app-updates/kotlin-java), [시험](https://developer.android.com/guide/playcore/in-app-updates/test), [FakeAppUpdateManager](https://developer.android.com/reference/com/google/android/play/core/appupdate/testing/FakeAppUpdateManager)

- 유연 흐름은 Play 동의 창을 거쳐 뒤에서 받고, 그동안 앱을 계속 쓴다. 즉시 흐름은 전체 화면으로 설치와 재시작까지 Play가 한다.
- 다 받아도 Play가 앱을 다시 켜지 않는다. 문서는 알림(스낵바와 [RESTART])으로 확인을 받고 `completeUpdate()`를 부르라고 한다. 앞에 있을 때 부르면 전체 화면 뒤 메인 액티비티로 다시 켜진다.
- 앱이 앞으로 올 때마다 받은 상태인지 보라고 한다. 설치하지 않으면 받은 파일이 공간을 차지한다.
- 진행 표시와 설치 안내는 앱이 그린다. 동의 창은 건너뛸 수 없다.
- `updateAvailability`(AVAILABLE 등), `clientVersionStalenessDays`(Play가 새 버전을 안 지 며칠), `updatePriority`(0~5, Play Developer API로 출시할 때만 정함, 내부 앱 공유에서는 못 씀).
- 조건: Play에서 받은 앱, 같은 applicationId·서명 키, 더 높은 versionCode. 시험은 내부 앱 공유, 단위 시험은 `FakeAppUpdateManager`.
- 라이브러리: `com.google.android.play:app-update-ktx:2.1.0`(Google Maven 최신).
- 거절 뒤 다시 묻는 빈도의 숫자 권장은 없다. "너무 자주 묻지 말 것", "거절하면 업데이트 없이 계속 쓰게 하고 나중에 다시 물을 것"만 있다.
- Play 정책: 앱은 Play의 업데이트 방식 말고는 스스로 업데이트할 수 없다([정책](https://support.google.com/googleplay/android-developer/answer/9888379)). 이 기능은 Play 방식만 쓴다.

**결정**: 유연 흐름. 받은 뒤 설치는 사용자가 누르는 [다시 시작하기]로만(스낵바 대신 같은 카드와 설정 줄이 바뀐다).

## R2 iOS: 새 버전을 아는 방법

| 방법 | 장점 | 단점 |
| --- | --- | --- |
| App Store 공개 조회 `itunes.apple.com/lookup?id=6835116731&country=<스토어 국가>` | 출시하면 저절로 맞음. 손으로 적을 것이 없음 | `version` 필드 설명이 문서에 없음. 반영 지연 근거 없음. 분당 약 20회 한도, 캐시 권장([문서](https://performance-partners.apple.com/search-api)) |
| Firebase Remote Config `latest_version`·`minimum_version` | Firebase 공식 사례가 있음([문서](https://firebase.google.com/docs/remote-config/use-cases/minimum-version)). 이미 쓰는 도구 | 출시마다 손으로 적어야 함. 심사 통과 시각과 어긋나기 쉬움 |

- 2026-11-21 `lookup?bundleId=com.apple.Pages&country=kr`를 불러 `version`, `currentVersionReleaseDate`, `trackViewUrl`이 오는 것을 확인했다. 문서에 있는 인자는 `id`라 `id`로 부른다.
- 심사 지침에 업데이트 안내를 막는 조항은 없다. 3.2.2(x)는 평가·리뷰 같은 스토어 행동을 기능 조건으로 강제하지 못하게 한다([지침](https://developer.apple.com/app-store/review/guidelines/)). 업데이트가 여기에 드는지는 문구로 알 수 없어 앱을 막는 화면은 피한다.
- HIG에 업데이트 안내 항목은 없다. "앱이 시작할 때 알림 창을 띄우지 말라", "정보만 주는 알림 창은 피하라"([Alerts](https://developer.apple.com/design/human-interface-guidelines/alerts)).
- iOS 자동 업데이트는 기본으로 켜져 있다([지원](https://support.apple.com/102629)). 끈 사람이 이 기능의 대상이다.

**결정**: 새 버전 유무는 공개 조회(하루 한 번, 마지막 값 저장). 한 줄 소식과 꼭 필요한 업데이트 표시는 Remote Config. App Store 화면은 StoreKit의 앱 위 상품 화면(`SKStoreProductViewController`)으로 띄운다.

## R3 화면 레퍼런스

- Firebase 사례 문서: 선택 업데이트는 닫을 수 있는 배너와 [나중에], 강제는 닫을 수 없는 창.
- Android 문서: 받은 뒤 스낵바와 [RESTART].
- Telegram: 업데이트 정보 API에 `can_not_skip`이 있다([문서](https://core.telegram.org/method/help.getAppUpdate)). 채팅 목록 위 "Update Telegram" 줄은 웹판 버그 기록에서만 확인했다([기록](https://bugs.telegram.org/c/63085)). 앱의 공식 설명은 찾지 못했다.
- 토스: 미니앱 문서의 `isMinVersionSupported` 예시 문구 "최신 버전으로 업데이트가 필요해요."([문서](https://developers-apps-in-toss.toss.im/documentation/common/network-environment/version)). 토스·당근·배민 기술 블로그에 업데이트 안내 설계 글은 찾지 못했다.
- Material 3 배너·스낵바 페이지는 읽지 못했다(스크립트로 그리는 화면). Day One 지원 페이지에서도 관련 설명을 찾지 못했다.

**결정**: 홈 맨 위 카드(이미 있는 상태 상자 부품)와 설정 줄. 시작할 때 뜨는 대화상자는 쓰지 않는다. 받기 진행과 다시 열기를 같은 카드 안에서 바꿔 보여 줘 창을 늘리지 않는다.

## R5 Remote Config 요금과 못 받을 때

출처: [요금](https://firebase.google.com/pricing), [요금제](https://firebase.google.com/docs/projects/billing/firebase-pricing-plans), [불러오기](https://firebase.google.com/docs/remote-config/loading), [Android 시작](https://firebase.google.com/docs/remote-config/android/get-started)

- 2026-09-01부터 Remote Config는 프로젝트당 하루 10만 번까지 무료다. Blaze는 넘는 만큼 요청당 $0.000006, Spark는 넘으면 그달 남은 동안 그 제품이 꺼진다.
- 이 프로젝트는 Spark다(`docs/release.md`). 결제 계정을 연결하지 않으면 Blaze로 바뀌지 않아 요금이 나갈 길이 없다.
- 값을 12시간에 한 번만 받는다(`FirebaseAdSwitch.kt`, 기본 최소 간격도 12시간). 기기 하나가 하루 두 번 넘게 묻지 않으므로 하루 활성 기기 5만 대까지는 한도 안이다.
- 받지 못하면 앱 안 기본값(`setDefaultsAsync`)을 쓴다. 문서는 네트워크에 기대지 말고 기본값을 꼭 두라고 한다.
- 기본값: 한 줄 소식 없음, 최소 버전 없음. 받지 못하면 기본 문구 카드가 되고 강제는 걸리지 않는다. 덜 묻는 쪽으로 실패한다.

## R4 꼭 필요한 업데이트

- Firebase: API가 깨지는 변경, 심각한 보안 문제, 서비스 종료일 때. 작은 시험군에서 먼저 확인.
- Android: 즉시 흐름은 업데이트가 핵심 기능에 꼭 필요할 때만.
- 이 앱은 서버가 없어 API 호환으로 막을 일이 거의 없다. 새 백업 형식은 이미 옛 앱이 "앱을 업데이트하면 복원할 수 있어요"로 막는다(`ManifestCheck.NEEDS_NEWER_APP`).
