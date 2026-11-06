# Data Model: 백업 v2

스펙 001 [data-model.md](../001-foundation-migration/data-model.md)(확정 2026-11-01)의 `entry`, `media`, 설정 키 위에 이 스펙이 더하는 것을 정한다.
드라이브에 놓는 파일과 목록 파일 형식은 [contracts/backup-format.md](contracts/backup-format.md), 합치기 규칙은 [contracts/merge-rules.md](contracts/merge-rules.md)다.
근거는 [research.md](research.md)다.

**상태**: 확정(2026-11-01 사용자 확인). `media.status`에 `pending`을 더하고, 백업용 표 셋을 일기 표와 따로 두고, 영구 삭제 흔적은 두지 않고,
Android 시스템 백업(클라우드 자동 백업, 기기 간 이전)에는 일기를 넣지 않는다. 바꾸려면 백업 형식 버전·이전 시험과 함께 고치고 `docs/DECISIONS.md`에 남긴다.

## 파일 자리 (더하는 것)

모두 앱 전용 저장소다. 캐시 폴더에는 두지 않는다(헌법 I).

| 이름 | Android | iOS | 드라이브 백업 | Android 기기 간 이전 |
| --- | --- | --- | --- | --- |
| 백업 설정·상태 | `files/datastore/backup.preferences_pb` | `Application Support/datastore/` | 안 올림(기기 것) | 뺀다 |
| 진행 중인 백업·복원 | `files/backup/state.json`, `files/backup/restore.json` | `Application Support/backup/` | 안 올림 | 뺀다 |
| 올리기 전 스냅샷 | `files/backup/outgoing/<이름>.db` | 같음 | 이것을 올림. 끝나면 지운다 | 뺀다 |
| 받은 스냅샷 | `files/backup/incoming/<이름>.db` | 같음 | 안 올림. 합친 뒤 지운다 | 뺀다 |
| 되돌리기 사본 | `files/backup/rollback/<시각>/diary.db`, `settings.json` | 같음 | 안 올림. 7일 뒤 지운다 | 뺀다 |
| 묶음 파일 만들기 | `files/backup/export/` | 같음 | 안 올림. 저장이 끝나면 지운다 | 뺀다 |

드라이브에 올라가는 것은 스냅샷, 목록 파일, 사진뿐이다. 썸네일, 잠금 저장소, 이전 작업 파일, 위 표의 기기 파일은 올리지 않는다(FR-011).

## DB: `diary.db` (Room 3, 버전 2)

처음에는 스펙 001 R4대로 버전 1에 모으려 했다. 그러면 이미 시험 기기에 깔린 DB가 켜지다 죽어서 버전 2와 자동 이전으로 바꿨다(2026-11-05).

### `media.status`에 `pending` 더하기 (스펙 001 표를 바꿈, 사용자 확인 2026-11-01)

| 값 | 뜻 | `file_name`·`sha256` | 파일 |
| --- | --- | --- | --- |
| `normal` | 기기에 파일이 있다 | 있음 | 있음 |
| `missing` | 원본을 찾을 수 없다(이전 때 못 찾음, 받은 파일이 확인값과 다름) | null | 없음 |
| `pending` (새로) | 내용과 이름은 알고 파일은 아직 기기에 없다 | 있음 | 없음 |

- 복원은 일기 글을 먼저 한 번에 합치고 사진은 그 뒤에 받는다. 받는 동안 사진 줄은 `pending`이고, 받은 파일의 확인값이 맞으면 `normal`이 된다.
  끊겨도 `pending`인 줄만 다시 받으면 된다(FR-024).
- 공간이 부족해 사진 일부만 올라간 백업(FR-018)을 복원하면, 백업에 없는 사진은 `pending`으로 남는다. 화면에는 "찾을 수 없는 사진"이 아니라
  "원래 기기에 있는 사진, 아직 백업되지 않았어요"로 보인다. 나중에 그 사진이 든 백업을 다시 합치면 채워진다.
- 같은 값으로 스펙 001의 열린 문제(이전할 때 기기 공간이 부족하면 사진을 나중에 옮기기)도 풀 수 있다. 그때 쓸지는 스펙 001 개발에서 정한다.
- `pending`은 사진 정리(스펙 002)가 지우지 않고, 백업할 때 올릴 파일이 없으므로 목록 파일에 "백업에 없음"으로 적는다.

### `backup_media` (새로)

드라이브에 올라가 확인까지 끝난 사진. 드라이브가 원본이고 이 표는 그 거울이다. 백업마다 드라이브의 `media/` 목록으로 다시 맞춘다.

