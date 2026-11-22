# 조사: 백업 구글 계정 변경

2026-11-22. 공식 문서와 공식 도움말에서 확인한 것만 적고, 확인하지 못한 것은 그렇게 적는다.

## 재현

| 플랫폼 | 기기 | 순서 | 결과 |
| --- | --- | --- | --- |
| Android | Diary_API31 에뮬레이터(Android 12, 구글 계정 1개), develop 디버그 빌드 | 연결 → 백업 설정 [연결 끊기] → [구글 드라이브 연결하기] | 계정 선택 창 없이 처음 계정으로 연결됨 |
| Android | 같은 기기, `SELECT_ACCOUNT`를 넣은 시험 빌드 | 같은 순서 | "계정 선택" 창이 뜸. 같은 계정을 고르니 드라이브의 기존 백업("구글 드라이브에 백업이 있어요")이 그대로 보임 |
| iOS | iPhone 17 시뮬레이터(iOS 26.2), 2.1.0 개발 빌드 | 연결 안 된 상태에서 [백업 켜기] | 권한이 이미 있는 계정("이미 일부 액세스 권한이 있음")인데도 구글 "계정을 선택하세요." 화면이 먼저 뜸 |

## R1. Android에서 계정 선택 창을 띄우는 길

- **결정**: hint가 없는 연결 요청에 `AuthorizationRequest.Builder.setPrompt(AuthorizationRequest.Prompt.SELECT_ACCOUNT)`를 넣는다.
- **까닭**
  - 구글은 권한을 받은 계정을 앱의 기본 계정으로 쓰고 다음부터 계정 고르기를 건너뛴다고 적었다(["uses the account as the default and skips the account selection step"](https://developer.android.com/identity/authorization)).
  - `SELECT_ACCOUNT`는 "사용자에게 계정을 고르게 한다"는 공식 값이고 `setAccount`보다 앞선다([Prompt](https://developers.google.com/android/reference/com/google/android/gms/auth/api/identity/AuthorizationRequest.Prompt), [Builder](https://developers.google.com/android/reference/com/google/android/gms/auth/api/identity/AuthorizationRequest.Builder)).
  - 21.5.0(2026-01-07)에 들어왔고 우리 22.0.0의 `classes.jar`에 `AuthorizationRequest$Prompt.SELECT_ACCOUNT`와 `Builder.setPrompt(int)`가 있다([릴리스 노트](https://developers.google.com/android/guides/releases)).
  - 재현 표대로 시험 빌드에서 동작을 확인했다.
- **버린 안**
  - `revokeAccess`: 이 앱에 준 범위를 모두 거둔다. 다음에 다시 동의를 받아야 하고, 계정 고르기를 띄운다는 문서는 없다. R3의 위험이 있다.
  - `clearToken`: 기기에 남은 토큰 하나만 지운다. 권한과 기본 계정은 남는다.
  - Credential Manager `clearCredentialState()`: 문서는 Credential Manager로 로그인한 경우의 방법으로 적었다. 우리는 로그인 없이 권한만 받는다.
- **확인하지 못한 것**: Credential Manager 없이 권한만 받았을 때 기본 계정을 기억하는 동작은 문서에 없다. 재현으로만 확인했다.

## R2. iOS

- **결정**: 연결 흐름은 그대로 둔다. 드라이브 권한을 뺀 허용에서 `signOut()`을 부르지 않는다.
- **까닭**
  - 재현 표대로 지금도 계정 선택 화면이 뜬다.
  - GoogleSignIn-iOS 10.1.0에는 계정 고르기를 강제할 공개 값이 없다. `hint`는 "가능하면 미리 채움"뿐이다([GIDSignIn](https://developers.google.com/identity/sign-in/ios/reference/Classes/GIDSignIn)). 그래서 연결할 때 hint를 넘기지 않는 지금 방식을 지킨다.
  - `signOut()`은 키체인의 로그인만 지운다. 새 계정으로 로그인이 바뀐 뒤에 부르면 이전 계정 로그인도 남지 않는다. 부르지 않으면 앱의 연결 계정 값이 그대로 남고, 토큰을 받지 못하면 "다시 연결이 필요해요"로 안내된다.
- **확인하지 못한 것**: 웹 로그인 창이 Safari 쿠키를 함께 쓰므로, 구글이 어떤 조건에서 계정 화면을 건너뛰는지는 구글 공식 문서에 없다. 기기 시험에서 매번 확인한다(quickstart).

## R3. 권한을 거두지 않는다

- **결정**: 연결 끊기와 계정 변경 모두 구글 권한을 거두지 않는다(2026-11-06 결정 유지).
- **까닭**
  - `drive.file`은 앱이 만들었거나 연 파일에만 닿는다([범위](https://developers.google.com/workspace/drive/api/guides/api-specific-auth)).
  - 권한을 거둔 뒤 같은 프로젝트로 다시 받았을 때 예전 파일에 다시 닿는지는 Drive 문서와 Picker 문서에 없다.
  - 비공식 보고(2015년 Stack Overflow #30172915, rclone 문서)는 "다시 닿지 못한다" 쪽이다. 헌법 I에 걸리므로 시험 계정으로 확인하기 전에는 넣지 않는다.

## R4. 레퍼런스

| 앱 | 자리 | 이전 계정 백업 | 바꾼 뒤 | 확인 창 |
| --- | --- | --- | --- | --- |
| [WhatsApp](https://faq.whatsapp.com/481135090640375/?locale=en_US) | 채팅 백업 > Google Account | 도움말에 없음 | 사용자가 [Back up]을 누름 | 도움말에 없음 |
| [Daylio](https://daylio.net/faq/docs/daylio-faq/backup/switch-google-accounts-for-backups/) | 백업·복원 맨 아래 [Log Out] → 새 계정 로그인 | 도움말에 없음. 바꾸기 전 이전 계정에 마지막 백업을 권함 | 사용자가 백업을 누름 | 도움말에 없음 |
| [LINE](https://help.line.me/line/smartphone/?contentId=20023473) | 복원 때 구글 계정 고르기 | 그 계정에만 있음 | 마지막 백업 날짜·크기, 없으면 "No data found" | 도움말에 없음 |
| [Journey](https://support.journey.cloud/en/categories/synchronization-cloud/articles/migrate-from-google-drive-to-a-new-google-drive) | 계정 바꾸기 없음. 내보내기 → 로그아웃 → 새 계정 → 가져오기 | 그대로 둠 | 수동 가져오기 | 가져오기 위험 경고 |

- 모두 이전 계정의 백업을 지우지 않는다. 바꾸기 전 확인 창은 도움말에서 찾지 못했다.
- 우리는 바꾼 뒤 처음 연결과 같은 흐름을 탄다(사용자 확정). WhatsApp·Daylio처럼 사용자가 백업을 누르게 하지 않는 까닭은, 처음 연결이 이미 "찾은 백업이 있으면 묻고, 없으면 바로 첫 백업"이고(스펙 003 US1 시나리오 7) 같은 행동을 두 갈래로 나누지 않으려는 것이다.
- Daylio의 "바꾸기 전 마지막 백업 권하기"는 넣지 않는다. 일기는 기기에 있고 새 계정으로 모두 올라간다.
