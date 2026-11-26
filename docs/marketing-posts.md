# 홍보 글 초안

작성일: 2026-11-19. 올리지 않았다. 올리기 전에 사용자 확인을 받는다.
기능 설명은 `docs/store/listing.md`와 `docs/DECISIONS.md`에 있는 사실만 썼다. 순위·수상·할인·없는 기능은 쓰지 않았다.

## 규칙 확인 결과 (2026-11-19)

| 곳 | 확인한 것 | 우리 글에 주는 뜻 |
| --- | --- | --- |
| 디시 마이너 갤러리 공통 운영원칙(`gall.dcinside.com/ajax/minor_ajax/minor_guide`, 직접 읽음) | 이용제한 사유 4번이 "도배, 스팸, 상업적 홍보 및 광고 활동"이고 하위에 "홍보성 타 사이트 링크 포함 및 광고 게시물"이 있다. 8번은 갤러리 주제와 동떨어진 글. 위반하면 글 노출 제한, 이용 정지(일시·영구) | 스토어 링크를 본문에 넣으면 걸릴 수 있다. 무료+광고+구독 앱이라 상업 활동으로 볼 여지도 있다. 일기 갤러리에는 주제 문제도 겹친다. **링크는 뺐다.** 그래도 위험은 남는다 |
| 디시 일기·개발자 갤러리 공지 | 일기 갤러리 공지에는 홍보 규칙이 없다. 개발자 갤러리는 공지가 없다. 매니저가 개인 판단으로 지울 수 있다 | 개발자 갤러리에는 "앱 만들었는데 피드백 부탁" 글이 실제로 올라와 있다(11-09 그림일기 앱 글 등). 다만 그 글들이 남아 있다고 규칙에 걸리지 않는다는 뜻은 아니다 |
| 긱뉴스 Show GN(`news.hada.io/blog/show`, 검색으로 확인) | 앱이 대상이다. 직접 만든 것이어야 한다. 글 종류를 Show로 고른다. 지인에게 추천·댓글 부탁 금지, 버전마다 반복 등록 금지, 짧은 기간 재등록 제한. 완성도가 높을 필요는 없다 | **가장 안전하고 맞는 곳.** 2026-11-26 올림(아래 2-1) |
| Product Hunt(공식 도움말, 검색으로 확인) | 모바일 전용 규칙은 없다. 추천(업보트) 부탁 금지, 돈 주고 사기 금지, 업보트 대가 경품 금지. 개인 계정으로. 올리면 검토 후 공개 | 초안에 "upvote 부탁"은 없다. 피드백만 구한다 |
| Show HN | 2026-11-26에 원문을 읽었다(아래 2-2). 이전 기록: 공식 규칙 원문은 읽지 못했다. 검색된 글들은 스토어 링크를 올리는 게 흔하고, 가입·유료 장벽에는 불만이 붙었다 | 올리기 전에 `news.ycombinator.com/showhn.html`을 직접 읽는다 |
| Reddit r/journaling | 미러에서 확인: 손글씨 일기 중심이고 자기 홍보 금지. 앱 이야기는 r/digitaljournaling으로 보낸다. r/digitaljournaling은 2026-03 모더레이터 공지로 글과 댓글의 홍보를 금지한다 | **올리지 않는다.** 아래 영어 Reddit 초안은 쓰지 않는 쪽으로 정한다 |
| 클리앙·블라인드·네이버 카페·에브리타임 | 이번에도 읽지 못했다. 클리앙은 앞서 읽은 이용규칙대로 직접홍보 게시판만 가능 | 미확인이라 올리지 않는다 |

## 링크

- Google Play: https://play.google.com/store/apps/details?id=org.sjhstudio.diary
- App Store: https://apps.apple.com/kr/app/id6835116731

2026-11-19에 두 주소 모두 200으로 열리는 것을 확인했다.

## 스크린샷 (올릴 순서)

`docs/store/screens/` 한국어 폰 화면이다. 영어 글에는 `docs/store/screens/en/`을 쓴다.

1. `phone-1.png` 기분 고르기
2. `phone-3.png` 달력
3. `phone-2.png`, `phone-4.png` 중 한 장
4. iPhone 화면은 `docs/store/screens/ios/ko/iphone-1.png`

