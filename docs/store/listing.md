# 스토어 등록 문구

Google Play 2.0.0 기준이다. 스크린샷은 `docs/store/screens/`(폰 `phone-*`, 태블릿 `tablet-*`), 만드는 법은 아래 "스크린샷 머리말"에 있다.
없는 기능(위젯, 동기화, 스티커)과 순위·수상·할인 표현은 쓰지 않는다(Play 메타데이터 정책).

## 앱 이름

- 한국어: 1일 1일기 - 바쁜 현대인을 위한 간편한 일기장
- 영어: 1 Day 1 Diary - Simple Journal
- 일본어: 1日1日記 - 忙しい人のためのかんたん日記帳

1.7.4부터 쓰던 한국어 이름을 그대로 둔다. 당근·오늘의집처럼 "브랜드 - 한 줄 소개" 꼴이라 무엇을 하는 앱인지 읽히고, 기존 사용자와 검색 노출이 이어진다(2026-11-12 사용자 결정). 영어는 Play 제한 30자에 맞춰 줄였다.

## 간단한 설명

Play는 2026-11-24에 아래 간단한 설명을 게시했다. 같은 날 자세한 설명 첫머리를 두 줄로 바꿔 기본·맞춤 등록정보 여섯 곳을 검토에 보냈다. 관리형 게시가 꺼져 있어 승인되면 바로 게시된다. App Store 설명은 Play와 같은 글을 2.2.0 버전에 세 언어로 넣었다(2026-11-25, 심사 전. 출시된 버전은 설명을 고칠 수 없다).

- 한국어: 한 줄이면 오늘 일기 끝. 짧게 써도 매일 쌓이면 나만의 기록이 돼요.
- 영어: Today's entry in one line. Short entries add up to a record of your own.
- 일본어: 1行で今日の日記は完了。短くても、毎日つづければ自分だけの記録になります。

짧은 칸에는 앱을 쓰는 까닭만 둔다. 드라이브 백업은 줄여 쓰면 무엇이 어디로 가는지 흐려져 자세한 설명에서 다룬다. 이름의 "바쁜 현대인을 위한 간편한 일기장"과 설명 첫 줄을 되풀이하지 않고, App Store 홍보 문구 앞 두 줄과 같은 말을 쓴다. 기분은 고르지 않아도 되므로 첫머리에 기분을 앞세우지 않는다(2026-11-24 사용자 지적, 소개 사이트와 같은 말).

## 자세한 설명

Play는 목록 태그를 받지 않아 줄머리표(•)로 쓰고 묶음 제목만 `<b>`로 굵게 한다. 광고 위치는 Play의 "광고 포함" 표시와 "새로운 기능" 란이 알려 주므로 여기서는 빼고 Plus 혜택만 짧게 둔다. 복원 합치기, 개인정보, 예전 버전 안내도 넣지 않는다(2026-11-12 사용자 검토).

### 한국어

```text
바쁜 날에도 쓰기 쉽게 꼭 필요한 것만 담은 일기장이에요.
매일 한 편씩 쓰는 습관을 만들어 보세요.

<b>일기 쓰기</b>
• 사진, 날씨, 장소를 함께 담아요. 사진은 한 편에 10장까지예요.
• 9가지 기분으로 그날의 감정을 기록할 수 있어요.
• 글자 색과 형광펜으로 중요한 부분을 표시해요.
• 쓰는 동안 자동으로 임시 저장돼요.

<b>지난 일기 보기</b>
• 목록과 달력으로 지난 일기를 한눈에 봐요.
• 검색과 즐겨찾기로 원하는 일기를 바로 찾아요.
• 주·월·년 통계로 어떤 기분이 많았는지 살펴봐요.
• 실수로 삭제한 일기도 30일 동안 휴지통에서 복원할 수 있어요.

<b>백업과 잠금</b>
• 일기와 사진을 내 구글 드라이브에 백업해요. 드라이브 없이 파일 하나로 보관할 수도 있어요.
• 비밀번호와 생체인증으로 나만 볼 수 있게 잠가요.

<b>글꼴과 화면</b>
• 글꼴 10종, 본문 글자 크기, 다크 모드를 원하는 대로 골라요.
• 목록은 피드, 카드, 사진 격자 세 가지로 볼 수 있어요.
• 태블릿에서는 목록과 일기가 나란히 보여요.

<b>Plus(연 구독)</b>
• 광고 없이 써요.
• 한 편에 사진을 30장까지 넣어요.
• 체크리스트와 고급 통계를 써요.
• 광고만 없애는 1회 구매도 있어요.

문의: sjunh812@naver.com
```

### 영어

```text
A diary with just what you need, so it's easy to write even on busy days.
Build a habit of writing one entry a day.

<b>Write</b>
• Add photos, weather, and place. Up to 10 photos per entry.
• Mark the day with one of nine moods.
• Mark what matters with text color and highlights.
• Your draft is saved automatically as you write.

<b>Look back</b>
• See past entries at a glance in a list or on a calendar.
• Find any entry quickly with search and favorites.
• See which moods came up most each week, month, and year.
• Deleted an entry by mistake? Restore it from Trash within 30 days.

<b>Backup and lock</b>
• Back up entries and photos to your own Google Drive, or keep them in a single file without Drive.
• Lock the app with a passcode or biometrics so only you can read it.

<b>Fonts and display</b>
• Choose from 10 fonts, body text size, and dark mode.
• View your list as a feed, cards, or a photo grid.
• On tablets, the list and entry appear side by side.

<b>Plus (yearly subscription)</b>
• Write without ads.
• Add up to 30 photos per entry.
• Use checklists and advanced stats.
• Just want to remove ads? There's a one-time purchase too.

Contact: sjunh812@naver.com
```

### 일본어

```text
忙しい日でも書きやすいように、必要なものだけを入れた日記帳です。
毎日1つずつ書く習慣をつくってみましょう。

<b>日記を書く</b>
• 写真、天気、場所を一緒に残せます。写真は1件に10枚までです。
• 9種類の気分で、その日を記録できます。
• 文字色やマーカーで、大事なところを目立たせられます。
• 書いている間は自動で下書き保存されます。

<b>過去の日記を見る</b>
• 一覧とカレンダーで過去の日記をひと目で見られます。
• 検索とお気に入りで、読みたい日記をすぐに探せます。
• 週・月・年ごとに、どんな気分が多かったかをふりかえれます。
• 間違えて削除した日記も、30日間はゴミ箱から復元できます。

<b>バックアップとロック</b>
• 日記と写真を自分のGoogleドライブにバックアップできます。ドライブを使わずに1つのファイルで保管することもできます。
• パスコードと生体認証で、自分だけが見られるようにロックできます。

<b>フォントと画面</b>
• フォント10種類、本文の文字サイズ、ダークモードを好みに合わせて選べます。
• 一覧はフィード、カード、写真グリッドの3種類で表示できます。
• タブレットでは一覧と日記が並んで表示されます。

<b>Plus（年間サブスクリプション）</b>
• 広告なしで使えます。
• 1件に写真を30枚まで追加できます。
• チェックリストと詳しい統計を使えます。
• 広告だけを消せる1回の購入もあります。

お問い合わせ: sjunh812@naver.com
```

## App Store 설명

App Store는 `<b>`를 그리지 않아 묶음 제목을 그대로 두고, "태블릿"은 "iPad"로 바꿨다. 아래 본문 다음 줄에 문의 주소, 그 아래에 구독 안내와 약관 줄을 붙여 한 칸에 넣는다.
구독 앱은 설명에 이용약관 링크가 있어야 한다(App Store 심사 지침 3.1.2). Day One, Daylio, Journey, Diarium, 꼬박일기처럼 설명 끝에 우리 이용약관과 처리방침 주소를 적는다. App Store Connect의 사용권 계약(EULA) 칸은 비워 Apple 표준 사용권 계약을 쓴다(2026-11-13 조사).

### 한국어

```text
바쁜 날에도 쓰기 쉽게 꼭 필요한 것만 담은 일기장이에요.
매일 한 편씩 쓰는 습관을 만들어 보세요.

일기 쓰기
• 사진, 날씨, 장소를 함께 담아요. 사진은 한 편에 10장까지예요.
• 9가지 기분으로 그날의 감정을 기록할 수 있어요.
• 글자 색과 형광펜으로 중요한 부분을 표시해요.
• 쓰는 동안 자동으로 임시 저장돼요.

지난 일기 보기
• 목록과 달력으로 지난 일기를 한눈에 봐요.
• 검색과 즐겨찾기로 원하는 일기를 바로 찾아요.
• 주·월·년 통계로 어떤 기분이 많았는지 살펴봐요.
• 실수로 삭제한 일기도 30일 동안 휴지통에서 복원할 수 있어요.

백업과 잠금
• 일기와 사진을 내 구글 드라이브에 백업해요. 드라이브 없이 파일 하나로 보관할 수도 있어요.
• 비밀번호와 생체인증으로 나만 볼 수 있게 잠가요.

글꼴과 화면
• 글꼴 10종, 본문 글자 크기, 다크 모드를 원하는 대로 골라요.
• 목록은 피드, 카드, 사진 격자 세 가지로 볼 수 있어요.
• iPad에서는 목록과 일기가 나란히 보여요.

Plus(연 구독)
• 광고 없이 써요.
• 한 편에 사진을 30장까지 넣어요.
• 체크리스트와 고급 통계를 써요.
• 광고만 없애는 1회 구매도 있어요.

문의: sjunh812@naver.com

Plus 구독 안내
• 1년마다 자동으로 갱신돼요. 갱신 24시간 전까지 해지하지 않으면 다음 1년 요금이 결제돼요.
• 구독 관리와 해지는 설정 > Apple 계정 > 구독에서 할 수 있어요.

이용약관: https://sjunh812.pages.dev/1day1diary/terms/
개인정보처리방침: https://sjunh812.pages.dev/1day1diary/privacy/
```

