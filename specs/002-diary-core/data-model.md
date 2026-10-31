# Data Model: 일기 핵심 화면

`Entry`와 `Media`의 뼈대와 레거시 이전은 스펙 001 소유다. 여기 적은 필드 중 "(002)"는 이 스펙이 더하거나 규칙을 정하는 것이다.
레거시 값과의 대응은 `docs/migration.md`, 백업에 실리는 범위는 `docs/backup.md`를 따른다.

## Entry (일기)

| 필드 | 타입 | 규칙 |
| --- | --- | --- |
| `id` | String(UUID) | 기본 키. 기기와 백업 사이에서 유일 |
| `date` | LocalDate | 사용자가 고른 일기 날짜. 시간대가 바뀌어도 그대로 |
| `time` | LocalTime | 시와 분. 목록·상세 표시용 |
| `bodyHtml` | String | 제한된 HTML(`contracts/rich-text-html.md`). 빈 글은 `""` |
| `plainText` | String | `bodyHtml`에서 태그를 뺀 글. 검색·미리보기·공유용. 저장 때마다 다시 만든다 |
| `mood` | Mood? | 9종 또는 null |
| `weather` | Weather? | 7종 또는 null |
| `placeName` | String? | 앞뒤 공백을 뺀 30자 안팎. 빈 문자열은 null |
| `isFavorite` | Boolean | |
| `createdAt` | Instant | |
| `updatedAt` | Instant | 내용이 바뀔 때마다 |
| `deletedAt` | Instant? (002) | null이면 보이는 일기, 값이 있으면 휴지통 |
| `legacyId`, `legacyCreateDate` | Int?, String? | 001 이전용 |

색인(002): `(deletedAt, date DESC, time DESC)` 목록·달력·통계용, `(deletedAt, isFavorite)`.

저장 조건: `plainText`가 비어 있지 않거나, 사진이 1장 이상이거나, `mood`가 있다(FR-002).

상태:

```text
[보임] --지우기--> [휴지통] --30일 지남 또는 완전히 지우기--> (영구 삭제)
   ^                  |
   +----되돌리기/되살리기-+
```

## Media (사진)

| 필드 | 타입 | 규칙 |
| --- | --- | --- |
| `id` | String(UUID) | |
| `entryId` | String | `Entry.id`. 일기가 영구 삭제되면 함께 지운다 |
| `fileName` | String | `<sha256>.<ext>`. 새로 붙인 사진은 늘 `jpg`, 이전된 사진은 원래 확장자. 앱 저장소 `files/media/` 아래. 절대경로를 담지 않는다. 백업 `media/`와 같은 이름 |
| `sha256` | String | 백업 사진 이름과 같다 |
| `width`, `height` | Int | 축소 뒤 크기(긴 변 2,560 이하, 레거시 이전분은 원래 크기) |
| `sortOrder` | Int | 0부터 |
| `status` | NORMAL / MISSING | MISSING은 001 이전에서 원본을 못 찾은 사진 |
| `legacyPath` | String? | MISSING일 때 원래 경로 |

한도(002): 새로 더할 때 `count(entryId) < limit`(무료 10, Plus 30). 이미 한도를 넘은 일기는 그대로 두고 더하기만 막는다(헌법 III 예외).

## Draft (임시 저장본, 002)

| 필드 | 타입 | 규칙 |
| --- | --- | --- |
| `key` | String | 새 일기는 `"new"`, 고치는 일기는 `Entry.id` |
| `date`, `time`, `bodyHtml`, `mood`, `weather`, `placeName`, `isFavorite` | Entry와 같음 | |
| `weatherAutoFilled` | Boolean | 자동으로 채운 날씨인지. 날짜를 바꾸면 이것만 지운다(FR-005) |
| `updatedAt` | Instant | |

## DraftMedia (002)

`Draft.key`, `fileName`, `sha256`, `width`, `height`, `sortOrder`. 저장하면 `Media`로 옮기고 지운다.

## 사진 파일 정리 (002)

`files/media`의 파일 중 `Media`(휴지통 일기 포함), `DraftMedia` 어디에서도 쓰지 않고, 파일 수정 시각이 하루 넘게 지난 것만 지운다. 앱 시작 때 휴지통 비움 뒤에 돈다.

## Mood, Weather

| Mood | 레거시 | 이름 |
| --- | --- | --- |
| `ANGRY` | 0 | 화남 |
| `COOL` | 1 | 쿨 |
| `SAD` | 2 | 슬픔 |
| `SICK` | 3 | 아픔 |
| `LAUGH` | 4 | 웃음 |
| `MEH` | 5 | 보통 |
| `BAD` | 6 | 나쁨 |
| `GOOD` | 7 | 좋음 |
| `TIRED` | 8 | 피곤 |

| Weather | 레거시 | 이름 |
| --- | --- | --- |
| `SUNNY` | 0 | 맑음 |
| `PARTLY_CLOUDY` | 1 | 구름 조금 |
| `MOSTLY_CLOUDY` | 2 | 구름 많음 |
| `CLOUDY` | 3 | 흐림 |
| `RAIN` | 4 | 비 |
| `SLEET` | 5 | 비/눈 |
| `SNOW` | 6 | 눈 |

DB와 백업에는 소문자 ID(`angry`, `cool`, `sad`, `sick`, `laugh`, `meh`, `bad`, `good`, `tired` / `sunny`, `partly_cloudy`, `mostly_cloudy`, `cloudy`, `rain`, `sleet`, `snow`)로 둔다. 순서 번호로 두지 않아 종류를 더해도 기존 값이 바뀌지 않는다. 이전 문서의 ID와 같다(`docs/migration.md`).

## SearchFilter

`query: String`, `moods: Set<Mood>`, `weathers: Set<Weather>`, `hasPhoto: Boolean`, `favoritesOnly: Boolean`, `period: DateRange?`.
통계의 기분 → 일기 이동은 `SearchFilter(moods = {m}, period = 그 통계 기간)`을 만든다(FR-031).

## StatsPeriod

`Week(start)`, `Month(yearMonth)`, `Year(year)`, `All`. 이전·다음은 같은 종류로 한 칸씩, 오늘이 든 기간 뒤로는 가지 않는다.

## ListPreferences (DataStore)

`previewThreeLines: Boolean`(레거시 `skip_note`, 기본 true), `viewMode: TEXT | PHOTO`, `favoritesOnly: Boolean`.
