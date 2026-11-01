# Research: 기반과 이전

조사일 2026-11-01. 버전은 이날 Google Maven(`dl.google.com/android/maven2`)과 Maven Central의 `maven-metadata.xml`에서 확인한 값이다.
"(판단)"은 출처가 아니라 근거를 보고 내린 판단이고, "(시험으로 확인)"은 개발 중 시험으로 맞는지 보는 것이다.

## 구조 비교 (R1의 근거)

읽은 시점: Now in Android(NIA) `2982e63`(2026-10-20), KotlinConf 앱 `907ce23`(2026-10-19), FeedFlow `efc5971`(2026-10-31), 오발있(로컬 저장소).

| 항목 | NIA | KotlinConf | FeedFlow | 오발있 | 이 계획 |
| --- | --- | --- | --- | --- | --- |
| 모듈 | core 17개 + feature api/impl | core, app:shared, ui-components(기능 모듈 없음) | core, database, shared, feedSync/* | composeApp, shared:app, shared:core:{model,network,data,designsystem,ui,testing}, shared:feature 6개 | 오발있 구조 + core/database·datastore·migration·platform, network 없음, api/impl 없음 |
| convention plugin | 15개 | 없음 | KmpLibrary 등 4개 | android.application, kmp.library, kmp.feature | 오발있 셋 + room, 루트 검사 |
| DI | Hilt | Metro | Koin | Koin BOM 4.2.2 | Koin BOM 4.2.2 |
| 화면 전환 | Navigation 3 + ViewModel 장식자 | JB navigation3-ui + 같은 장식자 | Navigation 3 | Navigation 3 + 탭 상태 장식자 | Navigation 3 + 표준 ViewModel 장식자 |
| 저장 | Room 2.8 + Proto DataStore | 설정만 | SQLDelight | DataStore(DB 없음) | Room 3 + DataStore |
| UI 상태 | sealed UiState, `stateIn(WhileSubscribed(5_000))`, `collectAsStateWithLifecycle` | 비슷 | 비슷 | 비슷 | NIA 방식 |
| 시험 | mock 금지·대역, Roborazzi | 저장소 이전 시험(commonTest) | 대역 + Turbine + Maestro | kotlin.test + Turbine, iOS UI 시험 | 대역 + Turbine, 레거시 고정 파일 이전 시험 |
| 성능 | benchmarks, baseline profile, `compose_compiler_config.conf`, lazy 키 | 없음 | baseline-prof.txt 커밋 | 없음 | NIA 방식 |
| 정적 분석 | Spotless + ktlint, 디자인 시스템 lint | 없음 | detekt + compose-rules | 없음 | Spotless + ktlint + 디자인 토큰 검사 |
| iOS | - | static framework + Xcode 프로젝트, SKIE 없음 | SwiftUI + SKIE | Xcode 없이 .app 묶음 | static framework + Xcode 프로젝트, SKIE 없음 |

출처: NIA `docs/ModularizationLearningJourney.md`, `docs/ArchitectureLearningJourney.md`, `README.md`(시험 절);
[NIA](https://github.com/android/nowinandroid), [KotlinConf](https://github.com/JetBrains/kotlinconf-app), [FeedFlow](https://github.com/prof18/feed-flow).
표본 넷 가운데 공유 코드에서 Room을 쓴 곳은 없어서 Room 3 KMP는 공식 문서로 따로 근거를 댄다(R4).

## 정한 것

### R1. 모듈은 오발있 구조에 NIA의 core 나누기를 더한다

- Decision: `composeApp`, `iosApp`, `shared/app`, `shared/core/{model,database,datastore,data,migration,designsystem,ui,platform,testing}`, `shared/feature/*`,
  `benchmarks`. 기능 모듈은 api/impl로 나누지 않는다. 네트워크 모듈은 두지 않는다(날씨는 스펙 002가 `core/data` 안에서 부른다).
- Rationale: 오발있과 같은 모양이라 두 프로젝트의 지식을 나눠 쓴다(`CLAUDE.md`). NIA의 api/impl 분할은 기능 수십 개와 여러 사람이 동시에 만드는
  빌드 속도 문제를 푸는 것이다(판단: 화면 열몇 개를 혼자 만드는 앱에는 비용이 크다). 이전 코드는 레거시 형식을 다 다룬 뒤 지울 수 있게 한 모듈에 모은다.
- Alternatives: KotlinConf처럼 기능 모듈 없이 하나(화면이 늘면 빌드와 경계가 흐려짐), NIA처럼 api/impl(지금 규모에 과함).

### R2. 빌드 축과 버전

| 묶음 | 버전 | 비고 |
| --- | --- | --- |
| Kotlin / KSP | 2.4.21 / 2.3.12 | 오발있 2.4.20의 최신 패치 |
| Compose Multiplatform / material3 | 1.12.2 / 1.9.0 | |
| AGP | 9.3.3 | KMP 라이브러리는 `com.android.kotlin.multiplatform.library` |
| Koin BOM | 4.2.2 | |
| Navigation 3 | androidx `navigation3-runtime` 1.2.0, JetBrains `navigation3-ui` 1.2.0 | JB 1.2.0 정식(오발있은 beta01) |
| lifecycle(JetBrains) | 2.11.0 | ViewModel, `lifecycle-viewmodel-navigation3` |
| Room 3 / sqlite-bundled | 3.0.3 / 2.7.1 | 정식. 3.1.0-alpha03, 2.8.0-alpha03은 쓰지 않는다 |
| Paging | 3.5.3 | `paging-common`·`paging-compose`에 iOS 아티팩트가 있다 |
| DataStore | 1.3.0 | 오발있 1.2.1과 같은 API |
| material3-adaptive(JetBrains) | 1.3.0 | 창 크기 등급. `adaptive-navigation3`은 1.4.0-alpha01이라 쓰지 않는다(R25) |
| Coil 3 / FileKit / Compottie | 3.6.3 / 0.17.0 / 2.3.3 | Compottie는 스펙 006 |
| core-splashscreen / biometric | 1.2.0 / 1.1.0 | Android 전용, 정식 |
| coroutines / datetime / serialization | 1.11.0 / 0.8.0 / 1.11.0 | |
| compileSdk / targetSdk / minSdk | 37 / 36 / 24 | core 1.19.0과 navigation3-ui 1.2.0의 AAR이 `minCompileSdk=37`이라 컴파일 SDK는 37(2026-11-01 확인). 타깃은 Play 요구 36 |
| Gradle / 데몬 JDK | 9.7.1 / 17 | `gradle/gradle-daemon-jvm.properties`가 JDK 17을 받아 쓴다(오발있과 같은 방식) |
| 그 밖 androidx | activity-compose 1.13.0, core-ktx 1.19.0, profileinstaller 1.4.1, benchmark·baselineprofile 1.5.0 | 오발있과 같다 |
| 정적 분석 | Spotless 8.10.4 + ktlint 1.8.0(`android_studio` 형식, 한 줄 140자) | R20 |

- Decision: 위 표를 `gradle/libs.versions.toml`에 고정한다. 알파·베타는 쓰지 않는다. JDK 17 툴체인으로 빌드한다(이 기기 기본 JDK 25에서는
  레거시 Gradle 8.9가 돌지 않아 레거시 빌드에는 JBR 17을 쓴다).

### R3. convention plugin

- Decision: `build-logic/convention`에 `diary.android.application`(composeApp), `diary.kmp.library`(android + iosArm64 + iosSimulatorArm64,
  JDK 17, Compose 안정성 설정 파일), `diary.kmp.feature`(library + Compose + Koin + lifecycle + Navigation 3 + core 의존), `diary.room`(KSP + Room 3,
  스키마 내보내기 `shared/core/database/schemas`)를 둔다. 루트에는 Spotless·ktlint, 디자인 토큰 검사(R18), git 훅 설치(R19)를 둔다.
- Rationale: 오발있의 셋을 그대로 쓰고 NIA처럼 반복 설정(Room, 정적 분석)을 플러그인으로 뺀다. Apple Silicon만 쓰므로 iosX64는 빼고 iosArm64(기기)를 더한다.

### R4. 저장은 Room 3 + 번들 SQLite 하나, R1 출시 때 버전 1

- Decision: `DiaryDatabase`(Room 3.0.3, `BundledSQLiteDriver`, 쿼리는 `Dispatchers.IO`). 표는 `entry`, `media`(001), `draft`, `draft_media`(002),
  백업 표(003)를 R1 출시 전까지 같은 버전 1에 모은다. 출시 전에는 스키마 JSON만 고치고 마이그레이션을 쓰지 않는다. 출시 뒤 바꿈은 Room
  자동 마이그레이션과 시험으로 한다. 목록은 `room3-paging`의 `PagingSource`를 Paging 3.5.3으로 받는다(스펙 002 O2를 닫음).
- Rationale: Room 3가 KMP를 지원하고 번들 드라이버로 Android·iOS가 같은 SQLite(FTS5, `VACUUM INTO` 포함)를 쓴다
  ([Room 3](https://developer.android.com/jetpack/androidx/releases/room3), [KMP Room](https://developer.android.com/kotlin/multiplatform/room)).
  Paging의 iOS 아티팩트(`paging-common-iosarm64` 3.5.3, `room3-paging-iosarm64`)를 Maven에서 확인했다.
- Alternatives: SQLDelight(FeedFlow, 레거시 이전과 같이 쓰기엔 장점 없음), DataStore만(오발있, 일기 1만 편 질의 불가).

### R5. 원본은 바이트 복사만, 사본을 번들 드라이버의 날 SQL로 읽는다

- Decision: 처음 열 때 `databases/note.db`, `note.db-wal`, `note.db-shm`을 스트림으로 `legacy-snapshot/`에 복사한 뒤에야 무엇이든 연다.
  원본에는 SQLite 연결을 열지 않는다. 사본(세 파일)을 번들 드라이버로 열어 `PRAGMA table_info(Note)`로 열을 확인하고 `_id` 순으로 한 줄씩 읽는다.
  `PRAGMA integrity_check`가 실패하면 `_id` 범위를 반씩 나눠 읽히는 줄만 건지고 못 읽은 범위를 센다. 설정은 `shared_prefs/pref.xml`,
  `pref2.xml`을 XML로 직접 읽고(`getSharedPreferences`로 열지 않는다), 사본에는 `password` 줄을 뺀 것을 쓴다.
- Rationale: DB를 한 번이라도 열면 닫을 때 WAL이 본 파일에 합쳐져 원본이 바뀐다([sqlite.org WAL](https://www.sqlite.org/wal.html),
  `docs/migration.md` 원칙 2). 레거시 스키마의 `TIMESTAMP` 열 때문에 Room으로는 열 수 없다(`docs/migration.md`). `getSharedPreferences`는 나중에
  같은 이름으로 쓰기를 하면 원본 XML을 다시 쓴다(판단). 사본의 WAL은 사본에 합쳐져도 괜찮다.
- Alternatives: Room 마이그레이션으로 기존 파일을 고침(원본 훼손, 되돌릴 곳 없음), Android 프레임워크 `SQLiteDatabase`로 읽기(번들 드라이버와 같은 결과지만 두 SQLite를 시험해야 함).

### R6. 검증 지문은 새 값을 옛 표현으로 되돌려 견준다

- Decision: 레거시 줄마다 `legacyCanonical(row)`을 만들고, 새 DB에서 읽은 일기와 사진을 옛 표현으로 되돌린 `restoredCanonical(entry, media)`를
  만들어 SHA-256을 견준다. 되돌리기는 기분·날씨 ID → 숫자, `toPlainText(bodyHtml)`과 `plainText`가 둘 다 `contents`와 같은지, 사진 줄 → 원래
  경로 목록이다. 줄 수, 찾은 사진 + 못 찾은 사진 = 경로 수, 복사한 사진 파일의 SHA-256 재계산도 본다. 형식은 `contracts/legacy-mapping.md`.
- Rationale: 같은 함수로 양쪽을 만들면 매핑 버그가 그대로 통과한다(판단). 앞으로 가는 변환과 되돌리는 변환이 둘 다 맞아야 같은 값이 나온다.
  매핑 자체의 정답은 고정 파일 시험의 기대값으로 따로 지킨다.

### R7. 이전은 임시 DB에 쓰고 이름 바꾸기로 한 번에 바꾼다

- Decision: 단계는 `NotStarted → Snapshotting → Copying → Verifying → Committing → Done`(또는 `Partial`, `Failed`). 상태와 결과는
  `migration/state.json`, `migration/report.json`에 임시 파일 쓰기 + 이름 바꾸기로 남긴다. 새 기록은 `migration/diary-new.db`(저널 TRUNCATE)에
  쓰고 검증을 통과하면 닫은 뒤 `diary.db`로 이름을 바꾸고 `Done`을 쓴다. 다음 시작에 상태가 `Done`·`Partial`이 아니면 `diary-new.db`를 지우고
  처음부터 한다. `Committing`에서 끊겼고 `diary.db`가 있으면 지문 검증만 다시 해서 맞으면 `Done`, 아니면 지우고 처음부터 한다.
  `Done` 전에는 어떤 화면도 새 DB를 열지 않는다.
- Rationale: 같은 파일 시스템 안의 이름 바꾸기는 원자적이라 반쯤 옮긴 DB가 `diary.db`로 보이는 순간이 없다(FR-007, 판단). 상태를 Room 밖 작은 파일에
  두어 DB가 없는 때에도 읽힌다.
- Alternatives: 같은 DB에 쓰고 "이전 중" 표시(지우기를 빠뜨리면 반쯤 옮긴 줄이 보임), WorkManager로 백그라운드 이전(끊기면 어차피 처음부터라 얻는 것이 적고 iOS와 다름).
  앱을 벗어나면 프로세스가 끝날 수 있어 진행 화면에서는 화면 꺼짐을 막는다.

### R8. 사진은 앱 저장소로 한 번만 복사하고 실패해도 남긴다

- Decision: 원래 경로마다 파일이 있으면 `media/<sha256>.<ext>`로 복사하고(이미 같은 해시가 있으면 건너뜀) 복사본의 해시를 다시 잰다. 확장자는
  경로의 확장자, 없으면 파일 머리 바이트로 정한다. 가로세로는 EXIF 방향을 반영한 값을 담는다. 원래 경로는 찾은 사진에도 `media.legacy_path`로
  남긴다. 이전이 실패해도 `media/`의 사본은 지우지 않는다(내용 해시 이름이라 다시 해도 같은 파일). 원본 사본 폴더에는 사진을 또 복사하지 않고
  "경로 → 해시" 목록(`legacy-snapshot/photos.json`)을 둔다.
- Rationale: 캐시의 사진은 OS가 언제든 지울 수 있다(`docs/migration.md`). 이전 첫 단계에서 지워지지 않는 곳으로 옮기면 실패해도 사진은 지켜진다(헌법 I).
  사진을 두 번 복사하면 사진 5천 장 기기에서 공간이 두 배로 든다(판단). 원래 경로는 이전 검증(R6)과 옛 드라이브 백업 합치기(스펙 003)에 쓴다.
- 공간 확인: 시작 전에 `(note.db + wal + shm) × 2 + 아직 없는 사진 크기 합 + 사진 수 × 40KB + 50MB`를 남은 공간과 견주고, 모자라면 시작하지 않고 필요한 양을 알린다.
- 하드 링크는 쓸 수 없다: 복사 대신 캐시 사진에 하드 링크를 걸면 공간이 들지 않아 검토했지만, Android 16 에뮬레이터에서 `ln cache/x files/y`가
  SELinux에 막혔다(`avc: denied { link } … tcontext=app_data_file`, 2026-11-01 `run-as`로 시험). 앱 자신의 도메인도 같은 제한일 가능성이 커서(판단)
  복사를 그대로 둔다. 원본을 옮기는(rename) 방법은 공간은 안 들지만 원본을 건드려 헌법 I에 어긋난다.

### R9. 줄임 사진은 긴 변 480px, 백업에서 빼고 캐시 폴더 밖에 둔다

- Decision: `thumbs/<sha256>.jpg`(긴 변 480px, JPEG 80). Android는 `noBackupFilesDir/thumbs`, iOS는 `Application Support/thumbs`(백업 제외 표시).
  이전의 `Copying` 단계에서 사진을 복사한 직후 최대 3개씩 나란히 만들고, 실패해도 이전은 계속한다(FR-037). 화면은 Coil 3의 사용자 정의 Fetcher가
  줄임 사진을 읽고, 없으면 원본에서 표본 축소로 만들어 저장한 뒤 돌려준다. 원본 크기는 사진 크게 보기에서만 화면 크기로 줄여 읽는다(FR-036).
- Rationale: 지금 앱은 목록 칸마다 원본을 통째로 디코딩한다(`PhotoAdapter.setPhoto`). 목록 사진 칸 64dp는 4배 밀도에서 256px, 폰 격자 칸 약 130dp는
  3배 밀도에서 390px라 480px이면 충분하다(판단, 출시 전 측정으로 조정). 다시 만들 수 있으니 백업에 넣지 않는다. 헌법 I은 사진과 DB를 캐시에 두지
  말라고 하므로 파생 파일도 캐시 폴더에는 두지 않는다.
- Alternatives: Coil 디스크 캐시만(로컬 파일은 디스크 캐시를 거치지 않아 매번 원본을 읽음, 판단), 캐시 폴더(OS가 지우면 넘길 때마다 다시 만듦).

### R10. 성능 바탕

- Decision: 목록·달력·검색은 Paging 3로 한 번에 60줄씩 읽고 달 머리는 `insertSeparators`로 넣는다(FR-035). `LazyColumn` 항목에 일기 ID를 키로
  주고 화면 모델은 불변 클래스로 둔다. `compose_compiler_config.conf`에 `core.model.*`, `kotlinx.datetime.*`를 안정 타입으로 적는다(FR-038).
  `benchmarks`에 시작 시간과 목록 넘기기 Macrobenchmark, baseline profile 생성기를 두고 profile은 저장소에 커밋한다.
- Rationale: NIA 관행([baseline profile](https://developer.android.com/topic/performance/baselineprofiles/overview),
  [안정성 설정 파일](https://developer.android.com/develop/ui/compose/performance/stability/fix#configuration-file)). strong skipping은 Kotlin 2.0.20부터
  기본이라 키와 불변 모델이 있으면 바뀐 줄만 다시 그린다. 지금 앱은 `selectAll`과 `notifyDataSetChanged`로 매번 전체를 그린다.

### R11. 잠금 비밀번호는 기기에 묶인 키의 HMAC 확인값만 둔다

- Decision: Android Keystore의 HMAC-SHA256 키(별칭 `diary.pin.v1`, 사용자 인증 요구 없음), iOS는 키체인의 256비트 임의 키
  (`kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly`)로 `HMAC(key, salt ‖ PIN)`을 만들어 `pinMac`, `salt`만 DataStore에 둔다. 비교는 상수 시간으로 한다.
  키가 없어졌는데(백업으로 다른 기기에 옮김, 키체인 초기화) 잠금이 켜져 있으면 FR-019의 "폰 화면 잠금으로 확인 → 새 비밀번호" 흐름으로 보낸다.
  잠금 저장소는 Android 자동 백업과 기기 간 이전에서 뺀다.
- Rationale: 4자리는 1만 가지뿐이라 소금 친 해시도 파일을 가져가면 바로 풀린다(판단). 키가 기기 밖으로 나가지 않으면 확인값만으로는 풀 수 없다
  ([Android Keystore](https://developer.android.com/privacy-and-security/keystore), HMAC은 API 23부터;
  [키체인 접근성](https://developer.apple.com/documentation/security/ksecattraccessibleafterfirstunlockthisdeviceonly)). 사용자 입력("기기 보안 저장소 키로 보호한 확인값")과 같다.
- Alternatives: PBKDF2·Argon2 해시만(4자리에는 무의미), 평문 유지(헌법 II 위반).

### R12. 생체 인증과 폰 화면 잠금

- Decision: Android는 `androidx.biometric` 1.1.0. 열기는 `BIOMETRIC_WEAK`(취소·실패·잠김이면 비밀번호 칸으로), 비밀번호를 잊었을 때는
  `BIOMETRIC_WEAK or DEVICE_CREDENTIAL`로 폰 잠금을 확인하고, `KeyguardManager.isDeviceSecure()`가 거짓이면 백업에서 되살리는 길을 안내한다.
  iOS는 `LAContext`로 열기는 `.deviceOwnerAuthenticationWithBiometrics`, 분실은 `.deviceOwnerAuthentication`(생체 또는 기기 암호).
  `NSFaceIDUsageDescription`을 넣는다. 연속 5번 틀리면 30초 기다림이고 횟수와 기다림 끝 시각은 저장해 앱을 다시 켜도 이어진다(FR-020).
- Rationale: `DEVICE_CREDENTIAL` 단독은 API 30 미만에서 안 되고 `BIOMETRIC_STRONG or DEVICE_CREDENTIAL`은 API 28~29에서 안 된다
  ([Authenticators](https://developer.android.com/reference/androidx/biometric/BiometricManager.Authenticators)). `BIOMETRIC_WEAK or DEVICE_CREDENTIAL`은
  minSdk 24부터 된다. Day One·Daylio도 분실 때 생체 인증으로 다시 정한다(스펙 근거).

### R13. 잠금은 앱 전체를 덮는 관문, 다시 잠그기는 단조 시계로

- Decision: `shared/app`의 루트가 `LockGate { MigrationGate { 앱 틀 } }`이다. 잠긴 동안 안쪽 내용은 그리지 않고 잠금 화면만 그리며, 화면 키 목록과
  저장 상태는 그대로 두어 열면 보던 화면으로 돌아간다. 백그라운드로 갈 때 단조 시계(Android `elapsedRealtime`, iOS `systemUptime`)를 적고, 돌아올 때
  고른 시간(바로·1분·5분)이 지났으면 잠근다. "앱을 새로 열 때만"은 프로세스 시작 때만 잠근다.
- 최근 앱 가림: Android 13 이상은 잠금이 켜지면 `setRecentsScreenshotEnabled(false)`. Android 7~12는 `onPause`에서 `FLAG_SECURE`를 켜고
  `onResume`에서 끈다(사용자 화면 캡처는 막지 않음, FR-018). 함께 `ON_PAUSE`에서 가림 화면을 그린다. iOS는 `sceneWillResignActive`에서 시작 화면과
  같은 가림 창을 올리고 `sceneDidBecomeActive`에서 내린다.
- Rationale: [Activity.setRecentsScreenshotEnabled](https://developer.android.com/reference/android/app/Activity#setRecentsScreenshotEnabled(boolean))는 API 33부터다.
  Apple은 백그라운드로 갈 때 민감한 내용을 가리라고 한다([Apple](https://developer.apple.com/documentation/uikit/preparing-your-ui-to-run-in-the-background)).
  Android 7~12에서 `onPause`에 켠 `FLAG_SECURE`가 최근 앱 미리보기에 반영되는지는 기기마다 다를 수 있다(시험으로 확인, 열린 결정 O3).
  벽시계를 쓰면 시간을 바꿔 다시 잠금을 피할 수 있다(판단).

### R14. 시작 화면은 시스템 스플래시 하나, 옛 런처 이름을 남긴다

- Decision: Android는 `core-splashscreen` 1.2.0의 `installSplashScreen()`과 `setKeepOnScreenCondition { !startup.ready }`. `ready`는 설정·잠금 상태·
  이전 상태 파일 읽기(작은 파일 셋)와 잠금 잇기(R16)가 끝나면 참이다. Room은 열지 않는다. 테마 `Theme.Diary.Starting`은 바탕 `canvas`(라이트 흰색,
  `values-night` `#16171B`)와 가운데 연필 벡터(움직임 없음)다. 옛 런처 이름 `org.sjhstudio.diary.SplashActivity`를 새 `MainActivity`를 가리키는
  `activity-alias`로 남긴다. iOS는 `Info.plist`의 `UILaunchScreen`(색 에셋 `LaunchBackground`, 이미지 `LaunchPencil`)이다.
- Rationale: Android는 작은 데이터만 읽는 동안 붙잡으라고 한다([Splash screen](https://developer.android.com/develop/ui/views/launch/splash-screen),
  [이전 안내](https://developer.android.com/develop/ui/views/launch/splash-screen/migrate)). 런처 액티비티 이름을 바꾸면 홈 화면 바로가기가 깨질 수 있다
  ([Things That Cannot Change](https://android-developers.googleblog.com/2011/06/things-that-cannot-change.html), [activity-alias](https://developer.android.com/guide/topics/manifest/activity-alias-element)).
  지금 앱의 런처는 `.SplashActivity`다(레거시 매니페스트). Apple은 첫 화면과 거의 같은 정적 시작 화면을 권한다([HIG](https://developer.apple.com/design/human-interface-guidelines/launching)).

### R15. 매일 알림: 옛 수신기 이름을 남기고 정확한 알람 권한 없이 다시 예약한다

- Decision: 옛 앱은 `AlarmManager.setAlarmClock`으로 `org.sjhstudio.diary.helper.AlarmReceiver`를 예약했다(레거시 `AlarmHelper`). 업데이트 뒤 이 예약이
  울리면 새 앱에 같은 이름의 수신기가 있어야 하므로 그 이름의 클래스를 `composeApp`에 두고 새 알림 코드로 넘긴다. `MY_PACKAGE_REPLACED`와
  `BOOT_COMPLETED`에서 다시 예약한다. 이전 전이면 옛 `pref2.xml`을 읽기만 해서 시각을 얻는다. 새 예약은 `setWindow(RTC_WAKEUP, 시각, 10분)`으로
  `SCHEDULE_EXACT_ALARM` 없이 한다. iOS는 `UNCalendarNotificationTrigger(repeats: true)`다.
- Rationale: 없는 클래스로 브로드캐스트가 오면 앱 프로세스에서 클래스를 못 찾는다(판단, 업그레이드 시험으로 확인). 정확한 알람 권한은 Android 14부터
  새로 설치한 앱에 기본 거부라 일기 알림에 쓰기 어렵다([알람 예약](https://developer.android.com/develop/background-work/services/alarms/schedule)).
  일기 알림은 몇 분 늦어도 된다(판단). 알림 허락 묻기는 스펙 004다.

### R16. 설정과 잠금은 DataStore, 잠금 잇기는 스플래시 동안

- Decision: `settings.preferences_pb`(글꼴, 글자 크기, 화면 모드, 위치 안내, 알림, 글꼴 바뀜 안내, 002의 목록 모양)와 `lock.preferences_pb`(R11)를
  둘로 나눈다. 1.7.x 잠금이 켜진 기기는 스플래시 동안 옛 `pref.xml`을 읽기만 해서 비밀번호 확인값을 만들고, 원본 사본이나 DB 이전은 잠금 화면 뒤에서
  돈다. 이전 진행·실패 화면은 잠금을 연 뒤에만 보인다(FR-015).
- Rationale: 잠금 화면을 먼저 띄우려면 이전보다 먼저 확인값이 있어야 한다. 이 읽기는 원본을 바꾸지 않는다(R5).

### R17. 글꼴 10종과 글자 크기 5단계

- Decision: 본문 글꼴 10종과 UI 글꼴 Pretendard 4굵기를 `core/designsystem`의 Compose 리소스에 넣고, 고른 글꼴만 그릴 때 읽는다. 글꼴 ID는
  `pretendard`, `gowun_dodum`, `cafe24_surround`, `maru_buri`, `gowun_batang`, `nanum_pen`, `nanum_jeomggol`, `kkomi`, `gamja_flower`, `hi_melody`와
  `system`이다(2026-11-01 다시 고름, `docs/design.md`).
- 글꼴 내려받기(2026-11-01 사용자 요청): Pretendard·국립공원 꼬미·나눔손글씨 펜(3.5MB 이하)만 Compose 리소스로 넣고, 나머지 6종은 Android에서 글꼴마다
  Play Feature Delivery 필요할 때 설치 모듈(`fonts:<id>`, 자산만 있는 모듈)로 둔다. 받으면 `SplitCompat`으로 모듈 자산을 읽어 `files/fonts/<id>.ttf`로
  복사하고 파일에서 `FontFamily`를 만든다. 받기 전에는 미리보기 그림만 보인다. iOS는 R2에 On-Demand Resources(또는 그때 Apple 권장 방식)로 같은 일을 한다.
  근거: [Play Feature Delivery](https://developer.android.com/guide/playcore/feature-delivery), [On-Demand Resources](https://developer.apple.com/library/archive/documentation/FileManagement/Conceptual/On_Demand_Resources_Guide/Managing.html).
  Google Play 서비스의 Downloadable Fonts([문서](https://developer.android.com/guide/topics/ui/look-and-feel/downloadable-fonts))는 Google Fonts에 있는 4종에만 쓸 수 있고
  마루 부리·점꼴체에는 못 써서, 한 가지 방식으로 맞추려고 쓰지 않는다. Play Asset Delivery는 문서가 게임을 기준으로 설명해 앱에 쓰는지 확인하지 못했다.
- 글자 크기: 앱 틀(탭, 설정, 버튼)은 앱 설정 없이 기기 글자 크기를 따른다(Android sp·글꼴 배율, iOS Dynamic Type). 앱 설정 "본문 글자 크기"는
  일기 본문(쓰기·상세·목록 미리보기)에만 걸리고 기기 글자 크기 위에 곱해진다. 단계는 5개 15/24, 17/28(기본, `reading`), 19/30, 21/34, 24/38이고
  미리보기 문장을 보며 고른다. 옛 `font_size`는 같은 자리로 옮긴다: 0·1 → 15, 2 → 17, 3 → 19, 4 → 21(사용자 결정 2026-11-01, 옛 동작에 묶이지 않는다).
- Rationale: Day One은 글꼴 크기 설정을 일기 본문에만 건다(iOS "font size (entries only)", Android "Editor Font Size",
  [iOS 설정](https://dayoneapp.com/?p=2220), [Android 설정](https://dayoneapp.com/?p=2234)). Bear도 본문 타이포그래피(크기, 줄 높이, 줄 폭)를 따로 둔다
  ([Bear](https://bear.app/faq/typography-options/)). 토스는 폰 글자 크기를 앱에 그대로 반영하고([토스피드](https://toss.im/tossfeed/article/mobile-accessibility)),
  Apple은 Dynamic Type을 따르라고 한다([HIG Typography](https://developer.apple.com/design/human-interface-guidelines/typography)). 옛 앱은 기기 설정을 무시하고
  앱 전체에 배율(0.6~1.2)을 곱했다(레거시 `BaseActivity.attachBaseContext`). 그래서 틀은 기기에 맡기고 일기 본문만 앱에서 고른다. 옛 단계를 같은 자리로 옮기면
  기본을 쓰던 사람은 새 기본으로 가고, 크게 쓰던 사람은 계속 크게 본다(새 크기는 옛 크기보다 작지도 않다).
- Alternatives: 옛 앱처럼 앱 전체 배율(기기 접근성 설정과 겹쳐 200%에서 깨짐), 카카오톡처럼 "기기 설정 따르기"를 따로 두기(본문이 기기 배율을 이미 따르므로 "보통"이 같은 뜻).

### R18. 화면 문구와 디자인 토큰을 빌드에서 지킨다

- Decision: 화면 문구는 모듈마다 `composeResources/values/strings.xml`(`publicResClass = false`). 루트 `designTokenCheck` 작업이 `shared/feature/**`와
  `shared/app/**`의 Kotlin에서 `Color(0x`, 숫자 `.dp`·`.sp`, `RoundedCornerShape(숫자`, `MaterialTheme.colorScheme`, `MaterialTheme.typography`,
  `Text("`를 찾으면 실패한다(`designsystem`과 미리보기 파일은 뺀다). `check`에 묶는다.
- Rationale: FR-028·FR-030, 헌법 V. NIA의 디자인 시스템 lint는 Android Lint라 commonMain을 보는지 확인하지 못했다(조사 메모). 단순한 문자열 검사로
  같은 효과를 얻는다(판단).

### R19. 커밋 형식 검사

- Decision: `.githooks/commit-msg`가 첫 줄을 `^(feat|fix|refactor|perf|test|docs|style|build|ci|chore|revert)(\([a-z0-9-]+\))?!?: \S.{0,70}$`로 보고
  `Merge `, `Revert "`로 시작하는 자동 메시지는 통과시킨다. 루트 `installGitHooks` 작업이 `git config core.hooksPath .githooks`를 하고 `preBuild`에 묶는다.
- Rationale: `CLAUDE.md` 커밋 규칙(Conventional Commits 1.0.0, 한글 제목 50자 안팎). Node 도구(commitlint) 없이 셸 하나로 된다.

### R20. 정적 분석과 CI

- Decision: Spotless + ktlint(NIA 방식). GitHub Actions `ci.yml`: Ubuntu에서 `spotlessCheck designTokenCheck allTests assembleDebug`, macOS에서
  `iosSimulatorArm64Test`와 `xcodebuild -scheme iosApp -sdk iphonesimulator CODE_SIGNING_ALLOWED=NO build`. 업그레이드 계측 시험(API 24·29·35·36)은
  수동 실행 작업(`workflow_dispatch`)으로 둔다.
- Rationale: 공개 저장소의 GitHub Actions 표준 러너는 무료다([Actions 요금](https://docs.github.com/en/billing/managing-billing-for-your-products/managing-billing-for-github-actions/about-billing-for-github-actions)).
  에뮬레이터 넷을 매 푸시마다 돌리면 오래 걸린다(판단).

### R21. 사용 통계와 오류 보고

- Decision: `core/platform`의 `Telemetry` 인터페이스. Android는 Firebase Analytics·Crashlytics, iOS는 Swift 쪽이 Firebase iOS SDK(SPM)로 구현해
  `MainViewController`에 넘긴다. 보낼 수 있는 이벤트와 값은 `contracts/app-shell.md`의 고정 표뿐이고 값은 열거형과 개수 구간이다. 이전 오류는
  `MigrationError(kind)`로 바꾼 뒤 보내 예외 메시지(경로, SQL)가 나가지 않게 한다. Crashlytics에 사용자 정의 로그를 남기지 않는다.
- Rationale: 헌법 II, FR-013, SC-009. 예외 메시지에 사진 경로나 값이 섞일 수 있다(판단).

### R22. iOS 앱 틀

- Decision: `shared/app`을 static framework `Shared`로 내보내고 Xcode 프로젝트 `iosApp`이 `ComposeUIViewController`로 띄운다. SKIE는 쓰지 않는다.
  최소 iOS 17. 앱 아이콘은 1024px 하나에 다크·틴트 변형(Xcode 에셋), Firebase는 SPM.
- Rationale: KotlinConf와 같은 구조(R1). 오발있처럼 Xcode 없이 묶으면 위젯(WidgetKit)과 실기기 배포가 안 된다(조사 메모).

### R23. 레거시 글 → 본문 HTML은 정확히 되돌아오는 규칙으로

- Decision: 빈 글은 `""`. 그 밖에는 `contents`를 `"\n\n"`이 나올 때마다 왼쪽부터 잘라 덩어리를 만들고, 덩어리를 이스케이프(`&`, `<`, `>`, `"`, `'`)한 뒤
  덩어리 안의 `"\n"`을 `<br>`로 바꿔 `<p>…</p>`로 감싼다. 공백, 탭, `\r`, 이모지는 그대로 둔다. `toPlainText`는 `p` 사이를 `"\n\n"`, `br`을 `"\n"`으로
  잇고 엔티티를 풀며 공백을 줄이지 않는다. 예: `"a\n\n\nb"` → `<p>a</p><p><br>b</p>` → `"a\n\n\nb"`. 규칙과 시험 목록은 `contracts/legacy-mapping.md`.
  스펙 002 `contracts/rich-text-html.md`에 "공백은 그대로 둔다"를 더한다.
- Rationale: FR-006은 다시 글로 바꾼 본문이 원문과 한 글자도 다르지 않아야 한다. "빈 줄로 나눈다"만으로는 빈 줄이 둘 이상이거나 글이 줄바꿈으로 시작·끝날
  때 되돌릴 수 없다(판단).
- 남는 위험: 편집기(compose-rich-editor)가 HTML을 읽으며 연속 공백을 줄이면 화면에 보이는 글이 원문과 달라진다. 저장 형식은 그대로 두고 편집기
  경계에서 고친다(열린 결정 O4, 스펙 002).

### R24. 앱 아이콘 자산

- Decision: Figma 섹션 "앱 아이콘 확정 · A2"에서 Android 적응형 아이콘 전경·배경(108dp, 연필은 가운데 66dp 안)과 단색 테마 아이콘, iOS 1024px 라이트·
  다크·틴트, Play 512px, App Store 1024px을 내보낸다. 스플래시 연필은 같은 그림을 바탕 칸 없이 벡터로 쓴다.
- Rationale: [적응형 아이콘](https://developer.android.com/develop/ui/views/launch/icon_design_adaptive), [HIG App icons](https://developer.apple.com/design/human-interface-guidelines/app-icons),
  `docs/design.md` 앱 아이콘 확정.

### R25. 앱 틀의 폭 규칙과 두 칸

- Decision: material3-adaptive 1.3.0의 `currentWindowAdaptiveInfo()`로 폭 등급을 보고 600dp 미만은 하단 탭, 이상은 레일을 그린다. 두 칸(840dp부터)은
  Navigation 3 런타임의 `SceneStrategy`를 직접 만들어 스펙 002가 쓴다(`adaptive-navigation3`이 알파라서, 스펙 002 O3을 닫음).
- Rationale: `docs/design.md` 넓은 화면, [창 크기 등급](https://developer.android.com/develop/ui/compose/layouts/adaptive/use-window-size-classes).

## 열린 결정

| 번호 | 무엇 | 정하는 조건 | 기본값 |
| --- | --- | --- | --- |
| O1 | 줄임 사진 크기(480px)와 품질 | 1만 편·사진 5천 장 기기에서 넘기기(SC-010)와 용량을 재고 정한다 | 480px, JPEG 80 |
| O2 | UI 글꼴 Pretendard를 줄일지 | 4굵기 전체 크기를 재서 앱 크기가 지금(118MB)보다 크면 한글 11,172자와 영문만 남긴 서브셋으로 | 전체 |
| O3 | Android 7~12 최근 앱 가림 | `onPause`의 `FLAG_SECURE`가 미리보기를 가리는지 API 24·29·31 에뮬레이터에서 본다. 안 되면 잠금이 켜진 동안 늘 `FLAG_SECURE`(그 버전에서 화면 캡처도 막힘)로 바꾸고 스펙 FR-018을 고칠지 사용자에게 묻는다 | `onPause`에서 켬 |
| O4 | 편집기가 공백을 줄이는지 | 스펙 002 쓰기 화면 개발 첫 작업에서 연속 공백·앞뒤 공백·탭으로 확인 | 저장 형식은 그대로, 편집기 경계에서 고침 |
| O5 | Android 자동 백업·기기 간 이전 범위 | 닫음(2026-11-01, 스펙 003 research R20): 클라우드 자동 백업과 기기 간 이전 모두에서 일기·사진·설정·잠금을 뺀다 | - |

## 출처

- 구조: 위 "구조 비교" 표의 저장소, [Conventional Commits](https://www.conventionalcommits.org/ko/v1.0.0/)
- 저장: [Room 3](https://developer.android.com/jetpack/androidx/releases/room3), [KMP Room](https://developer.android.com/kotlin/multiplatform/room), [SQLite WAL](https://www.sqlite.org/wal.html), [Compatibility WAL](https://source.android.com/docs/core/perf/compatibility-wal)
- 잠금: [Android Keystore](https://developer.android.com/privacy-and-security/keystore), [BiometricManager.Authenticators](https://developer.android.com/reference/androidx/biometric/BiometricManager.Authenticators), [LAPolicy.deviceOwnerAuthentication](https://developer.apple.com/documentation/localauthentication/lapolicy/deviceownerauthentication), [키체인 접근성](https://developer.apple.com/documentation/security/ksecattraccessibleafterfirstunlockthisdeviceonly), [setRecentsScreenshotEnabled](https://developer.android.com/reference/android/app/Activity#setRecentsScreenshotEnabled(boolean)), [Apple 백그라운드 준비](https://developer.apple.com/documentation/uikit/preparing-your-ui-to-run-in-the-background)
- 시작: [Splash screen](https://developer.android.com/develop/ui/views/launch/splash-screen), [Things That Cannot Change](https://android-developers.googleblog.com/2011/06/things-that-cannot-change.html), [HIG Launching](https://developer.apple.com/design/human-interface-guidelines/launching), [UILaunchScreen](https://developer.apple.com/documentation/bundleresources/information-property-list/uilaunchscreen)
- 알림: [알람 예약](https://developer.android.com/develop/background-work/services/alarms/schedule)
- 성능: [Baseline Profiles](https://developer.android.com/topic/performance/baselineprofiles/overview), [Compose 안정성 설정](https://developer.android.com/develop/ui/compose/performance/stability/fix#configuration-file)
- 백업 제외: [Android 자동 백업](https://developer.android.com/identity/data/autobackup)
- 레거시 사실: `app/src/main/AndroidManifest.xml`(런처 `.SplashActivity`, `helper.AlarmReceiver`), `AlarmHelper.java`(`setAlarmClock`), `BaseActivity.kt`(글자 배율), `WriteFragment.java`(사진 경로를 `,`로 이음, 끝에도 `,`)
