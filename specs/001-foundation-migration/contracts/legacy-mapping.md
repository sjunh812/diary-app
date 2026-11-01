# 레거시 → 새 모델 대응 계약

`shared/core/migration`이 지키는 규칙이다. 기준 문서는 `docs/migration.md` "옮겨 담기"이고, 여기서는 코드와 시험이 그대로 따를 수 있게 모호한 곳을 정한다.
이 규칙을 바꾸면 이전 고정 파일 시험의 기대값과 `docs/migration.md`를 같은 커밋에서 고친다.

## 일기 줄 (`Note` → `entry`)

| 레거시 | 새 값 | 규칙 |
| --- | --- | --- |
| `_id` | `legacy_id` | 그대로 |
| - | `id` | 새 UUID |
| `create_date` | `legacy_create_date` | 원문 그대로(null이면 null) |
| `create_date` 앞 10글자 | `date` | `yyyy-MM-dd`로 읽혀 실제 있는 날짜면 그대로. 아니면 `modify_date` 앞 10글자, 그것도 아니면 이전한 날(기기 시간대)을 쓰고 `dateUnparsed`를 센다 |
| `create_date` 12~16번째 글자 | `time` | `HH:mm`(00~23, 00~59)이면 그대로, 아니면 `00:00`이고 `dateUnparsed`를 센다. 초 자리는 버린다 |
| `create_date` | `dateDiffers` 집계 | 옛 앱의 느슨한 해석(초 자리를 초로 더해 넘치면 다음 날)으로 만든 날짜가 `date`와 다르면 센다. 예 `2024-03-01 23:59:457` → 3월 2일 00:07:37 → 다름 |
| `date` + `time` | `created_at` | 기기 시간대의 그 시각 |
| `modify_date` | `updated_at` | `yyyy-MM-dd HH:mm`까지 읽히면 그 시각, 아니면 `created_at` |
| `contents` | `plain_text`, `body_html` | 아래 "본문 변환". null은 `""` |
| `mood` | `mood` | 0 `angry`, 1 `cool`, 2 `sad`, 3 `sick`, 4 `laugh`, 5 `meh`, 6 `bad`, 7 `good`, 8 `tired`. 그 밖(−1, null, 범위 밖)은 null |
| `weather` | `weather` | 0 `sunny`, 1 `partly_cloudy`, 2 `mostly_cloudy`, 3 `cloudy`, 4 `rain`, 5 `sleet`, 6 `snow`. 그 밖은 null |
| `address` | `place_name` | 앞뒤 공백을 뺀 값이 비면 null, 아니면 원문 그대로(자르지 않음) |
| `star` | `is_favorite` | 1이면 true, 0·null·그 밖은 false |
| `location_x`, `location_y` | `legacy_location` | 둘 다 비었거나 null이면 null, 아니면 `x|y` 원문 |
| `picture` | `media` 줄들 | 아래 "사진" |
| `year`, `month` | 담지 않음 | `date`와 어긋나도 이전 결과에 남기지 않는다(옛 앱 버그, `docs/migration.md`) |

## 본문 변환

`legacyToHtml(contents)`:

1. `contents`가 `""`이면 `""`.
2. 왼쪽부터 `"\n\n"`이 나올 때마다 잘라 덩어리 목록을 만든다(`"a\n\n\nb"` → `["a", "\nb"]`).
3. 덩어리마다 `&` → `&amp;`, `<` → `&lt;`, `>` → `&gt;`, `"` → `&quot;`, `'` → `&#39;`로 바꾼 뒤 `"\n"`을 `<br>`로 바꾸고 `<p>…</p>`로 감싼다.
4. 이어 붙인다. 공백, 탭, `\r`, 이모지, 그 밖의 글자는 건드리지 않는다.

`toPlainText(html)`(스펙 002 `contracts/rich-text-html.md`와 같은 함수): `p`는 `"\n\n"`으로 잇고 `br`은 `"\n"`, `li`는 앞에 `• `와 줄바꿈,
엔티티(`&amp;` `&lt;` `&gt;` `&quot;` `&#39;` `&#숫자;` `&#x16진수;` `&nbsp;`)는 글자로 풀고, 공백은 줄이지 않는다. `""`은 `""`.

모든 레거시 글에서 `toPlainText(legacyToHtml(c)) == c`다. 고정 시험 값:

| 원문 | `body_html` |
| --- | --- |
| `""` | `""` |
| `"a"` | `<p>a</p>` |
| `"a\n"` | `<p>a<br></p>` |
| `"\n"` | `<p><br></p>` |
| `"\n\n"` | `<p></p><p></p>` |
| `"a\n\nb"` | `<p>a</p><p>b</p>` |
| `"a\n\n\nb"` | `<p>a</p><p><br>b</p>` |
| `"a\n\n\n\nb"` | `<p>a</p><p></p><p>b</p>` |
| `"<b>&amp;</b>"` | `<p>&lt;b&gt;&amp;amp;&lt;/b&gt;</p>` |
| `"  두 칸\t탭\r\n"` | `<p>  두 칸\t탭\r<br></p>` |
| `"😀 &nbsp;"` | `<p>😀 &amp;nbsp;</p>` |

