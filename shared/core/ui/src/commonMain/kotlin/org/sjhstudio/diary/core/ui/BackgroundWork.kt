package org.sjhstudio.diary.core.ui

/**
 * 앱을 나가도 백업과 복원이 이어지는지.
 * Android는 시스템 작업이 끝까지 지켜 주고 끝나면 알림을 보낸다.
 * iOS는 앱이 뒤로 가면 곧 멈추고 다시 열 때 이어서 하므로 화면에 다르게 말해야 한다.
 */
expect val workContinuesInBackground: Boolean
