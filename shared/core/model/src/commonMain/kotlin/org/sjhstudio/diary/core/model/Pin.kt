package org.sjhstudio.diary.core.model

/**
 * 잠금 비밀번호 4자리(contracts/lock.md)
 * 글자 배열로 들고 있다가 쓰고 나면 [clear]로 지움. String은 지울 수 없어 메모리에 남음
 */
class Pin private constructor(private val digits: CharArray) {
    /** 확인값을 만들 ASCII 바이트. 받은 쪽이 쓰고 나서 지움 */
    fun asciiBytes(): ByteArray = ByteArray(LENGTH) { digits[it].code.toByte() }

    fun clear() = digits.fill('\u0000')

    override fun toString(): String = "Pin(****)"

    companion object {
        const val LENGTH = 4

        /** ASCII 숫자 정확히 4자리가 아니면 null. [digits]는 복사해 두니 부른 쪽이 따로 지움 */
        fun of(digits: CharArray): Pin? = if (digits.size == LENGTH && digits.all { it in '0'..'9' }) Pin(digits.copyOf()) else null

        /** 옛 앱 설정의 비밀번호처럼 이미 글자열로 온 값 */
        fun of(text: String): Pin? = of(text.toCharArray())
    }
}