### 영어

```text
A diary with just what you need, so it's easy to write even on busy days.
Build a habit of writing one entry a day.

Write
• Add photos, weather, and place. Up to 10 photos per entry.
• Mark the day with one of nine moods.
• Mark what matters with text color and highlights.
• Your draft is saved automatically as you write.

Look back
• See past entries at a glance in a list or on a calendar.
• Find any entry quickly with search and favorites.
• See which moods came up most each week, month, and year.
• Deleted an entry by mistake? Restore it from Trash within 30 days.

Backup and lock
• Back up entries and photos to your own Google Drive, or keep them in a single file without Drive.
• Lock the app with a passcode or biometrics so only you can read it.

Fonts and display
• Choose from 10 fonts, body text size, and dark mode.
• View your list as a feed, cards, or a photo grid.
• On iPad, the list and entry appear side by side.

Plus (yearly subscription)
• Write without ads.
• Add up to 30 photos per entry.
• Use checklists and advanced stats.
• Just want to remove ads? There's a one-time purchase too.

Contact: sjunh812@naver.com

About Plus
• Renews automatically every year unless canceled at least 24 hours before the renewal date.
• Manage or cancel in Settings > Apple Account > Subscriptions.

Terms of Use: https://sjunh812.pages.dev/1day1diary/terms/
Privacy Policy: https://sjunh812.pages.dev/1day1diary/privacy/
```

### 일본어

```text
忙しい日でも書きやすいように、必要なものだけを入れた日記帳です。
毎日1つずつ書く習慣をつくってみましょう。

日記を書く
• 写真、天気、場所を一緒に残せます。写真は1件に10枚までです。
• 9種類の気分で、その日を記録できます。
• 文字色やマーカーで、大事なところを目立たせられます。
• 書いている間は自動で下書き保存されます。

過去の日記を見る
• 一覧とカレンダーで過去の日記をひと目で見られます。
• 検索とお気に入りで、読みたい日記をすぐに探せます。
• 週・月・年ごとに、どんな気分が多かったかをふりかえれます。
• 間違えて削除した日記も、30日間はゴミ箱から復元できます。

バックアップとロック
• 日記と写真を自分のGoogleドライブにバックアップできます。ドライブを使わずに1つのファイルで保管することもできます。
• パスコードと生体認証で、自分だけが見られるようにロックできます。

フォントと画面
• フォント10種類、本文の文字サイズ、ダークモードを好みに合わせて選べます。
• 一覧はフィード、カード、写真グリッドの3種類で表示できます。
• iPadでは一覧と日記が並んで表示されます。

Plus（年間サブスクリプション）
• 広告なしで使えます。
• 1件に写真を30枚まで追加できます。
• チェックリストと詳しい統計を使えます。
• 広告だけを消せる1回の購入もあります。

お問い合わせ: sjunh812@naver.com

Plusについて
• 更新日の24時間前までに解約しない限り、1年ごとに自動更新されます。
• 管理と解約は「設定」>「Apple アカウント」>「サブスクリプション」から行えます。

利用規約: https://sjunh812.pages.dev/1day1diary/terms/
プライバシーポリシー: https://sjunh812.pages.dev/1day1diary/privacy/
```

## App Store 이름·부제·홍보 문구·키워드

