# 백업 형식 v2

드라이브 폴더와 묶음 파일(zip)에 무엇을 어떤 이름으로 두는지 정한다. 설계 배경은 `docs/backup.md` "백업 형식 v2", 근거는 [research.md](../research.md) R5~R10이다.
이 형식을 바꾸면 `formatVersion`을 올리고, 새 앱이 옛 버전을 읽는 시험을 같은 커밋에 더한다(FR-035).

## 드라이브 폴더

사용자 드라이브의 내 드라이브 맨 위에 보이는 폴더를 만든다. 권한은 `drive.file`이라 이 앱이 만든 파일만 보인다(FR-002).

```text
1일 1일기 백업/                                   appProperties diary.kind=root
  README.txt                                      이 폴더가 무엇인지, 지우면 백업이 사라진다는 안내(FR-003)
  snapshots/                                      diary.kind=snapshots
    2026-12-31_21-30-00_Galaxy-S24_ab12.db        스냅샷   diary.kind=snapshot, diary.device=<기기 ID>
    2026-12-31_21-30-00_Galaxy-S24_ab12.json      목록 파일 diary.kind=manifest, diary.device=<기기 ID>, diary.format=2
  media/                                          diary.kind=media
    3f9a…c1.jpg                                   사진     diary.kind=photo (정리 대상이면 diary.unrefSince=<epoch 밀리초>)
```

- 폴더와 파일은 이름이 아니라 `appProperties`로 찾는다. 사용자가 폴더 이름을 바꿔도 이어진다. 같은 종류가 둘 이상이면 가장 먼저 만든 것을 쓴다.
- 스냅샷·목록 파일 이름: `<기기 현지 시각 yyyy-MM-dd_HH-mm-ss>_<기기 이름, 공백은 ->_<기기 ID 앞 4자>`. 드라이브에서 사람이 보고 고를 수 있게 한다.
  정확한 시각은 목록 파일의 `createdAt`이다.
- 기기 이름: Android는 사용자가 정한 기기 이름(`Settings.Global.DEVICE_NAME`, 없으면 `Build.MODEL`), iOS는 모델 이름(`iPhone`, `iPad`).
- 사진 이름: `<sha256 소문자 hex>.<확장자>`. 기기 `media/`의 파일 이름과 같다. 같은 사진은 한 번만 있다(FR-006).
- `README.txt` 글은 문자열 리소스(`backup_readme_body`)에서 만든다. 한국어와 영어 두 단락이다.

## 스냅샷 (`.db`)

1. 열린 DB에서 `VACUUM INTO 'backup/outgoing/<이름>.db'`로 한 파일을 뜬다(번들 SQLite, WAL 문제 없음).
2. 사본을 열어 기기 것인 표(`backup_media`, `sync_base`, `merge_mark`, `draft`, `draft_media`)를 비우고 `VACUUM`한다.
3. 사본의 일기 수와 사진 줄 수를 센다. 사본의 SHA-256을 잰다. 이 값들이 목록 파일에 들어간다.
4. 스냅샷 DB의 버전은 Room 스키마 버전(R1 출시 때 1)이다.

## 목록 파일 (`.json`, 완료 표시)

맨 마지막에 올린다. 목록 파일이 있고 아래 확인을 통과한 백업만 "끝까지 된 백업"이다(FR-007).

```json
{
  "format": "org.sjhstudio.diary.backup",
  "formatVersion": 2,
  "createdAt": 1798720200000,
  "timeZone": "Asia/Seoul",
  "app": { "versionName": "2.0.0", "versionCode": 37, "platform": "android" },
  "device": { "id": "2f1c…", "name": "Galaxy S24" },
  "snapshot": { "fileName": "2026-12-31_21-30-00_Galaxy-S24_ab12.db", "byteSize": 5242880, "sha256": "…", "dbVersion": 1 },
  "counts": { "entries": 1204, "entriesInTrash": 3, "mediaRows": 862, "mediaMissing": 2, "mediaPending": 0 },
  "media": [
    { "sha256": "3f9a…c1", "ext": "jpg", "byteSize": 812345, "inBackup": true }
  ],
  "settings": {
    "font": "pretendard", "bodyTextSize": "medium", "themeMode": "system",
    "reminderEnabled": true, "reminderMinuteOfDay": 1320,
    "listStyle": "feed", "favoritesOnly": false, "askLocationAgain": true,
    "lockEnabled": true
  }
}
```

- `media`는 스냅샷의 `normal`·`pending` 사진 줄이 가리키는 파일을 한 번씩 적는다. `inBackup=false`는 공간이 부족해 올리지 못했거나(FR-018),
  기기에 파일이 없어(`pending`) 올릴 수 없었던 사진이다. `missing` 줄은 적지 않는다(FR-011).
- 일기 내용, 장소, 기분, 사진 경로, 잠금 확인값은 목록 파일에 넣지 않는다. `lockEnabled`는 켜져 있었는지만 알린다(FR-026).
- 읽는 쪽은 모르는 키를 무시한다. 없는 키는 기본값으로 본다.

### 끝까지 된 백업인지 보는 법

| 확인 | 실패하면 복원 목록에 |
| --- | --- |
| 목록 파일을 읽고 `format`이 맞음 | "목록 파일이 깨졌어요"(고를 수 없음) |
| `formatVersion`, `snapshot.dbVersion`이 앱이 아는 범위 | "앱을 업데이트하면 복원할 수 있어요"(고를 수 없음) |
| 같은 폴더에 `snapshot.fileName` 파일이 있음 | "스냅샷이 없어요"(고를 수 없음) |
| 드라이브가 알려 준 스냅샷 `sha256Checksum`이 `snapshot.sha256`과 같음 | "스냅샷이 깨졌어요"(고를 수 없음) |
| 받은 스냅샷을 다시 잰 SHA-256과 일기 수가 같음 | 복원을 멈추고 실패를 알림(FR-021) |

