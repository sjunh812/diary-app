# 구현 계획: 백업 구글 계정 변경

**브랜치**: `feat/drive-switch-account` | **작성**: 2026-11-22 | **명세**: [spec.md](spec.md)

## 요약

Android 드라이브 권한 요청에 "계정을 고르게 하라"는 값을 넣어 끊었다 다시 이을 때도 계정 선택 창이 뜨게 한다. 백업 설정의 [구글 계정] 줄을 누를 수 있게 하고(프로토타입 안 B), 누르면 지금의 연결 흐름을 그대로 탄다. 권한 받기와 계정 적기는 백업·복원이 함께 쓰는 잠금(`BackupGate`) 안에서 해, 한 번의 백업이 두 계정에 걸치지 않게 한다. 백업·복원이 잠금을 쥐고 있으면 바꾸지 않고 토스트로 까닭을 알린다. 저장 형식과 백업 형식은 바꾸지 않는다.

## 기술 배경

- 새 라이브러리 없음. Android `play-services-auth` 22.0.0의 `AuthorizationRequest.Builder.setPrompt(Prompt.SELECT_ACCOUNT)`(21.5.0부터), iOS GoogleSignIn-iOS 10.1.0 그대로(research R1·R2).
- 시험: `commonTest`(`BackupConnectTest`에 계정 변경·잠금 경우, `FakeDriveAuthorizer`), `iosTest`(백업 설정 화면 시험), 플랫폼 권한 코드는 에뮬레이터·시뮬레이터에서 손으로 확인(`/backup-roundtrip`).

## 헌법 점검

| 원칙 | 이 계획에서 지키는 자리 |
| --- | --- |
| I 기록은 잃지 않는다 | 구글 권한을 거두지 않는다(R3). 드라이브 파일을 지우지 않는다. 권한 받기·계정 적기를 잠금 안에서 해 백업·복원과 겹치지 않는다. 취소·실패면 이전 연결 그대로 |
| II 일기는 사용자 것 | 계정 이메일·고유값을 로그와 통계에 넣지 않는다. 새로 보내는 곳 없음 |
| III 무료 기능·광고 규칙 | 백업 설정은 광고 없는 화면 그대로. 계정 변경은 무료 |
| IV 돈 드는 서버 없음 | 서버 없음. 사용자 드라이브 그대로 |
| V 심플 | 새 줄 없이 있던 [구글 계정] 줄에 화살표. 확인 창 없음 |
| VI 어떤 기기에서도 | 두 플랫폼 같은 동작(계정 선택 창). 폰·태블릿 같은 줄 |
| VII 근거 | `research.md`에 공식 문서와 레퍼런스 앱 도움말, 재현 기록 |

## 구조

```text
shared/core/platform/src/androidMain/.../AndroidDriveAuthorizer.kt   hint 없을 때 SELECT_ACCOUNT(권한 빠진 허용은 이미 실패로 봄)
shared/core/data/src/commonMain/.../BackupRepository.kt              changeAccount(): Boolean(false = 백업·복원 중)
shared/core/backup/src/commonMain/.../BackupController.kt            connect·changeAccount가 잠금 안에서 권한 받기·계정 적기
shared/core/model/src/commonMain/.../Backup.kt                       NeedsReconnect에 연결했던 이메일(화면 값, 저장 안 함)
shared/feature/backup/src/commonMain/.../BackupScreens.kt            BackupOptionsScreen [구글 계정] 줄에 화살표, 누름, 토스트
shared/feature/backup/src/commonMain/.../BackupViewModel.kt          changeAccount(), 바꾸지 못함 알림
shared/feature/backup/src/commonMain/composeResources/values*/strings.xml  문구 3개 언어
shared/feature/backup/src/commonMain/.../preview/                    줄 상태별 프리뷰
shared/app/src/commonMain/.../di/AppModules.kt                       BackupController에 BackupGate 넘김
```

## 데이터 모델

저장하는 값은 바뀌지 않는다(FR-012). `BackupPrefs`의 `accountKey`·`accountLabel`과 `withoutBackupRecord()`가 지우는 칸은 그대로다.

화면 값만 바뀐다.

| 값 | 뜻 |
| --- | --- |
| `BackupConnection.NeedsReconnect(accountLabel)` | 다시 연결이 필요한 상태에서도 [구글 계정] 줄에 연결했던 이메일을 보인다. 값은 지금도 저장된 `accountLabel` |

백업·복원 중인지는 따로 상태로 두지 않는다. 누르는 순간 잠금을 얻는지(`gate.tryEnter()`)로 정한다. 화면과 실제 동작이 어긋날 틈이 없다.

이어 올리기 주소는 한 번의 백업 안에서만 메모리에 있다(`BackupRunner.uploadLarge`). 잠금 안에서 계정이 바뀌지 않으므로 따로 지울 저장 값이 없다(spec Edge Cases "이어 올리기").

## 흐름

1. **연결하기·다시 연결**(연결 안 됨, 다시 연결 필요): `BackupViewModel.connect()` → `BackupController.connect(null)` → 잠금을 기다려 쥔 뒤(`gate.enter()`, 다시 연결이 필요한 상태의 백업은 곧 권한 오류로 끝난다) `authorizer.connect(null)`. Android는 hint가 없으면 `SELECT_ACCOUNT`로 계정 선택 창을 띄운다. iOS는 지금처럼 구글 계정 선택 화면이 뜬다.
2. **홈 카드 [백업 켜기]**: hint(예전 앱 계정)가 있으면 지금처럼 `setAccount`만 쓰고 고르기를 강제하지 않는다.
3. **계정 변경**: `BackupViewModel.changeAccount()` → `BackupController.changeAccount()`.
   - `gate.tryEnter()`가 실패하면(백업·복원 중) `false`를 돌려주고, 화면은 "백업이나 복원이 끝나면 바꿀 수 있어요." 토스트를 띄운다.
   - 잠금을 쥔 채 `authorizer.connect(null)` → 결과가 `Connected`면 `store.update`로 새 계정과 백업 기록 정리(`withoutBackupRecord`, 같은 계정이면 그대로) → 잠금을 놓는다.
   - 잠금을 놓은 뒤 지금의 `start()` 뒷부분(자동 백업 예약, 백업 찾기, 합치기 권하기 또는 첫 백업)을 그대로 탄다. 백업 찾기와 첫 백업은 각자 잠금을 다시 쥔다.
   - 취소·실패면 store를 건드리지 않는다. Android는 성공했을 때만 기기에 둔 이메일을 바꾸므로 이전 계정 토큰을 계속 받는다.
4. **권한 빠진 허용**
   - Android: 지금처럼 허용 범위에 `drive.file`이 없으면 `Failed(NO_DRIVE_ACCESS)`, 이메일을 바꾸지 않는다.
   - iOS: GoogleSignIn은 고르는 순간 로그인을 새 계정으로 바꿔, `signOut()`을 빼도 앞 계정 로그인은 돌아오지 않는다. 브리지는 그대로 두고, 공통 코드가 실패·취소 뒤 `accessToken()`을 한 번 불러 `NeedsUser`면 `AUTH`를 적는다("다시 연결이 필요해요"). 드라이브 파일은 건드리지 않는다.
5. **7일 알림·자동 백업**: 다른 계정이면 `withoutBackupRecord`가 실패 기록을 지우고, 첫 백업 성공 때 지금처럼 알림을 다시 건다. 자동 백업 예약은 값이 켜져 있으면 그대로 둔다.

## 열린 결정

없음(Clarifications 2026-11-22에서 닫음).
