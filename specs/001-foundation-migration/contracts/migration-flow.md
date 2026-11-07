# 이전 흐름 계약

`shared/core/migration`의 `MigrationRunner`와 `shared/feature/migration` 화면이 지키는 순서다. iOS는 옮길 것이 없어 늘 "없음"이다.

## 시작할 때

```text
프로세스 시작
 ├─ state.json이 Done·Partial → 이전 없음(관문 통과)
 ├─ 원본(databases/note.db 또는 shared_prefs/pref.xml)이 없음 → 이전 없음(새 설치). 상태 파일을 쓰지 않는다
 └─ 원본 있음
     ├─ (스플래시 동안) 잠금·설정 잇기: pref.xml, pref2.xml을 읽기만 해서 lock·settings 저장소에 쓴다. 이미 했으면 건너뜀
     └─ (스플래시 뒤, 잠금 화면 뒤에서) 이전 단계 시작. state.json이 Done·Partial 말고 다른 단계면 diary-new.db를 지우고 처음부터
```

잠금·설정 잇기는 DB 이전과 따로 끝난다. DB 이전이 실패해도 잇기 결과는 남는다(원본은 그대로라 다시 해도 같다).

## 단계

| 단계 | 하는 일 | 실패하면 |
| --- | --- | --- |
| `Snapshotting` | 남은 공간 확인(research R8 식). `note.db`, `-wal`, `-shm`을 스트림으로 `legacy-snapshot/`에 복사하며 원본 SHA-256을 잰다. `pref.xml`(비밀번호 줄 뺌), `pref2.xml` 복사. `snapshot.json` 쓰기 | `NO_SPACE`(필요한 양 함께), `SNAPSHOT_IO` |
| (판단) | 사본을 열어 `Note` 줄 수를 센다. 0이면 `Done`(결과 화면 없음, US1-9) | `READ_ERROR` |
| `Copying` | 사본을 `_id` 순으로 읽어 `contracts/legacy-mapping.md`대로 바꾸고 `diary-new.db`에 200줄씩 트랜잭션으로 쓴다. 사진은 `media/`로 복사하고 `photos.json`에 적는다. 썸네일은 최대 3개씩 나란히(실패는 세기만). 깨진 범위는 반씩 나눠 읽히는 줄만 건진다 | `READ_ERROR`, `WRITE_ERROR` |
| `Verifying` | 사본을 다시 읽어 줄마다 `legacyCanonical`과 새 DB의 `restoredCanonical`을 견주고, 사진 파일 해시를 본다. 지문이 다른 일기는 새 DB에서 빼고 못 읽은 일기로 센다. 사진 파일이 깨졌으면 일기는 두고 그 사진만 `missing`으로 바꾼다 | 한 편도 남지 않을 때만 `MISMATCH` |
| `Committing` | `diary-new.db`를 닫고 `diary.db`로 이름을 바꾼 뒤 `report.json`, `Done`(못 읽은 줄이 있으면 `Partial`)을 쓴다 | 다음 시작에서 지문만 다시 보고 맞으면 `Done` |

일기 한 편의 문제로는 멈추지 않는다. 바꾸기나 쓰기에 실패한 일기는 빼고 나머지를 옮겨 `Partial`로 끝낸다. 뺀 일기는 앱이 업데이트된 뒤 처음 열 때 다시 읽는다(2026-11-04 사용자 결정).

실패는 공간 부족, 사본 실패, 읽기 실패, 한 편도 옮기지 못한 경우뿐이다. 모든 실패는 `Failed`와 `failure.kind`를 쓰고 `diary-new.db`를 지운다. `legacy-snapshot/`과 `media/`는 남긴다.

## 화면 상태 (`MigrationGate`)

| 상태 | 화면 | 앱 틀로 가는가 |
| --- | --- | --- |
| 원본 확인 중(보통 1초 안) | 바탕색만 | 아니오 |
| `Snapshotting` | "기록을 옮길 준비를 하고 있어요"와 끝을 모르는 진행 표시 | 아니오 |
| `Copying`, `Verifying`, `Committing` | 일기 n/N, 사진 n/N(0.25초마다 갱신), 화면 꺼짐 막기. 일기 내용과 사진은 없다 | 아니오 |
| `Done`·`Partial` 직후 한 번 | 결과(옮긴 일기, 찾은·못 찾은 사진, 못 읽은 일기, 날짜가 달랐던 일기). `Partial`이면 "일부만 옮겨졌어요"와 "앱을 업데이트하면 한 번 더 읽어 봐요" | 확인을 누르면 예 |
| `Failed` | 무엇이 안 됐는지와 다시 시도, 예전 일기 보기(사본이 있을 때만, 없으면 줄을 숨긴다), 원본 파일 내보내기, 문의하기. 공간 부족이 아니면 새 일기부터 쓰기(`Deferred`로 앱 틀로 간다) | 공간 부족은 아니오. 그 밖에는 사용자가 고르면 예. 다시 시도는 사본에서 없는 줄만 지금 DB에 더해 새로 쓴 일기를 덮지 않는다. 처음부터 다시 하면 그 일기를 덮을 수 있어서다 |

- 잠금이 켜져 있으면 이 화면들은 모두 잠금을 연 뒤에만 보인다(FR-015).
- 결과는 설정 "기록 이전 결과"에서 다시 본다(FR-010).

## 실패 화면의 동작

