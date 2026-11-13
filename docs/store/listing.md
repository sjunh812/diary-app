# 스토어 등록 문구

Google Play 2.0.0 기준이다. 스크린샷은 `docs/store/screens/`(폰 `phone-*`, 태블릿 `tablet-*`), 만드는 법은 `scripts/store/make_screens.swift`다.
없는 기능(위젯, 동기화, 스티커)과 순위·수상·할인 표현은 쓰지 않는다(Play 메타데이터 정책).

## 앱 이름

- 한국어: 1일 1일기 - 바쁜 현대인을 위한 간편한 일기장
- 영어: 1 Day 1 Diary - Simple Journal
- 일본어: 1日1日記 - 忙しい人のためのかんたん日記帳

1.7.4부터 쓰던 한국어 이름을 그대로 둔다. 당근·오늘의집처럼 "브랜드 - 한 줄 소개" 꼴이라 무엇을 하는 앱인지 읽히고, 기존 사용자와 검색 노출이 이어진다(2026-11-12 사용자 결정). 영어는 Play 제한 30자에 맞춰 줄였다.

## 간단한 설명

- 한국어: 기분을 고르고 몇 줄만 쓰면 오늘 일기 끝. 사진까지 내 구글 드라이브에 백업할 수 있어요.
- 영어: Pick a mood, jot a few lines, done. Back up photos to your own Google Drive.
- 일본어: 気分を選んで数行書けば今日の日記は完了。写真も自分のGoogleドライブにバックアップできます。

백업은 사용자가 켜야 하므로 "백업해요"가 아니라 "백업할 수 있어요"로 쓴다.

## 자세한 설명

Play는 목록 태그를 받지 않아 줄머리표(•)로 쓰고 묶음 제목만 `<b>`로 굵게 한다. 광고 위치는 Play의 "광고 포함" 표시와 "새로운 기능" 란이 알려 주므로 여기서는 빼고 Plus 혜택만 짧게 둔다. 복원 합치기, 개인정보, 예전 버전 안내도 넣지 않는다(2026-11-12 사용자 검토).

### 한국어

```text
바쁜 날에도 몇 줄이면 오늘 일기를 남길 수 있어요.
회원가입은 필요 없어요.

<b>일기 쓰기</b>
• 오늘 기분을 9가지 중에서 골라 남겨요.
• 사진, 날씨, 장소도 함께 담아요. 사진은 한 편에 10장까지예요.
• 굵게, 글자 색, 형광펜, 목록으로 글을 정리해요.
• 쓰는 동안 자동으로 임시 저장돼요.

<b>지난 일기 보기</b>
• 목록과 달력으로 지난 일기를 한눈에 봐요.
• 검색과 즐겨찾기로 원하는 일기를 바로 찾아요.
• 주·월·년 통계로 내 기분 흐름을 살펴봐요.
• 실수로 삭제한 일기도 30일 동안 휴지통에서 복원할 수 있어요.

<b>백업과 잠금</b>
• 일기와 사진을 내 구글 드라이브에 백업해요. 드라이브 없이 파일 하나로 보관할 수도 있어요.
• 비밀번호와 생체 인증으로 나만 볼 수 있게 잠가요.

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
Leave today's entry in just a few lines, even on busy days.
No sign-up needed.

<b>Write</b>
• Pick today's mood from nine options.
• Add photos, weather, and place too. Up to 10 photos per entry.
• Organize your writing with bold, text color, highlights, and lists.
• Your draft is saved automatically as you write.

<b>Look back</b>
• See past entries at a glance in a list or on a calendar.
• Find any entry quickly with search and favorites.
• Follow how your mood changes with weekly, monthly, and yearly stats.
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
忙しい日でも、数行で今日の日記を残せます。
会員登録は不要です。

<b>日記を書く</b>
• 今日の気分を9種類の中から選んで残せます。
• 写真、天気、場所も一緒に残せます。写真は1件に10枚までです。
• 太字、文字色、マーカー、リストで文章を整えられます。
• 書いている間は自動で下書き保存されます。

<b>過去の日記を見る</b>
• 一覧とカレンダーで過去の日記をひと目で見られます。
• 検索とお気に入りで、読みたい日記をすぐに探せます。
• 週・月・年の統計で、気分の流れをふりかえれます。
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
바쁜 날에도 몇 줄이면 오늘 일기를 남길 수 있어요.
회원가입은 필요 없어요.

일기 쓰기
• 오늘 기분을 9가지 중에서 골라 남겨요.
• 사진, 날씨, 장소도 함께 담아요. 사진은 한 편에 10장까지예요.
• 굵게, 글자 색, 형광펜, 목록으로 글을 정리해요.
• 쓰는 동안 자동으로 임시 저장돼요.

지난 일기 보기
• 목록과 달력으로 지난 일기를 한눈에 봐요.
• 검색과 즐겨찾기로 원하는 일기를 바로 찾아요.
• 주·월·년 통계로 내 기분 흐름을 살펴봐요.
• 실수로 삭제한 일기도 30일 동안 휴지통에서 복원할 수 있어요.

백업과 잠금
• 일기와 사진을 내 구글 드라이브에 백업해요. 드라이브 없이 파일 하나로 보관할 수도 있어요.
• 비밀번호와 생체 인증으로 나만 볼 수 있게 잠가요.

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

이용약관: https://sjunh812.notion.site/411ec02c164a80d3b1f5f1b5bbe1c7c4
개인정보처리방침: https://sjunh812.notion.site/411ec02c164a805b9bfec5bb88d766a9
```

