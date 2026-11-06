<img src="https://user-images.githubusercontent.com/79048895/159389254-d84a5c96-8edc-4aec-bc12-53d5e0dba38b.png" width="96" alt="1일 1일기 앱 아이콘" />

# 1일 1일기

바쁜 사람을 위한 간편한 일기장이다. 기분을 고르고 몇 줄만 남기면 하루가 쌓인다.
회원가입은 없고, 기록은 사진까지 내 구글 드라이브에 백업한다.

[Google Play에서 받기](https://play.google.com/store/apps/details?id=org.sjhstudio.diary)

지금 Play에 있는 버전은 Java/Kotlin View로 만든 1.7.4다.
이 저장소에서는 앱을 Compose Multiplatform으로 다시 만들고 있다.
2.0.0은 Play에 업데이트로 내고, iOS 버전은 그다음 단계에서 App Store에 새로 낸다.
기획은 [`docs/PRD.md`](docs/PRD.md)에 있다.

## 스크린샷

지금 Play에 있는 1.7 버전 화면이다.

<img src="https://github.com/user-attachments/assets/7ce84dea-878d-4948-902d-7ae501d0d9b7" width="180" height="324" alt="1.7 버전 화면 1" />
<img src="https://github.com/user-attachments/assets/88d595ad-af2d-4232-94ab-6db6f3b857f8" width="180" height="324" alt="1.7 버전 화면 2" />
<img src="https://github.com/user-attachments/assets/db682753-aeb2-405d-9a89-948fe74c2f08" width="180" height="324" alt="1.7 버전 화면 3" />
<img src="https://github.com/user-attachments/assets/0bd05396-0e7a-4ba8-981f-76b8058ce017" width="180" height="324" alt="1.7 버전 화면 4" />
<img src="https://github.com/user-attachments/assets/dc053af8-bce2-4b8b-9160-bf7758abb9ec" width="180" height="324" alt="1.7 버전 화면 5" />

## 주요 기능

2.0 기준이다.

- **쓰기**: 글, 사진 여러 장, 기분 9가지, 날씨 7가지, 장소를 한 편에 담는다. 굵게, 글자색, 형광펜, 목록 같은 기본 서식을 쓸 수 있고 쓰는 동안 임시 저장된다.
- **돌아보기**: 목록, 달력, 전문 검색, 즐겨찾기, 기분 통계로 지난 일기를 찾아본다.
- **백업과 복원**: 일기와 사진을 내 구글 드라이브에 올린다. 복원은 지금 휴대폰의 일기를 지우지 않고 합친다. 드라이브 없이 파일 하나로 보관할 수도 있다.
- **기록 이전**: 1.7.x에 쌓인 일기, 사진, 설정을 새 앱으로 옮긴다. 원본은 고치거나 지우지 않는다.
- **잠금과 휴지통**: 비밀번호와 생체 인증으로 잠근다. 삭제한 일기는 30일 동안 복원할 수 있다.
- **내게 맞게**: 글꼴 10종, 본문 글자 크기, 다크 모드, 목록 모양 3가지를 고른다. 태블릿에서는 목록과 일기를 나란히 본다. 화면은 한국어, 영어, 일본어로 나온다.

1.7.4의 기능과 사진 포함 백업은 계속 무료다.
2.0부터 광고가 들어가지만 일기를 쓰고 읽는 화면에는 없다.
광고를 없애는 평생권과 연 구독 Plus(한 편에 사진 30장, 체크리스트, 고급 통계)를 따로 판다.

## 기술 스택

| 영역 | 사용 |
| --- | --- |
| 언어와 UI | Kotlin 2.4, Compose Multiplatform 1.12, Material 3 |
| 구조 | Koin 4, Navigation 3, `build-logic` convention plugin |
| 데이터 | Room 3 + `BundledSQLiteDriver`(FTS5, `VACUUM INTO`), DataStore, Paging |
| 편집과 미디어 | compose-rich-editor, Coil 3, FileKit, Compottie(Lottie) |
| 백업 | Google Drive REST(Ktor), WorkManager, okio |
| 운영 | Firebase Analytics, Crashlytics, AdMob |
| 빌드와 검사 | AGP 9.3, Spotless(ktlint), Baseline Profile, GitHub Actions |

결제(RevenueCat KMP)와 위젯(Glance, WidgetKit)은 아직 넣지 않았다.
정확한 버전은 [`gradle/libs.versions.toml`](gradle/libs.versions.toml)에 있다.

## 모듈 구조

```text
composeApp/       안드로이드 앱
iosApp/           Xcode 프로젝트
shared/
  app/            시작 관문과 화면 전환 뼈대
  core/           model, database, datastore, data, drive, backup, migration,
                  designsystem, ui, platform, testing
  feature/        home, write, calendar, search, insight, trash, lock,
                  migration, settings, backup, welcome, plus
fonts/            고를 때 받는 일기 글꼴(Play 필요할 때 설치 모듈)
benchmarks/       시작 시간 측정과 Baseline Profile 생성
build-logic/      Gradle convention plugin과 검사 작업
app/              1.7.4 레거시 앱. 빌드에 포함되지 않고 이전 코드의 참고용으로 둔다
```

구조는 Now in Android와 KotlinConf 앱을 열어 보고 정했다.
비교한 내용은 [`specs/001-foundation-migration/research.md`](specs/001-foundation-migration/research.md)에 있다.

## 빌드

Android SDK가 필요하다. JDK 17은 Gradle이 받아서 쓴다.

```bash
./gradlew :composeApp:assembleDebug                               # 개발 APK 빌드
ANDROID_SERIAL=emulator-5554 ./gradlew :composeApp:installDebug   # 에뮬레이터에 설치
./gradlew spotlessCheck designTokenCheck stringResourceCheck allTests assembleDebug   # CI와 같은 검사
```

iOS는 Xcode와 xcodegen이 필요하다. Xcode 프로젝트의 원본은 `iosApp/project.yml`이다.

```bash
(cd iosApp && xcodegen generate)
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -sdk iphonesimulator \
  -destination 'platform=iOS Simulator,name=iPhone 17' -derivedDataPath iosApp/build build
```

광고 ID와 Firebase 설정 파일은 저장소에 없다. 없어도 빌드되고, 광고와 사용 통계만 꺼진다.
넣는 방법은 [`docs/release.md`](docs/release.md)에 있다.

## 문서

| 문서 | 내용 |
| --- | --- |
| [`docs/PRD.md`](docs/PRD.md) | 리뉴얼 기획 총괄 |
| [`docs/migration.md`](docs/migration.md) | 기존 기록 이전. 저장 형식을 건드리기 전에 반드시 본다 |
| [`docs/backup.md`](docs/backup.md) | 백업, 복원, 동기화 설계 |
| [`docs/design.md`](docs/design.md) | 디자인 시스템과 넓은 화면 규칙 |
| [`docs/business.md`](docs/business.md) | 수익 모델과 무료·유료 범위 |
| [`docs/release.md`](docs/release.md) | 스토어 출시 준비 |
| [`docs/DECISIONS.md`](docs/DECISIONS.md) | 결정 기록과 열린 결정 |
| [`.specify/memory/constitution.md`](.specify/memory/constitution.md) | 모든 작업이 지키는 원칙 |

## 개발 흐름

기능 하나를 `/diary-flow` 순서로 만든다. 결과는 기능마다 `specs/<번호>-<이름>/`에 남는다.

```text
기획 → Figma 프로토타입 → Figma 디자인 → 디자인 점검 → 개발 → 마무리
```

`/diary-flow`는 [Spec Kit](https://github.com/github/spec-kit) 스킬을 이 순서로 묶은 Claude Code 스킬이다.
커밋은 [Conventional Commits](https://www.conventionalcommits.org/ko/v1.0.0/)를 따르고, 첫 빌드가 켜는 Git 훅이 형식을 검사한다.
자세한 규칙은 [`CLAUDE.md`](CLAUDE.md)에 있다.

## 라이선스

아직 오픈소스 라이선스를 정하지 않았다.
앱에 쓴 라이브러리와 글꼴의 라이선스는 앱의 설정 > 정보 > 오픈소스 라이선스에서 볼 수 있다.
