#!/bin/sh
# GitHub release 환경에 Android CD 비밀값 넣기. 값은 화면에 나오지 않음
# 사람이 직접 실행. 비밀번호는 실행 중에 입력
#
#   sh scripts/release/set-secrets.sh <업로드 키 .jks 경로> [Play 서비스 계정 .json 경로]
#
# 광고 ID와 RevenueCat 키는 local.properties, Firebase 설정은 composeApp/google-services.json에서 읽음
# iOS 값(인증서, 프로필, Local.xcconfig)은 App Store 준비가 끝난 뒤 추가(docs/release.md "CD 비밀값")
set -eu

ENV=release
KEYSTORE=${1:?업로드 키 .jks 경로를 주세요}
PLAY_JSON=${2:-}
[ -f "$KEYSTORE" ] || { echo "키 파일이 없어요: $KEYSTORE"; exit 1; }

prop() { sed -n "s/^$1=//p" local.properties | head -n 1; }
put() { gh secret set "$1" --env "$ENV" >/dev/null && echo "넣음 $1"; }

# 값이 빈 줄은 건너뜀. 빈 값을 넣으면 CD가 광고 없는 빌드를 만듦
for pair in admob.app.id:ADMOB_APP_ID admob.native:ADMOB_NATIVE admob.interstitial:ADMOB_INTERSTITIAL \
    admob.rewarded:ADMOB_REWARDED revenuecat.android:REVENUECAT_ANDROID; do
    value=$(prop "${pair%%:*}")
    if [ -n "$value" ]; then printf %s "$value" | put "${pair##*:}"; else echo "건너뜀 ${pair##*:}(local.properties에 값 없음)"; fi
done

base64 -i "$KEYSTORE" | tr -d '\n' | put ANDROID_KEYSTORE_BASE64
if [ -f composeApp/google-services.json ]; then
    base64 -i composeApp/google-services.json | tr -d '\n' | put GOOGLE_SERVICES_JSON_BASE64
fi
if [ -n "$PLAY_JSON" ]; then put PLAY_SERVICE_ACCOUNT_JSON < "$PLAY_JSON"; fi

# 터미널에 보이지 않게 입력받음. 중간에 끊어도 입력 표시 복원
trap 'stty echo 2>/dev/null' EXIT INT TERM
ask() {
    printf '%s: ' "$2"
    stty -echo
    read -r value
    stty echo
    echo
    printf %s "$value" | put "$1"
}
ask ANDROID_KEYSTORE_PASSWORD "키 파일 비밀번호"
ask ANDROID_KEY_ALIAS "키 별칭(alias)"
ask ANDROID_KEY_PASSWORD "키 비밀번호"

echo "끝. 확인: gh secret list --env $ENV"
