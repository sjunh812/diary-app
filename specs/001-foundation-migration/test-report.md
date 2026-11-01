# 업그레이드 시험 기록 (T051)

1.7.4(`94179a4`) → 새 앱(2.0.0, 37) 업그레이드를 에뮬레이터에서 돌린 기록이다. 돌리는 법은 `scripts/legacy/upgrade-test.sh`.
아래 두 번째 기록이 강제 종료, 계측 시험, 1,000편·사진 1,000장 시간 재기다. API 24·29와 quickstart §2의 나머지 견본(wal-only, restored-from-drive,
low-space, corrupt, locked, fonts)은 아직이다(남은 일 참고).

## 2026-11-01 · API 36(Android 16) 에뮬레이터 `Medium_Phone`, 시험 자료 `scripts/legacy/seed.sh`

| 항목 | 기대 | 결과 |
| --- | --- | --- |
| 시험 자료 | 일기 4편(즐겨찾기 1, 밀리초 초 자리 1, 빈 글 1), 사진 경로 5개(같은 파일 두 번, 없는 파일 1), `pref.xml`·`pref2.xml` | 1.7.4 목록에 그대로 보임(꼬미 글꼴) |
| 원본 해시 | `note.db`, `pref.xml`, `pref2.xml`, 캐시 사진 2장의 SHA-256이 전후로 같다(SC-002) | 같음 |
| 이전 | `Done`, 결과 화면 한 번 | `Done`, "4편을 모두 옮겼어요 · 사진 3장 · 찾을 수 없는 사진 1장 · 날짜를 바로잡은 일기 1편", 다시 열면 결과 없음 |
| 새 DB | 일기 4편, 사진 줄 normal 3·missing 1, 같은 사진은 파일 1개 | 같음. `media/`에 파일 2개 |
| 날짜 | `2024-10-30 23:59:457`은 10월 30일 23:59로 남는다 | 같음 |
| 남은 파일 | `files/migration`에 `state.json`, `report.json`만 | 같음(처음엔 빈 `diary-new.db.lck`가 남아 고침) |
| 걸린 시간 | - | 이전 1초 안(앱 열기·확인 포함 8초) |

덤으로 본 것: 1.7.4는 매일 알림이 켜져 있으면 Android 16에서 정확한 알람 권한이 없어 바로 꺼진다(`AlarmReceiver.setBootData`). 새 앱은 정확한 알람
권한 없이 다시 예약한다(research R15). 업그레이드한 사용자는 이 문제를 더 겪지 않는다.

## 2026-11-01 (2) · 강제 종료, 계측 시험, 큰 견본

돌린 명령: `ALARM=0 INSTRUMENT=1 [KILL_AT=<단계>] [COUNT=1000] sh scripts/legacy/upgrade-test.sh`. 견본 사진은 진짜 그림(`scripts/legacy/fixtures`)이다.
"계측 시험"은 앱 안에서 도는 `UpgradeVerificationTest`가 일기 값·사진 순서·찾을 수 없는 사진·줄임 사진을 확인한 것이다(T050).

| 기기 | 조합 | 끊긴 단계에서 다시 열기 | 원본 해시 | 새 DB | 이전 시간 | 계측 시험 |
| --- | --- | --- | --- | --- | --- | --- |
| API 36 폰(Medium_Phone) | 기본 | - | 그대로 | 일기 4, 사진 normal 3·missing 1 | 0.8초 | 통과 |
| API 36 폰 | `KILL_AT=Snapshotting` | 처음부터 다시, 결과 같음 | 그대로 | 같음 | 0.9초 | 통과 |
| API 36 폰 | `KILL_AT=Copying` | 처음부터 다시, 결과 같음 | 그대로 | 같음 | 0.7초 | 통과 |
| API 36 폰 | `KILL_AT=Verifying` | 처음부터 다시, 결과 같음 | 그대로 | 같음 | 0.5초 | 통과 |
| API 36 폰 | `KILL_AT=Committing` | 처음부터 다시, 결과 같음 | 그대로 | 같음 | 1.1초 | 통과 |
| API 36 폰 | `COUNT=1000`(1,004편, 사진 1,003장) | - | 그대로 | 일기 1,004, 사진 normal 1,003·missing 1, 줄임 사진 1,002장 실패 0 | 17.3초, 다시 21.1초 | 통과 |
| API 35 태블릿(Pixel_Tablet) | 기본 | - | 그대로 | 같음 | 0.2초 | 통과 |
| API 35 태블릿 | `KILL_AT=Copying`, `KILL_AT=Committing` | 처음부터 다시, 결과 같음 | 그대로 | 같음 | 0.2초 | 통과 |
| API 35 태블릿 | `COUNT=1000` | - | 그대로 | 일기 1,004, 사진 normal 1,003·missing 1, 줄임 사진 1,002장 실패 0 | 2.6초 | 통과 |

- 이전 시간은 마지막 시도의 시작(`state.json` startedAt)부터 끝(`report.json` finishedAt)까지다. 줄임 사진 만들기가 들어 있다(FR-037).
- API 36 폰과 API 35 태블릿의 시간 차이는 앱이 아니라 에뮬레이터 차이다. 같은 사진 1,000장의 SHA-256을 셸로만 재도 1.49초 대 0.11초였다.
- SC-003(2019년 중급 기기, Android 10에서 1,000편·사진 1,000장 2분 안)은 에뮬레이터 둘 다 크게 밑돌았다. 다만 실제 2019년 기기와 Android 10(API 29)에서
  잰 것은 아니다.
- 스펙 003 왕복 시험용 고정 자료를 `shared/core/testing/src/androidHostTest/resources/migrated/`에 남겼다(FR-034, 같은 폴더 README).

### 못 한 것

- API 24·29: 이 Mac에 시스템 이미지가 없다. 있는 API 26(default)·API 31(Google Play) 이미지로 에뮬레이터를 만들었지만
  에뮬레이터 37.1.11에서 켜지다 멈췄다("QEMU2 ... hanging thread", 그래픽을 소프트웨어로 바꿔도 같음). 만든 설정은 지웠다.
  24·29 이미지를 새로 받아 다시 돌려야 한다(이미지마다 1GB 안팎).
- quickstart §2의 wal-only, restored-from-drive, low-space, corrupt, locked, fonts 견본은 `seed.sh`에 아직 없다.
  같은 경우를 `shared/core/migration`의 공통 시험(MigrationRunnerTest 등)이 고정 파일로 본다.

