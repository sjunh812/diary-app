# 스토어 출시 준비

Play는 기존 앱 업데이트, App Store는 새 출시다. 조사일 2026-10-31. 규격과 정책은 바뀌니 제출 직전에 출처를 다시 본다.

## 업데이트가 이어지려면 (Play)

- `applicationId = "org.sjhstudio.diary"`를 `composeApp`에 **명시**한다. 안 쓰면 namespace를 따라가 다른 앱이 된다([configure-app-module](https://developer.android.com/build/configure-app-module)).
- 같은 앱 서명 키, `versionCode` 37 이상(36은 내지 않은 1.7.5가 썼다). 직접 관리하는 앱 서명 키는 잃으면 다시 만들 수 없다([앱 서명](https://support.google.com/googleplay/android-developer/answer/9842756?hl=en)).
- targetSdk 36 이상(2026-08-31부터 신규·업데이트 앱)([target-sdk](https://developer.android.com/google/play/requirements/target-sdk)).
- Play 결제 라이브러리 8 이상.
- 단계적 출시와 이전 실패 시 멈춤은 `docs/migration.md` 출시 순서를 따른다.

## CD 비밀값

`v*` 태그를 올리면 `.github/workflows/release.yml`이 돈다. 태그와 앱 버전이 다르면 멈춘다(`scripts/release/check-version.sh`). 변경 기록은 `feat`·`fix` 커밋 제목에서 뽑아 GitHub Release 초안으로 남긴다(`scripts/release/changelog.sh`).
비밀값이 없으면 그 단계만 건너뛰고 경고를 남긴다. 값은 Settings > Environments > `release`의 Environment secrets에 넣는다.
넣은 값이 맞는지는 Actions > Release > Run workflow(develop)로 미리 본다. 태그 없이 돌리면 서명된 AAB를 만들어 검증만 하고 Play 업로드와 릴리스 초안은 건너뛴다. 비밀번호나 별칭이 틀리면 "AAB 만들기" 단계가 실패한다.
Android 값은 `sh scripts/release/set-secrets.sh <업로드 키 .jks> [Play 서비스 계정 .json]`으로 한 번에 넣는다. 광고 ID와 RevenueCat 키는 `local.properties`에서 읽고, 비밀번호는 실행 중에 묻는다. 값은 화면에 나오지 않는다. 사람이 직접 돌린다. 여기에 사용자 본인을 Required reviewers로 걸면 태그마다 승인 뒤에 돈다.

| 비밀값 | 무엇 | 없으면 |
| --- | --- | --- |
| `ANDROID_KEYSTORE_BASE64` | 업로드 키 파일을 `base64 -i upload.jks`로 바꾼 값 | AAB를 건너뛴다 |
| `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD` | 키 파일 비밀번호, 별칭, 키 비밀번호 | AAB를 건너뛴다 |
| `PLAY_SERVICE_ACCOUNT_JSON` | Play Console에서 권한을 준 서비스 계정 키(JSON 원문) | 내부 트랙 업로드를 건너뛴다 |
| `GOOGLE_SERVICES_JSON_BASE64` | `composeApp/google-services.json`의 base64 | 빌드를 멈춘다. 비정상 종료 보고, 이전 실패 통계, 전면 광고 원격 스위치가 빠진 앱은 내보내지 않는다(2026-11-13) |
| `ADMOB_APP_ID`, `ADMOB_NATIVE`, `ADMOB_INTERSTITIAL`, `ADMOB_REWARDED` | 위 "광고와 결제 키"의 값 | 광고가 나오지 않는 빌드 |
| `IOS_CERTIFICATE_P12_BASE64`, `IOS_CERTIFICATE_PASSWORD` | Apple Distribution 인증서(.p12)의 base64와 비밀번호 | 아카이브를 건너뛴다 |
| `IOS_PROVISIONING_PROFILE_BASE64` | App Store 배포 프로필의 base64 | 아카이브를 건너뛴다 |
| `IOS_TEAM_ID` | Apple 개발자 팀 ID | 아카이브를 건너뛴다 |
| `GOOGLE_SERVICE_INFO_PLIST_BASE64` | `iosApp/iosApp/GoogleService-Info.plist`의 base64 | iOS 빌드를 멈춘다. 비정상 종료 보고와 전면 광고 원격 스위치가 빠진 앱은 내보내지 않는다(2026-11-13) |
| `IOS_LOCAL_XCCONFIG_BASE64` | `iosApp/iosApp/Local.xcconfig`의 base64. 구글 클라이언트 ID, 광고 ID, RevenueCat Apple 키가 다 들어 있어야 한다 | iOS 빌드를 멈춘다. 값이 하나라도 비어도 멈춘다(`scripts/release/check-ios-config.sh`) |

- Play 업로드는 내부 트랙만 간다. 워크플로에 프로덕션으로 가는 길이 없고 승격은 Play Console에서 사람이 한다.
- Play 앱 서명을 쓰므로 CI에는 업로드 키만 둔다. 앱 서명 키는 CI에 넣지 않는다.
- 서비스 계정은 이 앱 하나에 "내부 테스트 트랙에 출시" 권한만 준다.
- iOS는 IPA까지 만들어 산출물로 남기고 App Store Connect에 올리지 않는다.
- 공개 저장소의 Actions 산출물은 로그인한 누구나 받을 수 있다. AAB와 IPA는 사흘만 두고, 난독화 매핑은 산출물에 담지 않는다.
- 첫 태그 전에는 iOS 서명 경로를 시험하지 못했다. 처음 돌릴 때 `release` 작업 로그를 끝까지 본다.

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

만드는 법: 스크린샷은 실제 앱 화면(에뮬레이터·시뮬레이터 캡처)에 짧은 머리말을 얹은 세로 틀로 만든다. 한국어, 영어, 일본어 세 벌을 만들고
앱 화면도 그 언어로 찍는다. 틀은 Figma "화면" 페이지 아래 "스토어"
섹션에 컴포넌트로 두고 폰·태블릿·iPad 크기 변형을 같이 둔다. 순서 초안:

1. 쓰기(기분, 사진, 서식)
2. 일기 목록(기본 피드형)
3. 달력
4. 돌아보기(기분 통계)
5. 백업

시그니처 기능 다섯 가지만 보인다(2026-11-07 사용자 결정). 글꼴은 창원단감둥근체, 상태 표시줄은 잘라 낸다.

폰 5장과 피처 그래픽은 `docs/store/screens/`에 있고 문구는 `docs/store/listing.md`에 있다(2026-11-07).
태블릿 4장(`tablet-1`~`4`, 2560×1600 가로)은 Play 7"·10" 칸에 같이 쓴다. 쓰기, 목록과 일기 나란히, 달력, 돌아보기 순서이고, 백업 화면은 드라이브 연결이 필요해 뺐다(2026-11-11).
App Store용은 `docs/store/screens/ios/{ko,en,ja}/`에 iPhone 4장(1206×2622, 6.3" 필수 규격)과 iPad 13" 가로 4장(2752×2064)씩 있다(2026-11-13). 쓰기, 목록, 달력, 돌아보기 순서이고 백업 화면은 드라이브 연결이 필요해 뺐다. 원본 캡처는 `docs/store/source/ios/`, 만드는 법은 `scripts/store/seed_ios.py`(견본 일기 30편, 언어마다 새로 씀)와 `make_photos.swift`(직접 그린 풍경 그림)로 시뮬레이터에 넣고 찍은 뒤 `make_screens.swift ... <언어> ios`다. 한국어·영어는 창원단감둥근체, 일본어는 시스템 글꼴, Plus 상태(개발 빌드 실행 인자 `-diaryPlan PLUS`)로 찍었다.
원본 캡처는 `docs/store/source/tablet/`에 있다. Pixel Tablet 에뮬레이터에 견본 일기 48편과 직접 그린 풍경 사진 6장을 넣고 창원단감둥근체로 찍었다.
영어·일본어는 `docs/store/screens/en/`, `ja/`에 폰 4장과 태블릿 4장씩 있다. 일기 내용도 그 언어 견본으로 바꿔 찍었고, 백업 화면과 피처 그래픽은 없다. 일본어는 D13 추천안대로 시스템 글꼴로 찍었다. 머리말은 원어민 검수 전이다(2026-11-11).
기준은 심플, 미감, 가독성이다. 앱 전용 색(`selected`) 바탕 하나, 큰 제목, 곧게 놓은 실제 화면만 둔다. 강조는 낱말 하나다. 도형, 스티커, 기울인 카드는 넣지 않는다(2026-11-07 사용자 지적).

## 소개 문구

- 짧은 설명(Play 80자, App Store 부제 30자)과 긴 설명은 `docs/store/listing.md`에 있다. 사진 포함 백업과 회원가입 없음을 앞에 둔다.
- 긴 설명 구성: 한 줄 소개 → 이런 분께 → 주요 기능(무료) → Plus → 백업과 개인정보 → 문의.
- 문구는 `korean-humanizer`로 번역투를 걷어 내고 `korean-spell-check`로 맞춤법을 본다.
- 영어와 일본어 설명은 한국어를 옮기지 않고 그 언어로 새로 쓴 뒤 원어민 검수를 받는다. 앱 이름은 언어마다 따로 쓸 수 있다(열린 결정 D12).
  스토어 화면에 나라별 법적 표시가 필요한지는 아래 "출시 국가"에 적는다.
- 리뉴얼 첫 업데이트의 "새로운 기능" 란에는 "리뉴얼 전 기능은 계속 무료"와 "기존 일기와 사진은 자동으로 옮겨져요"를 먼저 쓴다.

## 개인정보

### 처리방침

모든 앱이 스토어와 앱 안 양쪽에 공개 URL로 둬야 한다(PDF 불가)([Play](https://support.google.com/googleplay/android-developer/answer/10144311?hl=en), App Store 5.1.1(i)).
호스팅은 무료인 GitHub Pages(이 저장소 `docs/` 밖의 별도 폴더) 같은 곳에 둔다.
한국어, 영어, 일본어 세 벌을 같은 내용으로 두고 앱은 화면 언어에 맞는 주소를 연다. 한국어판이 원본이고 어긋나면 한국어판을 따른다고 적는다.

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
| 현재 위치 | 수집 아님으로 본다. 앱은 좌표를 저장하거나 밖으로 보내지 않고 운영체제의 주소 변환만 부른다. 제출 전에 다시 확인한다 | 같음 | 아래 위치정보법 |

- iOS `PrivacyInfo.xcprivacy`에 추적 여부, 수집 데이터, Required Reason API(UserDefaults, 파일 시각 등)를 적는다. Firebase Core·Crashlytics·GoogleSignIn은 서명된 매니페스트가 필요한 SDK 목록에 있다([Apple](https://developer.apple.com/support/third-party-SDK-requirements/)).
- Sign in with Apple(4.8): 구글 로그인으로 사용자 계정을 만들지 않고 드라이브 권한만 받으면 문언상 해당하지 않는다(해석). 심사 메모에 "계정 없음, 백업 권한용"이라고 적는다.
- 계정 삭제(5.1.1(v)): 계정을 만들지 않으니 해당하지 않는다. 대신 설정에 "구글 드라이브 연결 해제"를 둔다.
- 결제 화면: 상품 이름, 가격, 제공 내용, 구매 복원, 약관·처리방침 링크(App Store 3.1.1, 3.1.2, Play 정기결제 정책).

### 위치정보법

기기 안에서만 위치를 쓰고 사업자의 위치정보시스템으로 보내지 않으면 위치기반서비스 신고 대상에서 빠진다([위치정보지원센터](https://www.lbsc.kr/front/content/contentViewer.do?contentId=CONTENT_0000081)).
이 앱은 사용자가 [내 위치 가져오기]를 누른 때만 좌표를 운영체제의 주소 변환(Android Geocoder, iOS CLGeocoder)에 넘긴다. 운영체제가 Google·Apple 서버를 쓰는 경우가 여기에 해당하는지는 확인하지 못했다. 문의는 하지 않기로 했다(2026-11-11 사용자 결정). 문제가 생기면 위치정보지원센터(02-588-0185)에 묻는다. 개인정보처리방침에는 누를 때만 위치를 쓰고 좌표는 남기지 않으며 주소 변환에 운영체제 기능을 쓴다고 적는다.
1인 창조기업은 신고 없이 시작하고 1개월 안에 신고할 수 있다(제9조의2).

## 광고와 결제 키

저장소에 두지 않고 `local.properties`에 넣는다. 값이 없으면 출시 빌드는 광고를 부르지 않는다.

```properties
admob.app.id=ca-app-pub-...~...
admob.native=ca-app-pub-.../...
admob.interstitial=ca-app-pub-.../...
admob.rewarded=ca-app-pub-.../...
```

iOS는 `iosApp/iosApp/Local.xcconfig`에 넣는다(예시는 `Local.xcconfig.example`). xcconfig는 `//`를 주석으로 읽어 광고 단위의 슬래시를 `/$()`로 끊는다. CD는 이 파일을 `IOS_LOCAL_XCCONFIG_BASE64`로 받는다.

```xcconfig
ADMOB_APP_ID = ca-app-pub-...~...
ADMOB_NATIVE = ca-app-pub-.../$()...
ADMOB_INTERSTITIAL = ca-app-pub-.../$()...
ADMOB_REWARDED = ca-app-pub-.../$()...
REVENUECAT_IOS = appl_...
```

`REVENUECAT_IOS`는 RevenueCat 프로젝트에 만든 App Store 앱의 공개 키다. 비면 iOS는 구매가 없는 무료 앱으로 뜨고, CD는 그런 빌드를 만들지 않는다.

개발 빌드는 늘 Google 시험 광고를 받는다. 실제 광고를 눌러 시험하지 않는다. iOS 앱 ID가 비면 SDK가 앱을 끝내서 `Config.xcconfig`에 구글 공개 시험 앱 ID를 기본으로 둔다.

## 사업자 준비

처음 판매하는 개인 개발자 기준 순서다(2026-11-05 공식 자료 확인). `[세무]`는 세무사에게 확인할 것이다. 번호가 나오기 전까지 개발은 시험 환경으로 한다.

**0. 시작 전**

- [ ] 회사 취업규칙과 근로계약의 겸업 조항을 본다. 근로기준법에는 규정이 없고 회사 규칙에 달렸다([고용부](https://www.moel.go.kr/minwon/fastcounsel/fastcounselView.do?inetDcssMngId=202403041548097201000)).

**1. 사업자등록**

- [x] (2026-11-05 처리 완료, 번호는 저장소에 적지 않는다) 홈택스 > 신청/제출 > 사업자등록신청/정정. 시작 전이나 시작 뒤 20일 안에 한다([국세청](https://www.nts.go.kr/nts/cm/cntnts/cntntsView.do?mi=2444&cntntsId=7777)). 수수료 없음, 보통 2일, 최대 5일([정부24](https://www.gov.kr/mw/AA020InfoCappView.do?CappBizCD=12100000072)).
- [ ] 사업장이 따로 없으면 집 주소로 한다([부가세법 6조](https://www.law.go.kr/LSW/lsLawLinkInfo.do?lsJoLnkSeq=900314560&chrClsCd=010202)). 임차했다면 계약서 사본이 필요하다.
- [ ] 업종 후보는 722000(응용 소프트웨어 개발 및 공급업)이다. 스토어 직접 판매 성격이면 525101도 후보다. 공식 코드표 원문은 확인하지 못했다 `[세무]`.
- [ ] 과세 유형을 정한다. 간이과세는 연 매출 1억 400만 원 미만이고, 4,800만 원 미만이면 납부 면제이며, 환급은 없다([국세청](https://www.nts.go.kr/nts/cm/cntnts/cntntsView.do?cntntsId=7806)) `[세무]`.

**2. 통신판매업 신고**

- [x] (2026-11-06 신고 접수, 등록면허세 40,500원 납부. 2026-11-12 신고번호를 받았다. 납부 번호와 신고번호는 저장소에 적지 않는다) 정부24나 구청에서 한다. 3일, 수수료 없음. 등록면허세는 신고가 수리되면 내고 그 뒤 매년 1월 기준으로 다시 낸다. 서울 구와 인구 50만 이상 시 40,500원, 그 밖의 시 22,500원, 군 12,000원이다(광진구 [안내](https://www.gwangjin.go.kr/portal/main/contents.do?menuNo=200744), 금액 구분은 [2차 자료](https://hometax-go.kr/mailorder-costs/)라 고지서로 확인한다). 폐업 신고를 해야 부과가 멈춘다.
- [ ] 서류는 사업자등록증, 신분증, 구매안전 확인증이다. 앱은 망으로 전송되는 재화라 비적용 확인서 대상으로 보이니 구청에 묻는다([제24조](https://www.law.go.kr/LSW//lsLawLinkInfo.do?lsJoLnkSeq=1000527507&lsId=009318&chrClsCd=010202&print=print)).
- [ ] 직전 연도 거래 50회 미만이나 간이과세자는 신고 면제 고시가 있다([2020년 개정 보도](https://www.newsis.com/view/NISX20200729_0001111296)). 고시 원문은 도구로 열리지 않았다. Play는 유료 앱이나 인앱 구매가 있으면 라이선스 번호를 넣으라고 안내하고 면제자용 방법은 그 페이지에 없다(2026-11-05 확인). 그래서 면제 대상이어도 인앱 구매를 팔려면 신고하는 쪽으로 본다. 광고만 있는 앱은 이 요구 대상이 아니다. 앱 개발자가 대상인지 공정위 해석은 찾지 못했다.

**3. Play Console**

- [ ] 개발자 계정 > 내 정보에 사업자등록번호, 전자상거래 라이선스 번호, 발급 기관을 넣는다. 한국 사용자에게 보인다([요구사항](https://support.google.com/googleplay/android-developer/answer/6223646?hl=ko)).
- [x] 판매 계정은 결제 프로필 주소가 공개된다. 집 주소가 공개되어도 그대로 간다(2026-11-06 사용자 결정).
- [ ] 결제 프로필 > 대한민국 세금 정보에 사업자등록번호를 넣는다. 없으면 수수료에 부가세 10%가 붙는다([세율](https://support.google.com/googleplay/android-developer/answer/138000?hl=ko)). 2026-11-13 Google에 문의 중(사용자). 진행 상태는 콘솔에서 보이지 않는다.
- [x] 수수료 프로그램에 등록한다. 첫 100만 달러 15%, 초과분 30%, 구독 갱신 15%다([수수료](https://support.google.com/googleplay/android-developer/answer/112622?hl=ko), [등록](https://support.google.com/googleplay/android-developer/answer/10632485?hl=ko)). 2026-11-12 Play 알림 "계정 그룹이 15% 서비스 수수료 프로그램에 등록됨"으로 확인.
- [x] 정산 계좌를 시험 입금으로 인증한다([인증](https://support.google.com/googleplay/android-developer/answer/7161378?hl=ko)). 지급은 다음 달 15일쯤이다. 사용자 인증 완료(2026-11-13 확인).

**4. App Store (R2)**

- [ ] 개인도 사업자등록번호가 필요하다. Business > Agreements > Compliance > Korean Law([Apple](https://developer.apple.com/help/app-store-connect/manage-compliance-information/manage-korea-compliance-information)).

**5. 직장인이 알아 둘 것**

- [ ] 근로소득과 사업소득을 합쳐 5월에 종합소득세를 신고한다.
- [ ] 보수 외 소득이 연 2,000만 원을 넘으면 초과분에 건강보험료가 붙는다([공단](https://www.nhis.or.kr/nhis/policy/wbhada01900m01.do)).

**6. 해마다**

- [ ] 부가세 신고: 일반과세자 1월·7월 25일, 간이과세자 1월 25일([기한](https://www.nts.go.kr/nts/cm/cntnts/cntntsView.do?mi=2273&cntntsId=7694)).
- [ ] 한국 고객 부가세, 해외 매출 영세율, AdMob 수익 처리, 현금영수증 의무는 공식 답을 다 찾지 못했다 `[세무]`. AdMob 계정 유형은 바꿀 수 없으니 사업자로 만든다([AdMob](https://support.google.com/admob/answer/12835021?hl=ko)).

## 출시 국가

열린 결정 D11이다. 정하는 것은 사용자이고, 아래는 2026-11-01 공식 문서로 확인한 사실과 추천이다.

### 추천

- **처음(2.0.0)**: 한국, 일본, 미국, 캐나다, 호주, 뉴질랜드. 세 언어가 바로 맞고, 맞춤 광고 동의 창(Google 인증 CMP)을 두지 않아도 된다.
- **나중에**: EEA·영국·스위스. 동의 창을 넣어야 하고, 광고나 결제가 있으면 판매자(trader)로 선언해 주소와 전화번호가 공개된다.
  공개할 연락처(사업장 주소 같은 것)를 마련한 뒤 더한다.
- **그 밖의 나라**: 영어로 보이니 국가만 더하면 된다. 두 스토어 모두 새 빌드 없이 국가를 더하고 뺄 수 있다.
- 유료 기능(Plus)을 팔려면 나라와 상관없이 사업자등록번호가 필요하고, Play는 통신판매업 신고번호도 받는다(한국 개발자). D9(Plus 시기)와 함께 정한다.

### 나라마다 걸리는 일

| 나라 | 걸리는 일 | 근거 |
| --- | --- | --- |
| 한국 | 유료 기능: 사업자등록번호·통신판매업 신고번호(Play), 사업자등록번호(Apple, 제품 페이지에 보임). Play 판매자 상태 선언은 모든 계정이 2027-02-01까지 하고, EEA·영국에 내지 않으면 비판매자로 선언할 수 있다 | [Play 개발자 정보](https://support.google.com/googleplay/android-developer/answer/6223646), [Apple 한국](https://developer.apple.com/help/app-store-connect/manage-compliance-information/manage-korea-compliance-information/), [Play 판매자 선언](https://support.google.com/googleplay/android-developer/answer/17594752) |
| 일본 | 유료 기능: 특정상거래법 표시(이름·전화·주소, 청구하면 바로 알린다고 적으면 생략할 수 있는 경우가 있음), 정기 결제 최종 확인 화면 항목(가격, 기간, 자동 갱신, 해지 방법), Google 결제 프로필의 자금결제법 동의(2026-12-01까지)와 할부판매법 공시 | [Play 개발자 정보](https://support.google.com/googleplay/android-developer/answer/6223646), [소비자청 Q&A](https://www.no-trouble.caa.go.jp/qa/advertising.html), [최종 확인 화면 지침](https://www.no-trouble.caa.go.jp/pdf/20241119la02_09.pdf), [Google 결제](https://support.google.com/paymentscenter/answer/17595997) |
| 미국 | 텍사스(2026-06-04)·유타(2026-05-06)·루이지애나(2026-07-01)의 새 Apple 계정에 연령 확인과 보호자 동의가 걸린다. 앱은 Declared Age Range API와 중대한 변경 동의를 다룰 수 있어야 한다. Play는 주 단위로 빼 주지 않는다 | [Apple](https://developer.apple.com/news/?id=sg176nne), [Apple 연령 확인](https://developer.apple.com/support/age-assurance/), [Play](https://support.google.com/googleplay/android-developer/answer/16569691) |
| 캐나다·뉴질랜드 | 스토어 쪽에서 따로 찾은 요구가 없다(확인 못 함) | - |
| 호주 | Apple이 성인 확인이 안 된 사용자에게 18+ 앱만 막는다. 우리 앱은 해당하지 않는다 | [Apple](https://developer.apple.com/news/?id=f5zj08ey) |
| 영국 | 맞춤 광고에 인증 CMP(2024-01-16부터). Play 판매자 선언 대상이고, 개인 판매자는 전화·주소가 모든 나라에 보인다(2027-03-01부터) | [Google CMP](https://support.google.com/adsense/answer/13554116), [Play 판매자 선언](https://support.google.com/googleplay/android-developer/answer/17594752) |
| EEA·스위스 | 인증 CMP(EEA 2024-01-16, 스위스 2024-07-31). Apple 판매자 정보(주소·전화·이메일, 주소 증빙)가 없으면 EU에서 내려간다(2025-02-17부터). Play 판매자 선언 | [Apple DSA](https://developer.apple.com/help/app-store-connect/manage-compliance-information/manage-european-union-digital-services-act-trader-requirements/), [Apple 공지](https://developer.apple.com/news/?id=einwn76m), [Google CMP](https://support.google.com/adsense/answer/13554116) |

- 시장: 모바일 OS 중 iOS 비율은 일본 53.8%, 미국 52.0%, 영국 50.1%, 한국 39.3%다(StatCounter 2026년 10월,
  [일본](https://gs.statcounter.com/os-market-share/mobile/japan/)). 일본과 영어권은 iOS가 절반을 넘는다.
- 국가 바꾸기: App Store는 언제든 바꾸고 24시간 안에 반영되며, 뺀 나라의 기존 사용자도 업데이트를 받는다
  ([Apple](https://developer.apple.com/help/app-store-connect/manage-your-apps-availability/manage-availability-for-your-app-on-the-app-store/)).
  Play는 Production의 국가 목록에서 더하고 빼고, 구독은 새 나라의 가격을 정해야 한다([Play](https://support.google.com/googleplay/android-developer/answer/7550024)).
- 광고 동의 창은 EEA·영국·스위스에 내지 않으면 미룰 수 있다. 미국 주 개인정보 메시지는 선택이다([AdMob](https://developers.google.com/admob/android/privacy/us-states)).

## 출시 전 확인

- [x] 글꼴 10종 라이선스 고지(2026-11-04, 2026-11-13 다섯 자리 바꿈): 설정 > 정보 > "오픈소스 라이선스"에 OFL 전문(Pretendard, 학교안심 받아쓰기, 고운바탕), 부산시 전용서체 안내,
      네이버 글꼴 저작권 안내와 OFL 전문(점꼴체), 네이버 글꼴 저작권 안내(힘내라는 말보단), 창원단감둥근체·수박양체 안내문,
      온글잎 폰트 안내(2026-11-14 교보 손글씨 대신), 세종글꽃체 저작권 안내를 넣었다.
      창원단감둥근체는 [창원시 공식 안내](https://www.changwon.go.kr/cwportal/depart/11071/11079/13531.web)로 확인했다(누구나 무료, 모바일 등 별도 허가 없이 사용). 앱에 파일을 넣는 경우를 따로 적은 문구는 없지만 문의는 하지 않기로 했다(2026-11-11 사용자 결정).
- [x] 네이버 글꼴 라이선스 전문: 네이버 글꼴 저작권 안내와 OFL 전문이 앱에 들어 있다(`files/licenses/`). 네이버 도움말 원문과 글자 단위 대조는 도움말 페이지가 도구로 열리지 않아 하지 않았다(2026-11-13 사용자 확인으로 완료 처리).
- [x] 교보손글씨 2019 사용자는 창원단감둥근체로 옮겨 가고 처음 열 때 한 번 알린다(2026-11-14, 교보손글씨 2020 박도연 자리를 온글잎 위씨리스트로 바꿈).
- [ ] 777미니콩다방·777꼬마나비는 2.0.0에서 빠진다. 1.7.5로 먼저 빼는 안은 내지 않기로 했고(2026-11-01), 그때까지 1.7.4가 그대로 배포된다
- [x] Play Console 포그라운드 서비스 신고: 지금 백업·복원이 `dataSync` 종류를 쓴다(스펙 003 research R14). 쓰는 까닭과 화면 영상을 낸다 2026-11-12 신고 완료(`FOREGROUND_SERVICE_DATA_SYNC`), 앱 코드도 `dataSync` 한 종류만 쓴다(2026-11-14 확인).
- [ ] 구글 동의 화면 브랜드 검증: 앱 이름·로고를 띄우려면 필요하다(2~3영업일)
      ([Google](https://support.google.com/cloud/answer/13463073), 스펙 003 근거).
      준비는 끝났다(2026-11-13). Search Console에서 `https://sjunh812.github.io/` 소유가 확인됐고, 브랜딩에 홈페이지 `https://sjunh812.github.io/1day1diary/`(앱 소개, 처리방침·약관 링크),
      처리방침 `https://sjunh812.github.io/1day1diary/privacy/`, 약관 `https://sjunh812.github.io/1day1diary/terms/`, 승인된 도메인 `sjunh812.github.io`를 넣어 저장했다.
      로고는 옛 회색 연필에서 지금 스토어 아이콘(`docs/store/play-512.png`)으로 바꿨다(사용자, 2026-11-13). 아이콘을 바꾸면 이 로고도 함께 바꾼다.
      같은 날 [브랜딩 확인]을 냈지만 자동 검사가 "홈페이지 웹사이트가 나에게 등록되어 있지 않다"로 막았다. 소유 확인이 시스템에 반영되기까지 24시간을 기다린 뒤
      브랜딩 페이지 [문제 보기] > "문제를 해결함"으로 다시 낸다(2026-11-14 이후). 범위는 민감하지 않은 것만 쓰고 게시 상태는 프로덕션이라, 검증 전에도 로그인 사용자 수 제한은 없다.
- [ ] 약관과 처리방침은 세 곳에 있다: 원문 `docs/legal/`, 앱이 거는 Notion, 구글 브랜딩이 거는 GitHub Pages(`sjunh812.github.io` 저장소 `1day1diary/`). 고치면 세 곳을 함께 고친다.
- [x] 기분·날씨 아이콘: 기분 얼굴은 우리가 Figma에서 그린 것을 코드로 옮겼다(`DiaryMoodFace`). 날씨와 화면 아이콘은 Lucide(ISC) 경로를 옮겼고, 빠져 있던 Lucide 고지를 오픈소스 라이선스 목록에 더했다. 같은 때 iOS에만 들어가는 SDK(GoogleSignIn, Firebase, Google Mobile Ads, RevenueCat KMP)도 목록에 더했다(`config/aboutlibraries/`, 2026-11-13)
- [x] 공개 저장소에 올라간 네이버 지도 API 키를 콘솔에서 삭제하거나 막기(2026-11-01 해제, 시험 호출이 401 "Authentication Failed"). 네이버 지도 API는 유료로 바뀌어 리뉴얼에서 쓰지 않는다
- [x] 위치정보지원센터 문의: 하지 않기로 함(2026-11-11 사용자 결정, `docs/DECISIONS.md`)
- [x] 개인정보처리방침에 "복구 문의"를 적는다: 사용자가 스스로 첨부한 예전 일기 파일만 받고, 복구에만 쓰며, 복구한 파일을 돌려준 뒤 바로 지운다(헌법 II 예외, 스펙 001 FR-010a). 받는 곳(메일 계정)과 지우는 방법도 적는다
- [x] 처리방침과 약관을 공개하고 앱에 걸기(`LegalLinks`, 원문은 `docs/legal/`, 2026-11-07)
- [ ] 처리방침 URL을 Play Console, App Store Connect에 넣기 Play는 2026-11-13 검토 승인(노션 주소). App Store는 앱을 만든 뒤.
- [ ] Play 데이터 보안, App Store 개인정보 라벨(Privacy Manifest는 넣음, 위 "App Store 제출") Play 데이터 보안은 2026-11-13 검토 승인.
- [ ] Play Console 앱 콘텐츠의 광고 포함 선언, 광고 ID 선언. iOS ATT 문구와 창은 넣음(2026-11-12, `SKAdNetworkItems` Google 목록 50개). 처리방침 3번에 iOS의 추적 끄는 곳을 더함(원문과 Notion, 2026-11-12), UMP 동의
- [x] AdMob iOS 앱과 광고 단위 3개를 만들어 `Local.xcconfig`에 넣기(2026-11-11, 계정 승인 뒤)
- [x] `app-ads.txt`를 `https://sjunh812.github.io/app-ads.txt`에 올리기(개발자 블로그 저장소 맨 앞, 2026-11-11)
- [x] Play Console 스토어 설정의 개발자 웹사이트를 `https://sjunh812.github.io`로 바꿈(2026-11-11). AdMob 앱 인증은 크롤러가 새 주소를 읽을 때까지(최대 7일) 기다린 뒤 "업데이트 확인"
- [ ] App Store 출시 뒤 AdMob iOS 앱에 스토어를 연결하고, App Store의 마케팅 URL도 같은 주소로 둔다
- [ ] Play "새로운 기능" 란과 App Store 설명에 광고와 광고 제거를 적기(Play 문구와 App Store 설명 끝 약관 줄은 `docs/store/listing.md`에 씀)
- [ ] 스크린샷(폰 ✓, 7"·10" 태블릿 ✓, iPhone 6.3" ✓, iPad 13" ✓ 세 언어, 2026-11-13), 피처 그래픽 ✓, 아이콘 ✓. 영어·일본어 Play 스크린샷 ✓(원어민 검수 전)
- [ ] 영어·일본어 화면 문구와 스토어 설명을 그 언어 앱·운영체제 용어로 검색해 맞추기(원어민 검수는 하지 않기로 함, 2026-11-11). 앱 이름(D12)과 일본어 화면 글꼴(D13)도 이때 정한다
- [ ] 세 언어로 화면 확인: 가장 긴 영어에서 잘리거나 겹치는 곳이 없는지, 날짜·시각이 그 언어 형식인지 2026-11-14 폰(API 36)에서 홈·달력·돌아보기·설정·쓰기·Plus·백업을 세 언어, 기본·1.3배 글자로 봤다. 겹침은 없고 날짜·시각은 각 언어 형식. 1.3배에서 홈의 백업 상태 줄이 영어·일본어로 말줄임되고, 일본어 백업 화면 설명이 "変わった/分だけ"처럼 어절 중간에서 줄이 나뉜다(다듬을 거리). 태블릿은 보지 못했다.
- [x] Play Console 판매자(trader) 상태 선언: 모든 계정이 2027-02-01까지 한다(위 "출시 국가") 2026-11-13 검토 승인.
- [x] 출시 국가(D11): 한국, 일본, 미국, 캐나다, 호주, 뉴질랜드(2026-11-11 사용자 결정). 일본에 유료 기능을 팔기 전에 특정상거래법 표시와 결제 프로필 동의를 마친다
- [x] 이전 업그레이드 계측 테스트 통과(`docs/migration.md` 시험) 2026-11-14 별도 에뮬레이터(API 29)에서 통과: 기본, 1004편·사진 336장, 복사 중·바꿔 넣기 중 강제 종료, 잠금(0427) 다섯 가지 모두 원본 해시 그대로, 일기·사진 수 일치, 계측 시험 통과.

## App Store 제출

App Store는 새 출시다(조사일 2026-11-13). 앱 쪽 준비와 콘솔에서 사람이 할 일을 나눠 적는다. 규격과 정책은 제출 직전에 다시 본다.

### 출시 순서 (2026-11-14 정리)

앞 단계가 뒤 단계의 전제다. 자세한 값은 아래 "콘솔에서 할 일"과 "결제 콘솔 설정"에 있다. [사용자]는 로그인·계약·키 입력이라 직접, [같이]는 화면을 보며 함께, [코드]는 저장소에서 한다.

1. [사용자] Apple Developer Program 가입·갱신 상태를 확인한다. 팀 ID를 적어 둔다(`project.yml`에 아직 서명 팀이 없다).
2. [사용자] App Store Connect > Business: 유료 앱 계약, 세금, 은행, Korean Law(사업자등록번호). 승인에 시간이 걸릴 수 있어 가장 먼저 낸다.
3. [사용자] Apple Developer: 앱 ID `org.sjhstudio.diary`(인앱 결제만), Apple Distribution 인증서, App Store 배포 프로필.
4. [같이] App Store Connect 앱 만들기(기본 한국어, 영어·일본어 더함). 인앱 구입 `ad_free`, 구독 그룹과 `plus_yearly`를 세 언어로 만든다.
5. [사용자] RevenueCat: 계정 권한 문제("앱 구성을 추가할 권한이 없다")를 먼저 푼다 → App Store 앱 추가 → 인앱 구입 키 올리기 → 두 상품을 권한 `ad_free`·`plus`에 잇기 → Apple 공개 키 받기.
6. [사용자] 키 넣기: `iosApp/iosApp/Local.xcconfig`에 `REVENUECAT_IOS` 줄이 아직 없다(구글·광고 값 6개는 있음). CD 비밀값 `IOS_*` 4개, `GOOGLE_SERVICE_INFO_PLIST_BASE64`, `IOS_LOCAL_XCCONFIG_BASE64`.
7. [코드] 출시 전 앱 손볼 것: iOS 네이티브 광고 카드를 Android와 같은 모양(앱 글꼴, 목록 줄·작은 연보라 버튼)으로 맞춘다(Android만 2026-11-13에 바뀜). iOS 세 언어·큰 글자 화면을 시뮬레이터에서 본다(Android만 봄).
8. [같이] Xcode Archive → Privacy Report 확인 → TestFlight 업로드. CD는 IPA만 만들고 올리지 않는다.
9. [사용자] TestFlight 실기기: 샌드박스 구매·취소·승인 대기·복원·재설치 뒤 복원·구독 만료, 구글 드라이브 백업·복원, 첫 광고 앞 추적 허용(ATT) 창, Face ID 잠금.
10. [같이] 등록 정보: 이름·부제·홍보 문구·키워드·설명(끝에 약관·처리방침), 스크린샷(iPhone 6.3"·iPad 13"), 연령 등급, 카테고리, 개인정보 라벨, URL(처리방침 노션·지원·마케팅), 심사 메모, 출시 국가.
11. [사용자] 심사 제출.
12. 출시 뒤: [코드] 설정 "리뷰 남기기"에 App Store 앱 번호, [사용자] AdMob iOS 앱에 스토어 연결, Google Cloud iOS OAuth 클라이언트에 App Store ID·팀 ID.

### 앱에 들어 있는 것

| 항목 | 상태 | 자리 |
| --- | --- | --- |
| 권한 문구(Face ID, 카메라, 위치, 추적) 세 언어 | 있음 | `iosApp/project.yml`, `*.lproj/InfoPlist.strings` |
| 사진 고르기 | 권한 문구가 필요 없다. 시스템 사진 고르기는 앱에 보관함 권한을 주지 않는다 | FileKit |
| 알림 | 매일 알림과 백업 알림 모두 기기 안 알림이다. 푸시 권한(APNs)과 인증서는 쓰지 않는다 | `IosReminderScheduler`, `IosBackupNotifier` |
| 추적 동의(ATT) | 홈 목록의 첫 광고를 받기 직전에 한 번 묻는다. 거절해도 광고는 나온다 | `GoogleAdsBridge` |
| 광고 동의 창(UMP) | 넣지 않았다. 처음 출시 국가에 EEA·영국·스위스가 없다 | 위 "출시 국가" |
| 개인정보 매니페스트 | 앱이 직접 쓰는 필수 사유 API 네 가지(남은 공간, 파일 시각, 부팅 시각, UserDefaults)를 적었다. 수집 데이터는 SDK(Firebase, Google 광고, GoogleSignIn)가 각자 매니페스트로 알린다. RevenueCat은 KMP SDK 안에 정적으로 묶여 와서 자체 매니페스트가 따라오지 않으므로, 그 매니페스트의 구매 기록(앱 기능, 연결 안 됨)을 우리 매니페스트에 옮겨 적었다. 제출 전에 Archive > Generate Privacy Report로 한 번 본다 | `iosApp/iosApp/PrivacyInfo.xcprivacy` |
| 수출 규정 | 표준 HTTPS만 써서 `ITSAppUsesNonExemptEncryption`을 false로 뒀다. TestFlight가 빌드마다 묻지 않는다 | `iosApp/project.yml` |
| 구글 드라이브 | iOS OAuth 클라이언트 ID를 `Local.xcconfig`에서 읽는다. 로그인은 드라이브 권한만 받고 계정을 만들지 않는다 | `GoogleDriveBridge` |
| 자동 백업 | 백그라운드 작업 하나(`org.sjhstudio.diary.backup.daily`)와 앱이 뒤로 갈 때 확인. 실행 시각은 시스템이 정한다 | `DailyBackupTask` |
| 구매 | RevenueCat KMP SDK. 광고 제거, Plus 연 구독, 구매 복원, 구독 관리 링크. 복원은 Apple 계정 로그인 창을 띄울 수 있다 | `IosBilling.kt` |
| 전면 광고 원격 스위치 | Firebase Remote Config `interstitial_enabled`. Android와 같은 값을 본다 | `FirebaseReporter` |

아직 없는 것: 설정의 "리뷰 남기기" 줄(App Store 앱 번호가 정해진 뒤 `storeReviewUrl`에 넣는다), 개발자 모드(실행 인자 `-diaryPlan PLUS`로 요금제만 바꾼다).

### 콘솔에서 할 일

- [ ] Apple Developer: 앱 ID `org.sjhstudio.diary`를 만든다. 켤 기능은 인앱 결제뿐이다(푸시, iCloud, App Group은 쓰지 않는다). Apple Distribution 인증서와 App Store 배포 프로필을 만들어 CD 비밀값에 넣는다.
- [ ] App Store Connect > Business: 유료 앱 계약, 세금, 은행 정보. 계약이 끝나야 상품 가격이 내려온다. Compliance > Korean Law에 사업자등록번호를 넣는다(위 "사업자 준비" 4).
- [ ] 앱 만들기: 번들 ID `org.sjhstudio.diary`, 기본 언어 한국어, 영어와 일본어 현지화를 더한다. 앱 이름은 영어 "1 Day 1 Diary", 일본어 「1日1日記」로 정했다(D12, 2026-11-13).
- [ ] 인앱 구입: 비소모품 `ad_free`(₩9,900), 구독 그룹 하나에 `plus_yearly`(1년, ₩14,900, 체험 없음). 상품마다 세 언어 표시 이름과 설명, 심사용 화면 캡처를 넣는다. 첫 상품은 앱 빌드와 함께 심사에 낸다.
- [ ] RevenueCat: 계정에 "앱 구성을 추가할 권한이 없다"고 나와 먼저 계정 조건(이메일 인증 등)을 푼다(2026-11-13 확인). 같은 프로젝트에 App Store 앱을 더하고 인앱 구입 키(App Store Connect > 사용자 및 액세스 > 통합)를 올린다. 두 상품을 권한에 잇고 Apple 공개 키를 `Local.xcconfig`에 넣는다.
- [x] Google Cloud(`diary-app`): iOS OAuth 클라이언트의 번들 ID가 `org.sjhstudio.diary`인 것을 확인했다(2026-11-13). App Store ID와 팀 ID 칸은 앱이 생긴 뒤 넣는다. 동의 화면 브랜드 검증은 "출시 전 확인"에 있다.
- [x] Firebase(`diary-app`): iOS 앱은 이미 있었고 `GoogleService-Info.plist`를 받아 `iosApp/iosApp/`에 두었다(2026-11-13). CD 비밀값 `GOOGLE_SERVICE_INFO_PLIST_BASE64`는 직접 넣는다. Remote Config의 `interstitial_enabled`는 Android와 함께 쓴다.
- [ ] 개인정보 라벨: 위 "스토어 신고" 표대로 답한다. 광고에 쓰는 기기 ID와 광고 데이터는 "추적에 사용"으로 답한다(ATT를 띄운다). 일기, 사진, 기분, 주소는 수집하지 않는다.
- [ ] 연령 등급 설문, 카테고리(라이프스타일), 개인정보처리방침 URL, 지원 URL, 마케팅 URL(`https://sjunh812.github.io`).
- [ ] 이름·부제·홍보 문구·키워드·설명 넣기: 문구는 `docs/store/listing.md`의 "App Store 부제·홍보 문구·키워드"와 "App Store 설명"에 세 언어로 있다.
- [ ] 구독 앱은 설명에 이용약관 링크가 있어야 한다(3.1.2). 레퍼런스 앱 대부분처럼 설명 끝에 우리 약관과 처리방침 주소를 적고 EULA 칸은 비워 Apple 표준을 쓴다(2026-11-13 사용자 결정, 문구는 `docs/store/listing.md` "App Store 설명").
- [x] 노션의 이용약관과 개인정보처리방침을 `docs/legal/` 원문(2026-11-13판)으로 바꿨다. 같은 날 읽기 쉬운 말로 다시 쓴 글로 노션과 GitHub Pages를 함께 바꿨다. 결제·환불·해지를 App Store까지 담았다(2026-11-13, 공개 페이지에서 확인).
- [ ] 스크린샷 올리기: `docs/store/screens/ios/{ko,en,ja}/`의 iPhone 4장은 위 표의 medium(1206×2622) 칸, iPad 4장은 13" 칸에 올린다. App Store Connect가 6.9"(large) 칸을 따로 요구하면 1320×2868로 다시 만든다(확인 못 함).
- [ ] 심사 메모: 회원 계정이 없다. 구글 로그인은 백업을 사용자 본인의 구글 드라이브에 두는 권한만 받는다(4.8 Sign in with Apple, 5.1.1(v) 계정 삭제에 해당하지 않음). 백업을 시험할 구글 계정이 필요하면 심사용 계정을 따로 적는다. 구매 복원은 설정 > Plus 화면에 있다.
- [ ] 출시 국가: 한국, 일본, 미국, 캐나다, 호주, 뉴질랜드(D11). 미국 일부 주의 연령 확인 요구(위 "나라마다 걸리는 일")가 이 앱에 걸리는지 제출 직전에 다시 본다.
- [ ] TestFlight에서 샌드박스 계정으로 구매, 취소, 승인 대기(구입 요청), 복원, 재설치 뒤 자동 복원, 구독 만료를 확인한다. 시뮬레이터에서는 RevenueCat 키가 없어 확인하지 못했다(2026-11-13).

## Firebase

Spark 무료 요금제. 설정 파일은 저장소에 두지 않는다.
프로젝트는 드라이브 OAuth와 같은 `diary-app`(`stone-rhythm-310608`)이다. Firebase 프로젝트를 지우면 Cloud 프로젝트도 지워져 드라이브 백업 연결이 끊긴다. 지우지 않는다.
비정상 종료 시험: `adb shell am start -n org.sjhstudio.diary/.debug.DevModeActivity --ez crash true` 뒤 앱을 다시 연다.

- Android: `composeApp/google-services.json`
- iOS: `iosApp/iosApp/GoogleService-Info.plist`

파일이 있으면 빌드가 켜고, 없으면 아무것도 보내지 않는다. 개발 빌드는 보내지 않는다. 확인할 때는 `local.properties`에 `firebase.debug=true`를 넣고 `adb shell setprop debug.firebase.analytics.app org.sjhstudio.diary`로 DebugView를 켠다.
출시 빌드는 Android 난독화 매핑과 iOS dSYM을 자동으로 올린다.

## 결제 콘솔 설정

앱은 아래 이름을 쓴다(`StoreIds`).

- Play 상품: 인앱 `ad_free`(광고 제거), 구독 `plus_yearly`(기본 요금제 연간).
- RevenueCat 권한: `ad_free`는 `ad_free` 상품, `plus`는 `plus_yearly` 상품에 잇는다.
- Google 실시간 개발자 알림(RTDN)은 켜져 있다: Play Console 수익 창출 설정의 주제 `projects/stone-rhythm-310608/topics/Play-Store-Notifications`, 알림 범위 "정기 결제, 무효화된 구매, 모든 일회성 제품", 그 주제에 RevenueCat 구독이 붙어 있다. Pub/Sub은 결제 계정 없이 쓴다. 2026-11-14 광고 제거 시험 주문을 환불(자격 삭제)하자 1분 뒤 RevenueCat에 환불이 찍히고 권한이 빠졌으며, 앱은 다음 포그라운드 때 광고를 다시 보였다(구매 복원을 누르지 않아도 됨).
- RevenueCat Google 공개 키를 `local.properties`의 `revenuecat.android`와 CD 비밀값 `REVENUECAT_ANDROID`에 넣는다. 이 값이나 광고 ID가 없으면 CD가 멈춘다.
- App Store 상품도 같은 ID로 만든다: 비소모품 `ad_free`, 자동 갱신 구독 `plus_yearly`(구독 그룹 하나, 1년). RevenueCat 같은 프로젝트에 App Store 앱을 더하고 두 상품을 같은 권한(`ad_free`, `plus`)에 잇는다. 구매는 스토어마다 따로다(`docs/business.md` 결제 구현 원칙).
- RevenueCat Apple 공개 키를 `iosApp/iosApp/Local.xcconfig`의 `REVENUECAT_IOS`에 넣는다.

현황(2026-11-07): AdMob 앱과 광고 단위 3개(전면, 네이티브, 보상형)를 만들었고 계정은 재활성화 심사 중이다. RevenueCat 프로젝트, Play 앱, 권한 `plus`와 `ad_free`를 만들었다. 서비스 계정 JSON과 Play 상품 연결이 남았다. RevenueCat 이메일 인증도 남았다.
