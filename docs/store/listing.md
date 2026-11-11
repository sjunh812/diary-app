# 스토어 등록 문구

Google Play 2.0.0 기준이다. 스크린샷은 `docs/store/screens/`(폰 `phone-*`, 태블릿 `tablet-*`), 만드는 법은 `scripts/store/make_screens.swift`다.
없는 기능(위젯, 동기화, 스티커)과 순위·수상·할인 표현은 쓰지 않는다(Play 메타데이터 정책).

## 앱 이름

1일 1일기: 간편한 일기장

## 간단한 설명

- 한국어: 기분을 고르고 몇 줄만 쓰면 되는 일기장이에요. 사진까지 내 구글 드라이브에 백업해요.
- 영어(원어민 검수 전): A simple diary for busy days. Back up entries and photos to your Google Drive.
- 일본어(원어민 검수 전): 忙しい日でも数行で書ける日記帳。写真も自分のGoogleドライブにバックアップできます。

## 자세한 설명

바쁜 날에도 몇 줄이면 오늘 일기를 남길 수 있어요.
회원가입이 없어서 받자마자 바로 쓸 수 있어요.

[쓰기]
글과 사진, 기분, 날씨, 장소를 한 편에 담아요.
사진은 한 편에 10장까지 넣어요.
굵게, 글자색, 형광펜, 목록으로 글을 정리해요.
쓰는 동안 임시 저장돼요.

[돌아보기]
목록과 달력으로 지난 일기를 봐요.
검색과 즐겨찾기로 지난 일기를 찾아요.
기분 통계도 볼 수 있어요.

[백업과 복원]
일기와 사진을 내 구글 드라이브에 백업해요.
복원할 때는 지금 휴대폰의 일기를 지우지 않고 합쳐요.
드라이브 없이 파일 하나로 보관할 수도 있어요.

[잠금과 휴지통]
비밀번호와 생체 인증으로 잠가요.
삭제한 일기는 30일 동안 복원할 수 있어요.

[내게 맞게]
글꼴 10종, 본문 글자 크기, 다크 모드, 목록 보기 3가지를 고를 수 있어요.
태블릿에서는 목록과 일기를 나란히 봐요.

[광고와 Plus]
일기 작성·상세 화면, 잠금, 백업과 복원, 설정에는 광고가 없어요.
목록과 돌아보기에는 광고가 나와요.
일기를 저장하고 목록으로 돌아갈 때 하루 1번 전면 광고가 나올 수 있어요.
광고는 1회 구매로 없앨 수 있어요.
Plus는 연 구독이에요. 광고 제거에 더해 한 편에 사진 30장, 체크리스트, 고급 통계를 쓸 수 있어요.

[예전 버전을 쓰시던 분께]
일기, 사진, 설정을 새 앱으로 옮겨요. 원본은 수정하거나 삭제하지 않아요.

[개인정보]
일기 내용과 사진은 직접 고른 저장소 말고는 어디로도 보내지 않아요.

문의: sjunh812@naver.com

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

## 자세한 설명 · Play 입력용(한국어)

위 자세한 설명을 Play Console에 그대로 붙여 넣을 수 있게 다듬은 판이다. Play는 목록 태그(`ul`, `li`)를 받지 않아 줄머리표(•)로 쓰고, 묶음 제목만 `<b>`로 굵게 한다. 빈 줄로 묶음을 나눈다. 굵게가 Play에 어떻게 보이는지는 저장 전 미리보기로 확인한다(공식 지원 태그 목록은 찾지 못했다).

