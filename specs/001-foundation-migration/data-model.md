# Data Model: 기반과 이전

`Entry`, `Media`의 뜻과 화면 규칙은 스펙 002 [data-model.md](../002-diary-core/data-model.md)와 같고, 여기서는 저장 형식(DB 열, 파일 자리, 설정 키)과
이전·잠금에 필요한 것을 정한다. 레거시 값과의 대응은 [contracts/legacy-mapping.md](contracts/legacy-mapping.md), 근거는 [research.md](research.md)다.

**상태**: 확정(2026-11-01 사용자 확인). DB 열, 파일 자리, 설정·잠금 키를 바꾸려면 이전 시험·백업 형식(스펙 003)과 함께 고치고 `docs/DECISIONS.md`에 남긴다.

## 파일 자리

모두 앱 전용 저장소다. 캐시 폴더에는 아무것도 두지 않는다(헌법 I).

| 이름 | Android | iOS | 백업(스펙 003) |
| --- | --- | --- | --- |
| 일기 DB | `databases/diary.db` | `Application Support/diary.db` | 스냅샷으로 올림 |
| 사진 | `files/media/<sha256>.<ext>` | `Application Support/media/` | 올림 |
| 썸네일 | `no_backup/thumbs/<sha256>.jpg` | `Application Support/thumbs/`(백업 제외) | 안 올림 |
| 받은 글꼴 | `no_backup/fonts/<id>.ttf` | `Application Support/fonts/`(백업 제외) | 안 올림. 스토어에서 다시 받는다 |
| 설정 | `files/datastore/settings.preferences_pb` | `Application Support/datastore/` | 목록 파일의 `settings`로 올림(스펙 003 `contracts/backup-format.md`) |
| 잠금 | `files/datastore/lock.preferences_pb` | 같음 | 안 올림. Android 자동 백업에서도 뺀다 |
| 이전 상태·결과·작업 DB | `files/migration/state.json`, `report.json`, `diary-new.db` | 없음 | 안 올림 |
| 원본 사본 | `files/legacy-snapshot/` | 없음 | 안 올림 |

## DB: `diary.db` (Room 3, 버전 1)

R1 출시 때 버전 1이다. 002는 `draft`, `draft_media`를, 003은 백업 표를 출시 전에 같은 버전에 더한다.

### `entry`

| 열 | 타입 | 널 | 규칙 |
| --- | --- | --- | --- |
| `id` | TEXT PK | 아님 | UUID v4 소문자 |
| `date` | TEXT | 아님 | `yyyy-MM-dd`. 사용자가 고른 일기 날짜, 시간대와 상관없음 |
| `time` | TEXT | 아님 | `HH:mm` |
| `body_html` | TEXT | 아님 | 스펙 002 `contracts/rich-text-html.md`. 빈 글은 `""` |
| `plain_text` | TEXT | 아님 | 서식 없는 글. 레거시는 원문 그대로 |
| `mood` | TEXT | 가능 | `angry`·`cool`·`sad`·`sick`·`laugh`·`meh`·`bad`·`good`·`tired` |
| `weather` | TEXT | 가능 | `sunny`·`partly_cloudy`·`mostly_cloudy`·`cloudy`·`rain`·`sleet`·`snow` |
| `place_name` | TEXT | 가능 | 빈 문자열은 null |
| `is_favorite` | INTEGER | 아님 | 0/1 |
| `created_at` | INTEGER | 아님 | epoch 밀리초 |
| `updated_at` | INTEGER | 아님 | epoch 밀리초 |
| `deleted_at` | INTEGER | 가능 | 스펙 002 휴지통. null이면 보임 |
| `legacy_id` | INTEGER | 가능 | 레거시 `_id`. 유일 색인(null 여럿 허용) |
| `legacy_create_date` | TEXT | 가능 | 레거시 `create_date` 원문 |
| `legacy_location` | TEXT | 가능 | 레거시 `location_x`·`location_y`가 하나라도 비어 있지 않을 때만 `x|y` 원문 |

색인: `(deleted_at, date DESC, time DESC, created_at DESC)` 목록·달력, `(deleted_at, is_favorite)`, `legacy_id` 유일.

### `media`

