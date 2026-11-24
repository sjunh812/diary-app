# 스토어 등록 문구

Google Play 2.0.0 기준이다. 스크린샷은 `docs/store/screens/`(폰 `phone-*`, 태블릿 `tablet-*`), 만드는 법은 `scripts/store/make_screens.swift`다.
없는 기능(위젯, 동기화, 스티커)과 순위·수상·할인 표현은 쓰지 않는다(Play 메타데이터 정책).

## 앱 이름

- 한국어: 1일 1일기 - 바쁜 현대인을 위한 간편한 일기장
- 영어: 1 Day 1 Diary - Simple Journal
- 일본어: 1日1日記 - 忙しい人のためのかんたん日記帳

1.7.4부터 쓰던 한국어 이름을 그대로 둔다. 당근·오늘의집처럼 "브랜드 - 한 줄 소개" 꼴이라 무엇을 하는 앱인지 읽히고, 기존 사용자와 검색 노출이 이어진다(2026-11-12 사용자 결정). 영어는 Play 제한 30자에 맞춰 줄였다.

## 간단한 설명

Play는 2026-11-24에 아래 간단한 설명과 자세한 설명(기본 등록정보 세 언어, 맞춤 등록정보 영어·일본어)을 검토에 보냈다. 관리형 게시가 꺼져 있어 승인되면 바로 게시된다.

- 한국어: 한 줄이면 오늘 일기 끝. 사진까지 내 구글 드라이브에 백업할 수 있어요.
- 영어: Today's entry in one line. Back up photos to your own Google Drive.
- 일본어: 1行で今日の日記は完了。写真も自分のGoogleドライブにバックアップできます。

백업은 사용자가 켜야 하므로 "백업해요"가 아니라 "백업할 수 있어요"로 쓴다. 기분은 고르지 않아도 되므로 첫머리에 기분을 앞세우지 않는다(2026-11-24, 소개 사이트와 같은 말).

## 자세한 설명

Play는 목록 태그를 받지 않아 줄머리표(•)로 쓰고 묶음 제목만 `<b>`로 굵게 한다. 광고 위치는 Play의 "광고 포함" 표시와 "새로운 기능" 란이 알려 주므로 여기서는 빼고 Plus 혜택만 짧게 둔다. 복원 합치기, 개인정보, 예전 버전 안내도 넣지 않는다(2026-11-12 사용자 검토).

### 한국어

