package org.sjhstudio.diary.app

import kotlin.time.Duration
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import org.sjhstudio.diary.app.update.IosStoreUpdates
import org.sjhstudio.diary.core.data.AppUpdateRepository
import org.sjhstudio.diary.core.data.RemoteValues
import org.sjhstudio.diary.core.data.StoreUpdates
import org.sjhstudio.diary.core.model.StoreCheck
import platform.Foundation.NSUserDefaults

private const val UPDATE_ARGUMENT = "diaryUpdate"
private const val FAKE_VERSION = "99.0"

/**
 * 개발 빌드에서만 새 버전을 흉내(스펙 009)
 * 실행 인자 `-diaryUpdate available|note|required`. [업데이트]는 실제 App Store 화면을 띄움
 * 출시 빌드는 부르지 않음
 */
internal fun applyDebugUpdate() {
    val mode = NSUserDefaults.standardUserDefaults.stringForKey(UPDATE_ARGUMENT) ?: return
    loadKoinModules(
        module {
            single<StoreUpdates> {
                val real = IosStoreUpdates(IosUpdate.bridge, "0")
                object : StoreUpdates by real {
                    // 흉내 값을 하루 기억하면 인자를 빼도 남아 늘 다시 물음
                    override val checkEvery: Duration = Duration.ZERO

                    override suspend fun check(): StoreCheck = StoreCheck.Available(FAKE_VERSION)
                }
            }
            single<RemoteValues> {
                RemoteValues { key ->
                    when {
                        mode == "required" && key == AppUpdateRepository.KEY_MIN_VERSION -> FAKE_VERSION
                        mode == "note" && key == AppUpdateRepository.KEY_NOTE_VERSION -> FAKE_VERSION
                        mode == "note" && key == AppUpdateRepository.KEY_NOTE_PREFIX + "ko" -> "결제 뒤에도 광고가 보이던 문제를 고쳤어요."
                        mode == "note" && key == AppUpdateRepository.KEY_NOTE_PREFIX + "en" -> "Fixed ads showing after purchase."
                        else -> IosUpdate.remote.string(key)
                    }
                }
            }
        }
    )
}
