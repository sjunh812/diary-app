# 기존 기록 이전

리뉴얼에서 가장 중요한 문서다. 1.7.4(35)까지 쌓인 일기, 사진, 설정이 새 앱으로 하나도 빠짐없이 넘어가야 한다.
이 문서의 규칙은 헌법 원칙 I(기록은 잃지 않는다)이 지킨다. 바꾸려면 `docs/DECISIONS.md`에 까닭을 남긴다.

## 지금 기기에 있는 것 (1.7.4 기준, 코드로 확인)

### 일기 DB

- 파일: `databases/note.db`, `SQLiteOpenHelper` 버전 2. 테이블은 `onCreate`가 아니라 `MainActivity`가 `CREATE TABLE IF NOT EXISTS`로 만든다(`NoteDatabase.createTable`).
- 테이블 `Note`

| 열 | 타입 | 뜻과 주의 |
| --- | --- | --- |
| `_id` | INTEGER PK AUTOINCREMENT | 기기 안에서만 유일하다. 백업 복원하면 다른 기기와 겹친다 |
| `weather` | INTEGER, 기본 -1 | -1 없음, 0 맑음, 1 구름 조금, 2 구름 많음, 3 흐림, 4 비·소나기, 5 비/눈, 6 눈 |
| `address` | TEXT | 역지오코딩한 주소 문자열 |
| `location_x`, `location_y` | TEXT | 수정할 때마다 `''`로 지워진다. 옮기지 않아도 되지만 값이 있으면 그대로 담는다 |
| `contents` | TEXT | 서식 없는 글. 이모지, 따옴표, 아주 긴 글이 있을 수 있다 |
| `mood` | INTEGER, 기본 -1 | -1 없음, 0 화남, 1 쿨, 2 슬픔, 3 아픔, 4 웃음, 5 보통, 6 나쁨, 7 좋음, 8 피곤 |
| `picture` | TEXT | 사진 절대경로를 쉼표로 이은 문자열. 빈 문자열이면 사진 없음 |
| `create_date` | TIMESTAMP(TEXT) | `yyyy-MM-dd HH:mm:ss` 기기 현지 시각, 오프셋 없음. UNIQUE 인덱스 `Note_Index`. 사용자가 날짜를 골라 쓰면 그 날짜가 들어간다. 즉 "작성 시각"이 아니라 "일기의 날짜"다 |
| `modify_date` | TIMESTAMP(TEXT) | 수정 시각. 날짜 변경이 없는 수정에서만 갱신된다 |
| `year`, `month` | INTEGER | 목록 묶음용 파생값. `update2`는 `Note.day`(이름과 달리 월)를 `month`에 넣는다 |
| `star` | INTEGER | 즐겨찾기 0/1. 버전 1에서 올라온 줄은 `ALTER TABLE ... INTAGER`로 더해져 기본값이 없어 NULL일 수 있다 |

