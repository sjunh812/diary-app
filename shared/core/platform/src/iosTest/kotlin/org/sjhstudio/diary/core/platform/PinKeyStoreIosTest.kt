package org.sjhstudio.diary.core.platform

import kotlin.test.Test
import kotlin.test.assertEquals

// 잠금 확인값 계산(스펙 001 T056)
// 시험 실행기는 앱 묶음 없이 돌아 키체인을 쓸 수 없음(errSecNotAvailable)
// 키체인에 키를 두고 다시 읽는 일은 앱을 시뮬레이터에 올려 quickstart §4로 확인
class PinKeyStoreIosTest {
    /** RFC 4231 시험 2: 키 "Jefe", 데이터 "what do ya want for nothing?" */
    @Test
    fun hmac_matches_rfc_4231() {
        val mac = hmacSha256("Jefe".encodeToByteArray(), "what do ya want for nothing?".encodeToByteArray())
        assertEquals("5bdcc146bf60754e6a042426089575c75a003f089d2739839dec58b964ec3843", mac.toHexString())
    }
}
