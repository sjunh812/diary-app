# 작업 목록: 백업 구글 계정 변경

**입력**: [spec.md](spec.md), [plan.md](plan.md), [research.md](research.md), [quickstart.md](quickstart.md)
**선행**: 스펙 003(드라이브 연결 `BackupController`, 백업 설정 B13, 잠금 `BackupGate`)

## 1단계: 화면

- [x] T001 Figma 프로토타입 "프로토타입 · 계정 변경"(백업 설정 연결됨 → 계정 선택 → 첫 백업 / 찾은 백업 / 취소 / 실패, 백업 중 누름, 끊은 뒤 다시 연결) `prototype.md`, 사용자 확정(안 B, 2026-11-22)
- [x] T002 Figma "화면" 7 백업 묶음 "백업 설정 (B13)" 줄의 [구글 계정]에 화살표(라이트·다크·태블릿), 상태 섹션에 계정 선택·백업 중 토스트, `/figma-audit`
- [x] T003 디자인 점검 `design-review.md`(프로토타입과 견준 기록 포함)

## 2단계: US1 끊은 뒤 다시 이을 때 계정 고르기 (P1)

- [x] T004 [US1] Android: hint가 없으면 `setPrompt(AuthorizationRequest.Prompt.SELECT_ACCOUNT)`(권한 빠진 허용은 이미 `NO_DRIVE_ACCESS`) `shared/core/platform/src/androidMain/kotlin/org/sjhstudio/diary/core/platform/AndroidDriveAuthorizer.kt`
- [x] T005 [P] [US1] iOS: 브리지는 그대로(고르는 순간 로그인이 바뀌어 signOut을 빼도 앞 계정이 돌아오지 않음). 실패 뒤 연결 확인은 T010 `BackupController.stillConnected`
- [x] T006 [US1] 계약 문서에 계정 고르기와 권한 빠진 허용 규칙 `specs/003-backup-v2/contracts/drive-gateway.md`, `docs/backup.md` "인증"

## 3단계: US2 연결된 채로 계정 변경 (P1)

- [x] T007 [US2] 시험 먼저: 연결된 채 다른 계정(기록 지움·자동 백업 값 지킴·찾은 백업 권하기·없으면 첫 백업), 같은 계정, 취소, 실패, 백업이 잠금을 쥔 동안 `changeAccount()`가 false이고 권한을 묻지 않음, 바꾸는 동안 백업이 Busy, 다시 연결 필요 상태의 이메일 `shared/core/backup/src/commonTest/kotlin/org/sjhstudio/diary/core/backup/BackupConnectTest.kt`
- [x] T008 [US2] `BackupRepository.changeAccount(): Boolean` `shared/core/data/src/commonMain/kotlin/org/sjhstudio/diary/core/data/BackupRepository.kt`
- [x] T009 [US2] `BackupConnection.NeedsReconnect`에 연결했던 이메일 `shared/core/model/src/commonMain/kotlin/org/sjhstudio/diary/core/model/Backup.kt`와 쓰는 곳
- [x] T010 [US2] `BackupController`: 생성자에 `gate`, `connect`(잠금을 기다림)와 `changeAccount`(바로 못 쥐면 false)가 잠금 안에서 권한 받기와 계정 적기 `shared/core/backup/src/commonMain/kotlin/org/sjhstudio/diary/core/backup/BackupController.kt`, `shared/app/src/commonMain/kotlin/org/sjhstudio/diary/app/di/AppModules.kt`, 시험의 생성자 맞춤
- [x] T011 [US2] `BackupViewModel.changeAccount()`와 바꾸지 못함 알림 `shared/feature/backup/src/commonMain/kotlin/org/sjhstudio/diary/feature/backup/BackupViewModel.kt`
- [x] T012 [US2] `BackupOptionsScreen` [구글 계정] 줄에 화살표·누름(다시 연결 필요에서도 이메일), 바꾸지 못하면 토스트, 계정을 고르면 백업 화면으로, 세 언어 문자열 `shared/feature/backup/src/commonMain/kotlin/org/sjhstudio/diary/feature/backup/BackupScreens.kt`, `shared/feature/backup/src/commonMain/composeResources/values*/strings.xml`
- [x] T013 [P] [US2] 프리뷰(한국어 먼저, 라이트·다크, 다시 연결 필요) `shared/feature/backup/src/commonMain/kotlin/org/sjhstudio/diary/feature/backup/preview/`
- [x] T014 [P] [US2] 백업 설정 화면 시험(이메일과 화살표, 누르면 바꾸기 부름) `shared/feature/backup/src/iosTest/kotlin/org/sjhstudio/diary/feature/backup/`