목록 파일이 없는 스냅샷은 목록에 보이지 않는다. 사용자가 목록 파일만 지웠으면 그 스냅샷은 끊긴 백업처럼 7일 뒤 그 기기가 정리하고, 스냅샷만 지웠으면
목록에 "스냅샷이 없어요"로 남는다.

복원 목록은 기기(목록 파일의 `device`)별로 묶고, 플랫폼(Android·iOS)과 앱 버전을 함께 보인다. 같은 계정에 두 플랫폼의 백업이 섞여도 형식이 같아 어느 쪽에서든 고를 수 있다.

## 올리는 순서와 확인

1. 드라이브 `media/` 목록을 한 번 읽어(`name`, `sha256Checksum`) `backup_media`를 맞춘다.
2. 올릴 사진을 정한다: 스냅샷이 가리키는 파일 중 드라이브에 없는 것. 공간이 부족하면(아래) 일부만.
3. 사진 → 스냅샷 → 목록 파일 순서로 올린다. 사진은 내용 확인값 이름이라 몇 번을 다시 올려도 결과가 같다.
4. 파일마다 올린 뒤 드라이브가 돌려준 `sha256Checksum`을 이름(사진)이나 잰 값(스냅샷, 목록 파일)과 견준다. 다르면 그 파일을 지우고 다시 올린다.
   `sha256Checksum`이 비어 있으면 `md5Checksum`과 기기에서 잰 MD5로 견준다.
5. 목록 파일 확인이 끝나야 "백업 완료"를 보이고 `last_success_at`, `sync_base`를 고친다(FR-008).

## 공간이 모자랄 때

- 시작 전에 드라이브 남은 공간(`about.storageQuota`)과 올릴 양(새 사진 + 스냅샷 + 목록 파일)을 견준다. 한도가 없는 계정이면 이 확인을 건너뛴다.
- 부족하면 스냅샷과 목록 파일 자리를 먼저 떼어 두고, 남은 공간에 최근 일기(`date`, `time`이 늦은 순)의 사진부터 들어가는 만큼 고른다(FR-018).
  고르지 못한 사진은 목록 파일에 `inBackup=false`로 적는다. 순서는 그대로 사진 → 스냅샷 → 목록 파일이다.
- 스냅샷조차 들어가지 않으면 시작하지 않고 부족한 양을 알린다.
- 올리는 중에 드라이브가 공간 부족(`storageQuotaExceeded`)을 돌려주면 그때까지 올린 사진만 `inBackup=true`로 적고 스냅샷과 목록 파일을 올려 본다.
  그것도 안 되면 실패(`NO_SPACE`)로 끝낸다. 이미 올린 사진은 다음 백업에 그대로 쓴다.

## 보관과 정리 (FR-010)

- 기기마다 자기 백업만 정리한다. 다른 기기의 백업은 지우지 않는다.
- 이 기기의 끝까지 된 백업 중 최근 5개와, 그보다 옛것은 달(기기 현지 시각)마다 가장 늦은 1개를 남긴다. 나머지는 목록 파일 → 스냅샷 순서로 드라이브 휴지통에 넣는다.
- 목록 파일 없이 7일 넘게 남은 이 기기의 스냅샷은 휴지통에 넣는다(끊긴 백업).
- 사진: 모든 기기의 남은 목록 파일에서 `inBackup=true`로 가리키는 해시를 모은다. 어디에서도 가리키지 않는 사진에는 `diary.unrefSince`를 적고,
  그 뒤 30일이 지나도 여전히 가리키지 않으면 휴지통에 넣는다. 다시 가리키면 `diary.unrefSince`를 지운다.
- 지우지 않고 드라이브 휴지통에 넣는다. 드라이브가 30일 뒤 비운다. 공간 부족 안내에는 드라이브 휴지통 비우기를 함께 적는다.

## 묶음 파일 (`.zip`, FR-031)

드라이브 폴더와 같은 내용을 파일 하나로 묶는다.

```text
1일1일기-백업-2026-12-31.zip
  README.txt
  manifest.json            위 목록 파일과 같은 형식. 기기에 있는 사진은 모두 inBackup=true, 기기에 없는(pending) 사진은 false
  snapshot.db
  media/<sha256>.<ext>
```

- 압축하지 않고(STORE) 담는다. 사진은 이미 JPEG로 줄어 있다. 4GB가 넘으면 ZIP64로 쓴다(research R18).
- 불러올 때는 `manifest.json`을 먼저 읽고, 스냅샷과 사진을 하나씩 꺼내며 확인값을 맞춘다. 맞지 않는 사진만 `missing`으로 두고 나머지는 합친다.
- 파일 이름의 날짜는 기기 현지 날짜다.
- 앱은 휴대폰 저장소에서 백업 파일을 찾지 않는다. 저장소 전체를 읽는 권한을 받지 않고(헌법 II), 사용자가 시스템 파일 고르기 창으로 직접 고른다.
  새 휴대폰으로 파일을 옮기는 것도 사용자가 한다(컴퓨터, USB, 다른 클라우드 앱). 화면에 그 방법을 적어 둔다(`prototype.md` B8).
