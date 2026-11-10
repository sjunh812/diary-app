package org.sjhstudio.diary.core.migration

import kotlin.coroutines.cancellation.CancellationException
import okio.FileSystem
import okio.IOException
import org.sjhstudio.diary.core.data.lock.LockRepository
import org.sjhstudio.diary.core.datastore.LockStore
import org.sjhstudio.diary.core.model.MigrationStep
import org.sjhstudio.diary.core.model.Pin

/**
 * 옛 앱(1.7.x)의 잠금을 새 잠금으로 잇는다(contracts/legacy-mapping.md "설정", research R16).
 * 스플래시 동안 기록 이전보다 먼저 돈다.
 * 잠금 화면을 먼저 띄우려면 확인값이 먼저 있어야 해서다.
 * 옛 `pref.xml`은 읽기만 한다.
 *
 * 이전을 시작하기 전(`NotStarted`)에만 한다.
 * 이전이 시작된 뒤에는 새 앱의 잠금 설정이 기준이라, 사용자가 끈 잠금을 옛 파일로 다시 켜지 않는다.
 */
class LegacyLockBootstrap(
    private val fileSystem: FileSystem,
    private val files: LegacyFiles,
    private val migrationStep: suspend () -> MigrationStep,
    private val lock: LockRepository,
    private val lockStore: LockStore
) {
    /** 잠금을 켰으면 true다. */
    suspend fun run(): Boolean {
        if (migrationStep() != MigrationStep.NOT_STARTED || !fileSystem.exists(files.prefXml)) return false
        // 읽지 못하면 잠금이 있었는지 알 수 없다.
        // 풀어 두는 대신 잠근 채 새 비밀번호를 정하게 한다(헌법 II).
        val prefs = readPrefs() ?: return lockWithoutPin(biometric = false)
        if (prefs[USE_PW] != true) return false
        val biometric = prefs[FINGER_PRINT] == true
        val pin = (prefs[PASSWORD] as? String)?.let(Pin::of) ?: return lockWithoutPin(biometric)
        try {
            lock.setPin(pin)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            // 기기 키를 만들지 못했다.
            // 잠근 채 새 비밀번호를 정하게 한다.
            return lockWithoutPin(biometric)
        }
        lock.setBiometric(biometric)
        return true
    }

    private fun readPrefs(): Map<String, Any>? = try {
        LegacyPrefs.parse(fileSystem.read(files.prefXml) { readUtf8() })
    } catch (_: IOException) {
        null
    }

    /**
     * 잠금은 켜고 확인값은 비운다.
     * 잠금 관문이 폰 화면 잠금 확인 뒤 새 비밀번호를 정하게 한다.
     */
    private suspend fun lockWithoutPin(biometric: Boolean): Boolean {
        lockStore.update { it.copy(enabled = true, pinMac = null, pinSalt = null, biometricEnabled = biometric) }
        return true
    }

    private companion object {
        const val USE_PW = "use_pw"
        const val PASSWORD = "password"
        const val FINGER_PRINT = "finger_print"
    }
}