App Store에만 있는 칸이다. 2026-11-16 App Store Connect에 넣은 값이다. 이름과 부제의 낱말은 이미 검색에 잡히고, 키워드는 띄어쓰기 없이 쉼표로만 나눈다([Apple 검색 안내](https://developer.apple.com/app-store/search/)). 홍보 문구는 심사 없이 바꿀 수 있고, 아래 문구는 2026-11-24 세 언어에 넣었다.

| 칸 | 한국어 | 영어 | 일본어 |
| --- | --- | --- | --- |
| 이름(30자) | 1일 1일기 | 1Day1Diary - Everyday Diary | 1日1日記 - 毎日つける日記 |
| 부제(30자) | 바쁜 현대인을 위한 간편한 일기장 | A simple diary for busy people | 忙しい人のためのかんたん日記 |

2.2.1(2026-11-26)부터 이름을 Play와 맞췄다. "일기장"을 가장 센 칸인 이름에 두려는 것이다. 한국 App Store에서 "일기장" 검색이 200위 밖이었다. 부제는 일기 습관을 들이는 내용으로, 이름과 겹치지 않는 낱말(습관, 매일, 한 줄)을 쓴다(사용자 결정).

| 칸 | 한국어 | 영어(미국) | 영어(영국) | 일본어 |
| --- | --- | --- | --- | --- |
| 이름 | 1일 1일기 - 바쁜 현대인을 위한 간편한 일기장 | 1Day1Diary - Simple Journal | 1Day1Diary: Simple Journal | 1日1日記 - 忙しい人のためのかんたん日記帳 |
| 부제 | 하루 한 줄, 매일 쓰는 습관 | Build a daily journaling habit | Build a daily journaling habit | 1日1行、毎日つづく日記習慣 |

- 영국 영어 현지화를 더했다. 싱가포르·홍콩·말레이시아·인도·필리핀·남아공·UAE·이스라엘·나이지리아·케냐·파키스탄의 기본 언어이고, 호주·뉴질랜드·한국의 추가 언어다. 전에는 홍콩이 맞는 현지화가 없어 한국어 페이지로 떴다. 근거: Apple "App Store localizations" 표.
- App Store는 한 앱 안에서도 현지화마다 이름이 달라야 한다. 영국 영어에 미국 영어와 같은 이름을 넣으면 "이미 사용 중"으로 거절해 쌍점 꼴로 바꿨다. 호주·캐나다 영어도 같은 까닭으로 넣지 않았다. 호주·뉴질랜드는 영국 영어를 함께 보고, 캐나다는 미국 영어로 보인다(lookup으로 확인).
- 영국 영어의 설명·홍보 문구·URL은 미국 영어와 같고, 스크린샷은 미국 영어 그림을 이어받는다.
- 검색 결과 그림(3:2)은 세 언어 모두 뺐다(2026-11-26). 검색 결과에 미리보기가 안 보인다는 말이 있었고, 빼면 스크린샷이 뜬다.

App Store 이름은 Play와 다르다. 한국어 부제는 1.7.4부터 쓰던 한 줄 소개다(사용자 결정). 영어 "1 Day 1 Diary"와 일본어 「1日1日記」는 다른 앱이 이미 써서 넣을 수 없었다. 그래서 "브랜드 - 한 줄 소개" 꼴로 이름을 늘렸다(`docs/DECISIONS.md` 2026-11-16).

홍보 문구(170자)

```text
한 줄이면 오늘 일기 끝.
짧게 써도 매일 쌓이면 나만의 기록이 돼요.
일기와 사진은 내 기기와 내 구글 드라이브에만 남아요.
```

```text
Today's entry, in one line.
Short entries add up to a record of your own.
Your entries and photos stay on your device and your own Google Drive.
```

```text
1行で今日の日記は完了。
短くても、毎日つづければ自分だけの記録になります。
日記と写真は、自分の端末と自分のGoogleドライブだけに残ります。
```

홍보 문구와 설명 첫 줄이 화면에 이어 보여 같은 말이 두 번 나오지 않게 했다. 설명 첫머리의 "회원가입은 필요 없어요"는 세 문장의 말끝이 매번 바뀌어 어색해 뺐다(2026-11-24 사용자 결정).

키워드(100자). 한국어는 이름의 "일기"와 부제의 "일기장"을 되풀이하지 않는다. 붙여 쓴 검색어(기분일기 등)는 남긴다(2026-11-16)

```text
일기,다이어리,감정,기분,무드,기록,사진,한줄,메모,잠금,비밀,백업,캘린더,감사,회고,일상,마음,데일리,기분일기,감정일기,하루일기,비밀일기,사진일기,일기쓰기
```

```text
mood,tracker,log,feelings,photo,private,lock,backup,calendar,gratitude,notes,memories,reflect
```

영국 영어(2.2.1부터, 미국 영어와 겹치지 않게)

```text
emotions,writing,routine,mindful,wellbeing,thoughts,secret,password,reminder,goals,personal,life
```

```text
ダイアリー,気分,感情,記録,写真,一行,メモ,ロック,バックアップ,カレンダー,感謝,ライフログ,振り返り,日常,デイリー,手帳,絵日記
```

## App Store 헤더·검색 결과 그림

App Store 상품 페이지 맨 위 헤더(21:9, 3840×1646)와 검색 결과 카드(3:2, 3840×2560)다. 2026-11-16 App Store Connect 2.0.1에 세 언어로 넣었고, 2026-11-25 2.2.0에서 아래 새 틀 그림으로 바꿨다.

- 헤더·검색 결과: 2026-11-24부터 스크린샷과 같은 틀(첫 장 문구와 목록 화면)이다. 아래 "스크린샷 머리말"과 `scripts/store/make_banner.swift`.
- iPhone은 헤더를 화면 폭에 맞춰 줄여 보여 준다. 작은 그림은 쌀알처럼 보여 크게 둔다. 바꾼 뒤에는 App Store Connect [미리보기]로 iPhone·iPad를 본다.

## App Store 맞춤형 제품 페이지

검색어에 맞는 화면을 먼저 보여 주는 페이지다. 키워드 칸의 낱말을 페이지에 이어 두면 그 낱말로 검색한 사람에게 이 페이지가 뜬다(앱마다 70개, 이름·부제 낱말은 못 씀, [Apple](https://developer.apple.com/app-store/custom-product-pages/), [Phiture](https://phiture.com/blog/keyword-based-custom-product-pages-cpps-arrive-in-app-store-connect/)). 바꿀 수 있는 것은 스크린샷, 앱 미리보기, 홍보 문구다. 설명은 기본 페이지를 그대로 쓴다.

두 페이지 모두 기본 페이지를 복사한 뒤 스크린샷을 새로 찍어 바꿨다(2026-11-16 초안, 심사 전). 헤더와 검색 결과 그림은 언어별 기본 그림이다. 스크린샷은 `docs/store/screens/ios/<언어>/{mood,photo}`, 원본 캡처는 `docs/store/source/ios/<언어>/{mood,photo}`와 `tablet/{mood,photo}`다.

- 만드는 법: `seed_ios.py <UDID> <언어> <그림 폴더> mood`는 1월부터 일기를 채워 한 해 기분이 보이게 하고, `photo`는 사진 붙은 일기를 늘린다. 홈 목록은 사진 격자로 찍는다. 합성은 `make_screens.swift <캡처> <결과> <글꼴> <태블릿 캡처> <언어> mood|photo`.
- iPad는 쓰기 화면을 뺀다. 기분이 옆 칸에 작게 놓여 빈 화면처럼 보인다.

"기분 일기" 머리말: 한 해 기분이 한눈에 보여요 / 그날 기분이 달력에 남아요 / 자주 고른 기분을 돌아봐요 / 아홉 가지 기분 중 하나를 골라요(iPhone만). 영어 A year of moods at a glance / Every day's mood on your calendar / See which moods came up most / Pick one of nine moods, 일본어 1年の気分がひと目でわかる / その日の気分がカレンダーに残る / よく選んだ気分をふりかえる / 9つの気分からひとつ選ぶ(원어민 검수 전).

```text
기분을 고르고 한 줄만 써도 오늘 일기 끝.
한 달 기분을 달력으로 봐요.
어떤 기분이 많았는지 돌아봐요.
일기와 사진은 내 기기와 내 구글 드라이브에만 남아요.
```

```text
Pick a mood, write one line, done.
See your moods on a calendar and which ones came up most.
Your entries and photos stay on your device and Google Drive.
```

```text
気分を選んで1行書けば、今日の日記は完了。
1か月の気分をカレンダーで見られます。
どんな気分が多かったかをふりかえれます。
日記と写真は、自分の端末と自分のGoogleドライブだけに残ります。
```

- 키워드 잇기 칸은 아직 화면에 없다. 앱이 출시되기 전이라 그런 것으로 보이며 Apple 문서로 확인하지 못했다. 칸이 생기면 이을 낱말: 한국어 기분일기·감정일기·기분·감정·무드, 영어 mood·feelings, 일본어 気分·感情.
- 심사는 키워드를 이은 다음 넣는다. 맞춤형 제품 페이지는 앱 업데이트와 따로 심사받는다.

"사진 일기" 머리말: 사진으로 지난 일기를 봐요 / 한 편에 사진을 여러 장 담아요 / 사진까지 드라이브에 백업해요. 영어 Look back through photos / Add several photos to one entry / Back up photos to your Drive, 일본어 写真で日記をふりかえる / 1つの日記に写真を何枚も / 写真までドライブにバックアップ(원어민 검수 전).

```text
한 편에 사진을 여러 장 담아요.
사진으로 지난 일기를 봐요.
일기와 사진은 내 기기와 내 구글 드라이브에만 남아요.
```

```text
Add several photos to one entry.
Look back through your entries by photo.
Your entries and photos stay on your device and Google Drive.
```

```text
1つの日記に写真を何枚も残せます。
写真で日記をふりかえれます。
日記と写真は、自分の端末と自分のGoogleドライブだけに残ります。
```

- 이을 낱말: 한국어 사진일기·사진·백업, 영어 photo·backup, 일본어 写真·バックアップ.

## App Store 앱 내 이벤트

정말 기간이 정해진 일만 올린다. 늘 있는 기능을 이벤트로 포장하면 반려된다([Apple 배지](https://developer.apple.com/help/app-store-connect/reference/in-app-events/in-app-event-badges), [Phiture](https://phiture.com/mobilegrowthstack/in-app-events-how-to-get-approved/)). 기간은 31일까지, 공개는 시작 14일 전부터다. 카드 그림은 16:9(1920×1080 이상), 상세 그림은 9:16(1080×1920 이상)이다.

- 후보: 12월 "올해 기분 돌아보기"(돌아보기 년 탭). 이벤트에서 그 화면으로 바로 여는 딥 링크가 없어 `/diary-flow`로 먼저 만든다.
- 리텐션 메시지(구독 해지 화면 안내)는 구독자가 생긴 뒤에 본다. 할인 없이 파는 지금 판매안과 겹친다.

## 맞춤 스토어 등록정보

"1일 1일기"(검색 키워드 대상). 문구와 그래픽은 기본 등록정보를 그대로 물려받는다(그룹 애셋 상속). 검색 키워드는 일기, 일기장, 다이어리, 일기 앱, 감정 일기, 기분 일기, 하루 일기, 사진 일기, 비밀 일기다(2026-11-12).

- 영어(en-US)·일본어(ja-JP)를 더했다(2026-11-13). 같은 날 앱 콘텐츠 신고 5건과 함께 검토에 들어갔고, 관리형 게시라 승인 뒤 직접 게시해야 한다. 두 언어는 그룹을 물려받지 못해 기본 등록정보와 같은 것을 직접 넣었다: 위 이름·간단한 설명·자세한 설명, 아이콘 `play-512.png`, 피처 그래픽 `feature.png`(한국어 그림, 기본 등록정보와 같음), 휴대폰 `{en,ja}-phone-1~4`, 7·10인치 태블릿 `{en,ja}-tablet-1~4`.
- 한국어는 그룹을 물려받는데, 그룹이 아직 1.7.4 시절 글과 그림을 보여 준다. 기본 등록정보 초안을 게시하면 함께 바뀌는지 게시 뒤에 확인한다.
- 기본 등록정보의 문구나 그림을 바꾸면 여기 영어·일본어도 함께 바꾼다.

## 인앱 상품 이름

앱 안 문구와 같은 말을 쓴다.

| 상품 | 한국어 | 영어 | 일본어 |
| --- | --- | --- | --- |
| `ad_free` 이름 | 광고 제거 | Remove ads | 広告削除 |
| `ad_free` 설명 | 구독이 아니에요. 한 번만 결제하면 앞으로 계속 광고 없이 쓸 수 있어요. | It's not a subscription. Pay once and stay ad-free from now on. | サブスクリプションではありません。1回のお支払いで、ずっと広告なしで使えます。 |
| `plus_yearly` 이름 | 1일 1일기 Plus | 1 Day 1 Diary Plus | 1日1日記 Plus |
| `plus_yearly` 혜택 | 광고 없이 쓰기 · 한 편에 사진 30장 · 체크리스트 · 고급 통계 | Write without ads · 30 photos per entry · Checklists · Advanced stats | 広告なしで書く · 1件に写真30枚 · チェックリスト · 詳しい統計 |

App Store는 상품 설명 칸이 짧아(비소모품 45자, 구독 55자 안팎) 아래처럼 줄여 넣었다(2026-11-16). 이름은 위와 같다. 구독 그룹 표시 이름은 세 언어 모두 "Plus"이고, 앱 이름은 한국어 1일 1일기, 영어 1 Day 1 Diary, 일본어 1日1日記로 직접 적었다.

| 상품 | 한국어 | 영어 | 일본어 |
| --- | --- | --- | --- |
| `ad_free` 설명 | 구독이 아니에요. 한 번 결제로 계속 광고 없이 써요. | Not a subscription. Pay once, no more ads. | サブスクリプションではありません。1回のお支払いで、ずっと広告なしで使えます。 |
| `plus_yearly` 설명 | 광고 없이 쓰기 · 한 편에 사진 30장 · 체크리스트 · 고급 통계 | No ads · 30 photos/entry · Checklists · Advanced stats | 広告なしで書く · 1件に写真30枚 · チェックリスト · 詳しい統計 |

## 새로운 기능

### 2.2.0 (49)

두 스토어 모두 이 노트를 쓴다. 2.1.0과 같은 꼴이다(버전 줄 아래 세 줄, 해요체, 마침표).

```text
<ko-KR>
[버전 2.2.0]
• 앱 테마 색을 고를 수 있어요.
• 긴 일기를 쓰고 수정할 때 화면이 더 매끄러워졌어요.
• 앱 안정성을 개선했어요.
</ko-KR>
<en-US>
[Version 2.2.0]
• Choose a theme color for the app.
• Writing and editing long entries is now smoother.
• Improved app stability.
</en-US>
<ja-JP>
[バージョン 2.2.0]
• アプリのテーマカラーを選べるようになりました。
• 長い日記を書いたり編集したりするときの動きがなめらかになりました。
• アプリの安定性を改善しました。
</ja-JP>
```

### 2.1.0 (47)

Android(2.0.1에서)와 iOS(2.0.2에서) 모두 이 노트를 쓴다. 이번 버전부터 두 스토어의 버전과 빌드 번호를 맞춘다(`docs/DECISIONS.md` 2026-11-21). 버전 줄 아래 세 줄 안으로, 해요체로 쓰고 줄 끝에 마침표를 찍는다(사용자 요청 2026-11-22).

```text
<ko-KR>
[버전 2.1.0]
• 결제한 뒤에도 광고가 보이던 문제를 수정했어요.
• 목록에서 일기를 3줄, 5줄, 전체로 골라 볼 수 있어요.
• 사진 백업이 더 빨라졌어요.
</ko-KR>
<en-US>
[Version 2.1.0]
• Fixed ads still showing after purchase.
• Choose 3 lines, 5 lines, or the full entry in your list.
• Photo backups are now faster.
</en-US>
<ja-JP>
[バージョン 2.1.0]
• 購入後も広告が表示される問題を修正しました。
• 一覧で日記を3行・5行・全文から選んで表示できます。
• 写真のバックアップが速くなりました。
</ja-JP>
```

### 2.0.1 (45)

Android 사용자에게 보이는 변화 가운데 중요한 다섯 줄만 적고, 맨 위에 버전을 쓴다. 길면 헷갈린다(사용자 요청 2026-11-17). 버전 표기와 한 줄에 한 가지는 카카오톡 Play "새로운 기능"([v26.9.2] ● …)을, 버전을 먼저 쓰는 것은 Day One(2026.23:)을 따랐다. 세부 동작 대신 "스크롤 성능을 개선했어요"처럼 체감 위주로 쓴다. 리뷰 창은 적지 않는다(스토어가 띄울지 정해서 약속이 될 수 있다).

```text
<ko-KR>
[버전 2.0.1]
• 하루 한 번 나오는 전면 광고는 이제 일기를 저장한 직후에 나와요
• 기기 저장 공간이 꽉 찼을 때 앱이 꺼지던 문제를 고쳤어요
• 스크롤 성능을 개선했어요
• 알림을 꺼두면 설정의 쓰기 알림도 알림 꺼짐으로 보여요
• 생체인증을 쓸 수 없으면 잠금 설정에서 이유를 알려줘요
</ko-KR>
<en-US>
[Version 2.0.1]
• The once-a-day full-screen ad now shows right after you save an entry
• Fixed the app closing when your device storage is full
• Improved scrolling performance
• Daily reminder shows as off when notifications are blocked
• Lock settings now explain why biometrics aren't available
</en-US>
<ja-JP>
[バージョン 2.0.1]
• 1日1回の全画面広告は、日記を保存した直後に表示されるようになりました
• 端末の空き容量がないときにアプリが終了する問題を修正しました
• スクロールのパフォーマンスを改善しました
• 通知をオフにすると、設定の毎日のリマインダーもオフと表示されます
• 生体認証を使えないとき、ロック設定で理由を表示するようにしました
</ja-JP>
```

### 2.0.0 (43)

출시한 그대로 남긴 기록이다. 전면 광고 자리는 2.0.1에서 "저장해 작성 화면이 닫힐 때"로 바뀌었다(위 2.0.1 노트, `docs/DECISIONS.md` 2026-11-16).

Play는 언어마다 500자까지 받는다. 기존 사용자가 업데이트 뒤 광고를 처음 보게 되므로 광고와 광고 제거를 함께 적는다(`docs/release.md` 출시 전 확인).
Play 출시 노트 칸에 언어 태그째 붙여 넣는다. 태그(`<b>`)는 출시 노트에서 글자로 보일 수 있어 쓰지 않고, 묶음 제목은 줄로만 나눈다.
새로워진 점, Plus, 광고 안내 순서로 쓴다. 1.7.4에 있던 기능(글꼴, 다크 모드, 잠금)은 새 기능으로 적지 않는다. 전면 광고(하루 한 번, 저장 뒤 목록으로 돌아갈 때)도 적는다. 처음 보는 전면 광고에 놀라 떠나지 않게 하려는 것이다(2026-11-15).
영어·일본어는 원어민 검수 전이다.

```text
<ko-KR>
[버전 2.0.0]
1일 1일기를 처음부터 새로 만들었어요 ✨
쓰던 일기와 사진은 그대로 옮겨지고, 쓰던 기능은 계속 무료예요.

새로워진 점
• 에디터(볼드, 글자 색, 형광펜, 목록)로 글을 꾸며요
• 쓰는 동안 자동으로 임시 저장돼요
• 기분, 날씨, 기간으로 검색해요
• 기분 흐름을 주·월·년으로 봐요
• 삭제한 일기는 30일 동안 복원할 수 있어요
• 목록을 피드, 카드, 사진 격자로 봐요
• 사진까지 구글 드라이브에 백업해요
• 태블릿에서는 목록과 일기를 나란히 봐요

Plus가 새로 나왔어요
• 광고 없이 한 편에 사진 30장, 체크리스트, 고급 통계까지 써요

광고 안내
• 목록과 돌아보기에 광고가 들어가요
• 전면 광고는 하루 한 번, 저장 뒤 목록으로 돌아갈 때만 나와요
• 쓰고 읽는 화면에는 광고가 없어요
• 광고만 없애는 1회 구매도 있어요
</ko-KR>
<en-US>
[Version 2.0.0]
Rebuilt from scratch ✨
Entries and photos come along; old features stay free.

What's new
• Text styles and auto-save
• Search by mood, weather, or date
• Mood trends by week, month, year
• 30-day Trash and new list views
• Photo backup to Google Drive
• Side-by-side view on tablets

New: Plus
• No ads, 30 photos per entry, checklists, advanced stats

Ads
• In the list and Insights, plus one full-screen ad a day
• None while you write or read
• Remove ads with one purchase
</en-US>
<ja-JP>
[バージョン 2.0.0]
アプリを一から作り直しました ✨
今までの日記と写真はそのまま引き継がれ、これまでの機能はずっと無料です。

新しくなったこと
• 太字、文字色、蛍光ペン、リストで文章を飾れます
• 書いている間、自動で下書き保存されます
• 気分・天気・期間で検索し、気分の流れを週・月・年で見られます
• 削除した日記は30日間ゴミ箱から復元できます
• 一覧をフィード、カード、写真グリッドで見られます
• 日記と写真を自分のGoogleドライブにまとめてバックアップできます
• タブレットでは一覧と日記を並べて見られます

Plusが登場しました
• 広告なしで使えます
• 1つの日記に写真を30枚まで入れられます
• チェックリストと詳しい統計が使えます

広告について
• 一覧とふりかえりに広告が表示されます
• 全画面広告は1日1回、日記を保存して一覧に戻るときだけ表示されます
• 日記を書く画面と読む画面には広告はありません
• 広告だけを消す1回の購入もあります
</ja-JP>
```

## 스크린샷 머리말

2026-11-24에 새 틀로 바꿨다. 아주 연한 라벤더 바탕, 굵은 두 줄 제목과 연한 설명 한 줄, 상태바를 자른 앱 화면 전체를 얇고 밝은 틀에 담는다. 장마다 같은 꼴이고 꾸밈 그림은 넣지 않는다(사용자 지적: 잘라 낸 카드와 아이콘은 어색하다). 레퍼런스는 Toss, Day One, Things의 스토어 화면이다. 글꼴은 창원단감둥근체(일본어는 시스템 글꼴)이고, 한국어·영어는 앱 설정 글꼴도 창원단감둥근체로 바꿔 제목과 앱 화면을 한 벌로 찍는다(2026-11-25 사용자 요청). 일본어는 이 글꼴에 가나가 없어 앱 화면도 시스템 글꼴로 둔다. 목록은 피드형, 백업은 켜 둔 상태로 찍는다.

2026-11-25에 두 스토어에 올렸다.

- Play: 기본 등록정보 세 언어와 맞춤 등록정보 영어·일본어에 그래픽 이미지, 폰 6장, 7·10인치 태블릿 6장씩. 2.2.0 프로덕션 출시 때 함께 검토에 냈다.
- Play 태블릿은 같은 날 가로로 바꿔 다시 검토에 냈다. iPad 세로 캡처를 9:16 틀에 넣으니 화면이 작고 어색했다(사용자 지적). 제목은 한 줄로 두고, 일본어는 띄어 쓰지 않는다. App Store iPad는 세로 그대로다.
- 같은 날 허용 범위 안에서 가장 크게 다시 만들었다. Play 폰 1620×2880, 태블릿 3840×2160, App Store iPhone 6.9" 1320×2868이다. 작게 만든 그림은 큰 화면에서 늘어나 흐려 보였다(사용자 지적). 폰 캡처는 iPhone 17 Pro Max(1320×2868, `source/ios/<언어>/promax/`)라 앱 화면이 거의 원래 크기로 들어간다. 그래픽 이미지(1024×500)와 아이콘(512×512)은 크기가 정해져 있다.
- App Store: 2.2.0 버전 세 언어에 헤더, 검색 결과, iPhone 6장, iPad 6장. 빌드를 붙여 심사에 낼 때 함께 나간다.

1. 목록: 한 줄이면 / 오늘 일기 끝 — 바쁜 날에도 쓰기 쉬운 일기장
2. 사진 일기: 사진과 함께 / 남겨요 — 한 편에 사진 10장까지 넣을 수 있어요
3. 달력: 그날의 기분이 / 달력에 남아요 — 한 달 기분을 한눈에 봐요
4. 돌아보기: 어떤 기분이 많았는지 / 돌아봐요 — 기간별로 볼 수 있어요
5. 테마: 좋아하는 색으로 / 바꿔 보세요 — 테마 색 7가지 중에서 골라요
6. 백업: 사진까지 / 내 드라이브에 백업해요 — 회원가입 없이 하루 한 번 자동으로 올려요

영어(원어민 검수 전): One line, and today is done / Keep your day with photos / Your moods on a calendar / Look back on your moods / Make it your color / Back up photos to your own Drive.
일본어(원어민 검수 전): 1行で今日の日記は完了 / 写真と一緒に残せます / その日の気分がカレンダーに / どんな気分が多かったかふり返れます / 好きな色に変えられます / 写真まで自分のドライブに.

다섯 언어(2026-11-27, 원어민 검수 전, 아직 어느 스토어에도 올리지 않음). 같은 틀과 같은 여섯 장이고, 스크린샷 폴더 이름은 `zh-Hant`, `es`, `pt`, `de`, `fr`이다.

- 번체 중국어: 一行 就寫完今天的日記 / 和照片一起 留下今天 / 每天的心情 留在月曆上 / 回顧哪種心情 最多 / 換成 喜歡的顏色 / 連照片都備份到 自己的雲端硬碟
- 스페인어: Una línea y listo por hoy / Guarda tu día con fotos / Tu ánimo en el calendario / Repasa cómo te sentiste / Ponle tu color / Respalda tus fotos en tu propio Drive
- 포르투갈어: Uma linha e pronto por hoje / Guarde seu dia com fotos / Seu humor no calendário / Reveja seu humor / Deixe com a sua cor / Backup das fotos no seu próprio Drive
- 독일어: Eine Zeile, und der Tag ist notiert / Halte deinen Tag mit Fotos fest / Deine Stimmung im Kalender / Blick zurück auf deine Stimmung / Mach es in deiner Farbe / Fotos sichern in deinem Drive
- 프랑스어: Une ligne, et la journée est écrite / Gardez votre journée en photos / Votre humeur sur le calendrier / Revoyez vos humeurs / À vos couleurs / Sauvegardez vos photos dans votre Drive
- 가로 그림의 강조 낱말: 一行, Una línea, Uma linha, Eine Zeile, Une ligne.
- 글꼴: 창원단감둥근체에 악센트 글자(á, ç, ü, é 등)와 한자가 없다. 그래서 중국어는 시스템 글꼴(평팡 번체), 유럽 네 언어는 앱 기본 글꼴 Pretendard로 제목과 앱 화면을 함께 찍었다. 앱에서 창원단감둥근체를 고르면 악센트 글자만 다른 글꼴로 섞여 보인다.

그래픽 이미지·App Store 헤더·검색 결과 그림은 1번 제목과 설명에 목록 화면을 둔다. 가로 그림은 제목의 낱말 하나("한 줄", "One line", "1行")만 브랜드색 #6550B9로 강조하고, 글자는 화면보다 무겁지 않게 줄였다(사용자 요청 2026-11-24). 헤더는 Apple 안전 영역(가운데 1646×661)에 글만 두고, 넓은 화면에서만 보이는 양옆에 화면을 둔다. 그래픽 이미지와 검색 결과 그림은 글과 화면을 한 묶음으로 가로 가운데에 두고, 글과 화면 모두 세로 가운데에 맞춘다. 글 폭은 언어마다 재서 사이 간격만 바꾼다(2026-11-25 사용자 지적: 글과 폰 대칭).
만드는 법: 폰·태블릿 `swift scripts/store/make_store_screens.swift <캡처> <결과> <언어> <폭> <높이> <앞이름> [tablet|landscape]`(Play 태블릿은 `3840 2160 tablet landscape`, Play 폰은 Pro Max 캡처로 `1620 2880 phone`), 가로 그림 `swift scripts/store/make_banner.swift <캡처> <결과> <언어> feature|header|search`. 캡처는 `docs/store/source/ios/<언어>/`(iPhone 17 Pro), `ipad/`(iPad Pro 13" 세로), `ipad-land/`(iPad Pro 13" 가로, Play 태블릿)다.

## 소개 영상

Threads·Reddit·릴스에 올릴 세로 영상이다(1080×1920, 60fps, 7.5Mbps, 17.3초, 효과음과 배경음 포함). `docs/store/promo/promo-ko.mp4`, `promo-en.mp4`이고 2026-11-26에 확정했다.

휴대폰 화면 녹화를 틀에 넣지 않고 앱 부품을 꺼내 움직인다. 처음 만든 녹화판은 "스크린샷을 그냥 틀어 준 느낌"이라 버렸다(사용자 지적). 레퍼런스는 How We Feel(색 점과 화면 전체 색 덮기), Granola(떠 있는 입력 카드), Bevel(숫자 하나 크게), Superlist(낱말 하나씩 뜨는 자막)다. 처음에 넣었던 색 테마 장면과 달력 장면은 전하는 바가 흐려 뺐다(사용자 지적). 쓰기 카드는 앱에 없는 한 줄 입력창이 아니라 실제 쓰기 화면을 잘라 쓴다(사용자 지적).

0. 첫 제목 카드(2.3초): 앱 이름, "하루 한 줄로 / 꾸준히 쓰는 / 일기장"(형광펜 "꾸준히"), 아래에서 올라오는 웃음·좋음 얼굴과 오른쪽 가장자리에서 빼꼼 내미는 쿨 얼굴. 요미네코 소개 영상처럼 기능 전에 앱부터 알린다(사용자 제안)
1. 바쁜 날엔 / 한 줄이면 충분해요: 실제 쓰기 화면 카드에 "퇴근길에 본 노을이 너무 예뻤다 🌇"가 쳐진다
2. 그날의 기분을 / 골라봐요: 기분 시트처럼 5개·4개 두 줄로 얼굴이 뜨고, 웃음을 고르면 칩이 "웃음"으로 바뀐다
3. 이번 달엔 웃음이 / 가장 많았어요: 고른 웃음 얼굴이 그대로 날아가 36% 옆에 앉고 기분 막대가 자란다
4. 사진도 / 함께 남겨요: 사진 세 장이 일기 카드 위로 날아온다
5. 회원가입 없이 · 잠금으로 나만 보기 · 사진까지 내 드라이브에 백업: 밝은 바탕에 다크 홈 화면과 흰 칩
6. 아이콘, 이름, App Store · Google Play(밝은 바탕)

영어: Busy day? / One line is enough, Pick the mood / of your day, Top mood this month: / Laughing, Add photos / to your day, No sign-up · Lock it, keep it private · Photos back up to your Drive.

장면마다 자막을 다 읽을 시간을 둔다(사용자 지적: 기분 고르기·웃음 고른 뒤·약속 칩이 빨랐다). 통계 숫자는 시연용이다. 소리는 남의 음원 없이 스크립트에서 합성한다. 첫 제목 카드는 배경음만 둔다(사용자 지적). 자막은 모두 1.5초 넘게 머문다. 저장소는 파일 하나를 15MB까지만 받아(저장소 위생 검사) 7.5Mbps로 뽑는다. 올릴 때는 비트레이트만 20Mbps로 바꿔 따로 뽑은 고화질판을 쓴다.

- 배경음: 둥근 펠트 피아노가 가운데 음역에서 반 박마다 움직이는 장조 아르페지오(Cadd9-G6-Fmaj9-G7, 한 마디 2초)이고 박마다 낮은 뿌리음이 받친다. 처음 칼림바판은 음역이 높았고, 더 차분한 두 후보는 너무 처졌다(사용자 지적). 그 사이로 골랐다.
- 효과음: 실로폰 같은 얇은 소리가 거슬리고 끝이 웅장하다는 지적(사용자)에 방울 소리와 나무 실로폰(마림바) 소리로 바꿨다. 귀엽고 높지 않게, 배경음보다 조금 위 음역이다. 기분이 뜰 때 방울 셋, 웃음을 고르면 "뿅"과 시-레-솔, 착지, 퍼센트가 오르는 만큼 음계를 따라 오르는 톡, 사진 세 번, 칩 세 음, 로고가 뜰 때 미-솔-도다.
- 어울림: 효과음의 끝음은 그 순간 배경음 화음의 음이다(Fmaj9 → G7 → C → G → Fmaj9, 마지막 G7은 로고의 도로 풀림). 장면 시각을 바꾸면 효과음 음도 다시 맞춘다. 고르기와 로고 소리에만 배경음을 0.4초 낮추고, 작은 소리는 낮추지 않는다(사진 소리가 과하다는 지적).

만드는 법: 문구와 시간은 `docs/store/promo/<언어>.json`, 재료는 `docs/store/promo/assets/`다. 저장소 맨 위에서 `swiftc -O scripts/store/promo/make_promo.swift -o /tmp/make_promo` 뒤 `/tmp/make_promo docs/store/promo/ko.json`. 기분 얼굴은 앱의 벡터 경로(`DiaryMoodFace`)와 기분 색을 그대로 옮겨 그려 크게 키워도 선명하다. 쓰기 카드와 다크 홈은 시뮬레이터 녹화에서 `grab_frame.swift`로 뽑았다.

## 다섯 언어 등록정보 초안 (2026-11-27)

원어민 검수 전 초안이다. 아직 어느 스토어에도 넣지 않았다.

- 기능 범위는 위 영어 원문과 같다. 화면 낱말은 앱 문자열(`values-{b+zh+Hant,es,pt,de,fr}`)을 따른다. 예: 돌아보기 回顧·Resumen·Insights·Rückblick·Bilan, 휴지통 垃圾桶·papelera·lixeira·Papierkorb·corbeille.
- App Store 이름은 다섯 언어끼리, 그리고 기존 이름(영어 두 가지, 한국어, 일본어, 2.2.0까지 쓴 "1Day1Diary - Everyday Diary")과 모두 다르다. 브랜드는 "1Day1Diary", 중국어는 「1日1日記」다.
- 키워드는 이름·부제 낱말을 되풀이하지 않는다. App Store는 이름·부제·키워드 낱말을 섞어 검색에 쓰므로, 프랑스어 "journal intime"은 이름의 journal과 키워드의 intime으로, 중국어 「心情日記」는 붙여 쓴 검색어로 남긴다(한국어 기분일기와 같은 꼴).
- 글자 수는 Play·App Store 모두 글자 기준, 키워드는 UTF-8 바이트다. 기존 한국어(215바이트)·일본어(175바이트) 키워드가 통과한 것을 보면 App Store는 글자로 세는 것으로 보인다. 그래도 이 초안은 100바이트 안에 맞췄다. 글자 기준이면 중국어는 낱말을 더 넣을 수 있다.
- App Store 설명은 Play 설명에서 `<b>`를 빼고 태블릿 줄을 iPad로 바꾼 뒤 구독 안내와 약관 줄을 붙였다.
- 언어 코드: Play는 zh-TW, es-419, pt-BR, de-DE, fr-FR. App Store는 중국어(번체), 스페인어(멕시코), 포르투갈어(브라질), 독일어, 프랑스어.

### 한눈에 보기

| 언어 | Play 이름 | App Store 이름 | App Store 부제 |
| --- | --- | --- | --- |
| 번체 중국어(대만, zh-TW) | 1日1日記 - 給忙碌的人的簡單日記本 (19자) | 1日1日記 - 簡單日記本 (13자) | 每天一句，養成寫日記的習慣 (13자) |
| 스페인어(중남미 중립, es-MX·es-419, tú) | 1 Day 1 Diary - Diario simple (29자) | 1Day1Diary - Diario simple (26자) | Crea el hábito de escribir (26자) |
| 브라질 포르투갈어(pt-BR, você) | 1 Day 1 Diary - Diário simples (30자) | 1Day1Diary - Diário simples (27자) | Crie o hábito de escrever (25자) |
| 독일어(de-DE, du) | 1 Day 1 Diary - Tagebuch (24자) | 1Day1Diary: Einfaches Tagebuch (30자) | Jeden Tag eine Zeile schreiben (30자) |
| 프랑스어(fr-FR, vous) | 1 Day 1 Diary - Journal intime (30자) | 1Day1Diary - Journal simple (27자) | Prenez l’habitude d’écrire (26자) |

### 번체 중국어(대만, zh-TW)

- Play 이름: 1日1日記 - 給忙碌的人的簡單日記本 (19자)
- Play 간단한 설명: 一行就寫完今天的日記。短短幾句，每天累積，就是屬於你的紀錄。 (30자)
- App Store 이름: 1日1日記 - 簡單日記本 (13자)
- App Store 부제: 每天一句，養成寫日記的習慣 (13자)
- App Store 키워드 (96바이트):

```text
心情日記,日誌,手帳,心情,情緒,紀錄,照片,私密,密碼,備份,月曆,感恩,回顧
```

App Store 홍보 문구 (63자)

```text
一行就寫完今天的日記。
短短幾句，每天累積，就是屬於你的紀錄。
日記和照片只會留在你的裝置和你自己的 Google 雲端硬碟。
```

Play 자세한 설명 (483자)

```text
一本只放必要功能的日記，忙碌的日子也能輕鬆寫。
養成每天寫一篇的習慣吧。

<b>寫日記</b>
• 加入照片、天氣和地點。每篇最多10張照片。
• 從9種心情中選一種，記下這一天。
• 用文字顏色和螢光筆標出重點。
• 書寫時會自動儲存草稿。

<b>回顧過去</b>
• 用列表或月曆，一眼看到過去的日記。
• 用搜尋和收藏，快速找到想看的日記。
• 看看每週、每月、每年哪種心情最多。
• 不小心刪除了日記？30天內都能從垃圾桶還原。

<b>備份與鎖定</b>
• 把日記和照片備份到你自己的 Google 雲端硬碟，也可以不用雲端硬碟，存成一個檔案。
• 用密碼或生物辨識鎖定，只有你能看。

<b>字型與畫面</b>
• 可選擇10種字型、內文文字大小和深色模式。
• 列表可用動態、卡片、照片格狀三種方式檢視。
• 在平板上，列表和日記會並排顯示。

<b>Plus（年訂閱）</b>
• 無廣告寫日記。
• 每篇最多加入30張照片。
• 可使用核取清單和進階統計。
• 只想移除廣告？也有一次性購買。

聯絡我們：sjunh812@naver.com
```

App Store 설명 (627자)

```text
一本只放必要功能的日記，忙碌的日子也能輕鬆寫。
養成每天寫一篇的習慣吧。

寫日記
• 加入照片、天氣和地點。每篇最多10張照片。
• 從9種心情中選一種，記下這一天。
• 用文字顏色和螢光筆標出重點。
• 書寫時會自動儲存草稿。

回顧過去
• 用列表或月曆，一眼看到過去的日記。
• 用搜尋和收藏，快速找到想看的日記。
• 看看每週、每月、每年哪種心情最多。
• 不小心刪除了日記？30天內都能從垃圾桶還原。

備份與鎖定
• 把日記和照片備份到你自己的 Google 雲端硬碟，也可以不用雲端硬碟，存成一個檔案。
• 用密碼或生物辨識鎖定，只有你能看。

字型與畫面
• 可選擇10種字型、內文文字大小和深色模式。
• 列表可用動態、卡片、照片格狀三種方式檢視。
• 在 iPad 上，列表和日記會並排顯示。

Plus（年訂閱）
• 無廣告寫日記。
• 每篇最多加入30張照片。
• 可使用核取清單和進階統計。
• 只想移除廣告？也有一次性購買。

聯絡我們：sjunh812@naver.com

關於 Plus
• 除非在續訂日至少24小時前取消，否則每年會自動續訂。
• 可在「設定」>「Apple 帳號」>「訂閱項目」管理或取消。

使用條款：https://sjunh812.pages.dev/1day1diary/terms/
隱私權政策：https://sjunh812.pages.dev/1day1diary/privacy/
```

### 스페인어(중남미 중립, es-MX·es-419, tú)

- Play 이름: 1 Day 1 Diary - Diario simple (29자)
- Play 간단한 설명: Tu entrada de hoy en una línea. Las entradas cortas se vuelven tu registro. (75자)
- App Store 이름: 1Day1Diary - Diario simple (26자)
- App Store 부제: Crea el hábito de escribir (26자)
- App Store 키워드 (99바이트):

```text
personal,journal,ánimo,emociones,notas,fotos,privado,secreto,bloqueo,calendario,gratitud,recuerdos
```

App Store 홍보 문구 (161자)

```text
Tu entrada de hoy, en una línea.
Las entradas cortas se vuelven tu propio registro.
Tus entradas y fotos se quedan en tu dispositivo y en tu propio Google Drive.
```

Play 자세한 설명 (1381자)

```text
Un diario con solo lo que necesitas, para que escribir sea fácil incluso en los días ocupados.
Crea el hábito de escribir una entrada al día.

<b>Escribe</b>
• Agrega fotos, clima y lugar. Hasta 10 fotos por entrada.
• Marca el día con uno de 9 estados de ánimo.
• Destaca lo importante con color del texto y resaltado.
• Tu borrador se guarda automáticamente mientras escribes.

<b>Mira atrás</b>
• Ve tus entradas pasadas de un vistazo, en una lista o en un calendario.
• Encuentra cualquier entrada rápido con la búsqueda y los favoritos.
• Descubre qué estados de ánimo predominaron cada semana, mes y año.
• ¿Borraste una entrada por error? Restáurala desde la papelera en un plazo de 30 días.

<b>Copia de seguridad y bloqueo</b>
• Haz una copia de seguridad de tus entradas y fotos en tu propio Google Drive, o guárdalas en un solo archivo sin Drive.
• Bloquea la app con un código o biometría para que solo tú puedas leerla.

<b>Fuentes y pantalla</b>
• Elige entre 10 fuentes, el tamaño del texto y el modo oscuro.
• Ve tu lista como feed, tarjetas o cuadrícula de fotos.
• En tablets, la lista y la entrada se muestran lado a lado.

<b>Plus (suscripción anual)</b>
• Escribe sin anuncios.
• Agrega hasta 30 fotos por entrada.
• Usa listas de tareas y estadísticas avanzadas.
• ¿Solo quieres quitar los anuncios? También hay una compra única.

Contacto: sjunh812@naver.com
```

App Store 설명 (1684자)

```text
Un diario con solo lo que necesitas, para que escribir sea fácil incluso en los días ocupados.
Crea el hábito de escribir una entrada al día.

Escribe
• Agrega fotos, clima y lugar. Hasta 10 fotos por entrada.
• Marca el día con uno de 9 estados de ánimo.
• Destaca lo importante con color del texto y resaltado.
• Tu borrador se guarda automáticamente mientras escribes.

Mira atrás
• Ve tus entradas pasadas de un vistazo, en una lista o en un calendario.
• Encuentra cualquier entrada rápido con la búsqueda y los favoritos.
• Descubre qué estados de ánimo predominaron cada semana, mes y año.
• ¿Borraste una entrada por error? Restáurala desde la papelera en un plazo de 30 días.

Copia de seguridad y bloqueo
• Haz una copia de seguridad de tus entradas y fotos en tu propio Google Drive, o guárdalas en un solo archivo sin Drive.
• Bloquea la app con un código o biometría para que solo tú puedas leerla.

Fuentes y pantalla
• Elige entre 10 fuentes, el tamaño del texto y el modo oscuro.
• Ve tu lista como feed, tarjetas o cuadrícula de fotos.
• En iPad, la lista y la entrada se muestran lado a lado.

Plus (suscripción anual)
• Escribe sin anuncios.
• Agrega hasta 30 fotos por entrada.
• Usa listas de tareas y estadísticas avanzadas.
• ¿Solo quieres quitar los anuncios? También hay una compra única.

Contacto: sjunh812@naver.com

Acerca de Plus
• Se renueva automáticamente cada año, salvo que la canceles al menos 24 horas antes de la fecha de renovación.
• Adminístrala o cancélala en Configuración > Cuenta de Apple > Suscripciones.

Términos de uso: https://sjunh812.pages.dev/1day1diary/terms/
Política de privacidad: https://sjunh812.pages.dev/1day1diary/privacy/
```

### 브라질 포르투갈어(pt-BR, você)

- Play 이름: 1 Day 1 Diary - Diário simples (30자)
- Play 간단한 설명: A entrada de hoje em uma linha. Entradas curtas viram um registro só seu. (73자)
- App Store 이름: 1Day1Diary - Diário simples (27자)
- App Store 부제: Crie o hábito de escrever (25자)
- App Store 키워드 (97바이트):

```text
pessoal,journal,humor,emoções,notas,fotos,privado,secreto,senha,calendário,gratidão,memórias
```

App Store 홍보 문구 (160자)

```text
A entrada de hoje, em uma linha.
Entradas curtas se somam e viram um registro só seu.
Suas entradas e fotos ficam no seu aparelho e no seu próprio Google Drive.
```

Play 자세한 설명 (1263자)

```text
Um diário só com o que você precisa, para escrever com facilidade até nos dias corridos.
Crie o hábito de escrever uma entrada por dia.

<b>Escreva</b>
• Adicione fotos, clima e local. Até 10 fotos por entrada.
• Marque o dia com um dos 9 humores.
• Realce o que importa com cor do texto e destaque.
• Seu rascunho é salvo automaticamente enquanto você escreve.

<b>Relembre</b>
• Veja entradas antigas num relance, em lista ou no calendário.
• Encontre qualquer entrada rápido com a busca e os favoritos.
• Veja quais humores mais apareceram em cada semana, mês e ano.
• Apagou uma entrada sem querer? Restaure da lixeira em até 30 dias.

<b>Backup e bloqueio</b>
• Faça backup das entradas e fotos no seu próprio Google Drive, ou guarde tudo em um único arquivo, sem o Drive.
• Bloqueie o app com senha ou biometria para que só você possa ler.

<b>Fontes e tela</b>
• Escolha entre 10 fontes, o tamanho do texto e o modo escuro.
• Veja a lista como feed, cartões ou grade de fotos.
• Em tablets, a lista e a entrada aparecem lado a lado.

<b>Plus (assinatura anual)</b>
• Escreva sem anúncios.
• Adicione até 30 fotos por entrada.
• Use checklists e estatísticas avançadas.
• Só quer remover os anúncios? Também há uma compra única.

Contato: sjunh812@naver.com
```

App Store 설명 (1561자)

```text
Um diário só com o que você precisa, para escrever com facilidade até nos dias corridos.
Crie o hábito de escrever uma entrada por dia.

Escreva
• Adicione fotos, clima e local. Até 10 fotos por entrada.
• Marque o dia com um dos 9 humores.
• Realce o que importa com cor do texto e destaque.
• Seu rascunho é salvo automaticamente enquanto você escreve.

Relembre
• Veja entradas antigas num relance, em lista ou no calendário.
• Encontre qualquer entrada rápido com a busca e os favoritos.
• Veja quais humores mais apareceram em cada semana, mês e ano.
• Apagou uma entrada sem querer? Restaure da lixeira em até 30 dias.

Backup e bloqueio
• Faça backup das entradas e fotos no seu próprio Google Drive, ou guarde tudo em um único arquivo, sem o Drive.
• Bloqueie o app com senha ou biometria para que só você possa ler.

Fontes e tela
• Escolha entre 10 fontes, o tamanho do texto e o modo escuro.
• Veja a lista como feed, cartões ou grade de fotos.
• No iPad, a lista e a entrada aparecem lado a lado.

Plus (assinatura anual)
• Escreva sem anúncios.
• Adicione até 30 fotos por entrada.
• Use checklists e estatísticas avançadas.
• Só quer remover os anúncios? Também há uma compra única.

Contato: sjunh812@naver.com

Sobre o Plus
• A assinatura é renovada automaticamente todo ano, a menos que seja cancelada pelo menos 24 horas antes da data de renovação.
• Gerencie ou cancele em Ajustes > Conta Apple > Assinaturas.

Termos de Uso: https://sjunh812.pages.dev/1day1diary/terms/
Política de Privacidade: https://sjunh812.pages.dev/1day1diary/privacy/
```

### 독일어(de-DE, du)

- Play 이름: 1 Day 1 Diary - Tagebuch (24자)
- Play 간단한 설명: Dein Eintrag für heute in einer Zeile. Kurze Einträge werden zu deiner Chronik. (79자)
- App Store 이름: 1Day1Diary: Einfaches Tagebuch (30자)
- App Store 부제: Jeden Tag eine Zeile schreiben (30자)
- App Store 키워드 (96바이트):

```text
journal,stimmung,gefühle,notizen,fotos,privat,geheim,sperre,backup,kalender,dankbarkeit,routine
```

App Store 홍보 문구 (160자)

```text
Dein Eintrag für heute, in einer Zeile.
Kurze Einträge werden zu deiner eigenen Chronik.
Einträge und Fotos bleiben auf deinem Gerät und in deinem Google Drive.
```

Play 자세한 설명 (1372자)

```text
Ein Tagebuch mit genau dem, was du brauchst, damit das Schreiben auch an vollen Tagen leichtfällt.
Gewöhn dir an, jeden Tag einen Eintrag zu schreiben.

<b>Schreiben</b>
• Füge Fotos, Wetter und Ort hinzu. Bis zu 10 Fotos pro Eintrag.
• Halte den Tag mit einer von 9 Stimmungen fest.
• Hebe Wichtiges mit Textfarbe und Markierung hervor.
• Dein Entwurf wird beim Schreiben automatisch gespeichert.

<b>Zurückblicken</b>
• Sieh frühere Einträge auf einen Blick, als Liste oder im Kalender.
• Finde jeden Eintrag schnell über die Suche und deine Favoriten.
• Sieh, welche Stimmungen pro Woche, Monat und Jahr am häufigsten waren.
• Aus Versehen gelöscht? Hol den Eintrag innerhalb von 30 Tagen aus dem Papierkorb zurück.

<b>Backup und Sperre</b>
• Sichere Einträge und Fotos in deinem eigenen Google Drive oder bewahre sie ohne Drive in einer einzigen Datei auf.
• Sperre die App mit Code oder Biometrie, damit nur du sie lesen kannst.

<b>Schrift und Anzeige</b>
• Wähle aus 10 Schriften, die Textgröße und den dunklen Modus.
• Zeige deine Liste als Feed, Karten oder Fotoraster an.
• Auf Tablets stehen Liste und Eintrag nebeneinander.

<b>Plus (Jahresabo)</b>
• Schreib ohne Werbung.
• Füge bis zu 30 Fotos pro Eintrag hinzu.
• Nutze Checklisten und die erweiterte Statistik.
• Nur die Werbung loswerden? Dafür gibt es auch einen Einmalkauf.

Kontakt: sjunh812@naver.com
```

App Store 설명 (1698자)

```text
Ein Tagebuch mit genau dem, was du brauchst, damit das Schreiben auch an vollen Tagen leichtfällt.
Gewöhn dir an, jeden Tag einen Eintrag zu schreiben.

Schreiben
• Füge Fotos, Wetter und Ort hinzu. Bis zu 10 Fotos pro Eintrag.
• Halte den Tag mit einer von 9 Stimmungen fest.
• Hebe Wichtiges mit Textfarbe und Markierung hervor.
• Dein Entwurf wird beim Schreiben automatisch gespeichert.

Zurückblicken
• Sieh frühere Einträge auf einen Blick, als Liste oder im Kalender.
• Finde jeden Eintrag schnell über die Suche und deine Favoriten.
• Sieh, welche Stimmungen pro Woche, Monat und Jahr am häufigsten waren.
• Aus Versehen gelöscht? Hol den Eintrag innerhalb von 30 Tagen aus dem Papierkorb zurück.

Backup und Sperre
• Sichere Einträge und Fotos in deinem eigenen Google Drive oder bewahre sie ohne Drive in einer einzigen Datei auf.
• Sperre die App mit Code oder Biometrie, damit nur du sie lesen kannst.

Schrift und Anzeige
• Wähle aus 10 Schriften, die Textgröße und den dunklen Modus.
• Zeige deine Liste als Feed, Karten oder Fotoraster an.
• Auf dem iPad stehen Liste und Eintrag nebeneinander.

Plus (Jahresabo)
• Schreib ohne Werbung.
• Füge bis zu 30 Fotos pro Eintrag hinzu.
• Nutze Checklisten und die erweiterte Statistik.
• Nur die Werbung loswerden? Dafür gibt es auch einen Einmalkauf.

Kontakt: sjunh812@naver.com

Über Plus
• Das Abo verlängert sich jedes Jahr automatisch, wenn du nicht mindestens 24 Stunden vor dem Verlängerungsdatum kündigst.
• Verwalten oder kündigen kannst du es unter Einstellungen > Apple Account > Abonnements.

Nutzungsbedingungen: https://sjunh812.pages.dev/1day1diary/terms/
Datenschutzerklärung: https://sjunh812.pages.dev/1day1diary/privacy/
```

### 프랑스어(fr-FR, vous)

- Play 이름: 1 Day 1 Diary - Journal intime (30자)
- Play 간단한 설명: L’entrée du jour en une ligne. Peu à peu, vos entrées forment votre journal. (76자)
- App Store 이름: 1Day1Diary - Journal simple (27자)
- App Store 부제: Prenez l’habitude d’écrire (26자)
- App Store 키워드 (96바이트):

```text
intime,carnet,humeur,émotions,notes,photos,privé,secret,verrou,sauvegarde,calendrier,gratitude
```

App Store 홍보 문구 (164자)

```text
L’entrée du jour, en une ligne.
Peu à peu, vos courtes entrées forment votre journal.
Vos entrées et photos restent sur votre appareil et votre propre Google Drive.
```

Play 자세한 설명 (1436자)

```text
Un journal avec l’essentiel, pour écrire facilement même les jours chargés.
Prenez l’habitude d’écrire une entrée par jour.

<b>Écrire</b>
• Ajoutez photos, météo et lieu. Jusqu’à 10 photos par entrée.
• Notez votre journée avec l’une des 9 humeurs.
• Faites ressortir ce qui compte avec la couleur du texte et le surlignage.
• Votre brouillon est enregistré automatiquement pendant que vous écrivez.

<b>Revoir</b>
• Retrouvez vos entrées passées d’un coup d’œil, en liste ou dans le calendrier.
• Trouvez vite une entrée grâce à la recherche et aux favoris.
• Voyez quelles humeurs reviennent le plus chaque semaine, mois et année.
• Une entrée supprimée par erreur ? Restaurez-la depuis la corbeille pendant 30 jours.

<b>Sauvegarde et verrouillage</b>
• Sauvegardez vos entrées et photos sur votre propre Google Drive, ou gardez-les dans un seul fichier, sans Drive.
• Verrouillez l’app avec un code ou la biométrie pour qu’elle reste rien qu’à vous.

<b>Polices et affichage</b>
• Choisissez parmi 10 polices, la taille du texte et le mode sombre.
• Affichez votre liste en fil, en cartes ou en grille de photos.
• Sur tablette, la liste et l’entrée s’affichent côte à côte.

<b>Plus (abonnement annuel)</b>
• Écrivez sans pub.
• Ajoutez jusqu’à 30 photos par entrée.
• Utilisez les listes à cocher et les statistiques avancées.
• Vous voulez seulement retirer les pubs ? Un achat unique existe aussi.

Contact : sjunh812@naver.com
```

App Store 설명 (1764자)

```text
Un journal avec l’essentiel, pour écrire facilement même les jours chargés.
Prenez l’habitude d’écrire une entrée par jour.

Écrire
• Ajoutez photos, météo et lieu. Jusqu’à 10 photos par entrée.
• Notez votre journée avec l’une des 9 humeurs.
• Faites ressortir ce qui compte avec la couleur du texte et le surlignage.
• Votre brouillon est enregistré automatiquement pendant que vous écrivez.

Revoir
• Retrouvez vos entrées passées d’un coup d’œil, en liste ou dans le calendrier.
• Trouvez vite une entrée grâce à la recherche et aux favoris.
• Voyez quelles humeurs reviennent le plus chaque semaine, mois et année.
• Une entrée supprimée par erreur ? Restaurez-la depuis la corbeille pendant 30 jours.

Sauvegarde et verrouillage
• Sauvegardez vos entrées et photos sur votre propre Google Drive, ou gardez-les dans un seul fichier, sans Drive.
• Verrouillez l’app avec un code ou la biométrie pour qu’elle reste rien qu’à vous.

Polices et affichage
• Choisissez parmi 10 polices, la taille du texte et le mode sombre.
• Affichez votre liste en fil, en cartes ou en grille de photos.
• Sur iPad, la liste et l’entrée s’affichent côte à côte.

Plus (abonnement annuel)
• Écrivez sans pub.
• Ajoutez jusqu’à 30 photos par entrée.
• Utilisez les listes à cocher et les statistiques avancées.
• Vous voulez seulement retirer les pubs ? Un achat unique existe aussi.

Contact : sjunh812@naver.com

À propos de Plus
• L’abonnement se renouvelle automatiquement chaque année, sauf résiliation au moins 24 heures avant la date de renouvellement.
• Gérez-le ou résiliez-le dans Réglages > Compte Apple > Abonnements.

Conditions d’utilisation : https://sjunh812.pages.dev/1day1diary/terms/
Politique de confidentialité : https://sjunh812.pages.dev/1day1diary/privacy/
```

### 검수 때 볼 것

- 중국어 이름 「簡單日記本」과 Play 이름은 대만에서 이미 쓰는 앱 이름과 겹치는지 App Store Connect에 넣어 봐야 안다. 거절되면 「1日1日記 - 簡單好寫的日記本」처럼 늘린다.
- 스페인어는 중남미 기준이라 iOS 설정 앱 이름을 "Configuración"으로 썼다. 스페인 iOS는 "Ajustes"다.
- 독일어 iOS의 "Apple Account"가 그대로 영어인지, 프랑스어 "Compte Apple"·포르투갈어 "Conta Apple"·중국어 「Apple 帳號」「訂閱項目」이 지금 iOS 표기와 같은지 기기에서 본다.
- 독일어 Play 이름은 30자에 맞추려고 "1 Day 1 Diary - Tagebuch"로 줄였다. App Store 이름 "1Day1Diary: Einfaches Tagebuch"는 30자 꽉 찬다.
- 프랑스어 이름 "1Day1Diary - Journal simple"은 영어 "1Day1Diary - Simple Journal"과 낱말 순서만 다르다. 다른 이름으로 받아 주는지 넣어 봐야 안다.
