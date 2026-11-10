package org.sjhstudio.diary.core.data.lock

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import org.sjhstudio.diary.core.model.LockSettings
import org.sjhstudio.diary.core.model.Pin
import org.sjhstudio.diary.core.model.RelockAfter

/**
 * 잠금 비밀번호와 잠금 설정(contracts/lock.md)
 * 넘겨받은 [Pin]은 쓰고 나서 지움
 */
interface LockRepository {
    val settings: Flow<LockSettings>

    /**
     * 잠금은 켜졌는데 확인값이나 기기 키가 없어 비밀번호를 대어 볼 수 없는지
     * 그러면 휴대폰 화면 잠금 확인 뒤 새 비밀번호를 정함
     */
    suspend fun needsNewPin(): Boolean

    /**
     * 새 salt로 확인값을 만들어 두고 잠금을 켬
     * 틀린 횟수도 지움
     * 기기 키를 만들지 못하거나 파일에 못 적으면 예외
     */
    suspend fun setPin(pin: Pin)

    suspend fun verify(pin: Pin, now: Instant): VerifyResult

    /** 지금 비밀번호가 맞을 때만 잠금을 끔 */
    suspend fun disable(current: Pin, now: Instant): VerifyResult

    /**
     * 비밀번호를 대어 볼 수 없고 휴대폰에도 화면 잠금이 없어 새 비밀번호를 정할 수 없을 때 잠금을 끔
     * 키가 정말 없어졌을 때만 끄고, 끄면 true
     * 키가 있는데 잠깐 못 쓰는 것이면 끄지 않음
     */
    suspend fun turnOffWithoutPin(): Boolean

    suspend fun setBiometric(enabled: Boolean)

    suspend fun setRelockAfter(value: RelockAfter)

    companion object {
        /**
         * 다섯 번 틀린 뒤 기다리는 시간
         * 화면은 남은 시간을 이보다 길게 보이지 않음(시계를 뒤로 돌려도 숫자판이 막히지 않게)
         */
        val WAIT: Duration = 30.seconds
    }
}

sealed interface VerifyResult {
    data object Ok : VerifyResult

    data class Wrong(val remainingBeforeWait: Int) : VerifyResult

    data class Waiting(val until: Instant) : VerifyResult

    /**
     * 키가 없어 확인할 수 없음
     * 휴대폰 화면 잠금 흐름으로 보냄
     */
    data object KeyMissing : VerifyResult

    /**
     * 맞았지만 잠금을 끈 것을 파일에 적지 못함
     * 잠금은 그대로 켜져 있음
     */
    data object NotSaved : VerifyResult
}