디시는 글 하나에 사진을 여러 장 올릴 수 있다. 3~4장이 적당하다.

---

## 1. 디시 개발자 갤러리 (합니다체)

이 갤러리는 공지가 없고, 만든 것을 올리고 피드백을 구하는 글이 자주 올라온다(2026-11-19 목록 확인). 그 꼴에 맞춰 썼다.

**제목**

```text
혼자 만든 일기 앱을 Compose Multiplatform으로 다시 만들었습니다. 쓴소리 부탁드립니다
```

**본문**

```text
안녕하세요. 개인 개발자입니다.

예전에 만든 일기 앱 "1일 1일기"를 Compose Multiplatform으로 다시 만들어 안드로이드 업데이트와 iOS 출시를 마쳤습니다.
바쁜 날에도 기분을 고르고 몇 줄만 쓰면 오늘 일기가 끝나는 앱입니다.

만들면서 지킨 것
- 회원가입이 없습니다. 서버도 없습니다.
- 일기와 사진은 기기에 저장됩니다. 백업은 사용자 본인의 구글 드라이브나 파일 하나로만 갑니다.
- 사진까지 백업에 들어갑니다. 복원은 기존 기록을 지우지 않고 합칩니다.
- 비밀번호와 생체 인증 잠금이 있습니다.

기술
- Kotlin Multiplatform, Compose Multiplatform (안드로이드와 iOS가 같은 화면 코드)
- Room 3 + 번들 SQLite, DataStore, Coil 3, Koin

솔직하게 말씀드릴 것
- 무료이고 광고가 있습니다. 일기 쓰기와 상세 화면에는 없고, 일기를 저장하고 목록으로 돌아올 때 하루에 한 번만 나옵니다.
- 광고 제거와 사진 30장 같은 Plus(연 구독)가 있습니다. 기본 기능은 그대로 무료입니다.

특히 이런 점을 봐 주시면 감사하겠습니다.
- 처음 열었을 때 어디서 막히는지
- 기분 고르기와 달력 화면이 직관적인지
- 백업과 복원이 믿음이 가는지

불편한 점은 댓글로 남겨 주시면 고치겠습니다. 읽어 주셔서 감사합니다.
```

**올리기 전 확인**
- 안드로이드는 업데이트이고 iOS는 새 출시라서 "다시 만들어"가 맞는지 한 번 더 읽는다.
- 기술 목록은 `CLAUDE.md` 아키텍처 절 기준이다. 실제로 쓰는 것만 남기려면 `libs.versions.toml`과 대조한다.
- 광고 문장은 `docs/DECISIONS.md` 광고 규칙과 같다.

---

## 1-2. 디시 앱 개발 마이너 갤러리 (`gall.dcinside.com/mgallery/board/lists/?id=app`)

2026-11-19 목록 확인: 제목 앞에 `[홍보]`를 단 1인 개발 앱 소개 글이 하루에도 몇 개씩 올라오고 남아 있다. 홍보 글이 이 갤러리의 주제에 맞는다.

**제목**

```text
[홍보] 기분 하나 고르고 몇 줄이면 끝나는 일기장 앱 (안드/iOS, 1인개발)
```

본문은 펨코 글(1-1)을 그대로 쓰고, 끝줄만 스토어 링크 두 개로 바꾼다.

## 1-1. 펨코 디지털 게시판 › 개발 탭 (음슴체)

규정(`fmkorea.com/9905277740`, 2026-10-22 개정, 직접 읽음): 디지털 갤러리 안의 광고 글은 종류와 상관없이 금지하고, 삭제하거나 계정을 제재한다. 바이럴로 의심되면 운영진 판단으로 영구 차단될 수 있다.
비슷한 글: 개발 탭의 「DX Manager 스토어버전을 만들었습니다」(2026-10-11)는 스토어 링크를 넣었는데도 남아 있고 댓글 반응도 좋다. 다만 그 앱은 무료 오픈소스이고, 우리는 광고와 구독이 있어 영리로 볼 여지가 더 크다. 그래서 첫 글에는 링크를 넣지 않고 앱 이름만 적는다.

**제목**

```text
기분 하나 고르고 몇 줄이면 끝나는 일기장 앱 만들었음
```

**본문** (사진: `docs/store/screens/phone-1.png`)

