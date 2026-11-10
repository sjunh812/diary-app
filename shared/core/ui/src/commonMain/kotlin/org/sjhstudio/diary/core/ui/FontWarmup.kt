package org.sjhstudio.diary.core.ui

import org.sjhstudio.diary.core.model.DiaryFont

/**
 * 시작 화면이 떠 있는 동안 고른 글꼴을 뒤에서 읽어 둔다.
 * 고른 글꼴은 앱 전체에 써서 첫 화면이 글자를 잴 때 파일을 읽는다.
 * 4.6MB 손글씨 글꼴은 그때 화면 스레드가 0.3초 멈췄다(2026-11-10 출시용 빌드 측정).
 * 안드로이드는 Compose 글꼴 캐시를 미리 채운다(`FontFamily.Resolver.preload`).
 * iOS는 리소스 글꼴을 이미 바로 읽어서 하지 않는다.
 */
fun interface FontWarmup {
    /** 실패해도 던지지 않는다. 화면이 그때 읽을 뿐이다. */
    suspend fun warm(font: DiaryFont)
}
