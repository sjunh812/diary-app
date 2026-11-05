# 스토어 출시 준비

Play는 기존 앱 업데이트, App Store는 새 출시다. 조사일 2026-10-31. 규격과 정책은 바뀌니 제출 직전에 출처를 다시 본다.

## 업데이트가 이어지려면 (Play)

- `applicationId = "org.sjhstudio.diary"`를 `composeApp`에 **명시**한다. 안 쓰면 namespace를 따라가 다른 앱이 된다([configure-app-module](https://developer.android.com/build/configure-app-module)).
- 같은 앱 서명 키, `versionCode` 37 이상(36은 내지 않은 1.7.5가 썼다). 직접 관리하는 앱 서명 키는 잃으면 다시 만들 수 없다([앱 서명](https://support.google.com/googleplay/android-developer/answer/9842756?hl=en)).
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

만드는 법: 스크린샷은 실제 앱 화면(에뮬레이터·시뮬레이터 캡처)에 짧은 머리말을 얹은 세로 틀로 만든다. 한국어, 영어, 일본어 세 벌을 만들고
앱 화면도 그 언어로 찍는다. 틀은 Figma "화면" 페이지 아래 "스토어"
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
이 앱은 사용자가 [현재 위치 가져오기]를 누른 때만 좌표를 운영체제의 주소 변환(Android Geocoder, iOS CLGeocoder)에 넘긴다. 운영체제가 Google·Apple 서버를 쓰는 경우가 여기에 해당하는지는 확인하지 못했다. 출시 전에 위치정보지원센터(02-588-0185)에 묻고 답을 `docs/DECISIONS.md`에 남긴다. 개인정보처리방침에는 누를 때만 위치를 쓰고 좌표는 남기지 않으며 주소 변환에 운영체제 기능을 쓴다고 적는다.
1인 창조기업은 신고 없이 시작하고 1개월 안에 신고할 수 있다(제9조의2).

## 광고와 결제 키

저장소에 두지 않고 `local.properties`에 넣는다. 값이 없으면 출시 빌드는 광고를 부르지 않는다.

```properties
admob.app.id=ca-app-pub-...~...
admob.native=ca-app-pub-.../...
admob.interstitial=ca-app-pub-.../...
admob.rewarded=ca-app-pub-.../...
```

개발 빌드는 늘 Google 시험 광고를 받는다. 실제 광고를 눌러 시험하지 않는다.

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

- [ ] 정부24나 구청에서 한다. 3일, 수수료 없음, 등록면허세 매년 40,500원(광진구 기준, [안내](https://www.gwangjin.go.kr/portal/main/contents.do?menuNo=200744)).
- [ ] 서류는 사업자등록증, 신분증, 구매안전 확인증이다. 앱은 망으로 전송되는 재화라 비적용 확인서 대상으로 보이니 구청에 묻는다([제24조](https://www.law.go.kr/LSW//lsLawLinkInfo.do?lsJoLnkSeq=1000527507&lsId=009318&chrClsCd=010202&print=print)).
- [ ] 직전 연도 거래 50회 미만이나 간이과세자는 신고 면제 고시가 있다. 다만 Play가 번호를 요구하므로 면제자가 번호 없이 통과하는지 구청과 Play 화면에서 확인한다. 앱 개발자가 대상인지 공정위 해석은 찾지 못했다.

**3. Play Console**

- [ ] 개발자 계정 > 내 정보에 사업자등록번호, 전자상거래 라이선스 번호, 발급 기관을 넣는다. 한국 사용자에게 보인다([요구사항](https://support.google.com/googleplay/android-developer/answer/6223646?hl=ko)).
- [ ] 판매 계정은 결제 프로필 주소가 공개된다. 집 주소면 공개되어도 되는지 먼저 정한다.
- [ ] 결제 프로필 > 대한민국 세금 정보에 사업자등록번호를 넣는다. 없으면 수수료에 부가세 10%가 붙는다([세율](https://support.google.com/googleplay/android-developer/answer/138000?hl=ko)).
- [ ] 수수료 프로그램에 등록한다. 첫 100만 달러 15%, 초과분 30%, 구독 갱신 15%다([수수료](https://support.google.com/googleplay/android-developer/answer/112622?hl=ko), [등록](https://support.google.com/googleplay/android-developer/answer/10632485?hl=ko)).
- [ ] 정산 계좌를 시험 입금으로 인증한다([인증](https://support.google.com/googleplay/android-developer/answer/7161378?hl=ko)). 지급은 다음 달 15일쯤이다.

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

- [x] 글꼴 10종 라이선스 고지(2026-11-04): 설정 > 정보 > "오픈소스 라이선스"에 OFL 전문(Pretendard, 고운돋움, 고운바탕, 감자꽃, 하이멜로디,
      나눔손글씨 펜), 네이버 글꼴 저작권 안내와 OFL 전문(마루 부리, 점꼴체), 카페24 안내와 OFL 전문, 네이버 글꼴 저작권 안내(힘내라는 말보단)를 넣었다.
      카페24 써라운드는 공식 배포 파일의 `License-Ssurround.pdf`로 확인했다(SIL OFL 1.1, 앱 임베딩 허용).
- [ ] 네이버 글꼴 라이선스 전문 확인: 네이버는 마루 부리와 클로바 나눔손글씨가 나눔 글꼴과 같은 오픈 라이선스라고 안내한다. 나눔 글꼴 전문은 SIL OFL 1.1이라
      그 전문을 넣었다. 전문 페이지(help.naver.com, hangeul.naver.com)는 도구로 열리지 않아 출시 전에 브라우저로 직접 열어 같은지 본다.
- [x] 앱에 들어간 라이브러리 고지(2026-11-04): AboutLibraries로 빌드 때 목록을 만들어 같은 화면에 보인다. 의존성을 바꾸면 `:composeApp:exportLibraryDefinitions`로 다시 만든다.
- [ ] (선택) 교보손글씨 2019를 다시 넣고 싶으면 교보문고에 앱 번들 허용을 서면으로 받는다. 지금 앱 사용자는 나눔손글씨 펜으로 옮겨 간다.
- [ ] 777미니콩다방·777꼬마나비는 2.0.0에서 빠진다. 1.7.5로 먼저 빼는 안은 내지 않기로 했고(2026-11-01), 그때까지 1.7.4가 그대로 배포된다
- [ ] Play Console 포그라운드 서비스 신고: 지금 백업·복원이 `dataSync` 종류를 쓴다(스펙 003 research R14). 쓰는 까닭과 화면 영상을 낸다
- [ ] 구글 동의 화면 브랜드 검증: 앱 이름·로고를 띄우려면 필요하다(2~3영업일). 우리 소유로 인증된 도메인의 홈페이지와 개인정보처리방침을 먼저 준비한다
      ([Google](https://support.google.com/cloud/answer/13463073), 스펙 003 근거).
- [ ] 기분·날씨 아이콘 새로 그리기(지금 PNG 출처 미확인)
- [x] 공개 저장소에 올라간 네이버 지도 API 키를 콘솔에서 삭제하거나 막기(2026-11-01 해제, 시험 호출이 401 "Authentication Failed"). 네이버 지도 API는 유료로 바뀌어 리뉴얼에서 쓰지 않는다
- [ ] 위치정보지원센터 문의
- [ ] 개인정보처리방침에 "고쳐 받기 문의"를 적는다: 사용자가 스스로 첨부한 예전 일기 파일만 받고, 고치는 데만 쓰며, 고친 파일을 돌려준 뒤 바로 지운다(헌법 II 예외, 스펙 001 FR-010a). 받는 곳(메일 계정)과 지우는 방법도 적는다
- [ ] 처리방침 URL을 Play Console, App Store Connect, 앱 설정에 걸기
- [ ] Play 데이터 보안, App Store 개인정보 라벨, Privacy Manifest 작성(광고를 넣으므로 AdMob 항목 포함)
- [ ] Play Console 앱 콘텐츠의 광고 포함 선언, 광고 ID 선언. iOS ATT 문구와 `SKAdNetworkItems`, UMP 동의
- [ ] Play "새로운 기능" 란과 App Store 설명에 광고와 광고 제거를 적기
- [ ] 스크린샷(폰, 7"·10" 태블릿, iPhone medium·large, iPad 13"), 피처 그래픽, 아이콘
- [ ] 영어·일본어 화면 문구와 스토어 설명 원어민 검수(2026-11-01 초안은 개발자가 씀). 앱 이름(D12)과 일본어 화면 글꼴(D13)도 이때 정한다
- [ ] 세 언어로 화면 확인: 가장 긴 영어에서 잘리거나 겹치는 곳이 없는지, 날짜·시각이 그 언어 형식인지
- [ ] Play Console 판매자(trader) 상태 선언: 모든 계정이 2027-02-01까지 한다(위 "출시 국가")
- [ ] 출시 국가(D11) 정하기. 일본에 유료 기능을 팔면 특정상거래법 표시와 결제 프로필 동의를 마친다
- [ ] 이전 업그레이드 계측 테스트 통과(`docs/migration.md` 시험)