```text
바쁜 날에도 몇 줄이면 오늘 일기를 남길 수 있어요.
회원가입 없이 받자마자 바로 쓸 수 있어요.

<b>쓰기</b>
• 글과 사진, 기분, 날씨, 장소를 한 편에 담아요.
• 사진은 한 편에 10장까지 넣어요.
• 굵게, 글자색, 형광펜, 목록으로 글을 정리해요.
• 쓰는 동안 자동으로 임시 저장돼요.

<b>돌아보기</b>
• 목록과 달력으로 지난 일기를 봐요.
• 검색과 즐겨찾기로 지난 일기를 찾아요.
• 주·월·년 기분 통계를 볼 수 있어요.

<b>백업과 복원</b>
• 일기와 사진을 내 구글 드라이브에 백업해요.
• 복원할 때는 지금 휴대폰의 일기를 지우지 않고 합쳐요.
• 드라이브 없이 파일 하나로 보관할 수도 있어요.

<b>잠금과 휴지통</b>
• 비밀번호와 생체 인증으로 잠가요.
• 삭제한 일기는 30일 동안 복원할 수 있어요.

<b>내게 맞게</b>
• 글꼴 10종, 본문 글자 크기, 다크 모드를 고를 수 있어요.
• 목록은 피드, 카드, 사진 격자 중에서 골라 봐요.
• 태블릿에서는 목록과 일기를 나란히 봐요.

<b>광고와 Plus</b>
• 일기 쓰기·상세 화면, 잠금, 백업과 복원, 설정에는 광고가 없어요.
• 목록과 돌아보기에는 광고가 나와요.
• 일기를 저장하고 목록으로 돌아갈 때 하루 1번 전면 광고가 나올 수 있어요.
• 광고는 1회 구매로 없앨 수 있어요.
• Plus(연 구독)는 광고 제거에 더해 한 편에 사진 30장, 체크리스트, 고급 통계를 쓸 수 있어요.

<b>예전 버전을 쓰시던 분께</b>
일기, 사진, 설정을 새 앱으로 옮겨요. 원본은 수정하거나 삭제하지 않아요.

<b>개인정보</b>
일기 내용과 사진은 직접 고른 저장소 말고는 어디로도 보내지 않아요.

문의: sjunh812@naver.com
```

## 자세한 설명 · 영어(원어민 검수 전)

Leave today's entry in just a few lines, even on busy days.
No sign-up needed. Start writing right after you install.

[Write]
Keep words, photos, mood, weather, and place together in one entry.
Add up to 10 photos per entry.
Organize your writing with bold, text color, highlights, and lists.
Your draft is saved as you write.

[Look back]
Browse past entries in a list or on a calendar.
Find them again with search and favorites.
See your mood statistics, too.

[Backup and restore]
Back up entries and photos to your own Google Drive.
Restoring merges with the entries already on your phone instead of erasing them.
You can also keep everything in a single file without Drive.

[Lock and trash]
Lock the app with a passcode or biometrics.
Deleted entries can be restored for 30 days.

[Make it yours]
Choose from 10 fonts, body text size, dark mode, and 3 list views.
On tablets, see the list and an entry side by side.

[Ads and Plus]
There are no ads while you write or read an entry, on the lock screen, in backup and restore, or in Settings.
Ads appear in the list and Insights.
A full-screen ad may appear once a day when you save an entry and return to the list.
Remove ads with a one-time purchase.
Plus is a yearly subscription. On top of no ads, it gives you 30 photos per entry, checklists, and advanced statistics.

[If you used the previous version]
Your entries, photos, and settings move to the new app. The originals are never changed or deleted.

[Privacy]
Your entries and photos go nowhere except the storage you choose.

Contact: sjunh812@naver.com

## 자세한 설명 · 일본어(원어민 검수 전)

忙しい日でも、数行で今日の日記を残せます。
会員登録は不要。インストールしてすぐに書けます。

【書く】
文章、写真、気分、天気、場所を1つの日記にまとめられます。
写真は1つの日記に10枚まで追加できます。
太字、文字色、マーカー、リストで文章を整えられます。
書いている間は自動で下書き保存されます。

【ふりかえる】
一覧とカレンダーで過去の日記を見られます。
検索とお気に入りで日記をすぐに探せます。
気分の統計も見られます。

【バックアップと復元】
日記と写真を自分のGoogleドライブにバックアップできます。
復元するときは、今のスマートフォンの日記を消さずにまとめます。
ドライブを使わずに1つのファイルで保管することもできます。

【ロックとゴミ箱】
パスコードと生体認証でロックできます。
削除した日記は30日間復元できます。

【自分好みに】
フォント10種類、本文の文字サイズ、ダークモード、3種類の一覧表示を選べます。
タブレットでは一覧と日記を並べて表示します。

【広告とPlus】
日記を書く画面と読む画面、ロック、バックアップと復元、設定には広告がありません。
一覧とふりかえりには広告が表示されます。
日記を保存して一覧に戻るとき、1日1回全画面広告が表示されることがあります。
広告は1回の購入で消せます。
Plusは年間サブスクリプションです。広告なしに加えて、1つの日記に写真30枚、チェックリスト、詳しい統計を使えます。

【以前のバージョンをお使いの方へ】
日記、写真、設定を新しいアプリに移行します。元のデータは変更も削除もしません。

【プライバシー】
日記の内容と写真は、ご自身で選んだ保存先以外には送信しません。

お問い合わせ: sjunh812@naver.com

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
