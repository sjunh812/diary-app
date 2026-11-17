---
name: "cmp-architecture"
description: "1일 1일기 코드를 쓰거나 고칠 때 따르는 구조·기술 기준. Now in Android와 JetBrains KotlinConf 앱을 열어 견준 결과로, 모듈 자리, 의존성 주입, 화면 모델과 단방향 흐름, 코루틴 디스패처, 화면 전환, 데이터 계층, 시험, 성능, iOS 연동을 정한다. 새 모듈·화면·저장소·화면 모델을 만들거나, 구조를 바꾸거나, 라이브러리를 들이거나, 코드 리뷰에서 구조·성능을 볼 때 쓴다."
argument-hint: "보려는 영역(예: '새 기능 모듈', '화면 모델', '시험', 'iOS') 또는 비움"
user-invocable: true
disable-model-invocation: false
---

# 구조와 기술 기준

혼자 유지보수하는 Android + iOS 앱이다. 기준은 유지보수와 성능이 먼저고, 널리 쓰이고 검증된 방법만 쓴다.
근거 저장소: [Now in Android](https://github.com/android/nowinandroid)(NIA, `2982e63`), [KotlinConf 앱](https://github.com/JetBrains/kotlinconf-app)(KC, `146e891`).
처음 비교는 `specs/001-foundation-migration/research.md` "구조 비교", 다시 본 날은 2026-11-17이다.

## 언제 펼치나

- 새 모듈, 화면, 저장소, 화면 모델을 만들 때
- 라이브러리를 들이거나 바꿀 때
- `/diary-flow` 셀프 코드 리뷰에서 구조·성능을 볼 때

아래와 다르게 가야 하면 NIA·KC나 공식 문서에서 근거를 찾아 `docs/DECISIONS.md`에 남긴 뒤 이 파일을 고친다.

## 1. 모듈 자리

| 무엇 | 어디 |
| --- | --- |
| 데이터 모양(일기, 설정 값) | `shared/core/model` |
| DB·DAO·쿼리 | `shared/core/database` |
| DataStore 설정 | `shared/core/datastore` |
| 저장소(여러 출처를 묶는 일, 쓰기 규칙) | `shared/core/data` |
| 공통 화면 부품과 토큰 | `shared/core/designsystem` |
| 여러 기능이 함께 쓰는 화면 조각, 안전한 코루틴 함수 | `shared/core/ui` |
| 플랫폼 기능(expect/actual) | `shared/core/platform` |
| 시험 대역 | `shared/core/testing` |
| 화면 하나의 묶음 | `shared/feature/<기능>` |
| 화면 전환과 앱 틀 | `shared/app` |

- 기능 모듈은 api/impl로 나누지 않는다. NIA의 분할은 기능 수십 개를 여러 사람이 만드는 빌드 속도 문제를 푸는 것이다.
- `core:domain` 모듈은 두지 않는다. NIA도 쓰임새가 셋뿐이다. 여러 화면 모델이 같은 저장소 조합을 되풀이하면 `core/data`에 작은 클래스로 묶어 Koin으로 넣는다.
- 저장소 규칙이 `core/ui`에 들어가지 않게 한다. 예: `SettingsRepository.bodyLookIn`이 `core/ui/DiaryBodyText.kt`에 있고 화면 모델 여섯이 부른다(옮길 후보).
- 기능끼리 서로 의존하지 않는다. 함께 쓰는 것은 core로 내린다.

## 2. 의존성 주입

- Koin 4를 쓴다. KC는 컴파일 때 검증하는 Metro를 쓰지만, 옮기려면 등록 130여 개와 인자 있는 화면 모델을 모두 고쳐야 하고 Kotlin 판마다 맞춰야 하는 컴파일러 플러그인이 붙는다.
- 대신 Koin 그래프가 다 풀리는지 보는 시험을 둔다(빠진 등록을 실행 전에 잡음).
- 디스패처는 생성자로 받는다. 저장소·백업·복원에서 `Dispatchers.IO`를 바로 쓰지 않는다(NIA `@Dispatcher(IO)`). 그래야 시험이 가상 시간으로 돈다.
  2026-11-17 기준 `core/data`(복원·잠금·사진), `core/backup`, `core/migration`, `DatabaseFactory`의 commonMain 20곳 가까이가 바로 쓴다(옮길 후보).

## 3. 화면 모델과 단방향 흐름

- 화면 상태는 불변 `data class <화면>UiState` 하나, `StateFlow`로 내보낸다. 화면은 상태를 그리고 행동을 함수로 올려 보낸다.
- 흐름은 `stateInSafely(scope, SharingStarted.WhileSubscribed(5_000), 처음 값)`. 맨 `viewModelScope.launch`, `stateIn`은 `coroutineSafetyCheck`가 막는다.
- 읽기·곁일은 `launchSafely`, 사용자가 누른 쓰기는 `launchWrite`(`shared/core/ui/SafeCoroutines.kt`).
- 한 번만 일어날 일(닫기, 안내 한 줄)은 `Channel(BUFFERED)` + `receiveAsFlow`. 상태에 플래그로 두지 않는다.
- 예상되는 실패(공간 부족, 네트워크, DB 읽기)는 저장소가 결과로 돌려준다. 화면 모델은 그 결과로 상태를 정한다.
- 상태의 주인은 하나다. 같은 값을 화면과 화면 모델이 따로 들지 않는다. 증상만 막는 플래그를 덧대지 않는다.

## 4. 화면 전환

- Navigation 3, 탭마다 뒤로 가기 묶음(`shared/app/.../navigation/TabNavigation.kt`). 넓은 화면 두 칸은 `ListDetailScene.kt`.
- 화면 키는 직렬화되는 불변 값이다. 클래스 이름을 바꿔도 복원된 묶음이 깨지지 않게 `@SerialName`을 붙인다(KC). 지금은 붙어 있지 않다.
- 화면 사이 결과 전달은 저장소나 화면 키 인자로 한다. 화면 모델끼리 직접 잇지 않는다.

## 5. 데이터 계층

- 기기 안이 원본이다(오프라인 우선). 서버 동기화는 없고 백업은 사용자 드라이브다.
- 목록은 Paging 3, 한 번에 60편. 설정이 바뀌면 `InvalidatingPagingSourceFactory`로 새로 만든다.
- 무거운 해석(서식 HTML 같은 것)은 페이징 매핑 안 IO에서 쪽마다 한 번 하고 불변 값으로 담는다. 화면에서 하지 않는다.
- 저장 형식(DB 스키마, 백업 형식)을 바꾸면 `docs/migration.md`, `docs/backup.md`를 따르고 이전·복원 시험을 같이 고친다.

## 6. 시험

- 대역(`shared/core/testing`의 `Fake*`, `InMemoryDataStore`, `TestClock`)을 쓰고 mock 라이브러리를 쓰지 않는다(NIA).
- 화면 상태·저장소 시험은 `commonTest`, Compose 화면 시험은 `src/iosTest`(`runComposeUiTest`).
- 시간과 날짜는 주입해 시험에서 바꾼다(`rememberToday`의 `read`·`waitForTomorrow` 같은 꼴). 실제 시간을 기다리는 시험을 만들지 않는다.
- 위험한 곳(저장, 결제, 복원, 이전)은 실패하는 시험을 먼저 만들고, 고친 부분을 되돌리면 시험이 실패하는지 확인한다.
- 화면 모델 시험마다 `setMain`/`resetMain`을 되풀이하지 않게 `shared/core/testing`에 도우미를 둔다(28곳이 되풀이함, 옮길 후보).

## 7. 성능

- 지연 목록은 일기 ID를 키로, 항목에는 불변 값만 넘긴다. 안정 타입은 `compose_compiler_config.conf`에 적는다.
- 화면을 바꾸면 `benchmarks`의 `ScrollBenchmark`·`StartupBenchmark`로 재고, 실기기 출시형 빌드(`benchmarkRelease`) 값으로 판단한다. 에뮬레이터 값은 방식 사이 비교에만 쓴다.
- 시작 경로를 바꾸면 Baseline Profile을 다시 만든다(`/release-prep`). 시작 때 막는 읽기를 넣지 않는다(KC의 시작 때 설정 읽기는 따르지 않는다).
- 개발 빌드는 StrictMode 로그로 화면 스레드 디스크 일을 본다.

## 8. iOS

- 공유 코드는 정적 `Shared` 프레임워크, Xcode 프로젝트는 `iosApp/project.yml`(xcodegen).
- iOS에서 기대하는 기본 동작을 맞춘다. KC처럼 상태 막대를 누르면 목록 맨 위로, 목록을 끌면 키보드 내림(`ScrollToTopHandler`, `HideKeyboardOnDragHandler`). 지금은 둘 다 없다(들일 후보).
- 안드로이드와 iOS 기능은 같다. 한쪽만 있는 동작은 까닭을 적는다.

## 9. 빌드와 검사

- 공통 설정은 `build-logic` convention plugin에 둔다. 모듈 `build.gradle.kts`에 같은 설정을 되풀이하지 않는다.
- 버전은 `gradle/libs.versions.toml`, 안정판만. 베타·RC는 쓰지 않는다(KC는 CMP 1.13 베타, Kotlin 2.5 RC).
- 검사 명령은 `CLAUDE.md` "빌드와 시험". 새 지적은 baseline에 넣지 않고 고친다.

## 들일 후보 (2026-11-17 비교에서 나온 것)

값이 크고 싼 것부터. 들이면 이 목록에서 지우고 위 본문을 고친다.

| 후보 | 근거 | 들이는 곳 |
| --- | --- | --- |
| 디스패처 주입 | NIA `core/common/.../NiaDispatchers.kt` | `core/data`, `core/backup`, `core/migration`, `DatabaseFactory` |
| Koin 그래프가 다 풀리는지 보는 시험 | Hilt·Metro는 컴파일 때 검증 | `shared/app` 시험 |
| iOS 상태 막대로 맨 위, 끌면 키보드 내림 | KC `iosMain/.../ScrollToTopHandler.kt`, `HideKeyboardOnDragHandler.kt` | 홈, 찾기, 달력 |
| 화면 모델 시험 도우미 | NIA `core/testing/util/MainDispatcherRule.kt` | `shared/core/testing` |
| CI에 배지 검사, 출시형 린트 | NIA `.github/workflows/Build.yaml` | `.github/workflows/ci.yml` |
| 릴리스 의존성 고정(dependency-guard) | NIA `app/dependencies/` | `composeApp` |
| 맨 Material 부품 막기 | NIA `lint/.../DesignSystemDetector.kt` | `DesignTokenCheckTask` |
| 라이트·다크 함께 그리는 프리뷰 표시 | KC `ui/utils/ComponentPreview.kt` | `shared/core/designsystem` |
| 시험 실패 메시지 개선(power-assert) | KC `app/shared/build.gradle.kts` | `build-logic` |
| `bodyLookIn`을 `core/data`로 | 저장소 규칙이 `core/ui`에 있음 | `shared/core/data` |
| 화면 키에 `@SerialName` | KC 화면 키 | `shared/app` |

따르지 않기로 한 것: 기능 api/impl 분할, `core:domain` 모듈, Metro로 옮기기, NIA식 동기화 모듈과 카탈로그 앱, 모듈 그래프 문서, Jacoco(커버리지가 필요하면 Kover).
확인하지 못한 것: Roborazzi의 iOS 지원, JetBrains판 M3 adaptive-navigation3, Koin verify가 `commonTest`에서 도는지, Kotlin/Native `gc=cms`가 아직 기본값이 아닌지.
