package org.sjhstudio.diary.core.ui

/**
 * 앱을 나가도 백업과 복원이 이어지는지
 * Android는 시스템 작업이 끝까지 지켜 주고 끝나면 알림을 보냄
 * iOS는 앱이 백그라운드로 가면 곧 멈추고 다시 열 때 이어서 하므로 화면 안내가 달라야 함
 */
expect val workContinuesInBackground: Boolean