- **다시 시도**: `attempt`를 하나 올리고 `Snapshotting`부터. 사본이 이미 있고 원본의 크기·SHA-256이 `snapshot.json`과 같으면 사본 복사는 건너뛴다.
- **예전 일기 보기**: `legacy-snapshot/note.db`를 읽기 전용으로 열어 일기 날짜 최근 순(`create_date` 역순, 같으면 `_id` 역순)으로 60줄씩 보여 준다
  (날짜, 글, 기분, 날씨, 장소, 사진). 사용자가 지난 날짜를 골라 쓸 수 있어 `_id` 순은 날짜 순과 다르다(2026-11-01 에뮬레이터 확인).
  사진은 `photos.json`으로 `media/`에서 찾는다. 수정·삭제는 없다.
- **원본 내보내기**: 저장 창(Android SAF, FileKit)으로 고른 곳에 `1일1일기-원본-yyyyMMdd.zip`(영어 `1Day1Diary-original-…`, 일본어 `1日1日記-元データ-…`, 문자열 `migration_export_file_name`)을 쓴다. 담는 것: `legacy-snapshot/`의 파일 전부(사본을 만들기 전에 실패했으면 빠진 파일을 원본에서 읽기만 해서 같은 자리에 담는다. 담을 파일이 없으면 실패로 알린다)
  (`pref.xml`은 비밀번호 줄을 뺀 사본), `media/`의 이전 사진, `README.txt`(무엇인지, 문의처).
- **복구 문의(일부만 옮겨진 기기, FR-010a)**: 시트에서 "첨부해서 문의"를 고르면 `files/migration/support/1일1일기-문의용-yyyyMMdd.zip`(문자열 `support_file_name`, `note.db`,
  `-wal`, `-shm`. 설정 파일과 사진은 없음, 2026-11-04)을 만들어 메일 앱에 첨부로 넘기고, 하루가 지나면 앱을 열 때 지운다(2026-11-01: 메일 앱이 첨부를 언제 읽을지 알 수 없어 보낸 직후에는 지우지 않는다. 새로 만들 때는 남은 묶음을 먼저 지운다). 메일 본문 틀에는
  앱·OS 버전, 읽지 못한 일기 수, "보내 주신 파일은 고쳐 돌려드린 뒤 바로 지워요"를 넣는다. 캐시 폴더에 두지 않는다.
- **고치는 쪽(개발)**: 받은 파일을 SQLite 복구(`sqlite3 .recover`)로 읽히는 줄을 살려 정상 `note.db`로 만들고 zip으로 돌려준다. 사용자는
  "파일에서 복원하기"로 고른다. 앱은 파일 안이 예전 형식이면 이전 버전 백업과 같은 규칙(날짜·시각·글이 같으면 한 번만)으로 합친다. 돌려준 뒤 받은 파일과
  만든 파일을 지우고, 지운 날을 문의 기록에 남긴다.
- **문의하기**: 메일 앱으로 `sjunh812@naver.com`(옛 앱 문의처)에 보낸다. 본문에는 앱·OS 버전, 실패 단계와 종류, 시도 횟수, 일기·사진 수 구간만 넣는다.

## `Partial`에서 다시 읽기 (FR-011)

사용자가 누르는 버튼은 없다. `state.json`이 `Partial`이고 지금 앱의 `versionCode`가 `retriedVersionCode`보다 크면, 앱을 연 뒤(잠금을 연 뒤) 백그라운드에서
한 번 다시 읽고 `retriedVersionCode`를 지금 버전으로 쓴다. 새 DB를 버리지 않는다. 사본에서 `legacy_id`가 아직 `entry`에 없는 줄만 다시 읽어 더하고,
더한 줄만 지문으로 검증한다. 그 사이 사용자가 쓴 일기는 그대로다. 더한 일기가 있으면 "못 읽었던 일기 n편을 옮겼어요"를 한 번 알리고,
모두 읽히면 `Done`으로 바꾼다. 처음 옮길 때도 그 버전을 `retriedVersionCode`에 적어 같은 버전에서는 다시 읽지 않는다.
두 후속 작업은 `MigrationFollowUp`이 관문을 지난 뒤 한 번 돌리고, 알림은 앱 틀의 토스트로 띄운다.

## 찾을 수 없는 사진 다시 찾기 (FR-011)

하루 한 번 앱을 열 때 `media.status = missing`이고 `legacy_path`가 있는 줄의 원래 경로에 파일이 있는지 본다. 있으면 이전과 같은 방법으로
`media/<sha256>.<ext>`로 복사하고 해시를 다시 잰 뒤 `normal`로 바꾼다. 찾은 것이 있으면 "찾을 수 없던 사진 n장을 찾았어요"를 한 번 알린다.
캐시에서 지워진 사진은 다시 찾을 수 없다. 이 일은 일기를 쓰고 읽는 것을 막지 않는다.

## 보내도 되는 값 (FR-013)

| 이벤트 | 값 |
| --- | --- |
| `migration_started` | `entries_bucket`, `photos_bucket`, `attempt` |
| `migration_finished` | `outcome`(`done`·`partial`), `duration_bucket`, `missing_photos_bucket`, `unreadable_bucket` |
| `migration_failed` | `step`, `kind`, `attempt` |

구간은 `0`, `1-9`, `10-99`, `100-999`, `1000-9999`, `10000+`, 시간 구간은 `<10s`, `<30s`, `<2m`, `<10m`, `10m+`다. 예외 메시지, 경로, 글, 기분은 보내지 않는다.
