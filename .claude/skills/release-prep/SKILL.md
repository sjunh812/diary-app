---
name: "release-prep"
description: "1일 1일기 새 버전을 낼 준비를 한다. 버전 올리기, Baseline Profile, 글꼴 모듈 시험, 라이선스 목록, 업그레이드 시험, 스토어 확인 목록, 릴리스 커밋과 태그를 다룬다. 릴리스 준비, 버전 올리기, 태그, Baseline Profile 다시 만들기를 할 때 쓴다."
argument-hint: "버전(예: 2.0.0 (37))"
user-invocable: true
disable-model-invocation: false
---

# 릴리스 준비

규칙의 원본은 `docs/release.md`와 `docs/migration.md` 출시 순서다. 커밋, 푸시, 태그는 사용자가 요청한 때만 한다.

## 1. 버전

`versionCode`는 37부터다. 36은 만들고 내지 않은 1.7.5가 썼다.

1. `composeApp/build.gradle.kts`의 `versionCode`, `versionName`
2. `iosApp/project.yml`의 `MARKETING_VERSION`, `CURRENT_PROJECT_VERSION`. 고친 뒤 `cd iosApp && xcodegen generate`로 다시 만든다.
3. 두 곳이 맞는지 본다.

   ```bash
   sh scripts/release/check-version.sh v2.0.0
   ```

## 2. 함께 다시 만드는 것

- 의존성이 바뀌었으면 오픈소스 목록: `./gradlew :composeApp:exportLibraryDefinitions`
- Baseline Profile: 아래 절
- 화면이 바뀌었으면 스토어 스크린샷: `docs/release.md` "스크린샷과 그래픽"

## 3. Baseline Profile

```bash
./gradlew :composeApp:assembleNonMinifiedRelease :benchmarks:assembleNonMinifiedRelease
```

홈 목록 넘기기까지 담으려면 일기가 쌓인 Android 14 이상 에뮬레이터에서 만든다. Android 13 이하는 측정 도구가 앱을 다시 깔아 일기가 지워진다.
사진이 있으면 이전에 여유 공간이 넉넉해야 해서(복사 뒤 1GB) 태블릿 에뮬레이터(Pixel_Tablet, API 35)를 쓴다.

1. 견본 1만 편을 이전한다: `ANDROID_SERIAL=<에뮬레이터> ALARM=0 COUNT=10000 PHOTO_EVERY=2 WAIT=1800 sh scripts/legacy/upgrade-test.sh`
2. 두 APK를 `adb install -r`로 덮어 깐다(일기는 남는다). 앱을 열어 편지와 온보딩을 넘겨 홈까지 간다.
3. 다음으로 만든다.

```bash
am instrument -w -e class org.sjhstudio.diary.benchmarks.BaselineProfileGenerator -e androidx.benchmark.enabledRules BaselineProfile org.sjhstudio.diary.benchmarks/androidx.test.runner.AndroidJUnitRunner
```

결과(`/sdcard/Android/media/org.sjhstudio.diary.benchmarks/*-prof.txt`)를 `composeApp/src/main/generated/baselineProfiles/`에 넣는다.
폰과 태블릿은 지나는 코드가 달라서, 한 기기에서만 만들었으면 지금 파일에 새 규칙만 덧붙인다(`awk '!seen[$0]++' 지금 새것 > 합친것`).
`generateBaselineProfile` 작업은 붙은 기기를 모두 쓰므로 실기기가 연결되어 있을 때는 쓰지 않는다.
홈 스크롤은 `ScrollBenchmark`를 `benchmarkRelease` APK로 돌린다. Android 14 이상에서는 Baseline Profile만 쓴 상태로 잰다.
시작 시간은 같은 방법으로 `StartupBenchmark`를 `benchmarkRelease` APK로 돌린다(에뮬레이터는 `-e androidx.benchmark.suppressErrors EMULATOR`).

## 4. 고를 때 받는 글꼴

개발 APK(`installDebug`)에는 글꼴이 담겨 있어 바로 받아진다. 출시 경로(`fonts/<id>`, Play 필요할 때 설치 모듈)는 번들로 시험한다.

1. `./gradlew :composeApp:bundleDebug`로 번들을 만든다.
2. bundletool `build-apks --local-testing --connected-device --device-id=emulator-5554`와 `install-apks`로 깐다.
3. bundletool은 AGP와 같은 1.18.3을 쓴다(`--aapt2`에 SDK build-tools의 aapt2).

## 5. 시험

1. CI와 같은 검사를 돌린다. 명령은 `CLAUDE.md` "빌드와 시험"에 있다.
2. 업그레이드 시험을 돌린다(`/emulator-check`). `docs/release.md` "출시 전 확인"의 마지막 줄이다.
3. 백업과 복원을 왕복한다(`/backup-roundtrip`).
4. 출시 빌드를 깔아 난독화로 깨진 곳이 없는지 본다. 개발 빌드만 보고 끝내지 않는다.

## 6. 스토어

`docs/release.md` "출시 전 확인"을 위에서부터 본다. 체크되지 않은 줄을 사용자에게 보여 주고 이번 판에 필요한 것을 묻는다.
Play "새로운 기능" 문구는 한국어, 영어, 일본어로 쓰고 `korean-humanizer`와 `korean-spell-check`로 다듬는다.

## 7. 커밋과 태그

```bash
git checkout main
git merge --no-ff develop
git commit -m "chore(release): 2.0.0 (37)"
git tag v2.0.0
```

릴리스 커밋은 `main`에 만들고, 끝나면 `develop`에도 머지한다. 태그를 푸시하면 `.github/workflows/release.yml`이 돈다. 비밀값은 `docs/release.md` "CD 비밀값"에 있다.

- 변경 기록은 `sh scripts/release/changelog.sh v2.0.0`으로 미리 볼 수 있다. 사람이 읽을 문구는 초안에서 다듬는다.
- Play 업로드는 내부 트랙으로만 간다. 단계적 출시와 이전 실패 시 멈춤은 `docs/migration.md` 출시 순서를 따른다.
- 태그를 지우고 다시 쓰지 않는다. 고칠 것이 있으면 다음 빌드 번호로 낸다.