| 열 | 타입 | 널 | 규칙 |
| --- | --- | --- | --- |
| `account_key` | TEXT | 아님 | 연결한 구글 계정(research R2). 계정마다 따로 |
| `sha256` | TEXT | 아님 | 사진 내용 확인값, 소문자 hex |
| `drive_file_id` | TEXT | 아님 | 드라이브 파일 ID |
| `byte_size` | INTEGER | 아님 | |
| `confirmed_at` | INTEGER | 아님 | 드라이브 확인값을 맞춰 본 epoch 밀리초 |

기본 키 `(account_key, sha256)`. "사진 1,203장 모두 백업됨"과 "아직 백업되지 않은 사진 420장"은 이 표와 `media`를 견줘 센다.

### `sync_base` (새로)

일기마다 "마지막으로 백업이나 복원으로 맞춰진 때의 `updated_at`". 합치기에서 어느 쪽이 고쳐졌는지 가르는 기준이다(FR-022).

| 열 | 타입 | 널 | 규칙 |
| --- | --- | --- | --- |
| `entry_id` | TEXT PK | 아님 | `entry.id` 외래 키, 일기를 영구 삭제하면 함께 지움 |
| `base_updated_at` | INTEGER | 아님 | 끝까지 된 백업에 실린 값, 또는 복원으로 받은 값 |

- 백업이 끝나면(목록 파일까지 확인) 그 스냅샷에 실린 일기의 `updated_at`으로 고친다.
- 복원으로 더하거나 바꾼 일기는 받은 `updated_at`으로 고친다.
- `entry`에 열을 더하지 않고 표를 따로 둔 까닭: 이 값은 기기 것이라 스냅샷에서 비우고, 일기 표를 쓰는 화면 코드에 섞이지 않게 하려고.

### `merge_mark` (새로)

합치기 결과로 사용자에게 알려야 하는 일기. "두 가지로 남겼어요" 표시의 원본이다.

| 열 | 타입 | 널 | 규칙 |
| --- | --- | --- | --- |
| `entry_id` | TEXT PK | 아님 | `entry.id` 외래 키, 영구 삭제하면 함께 지움 |
| `kind` | TEXT | 아님 | `kept_both`(양쪽 다 고쳐 둘 다 남김), `legacy_same_time`(이전 버전 백업에서 같은 날짜·시각에 글이 다름) |
| `pair_entry_id` | TEXT | 가능 | 짝이 되는 일기 |
| `source` | TEXT | 아님 | `drive`·`file`·`legacy_drive`·`legacy_file` |
| `created_at` | INTEGER | 아님 | |

사용자가 표시를 닫거나 둘 중 하나를 지우면 줄을 지운다.

### 기기 것인 표는 스냅샷에서 비운다

스냅샷은 DB 전체를 `VACUUM INTO`로 뜨고, 그 사본에서 `backup_media`, `sync_base`, `merge_mark`, `draft`, `draft_media`를 비운 뒤 확인값을 잰다.
임시 저장본은 기기를 옮길 때 복원하지 않는다(research R5).

### `updated_at` 규칙 (스펙 002 코드에 걸림)

합치기가 맞게 고르려면 일기에 보이는 값이 바뀔 때마다 `updated_at`이 바뀌어야 한다. 글, 기분, 날씨, 장소, 날짜·시각, 사진 더하기·빼기·순서에 더해
즐겨찾기와 휴지통 넣기·꺼내기도 `updated_at`을 바꾼다. 스펙 002 작업(저장, 즐겨찾기, 휴지통)에 이 규칙을 넣는다.

## 백업 설정·상태: `backup.preferences_pb` (DataStore, 새로)

스냅샷에 넣지 않고 기기 간 이전에서도 뺀다.

| 키 | 타입 | 기본 | 뜻 |
| --- | --- | --- | --- |
| `account_key` | String? | 없음 | 연결한 계정. 없으면 연결 안 됨 |
| `account_label` | String? | 없음 | 화면에 보일 이메일 |
| `root_folder_id` | String? | 없음 | 드라이브 "1일 1일기 백업" 폴더 ID. 없어지면 다시 찾고 없으면 새로 만든다 |
| `device_id` | String | 처음 실행 때 만든 UUID | 백업 이름과 목록 파일의 기기 구분. 앱을 지웠다 깔면 새로 만든다 |
| `auto_enabled` | Boolean | false | 연결하면 true로 켠다(FR-013) |
| `photos_on_cellular` | Boolean | false | 사진을 모바일 데이터로도 올린다(FR-014) |
| `last_success_at` | Long? | 없음 | 끝까지 된 마지막 백업 시각 |
| `last_success_snapshot` | String? | 없음 | 그 스냅샷 이름 |
| `last_attempt_at` | Long? | 없음 | |
| `failing_since` | Long? | 없음 | 연속 실패가 시작된 시각. 성공하면 지운다 |
| `last_failure` | String? | 없음 | `AUTH`·`NO_SPACE`·`OFFLINE`·`DRIVE_ERROR`·`LOCAL_IO`·`UNKNOWN` |
| `failure_notified_at` | Long? | 없음 | 7일 실패 알림을 보낸 시각. 성공하면 지운다(FR-016) |
| `photos_not_backed_up` | Int | 0 | 마지막 백업에서 못 올린 사진 수 |
| `nudge_dismiss_count` | Int | 0 | 권하기 카드 "나중에" 횟수(FR-036) |
| `nudge_hidden_until` | Long? | 없음 | 이때까지 카드를 숨긴다 |
| `user_disconnected` | Boolean | false | 스스로 연결을 끊었으면 true. 카드를 다시 띄우지 않는다 |
| `rollback_until` | Long? | 없음 | 되돌리기 사본이 남는 마지막 시각(FR-027) |

