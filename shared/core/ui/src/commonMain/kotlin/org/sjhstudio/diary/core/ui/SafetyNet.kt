package org.sjhstudio.diary.core.ui

/**
 * 안전망이 받은 예외를 넘길 곳
 * 앱이 시작할 때 비정상 종료 보고에 이어 붙임. 조용히 삼키면 고칠 곳을 알 수 없음
 */
object SafetyNet {
    var report: (Exception) -> Unit = {}
}
