# 드라이브 확인용 빌드

스펙 003 첫 작업(tasks T009~T011, research R1·O4)을 하는 작은 빌드다. 실제 앱과 같은 패키지 `org.sjhstudio.diary`, 같은 클라우드 프로젝트
`diary-app`(ID `stone-rhythm-310608`)로 시험한다. 확인이 끝나면 이 코드는 스펙 003의 드라이브 연결 코드로 옮기고 지운다.
설치는 에뮬레이터·시뮬레이터에만 한다.

## 콘솔 준비 (2026-11-01 마침)

- 프로젝트 이름을 GoogleMapProject에서 `diary-app`으로 바꿨다(ID는 그대로).
- 데이터 액세스에 `drive.file`을 더했다(`openid`, `userinfo.email`, `userinfo.profile`은 원래 있었음).
- Android 클라이언트 "1일 1일기 Debug (2026)": `org.sjhstudio.diary` + 이 맥 디버그 키 SHA-1. 원래 있던 "1일 1일기"(출시)와 "1일 1일기 Debug"(예전 맥)는 그대로.
- iOS 클라이언트 "1일 1일기 iOS": 번들 ID `org.sjhstudio.diary`. 클라이언트 ID는 `ios/Local.xcconfig`(저장소에 올리지 않음)에 둔다.

## 빌드

```bash
cd scripts/drive-probe/android && JAVA_HOME=<JDK 17> sh ./gradlew :app:assembleDebug
cd scripts/drive-probe/ios && cp Local.xcconfig.example Local.xcconfig  # 값 채우기
xcodegen generate && xcodebuild -project DriveCheck.xcodeproj -scheme DriveCheck -sdk iphonesimulator -derivedDataPath build build
```

## 확인 순서

시험용 구글 계정 하나로 한다(개인 계정 말고). 로그인은 사람이 직접 한다.

1. **예전 앱 백업 만들기**: 에뮬레이터(Medium_Phone, Play 스토어 있음)에서 Play 스토어로 1일 1일기 1.7.4를 깔고, 일기 하나를 쓰고, 설정 > 백업에서
   시험 계정으로 드라이브 백업을 한다. 그다음 1.7.4를 지운다(확인 빌드는 디버그 키라 서명이 달라 덮어 깔리지 않는다).
2. **Android 확인 빌드**: `adb -s emulator-5554 install app/build/outputs/apk/debug/app-debug.apk` → 1 연결 → 2 만들기 → 3 찾기.
   3에서 예전 앱 `note.db`가 보이면 "같은 프로젝트의 다른 Android 클라이언트(출시 키)가 만든 파일"이 보인다는 뜻이다.
3. **iOS 확인 빌드**: 시뮬레이터에서 1 연결 → 3 찾기(Android가 만든 `android-made.txt`와 `note.db`가 보이는지) → 2 만들기.
4. **다시 Android**: 3 찾기로 iOS가 만든 `ios-made.txt`가 보이는지 본다.
5. **백그라운드 토큰**: Android 4 → 홈으로 나가 20초 → 돌아와 5. iOS는 앱을 홈으로 보낸 뒤 다시 열어 4.

## 기록

| 방향 | 보임 | 날짜·메모 |
| --- | --- | --- |
| 예전 앱(출시 키) `note.db` → Android 확인 빌드(디버그 키) | | |
| 예전 앱 `note.db` → iOS | | |
| Android → iOS | | |
| iOS → Android | | |
| 작업 관리자 안 토큰(Android) | | |
| 백그라운드 토큰(iOS) | | |

결과는 `specs/003-backup-v2/research.md` R1·O4와 `docs/backup.md` 인증 절에 옮긴다.