## 진행 상태 파일

### BackupRun (`backup/state.json`)

> 2026-11-05 구현에서는 상태 파일 없이 잇는다. 올린 사진은 내용 해시로 드라이브와 대어 다시 올리지 않고, 받을 사진은 `media.status = pending` 줄이 곧 남은 일이다. 아래 표는 처음 설계로 남겨 둔다.

`version`(1), `runId`, `trigger`(`manual`·`auto`·`app_open`), `step`(`Preparing`·`Snapshotting`·`UploadingPhotos`·`UploadingSnapshot`·
`UploadingManifest`·`Verifying`·`Pruning`·`Done`·`Failed`), `snapshotName`, `snapshotSha256`, `photosTotal`, `photosDone`, `photosSkippedForSpace`,
`resumable`(올리던 파일 하나의 `sha256`, 세션 주소, 보낸 바이트), `startedAt`, `updatedAt`, `failure`(`step`, `kind`).
일기 내용과 사진 경로는 담지 않는다. 세션 주소는 드라이브가 정한 일회용 주소다.

### RestoreJob (`backup/restore.json`)

`version`(1), `jobId`, `source`(`drive`·`file`·`legacy_drive`), `snapshotName`, `mode`(`merge`·`replace`), `step`(`Downloading`·`Verifying`·
`Previewing`·`Applying`·`FetchingPhotos`·`Done`·`Failed`), `preview`(아래 RestorePreview), `photosTotal`, `photosDone`, `photosMissing`,
`rollbackDir`(바꾸기일 때), `failure`.

## 모델 (shared/core/model)

| 이름 | 필드 |
| --- | --- |
| `BackupStatus` | `connection`(`NotConnected`·`Connected(accountLabel)`·`NeedsReconnect`), `lastSuccessAt`, `photosNotBackedUp`, `running`(`BackupProgress?`), `failingSince`, `lastFailure`, `autoEnabled`, `photosOnCellular` |
| `BackupProgress` | `step`, `photosDone`, `photosTotal`, `bytesDone`, `bytesTotal` |
| `BackupEntry`(복원 목록 한 줄) | `snapshotName`, `deviceName`, `platform`, `createdAt`, `entryCount`, `photoCount`, `photosNotInBackup`, `appVersion`, `state`(`Ready`·`Damaged(reason)`·`NeedsNewerApp`) |
| `LegacyBackupEntry` | `driveFileId`, `modifiedAt`, `byteSize` |
| `RestorePreview` | `entriesAdded`, `entriesUpdated`, `entriesKeptBoth`, `entriesUnchanged`, `photosToDownload`, `bytesToDownload`, `photosNotInBackup`, `settingsChange`, `deviceBytesFree` |
| `RestoreResult` | `entriesAdded`, `entriesUpdated`, `entriesKeptBoth`, `photosRestored`, `photosMissing`, `photosPending`, `rollbackUntil?` |
| `BackupNudge` | `entryCount`, `photoCount`, `suggestedAccount?` |

## 상태 기계

```text
백업:
[Preparing] → [Snapshotting] → [UploadingPhotos] → [UploadingSnapshot] → [UploadingManifest] → [Verifying] → [Pruning] → [Done]
     어느 단계든 실패 → [Failed(kind)] → 다음 시도는 처음부터 다시 계획하되, 드라이브에 이미 있는 사진과 세션 주소가 살아 있는 파일은 이어서

복원:
[Downloading] → [Verifying] → [Previewing] --사용자 확인--> [Applying(한 트랜잭션)] → [FetchingPhotos] → [Done]
   Verifying 실패 → [Failed(MISMATCH)] 기기 기록은 그대로
   Applying 전 취소 → 아무것도 바뀌지 않음
   FetchingPhotos 중 끊김 → 다음 실행에서 pending 줄만 이어 받기
```