```text
일기 앱 깔았다가 사흘 만에 지운 적이 많아서
바쁜 날에도 부담 없이 쓸 수 있는 일기장을 혼자 만들어 봤음

[사진]

- 오늘 기분 하나 고르고 몇 줄 쓰면 끝
- 달력에서 한 달 기분이 한눈에 보임
- 회원가입 없음, 일기는 폰에 저장
- 백업은 내 구글 드라이브로 (사진까지)
- 비밀번호·생체인증 잠금

디자인은 꾸밈을 줄이고 글이 잘 읽히는 쪽으로 맞췄음
자주 누르는 버튼 자리랑 글자 크기도 여러 번 다듬었음

안드로이드·iOS 둘 다 있고 무료임
광고는 일기 저장하고 나올 때 하루 한 번만 뜨고, 광고 빼는 구독도 있는데 기본 기능은 다 무료
Kotlin + Compose Multiplatform이라 화면 코드 하나로 두 플랫폼 돌리는 중

써 보고 불편한 거 있으면 댓글로 알려 주셈
스토어에서 "1일 1일기" 검색하면 나옴
```

**올리기 전 확인**
- 탭은 "개발"로 고른다. "자유"로 올리면 광고로 보기 쉽다.
- 처음 쓰는 계정으로 올리면 바이럴로 의심받기 쉽다. 댓글이 달리면 직접 답한다.

---

## 2. 디시 일기 마이너 갤러리 (합니다체)

공지 3건(주의사항, 인사, 코멘트 안내)에 홍보 규칙은 없다. 주의사항은 혐짤 삭제와 글카스 삭제뿐이다. 다만 이 갤러리는 일기를 올리는 곳이라 앱 소개 글은 주제에서 벗어날 수 있다. 삭제될 위험이 있다. 글 수가 하루 몇 건이라 눈에 띄긴 쉽다.

권하는 방식은 **짧게, 일기 쓰는 사람에게 도움이 되는 글**로 올리는 것이다.

**제목**

```text
기분 고르고 세 줄이면 끝나는 일기 앱을 만들었습니다
```

**본문**

```text
이 갤러리에서 일기 쓰시는 분들께 도움이 될까 해서 올립니다.

바쁜 날은 일기를 쓰다가 포기하게 됩니다. 그래서 기분을 먼저 고르고, 몇 줄만 쓰면 끝나게 만들었습니다.

- 회원가입 없이 바로 씁니다.
- 사진과 날씨, 장소도 함께 남길 수 있습니다.
- 달력으로 한 달 기분을 한눈에 봅니다.
- 비밀번호와 생체 인증으로 잠글 수 있습니다.
- 일기와 사진은 내 구글 드라이브에 백업할 수 있습니다. 제 서버는 없습니다.

무료이고 광고가 있습니다. 일기 쓰기 화면에는 광고가 없습니다.

주제에서 벗어난 글이면 삭제하셔도 괜찮습니다. 쓰시다가 불편한 점이 있으면 알려 주세요.
```

---

## 2-1. 긱뉴스 Show GN

**2026-11-26에 올렸다: https://news.hada.io/topic?id=37882** (사용자가 직접 올림). 아래는 올라간 글 그대로다. 버전마다 다시 올리지 않는다. 지인에게 추천이나 댓글을 부탁하지 않는다.

- 처음에는 일반 글(id=37881)로 올라가 Show 목록에 없었다. 지우고 Show로 다시 올렸다. Show를 고르면 제목 앞에 "Show GN:"이 저절로 붙는다.
- 초안은 Show 목록의 실제 글(인사, 만든 이유, 주요 기능, 만들면서 겪은 일)을 보고 썼고, 사용자가 문장을 다듬어 올렸다. 소개 영상은 GitHub이 13MB 파일을 미리 보여 주지 않아 링크를 뺐다.
- 초안에 있던 광고·Plus 안내 문단은 올린 글에서 빠졌다. 광고를 묻는 댓글이 달리면 "일기 쓰는 화면에는 광고가 없고, 저장하고 나올 때 하루 한 번만 뜬다. 광고 제거는 Plus(연 구독)"로 답한다.
- 다음에 고칠 일이 있으면 같이 고칠 곳: "'여유' 라는게" → "'여유'라는 게", "임시 저장 되어" → "임시 저장되어", "보이는데 치중했어요" → "보이는 데 신경 썼어요", "해야했기" → "해야 했기".

