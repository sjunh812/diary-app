# 계약: 기분 테마 그림 원본과 생성 코드

Figma에서 내보낸 SVG가 원본이고, `scripts/mood/make_mood_art.py`가 이것을 읽어 Kotlin 코드를 만든다. 이 문서는 디자인과 생성기 사이의 약속이다. 어기는 원본은 생성기가 실패로 멈추고 무엇이 틀렸는지 적는다(조용히 건너뛰지 않는다).

## 1. 파일 자리

```text
design/mood-art/
  basic/good.svg … basic/bad.svg           테마마다 15장, 파일 이름은 Mood.id
  line/ shape/ dot/ gouache/
  bubble/ doodle/ veggie/
  clay/head.svg                            클레이는 겹(4절)
  clay/face/<mood>.svg                     표정 15
  clay/hair/<style>.svg                    머리 모양 9
  clay/glasses.svg
  textures/paper.png brush.png grain.png   이어 붙이는 회색 질감(64×64 안팎)
```

생성 결과: `shared/core/designsystem/src/commonMain/kotlin/org/sjhstudio/diary/core/designsystem/mood/art/MoodArt<테마>.kt`(테마마다 하나, 머리 주석에 "생성 파일, 손으로 고치지 않음"), 질감은 `shared/core/designsystem/src/commonMain/composeResources/drawable/mood_texture_<종류>.png`.

## 2. SVG 규칙

| 항목 | 규칙 |
| --- | --- |
| 칸 | `viewBox="0 0 44 44"`(지금 기분 칸과 같음). 그림은 이 칸 안에만. 반짝임(웃음)·zz(피곤함)도 칸 안 |
| 쓰는 요소 | `path`, `circle`, `ellipse`, `rect`, `line`, `polyline`, `polygon`, `g`. 생성기가 모두 경로로 바꾼다 |
| 쓰지 않는 요소 | `image`, `text`, `filter`, `mask`, `pattern`, 그라데이션, `transform`이 남은 요소(Figma 내보내기에서 "윤곽선으로" 펼친다) |
| 선 | `stroke-width`는 44 칸 기준 값. 생성기는 `LINE` 자리 선을 크기별 굵기 규칙(지금 `moodFaceLine`: 0.8 + 크기/55)으로 다시 계산할지 테마마다 정한다(`lineScales: true/false`, 테마 표에 적음) |
| 끝·이음 | `stroke-linecap`, `stroke-linejoin`을 그대로 옮김 |
| 질감 | 질감을 받을 면은 `data-texture="paper|brush|grain"`을 단다. 생성기는 그 면 모양을 질감을 그릴 자리로 적고, 노이즈 효과 자체는 옮기지 않는다 |

## 3. 색 자리

| SVG 값 | 생성 코드 | 런타임 색 |
| --- | --- | --- |
| 그 기분의 기분 색(`docs/design.md` 15색 표의 값) | `Slot.Mood` | `DiaryMoodColors`의 그 기분 칸 |
| `#1A1C20` | `Slot.Line` | `DiaryMoodColors.lineOn(mood)`(나쁨만 흰색, research 6절) |
| `#FFFFFF`, 투명도 있는 흰색 | `Slot.Highlight(alpha)` | 흰색(빛 반사, 비눗방울 테두리) |
| 그 밖의 값 | `Fixed(0xAARRGGBB)` | 그대로(채소 잎·꼭지·씨, 클레이 피부톤, 볼) |

- 다른 기분의 기분 색이 그림에 나오면 실패한다. 테마를 바꾸지 않아도 기분 색이 바뀌면 같이 바뀌어야 해서다.
- 클레이 머리 모양의 채움은 늘 `Slot.Mood`다(머리색 = 기분 색).
- 채소·과일 몸 색은 `Slot.Mood`, 잎·꼭지·씨는 `Fixed`다.

## 4. 클레이 겹

그리는 차례는 머리(`head.svg`, 피부톤 자리 `Slot.Skin`) → 표정(`face/<mood>.svg`) → 머리 모양(`hair/<style>.svg`) → 안경(`glasses.svg`, 켰을 때만)이다. 피부톤 6단계 값은 생성기 설정 표(`skinTones`)에 두고 `Slot.Skin`을 그 값으로 바꾼다. 머리 모양 id는 data-model "클레이 꼴"의 9개와 파일 이름이 같아야 한다.

## 5. 생성 코드 모양

```kotlin
// 생성 파일, 손으로 고치지 않음
internal object MoodArtLine : MoodArtSet {
    override val id = "line"
    override val lineScales = true
    override fun layers(mood: String): List<ArtLayer> = …  // 15개 모두
}
```

- `ArtLayer(path: String, fill: Slot?, stroke: Slot?, strokeWidth: Float, cap, join, texture: Texture?)`.
- 경로 글자 상수 하나가 64KB를 넘지 않게 나눈다(JVM 상수 한도).
- 숫자는 소수 둘째 자리까지 줄인다.

## 6. 생성기 시험

- 테마마다 15장이 모두 있고 이름이 `Mood.id`와 같다(하나라도 없으면 실패).
- 같은 원본으로 두 번 돌리면 결과 파일이 바이트까지 같다.
- 금지 요소·다른 기분 색·`viewBox` 어긋남을 넣은 작은 원본 셋으로 각각 실패하는지 본다.
- Kotlin 쪽 `commonTest`(`shared/core/designsystem/src/commonTest/.../mood/MoodArtTest.kt`): 모든 테마가 `Mood.entries` 15종에 그림을 주고, 모든 경로가 `addPathNodes`로 읽히고, 클레이 9 × 6 × 2 조합이 모두 그려진다.
- 모든 테마 얼굴은 같은 칸에서 같은 크기로 보이게 맞춘다. 기본 얼굴은 지금 크기 그대로 두고, 다른 테마 그림은 SVG 둘레 여백을 잘라 몸 크기를 기본 얼굴 원에 맞춘다. 웃음 반짝임과 피곤함 zz는 칸 밖으로 조금 나가도 된다(프로토타입 확정 때 사용자 지적: 기본만 커 보임).
