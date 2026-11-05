# Research: 일기 핵심 화면

조사일 2026-10-31. 버전과 요금은 `docs/backup.md`, `docs/business.md` 조사와 같은 날 확인한 값이다. "(판단)"은 출처가 아니라 근거를 보고 내린 판단이다.

## 정한 것

### R1. 저장은 Room 3 + 번들 SQLite, 스키마는 스펙 001과 하나의 버전 계획으로

- Decision: 스펙 001의 Room 3 DB에 `Draft`, `DraftMedia` 테이블과 `Entry(deletedAt, date)` 색인을 더한다. 001이 처음 내는 DB 버전에 함께 넣어 출시 전에는 마이그레이션이 없게 한다.
- Rationale: Room 3.0.3 안정판이 KMP를 지원하고 `BundledSQLiteDriver`로 Android·iOS가 같은 SQLite를 쓴다([Room 3](https://developer.android.com/jetpack/androidx/releases/room3), [KMP Room](https://developer.android.com/kotlin/multiplatform/room)). 출시 전 스키마를 한 번에 확정하면 R1 출시 때 레거시 → 새 DB 이전 하나만 시험하면 된다.
- Alternatives: SQLDelight 2.4.1(실행 시점 스키마 검증이 문서에 없음), 임시 저장본을 DataStore에 두기(사진 목록·서식이 커서 원자적 쓰기가 어렵다).

### R2. 서식 편집은 compose-rich-editor, 저장은 허용 태그만 남긴 HTML

- Decision: compose-rich-editor(Apache-2.0, v1.2.0, Compose 1.12 대응, Android·iOS)로 편집하고 HTML로 입출력한다. 저장 전에 `contracts/rich-text-html.md`의 허용 목록으로 정리하고, 같은 내용의 서식 없는 글을 `plainText`에 둔다.
- Rationale: 사용자 결정(제한된 HTML, `docs/DECISIONS.md`). 리뷰 요청인 글자색·형광펜을 표현하려면 Markdown으로는 부족하다. 라이브러리가 HTML 입출력을 지원한다(README). 라이선스는 GitHub API로 Apache-2.0 확인.
- Alternatives: Markdown(색 표현 불가), 자체 JSON(변환기를 직접 유지).
- 남는 위험: 붙여넣기로 들어온 HTML. 정리 함수가 허용 목록 밖 태그와 속성을 모두 버리는지 시험으로 지킨다.

### R3. 서식 색은 팔레트 키로 저장하고 다크 모드에서 토큰 색으로 그린다

- Decision: 글자색 5종, 형광펜 3종을 팔레트 키(`ink-red` 등)로 정한다. HTML에는 키에 대응하는 라이트 hex를 쓰고, 그릴 때 알려진 hex를 지금 테마의 토큰 색으로 바꾼다. 팔레트에 없는 색은 정리 단계에서 지운다.
- Rationale: 헌법 V(토큰만)와 다크 모드 가독성. 편집기는 인라인 `style` 색을 쓰므로 키를 별도 속성으로 두면 편집기 출력과 어긋난다(판단).
- Alternatives: `data-*` 속성으로 키 저장(편집기 호환을 직접 맞춰야 함), 아무 색이나 허용(다크 모드에서 안 보이는 글자).

### R4. 사진 고르기·카메라는 FileKit

- Decision: FileKit(MIT, 0.17.0)의 갤러리 고르기 `FileKitMode.Multiple(maxItems = 남은 장수)`와 카메라 고르기를 쓴다.
- Rationale: README에 여러 장 고르기와 `maxItems`, 카메라 고르기가 있다. Android Photo Picker, iOS PHPicker를 감싼다. iOS에서는 런처를 루트 스코프에서 기억해야 하고 바텀 시트 안에서는 안 된다(조사 메모) → 쓰기 화면 루트에 둔다.
- Alternatives: expect/actual로 직접(코드가 많다).

### R5. 사진 축소와 해시는 직접

- Decision: `core/platform`의 `ImageProcessor`를 expect/actual로 둔다. Android는 `BitmapFactory`의 다운샘플링 후 정확한 크기로 줄이고 EXIF 방향을 반영해 JPEG(품질 85)로 쓴다. iOS는 `UIGraphicsImageRenderer`로 같은 일을 한다. 긴 변 2,560px을 넘을 때만 줄인다. 결과 바이트의 SHA-256을 파일 이름으로 쓴다.
- Rationale: 사용자 결정(긴 변 약 2,500px). 내용 해시 이름은 백업 형식 v2의 사진 이름과 같아서 백업 때 다시 해시할 필요가 없다(`docs/backup.md`).
- Alternatives: 원본 보관(용량), Coil로 축소(저장용 인코딩이 목적이 아니다).

### R6. 임시 저장본과 사진 수명

- Decision: 쓰기 상태가 바뀌면 1초 디바운스로, 그리고 `ON_STOP`·화면 이탈 때 즉시 `Draft`를 쓴다. 고른 사진은 그 자리에서 `files/media`에 쓰고 `DraftMedia`로 잇는다. 저장하면 한 트랜잭션에서 `Entry`·`Media`를 쓰고 `Draft`를 지운다. 앱 시작 때 일기·임시 저장본·휴지통 어디에서도 쓰지 않고 만든 지 하루가 지난 사진 파일만 지운다.
- Rationale: SC-002(강제 종료 5초 전까지 보존)와 헌법 I. 지금 앱은 사진을 캐시에 두어 사라졌다.
- Alternatives: 저장 버튼에서만 사진 복사(그 전에 앱이 죽으면 임시 저장본이 사라진 파일을 가리킨다).

### R7. 달력은 직접 그린다

- Decision: `HorizontalPager` 한 장 = 한 달, 7열 격자. 칸 색은 `MoodColor` 토큰, 편수 표시는 2편 이상일 때만.
- Rationale: 지금 앱의 material-calendarview는 Android 전용이다. 기분 색 칸과 넓은 화면 배치를 우리 규칙대로 그려야 한다(판단).

### R8. 차트는 Canvas로 직접 그린다

- Decision: 기분 개수 막대, 날짜별 기분 흐름, 연간 기분 지도를 Compose Canvas로 그린다. 집계는 SQL `GROUP BY` 뒤 공통 코드에서 비율을 만든다.
- Rationale: 지금 앱의 MPAndroidChart는 Android 전용이다. 차트 셋이 단순하고 토큰 색·접근성 설명을 직접 붙여야 한다(판단).

### R9. 휴지통은 `deletedAt`, 비움은 앱 시작 때

- Decision: 지우기는 `deletedAt = now`. 모든 일반 질의는 `deletedAt IS NULL`. 앱 시작 때(백그라운드 코루틴) `deletedAt < now - 30일`을 영구 삭제하고 R6 사진 정리를 돈다. 되돌리기는 `deletedAt = NULL`.
- Rationale: 스펙 FR-032~035. 레퍼런스 30일(Apple 메모·사진·저널, Google 드라이브·포토). 백그라운드 예약 작업 없이도 다음 실행 때 처리하면 스펙을 만족한다.

### R10. 검색은 먼저 `LIKE`, 느리면 FTS5 trigram

- Decision: `plainText`와 `placeName`에 `LIKE '%…%' ESCAPE '\'`(사용자 입력의 `%`, `_` 이스케이프)로 찾는다. 조건(기분, 날씨, 사진 있음, 즐겨찾기, 기간)은 같은 질의의 `WHERE`에 붙인다. 1만 편에서 1초를 넘으면 FTS5 trigram 색인을 더하고 1~2글자 검색만 `LIKE`로 남긴다.
- Rationale: 한국어 한 글자 부분 일치(FR-025)는 단어 단위 FTS 토크나이저로 안 된다. trigram은 세 글자 이상만 색인을 탄다(판단, 출시 전 측정으로 확인). 번들 SQLite는 FTS5를 켜고 빌드된다(`docs/backup.md` 조사).
- Alternatives: 처음부터 FTS5(1~2글자 검색이 안 됨), 앱 메모리에서 검색(1만 편 본문을 다 올려야 함).

### R11. 창 크기와 두 칸

- Decision: material3-adaptive의 창 크기 등급으로 폭 600/840dp를 판단하고, Navigation 3 Scene 전략으로 홈(목록·상세)과 달력(달력·그날 목록)을 두 칸으로 그린다. 하단 탭 ↔ 레일은 `shared/app`의 스캐폴드가 바꾼다.
- Rationale: Material canonical layouts(list-detail은 expanded부터), Android 창 크기 기준(600/840dp). Apple HIG에는 숫자 기준이 없어 같은 값을 iPad에도 쓴다(스펙 FR-036).

### R12. 레거시 대비 바뀌는 동작

| 지금 앱 | 새 앱 | 까닭 |
| --- | --- | --- |
| 기분 필수 | 선택 | 리뷰 3 |
| 사진 한 장씩, 자르기 필수, 장수 제한 없음 | 여러 장, 자르기 선택, 무료 10·Plus 30(이전분 보존) | 리뷰 3, 사용자 결정 |
| 나가면 "작성 취소" 확인 후 버림 | 확인 없이 임시 저장 | 헌법 I, 리뷰 3 |
| 지우기는 확인 후 영구 삭제 | 휴지통 + 되돌리기 | 헌법 I |
| 검색: 본문만, 10자, 단순 포함 | 본문+장소, 조건 칩, 강조 | 리뷰 없음, 레퍼런스 |
| 통계: 파이 차트, 전체/올해/이번 달 | 주·월·년·전체 + 지난 기간, 기분 → 일기 | 리뷰 3, 2 |
| 지난 날짜에도 지금 날씨 자동 채움 | 오늘만 자동 채움 | 레거시 버그(조사 메모) |
| 상세 삭제가 사진 여러 장이면 파일을 못 지움 | 사진 정리(R6) | 레거시 버그 |

## 열린 결정

| 번호 | 무엇 | 정하는 조건 | 기본값 |
| --- | --- | --- | --- |
| O1 | 사진 자르기 라이브러리 | KMP(Android·iOS)를 지원하고 라이선스가 Apache/MIT인 것을 찾으면 쓴다. GitHub 검색으로 아직 못 찾았다 | 찾기 전까지 자르기는 P3로 미룬다(스펙상 필수 아님) |
| O2 | 목록 페이징 | 닫음(2026-11-01 스펙 001 R4): `room3-paging`과 Paging 3.5.3에 iOS 아티팩트가 있다 | Paging 3 |
| O3 | 두 칸 Scene을 CMP에서 | 닫음(2026-11-01 스펙 001 R25): JetBrains `adaptive-navigation3`이 1.4.0-alpha01뿐이라 쓰지 않는다 | `SceneStrategy`를 직접 만든다(Navigation 3 런타임 API) |
| O4 | 날씨·동네 이름 출처 | 닫음(2026-11-01 D7, 2026-11-05 바뀜): 동네 이름은 기기 지오코더를 누를 때만 쓴다. 자동 날씨는 위치를 읽어야 해서 이번 출시에서 뺐다 | 기기 지오코더 |
| O5 | 되돌리기 알림 시간 | 닫음(2026-10-31 디자인 시스템 점검): 5초, 누르는 동안 멈춤. TDS 토스트는 버튼이 있으면 5000ms, SEED 스낵바는 누르는 동안 멈춘다 | 5초 |

## 출처

- 쓰기: [Day One iOS 서식](https://dayoneapp.com/guides/tips-and-tutorials/editing-and-formatting-entries-in-day-one-for-ios/), [Day One 미디어](https://dayoneapp.com/guides/getting-started-with-day-one/adding-media-in-day-one/), [Apple 저널](https://support.apple.com/ko-kr/guide/iphone/iph492ee70a8/ios), [Journey 서식](https://support.journey.cloud/en/categories/writing-editing/articles/supported-text-formatting), [Journey 미디어 20개](https://blog.journey.cloud/announcing-journeys-enhanced-media-upload-combine-photos-videos-and-audio-with-20-media-files-per-entry/)
- 목록·달력·검색: [Day One 보기](https://dayoneapp.com/guides/day-one-ios/journal-views-in-day-one-for-ios/), [Day One 달력](https://dayoneapp.com/guides/tips-and-tutorials/calendar-view-in-day-one), [Day One 검색](https://dayoneapp.com/guides/day-one-ios/search-in-day-one/), [Apple 메모 검색](https://support.apple.com/ko-kr/guide/iphone/iphb8628c6b8/ios), [하루콩](https://apps.apple.com/kr/app/id1553223828)
- 통계: [Daylio 통계](https://daylio.net/faq/docs/daylio-faq/about/activity-and-mood-statistics/)
- 휴지통·되돌리기: [Apple 메모 최근 삭제](https://support.apple.com/en-gb/guide/icloud/mm2f42f05cb9), [Day One 휴지통](https://dayoneapp.com/guides/tips-and-tutorials/recover-deleted-entries-from-trash/), [Google 드라이브](https://support.google.com/drive/answer/2375102), [Material 3 Snackbar](https://m3.material.io/components/snackbar/guidelines), [당근 SEED Snackbar](https://seed-design.io/components/snackbar), [토스 TDS Toast](https://tossmini-docs.toss.im/tds-mobile/components/toast/)
- 넓은 화면: [창 크기 등급](https://developer.android.com/develop/ui/compose/layouts/adaptive/use-window-size-classes), [canonical layouts](https://developer.android.com/develop/ui/compose/layouts/adaptive/canonical-layouts), [HIG split views](https://developer.apple.com/design/human-interface-guidelines/split-views)
- 문구: [토스 라이팅 원칙](https://toss.tech/article/8-writing-principles-of-toss), [SEED 글쓰기](https://seed-design.io/foundations/writing), [SEED Result Section](https://seed-design.io/components/result-section)
- 라이브러리: [compose-rich-editor](https://github.com/MohamedRejeb/compose-rich-editor), [FileKit](https://github.com/vinceglb/FileKit), [Coil](https://github.com/coil-kt/coil)