```text
바쁜 날에도 쓰기 쉽게 꼭 필요한 것만 담은 일기장 앱이에요. 매일 한 편씩 쓰는 습관을 만들어 보세요.
회원가입은 필요 없어요.

<b>일기 쓰기</b>
• 사진, 날씨, 장소를 함께 담아요. 사진은 한 편에 10장까지예요.
• 9가지 기분으로 그날의 감정을 기록할 수 있어요.
• 굵게, 글자 색, 형광펜, 목록으로 글을 정리해요.
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
No sign-up needed.

<b>Write</b>
• Add photos, weather, and place. Up to 10 photos per entry.
• Mark the day with one of nine moods.
• Organize your writing with bold, text color, highlights, and lists.
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
会員登録は不要です。

<b>日記を書く</b>
• 写真、天気、場所を一緒に残せます。写真は1件に10枚までです。
• 9種類の気分で、その日を記録できます。
• 太字、文字色、マーカー、リストで文章を整えられます。
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
바쁜 날에도 쓰기 쉽게 꼭 필요한 것만 담은 일기장 앱이에요. 매일 한 편씩 쓰는 습관을 만들어 보세요.
회원가입은 필요 없어요.

일기 쓰기
• 사진, 날씨, 장소를 함께 담아요. 사진은 한 편에 10장까지예요.
• 9가지 기분으로 그날의 감정을 기록할 수 있어요.
• 굵게, 글자 색, 형광펜, 목록으로 글을 정리해요.
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
No sign-up needed.

Write
• Add photos, weather, and place. Up to 10 photos per entry.
• Mark the day with one of nine moods.
• Organize your writing with bold, text color, highlights, and lists.
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
会員登録は不要です。

日記を書く
• 写真、天気、場所を一緒に残せます。写真は1件に10枚までです。
• 9種類の気分で、その日を記録できます。
• 太字、文字色、マーカー、リストで文章を整えられます。
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

App Store에만 있는 칸이다. 2026-11-16 App Store Connect에 넣은 값이다. 이름과 부제의 낱말은 이미 검색에 잡히고, 키워드는 띄어쓰기 없이 쉼표로만 나눈다([Apple 검색 안내](https://developer.apple.com/app-store/search/)). 홍보 문구는 심사 없이 바꿀 수 있다.

| 칸 | 한국어 | 영어 | 일본어 |
| --- | --- | --- | --- |
| 이름(30자) | 1일 1일기 | 1Day1Diary - Everyday Diary | 1日1日記 - 毎日つける日記 |
| 부제(30자) | 바쁜 현대인을 위한 간편한 일기장 | A simple diary for busy people | 忙しい人のためのかんたん日記 |

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

"회원가입 없이"는 설명 첫머리에 있어 홍보 문구에서 뺐다. 홍보 문구와 설명 첫 줄이 화면에 이어 보여 같은 말이 두 번 나오지 않게 했다.

키워드(100자). 한국어는 이름의 "일기"와 부제의 "일기장"을 되풀이하지 않는다. 붙여 쓴 검색어(기분일기 등)는 남긴다(2026-11-16)

```text
다이어리,감정,기분,무드,하루,기록,사진,한줄,메모,잠금,비밀,백업,캘린더,감사,습관,회고,일상,마음,데일리,기분일기,감정일기,하루일기,비밀일기,사진일기
```

```text
mood,daily,log,feelings,journal,photo,private,lock,backup,calendar,gratitude,notes,memories,reflect
```

```text
ダイアリー,気分,感情,記録,写真,一行,メモ,ロック,バックアップ,カレンダー,感謝,習慣,ライフログ,振り返り,日常,デイリー
```

## App Store 헤더·검색 결과 그림

App Store 상품 페이지 맨 위 헤더(21:9, 3840×1646)와 검색 결과 카드(3:2, 3840×2560)다. 2026-11-16 App Store Connect 2.0.1에 세 언어로 넣었다.

- 헤더: 앱 아이콘의 연필과 앱 이름을 한 덩어리로 가운데에 두고, 사이는 100px(글자 높이의 절반쯤. 기울어진 연필의 지우개 끝이 글자에 가장 가까워 70px은 붙어 보였다, 2026-11-16)이고, 바탕은 가운데가 밝은 라벤더 그라데이션이다. 유명 앱(Structured, Goodnotes, Apple 예시)이 쓰는 "심볼 + 이름" 꼴이다. 기분 얼굴을 무늬나 줄로 넣은 안은 어수선해 뺐다. 눈에 보이는 연필과 글자 테두리로 가로세로 가운데를 맞춘다. `docs/store/screens/ios/{ko,en,ja}/header.png`, `swift scripts/store/make_header.swift <결과> <연필 SVG> - wordmark-<언어> <창원단감둥근체 파일>`.
- 검색 결과: 홍보 문구 첫 줄과 실제 목록 화면이다. 화면이 문구보다 무거워 테두리 기준 가운데는 오른쪽으로 쏠려 보여, 그림을 한 번 그려 무게 중심을 재고 그만큼 왼쪽으로 옮긴다(2026-11-16). Apple 안전 영역(가로 836~3004)은 넘지 않아, 문구가 긴 영어는 왼쪽 끝까지만 옮긴다. 넘치는 언어만 글자를 줄인다. `docs/store/screens/ios/{ko,en,ja}/search.png`, `make_screens.swift`의 `search` 모드.
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

시그니처 기능 다섯 가지만 보인다. 글꼴은 창원단감둥근체, 목록은 기본 피드형으로 찍는다.

아래 머리말과 `docs/store/screens/`의 그림은 2026-11-24에 바꿨다. 스토어에는 아직 예전 머리말(1번 "오늘 기분부터 / 가볍게 골라 봐요", 4번 "내 기분 흐름을 / 돌아봐요", 피처 그래픽 "바쁜 날에도 / 일기를 남겨 봐요")의 그림이 올라가 있다. 예전 그림도 틀린 말이 없어 다음에 스크린샷을 바꿀 때 함께 올린다(사용자 결정). App Store 검색 결과 그림도 같다.

1. 쓰기: 그날의 기분도 / 같이 남겨요(기분 시트를 연 화면)
2. 목록: 지난 일기가 / 한눈에 보여요
3. 달력: 한 달 기분을 / 달력으로 봐요
4. 돌아보기: 어떤 기분이 많았는지 / 돌아봐요
5. 백업: 사진까지 / 드라이브에 백업해요

태블릿(가로 한 줄): 그날의 기분도 같이 남겨요 / 목록과 일기를 나란히 봐요 / 한 달 기분을 달력으로 봐요 / 어떤 기분이 많았는지 돌아봐요

영어(원어민 검수 전): Add the day's mood too / Past entries at a glance / Your month's moods on a calendar / See which moods came up most. 태블릿 2번은 Your list and entry, side by side.
일본어(원어민 검수 전): その日の気分も一緒に残せる / 過去の日記がひと目でわかる / 1か月の気分をカレンダーで / どんな気分が多かったかふりかえる. 태블릿 2번은 一覧と日記を並べて見られます.
