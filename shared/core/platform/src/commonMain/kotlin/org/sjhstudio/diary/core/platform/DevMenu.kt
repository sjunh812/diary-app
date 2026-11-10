package org.sjhstudio.diary.core.platform

/** 개발 빌드에서만 등록되는 개발자 모드 입구. 출시 빌드에는 없어 설정에 줄이 생기지 않음 */
fun interface DevMenu {
    fun open()
}
