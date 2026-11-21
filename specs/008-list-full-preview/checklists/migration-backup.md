# 점검표: 이전·백업·복원

**목적**: 설정 값과 백업 설정 칸을 더하는 이 스펙이 옛 사용자와 백업을 깨지 않는지 요구사항 수준에서 본다
**작성**: 2026-11-17
**대상**: [spec.md](../spec.md), [data-model.md](../data-model.md)

## 옛 설정 잇기

- [x] CHK001 옛 `pref.xml`을 읽기만 하고 고치지 않는다고 적었는가? [data-model "옛 설정 판단", 헌법 I]
- [x] CHK002 파일이 없거나 깨졌을 때 동작이 정해졌는가? [spec US3-5]
- [x] CHK003 이미 2.0.x로 이전을 마친 기기도 한 번 판단한다고 적었는가? [FR-008, US3-3]
- [x] CHK004 한 번만 판단하고, 사용자가 직접 고른 뒤에는 다시 판단하지 않는가? [FR-008, data-model `legacy_preview_checked`·`previewChosen`]
- [x] CHK005 이전이 진행 중일 때의 순서가 정해졌는가? [spec Edge Cases "이전 중"]
- [x] CHK006 iOS에서 판단하지 않는다고 적었는가? [data-model]

## 백업·복원

- [x] CHK007 백업 형식 버전을 올리지 않는 까닭(있어도 그만인 칸, 옛 앱이 무시)을 적었는가? [plan "다시 본 헌법 점검"]
- [x] CHK008 옛 백업(칸 없음)을 새 앱이 복원할 때 기기 값을 그대로 둔다고 적었는가? [data-model ListPreferences]
- [x] CHK009 새 백업을 2.0.1 앱이 읽어도 깨지지 않는지 확인 단계가 있는가? [quickstart 4]
- [x] CHK010 기기 사정 값(`previewChosen`, 팁 기록, 옛 설정 판단)을 백업에 넣지 않는다고 적었는가? [data-model]
- [x] CHK011 일기 DB 스키마와 일기 글을 바꾸지 않는다고 적었는가? [plan 요약]

## 결제 경계

- [x] CHK012 Plus가 끝났을 때 저장 값은 남기고 그리기만 멈춘다고 적었는가? [FR-014, data-model]
- [x] CHK013 복원한 기기에 Plus가 없으면 서식 값이 와도 그리지 않는가? [data-model "실제로 그리는지는 showFormatting && Plus"]

## 메모

- 걸린 항목 없음. 2026-11-17 사용자 결정(백업 칸 넣기)을 반영했다.