그 밖에 10만 자 글, 서로게이트 쌍이 덩어리 경계에 걸린 글도 왕복 시험에 넣는다.

## 사진 (`picture` → `media`)

1. `picture`가 null이거나 `""`이면 사진 없음.
2. `","`로 나누고 빈 조각을 버린다(옛 앱은 끝에도 `,`를 붙인다). 조각의 순서가 `sort_order`다.
3. 조각 경로에 파일이 있으면 그 파일, 없고 조각에 앞뒤 공백이 있으면 다듬은 경로를 한 번 더 본다.
4. 파일이 있으면 `media/<sha256>.<ext>`로 복사(같은 해시가 이미 있으면 건너뜀)하고 `status = normal`. 확장자는 경로 확장자를 소문자로,
   없거나 알 수 없으면 파일 머리 바이트(JPEG `FF D8 FF`, PNG `89 50 4E 47`, WebP `RIFF....WEBP`, HEIF `ftypheic`·`ftypmif1`)로, 그래도 모르면 `bin`.
5. 파일이 없거나 읽다 실패하면 `status = missing`, 파일 열은 null.
6. 찾았든 못 찾았든 `legacy_path`에 조각 원문을 담는다.

## 설정

| 레거시(`pref.xml`, `pref2.xml`) | 새 값 | 규칙 |
| --- | --- | --- |
| `font_key` | `settings.font`, `font_replaced_from` | `data-model.md` DiaryFont 표. -1·0·1·3·100이 아니면 옛 이름을 `font_replaced_from`에 |
| `font_size` | `settings.body_text_size` | 0·1 `small`, 2·없음 `medium`, 3 `large`, 4 `xlarge`, 그 밖 `medium` |
| `mode_key` | `settings.theme_mode` | 0 `system`, 1 `light`, 2 `dark`, 그 밖 `system` |
| `ask_location` | `settings.ask_location_again` | 없으면 true |
| `use_pw`, `password` | `lock.enabled`, `pin_mac`, `pin_salt` | `use_pw`가 true이고 `password`가 ASCII 숫자 4자리면 확인값을 만들고 켠다. `use_pw`가 true인데 비밀번호가 맞지 않는 모양이면 잠금은 켜고 확인값은 비워 "키 없음" 흐름(폰 화면 잠금 확인 → 새 비밀번호)으로 보낸다. 폰에 화면 잠금이 없으면 잠금을 끄고 새 비밀번호를 정하라고 알린다 |
| `finger_print` | `lock.biometric_enabled` | 잠금이 켜질 때만 이어 간다 |
| - | `lock.relock_after` | `one_minute`(스펙 FR-017) |
| `is_alarm_key`, `hour_key`, `minute_key` | `reminder_enabled`, `reminder_minute_of_day` | 시 0~23, 분 0~59가 아니면 22:00 |
| 값이 `true`인 이메일 모양 키, `"<계정>'s id"` | `legacy_backup_account`, `legacy_backup_file_id` | `data-model.md` LegacyBackupLink |
| `skip_note`, `permission_guide` | 옮기지 않음 | FR-021, FR-023 |

## 검증 지문

필드는 `U+001F`, 사진 목록 항목은 `U+001E`로 잇고 UTF-8 바이트의 SHA-256을 소문자 hex로 쓴다.

```text
legacyCanonical(row) = "v1" US _id US create_date원문 US date US time
                       US contents US contents US mood숫자 US weather숫자 US address원문또는"" US star
                       US legacy_location US 사진들( 조각원문 "=" (sha256 | "missing") 를 RS로 이음 )

restoredCanonical(entry, media) = "v1" US legacy_id US legacy_create_date US date US time
                       US toPlainText(body_html) US plain_text US mood→숫자 US weather→숫자 US place_name또는"" US is_favorite→0/1
                       US legacy_location US media를 sort_order로( legacy_path "=" (sha256 | "missing") 를 RS로 이음 )
```

- `legacyCanonical`의 `date`, `time`은 이 계약의 규칙으로 만든 값, `mood`·`weather` 숫자는 범위 밖이면 −1, `address`는 `place_name` 규칙을 거친 값
  (공백만 있으면 `""`), `star`는 0/1로 고친 값이다.
- `restoredCanonical`은 새 DB에서 읽은 값만 쓴다. 두 지문이 줄마다 같아야 한다.
- 지문과 별도로 `normal` 사진 파일은 있어야 하고 다시 잰 SHA-256이 `sha256`과 같아야 한다.
