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
| `Copying` | 사본을 `_id` 순으로 읽어 `contracts/legacy-mapping.md`대로 바꾸고 `diary-new.db`에 200줄씩 트랜잭션으로 쓴다. 사진은 `media/`로 복사하고 `photos.json`에 적는다. 줄임 사진은 최대 3개씩 나란히(실패는 세기만). 깨진 범위는 반씩 나눠 읽히는 줄만 건진다 | `READ_ERROR`, `WRITE_ERROR` |
| `Verifying` | 사본을 다시 읽어 줄마다 `legacyCanonical`과 새 DB의 `restoredCanonical`을 견주고, 사진 파일 해시와 개수를 본다 | `MISMATCH` |
| `Committing` | `diary-new.db`를 닫고 `diary.db`로 이름을 바꾼 뒤 `report.json`, `Done`(못 읽은 줄이 있으면 `Partial`)을 쓴다 | 다음 시작에서 지문만 다시 보고 맞으면 `Done` |

모든 실패는 `Failed`와 `failure.kind`를 쓰고 `diary-new.db`를 지운다. `legacy-snapshot/`과 `media/`는 남긴다.

## 화면 상태 (`MigrationGate`)

| 상태 | 화면 | 앱 틀로 가는가 |
| --- | --- | --- |
| 원본 확인 중(보통 1초 안) | 바탕색만 | 아니오 |
| `Snapshotting` | "기록을 옮길 준비를 하고 있어요"와 끝을 모르는 진행 표시 | 아니오 |
| `Copying`, `Verifying`, `Committing` | 일기 n/N, 사진 n/N(0.25초마다 갱신), 화면 꺼짐 막기. 일기 내용과 사진은 없다 | 아니오 |
| `Done`·`Partial` 직후 한 번 | 결과(옮긴 일기, 찾은·못 찾은 사진, 못 읽은 일기, 날짜가 달랐던 일기). `Partial`이면 "일부만 옮겨졌어요"와 다시 시도 | 확인을 누르면 예 |
| `Failed` | 무엇이 안 됐는지와 다시 시도, 예전 일기 보기, 원본 파일 내보내기, 문의하기 | 아니오. 새 일기를 쓰면 다시 시도가 그 일기를 덮을 수 있어서다 |

- 잠금이 켜져 있으면 이 화면들은 모두 잠금을 연 뒤에만 보인다(FR-015).
- 결과는 설정 "기록 이전 결과"에서 다시 본다(FR-010).

## 실패 화면의 동작

- **다시 시도**: `attempt`를 하나 올리고 `Snapshotting`부터. 사본이 이미 있고 원본의 크기·SHA-256이 `snapshot.json`과 같으면 사본 복사는 건너뛴다.
- **예전 일기 보기**: `legacy-snapshot/note.db`를 읽기 전용으로 열어 `_id` 역순으로 60줄씩 보여 준다(날짜, 글, 기분, 날씨, 장소, 사진 수).
  사진은 `photos.json`으로 `media/`에서 찾는다. 고치기·지우기는 없다.
- **원본 내보내기**: 저장 창(Android SAF, FileKit)으로 고른 곳에 `1일1일기-원본-yyyyMMdd.zip`을 쓴다. 담는 것: `legacy-snapshot/`의 파일 전부
  (`pref.xml`은 비밀번호 줄을 뺀 사본), `media/`의 이전 사진, `README.txt`(무엇인지, 문의처).
- **문의하기**: 메일 앱으로 `sjunh812@naver.com`(옛 앱 문의처)에 보낸다. 본문에는 앱·OS 버전, 실패 단계와 종류, 시도 횟수, 일기·사진 수 구간만 넣는다.

## `Partial`에서 다시 시도 (FR-011)

새 DB를 버리지 않는다. 사본에서 `legacy_id`가 아직 `entry`에 없는 줄만 다시 읽어 더하고, 더한 줄만 지문으로 검증한다. 그 사이 사용자가 쓴 일기는 그대로다.

## 보내도 되는 값 (FR-013)

| 이벤트 | 값 |
| --- | --- |
| `migration_started` | `entries_bucket`, `photos_bucket`, `attempt` |
| `migration_finished` | `outcome`(`done`·`partial`), `duration_bucket`, `missing_photos_bucket`, `unreadable_bucket` |
| `migration_failed` | `step`, `kind`, `attempt` |

구간은 `0`, `1-9`, `10-99`, `100-999`, `1000-9999`, `10000+`, 시간 구간은 `<10s`, `<30s`, `<2m`, `<10m`, `10m+`다. 예외 메시지, 경로, 글, 기분은 보내지 않는다.
