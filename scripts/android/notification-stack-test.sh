#!/bin/sh
# 실제 알림과 런처로 여러 순서로 열어 앱 화면이 하나뿐인지 본다. 개발 빌드, 에뮬레이터 전용
#   ANDROID_SERIAL=emulator-5554 sh scripts/android/notification-stack-test.sh
set -u
SERIAL=${ANDROID_SERIAL:?에뮬레이터를 ANDROID_SERIAL로 지정해 주세요}
case "$SERIAL" in emulator-*) ;; *) echo "에뮬레이터에서만 돌립니다: $SERIAL"; exit 1 ;; esac
E="adb -s $SERIAL"
P=org.sjhstudio.diary
FAIL=0

$E shell pm grant $P android.permission.POST_NOTIFICATIONS 2>/dev/null
cnt() { $E shell dumpsys activity activities | grep -c "Hist  #.*$P/"; }
launcher() { $E shell am start -n $P/.SplashActivity -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -f 0x10200000 >/dev/null 2>&1; }
post() { $E shell am start -n $P/.debug.DevModeActivity --es notify "$1" >/dev/null; sleep 2; }
# 알림 판을 내려 글이 든 줄을 누름
tap() {
    $E shell cmd statusbar expand-notifications
    sleep 2
    $E shell uiautomator dump /sdcard/n.xml >/dev/null 2>&1
    b=$($E shell cat /sdcard/n.xml | tr '>' '\n' | grep "text=\"[^\"]*$1" | head -1 |
        sed -n 's/.*bounds="\[\([0-9]*\),\([0-9]*\)\]\[\([0-9]*\),\([0-9]*\)\]".*/\1 \2 \3 \4/p')
    if [ -z "$b" ]; then
        echo "  알림을 찾지 못함: $1"
        FAIL=1
        $E shell cmd statusbar collapse
        return
    fi
    set -- $b
    $E shell input tap $((($1 + $3) / 2)) $((($2 + $4) / 2))
    sleep 3
}
reminder() { post reminder; tap "$($E shell cmd locale get-app-locales $P | grep -q ko && echo '한 줄' || echo 'one line')"; }
backup() { post backup; tap "백업 알림 시험"; }
home() { $E shell input keyevent HOME; sleep 1; }
run() {
    echo "== $1"
    $E shell am force-stop $P
    $E shell cmd notification cancel_all >/dev/null 2>&1
    sleep 1
    for step in $2; do
        $step
        n=$(cnt)
        echo "  $step 뒤 앱 화면 ${n}개"
        [ "$n" = 1 ] || FAIL=1
        home
    done
}

run "런처 → 백업 알림" "launcher backup"
run "백업 알림으로 시작 → 쓰기 알림 → 백업 알림 → 런처" "backup reminder backup launcher"
run "쓰기 알림으로 시작 → 백업 알림 → 쓰기 알림 → 런처" "reminder backup reminder launcher"
[ $FAIL = 0 ] && echo "통과: 어느 순서로 열어도 화면 하나" || { echo "실패: 화면이 쌓이거나 알림을 못 찾음"; exit 1; }
