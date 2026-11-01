# 견본 사진

`seed.sh`가 1.7.4 캐시 자리에 넣는 사진이다. 저작권 걱정이 없게 그림을 직접 만들었다(색 띠 + 모서리 표시).

| 파일 | 크기 | 쓰임 |
| --- | --- | --- |
| `photo-landscape-exif6.jpg` | 2000×1200 JPEG, EXIF 방향 6 | 왼쪽 위 빨강, 오른쪽 위 파랑. 방향을 반영하면 세로 사진이 되고 빨강이 오른쪽 위로 간다. 줄임 사진(긴 변 480)이 288×480이면 맞다 |
| `photo-portrait.png` | 1200×1600 PNG | 왼쪽 위 초록. EXIF가 없는 그림. 줄임 사진은 360×480 |

다시 만들 때는 순수 파이썬 PNG 작성 → `sips -s format jpeg` → JFIF·sips Exif를 빼고 방향 태그 하나만 든 Exif를 SOI 뒤에 넣었다.
방향은 `sips -g orientation`이 보이지 않아 ImageIO(`kCGImagePropertyOrientation`)로 확인했다.
