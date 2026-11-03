# 앱 틀 계약

`shared/app`, `composeApp`, `iosApp`이 지키는 시작 순서와 틀, 다음 스펙들이 얹는 자리다.

## 시작 순서

1. **프로세스 시작** (Android `DiaryApplication.onCreate`, iOS `iOSApp.init`): Koin 시작, `Startup` 시작. `Startup`은 설정·잠금 DataStore와
   `migration/state.json`을 읽고(Android) 원본이 있으면 잠금·설정 잇기를 한다. 끝나면 `ready = true`. 목표 300ms 안, Room은 열지 않는다.
2. **첫 액티비티** (Android `MainActivity`): `installSplashScreen()` → `setKeepOnScreenCondition { !startup.ready }` → `setContent { DiaryApp() }`.
   고정 대기 없음. iOS는 `UILaunchScreen`이 첫 프레임까지 보인다.
3. **`DiaryApp()`**: `DiaryTheme(settings)` → `LockGate` → `MigrationGate` → `AppScaffold`.
   - `LockGate`: 잠겼으면 `LockScreen`만 그린다. 안쪽 화면 키 목록과 저장 상태는 버리지 않는다.
   - `MigrationGate`: `contracts/migration-flow.md` 화면 상태. 이전이 없거나 끝났으면 통과.
   - `AppScaffold`: 폭 600dp 미만 하단 탭, 이상 레일. 탭은 홈·달력·돌아보기·설정(스펙 002 프로토타입 확정)이고 쓰기 버튼은 홈·달력에 늘 보인다.
4. **백그라운드에서 돌아올 때**: 시작 화면 없음(FR-026). `LockController.onForeground`만 돈다.

## 매니페스트에 남기는 옛 이름 (Android)

| 이름 | 새 앱에서 | 까닭 |
| --- | --- | --- |
| `org.sjhstudio.diary.SplashActivity` | `MainActivity`를 가리키는 `activity-alias`(런처) | 홈 화면 바로가기가 옛 이름을 가리킨다(research R14) |
| `org.sjhstudio.diary.helper.AlarmReceiver` | 같은 이름의 수신기, `BOOT_COMPLETED`·`MY_PACKAGE_REPLACED`도 받음 | 옛 앱이 예약한 알람이 이 이름으로 온다(research R15) |

그 밖의 옛 액티비티 이름은 내보내지 않았던 것(`exported=false`)이라 남기지 않는다.

## 화면 키 (Navigation 3, `@Serializable`)

| 키 | 모듈 | 001에서 |
| --- | --- | --- |
| `HomeKey`, `CalendarKey`, `InsightKey` | 002 | 빈 자리 화면("곧 채워져요"가 아니라 빈 상태 컴포넌트) |
| `WriteKey(entryId: String?)` | 002 | 없음(쓰기 버튼은 002에서 잇는다) |
| `SettingsKey` | settings | 설정 틀 |
| `FontSettingsKey`, `TextSizeSettingsKey`, `ThemeSettingsKey` | settings | 있음 |
| `LockSettingsKey`, `PinSetupKey(mode)` | lock | 있음 |
| `ReminderSettingsKey` | settings | 켜기·시각. 알림 허용이 없으면 "알림이 꺼져 있어요"와 [허용하기](허용 묻기는 004) |
| `MigrationReportKey` | migration | 일부만 옮겨진 기기에서만 설정에 보인다. 결과, 원본 파일 내보내기, 문의하기 |
| `LicensesKey` | settings | 오픈소스 라이선스. 글꼴 10종과 앱에 들어간 라이브러리 |
| `FontLicenseKey(fontId)` | settings | 글꼴 하나의 라이선스 전문 |
| `LibraryLicenseKey(libraryId)` | settings | 라이브러리 하나의 라이선스 전문 |
| `DesignCatalogKey` | designsystem | 개발 빌드에서만(FR-029) |

뒤로 가기는 Navigation 3 기본. 잠금·이전 화면은 화면 키가 아니라 관문이라 뒤로 가기로 벗어날 수 없다.

## 사용 통계 고정 표 (`AnalyticsEvents`)

`Telemetry.log(event, params)`는 이 표에 있는 이벤트와 값만 받는다. 표에 없는 키는 개발 빌드에서 예외, 출시 빌드에서 버린다.

| 이벤트 | 값 |
| --- | --- |
| `screen_view` | `screen`(화면 키 이름) |
| `settings_changed` | `name`(`font`·`text_size`·`theme`·`reminder`·`lock`·`biometric`·`relock_after`), `value`(열거형 ID 또는 참·거짓) |
| `lock_result` | `result`(`ok`·`wrong`·`waiting`·`biometric_ok`·`biometric_cancel`) |
| `migration_*` | `contracts/migration-flow.md` |

Crashlytics에는 사용자 정의 키로 `migration_step`만 둔다. 로그(`Log`, `println`, `NSLog`)에 일기 글, 장소, 경로, 기분을 찍지 않는다.

## 빌드 검사

| 작업 | 막는 것 |
| --- | --- |
| `designTokenCheck` | `shared/feature/**`, `shared/app/**`의 Kotlin에서 `Color(0x`, 숫자 `.dp`·`.sp`, `RoundedCornerShape(숫자`, `MaterialTheme.colorScheme`, `MaterialTheme.typography`, `Text("`(미리보기 파일 제외) |
| `spotlessCheck` | ktlint 형식 |
| `.githooks/commit-msg` | `CLAUDE.md` 커밋 규칙에 맞지 않는 첫 줄 |