## 4단계: 문서와 확인

- [x] T015 `docs/backup.md`(계정 변경 절), `docs/DECISIONS.md`(2026-11-22 계정 변경·SELECT_ACCOUNT), 스펙 003 엣지 케이스에서 이 스펙 잇기
- [x] T016 CI 묶음(`spotlessCheck designTokenCheck coroutineSafetyCheck stringResourceCheck unusedImportCheck detekt :composeApp:lint allTests assembleDebug`)
- [x] T017 (Android 에뮬레이터 한국어·다크: 백업 중 토스트, 취소, 같은 계정, 오프라인 실패, 끊은 뒤 다시 연결. iOS 시뮬레이터 한국어·다크·가장 큰 글자: 연결 뒤 찾은 백업 시트, 백업 중 토스트, 취소, 같은 계정, 끊은 뒤 다시 연결. 다른 계정 실제 전환·태블릿은 못 봄. 가장 큰 글자에서 이메일 값이 두 줄로 글자 단위로 끊김(값 줄 부품의 원래 동작)) `/backup-roundtrip`, `/emulator-check`(한국어·다크·큰 글자·태블릿), iOS 시뮬레이터 `quickstart.md` 순서
- [ ] T018 문구 humanizer·맞춤법, `/self-review`, 커밋·푸시(`git push gitlab`)·MR

## 5단계: 복원 사진 계정(후속, 2026-11-22 앱 테마 리뷰에서 나옴)

- [x] T019 시험 먼저: 복원 뒤 사진이 남은 채 계정 바꾸기·끊고 다른 계정·다시 연결에서 다른 계정이면 새 계정 드라이브 요청 0번, 받을 수 없음으로 세지 않음, 앞 계정 이메일과 함께 멈춤. 앞 계정으로 돌아가면 받고 잊음. 같은 계정 요청 수 그대로. 옛 사용자, 새 드라이브 복원, 파일 복원 `shared/core/backup/src/commonTest/kotlin/org/sjhstudio/diary/core/backup/RestorePhotoAccountTest.kt`
- [x] T020 `BackupPrefs.photoAccount`·`photoAccountLabel`, `PhotoStop.OTHER_ACCOUNT`, `WaitingPhotos.account`, `RestoreController`의 복원·이어 받기·남은 사진 `shared/core/datastore`, `shared/core/model`, `shared/core/backup`
- [x] T021 백업 화면 아래 줄 문구 세 언어, 프리뷰, 화면 시험 `shared/feature/backup`
- [x] T022 Figma 화면 페이지 "백업 상태"에 "B1 · 사진이 다른 계정에 있음", `/backup-roundtrip` 자동 시험(Android·iOS 시뮬레이터), 에뮬레이터(Diary_API31) 한국어 라이트·다크·글자 1.3배에서 백업 화면 아래 줄 확인(저장 값과 받을 사진 줄을 직접 넣어 만든 상태). 실제 두 구글 계정 전환, 두 기기 왕복, 영어·일본어 기기 화면, 태블릿, iOS 화면은 못 봄
- [x] T023 커밋·푸시(`git push gitlab`)·MR
- [x] T024 드라이브 토큰을 연결 계정 것만 내주는 확인을 한 곳으로(`ConnectedAccountAuthorizer`, DI에서 드라이브 API·백업 실행·백업 연결이 함께 씀). 시험 먼저: 로그인이 다른 계정이면 복원 목록이 그 드라이브 백업을 보이지 않고 백업도 올리지 않음 `shared/core/backup/src/commonTest/kotlin/org/sjhstudio/diary/core/backup/ConnectedAccountTokenTest.kt`. MR !3이 머지되면 `BackupRunner.signedIn()`의 같은 확인을 이 한 곳으로 합침
