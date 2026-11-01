# Quickstart: 기반과 이전 확인하기

스펙 001이 끝났는지 손과 명령으로 확인하는 순서다. 명령과 스크립트는 001 개발이 만들고, 만든 뒤 `CLAUDE.md` "빌드와 시험"에 옮긴다.
설치는 에뮬레이터에만 한다(`ANDROID_SERIAL` 지정).

## 준비

- JDK 17(이 기기는 `~/Library/Java/JavaVirtualMachines/jbr-17.0.14`). 기본 JDK 25로는 레거시 Gradle 8.9가 돌지 않는다.
- Android SDK 36, 에뮬레이터 이미지 API 24·29·35·36(google_apis), Xcode와 iOS 17 이상 시뮬레이터.
- 첫 빌드가 `installGitHooks`로 `core.hooksPath`를 `.githooks`에 맞춘다.

## 1. 빠른 검사

```bash
./gradlew spotlessCheck designTokenCheck allTests
```

기대: 모두 통과. `allTests`는 Android 호스트 시험과 iOS 시뮬레이터 시험을 함께 돈다. 이전 고정 파일 시험(`docs/migration.md` "시험"의 모든 경우)은
`shared/core/migration`에 있고 원본 고정 파일의 SHA-256이 시험 전후로 같은지도 본다(SC-001, SC-002).

```bash
git commit --allow-empty -m "아무 말"
```

기대: commit-msg 훅이 막는다(FR-031). `git commit --allow-empty -m "chore: 훅 확인"`은 통과한다.

## 2. 1.7.x → 새 앱 업그레이드 (FR-032, SC-001~SC-006)

```bash
scripts/legacy/build-legacy-apk.sh 1.7.4
```

```bash
ANDROID_SERIAL=emulator-5554 ALARM=0 INSTRUMENT=1 sh scripts/legacy/upgrade-test.sh            # 기본 견본(일기 4편, 찾을 수 없는 사진 1장)
ANDROID_SERIAL=emulator-5554 ALARM=0 INSTRUMENT=1 COUNT=1000 sh scripts/legacy/upgrade-test.sh # 사진 든 일기 1,000편을 더한 큰 견본
```

2026-11-01: 처음 적은 `--api`, `--fixture` 인자 대신 환경 변수를 쓴다. 기기는 `ANDROID_SERIAL`로 고른다. `INSTRUMENT=1`이면 먼저
`./gradlew :composeApp:assembleDebug :composeApp:assembleDebugAndroidTest`로 두 APK를 만든다. 아래 표에서 `seed.sh`에 있는 견본은
기본(`basic`, `photos-missing` 포함)과 `COUNT`(`big`의 1,000편판)이다. 나머지는 공통 시험이 고정 파일로 본다(test-report.md).

`upgrade-test.sh`가 하는 일: 에뮬레이터를 깨끗이 → 옛 APK 설치 → `run-as`로 고정 자료(DB 세 파일, 설정 XML, 캐시 사진) 넣기 → 새 APK를 `adb install -r`로
덮어 설치 → 계측 시험 `UpgradeVerificationTest`가 이전 끝을 기다려 새 DB를 고정 자료의 기대값과 견주고 원본 파일 해시가 그대로인지 본다.

돌릴 조합(각 API 24, 29, 35, 36):

| 고정 자료 | 확인할 것 |
| --- | --- |
| `basic`(수백 편, 사진·기분·날씨·장소·즐겨찾기·고른 날짜) | 모든 일기 값과 사진 순서가 같다 |
| `wal-only`(마지막 일기가 WAL에만) | 그 일기도 옮겨진다 |
| `photos-missing`(캐시를 지운 기기) | "찾을 수 없는 사진" 자리와 개수 |
| `restored-from-drive`(다른 기기 경로를 가리키는 사진) | 모두 `missing`, 글은 그대로 |
| `big`(1만 편, 사진 5천 장) | 끝까지 되고 진행 숫자가 멈추지 않는다 |
| `low-space`(남은 공간 부족) | 시작하지 않고 필요한 양을 알린다, 원본 그대로 |
| `corrupt`(깨진 페이지) | `Partial`과 못 읽은 수, 원본 그대로 |
| `locked`(잠금·지문 켬, 알림 22:00) | 잠금 화면이 먼저, 옛 비밀번호로 열림, 다음 22:00 알림 |
| `fonts`(font_key 5, font_size 3, mode_key 2) | 꼬미, `large`, 다크, 글꼴 바뀜 안내 한 번 |

## 3. 끊겼다 다시 (FR-007)

개발 빌드는 `files/debug/kill_at` 파일에 단계 이름이 있으면 그 단계에서 프로세스를 끝낸다.

```bash
ANDROID_SERIAL=emulator-5554 ALARM=0 INSTRUMENT=1 KILL_AT=Copying sh scripts/legacy/upgrade-test.sh
```

기대: 다시 열면 처음부터 하고 결과가 끊기지 않은 이전과 같다. `Snapshotting`, `Verifying`, `Committing`도 같은 결과.

## 4. 잠금 (US2, SC-005)

1. `locked` 고정 자료로 업그레이드한 뒤 앱을 연다: 잠금 화면 → 지문(에뮬레이터 `adb -e emu finger touch 1`) → 열림.
2. 지문 취소 → 숫자판, 틀린 비밀번호 5번 → 30초 기다림 표시, 앱을 껐다 켜도 이어짐.
3. 홈으로 나갔다가 61초 뒤 돌아오기 → 잠금. 최근 앱 화면을 찍는다(`adb shell input keyevent KEYCODE_APP_SWITCH`, `adb exec-out screencap -p`)
   → 일기 내용이 없다. API 24·29·31·35에서 본다(research O3).
4. "비밀번호를 잊었어요" → 폰 PIN 확인 → 새 비밀번호. 폰 화면 잠금을 끈 에뮬레이터에서는 백업 안내.
5. iOS 시뮬레이터: 잠금 켜고 앱 전환기 → 가림 창.

## 5. 시작과 성능 (US4, SC-007, SC-010, SC-011)

```bash
./gradlew :benchmarks:connectedBenchmarkAndroidTest
```

기대: `StartupBenchmark` 콜드 스타트 중앙값이 기준 에뮬레이터에서 기록되고 중급 실기기에서 1.5초 안, 스플래시 한 번. `ScrollBenchmark`(1만 편·사진 5천 장)
늦은 프레임 1% 미만. 10분 넘기기 시험에서 메모리 부족 종료 0건. 홈 화면 아이콘을 원형·둥근 사각·테마 아이콘에서 보고, 업데이트 전 홈 화면에 둔
바로가기가 그대로 열리는지 본다(옛 런처 이름).

## 6. 앱 틀 (US5)

개발 빌드 설정 → 디자인 견본. 폭 400·700·1000dp(폰, 폴더블, 태블릿 에뮬레이터)와 라이트·다크, `adb shell settings put system font_scale 2.0`에서
잘림·겹침이 없고 색이 `docs/design.md` 토큰과 같다. 600dp에서 하단 탭 ↔ 레일이 바뀐다.

## 7. 사용 통계 점검 (SC-009)

```bash
adb shell setprop debug.firebase.analytics.app org.sjhstudio.diary
```

Firebase DebugView에서 이전·잠금·설정을 한 바퀴 돈 이벤트가 `contracts/app-shell.md` 고정 표에 있는 것뿐이고 값에 글·장소·경로·기분이 없다.
