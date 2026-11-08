<img src="https://user-images.githubusercontent.com/79048895/159389254-d84a5c96-8edc-4aec-bc12-53d5e0dba38b.png" width="96" alt="1일 1일기 앱 아이콘" />

# 1일 1일기

바쁜 날에도 하루 한 줄은 남길 수 있는 일기장이다.
기분을 하나 고르고 몇 줄 쓰면 그날이 쌓인다.
회원가입이 없고, 일기와 사진은 내 구글 드라이브에만 백업된다.

[Google Play에서 받기](https://play.google.com/store/apps/details?id=org.sjhstudio.diary)

Play에 있는 지금 버전은 Java/Kotlin View로 만든 1.7.4다.
이 저장소는 앱을 Compose Multiplatform으로 새로 만든 2.0이다.
2.0.0은 Play에 업데이트로 내고, iOS 버전은 그다음 단계에서 App Store에 새로 낸다.

<img src="docs/store/screens/phone-1.png" width="160" alt="기분 고르기" />
<img src="docs/store/screens/phone-2.png" width="160" alt="일기 목록" />
<img src="docs/store/screens/phone-3.png" width="160" alt="달력" />
<img src="docs/store/screens/phone-4.png" width="160" alt="돌아보기" />
<img src="docs/store/screens/phone-5.png" width="160" alt="드라이브 백업" />

## 할 수 있는 것

| 기능 | 내용 |
| --- | --- |
| 쓰기 | 글, 사진, 기분 9가지, 날씨 7가지, 장소를 한 편에 담는다. 굵게, 밑줄, 글자색, 형광펜, 목록을 쓸 수 있다. 쓰던 글은 앱이 꺼져도 남는다. |
| 다시 보기 | 목록, 달력, 검색, 즐겨찾기로 지난 일기를 찾는다. 공유하면 날짜와 기분이 함께 간다. |
| 돌아보기 | 주·월·년별로 어떤 기분이 많았는지 본다. |
| 백업과 복원 | 사진까지 내 구글 드라이브에 하루 한 번 올린다. 복원은 지금 휴대폰의 일기를 지우지 않고 합친다. 드라이브 없이 파일 하나로도 옮길 수 있다. |
| 기록 이전 | 1.7.x에 쌓인 일기, 사진, 설정을 그대로 옮긴다. 원본은 고치거나 지우지 않는다. |
| 잠금과 휴지통 | 비밀번호와 생체 인증으로 잠근다. 삭제한 일기는 30일 동안 휴지통에서 복원할 수 있다. |
| 내게 맞게 | 글꼴 10종, 본문 글자 크기, 다크 모드, 목록 모양을 고른다. 태블릿에서는 목록과 일기를 나란히 본다. |

1.7.4에서 무료였던 기능과 사진 포함 백업은 2.0에서도 무료다.
2.0부터 광고가 들어가지만 일기를 쓰고 읽는 화면, 잠금, 백업, 설정에는 없다.
유료 상품은 둘이다.

| 상품 | 값 | 받는 것 |
| --- | --- | --- |
| 광고 제거 | ₩9,900, 한 번 구매 | 광고가 사라진다 |
| Plus | 연 ₩14,900 | 광고 제거, 한 편에 사진 30장(무료는 10장), 체크리스트, 고급 통계 |

## 지키는 약속

모든 작업이 [헌법](.specify/memory/constitution.md)을 따른다. 요약하면 이렇다.

1. **기록은 잃지 않는다.** 이전과 복원은 원본을 먼저 복사하고 검증한 뒤에 바꾼다.
2. **일기는 사용자 것이다.** 일기 내용, 사진, 장소는 사용자가 고른 저장소 밖으로 나가지 않는다. 사용 통계와 오류 보고에도 넣지 않는다.
3. **무료였던 기능은 계속 무료다.**
4. **돈이 드는 서버를 두지 않는다.** 일기는 사용자의 드라이브에 둔다.
5. **어떤 기기에서도 편하다.** 폰, 폴더블, 태블릿, iPad에서 같은 기능을 쓴다.

## 기술 스택

| 영역 | 사용 |
| --- | --- |
| 언어와 UI | Kotlin 2.4, Compose Multiplatform 1.12 |
| 구조 | Koin 4, Navigation 3, `build-logic` convention plugin |
| 데이터 | Room 3 + `BundledSQLiteDriver`(FTS5, `VACUUM INTO`), DataStore, Paging |
| 편집과 미디어 | compose-rich-editor, Coil 3, FileKit, Compottie(Lottie) |
| 백업 | Google Drive REST(Ktor), WorkManager, iOS BGTask, okio |
| 결제와 광고 | RevenueCat(안드로이드), AdMob, Firebase Remote Config |
| 운영 | Firebase Analytics, Crashlytics |
| 빌드와 검사 | AGP 9.3, Spotless(ktlint), detekt, Android Lint, Baseline Profile, GitHub Actions |

iOS 결제와 홈 화면 위젯은 2단계에서 넣는다.
정확한 버전은 [`gradle/libs.versions.toml`](gradle/libs.versions.toml)에 있다.

## 구조

```text
composeApp/       안드로이드 앱(MainActivity, 알림 수신기)
iosApp/           Xcode 프로젝트. 원본은 project.yml이다
shared/
  app/            시작 관문, 화면 전환, 의존성 주입
  core/
    model/        화면과 저장소가 함께 쓰는 값
    database/     Room DB와 DAO
    datastore/    설정과 상태 파일
    data/         저장소(일기, 휴지통, 요금제, 광고, 알림)
    drive/        구글 드라이브 REST
    backup/       백업, 복원, 파일 내보내기
    migration/    1.7.x 기록 이전
    designsystem/ 색, 글자, 간격 토큰과 공용 부품
    ui/           여러 화면이 쓰는 화면 조각
    platform/     안드로이드와 iOS가 다른 일(expect/actual)
    testing/      시험 대역
  feature/        home, write, calendar, search, insight, trash, lock,
                  migration, settings, backup, welcome, plus
fonts/            고를 때 받는 일기 글꼴(Play 필요할 때 설치 모듈)
benchmarks/       시작 시간 측정과 Baseline Profile
build-logic/      Gradle convention plugin과 검사 작업
app/              1.7.4 레거시 앱. 빌드에 넣지 않고 이전 코드의 참고용으로 둔다
```

의존은 `feature → core → model` 한 방향으로만 흐른다.
화면은 ViewModel의 상태만 그리고, ViewModel은 저장소 인터페이스만 본다.
그래서 새 기능은 `shared/feature/<이름>` 모듈 하나와 `AppModules.kt`의 등록 몇 줄로 붙는다.
구조는 [Now in Android](https://github.com/android/nowinandroid)와 [KotlinConf 앱](https://github.com/JetBrains/kotlinconf-app)을 열어 보고 정했다.
비교한 내용은 [`specs/001-foundation-migration/research.md`](specs/001-foundation-migration/research.md)에 있다.

## 빌드와 시험

Android SDK가 필요하다. JDK 17은 Gradle이 받아서 쓴다.

```bash
./gradlew :composeApp:assembleDebug
```

```bash
ANDROID_SERIAL=emulator-5554 ./gradlew :composeApp:installDebug
```

CI와 같은 검사다. 코드 형식, 디자인 토큰, 문자열 리소스, detekt, Android Lint, 두 플랫폼 시험, 빌드를 한 번에 돌린다.

```bash
./gradlew spotlessCheck designTokenCheck stringResourceCheck detekt :composeApp:lint allTests assembleDebug
```

iOS는 Xcode와 xcodegen이 필요하다.

```bash
(cd iosApp && xcodegen generate)
```

```bash
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -sdk iphonesimulator \
  -destination 'platform=iOS Simulator,name=iPhone 17' -derivedDataPath iosApp/build build
```

- 화면 시험(`runComposeUiTest`)은 각 모듈의 `src/iosTest`에 두고 iOS 시뮬레이터에서 돈다.
- 화면 상태와 저장소 시험은 `src/commonTest`에 두어 두 플랫폼에서 돈다.
- `designTokenCheck`는 화면 코드에 원시 색, 숫자 `dp`·`sp`, 문자열 상수가 들어오면 막는다.
- 광고 ID, RevenueCat 키, Firebase 설정 파일은 저장소에 없다. 없어도 빌드되고 광고, 결제, 사용 통계만 꺼진다. 넣는 방법은 [`docs/release.md`](docs/release.md)에 있다.

## 문서

| 문서 | 내용 |
| --- | --- |
| [`docs/PRD.md`](docs/PRD.md) | 리뉴얼 기획 총괄 |
| [`docs/migration.md`](docs/migration.md) | 기존 기록 이전. 저장 형식을 바꾸기 전에 반드시 본다 |
| [`docs/backup.md`](docs/backup.md) | 백업, 복원, 동기화 설계 |
| [`docs/design.md`](docs/design.md) | 디자인 시스템과 넓은 화면 규칙 |
| [`docs/business.md`](docs/business.md) | 수익 모델과 무료·유료 범위 |
| [`docs/release.md`](docs/release.md) | 스토어 출시 준비 |
| [`docs/DECISIONS.md`](docs/DECISIONS.md) | 결정 기록 |
| [`CLAUDE.md`](CLAUDE.md) | 작업 규칙(커밋, 관례, 검사) |

## 개발 흐름

- 기능은 기획, Figma 프로토타입, Figma 디자인, 디자인 점검, 개발, 마무리 순서로 만든다. 기능마다 결과를 `specs/<번호>-<이름>/`에 남긴다.
- 브랜치는 `main`(배포), `develop`(개발), 기능 브랜치(`feat/<영문-이름>`)다. 기능 브랜치는 개발할 때만 `develop`에서 만들고 끝나면 `develop`에 합친다.
- 커밋은 [Conventional Commits](https://www.conventionalcommits.org/ko/v1.0.0/)를 따른다. 첫 빌드가 켜는 Git 훅이 형식을 검사한다.
- 화면 문구는 한국어, 영어, 일본어 문자열 리소스에 함께 넣는다.

## 라이선스

아직 오픈소스 라이선스를 정하지 않았다.
앱에 쓴 라이브러리와 글꼴의 라이선스는 앱의 설정 > 오픈소스 라이선스에서 볼 수 있다.