| 열 | 타입 | 널 | 규칙 |
| --- | --- | --- | --- |
| `id` | TEXT PK | 아님 | UUID |
| `entry_id` | TEXT | 아님 | `entry.id` 외래 키, 일기를 영구 삭제하면 함께 지움 |
| `sort_order` | INTEGER | 아님 | 0부터. `(entry_id, sort_order)` 유일 |
| `status` | TEXT | 아님 | `normal`, `missing`(이전 때 원본을 못 찾음, 받은 파일이 확인값과 다름), `pending`(이름과 확인값은 알고 파일은 아직 기기에 없음, 스펙 003이 더함 2026-11-01). `pending`은 `file_name`·`sha256`이 있다 |
| `file_name` | TEXT | 가능 | `<sha256>.<ext>`. `missing`이면 null. 절대경로를 담지 않는다 |
| `sha256` | TEXT | 가능 | 소문자 hex. `missing`이면 null. 색인 |
| `width`, `height` | INTEGER | 가능 | EXIF 방향을 반영한 픽셀 크기. `missing`이면 null |
| `byte_size` | INTEGER | 가능 | 파일 크기. 백업 용량 계산에 쓴다 |
| `legacy_path` | TEXT | 가능 | 이전된 사진은 찾았든 못 찾았든 원래 경로. 새로 붙인 사진은 null |
| `created_at` | INTEGER | 아님 | epoch 밀리초 |

같은 파일(같은 해시)을 여러 일기나 한 일기에서 여러 번 가리키면 파일은 하나, 줄은 여럿이다.
사진 파일 정리(스펙 002)는 `media`·`draft_media` 어디에서도 가리키지 않는 파일만 지운다.

## 설정: `settings.preferences_pb` (DataStore)

| 키 | 타입 | 기본 | 뜻 |
| --- | --- | --- | --- |
| `font` | String | `pretendard` | `DiaryFont` ID |
| `body_text_size` | String | `medium` | `BodyTextSize` |
| `theme_mode` | String | `system` | `system`·`light`·`dark` |
| `ask_location_again` | Boolean | true | 레거시 `ask_location` |
| `reminder_enabled` | Boolean | false | 매일 알림 |
| `reminder_minute_of_day` | Int | 1320 | 22:00 |
| `font_replaced_from` | String? | 없음 | 옛 글꼴이 대응표로 바뀌었을 때 옛 글꼴 이름. 처음 열 때 한 번 알리고 지운다 |
| `legacy_backup_account` | String? | 없음 | 옛 앱이 로그인한 구글 계정(스펙 003이 이전 백업 찾기에 씀) |
| `legacy_backup_file_id` | String? | 없음 | 옛 앱이 올린 드라이브 `note.db` 파일 ID |
| `list_style`, `favorites_only` | String, Boolean | `feed`, false | 스펙 002 |

### DiaryFont

| ID | 글꼴 | 레거시 `font_key` |
| --- | --- | --- |
| `pretendard` | Pretendard | -1, 0, 그 밖의 값 |
| `system` | 시스템 글꼴 | 100 |
| `gowun_dodum` | 고운돋움 | 없음(새 글꼴) |
| `cafe24_surround` | 카페24 써라운드 | 9, 12 |
| `maru_buri` | 마루 부리 | 없음(새 글꼴) |
| `gowun_batang` | 고운바탕 | 8 |
| `nanum_pen` | 나눔손글씨 펜 | 2, 7, 11 |
| `nanum_jeomggol` | 나눔손글씨 점꼴체 | 3 |
| `kkomi` | 국립공원 꼬미 | 1, 5 |
| `gamja_flower` | 감자꽃 | 4, 10 |
| `hi_melody` | 하이멜로디 | 6, 13 |

옛 `font_key`가 1, 3, -1, 0, 100이 아니면(같은 글꼴이 없으면) `font_replaced_from`에 옛 이름을 남겨 처음 열 때 한 번 알린다.
교보문고가 서면으로 허락하면 `kyobo_2019`를 더하고 2를 그쪽으로 잇는다.

### BodyTextSize

| ID | 크기/줄 높이(sp) | 레거시 `font_size` |
| --- | --- | --- |
| `small` | 15/24 | 0, 1 |
| `medium` | 17/28(기본, `reading`) | 2, 없음 |
| `large` | 19/30 | 3 |
| `xlarge` | 21/34 | 4 |
| `xxlarge` | 24/38 | 없음 |

일기 본문에만 걸리고 기기 글자 크기 위에 곱해진다. 앱 틀은 기기 글자 크기만 따른다. 옛 단계는 같은 자리로 옮긴다(research R17, 사용자 결정 2026-11-01).

## 잠금: `lock.preferences_pb` (DataStore)

| 키 | 타입 | 기본 | 뜻 |
| --- | --- | --- | --- |
| `enabled` | Boolean | false | |
| `pin_mac` | String | 없음 | Base64 `HMAC-SHA256(key, salt ‖ PIN)`. PIN 평문은 어디에도 없다 |
| `pin_salt` | String | 없음 | Base64 16바이트 임의값 |
| `key_alias` | String | `diary.pin.v1` | Keystore·키체인 키 이름 |
| `biometric_enabled` | Boolean | false | 레거시 `finger_print` |
| `relock_after` | String | `one_minute` | `immediately`·`one_minute`·`five_minutes`·`cold_start` |
| `failed_attempts` | Int | 0 | 연속 틀린 횟수, 맞으면 0 |
| `retry_after` | Long | 0 | 다시 입력할 수 있는 epoch 밀리초(5번 틀리면 지금 + 30초) |