**제목**

```text
Show GN: 1일 1일기 - 바쁜 날에도 쓰기 쉽게 꼭 필요한 것만 담은 일기 앱 (Android·iOS)
```

**URL 칸**: https://sjunh812.pages.dev/1day1diary/

**본문**

```text
안녕하세요!
저는 몇 년째 혼자 운영해 온 안드로이드 일기 앱을 Compose Multiplatform(CMP)으로 새로 만들어, 이번에 iOS로도 처음 출시하게 된 1인 개발자입니다.
남녀노소 상관없이 누구나 쉽게 쓸 수 있고 일기 습관을 들이기 좋은 앱이라, 직접 소개해 드리고 싶어 글을 쓰게 되었습니다.

왜 만들었나요?

AI 시대가 되면서 일은 더 바빠지고, 받아들여야 할 정보의 양도 훨씬 많아졌습니다.
그러다 보니 '여유' 라는게 점점 사라진 것 같아, 하루를 일기로 정리해 보면 어떨까 하는 생각으로 시작하게 되었습니다.
그런데 일기는 꾸준히 써야 의미가 있는데, 막상 해보면 작심삼일로 끝나기 쉬웠습니다.
그래서 누구나 부담 없이 시작하고, 질리지 않게 계속 쓸 수 있는 일기장을 직접 만들게 됐습니다.

주요 기능

- 기분만 골라도, 한 줄만 써도, 또는 사진 한 장만 올려도 그날의 일기가 작성돼요.
- 쓰다가 앱이 꺼져도 임시 저장 되어 있고, 실수로 지운 일기는 휴지통에서 복원할 수 있어요.
- 달력에 그날 그날의 기분이 쌓이고, 이번 주·이번 달·올해에 어떤 기분이 많았는지도 확인할 수 있어요.
- 회원가입이 없고, 서버도 따로 두지 않아, 일기는 나만 볼 수 있어요.
- 사진까지 내 구글 드라이브에 백업돼요. 복원할 때도 지금 쓰던 일기는 지우지 않고 합쳐줘요.
- 비밀번호나 생체인증으로 잠글 수 있고, 다크 모드도 지원해요.
- 테마 색 7가지, 글꼴 10가지 중에 골라 쓸 수도 있어요.
- 태블릿이나 폴더블처럼 넓은 화면에서는 목록과 일기를 나란히 보여줘요.

개발 포인트

- 안드로이드와 iOS의 화면과 로직을 대부분 공통으로 처리해요. 플랫폼별로 따로 둔 건 결제, 광고, 드라이브 로그인, 그리고 생체인증 정도예요.
- 최근 다양해지는 디바이스 화면들에서 일관된 디자인으로 보이는데 치중했어요. 이를 위해 화면 코드에 색 HEX 값이나 숫자 dp(px)를 직접 사용하게 되면 빌드가 깨지도록 Gradle 검사를 따로 만들어 뒀어요.
- 개발하는 시간은 공유 로직을 작성하는 부분보단 에디터와 iOS 키보드 처리에서 많이 들었어요. 결국 다른 멀티플랫폼으로 개발했기 때문에 각 플랫폼에서 동일한 화면으로 보이도록 해야했기 때문이에요.
- 안드로이드의 경우, 리뉴얼 업데이트이기 때문에 예전 버전에서 쓴 일기를 하나도 잃지 않고 마이그레이션될 수 있도록 심혈을 기울였어요. 원본 DB는 끝까지 건드리지 않았고, 사본을 새 DB로 옮긴 뒤 일기마다 원본과 내용이 같은지 해시로 맞춰 보고, 하나라도 다르면 새 DB를 버리도록 했어요.

App Store: https://apps.apple.com/kr/app/id6835116731
Google Play: https://play.google.com/store/apps/details?id=org.sjhstudio.diary

꼭 한번 사용해 보시고 불편한 점이나 개선할 점, 이외 CMP 관련해서 궁금한 점 있으면 편하게 남겨 주세요.

감사합니다.
```

---

## 2-2. Show HN

