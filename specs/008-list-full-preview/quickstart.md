# 확인 안내: 목록 미리보기 줄바꿈과 일기 전체 보기

명령은 `CLAUDE.md` "빌드와 시험"을 따른다. 설치는 에뮬레이터에만 한다.

## 1. 시험

```bash
./gradlew spotlessCheck designTokenCheck coroutineSafetyCheck stringResourceCheck unusedImportCheck detekt :composeApp:lint allTests assembleDebug
```

- 줄 규칙: [contracts/preview-text.md](contracts/preview-text.md)의 표가 `commonTest`에 있다.
- 옛 설정 판단, 팁 규칙(하루 하나), 설정 저장·백업 왕복이 `commonTest`에 있다.

## 2. 화면 (에뮬레이터, 한국어 먼저)

1. 줄마다 쓴 일정 일기, 빈 줄 낀 일기, 긴 한 문단, 체크리스트, 서식 일기를 쓴다.
2. 홈 3줄 보기에서 줄이 나뉘고 빈 줄이 없는지 본다.
3. 설정 > 화면 > 목록 보기 → 전체. 미리보기 상자가 바로 바뀌고 홈 피드·카드가 줄바꿈 그대로 끝까지 보이고 "…"가 없는지 본다. 5줄도 골라 다섯 줄 뒤 "…"인지 본다. 설정 첫 화면 값이 "피드 · 전체"인지 본다.
4. 사진 격자를 고르면 길이·서식 줄이 숨는지 본다.
5. 개발자 모드에서 Plus로 바꾸고 서식 그대로 보기를 켠다. 무료로 돌리면 목록만 서식 없이 돌아가는지 본다.
6. 3줄 보기에서 잘린 일기를 상세로 3번 열고 홈에 팁이 한 번 뜨는지, 같은 날 다른 팁이 뜨지 않는지 본다(개발 빌드에서 날짜를 넘겨 다음 날 확인).
7. 다크, 글자 크기 최대, 태블릿 두 칸, 영어·일본어를 본다.

## 3. 옛 설정 잇기 (Android)

`/emulator-check`의 업그레이드 시험으로 1.7.4를 깔고 "일기 3줄 보기"를 끈 뒤 일기를 쓰고 이 빌드를 덮어 깐다.

- 첫 홈이 전체 보기이고 "예전처럼 목록에서 일기 전체를 보여드려요." 팁이 한 번 뜬다.
- 2.0.1을 거쳐 이미 이전한 기기에 덮어 깔아도 같다.
- 3줄 보기를 켠 채였던 기기, 새 설치는 바뀌지 않는다.

## 4. 백업 왕복

`/backup-roundtrip`으로 전체 보기·서식 켬 설정을 백업하고 새 기기에 복원해 값이 돌아오는지, 2.0.1 앱이 이 백업을 그대로 읽는지 본다.

## 5. 성능 (`research.md` R4)

```bash
./gradlew :benchmarks:connectedBenchmarkAndroidTest -P android.testInstrumentationRunnerArguments.class=org.sjhstudio.diary.benchmarks.ScrollBenchmark
```

- 3줄 / 전체 / 전체+서식 각각 버벅인 프레임 2.5% 이하, 상위 1% 20ms 이하(3회 중간값).
- StartupBenchmark 세 경우가 지금보다 늘지 않는다.
- 결과를 `design-review.md`에 적는다. 전체가 버벅이면 숫자를 보이고 상한을 다시 정한다.