### 영어

```text
Leave today's entry in just a few lines, even on busy days.
No sign-up needed.

Write
• Pick today's mood from nine options.
• Add photos, weather, and place too. Up to 10 photos per entry.
• Organize your writing with bold, text color, highlights, and lists.
• Your draft is saved automatically as you write.

Look back
• See past entries at a glance in a list or on a calendar.
• Find any entry quickly with search and favorites.
• Follow how your mood changes with weekly, monthly, and yearly stats.
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

Terms of Use: https://sjunh812.notion.site/411ec02c164a80d3b1f5f1b5bbe1c7c4
Privacy Policy: https://sjunh812.notion.site/411ec02c164a805b9bfec5bb88d766a9
```

### 일본어

```text
忙しい日でも、数行で今日の日記を残せます。
会員登録は不要です。

日記を書く
• 今日の気分を9種類の中から選んで残せます。
• 写真、天気、場所も一緒に残せます。写真は1件に10枚までです。
• 太字、文字色、マーカー、リストで文章を整えられます。
• 書いている間は自動で下書き保存されます。

過去の日記を見る
• 一覧とカレンダーで過去の日記をひと目で見られます。
• 検索とお気に入りで、読みたい日記をすぐに探せます。
• 週・月・年の統計で、気分の流れをふりかえれます。
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

利用規約: https://sjunh812.notion.site/411ec02c164a80d3b1f5f1b5bbe1c7c4
プライバシーポリシー: https://sjunh812.notion.site/411ec02c164a805b9bfec5bb88d766a9
```

## App Store 부제·홍보 문구·키워드

App Store에만 있는 칸이다. 이름과 부제의 낱말은 이미 검색에 잡혀 키워드에 다시 넣지 않고, 키워드는 쉼표로만 나눈다(Apple 안내). 앱 이름은 위 "앱 이름"과 같다(세 언어 모두 30자 안). 홍보 문구는 심사 없이 바꿀 수 있다.

| 칸 | 한국어 | 영어 | 일본어 |
| --- | --- | --- | --- |
| 부제(30자) | 기분 고르고 몇 줄이면 오늘 일기 끝 | Pick a mood, write a few lines | 気分を選んで、数行書くだけ |

홍보 문구(170자)

```text
회원가입 없이 바로 써요. 기분을 고르고 몇 줄만 남기면 오늘 일기 끝. 일기는 휴대폰에 저장되고, 원하면 사진까지 내 구글 드라이브에 백업할 수 있어요.
```

```text
No sign-up. Pick a mood, write a few lines, and today's entry is done. Entries stay on your phone, and you can back them up, photos included, to your own Google Drive.
```