아직 올리지 않았다. 올리는 사람은 사용자다. 규칙(`news.ycombinator.com/showhn.html`, 2026-11-26에 읽음): 직접 만들어 써 볼 수 있는 것이어야 하고, 소개 페이지(landing page)는 올리지 않는다. 그래서 URL 칸에는 소개 사이트 대신 App Store 주소를 넣는다. 가입 없이 써 볼 수 있어야 하고, 만든 까닭과 방법을 적는다. 지인에게 추천이나 댓글을 부탁하지 않는다.

- 광고·Plus는 먼저 밝혔다. HN은 숨긴 수익 모델에 날카롭다.
- 댓글에 답할 거리: CMP에서 iOS 키보드·한글 입력 문제, 레거시 DB를 Room 대신 날 SQL로 읽은 까닭, 서버 없이 드라이브 `drive.file` 권한만 쓰는 백업.

**제목** (80자 한도, 73자)

```text
Show HN: 1 Day 1 Diary – a diary app I rebuilt with Compose Multiplatform
```

**URL 칸**: https://apps.apple.com/app/id6835116731

**본문**

```text
Hi HN. I've been running a small Android diary app on my own since 2022. This year I rebuilt it from scratch in Kotlin with Compose Multiplatform, and it's now on iOS too.

The idea is simple: most people who start a journal stop within a few weeks, so the app asks for very little. One line, a mood, or a single photo counts as the day's entry. No account, no server. Entries and photos stay on the phone, and backups go to the user's own Google Drive.

A few things that might be interesting here:

- Nearly all UI and logic is shared between Android and iOS. The platform-specific parts are billing, ads, Google sign-in for Drive, and biometric lock.
- The hardest part was the Android migration. The old app stored everything in its own SQLite file, and losing even one entry wasn't acceptable. The new app never touches the original. It copies the database (with its WAL files), reads the copy with raw SQL into a new Room database, then compares every entry against the original by SHA-256. If any entry differs, the new database is thrown away and the migration starts over on the next launch.
- To keep the design consistent across phones, foldables and tablets, a Gradle check fails the build if a feature module uses a raw color hex or a literal dp/sp value instead of a design token.
- Surprisingly, shared logic was the easy part. Most of my time went into the rich-text editor and the iOS keyboard, including Korean input composition on hardware keyboards.
- Backup has no server behind it. The app only uses the drive.file scope, so it can see the files it created and nothing else in the user's Drive.

It's free with ads (a small card in the list and at most one full-screen ad a day, after saving an entry, never while writing). A yearly subscription removes them. The UI is in English, Korean and Japanese.

Google Play: https://play.google.com/store/apps/details?id=org.sjhstudio.diary

I'd love feedback on the app, and I'm happy to answer questions about Compose Multiplatform on iOS.
```

---

## 3. 영어 글 (Product Hunt)

Product Hunt는 바로 쓸 수 있는 제품이 대상이다. 모바일 앱 전용 규칙과 추천 부탁 규칙은 읽지 못했다. 글자 수 제한(이름 60자, 소개 260자)은 기억에 의존한 값이라 올릴 때 입력 칸에서 확인한다.

**Name**

```text
1 Day 1 Diary
```

**Tagline**

```text
Pick a mood, write a few lines, done
```

**Description**

```text
A simple diary for busy days. Pick a mood, jot a few lines, add photos. No sign-up and no server: back up entries and photos to your own Google Drive. Free on Android and iOS.
```

**Maker's first comment**

```text
Hi, I'm the solo developer behind 1 Day 1 Diary.

I kept quitting diary apps because writing felt like a chore on busy days. So I built one where you pick a mood first, then write as little as you like. A few lines is enough.

What I cared about:
- No account. No server. Your entries and photos stay on your device.
- Backup goes only to your own Google Drive (photos included), or into a single file.
- Passcode and biometric lock.
- A calendar that shows a month of moods at a glance.

It's free with ads. There are no ads on the writing and entry screens, and one interstitial at most once a day after you save. Plus (yearly) removes ads and raises the photo limit from 10 to 30 per entry. Everything else stays free.

I'd love honest feedback, especially on the first-run experience and on whether backup and restore feel trustworthy.

Android: https://play.google.com/store/apps/details?id=org.sjhstudio.diary
iOS: https://apps.apple.com/kr/app/id6835116731
```

