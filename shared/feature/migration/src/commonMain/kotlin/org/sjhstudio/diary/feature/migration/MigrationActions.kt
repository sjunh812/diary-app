package org.sjhstudio.diary.feature.migration

import androidx.compose.runtime.Composable

/** 실패·일부만 화면의 원본 내보내기와 문의하기. 옮길 옛 앱이 없는 iOS는 null이다. */
class MigrationActions(val exportOriginal: () -> Unit, val contact: () -> Unit)

/** 내보내기·메일 결과를 토스트로 알리려고 넘기는 문구. */
fun interface MigrationMessage {
    fun show(message: String)
}

@Composable
expect fun rememberMigrationActions(onMessage: MigrationMessage): MigrationActions?
