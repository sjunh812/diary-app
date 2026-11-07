package org.sjhstudio.diary.core.ui

import androidx.compose.runtime.Composable

/**
 * 시스템 공유 창으로 글을 낸다(FR-019).
 * 사용자가 고른 곳으로만 나간다(헌법 II).
 */
@Composable
expect fun rememberShareText(): (String) -> Unit