---

## 4. 영어 글 (Reddit r/journaling 같은 곳) — 올리지 않는다

r/journaling은 자기 홍보 금지이고 앱 이야기는 r/digitaljournaling으로 보내며, 그곳도 홍보 금지다(위 규칙 표). 규칙이 허용하는 다른 서브레딧을 찾기 전까지 초안은 보관용이다.

Reddit 서브레딧마다 홍보 규칙이 다르다. 올리기 전에 해당 서브레딧 규칙을 읽지 못했으니 반드시 직접 본다. 규칙이 막으면 올리지 않는다.

**Title**

```text
I made a diary app for people who keep quitting journaling because it feels like a chore
```

**Body**

```text
I always dropped journaling after a week. Blank pages on a tired evening were the problem.

So I built a small app around one idea: pick how you feel first, then write as little as you want. A few lines counts as today's entry.

It also has photos, a month calendar of moods, search, and a passcode or biometric lock. There's no account and no server of mine. Backup goes to your own Google Drive, photos included.

It's free with ads (none on the writing screen). I'd really appreciate any feedback from people who actually journal.

Android: https://play.google.com/store/apps/details?id=org.sjhstudio.diary
iOS: https://apps.apple.com/kr/app/id6835116731
```

---

---

## 5. 일본 (일본어 글)

일본 링크는 2026-11-19에 둘 다 200으로 열렸다. App Store 일본 제목은 「1日1日記 - 毎日つける日記アプリ」다.

- App Store: https://apps.apple.com/jp/app/id6835116731
- Google Play: https://play.google.com/store/apps/details?id=org.sjhstudio.diary&hl=ja

### 규칙 확인 결과

| 곳 | 확인한 것 | 판단 |
| --- | --- | --- |
| Zenn(`zenn.dev/guideline`, 직접 읽음) | "광고 목적 글은 피해 달라. 제품 홍보가 주된 목적인 글은 안 된다. 홍보는 글 끝의 고정 문구 정도가 좋다." 체험에 바탕한 시행착오 기록을 권장 | 앱 소개 글은 안 된다. **만든 과정의 기술 글을 쓰고 끝에 링크**를 둔다 |
| Qiita(`help.qiita.com` 커뮤니티 가이드라인, 직접 읽음) | "선전이나 판매가 주된 목적인 글은 올리지 않는다." 단 자작물의 기술 해설이 주된 목적이면 선전이 아니다 | Zenn과 같다. 기술 글이면 된다 |
| X(트위터) | 규칙 조사 안 함. 개인 개발 후기 글들이 X 알림을 출발점으로 삼는 사례가 많다(검색 결과) | 자기 계정으로 올리는 소개는 일반적인 방법이다. 해시태그 효과는 확인 못 함 |
| Appliv(앱 소개 매체) | 무료 등록 신청이 있는지 확인 못 함. 확인된 것은 유료 광고(클릭 과금·설치 과금) | 무료 길이 있는지 사이트에서 직접 본다. 유료는 보류 |
| 5ちゃんねる, はてなブックマーク, note | 이번에 규칙을 읽지 못함 | 확인 전에는 올리지 않는다 |
| App Store 일본 | 추천 노미네이션을 세 언어로 냈다(11-16). 일본어 문구·스크린샷이 있다 | 심사와 노출은 Apple 몫. 문구는 `docs/store/listing.md` |

### X 게시글 (일본어)

```text
個人開発の日記アプリ「1日1日記」を Kotlin / Compose Multiplatform で作り直して、Android と iOS で公開しました。

気分を選んで数行書くだけ。会員登録も自前サーバーもありません。写真も含めて、バックアップは自分の Google ドライブに保存できます。

無料です(広告あり)。使ってみた感想をもらえるとうれしいです。

App Store: https://apps.apple.com/jp/app/id6835116731
Google Play: https://play.google.com/store/apps/details?id=org.sjhstudio.diary&hl=ja

#個人開発 #Kotlin #ComposeMultiplatform #日記アプリ
```

- 첨부는 일본어 스크린샷 `docs/store/screens/ja/phone-1.png`~`phone-3.png`.
- 광고 문장은 기능 위치와 같다(쓰기·상세 화면에는 없음).
- "感想をもらえるとうれしい"는 추천 부탁이 아니라 의견 요청이다.

