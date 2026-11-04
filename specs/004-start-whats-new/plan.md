# 구현 계획: 시작과 새 소식

**스펙**: `specs/004-start-whats-new/spec.md`

## 기술 맥락

새 기술을 고르지 않는다. 스펙 001·002·003의 축(CMP, Koin, Navigation 3, DataStore, 문자열 리소스)을 쓴다. 움직임 없이 정지 그림만 쓰므로 Compottie를 더하지 않는다.

## 구조

- `shared/feature/welcome`(새 모듈): `LetterScreen`, `OnboardingScreen`, `WhatsNewScreen`, `WelcomeViewModel`.
- `shared/core/datastore`: `WelcomeStore`(`welcome.preferences_pb`). 편지 봄, 온보딩 끝, 본 소식 버전, 본 팁. 백업 스냅샷에 들어가지 않는 기기 설정이다.
- `shared/core/data`: `WelcomeRepository`(지금 어느 단계인지 계산, 팁 조건, 세션당 하나).
- `shared/app/gate/WelcomeGate`: 이전 관문 안쪽, 앱 틀 바깥. 흐름은 `DiaryTheme → 잠금 → 이전 → 시작 → 앱 틀`이다.
- `shared/core/designsystem/DiaryTip`: 맥락 팁 말풍선 한 종류.

## 헌법 점검

| 원칙 | 지키는 자리 |
| --- | --- |
| I 기록 | 일기와 사진을 건드리지 않는다. 상태는 기기 설정뿐이다 |
| II 사용자 것 | 편지의 {n}은 기기 안 숫자이고 어디로도 보내지 않는다 |
| III 무료·광고 | 편지·온보딩·새 소식에 광고가 없다. 스펙 FR-005 |
| IV 서버 없음 | 소식은 앱에 넣는다 |
| V 심플 | 온보딩은 토큰과 기존 부품만 쓰고 그림은 강조 한 곳 |
| VI 어떤 기기 | 읽기 폭 560, 넓은 화면 가운데 |
| VII 근거 | `spec.md` 근거, `docs/PRD.md` 출처 |

## 저장 형식

DB와 백업 형식은 바뀌지 않는다. 새 DataStore 파일 하나만 더한다.
