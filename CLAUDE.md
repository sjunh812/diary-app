# 1일 1일기

바쁜 사람을 위한 간편한 일기장. 지금 버전은 Java/Kotlin View 기반 안드로이드 앱(`app/`, 1.7.4(35), `org.sjhstudio.diary`)이고,
Compose Multiplatform으로 다시 만들어 Play에는 업데이트, App Store에는 새로 낸다. 리뉴얼 기획은 `docs/PRD.md`에 있다.

## 문서

| 문서 | 내용 |
| --- | --- |
| `docs/PRD.md` | 리뉴얼 기획 총괄: 목표, 범위, 단계, 열린 결정 |
| `docs/migration.md` | 기존 기록 이전. 저장 형식을 건드리기 전에 반드시 본다 |
| `docs/backup.md` | 백업·복원·동기화 설계 |
| `docs/design.md` | 디자인 시스템과 넓은 화면 규칙 |
| `docs/business.md` | 수익 모델과 무료/유료 범위 |
| `docs/release.md` | 스토어 출시 준비(스크린샷, 소개 문구, 개인정보처리방침) |
| `docs/DECISIONS.md` | 결정 기록과 열린 결정 |
| `.specify/memory/constitution.md` | Spec Kit 헌법. 아래 "지켜야 할 선"과 같은 내용 |

규칙이 바뀌면 그 규칙이 있는 문서, 헌법, 관련 스펙을 같은 커밋에서 고치고 `docs/DECISIONS.md`에 날짜와 까닭을 남긴다.

## 지켜야 할 선

헌법 원칙의 요약이다. 넘는 요청이 오면 멈추고 어느 원칙에 걸리는지 말한 뒤 묻는다.

1. **기록은 잃지 않는다.** 이전과 복원은 원본을 고치거나 지우지 않고, 복사하고 검증한 뒤에 바꾼다. 복원은 합치기가 기본이다. 백업에 사진이 들어간다. 사진과 DB는 캐시에 두지 않는다.
2. **일기는 사용자 것이다.** 내용, 사진, 주소, 기분은 사용자가 고른 저장소 말고는 나가지 않는다. 사용 통계, 비정상 종료 보고, 광고, 로그에 넣지 않는다. 회원 계정을 만들지 않는다.
3. **지금 무료인 기능은 계속 무료다.** 1.7.4의 무료 기능과 사진 포함 백업은 무료로 둔다. 예외로 한 편 새 사진은 무료 10장·Plus 30장이고 이전된 사진은 모두 남긴다. 쓰고 읽는 중(쓰기, 저장, 상세, 잠금, 백업·복원, 이전)에는 광고가 없고 저장은 광고에 묶지 않는다. 전면 광고는 하루 1번, 저장 뒤 목록으로 돌아갈 때나 앱을 1분 넘게 쓴 뒤 화면이 넘어갈 때만(2026-11-01).
4. **돈이 드는 서버를 두지 않는다.** 일기와 사진은 사용자 본인의 클라우드에 둔다. 작은 서버 일은 카드 없이 쓰는 무료 플랜에서만.
5. **심플하고 맛있게.** 토큰만 쓰고, 강조는 한두 곳. 꾸미기는 일기 본문 안에서만.
6. **어떤 기기에서도 편하다.** 폰, 폴더블, 태블릿, iPad. 안드로이드와 iOS 기능은 같다.
7. **근거를 갖고 만든다.** 레퍼런스를 실제로 찾아 출처와 함께. 기능은 `/diary-flow` 순서로.

## 만드는 순서

기능은 `/diary-flow`(기획 → Figma 프로토타입 → Figma 디자인 → 개발 → 마무리)로 만든다. Spec Kit 스킬(`/speckit-*`)은 이 흐름 안에서 부른다.

## 레퍼런스

기능을 만들거나 고칠 때는 말하지 않아도 레퍼런스를 찾아 근거로 삼는다. 화면과 흐름은 토스, 당근, 배민과 해외 앱(Apple, Material,
Day One 등), 일기 기능은 국내외 일기 앱이다. 공식 문서, 스토어, 지원 페이지, 디자인 시스템 문서에서 확인한 것만 출처와 함께 적는다.

## 아키텍처 (제안, `docs/DECISIONS.md`에서 확정)

- Compose Multiplatform, Android + iOS. 오발있 저장소와 같은 축(Kotlin 2.4, CMP 1.12, AGP 9.3, Koin 4, Navigation 3,
  `build-logic` convention plugin)을 써서 두 프로젝트의 지식을 나눠 쓴다.
- 모듈: `composeApp`(안드로이드 앱, Glance 위젯), `iosApp`(Xcode 프로젝트, WidgetKit 확장), `shared/app`(화면 전환 뼈대),
  `shared/core/{model,database,data,designsystem,ui,platform,testing}`, `shared/feature/*`.
- 라이브러리 후보(2026-10-31 조사, 버전은 스펙 001 계획에서 고정): DB는 Room 3 + `BundledSQLiteDriver`(FTS5, `VACUUM INTO`),
  설정은 DataStore, 에디터는 compose-rich-editor, 이미지는 Coil 3, 사진 선택은 FileKit, 움직이는 그림은 Compottie(Lottie),
  결제는 RevenueCat KMP, 위젯은 Glance(Android)와 SwiftUI WidgetKit(iOS, App Group의 JSON 스냅샷을 읽음),
  생체 인증은 expect/actual(BiometricPrompt, LAContext). 레거시 DB는 Room이 아니라 날 SQL로 읽는다(`docs/migration.md`).