### Zenn / Qiita 기술 글 뼈대

앱 소개가 아니라 **직접 겪은 기술 이야기**여야 한다(두 곳 규칙). 아래는 뼈대이고, 실제 겪은 시행착오와 수치는 사용자가 알려 줘야 채울 수 있다. 지어내지 않는다.

제목 후보: 「個人開発の日記アプリを Compose Multiplatform で作り直して Android と iOS に出した話」

1. 왜 다시 만들었나: View 기반 앱을 Compose Multiplatform으로 옮긴 이유
2. 기존 사용자의 기록을 지키는 이전: 기존 DB를 Room이 아니라 날 SQL로 읽은 까닭(`docs/migration.md`)
3. 사진까지 들어가는 백업과 복원: 합치기가 기본인 이유(`docs/backup.md`)
4. 서버 없이 가는 구조: 구글 드라이브 백업, 회원가입 없음
5. Android와 iOS의 차이에서 막힌 곳(결제, 광고, 생체 인증)
6. 끝: 앱 링크 두 줄(Zenn 가이드가 허용하는 "글 끝의 홍보")

소재가 되는 결정은 `docs/DECISIONS.md`에 날짜와 함께 있다. 쓰기 전에 어떤 것이 일본 개발자에게 쓸모 있을지 함께 고른다.

### 일본어 글 점검
- 일본어는 문구를 직접 다듬었다. 원어민 검수는 받지 않았다. 올리기 전에 일본어 표현을 한 번 더 본다.
- 스토어 일본어 설명(`docs/store/listing.md`)과 같은 사실만 썼다.

### 일본 추가 후보 (2026-11-19 규칙 확인)

| 곳 | 판단 | 근거 |
| --- | --- | --- |
| note (note.com) | 가능. 앱 만든 이야기나 일기 습관 글 끝에 링크 | 가이드라인·약관에 자기 앱 소개 금지 조항 없음. 'No.1'·최상급 표현 주의 |
| はてなブックマーク | 보조만. 우리 note 글을 자기 계정으로 한 번 북마크 | 광고 목적 북마크, 부계정, 북마크 부탁은 스팸 |
| iPhone Mania | 보도자료 메일 한 통 | 운영자 정보 페이지에 제보·보도자료 창구. 게재 보장 없음 |
| AppBank | 보도자료 메일 한 통 | 보도자료 메일 주소 있음. 영업 메일 금지 |
| Tsukutta (tsukutta.app) | 조건부 | 개인 개발 앱 게시 사이트. 약관상 대상이 '주로 AI로 만든 앱' |
| 5ch | 제외 | 선전 링크는 선전 게시판 밖이면 삭제 |
| Appliv | 제외 | 무료 리뷰 신청 창구 없음(옛 페이지 404) |
| オクトバ, アプリオ, Reddit 일본 | 미확인 | 사이트 오류, 신청 창구 못 찾음, 접속 막힘 |

### 일본 앱 매체 보도자료 메일

보내는 곳(2026-11-19 각 사이트에서 확인):
- iPhone Mania: `pr-iphone@red-consulting.jp` (운영자 정보 페이지. SNS로 온 연락은 받지 않음, 게재 보장 없음)
- AppBank: `pressrelease@appbank.net` (보도자료 안내 페이지. 영업 메일 금지)

첨부: 아이콘 `docs/store/app-store-1024.png`, 일본어 스크린샷 `docs/store/screens/ja/phone-1~4.png`, 일본어 대표 이미지.
받는 곳마다 따로 보낸다(한 메일에 둘을 넣지 않는다). 첫 줄의 매체 이름만 바꾼다.

**제목**

```text
【アプリ紹介】気分を選んで数行書くだけの日記アプリ「1日1日記」iPhone版公開・Android版リニューアル
```

**본문**

