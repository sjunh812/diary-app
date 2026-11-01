# 잠금 계약

구현은 `shared/core/data`(저장소), `shared/core/platform`(키, 인증), `shared/app`(관문), `shared/feature/lock`(화면)에 둔다. 대역은 `shared/core/testing`.

```kotlin
@JvmInline value class Pin(val digits: String)        // ASCII 숫자 정확히 4자리, 아니면 만들 수 없다

interface LockRepository {
    val settings: Flow<LockSettings>                   // enabled, biometricEnabled, relockAfter, hasPin, keyAvailable
    suspend fun setPin(pin: Pin)                       // 새 salt, 확인값 저장, enabled = true, 틀린 횟수 0
    suspend fun verify(pin: Pin, now: Instant): VerifyResult
    suspend fun disable(current: Pin): VerifyResult    // 맞을 때만 끔
    suspend fun setBiometric(enabled: Boolean)
    suspend fun setRelockAfter(value: RelockAfter)
}

sealed interface VerifyResult {
    data object Ok : VerifyResult
    data class Wrong(val remainingBeforeWait: Int) : VerifyResult
    data class Waiting(val until: Instant) : VerifyResult
    data object KeyMissing : VerifyResult              // 키가 없어 확인할 수 없음 → 폰 화면 잠금 흐름
}

interface PinKeyStore {                                // Android Keystore HMAC, iOS 키체인
    fun ensureKey(alias: String)
    fun hasKey(alias: String): Boolean
    fun mac(alias: String, data: ByteArray): ByteArray?   // 키가 없으면 null
}

interface DeviceAuthenticator {
    suspend fun biometricStatus(): BiometricStatus     // Available, NoneEnrolled, Unavailable
    suspend fun authenticateBiometric(): AuthResult    // 열기용: 생체만
    suspend fun isDeviceSecure(): Boolean              // 폰에 화면 잠금이 있는가
    suspend fun authenticateDeviceOwner(): AuthResult  // 분실용: 생체 또는 폰 PIN·패턴·암호
}

class LockController(/* LockRepository, AppForeground, Clock */) {
    val state: StateFlow<LockState>                    // Locked, Unlocked, Waiting(until)
    fun onColdStart()
    fun onBackground(monotonicMillis: Long)
    fun onForeground(monotonicMillis: Long)
    fun onUnlocked()
}
```

## 규칙

- 확인값은 `HMAC-SHA256(key, salt ‖ PIN의 ASCII 바이트)`이고 상수 시간으로 비교한다. PIN은 메모리에서도 쓰고 나면 바로 버린다(문자 배열을 0으로).
- 틀리면 `failed_attempts`를 올린다. 5번째에 `retry_after = now + 30초`, 그 사이 `verify`는 `Waiting`. 맞으면 0으로.
- 잠금 화면: 4칸 점, 숫자판, 생체 버튼(켜져 있고 쓸 수 있을 때), "비밀번호를 잊었어요". 열 때 생체 인증이 켜져 있으면 바로 띄운다.
  취소·실패·생체 잠김이면 숫자판이 그대로 남는다.
- 생체 인증을 켰는데 지금 쓸 수 없으면(`NoneEnrolled`, `Unavailable`) 생체 설정을 끄지 않고 잠금 설정에 "지금 기기에서 생체 인증을 쓸 수 없어요"를 보인다.
- 다시 잠그기: `onBackground`에서 단조 시각을 적고 `onForeground`에서 `relock_after`(`immediately` 0, `one_minute` 60초, `five_minutes` 300초)가 지났으면 잠근다.
  `cold_start`는 `onColdStart`에서만 잠근다. 모든 값에서 프로세스 새로 시작은 잠근다.
- 비밀번호를 잊었어요: `isDeviceSecure()`가 참이면 `authenticateDeviceOwner()` 성공 뒤 새 비밀번호 두 번 입력 → `setPin`. 거짓이면
  "휴대폰에 화면 잠금이 없어요. 화면 잠금이 없으면 비밀번호를 다시 정할 수 없어요"와 백업 안내(스펙 003)를 보인다.
- 키 없음(`KeyMissing`, 레거시 비밀번호가 잘못된 모양): 잠금 화면 대신 바로 폰 화면 잠금 확인 → 새 비밀번호. 폰에 화면 잠금이 없으면 잠금을 끄고 알린다.
- 잠금 파일(`lock.preferences_pb`)이 깨졌거나 읽히지 않으면 잠금을 끄지 않고 "키 없음"으로 읽어 위 흐름으로 보낸다. 깨진 파일 때문에 잠금이
  풀리는 일이 없게 한다(헌법 II, 2026-11-01 `LockStore`).
- 비밀번호 정하기 화면은 "휴대폰 잠금을 아는 사람도 이 방법으로 일기를 열 수 있어요"를 보인다(FR-019).
- 잠금을 끄거나 비밀번호를 바꾸려면 지금 비밀번호를 넣는다(잊었으면 위 흐름).
- 최근 앱 가림(FR-018): 잠금이 켜져 있을 때만. Android 13 이상 `setRecentsScreenshotEnabled(false)`, 7~12 `onPause` `FLAG_SECURE` 켬·`onResume` 끔,
  iOS 비활성 때 가림 창. 잠금이 꺼져 있으면 아무것도 하지 않는다.
- 사용 통계에는 `lock_enabled`(참·거짓), `relock_after` 값, 잠금 화면 결과(`ok`·`wrong`·`waiting`)만 보낸다. PIN과 횟수 외 값은 없다.