- `applicationId`는 `org.sjhstudio.diary` 그대로, Play 앱 서명 키 그대로, `versionCode`는 35보다 크게, `minSdk`는 24 그대로(D4). 구글 드라이브 OAuth는 지금과
  같은 Google Cloud 프로젝트를 쓴다(이전 백업이 보이려면 필요, `docs/migration.md`).
- 레거시 `app/` 모듈은 이전 코드가 레거시 형식을 다 다룰 때까지 참고용으로 남긴다.
- 구조와 코드는 Now in Android(Google)와 널리 쓰이는 CMP 오픈소스 프로젝트(JetBrains KotlinConf 앱 등)를 실제로 열어 보고 근거로
  정한다: 모듈 나누기, convention plugin, 데이터 계층과 오프라인 우선, 화면 상태와 단방향 흐름, 시험 구성, 성능(Baseline Profile,
  안정적인 Compose 상태, 지연 목록 키). 유지보수와 성능을 먼저 본다(사용자 요청 2026-10-31). 스펙 001 계획에서 비교표를 남긴다.

## 빌드와 시험

스펙 001이 새 프로젝트 뼈대를 만들 때 여기에 명령을 채운다. 설치는 에뮬레이터에만 한다(`ANDROID_SERIAL` 지정).

## 관례

- 화면 말투는 해요체, 법적 고지만 합니다체.
- 화면 문구는 문자열 리소스에 둔다. 코드에 문자열 상수를 두지 않는다.
- 주석은 자연스러운 한국말로, 문장마다 줄을 바꾼다.
- 커밋과 푸시는 사용자가 요청할 때만. 형식은 아래 "커밋 규칙".

## 커밋 규칙

[Conventional Commits 1.0.0](https://www.conventionalcommits.org/ko/v1.0.0/)을 따른다(2026-10-31 사용자 요청).

```text
<type>(<scope>)!: <제목>

<본문: 왜 바꿨는지>

<꼬리말>
```

- **type**: `feat`(새 기능), `fix`(버그 고침), `refactor`(동작은 그대로 구조만, 예전 `refact` 대신), `perf`, `test`, `docs`,
  `style`(포맷만), `build`(Gradle·의존성), `ci`, `chore`(그 밖, 버전 올림은 `chore(release)`), `revert`.
- **scope**(있으면): 바뀐 곳 하나를 짧게. `app`, `designsystem`, `database`, `migration`, `backup`, `home`, `write`, `calendar`,
  `search`, `insight`, `trash`, `widget`, `ios`, `android`, `spec`(Spec Kit 문서), `design`(Figma·디자인 문서).
- **제목**: 한국어, 50자 안팎, 명사형으로 끝내고 마침표 없음. 무엇을 했는지만 쓰고 어떻게는 본문에 쓴다.
- **본문**: 제목 다음 빈 줄, 왜 바꿨는지와 영향. 72자 안팎에서 줄을 바꾼다.
- **꼬리말**: 저장 형식(DB 스키마, 백업 형식)이나 이미 있는 동작을 바꾸면 type 뒤에 `!`를 붙이고 `BREAKING CHANGE: <무엇이 바뀌고 어떻게 옮기는지>`를
  쓴다. 근거는 `Refs: specs/002-diary-core FR-006`이나 `Refs: #12`. 공동 작성자 줄은 맨 끝.
- 커밋 하나에 논리적 변경 하나, 빌드와 시험이 통과하는 상태로 남긴다. 포맷 정리와 기능 변경을 섞지 않는다.
- 브랜치는 `<type>/<영문-슬러그>`(예: `feat/photo-multi-pick`). 기본 브랜치에는 PR로 넣고 squash merge, PR 제목도 같은 형식.
- 릴리스는 `chore(release): 2.0.0 (36)` 커밋과 `v2.0.0` 태그. 변경 기록은 `feat`·`fix` 커밋에서 뽑는다.
- 스펙 001에서 commit-msg 훅(저장소의 `.githooks/`, `git config core.hooksPath .githooks`)으로 형식을 검사한다.

```text
feat(write): 사진 여러 장 한 번에 고르기

시스템 사진 고르기에서 남은 장수만큼 여러 장을 고르게 했다.
한도(무료 10장)에 닿아도 저장은 막지 않고 한 줄 안내를 띄운다.

Refs: specs/002-diary-core FR-006
```
- 비밀값(API 키, 서명 정보)은 저장소에 올리지 않는다. 레거시 `app/.../utils/ApiKey.kt`의 네이버 지도 키는 2022년부터 공개
  저장소에 올라가 있다. 네이버 지도 API는 유료로 바뀌어 리뉴얼에서는 쓰지 않는다(사용자 확인 2026-11-01). 결제가 연결된 계정이면 남이 쓴
  요금이 나올 수 있으니 네이버 클라우드 콘솔에서 그 키(앱)를 삭제하거나 막는다.