```text
会員登録なしですぐ使えます。気分を選んで数行書けば今日の日記は完了。日記はスマートフォンに保存され、写真も含めて自分のGoogleドライブにバックアップできます。
```

키워드(100자)

```text
다이어리,감정,기분,무드,하루,기록,사진,한줄,메모,잠금,비밀,백업,캘린더,감사,습관,회고,일상,마음,데일리
```

```text
mood,daily,log,feelings,tracker,photo,private,lock,backup,calendar,gratitude,notes,memories,reflect
```

```text
ダイアリー,気分,感情,記録,写真,一行,メモ,ロック,バックアップ,カレンダー,感謝,習慣,ライフログ,振り返り,日常,デイリー
```

## 맞춤 스토어 등록정보

"1일 1일기"(검색 키워드 대상). 문구와 그래픽은 기본 등록정보를 그대로 물려받는다(그룹 애셋 상속). 검색 키워드는 일기, 일기장, 다이어리, 일기 앱, 감정 일기, 기분 일기, 하루 일기, 사진 일기, 비밀 일기다(2026-11-12).

- 영어(en-US)·일본어(ja-JP)를 더했다(2026-11-13, 초안 저장). 두 언어는 그룹을 물려받지 못해 기본 등록정보와 같은 것을 직접 넣었다: 위 이름·간단한 설명·자세한 설명, 아이콘 `play-512.png`, 피처 그래픽 `feature.png`(한국어 그림, 기본 등록정보와 같음), 휴대폰 `{en,ja}-phone-1~4`, 7·10인치 태블릿 `{en,ja}-tablet-1~4`.
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

## 새로운 기능

Play는 언어마다 500자까지 받는다. 기존 사용자가 업데이트 뒤 광고를 처음 보게 되므로 광고와 광고 제거를 함께 적는다(`docs/release.md` 출시 전 확인).

### 한국어

앱을 처음부터 새롭게 만들었어요.
쓰시던 기능은 계속 무료예요.
기존 일기와 사진은 자동으로 옮겨져요.
이제 사진까지 함께 백업할 수 있어요.
목록과 돌아보기에 광고가 생겼어요. 일기 쓰기와 상세 화면에는 광고가 없어요.
광고는 1회 구매로 없앨 수 있어요.

### 영어(원어민 검수 전)

We rebuilt the app from the ground up.
Everything you used before stays free.
Your entries and photos move over automatically.
Backups now include your photos.
Ads now appear in the list and Insights. There are no ads while you write or read an entry.
You can remove ads with a one-time purchase.

### 일본어(원어민 검수 전)

アプリを一から作り直しました。
これまで使っていた機能はずっと無料です。
今までの日記と写真は自動で移行されます。
写真もまとめてバックアップできるようになりました。
一覧とふりかえりに広告が表示されるようになりました。日記を書く画面と読む画面には広告はありません。
広告は1回の購入で消せます。

## 스크린샷 머리말

시그니처 기능 다섯 가지만 보인다. 글꼴은 창원단감둥근체, 목록은 기본 피드형으로 찍는다.

1. 쓰기: 오늘 기분부터 / 가볍게 골라 봐요(기분 시트를 연 화면)
2. 목록: 지난 일기가 / 한눈에 보여요
3. 달력: 한 달 기분을 / 달력으로 봐요
4. 돌아보기: 내 기분 흐름을 / 돌아봐요
5. 백업: 사진까지 / 드라이브에 백업해요

태블릿(가로 한 줄): 오늘 기분부터 가볍게 골라 봐요 / 목록과 일기를 나란히 봐요 / 한 달 기분을 달력으로 봐요 / 내 기분 흐름을 돌아봐요

영어(원어민 검수 전): Start with today's mood / Past entries at a glance / Your month's moods on a calendar / Look back on your moods. 태블릿 2번은 Your list and entry, side by side.
일본어(원어민 검수 전): 今日の気分から気軽に選べます / 過去の日記がひと目でわかる / 1か月の気分をカレンダーで / 気分の流れをふりかえる. 태블릿 2번은 一覧と日記を並べて見られます.
