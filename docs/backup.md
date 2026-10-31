# 백업·복원·동기화

헌법 원칙 I(기록은 잃지 않는다), II(일기는 사용자 것이다), IV(돈이 드는 서버를 두지 않는다)를 지키는 설계다. 조사일은 2026-10-31.
레거시 백업과 이전은 `docs/migration.md`에 있다.

## 지금 백업의 문제 (1.7.4)

| 문제 | 근거 |
| --- | --- |
| 사진을 올리지 않는다 | `BackupActivity.createFile`이 `note.db`만 올린다. Play 업데이트 공지에도 사진은 백업되지 않는다고 적혀 있다 |
| 최근 일기가 빠질 수 있다 | Android 9부터 따로 정하지 않은 DB는 Compatibility WAL이 기본이다([AOSP](https://source.android.com/docs/core/perf/compatibility-wal)). SQLite는 WAL 파일도 DB의 일부라 같이 옮겨야 하고 떼면 커밋된 트랜잭션을 잃을 수 있다고 한다([sqlite.org](https://www.sqlite.org/wal.html)) |
| 복원이 덮어쓰기다 | 열린 DB 파일 위에 받은 파일을 그대로 쓴다(`DriveServiceHelper.downFile`). 지금 기기 일기와 사진이 사라진다 |
| 백업·복원 버튼을 헷갈린다 | 리뷰: 버튼을 헷갈려 일기 전체를 잃었다(2026.10) |
| 자동 백업이 없다 | 기기 변경·복원 유실 리뷰 17건 |

## 어디에 두나

서버를 두지 않는다(D1). 일기와 사진은 **사용자 본인의 Google Drive**에 둔다(D2, Android·iOS 모두, 2026-11-01 사용자 결정). 우리 비용은 0원이고 저장 공간은 사용자 할당량(무료 15GB,
Gmail·Photos와 공유)을 쓴다.

| 선택지 | 판단 | 근거 |
| --- | --- | --- |
| Google Drive `drive.file` (추천) | Android와 iOS 둘 다 쓸 수 있어 안드로이드 → 아이폰 이주가 된다. 레거시 백업을 읽을 수 있다. 사용자가 드라이브에서 파일을 볼 수 있다. 비민감 스코프라 앱 검증 부담이 작다 | [Drive 스코프](https://developers.google.com/workspace/drive/api/guides/api-specific-auth), [한도](https://developers.google.com/workspace/drive/api/guides/limits) |
| Google Drive `appDataFolder` | 사용자에게 안 보인다. `drive.appdata`로는 레거시 `drive.file` 백업을 읽을 수 없다 | [appdata](https://developers.google.com/workspace/drive/api/guides/appdata) |
| iCloud(CloudKit private) | iOS 전용. 사용자 iCloud 할당량(무료 5GB)을 쓴다. iOS 보조 선택지로 나중에 검토 | [CloudKit](https://developer.apple.com/documentation/cloudkit/ckcontainer/privateclouddatabase) |
| Firebase Cloud Storage | 2026-02-03부터 Blaze(카드 등록) 필수라 제외 | [Firebase](https://firebase.google.com/docs/storage/faqs-storage-changes-announced-sept-2024) |
| Supabase, Cloudflare R2 | 무료 용량(1GB, 10GB)이 사용자 수에 비해 작고, R2는 초과분이 과금이다 | [Supabase](https://supabase.com/pricing), [R2](https://developers.cloudflare.com/r2/pricing/) |

지켜볼 것: Drive API는 지금 추가 비용이 없지만 쿼터 초과분 과금을 2026년 후반에 시작하고 90일 전에 공지한다고 적혀 있다(limits 페이지).
공지가 나오면 다시 본다.

### 인증

- Android: 레거시 `GoogleSignIn`은 deprecated다(제거 날짜는 공식 페이지에 없음). Drive 권한은 `Identity.getAuthorizationClient(activity).authorize()`로
  받는다. 로그인이 먼저 필요하지 않고, 토큰은 약 1시간이며 만료되면 다시 부른다(이미 허용했으면 화면 없이). refresh token은 기기에
  두지 않는다([authorization](https://developer.android.com/identity/authorization)).
- iOS: GoogleSignIn-iOS로 토큰을 받고 REST를 직접 부른다.
- 같은 Google Cloud 프로젝트를 쓴다. 다른 프로젝트의 클라이언트가 레거시 `drive.file` 파일을 볼 수 있는지는 공식 문서에서 확인하지 못했다. 같은 프로젝트를 쓰면 이 위험이 없다.

## 백업 형식 v2

드라이브의 보이는 폴더 `1일 1일기 백업/` 아래에 둔다.

```text
1일 1일기 백업/
  snapshots/
    20261231-213000-pixel8.db          일기 DB 스냅샷 (VACUUM INTO)
    20261231-213000-pixel8.json        매니페스트 (마지막에 올린다 = 완료 표시)
  media/
    3f9a…c1.jpg                        사진, 이름은 내용 SHA-256 + 확장자(새 사진은 jpg)
  README.txt                           이 폴더가 무엇인지, 지우면 안 된다는 안내
```

- **스냅샷**: 앱 DB를 `VACUUM INTO`로 한 파일에 떠서 올린다. 일관된 스냅샷이고 WAL 문제가 없다([sqlite.org](https://www.sqlite.org/lang_vacuum.html)).
  앱은 Room의 번들 SQLite를 쓰므로 Android 버전과 상관없이 쓸 수 있다.
- **사진**: 내용 해시를 이름으로 쓰니 같은 사진은 한 번만 올라가고, 백업마다 새 사진만 올린다.
- **매니페스트**: 형식 버전, 앱 버전, 플랫폼, 일기 수, 사진 수, 스냅샷의 SHA-256, 이 스냅샷이 가리키는 사진 해시 목록.
- **올리는 순서**: 사진 → 스냅샷 → 매니페스트. 매니페스트가 없는 스냅샷은 미완성으로 보고 복원 목록에 띄우지 않는다.
- **완료 확인**: 올린 뒤 Drive가 돌려주는 `md5Checksum`과 로컬 값을 견준다. 다 맞아야 "백업 완료"라고 말한다.
- **보관**: 스냅샷은 최근 5개와 월별 1개를 남긴다. 사진은 남은 스냅샷 어느 것도 가리키지 않은 지 30일이 지나야 지운다.
- **할당량**: 백업 전에 `about.get`으로 남은 용량을 보고 모자라면 무엇이 얼마나 모자란지 알려 준다.

## 복원

- 목록: 기기 이름, 날짜, 일기 수, 사진 수를 보여 준다. 레거시 `note.db`는 "이전 버전 백업"으로 따로 보여 준다.
- 받은 스냅샷의 SHA-256과 일기 수를 확인한 뒤에 쓴다.
- 기본은 **합치기**다. 같은 일기(UUID)는 `updatedAt`이 늦은 쪽을 남기고, 양쪽 다 고쳐졌으면 둘 다 남겨 보여 준다.
- **바꾸기**는 따로 고르는 선택지다. 고르면 먼저 지금 기기 DB를 로컬 스냅샷으로 떠 두고, 되돌리기를 7일 동안 제공한다.
- 사진은 이 기기에 없는 해시만 받는다. 중간에 끊겨도 이어 받는다.
- 백업과 복원 버튼은 다른 화면, 다른 색, 다른 말로 둔다. 복원은 무엇이 어떻게 바뀌는지 숫자로 보여 주고 한 번 더 묻는다.

## 자동 백업

- 무료다(헌법 III). 리뷰 불만 1위가 기록 유실이다(`docs/business.md`).
- Android: WorkManager 하루 1번, 네트워크 연결 시. 사진은 기본 Wi-Fi에서만(설정으로 바꿀 수 있다).
- iOS: `BGProcessingTask`는 실행이 보장되지 않으니, 앱이 백그라운드로 갈 때 마지막 백업이 24시간 넘었으면 시작한다.
- 홈이나 설정에 "마지막 백업 오늘 오후 9:30 · 사진 1,203장 모두 백업됨"처럼 상태를 늘 보여 준다. 7일 넘게 실패하면 알림으로 알린다.

## 내보내기

드라이브를 쓰지 않는 사용자를 위해 기기 파일로도 내보낸다. 무료다.

- 묶음 파일(`.zip`: 스냅샷 DB + 사진 + 매니페스트)을 Android 파일 저장(SAF), iOS 공유 시트로 저장하고 다시 불러온다.
- 읽기용 내보내기(텍스트, PDF)는 별도다. PDF는 Plus 후보다(`docs/business.md`).

## 여러 기기 동기화 (나중, 별도 스펙)

폰과 태블릿, 안드로이드와 아이폰을 같이 쓰는 사용자를 위한 기능이다. Plus 후보다. 서버 없이 같은 드라이브 폴더를 쓴다.

- 기기마다 바뀐 일기를 `changes/<기기>/<순번>.jsonl`로 올리고 다른 기기 것을 받아 반영한다.
- 일기는 UUID, `updatedAt`, `deletedAt`(지운 표시)을 가진다. 같은 일기를 양쪽에서 고치면 둘 다 남긴다.
- 출시 범위에 넣지 않는다. 데이터 모델(UUID, `updatedAt`, `deletedAt`, 사진 해시)은 처음부터 이 기능을 막지 않게 만든다.

## 서버

일기와 사진을 다루는 서버는 두지 않는다. 공지, 강제 업데이트, 기능 스위치는 Firebase Remote Config로 한다(Spark 무료, 서버 코드 없음,
D1 2026-11-01 사용자 결정).
새 버전 소개 내용은 앱에 넣는다. Firebase를 쓰지 않기로 하면 같은 일을 Cloudflare Workers 무료 플랜(한도 넘으면 차단형)으로 한다
([Workers 한도](https://developers.cloudflare.com/workers/platform/limits/)).
