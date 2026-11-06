package org.sjhstudio.diary.app.gate

import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.waitUntilExactlyOneExists
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.sjhstudio.diary.app.Startup
import org.sjhstudio.diary.app.lock.LockController
import org.sjhstudio.diary.core.data.lock.DefaultLockRepository
import org.sjhstudio.diary.core.data.lock.LockRepository
import org.sjhstudio.diary.core.datastore.LockStore
import org.sjhstudio.diary.core.datastore.SettingsStore
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.Pin
import org.sjhstudio.diary.core.model.RelockAfter
import org.sjhstudio.diary.core.platform.AuthPrompt
import org.sjhstudio.diary.core.platform.AuthResult
import org.sjhstudio.diary.core.platform.BiometricStatus
import org.sjhstudio.diary.core.platform.DeviceAuthenticator
import org.sjhstudio.diary.core.platform.NoTelemetrySink
import org.sjhstudio.diary.core.platform.Telemetry
import org.sjhstudio.diary.core.testing.FakePinKeyStore
import org.sjhstudio.diary.core.testing.InMemoryDataStore
import org.sjhstudio.diary.feature.lock.lockFeatureModule

// 잠금 관문(스펙 001 T055, contracts/lock.md, research R13).
// 잠긴 동안 안쪽 화면은 그리지 않고, 열면 보던 상태 그대로 돌아온다.
// 생체 인증을 취소하면 숫자판이 남는다.
@OptIn(ExperimentalTestApi::class)
class LockGateUiTest {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val lockStore = LockStore(InMemoryDataStore<Preferences>(emptyPreferences()))
    private val repository = DefaultLockRepository(lockStore, FakePinKeyStore())
    private val controller = LockController(repository, scope)

    private fun start(biometric: Boolean = false) {
        runBlocking {
            repository.setPin(checkNotNull(Pin.of(PIN)))
            repository.setRelockAfter(RelockAfter.IMMEDIATELY)
            repository.setBiometric(biometric)
        }
        val startup = Startup({}, SettingsStore(InMemoryDataStore<Preferences>(emptyPreferences())), lockStore, scope)
        startKoin {
            modules(
                lockFeatureModule,
                module {
                    single<LockRepository> { repository }
                    single { controller }
                    single { startup }
                    single { Telemetry(NoTelemetrySink, strict = true) }
                }
            )
        }
        startup.start()
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
        scope.cancel()
    }

    @Test
    fun content_is_hidden_while_locked_and_comes_back_as_it_was() = runComposeUiTest {
        start()
        setContent {
            DiaryTheme(darkTheme = false) { LockGate(authenticator = FakeAuthenticator(BiometricStatus.Unavailable)) { Counter() } }
        }

        // 처음에는 잠겨 있고 안쪽 화면이 없다.
        waitUntilKeypad()
        onNodeWithTag(COUNTER).assertDoesNotExist()

        enterPin()
        waitUntilExactlyOneExists(hasTestTag(COUNTER), TIMEOUT)
        repeat(3) { onNodeWithTag(COUNTER).performClick() }
        onNodeWithTag(COUNTER).assertTextEquals("3")

        // 뒤로 갔다 오면 바로 잠긴다(바로 잠금).
        // 잠긴 동안 안쪽 화면은 그리지 않는다.
        runOnIdle {
            controller.onBackground()
            controller.onForeground()
        }
        waitUntilKeypad()
        onNodeWithTag(COUNTER).assertDoesNotExist()

        // 다시 열면 보던 화면의 상태가 그대로다.
        enterPin()
        waitUntilExactlyOneExists(hasTestTag(COUNTER), TIMEOUT)
        onNodeWithTag(COUNTER).assertTextEquals("3")
    }

    @Test
    fun canceling_biometrics_leaves_the_keypad() = runComposeUiTest {
        start(biometric = true)
        val authenticator = FakeAuthenticator(BiometricStatus.Available, biometricResult = AuthResult.Canceled)
        setContent { DiaryTheme(darkTheme = false) { LockGate(authenticator = authenticator) { Counter() } } }

        waitUntilKeypad()
        waitUntil(timeoutMillis = TIMEOUT) { authenticator.biometricAsked == 1 }
        waitForIdle()
        // 취소해도 열리지 않고 숫자판으로 열 수 있다.
        onNodeWithTag(COUNTER).assertDoesNotExist()
        enterPin()
        waitUntilExactlyOneExists(hasTestTag(COUNTER), TIMEOUT)
        assertEquals(1, authenticator.biometricAsked)
    }

    @Test
    fun biometrics_open_without_the_pin() = runComposeUiTest {
        start(biometric = true)
        val authenticator = FakeAuthenticator(BiometricStatus.Available, biometricResult = AuthResult.Success)
        setContent { DiaryTheme(darkTheme = false) { LockGate(authenticator = authenticator) { Counter() } } }

        waitUntilExactlyOneExists(hasTestTag(COUNTER), TIMEOUT)
        assertEquals(1, authenticator.biometricAsked)
    }

    private fun ComposeUiTest.waitUntilKeypad() = waitUntilExactlyOneExists(digit('0'), TIMEOUT)

    private fun ComposeUiTest.enterPin() = PIN.forEach { onNode(digit(it)).performClick() }

    private fun digit(digit: Char) = hasText(digit.toString()) and hasClickAction()

    private class FakeAuthenticator(private val status: BiometricStatus, private val biometricResult: AuthResult = AuthResult.Failed) :
        DeviceAuthenticator {
        var biometricAsked = 0

        override fun biometricStatus() = status

        override fun isDeviceSecure() = true

        override suspend fun authenticateBiometric(prompt: AuthPrompt): AuthResult {
            biometricAsked++
            return biometricResult
        }

        override suspend fun authenticateDeviceOwner(prompt: AuthPrompt) = AuthResult.Canceled
    }

    private companion object {
        const val PIN = "2580"
        const val COUNTER = "counter"
        const val TIMEOUT = 5_000L
    }
}

/**
 * 보던 화면의 상태 대신 쓰는 숫자.
 * 저장 상태로 두어 잠금을 지나도 남는지 본다.
 */
@Composable
private fun Counter() {
    var count by rememberSaveable { mutableIntStateOf(0) }
    BasicText(count.toString(), Modifier.testTag("counter").clickable { count++ })
}