```text
iPhone Mania 編集部 御中

はじめまして。韓国で個人でアプリを開発している Seo Junhyung(ソ・ジュンヒョン)と申します。
日記アプリ「1日1日記」のiPhone版を公開し、Android版も作り直しましたので、ご紹介させていただきます。

■ アプリの概要
気分を選んで数行書くだけで、今日の日記が完了する日記アプリです。
忙しい日でも続けやすいよう、シンプルで読みやすく、書きやすい画面を目指しました。

■ 主な機能
・9つの気分から1つ選んで記録
・1つの日記に写真を最大10枚(Plusは30枚)
・1か月の気分をカレンダーで表示、気分の流れをふりかえり
・パスコードと生体認証でロック
・会員登録は不要。日記と写真は端末に保存され、バックアップは自分のGoogleドライブへ(写真も含む)
・日本語、韓国語、英語に対応

■ 料金
基本無料(広告あり。日記を書く画面と読む画面には広告は表示されません)
広告の非表示などは「Plus」(年額)でご利用いただけます。

■ 対応環境
iPhone・iPad:iOS 17.0以降
Android:Android 7.0以降

■ ダウンロード
App Store:https://apps.apple.com/jp/app/id6835116731
Google Play:https://play.google.com/store/apps/details?id=org.sjhstudio.diary&hl=ja

■ 画像素材
アイコン、スクリーンショット4枚、紹介画像を添付しております。

ご多忙のところ恐れ入りますが、ご検討いただけましたら幸いです。

Seo Junhyung(ソ・ジュンヒョン)
メール:[보내는 메일 주소]
```

### note 글 (일본 일반 사용자용)

note 가이드라인상 자기 앱 소개 글은 막지 않는다. "No.1" 같은 최상급 표현은 쓰지 않는다. 사진은 일본어 대표 이미지를 맨 위에, 스크린샷은 본문 중간에.

**제목**

```text
忙しい日でも続けられる日記アプリを、韓国からひとりで作りました
```

**본문**

```text
はじめまして。韓国で個人でアプリを作っている Seo Junhyung(ソ・ジュンヒョン)です。
「1日1日記」という日記アプリを、iPhoneとAndroidで公開しました。

■ 白紙を前に手が止まる日のために
日記を書こうとしても、白い画面を前に何を書けばいいか迷って、そのまま閉じてしまう日がありました。
そこで「1日1日記」では、まず今日の気分を選びます。あとは数行書けば、今日の日記はおしまいです。

(スクリーンショット:気分を選ぶ画面)

■ シンプルで、読みやすく、書きやすく
いちばん気をつけたのは、使っていて迷わないことです。
画面はできるだけシンプルにして、文字は読みやすく、書く流れは止まらないようにしました。

(スクリーンショット:カレンダー画面)

選んだ気分はカレンダーに残るので、1か月の気分がひと目でわかります。
気分の流れをふりかえる画面もあります。

■ 日記は自分のものに
会員登録はありません。日記と写真は端末に保存されます。
バックアップは自分のGoogleドライブに、写真ごと保存できます。
パスコードや生体認証でロックもかけられます。

■ 料金について
基本無料で、広告があります。ただ、日記を書く画面と読む画面には広告を出していません。
広告を消したい方や、1つの日記に写真をたくさん残したい方向けに「Plus」(年額)も用意しています。

■ ダウンロード
App Store:https://apps.apple.com/jp/app/id6835116731
Google Play:https://play.google.com/store/apps/details?id=org.sjhstudio.diary&hl=ja

日本語はまだ勉強中なので、変な表現があったらごめんなさい。
使ってみた感想や「ここが使いにくい」という声をいただけると、とてもうれしいです。
```

- 원어민 검수 전이다. 끝에서 두 번째 문단으로 미리 밝혀 둔다.
- 해시태그(note 태그): #日記 #日記アプリ #個人開発 #アプリ紹介

---

## 점검 기록

- 사실 확인: 기능, 광고 위치, 사진 한도(무료 10장, Plus 30장), 서버 없음, 구글 드라이브 백업은 `docs/store/listing.md`, `CLAUDE.md`, `docs/DECISIONS.md`와 같다.
- 한국어는 합니다체로 썼고 의존 명사와 보조 용언 띄어쓰기를 직접 봤다. `korean-spell-check`(나라 맞춤법 검사기)는 이 문서에서는 돌리지 않았다.
- 못 본 것: 디시 일기·개발자 갤러리의 홍보 글 삭제 기준(공지에 없음), Product Hunt와 Reddit의 규칙, Show GN·Show HN의 모바일 앱 허용 여부.
