package org.sjhstudio.diary.core.backup

/** 쓰레기를 치운 뒤 남은 메모리. 큰 백업이 메모리에 쌓이지 않는지 두 플랫폼에서 잰다. */
internal expect fun retainedBytes(): Long
