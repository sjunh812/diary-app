#!/bin/sh
# 릴리스 태그와 앱에 적힌 버전이 같은지 본다.
#   sh scripts/release/check-version.sh v2.0.0
# Android와 iOS의 버전 이름, 빌드 번호가 서로 다르면 멈춘다.
set -eu

tag=${1:?태그를 주세요(예: v2.0.0)}
ROOT=$(cd "$(dirname "$0")/../.." && pwd)
gradle_file="$ROOT/composeApp/build.gradle.kts"
ios_file="$ROOT/iosApp/project.yml"

android_name=$(sed -n 's/^ *versionName = "\(.*\)"$/\1/p' "$gradle_file")
android_code=$(sed -n 's/^ *versionCode = \([0-9]*\)$/\1/p' "$gradle_file")
ios_name=$(sed -n 's/^ *MARKETING_VERSION: "\(.*\)"$/\1/p' "$ios_file")
ios_code=$(sed -n 's/^ *CURRENT_PROJECT_VERSION: "\(.*\)"$/\1/p' "$ios_file")

fail() {
  echo "버전이 맞지 않아요: $1" >&2
  exit 1
}

[ "$tag" = "v$android_name" ] || fail "태그 $tag, Android versionName $android_name"
[ "$android_name" = "$ios_name" ] || fail "Android $android_name, iOS $ios_name"
[ "$android_code" = "$ios_code" ] || fail "Android versionCode $android_code, iOS 빌드 번호 $ios_code"
echo "버전 확인: $android_name ($android_code)"
