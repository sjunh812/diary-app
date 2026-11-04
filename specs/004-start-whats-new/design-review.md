# 스펙 004 디자인 점검

화면을 개발로 넘기기 전에 Figma "시작 (W1~W5)"를 레퍼런스에 대어 본 기록이다.

| 본 것 | 근거 | 판단 |
| --- | --- | --- |
| 온보딩 장 수 | HIG Onboarding, Material "장점 최대 3개" | 업데이트 2장, 새 사용자 3장 |
| 건너뛰기 | HIG: 건너뛸 수 있게 | 위 오른쪽 글자 버튼, 언제나 있다 |
| 권한 | Android 알림 권한 문서 | 온보딩에서 묻지 않는다 |
| 편지 본문 | `docs/design.md` 읽기 글자 17/28, 읽기 폭 560 | `reading` 스타일, 왼쪽 정렬 |
| 판매 장치 | HIG: 구매는 써 본 뒤에 | 가격, 할인, 닫기 숨기기 없음 |
| 맥락 팁 | Apple TipKit의 줄 안 팁 | 화면을 막는 말풍선 대신 토스트 색 한 줄, 닫기 있음 |
| 강조 | 헌법 V | 주 버튼 하나, 점 하나 |

토스·당근에 온보딩 수치를 적은 공식 자료는 찾지 못했다. 버튼과 줄은 기존 부품이라 TDS·SEED 값에 맞다.
온보딩 그림은 결과 배지로 시작했다. 실제 화면을 줄인 그림이 나을지는 사용자에게 시안으로 묻는다.

## 다시 점검 (2026-11-05, 사용자 지적)

| 지적 | 근거 | 바꾼 것 |
| --- | --- | --- |
| 온보딩이 편지와 겹치고 글자만 있다 | [NN/g 시험](https://www.nngroup.com/articles/mobile-tutorials/) 70명, 튜토리얼을 읽어도 과제 성공률 차이 없음. [HIG](https://developer.apple.com/design/human-interface-guidelines/onboarding), [Android](https://developer.android.com/design/ui/mobile/guides/patterns/onboarding)도 짧게, 건너뛸 수 있게 | 두 장으로 줄이고 장마다 그림 하나. 달라진 점은 목록 한 장 |
| 아이콘이 다 같다 | 그림이 내용을 말해야 한다 | 쓰는 그림, 백업 그림, 줄마다 다른 아이콘 |
| 움직이는 설명을 넣으면 좋겠다 | 삼성 설명 화면의 구체 규격은 공식 문서에서 확인하지 못했다. [WCAG 2.2.2](https://www.w3.org/WAI/WCAG22/Understanding/pause-stop-hide.html)는 5초를 넘기는 자동 움직임에 멈출 수단을 요구한다 | 한 번 재생하고 끝 모습에서 멈춤, 동작 줄이기면 끝 모습만 |
| 편지가 글뿐이고 문단 간격이 크다 | [Arc 편지](https://browsercompany.substack.com/p/letter-to-arc-members-2025), [Day One 글](https://dayoneapp.com/blog/day-one-at-automattic)은 제목과 서명 묶음을 둔다. [WCAG 1.4.8](https://www.w3.org/WAI/WCAG22/Understanding/visual-presentation.html) 문단 간격은 줄 간격의 1.5배 이상 | 제목, 강조 두세 곳, 약속 상자, 서명 묶음, 문단 사이 16 |
| "개발자예요"가 어색하다 | [토스 기술 블로그](https://toss.tech/article/52501)처럼 "개발자 OOO입니다" 뒤에 해요체를 잇는 사례가 있다 | "1일 1일기 개발자입니다" |
| 새 소식 제목이 치우쳤다 | 앱은 가운데다. Figma 프레임만 손으로 왼쪽에 찍혀 있었다 | Figma 고침. 같은 곳으로 가던 줄 둘도 하나로 합침 |

손글씨 서명, 종이 질감, 큰 사진은 넣지 않았다. 사례를 확인하지 못했고 토큰 원칙에도 맞지 않는다.