잠금 상태(실행 중에만):

```text
[잠김] --비밀번호 맞음 / 생체 인증 성공--> [열림]
[잠김] --5번째 틀림--> [기다림 30초] --시간 지남--> [잠김]
[열림] --백그라운드에서 relock_after만큼 지남 / 프로세스 새로 시작--> [잠김]
[잠김] --비밀번호를 잊어버렸어요 + 폰 화면 잠금 확인--> [새 비밀번호 정하기] --> [열림]
[잠김, 키 없음] --> [폰 화면 잠금 확인] --> [새 비밀번호 정하기]
```

## 이전

### MigrationState (`migration/state.json`)

`version`(1), `step`(`NotStarted`·`Snapshotting`·`Copying`·`Verifying`·`Committing`·`Done`·`Partial`·`Failed`), `attempt`(몇 번째 시도),
`retriedVersionCode`(처음 옮겼거나 `Partial`을 마지막으로 다시 읽은 앱 버전, 없으면 0. 2026-11-01: 처음 옮긴 버전도 적어 같은 버전에서는 다시 읽지 않는다), `photoRecheckAt`(찾을 수 없는 사진을 마지막으로 다시 본 시각), `startedAt`, `updatedAt`, `failure`(`step`, `kind`: `NO_SPACE`·`SNAPSHOT_IO`·`READ_ERROR`·`WRITE_ERROR`·`MISMATCH`·`UNKNOWN`). 일기 내용, 경로, 예외 메시지는 담지 않는다.
단계와 다시 시작 규칙은 [contracts/migration-flow.md](contracts/migration-flow.md).

### MigrationReport (`migration/report.json`)

| 값 | 뜻 |
| --- | --- |
| `entriesTotal` | 레거시 줄 수(읽은 것 + 못 읽은 것 추정) |
| `entriesMigrated` | 옮긴 일기 수 |
| `entriesUnreadable` | 깨져서 못 읽은 줄 수. 1 이상이면 `Partial` |
| `photosFound`, `photosMissing` | 찾은 사진, 못 찾은 사진(경로 기준) |
| `dateDiffers` | 앞 10글자 날짜와 옛 앱의 느슨한 해석 날짜가 다른 일기 수 |
| `dateUnparsed` | 날짜를 읽지 못해 대체 규칙을 쓴 일기 수 |
| `thumbnailsFailed` | 썸네일을 못 만든 수(이전 실패 아님) |
| `settingsMigrated` | 옮긴 설정 이름 목록(글꼴, 글자 크기, 화면 모드, 위치 안내, 잠금, 생체, 알림, 알림 시각) |
| `finishedAt`, `outcome` | `Done` 또는 `Partial` |

### LegacySnapshot (`legacy-snapshot/`)

| 파일 | 내용 |
| --- | --- |
| `note.db`, `note.db-wal`, `note.db-shm` | 원본의 바이트 사본(있는 것만) |
| `pref.xml` | 원본에서 `password` 줄만 뺀 사본 |
| `pref2.xml` | 원본 사본 |
| `photos.json` | `[{ "path", "sha256" 또는 null, "size" }]` 레거시 경로 → 해시. 사진 자체는 `media/`에 있다 |
| `snapshot.json` | 만든 시각, 원본 파일마다 크기와 SHA-256(원본이 이전 전후로 같은지 시험이 견준다) |

이 폴더와 `media/`의 이전 사본은 이 스펙에서 지우지 않는다(`docs/migration.md` 원칙 1).

### LegacyBackupLink

옛 `pref.xml`에서 값이 `true`인 Boolean 키 가운데 이메일 모양인 것을 계정으로, `"<계정>'s id"` 문자열을 파일 ID로 읽어 설정의
`legacy_backup_account`, `legacy_backup_file_id`에 담는다(레거시 `BackupActivity`). 둘 다 없으면 비운다.

## 검증 규칙 요약

- `entry` 수 + `entriesUnreadable` = 레거시 줄 수.
- 줄마다 `legacyCanonical` = `restoredCanonical`(형식은 `contracts/legacy-mapping.md`).
- `media`의 `normal` 줄은 파일이 있고 다시 잰 SHA-256이 `sha256`과 같다.
- `photosFound + photosMissing` = 레거시 경로 수(빈 조각 제외).
- 설정 여덟 가지가 대응표대로다. `lock.pin_mac`은 옛 비밀번호로 확인된다.
