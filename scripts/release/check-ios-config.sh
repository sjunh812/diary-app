#!/bin/sh
# iOS 출시 빌드에 들어갈 Local.xcconfig에 값이 다 있는지 본다.
#   sh scripts/release/check-ios-config.sh iosApp/iosApp/Local.xcconfig
# 드라이브 연결, 광고, 구매 가운데 하나라도 비면 멈춘다. 값은 화면에 찍지 않는다.
set -eu

file=${1:?Local.xcconfig 경로를 주세요}
[ -f "$file" ] || { echo "파일이 없어요: $file" >&2; exit 1; }

missing=""
for name in GID_CLIENT_ID GID_REVERSED_CLIENT_ID ADMOB_APP_ID ADMOB_NATIVE ADMOB_INTERSTITIAL ADMOB_REWARDED REVENUECAT_IOS; do
  grep -Eq "^[[:space:]]*$name[[:space:]]*=[[:space:]]*[^[:space:]]" "$file" || missing="$missing $name"
done

# 예시 파일의 자리표시 값이 그대로면 연결되지 않는다.
if grep -Eq "(0000000000|xxxxxxxx)" "$file"; then
  echo "예시 값이 그대로 남아 있어요" >&2
  exit 1
fi

if [ -n "$missing" ]; then
  echo "Local.xcconfig에 값이 없어요:$missing" >&2
  exit 1
fi
echo "iOS 설정 확인됨"
