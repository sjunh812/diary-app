package org.sjhstudio.diary.core.data.lock

import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import org.sjhstudio.diary.core.model.LockSettings
import org.sjhstudio.diary.core.model.Pin
import org.sjhstudio.diary.core.model.RelockAfter

/**
 * 잠금 비밀번호와 잠금 설정(contracts/lock.md).
 * 넘겨받은 [Pin]은 쓰고 나서 지운다.
 */
interface LockRepository {
    val settings: Flow<LockSettings>

    /**
     * 잠금은 켜졌는데 확인값이나 기기 키가 없어 비밀번호를 대어 볼 수 없는가.
     * 그러면 폰 화면 잠금 확인 뒤 새 비밀번호를 정한다.
     */
    suspend fun needsNewPin(): Boolean

    /**
     * 새 salt로 확인값을 만들어 두고 잠금을 켠다.
     * 틀린 횟수도 지운다.
     * 기기 키를 만들지 못하거나 파일에 적지 못하면 예외가 난다.
     */
    suspend fun setPin(pin: Pin)

    suspend fun verify(pin: Pin, now: Instant): VerifyResult

    /** 지금 비밀번호가 맞을 때만 잠금을 끈다. */
    suspend fun disable(current: Pin, now: Instant): VerifyResult

    /**
     * 비밀번호를 대어 볼 수 없고 폰에도 화면 잠금이 없어 새 비밀번호를 정할 수 없을 때 잠금을 끈다.
     * 키가 정말 없어졌을 때만 끄고, 끄면 true다.
     * 키가 있는데 잠깐 못 쓰는 것이면 끄지 않는다.
     */
    suspend fun turnOffWithoutPin(): Boolean

    suspend fun setBiometric(enabled: Boolean)

    suspend fun setRelockAfter(value: RelockAfter)
}

sealed interface VerifyResult {
    data object Ok : VerifyResult

    data class Wrong(val remainingBeforeWait: Int) : VerifyResult

    data class Waiting(val until: Instant) : VerifyResult

    /**
     * 키가 없어 확인할 수 없다.
     * 폰 화면 잠금 흐름으로 보낸다.
     */
    data object KeyMissing : VerifyResult

    /**
     * 맞았지만 잠금을 끈 것을 파일에 적지 못했다.
     * 잠금은 그대로 켜져 있다.
     */
    data object NotSaved : VerifyResult
}