- 하루에 여러 편을 쓸 수 있다. 유일한 것은 초 단위 `create_date`뿐이다. 새 앱도 하루 여러 편을 막지 않는다.
- Android 9부터 저널 모드를 따로 정하지 않은 DB는 Compatibility WAL이 기본이다([AOSP](https://source.android.com/docs/core/perf/compatibility-wal)). SQLite는 WAL 파일도 DB의 일부라 같이 옮겨야 한다고 한다([sqlite.org](https://www.sqlite.org/wal.html)). 그래서 `note.db` 파일만 복사하면 `-wal`에 남은 최근 일기가 빠질 수 있다. 지금 백업(`BackupActivity.createFile`)이 정확히 이렇게 파일만 올린다.
- 이 스키마는 Room으로 바로 열 수 없다. Room은 `room_master_table`이 없는 DB를 열 때 엔티티 스키마를 열 타입 affinity로 검증하는데, `TIMESTAMP`는 어느 affinity에도 들지 않아 String(`TEXT`) 열과 맞지 않는다(androidx `room3-runtime` 소스 `RoomConnectionManager.checkIdentity`). 그래서 레거시 DB는 Room이 아니라 날 SQL로 읽는다.

### 사진

- 사진은 앱 저장소가 아니라 **캐시 폴더**에 있다. 자르기 라이브러리(CanHub 4.2.1)의 `getUriFilePath(context, true)`가 `context.cacheDir/temp_file_yyyyMMdd_HHmmss.<확장자>`로 복사한 경로를 DB에 그대로 넣는다(`MainActivity.cropImageActivityResult`, 라이브러리 `GetFilePathFromUri.kt`). 1.5.6(2022-04)부터 이렇다.
- 캐시는 저장 공간이 모자라면 OS가 지우고, 사용자가 "캐시 삭제"를 누르면 다 지워진다. 이미 사라진 사진이 있을 수 있다.
- 파일 이름이 초 단위라 같은 초에 두 장을 넣으면 뒤 사진이 앞 사진을 덮는다.
- 백업은 사진을 올리지 않고 복원은 기기의 사진을 지운다고 안내한다(`BackupActivity.startRestoreDialog`). 다른 기기에서 복원한 일기의 `picture`는 그 기기에 없는 경로를 가리킨다.

### 설정 (SharedPreferences)

| 파일 | 키 | 기본 | 새 앱에서 |
| --- | --- | --- | --- |
| `pref` | `font_key` | -1 | 글꼴. -1·0 기본(Pretendard), 100 시스템, 1~13 assets `font.otf`·`font2`~`font13`. 아래 "글꼴 대응"대로 잇는다 |
| `pref` | `font_size` | 2 | 글자 크기 단계(앱 전체 배율 0.6~1.2). 새 앱의 일기 본문 글자 크기 같은 자리로: 0·1 작게, 2 보통, 3 크게, 4 더 크게(스펙 001 R17) |
| `pref` | `mode_key` | 0 | 0 시스템, 1 라이트, 2 다크 |
| `pref` | `use_pw`, `password` | false, "" | 잠금. 비밀번호가 **평문**으로 있다. 새 앱은 해시로 바꿔 담고 잠금을 켠 채로 연다 |
| `pref` | `finger_print` | false | 생체 인증 |
| `pref` | `skip_note` | true | 목록에서 3줄만 보기. 옮기지 않는다: 새 앱은 목록 미리보기를 늘 3줄로 두고 전체 글은 상세에서 본다(2026-11-01 결정) |
| `pref` | `ask_location` | true | 위치 안내를 다시 물을지 |
| `pref` | `permission_guide` | false | 권한 안내를 본 적 있는지. 새 온보딩에서는 쓰지 않는다 |
| `pref` | `<이메일>`, `<이메일>'s id` | - | 구글 계정 로그인 여부와 드라이브 백업 파일 ID. 이전 백업 찾기에 쓴다 |
| `pref2` | `is_alarm_key`, `hour_key`, `minute_key` | false, 22, 0 | 매일 알림. 새 앱에서 다시 예약한다 |

### 글꼴 대응

새 앱 글꼴은 10종이다(`docs/design.md` 일기 본문 글꼴). 코드(`GraphFragment.getCurTypeFace`)로 확인한 `font_key`와 파일 대응, 새 글꼴은 아래와 같다.
같은 글꼴이 없으면 가장 비슷한 글꼴로 옮기고 처음 열 때 한 번 알린다.

| `font_key` | 지금 파일 | 지금 글꼴 | 새 글꼴 |
| --- | --- | --- | --- |
| -1, 0 | `main_font.ttf` | Pretendard | Pretendard |
| 100 | 시스템 | 시스템 sans-serif | 시스템 글꼴 |
| 1 | `font.otf` | 국립공원 꼬미 | 국립공원 꼬미 |
| 2 | `font2.ttf` | 교보손글씨 2019 | 나눔손글씨 펜(교보문고가 서면으로 허락하면 교보손글씨 2019) |
| 3 | `font3.ttf` | 나눔손글씨 점꼴체 | 나눔손글씨 점꼴체 |
| 4 | `font4.ttf` | 넥슨 배찌체 | 감자꽃 |
| 5 | `font5.ttf` | 777미니콩다방 | 국립공원 꼬미 |
| 6 | `font6.ttf` | 777꼬마나비 | 하이멜로디 |
| 7 | `font7.ttf` | 심경하체 | 나눔손글씨 펜 |
| 8 | `font8.ttf` | 강원교육모두 | 고운바탕 |
| 9 | `font9.ttf` | 쿠키런 | 카페24 써라운드 |
| 10 | `font10.ttf` | 온글잎 만두몽키체 | 감자꽃 |
| 11 | `font11.ttf` | 온글잎 윤우체 | 나눔손글씨 펜 |
| 12 | `font12.ttf` | 코트라 희망체 | 카페24 써라운드 |
| 13 | `font13.ttf` | ACC어린이마음고운 | 하이멜로디 |
| 그 밖의 값 | - | 지금 앱은 기본 글꼴로 보임 | Pretendard |

새 글꼴 10종이 2026-11-01 다시 정해지며(사용자 결정) 대응표도 바꿨다. "가장 비슷한" 짝은 분위기(가는 손글씨, 동글한 손글씨, 바탕, 둥근 고딕)로 고른 판단이고,
개발 때 실제 글자를 나란히 견줘 확정한다.
777 글꼴 두 종(5·6)은 1.7.5로 먼저 빼려다 내지 않기로 했다(2026-11-01). 고르던 사람은 리뉴얼 이전에서 위 표대로 바뀌고 처음 열 때 한 번 알린다.

### 구글 드라이브의 이전 백업

- 사용자 "내 드라이브"에 `note.db`(mimeType `application/x-sqlite3`, 설명 "Diary app backup file")로 있다. 스코프는 `drive.file`이라 이 앱(같은 Google Cloud 프로젝트의 OAuth 클라이언트)이 만든 파일만 보인다.
- 새 앱이 이 파일을 보려면 **지금과 같은 Google Cloud 프로젝트**의 OAuth 클라이언트를 써야 한다. 프로젝트를 새로 만들면 이전 백업을 못 찾는다. iOS 클라이언트도 같은 프로젝트에 만든다(같은 프로젝트면 `drive.file` 파일이 보이는지 조사로 확인할 것).
- 이 파일에는 사진이 없다. 기기를 바꾼 사용자는 글만 돌려받을 수 있다.

## 이전 원칙

1. **원본은 건드리지 않는다.** 이전하는 동안 `note.db`, `-wal`, `-shm`, 캐시의 사진, SharedPreferences를 고치거나 지우지 않는다. 지우는 것은 새 앱에서 첫 정상 백업이 끝나고 몇 버전 지난 뒤, 따로 결정한다. 그때까지 사진은 캐시 원본과 `media/` 사본으로 두 번 자리를 차지한다(캐시 원본은 OS가 지울 수 있다).
2. **먼저 안전한 곳에 사본을 둔다.** 새 앱 첫 실행에서 어떤 연결도 열기 전에 `note.db`, `note.db-wal`, `note.db-shm`을 바이트 그대로 함께 `filesDir/legacy-snapshot/`으로 복사한다. `shared_prefs/*.xml`도 복사하되 `pref.xml`에서는 `password` 줄을 뺀다. DB가 가리키는 사진은 `filesDir/media/<SHA-256>.<ext>`로 한 번만 복사하고 이전이 실패해도 지우지 않으며, 사본 폴더에는 "경로 → 해시" 목록만 둔다(스펙 001 research R8). 캐시가 아니라 지워지지 않는 곳이다. DB를 한 번이라도 열면 닫을 때 WAL이 본 파일에 합쳐지므로 복사가 먼저다.
3. **사본에서 읽고, 새 DB에 쓰고, 검증한 뒤에 바꾼다.** 원본이 아니라 사본(세 파일)을 날 SQL로 열어 줄 단위로 읽는다. 기존 파일에 Room Migration을 거는 방식은 쓰지 않는다. 원본을 고치게 되고, 이전 코드에 버그가 있으면 되돌릴 곳이 없다. 임시 새 DB(Room)에 쓰고, 줄 수와 줄마다의 지문을 견준다. 지문은 레거시 줄의 값과, 새 DB의 값을 옛 표현으로 되돌린 값을 각각 이은 문자열의 SHA-256이다(형식은 스펙 001 `contracts/legacy-mapping.md`). 하나라도 다르면 새 DB를 버리고 이전 실패 화면으로 간다. 맞으면 이름 바꾸기로 한 번에 바꾼다.
4. **다시 해도 같은 결과다.** 이전 상태(`NotStarted → Snapshotting → Copying → Verifying → Committing → Done`, 끝은 `Done`·`Partial`·`Failed`)를 기록한다(스펙 001 `contracts/migration-flow.md`). 중간에 앱이 죽으면 새 DB를 버리고 처음부터 다시 한다. 원본이 그대로라 몇 번을 해도 안전하다.
5. **실패해도 기록을 보여 준다.** 이전이 실패하면 사용자는 레거시 DB를 읽기 전용으로 볼 수 있고, 원본 사본을 파일로 내보낼 수 있고, 문의 메일에 실패 단계(내용 없이 단계와 줄 수만)를 담을 수 있다.
6. **일기 내용은 밖으로 보내지 않는다.** 사용 통계와 비정상 종료 보고에는 이전 단계, 줄 수 구간, 사진 찾음/못 찾음 수만 보낸다.

## 옮겨 담기

| 레거시 | 새 모델 | 규칙 |
| --- | --- | --- |
| `_id` | `Entry.legacyId` | 새 ID는 UUID. 레거시 ID는 이전 백업 합치기와 문의 대응에만 쓴다 |
| `create_date` | `Entry.date`(LocalDate), `Entry.time`(LocalTime), `Entry.legacyCreateDate` | 이전하는 기기의 시간대로 해석한다. 원문 문자열을 함께 남긴다. 날짜를 골라 쓴 일기는 시각을 `HH:mm:SS`(S는 밀리초)로 만들어 초 자리가 `05`~`999`일 수 있다(`Utils.timeFormat2`, `WriteFragment` 저장). 그래서 날짜는 앞 10글자(`yyyy-MM-dd`, 사용자가 고른 날)를 그대로 쓰고 시각은 시와 분만 쓴다. 옛 앱은 이 값을 느슨하게 읽어 23시 5x분 일기를 다음 날로 보였을 수 있다. 앞 10글자와 느슨한 해석의 날짜가 다른 줄은 이전 보고에 수를 센다 |
| `modify_date` | `Entry.updatedAt` | 파싱 실패하면 `create_date` |
| `contents` | `Entry.bodyHtml`(제한된 HTML), `Entry.plainText` | `plainText`에는 원문을 그대로 넣는다. `bodyHtml`은 원문을 HTML 이스케이프하고 줄바꿈을 문단·줄바꿈 태그로 바꿔 넣는다. 보이는 글은 원문과 한 글자도 달라지면 안 되므로, `bodyHtml`을 다시 평문으로 바꾼 값이 `plainText`와 같은지 검증 지문에 넣는다 |
| `mood` | `Entry.mood` | 위 표의 0~8을 안정된 문자열 ID(`angry`, `cool`, `sad`, `sick`, `laugh`, `meh`, `bad`, `good`, `tired`)로. -1은 null |
| `weather` | `Entry.weather` | 0~6을 문자열 ID(`sunny`, `partly_cloudy`, `mostly_cloudy`, `cloudy`, `rain`, `sleet`, `snow`)로. -1은 null |
| `address` | `Entry.placeName` | 빈 문자열은 null |
| `star` | `Entry.isFavorite` | NULL은 false |
| `picture` | `Media` 여러 줄 | 쉼표로 나눠 순서대로 `sortOrder`. 파일이 있으면 `filesDir/media/<SHA-256>.<ext>`로 복사하고 DB에는 절대경로가 아니라 파일 이름과 가로세로를 담는다(백업 형식의 사진 이름과 같다, `docs/backup.md`). 없으면 `Media.status = MISSING`으로 "찾을 수 없는 사진"을 보여 준다. 원래 경로는 찾은 사진에도 `legacy_path`로 남긴다 |
| `year`, `month` | 담지 않음 | `create_date`에서 다시 만든다. 값이 어긋난 줄은 이전 보고에 수만 센다 |

같은 파일을 가리키는 사진(같은 초 덮어쓰기)은 내용 해시가 같으니 파일은 하나만 두고 `Media` 줄은 레거시 순서대로 둔다.

## 이전 백업 불러오기

새 백업 화면에서 드라이브의 레거시 `note.db`를 찾으면 "이전 버전 백업"으로 보여 준다.

- 불러오기는 지금 기기 일기를 지우지 않고 **합친다**. 같은 `create_date`와 같은 내용이면 건너뛰고, 같은 `create_date`에 내용이 다르면 둘 다 남기고 사용자에게 보여 준다.
- 레거시 백업의 `picture` 경로는 이 기기에 파일이 있을 때만 잇는다.
- 안드로이드에서 아이폰으로 옮기는 사용자의 유일한 길이므로 iOS에서도 같은 기능을 둔다.

## 출시 순서

1. **1.7.x 긴급 수정은 내지 않는다(사용자 결정 2026-10-31, `docs/DECISIONS.md`).** 리뉴얼 전까지 캐시에서 지워지는 사진은 되찾을 수 없다. 이전 코드는 캐시 경로의 사진을 찾아 옮기고, 못 찾은 사진은 `MISSING`으로 남겨 보여 준다. 777 글꼴만 빼는 1.7.5를 만들었지만 내지 않기로 했다(2026-11-01). Play의 마지막 레거시 버전은 1.7.4(`94179a4`)다.
2. **새 앱 내부 테스트.** 실제 1.7.4 APK를 깔고 일기를 쓴 뒤 새 APK로 `adb install -r` 업그레이드하는 테스트를 자동화한다(아래 시험).
3. **Play 단계적 출시.** 1% → 5% → 20% → 50% → 100%. 이전 실패율이 0이 아니면 멈추고 고친다. Play는 단계적 출시를 멈출 수 있지만 이미 받은 사람을 되돌릴 수 없다. 그래서 원칙 1, 5가 필요하다.
4. **minSdk.** 24 그대로다(2026-11-01 D4). 올리면 그 기기 사용자는 1.7.x에 남아 새 이전과 백업 수정을 못 받는다.

## 시험

이전 코드는 다른 기능보다 먼저, 더 많이 시험한다. 고정 파일(fixture)은 `shared/core/migration/src/androidHostTest/resources/legacy/`에 둔다(스펙 001 tasks T031).

- 버전 1 스키마로 만들고 2로 올린 DB(`star` NULL)
- 빈 DB, 테이블만 있는 DB, 1만 줄 DB
- 이모지, 따옴표, 쉼표, 10만 자 글, 빈 글
- 사진 경로: 있음, 없음, 같은 파일 두 번, 캐시 밖 경로, 공백이 든 경로
- `create_date` 형식이 어긋난 줄, `year`/`month`가 어긋난 줄
- 초 자리가 밀리초인 `create_date`(`2024-03-01 23:59:457`, `2024-03-01 10:00:05`): 날짜가 3월 1일로 남는지
- WAL에만 있고 체크포인트 안 된 줄(레거시 헬퍼로 쓰고 연결을 닫지 않은 채 복사)
- 중간에 프로세스를 죽였다가 다시 이전
- 잠금이 켜진 상태로 업데이트했을 때 잠금 화면이 먼저 뜨는지
- 실제 1.7.4 APK → 새 APK 업그레이드(계측 테스트, 에뮬레이터 API 24, 29, 35, 최신)
