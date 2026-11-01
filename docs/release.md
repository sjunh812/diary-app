# 스토어 출시 준비

Play는 기존 앱 업데이트, App Store는 새 출시다. 조사일 2026-10-31. 규격과 정책은 바뀌니 제출 직전에 출처를 다시 본다.

## 업데이트가 이어지려면 (Play)

- `applicationId = "org.sjhstudio.diary"`를 `composeApp`에 **명시**한다. 안 쓰면 namespace를 따라가 다른 앱이 된다([configure-app-module](https://developer.android.com/build/configure-app-module)).
- 같은 앱 서명 키, `versionCode` 36 이상. 직접 관리하는 앱 서명 키는 잃으면 다시 만들 수 없다([앱 서명](https://support.google.com/googleplay/android-developer/answer/9842756?hl=en)).
- targetSdk 36 이상(2026-08-31부터 신규·업데이트 앱)([target-sdk](https://developer.android.com/google/play/requirements/target-sdk)).
- Play 결제 라이브러리 8 이상.
- 단계적 출시와 이전 실패 시 멈춤은 `docs/migration.md` 출시 순서를 따른다.

## 스크린샷과 그래픽

| 스토어 | 무엇을 | 규격 | 출처 |
| --- | --- | --- | --- |
| App Store | iPhone(Dynamic Island, medium) 필수 | 1179×2556 또는 1206×2622, 1~10장, 알파 채널 없음 | [규격](https://developer.apple.com/help/app-store-connect/reference/app-information/screenshot-specifications) |
| App Store | iPhone large | 1320×2868 등. medium과 함께 올리면 어느 기준이든 맞는다 | 같음 |
| App Store | iPad 13" (iPad 지원 시 필수) | 2064×2752 또는 2048×2732 | 같음 |
| App Store | 미리보기 영상(선택) | 15~30초, iPhone 886×1920, iPad 1200×1600 | [영상](https://developer.apple.com/help/app-store-connect/reference/app-information/app-preview-specifications) |
| Play | 아이콘, 피처 그래픽 | 512×512 PNG, 1024×500 | [Play 에셋](https://support.google.com/googleplay/android-developer/answer/9866151?hl=en) |
| Play | 폰 스크린샷 | 최소 2장, 추천 노출 자격은 1080px 이상 4장 이상(9:16) | 같음 |
| Play | 7"·10" 태블릿 | 각 최소 4장 추가 가능, 1,080~7,680px | 같음 |

만드는 법: 스크린샷은 실제 앱 화면(에뮬레이터·시뮬레이터 캡처)에 짧은 머리말을 얹은 세로 틀로 만든다. 틀은 Figma "화면" 페이지 아래 "스토어"
섹션에 컴포넌트로 두고 폰·태블릿·iPad 크기 변형을 같이 둔다. 순서 초안:

1. 오늘을 한 장에(쓰기 화면)
2. 사진까지 안전하게 백업(백업 상태)
3. 기분 달력과 통계
4. 내 취향대로 꾸미기(종이, 글꼴, 스티커)
5. 태블릿에서 넓게(목록·상세 두 칸)
6. 위젯
7. 잠금으로 나만 보는 일기

## 소개 문구

- 짧은 설명(Play 80자, App Store 부제 30자)과 긴 설명을 새로 쓴다. 광고 없이 무료로 쓰던 이미지를 살리고, 사진 포함 백업과 태블릿·iPad를 앞에 둔다.
- 긴 설명 구성: 한 줄 소개 → 이런 분께 → 주요 기능(무료) → Plus → 백업과 개인정보 → 문의.
- 문구는 `korean-humanizer`로 번역투를 걷어 내고 `korean-spell-check`로 맞춤법을 본다.
- 리뉴얼 첫 업데이트의 "새로운 기능" 란에는 "리뉴얼 전 기능은 계속 무료"와 "기존 일기와 사진은 자동으로 옮겨져요"를 먼저 쓴다.

## 개인정보

### 처리방침

모든 앱이 스토어와 앱 안 양쪽에 공개 URL로 둬야 한다(PDF 불가)([Play](https://support.google.com/googleplay/android-developer/answer/10144311?hl=en), App Store 5.1.1(i)).
호스팅은 무료인 GitHub Pages(이 저장소 `docs/` 밖의 별도 폴더) 같은 곳에 둔다.

개인정보보호법 제30조와 시행령 제31조의 필수 항목을 넣는다: 처리 목적, 항목, 보유 기간, 제3자 제공, 위탁, 국외 이전(Firebase·AdMob을 쓰면),
파기, 정보주체 권리와 행사 방법, 안전성 확보 조치, 보호책임자 연락처, 자동 수집 장치와 거부 방법([제30조](https://www.law.go.kr/법령/개인정보보호법/제30조),
[시행령 제31조](https://www.law.go.kr/법령/개인정보보호법시행령/제31조)). 작성지침은 2026.4. 개정판을 따르고, 일기·사진이 기기와 사용자 본인
드라이브에만 있다는 사실과 지우는 법을 쓴다([개인정보보호위원회](https://pipc.go.kr/np/cop/bbs/selectBoardArticle.do?bbsId=BS217&mCode=D010030000&nttId=12018)).
초안은 `korean-privacy-terms` 스킬로 시작한다.

### 스토어 신고

| 항목 | Play 데이터 보안 | App Store 개인정보 라벨 | 근거 |
| --- | --- | --- | --- |
| 일기, 사진, 기분, 주소 | 수집 아님(기기와 사용자 드라이브) | 수집 아님 | 기기 밖으로 나가 개발자가 접근할 수 있어야 "수집"이다([Apple](https://developer.apple.com/app-store/app-privacy-details/), [Play](https://support.google.com/googleplay/android-developer/answer/10787469?hl=en)) |
| Crashlytics | 진단(크래시 로그, 기기 정보, 설치 ID) | 크래시 데이터 | [Firebase Android](https://firebase.google.com/docs/android/play-data-disclosure), [iOS](https://firebase.google.com/docs/ios/app-store-data-collection) |
| Analytics | 앱 활동, 기기 ID | 사용 데이터 | 제출 전 Google Analytics 공개 문서로 확인 |
| AdMob(광고를 넣을 때만) | 대략 위치(IP), 상호작용, 진단, 광고 ID | 기기 ID, 광고 데이터 등 + ATT, `SKAdNetworkItems` | [AdMob Android](https://developers.google.com/admob/android/privacy/play-data-disclosure), [iOS](https://developers.google.com/admob/ios/privacy/data-disclosure) |
| 날씨·주소 API | 좌표를 보내는 방식이면 위치 항목 검토 | 같음 | 아래 위치정보법 |

- iOS `PrivacyInfo.xcprivacy`에 추적 여부, 수집 데이터, Required Reason API(UserDefaults, 파일 시각 등)를 적는다. Firebase Core·Crashlytics·GoogleSignIn은 서명된 매니페스트가 필요한 SDK 목록에 있다([Apple](https://developer.apple.com/support/third-party-SDK-requirements/)).
- Sign in with Apple(4.8): 구글 로그인으로 사용자 계정을 만들지 않고 드라이브 권한만 받으면 문언상 해당하지 않는다(해석). 심사 메모에 "계정 없음, 백업 권한용"이라고 적는다.
- 계정 삭제(5.1.1(v)): 계정을 만들지 않으니 해당하지 않는다. 대신 설정에 "구글 드라이브 연결 해제"를 둔다.
- 결제 화면: 상품 이름, 가격, 제공 내용, 구매 복원, 약관·처리방침 링크(App Store 3.1.1, 3.1.2, Play 정기결제 정책).

### 위치정보법

기기 안에서만 위치를 쓰고 사업자의 위치정보시스템으로 보내지 않으면 위치기반서비스 신고 대상에서 빠진다([위치정보지원센터](https://www.lbsc.kr/front/content/contentViewer.do?contentId=CONTENT_0000081)).
좌표를 제3자 역지오코딩·날씨 API로 보내는 것이 여기에 해당하는지는 확인하지 못했다. 출시 전에 위치정보지원센터(02-588-0185)에 묻고 답을 `docs/DECISIONS.md`에 남긴다.
1인 창조기업은 신고 없이 시작하고 1개월 안에 신고할 수 있다(제9조의2).

## 출시 전 확인

- [ ] 글꼴 10종 라이선스 마무리(`docs/design.md` 일기 본문 글꼴): 교보문고(교보손글씨 2019)와 온글잎(윤우체, ownglyph@voyagerx.com)에 앱 번들
      허용을 서면으로 확인하고, 안 되면 대체 글꼴(나눔손글씨 펜, Gaegu)로 바꾼다. "오픈소스와 글꼴 고지" 화면에 네이버·쿠키런 라이선스 전문과
      공공누리·강원교육 출처 표시를 넣는다.
- [ ] 지금 1.7.4에 들어 있는 777미니콩다방·777꼬마나비는 라이선스가 확인되지 않는다(같은 이름의 유료 구독 글꼴). 리뉴얼에서 빼고, 리뉴얼 전에
      먼저 뺄지 사용자가 정한다
- [ ] 기분·날씨 아이콘 새로 그리기(지금 PNG 출처 미확인)
- [ ] 공개 저장소에 올라간 네이버 지도 API 키를 콘솔에서 삭제하거나 막기(`app/.../utils/ApiKey.kt`). 네이버 지도 API는 유료로 바뀌어 리뉴얼에서 쓰지 않는다
- [ ] 위치정보지원센터 문의
- [ ] 처리방침 URL을 Play Console, App Store Connect, 앱 설정에 걸기
- [ ] Play 데이터 보안, App Store 개인정보 라벨, Privacy Manifest 작성(광고를 넣으므로 AdMob 항목 포함)
- [ ] Play Console 앱 콘텐츠의 광고 포함 선언, 광고 ID 선언. iOS ATT 문구와 `SKAdNetworkItems`, UMP 동의
- [ ] Play "새로운 기능" 란과 App Store 설명에 광고와 광고 제거를 적기
- [ ] 스크린샷(폰, 7"·10" 태블릿, iPhone medium·large, iPad 13"), 피처 그래픽, 아이콘
- [ ] 이전 업그레이드 계측 테스트 통과(`docs/migration.md` 시험)
