package org.sjhstudio.diary.core.platform

/** 의견 메일에 넣을 기기 정보. 사용자를 가리키는 값은 없음 */
data class DeviceInfo(val os: String, val model: String)

expect fun deviceInfo(): DeviceInfo
