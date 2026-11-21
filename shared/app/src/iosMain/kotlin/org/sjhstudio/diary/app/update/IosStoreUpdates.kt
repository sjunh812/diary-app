package org.sjhstudio.diary.app.update

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.withContext
import org.sjhstudio.diary.core.data.StoreUpdates
import org.sjhstudio.diary.core.model.AppVersion
import org.sjhstudio.diary.core.model.InstallStep
import org.sjhstudio.diary.core.model.StoreCheck
import platform.Foundation.NSLocale
import platform.Foundation.countryCode
import platform.Foundation.currentLocale

/** Swift가 구현하는 App Store 화면 다리(AppUpdateBridge.swift). 화면 스레드에서 부름 */
interface IosUpdateBridge {
    /** 앞에 나와 있는 화면 위에 이 앱의 App Store 상품 화면을 띄움. 띄웠으면 참 */
    fun present(appId: String): Boolean
}

/**
 * iOS 새 버전(스펙 009)
 * 공식 API가 없어 App Store 공개 조회로 알고, 받기와 설치는 App Store가 해 진행을 모름
 * 조회에는 앱 번호와 기기 지역만 보냄(헌법 II)
 */
class IosStoreUpdates(private val bridge: IosUpdateBridge?, override val installed: String) : StoreUpdates {
    // 처음 확인을 마칠 때까지 홈 맨 위 칸이 기다려 오래 걸리지 않게 끊음
    private val client by lazy {
        HttpClient(Darwin) {
            expectSuccess = true
            install(HttpTimeout) { requestTimeoutMillis = TIMEOUT_MILLIS }
        }
    }

    // 네트워크로 묻고 분당 약 20회 한도가 있어 하루 한 번
    override val checkEvery: Duration = 1.days

    override suspend fun check(): StoreCheck {
        val country = NSLocale.currentLocale.countryCode?.lowercase() ?: DEFAULT_COUNTRY
        val body =
            client.get(AppStoreLookup.URL) {
                parameter("id", AppStoreLookup.APP_ID)
                parameter("country", country)
            }.bodyAsText()
        val version = AppStoreLookup.version(body) ?: return StoreCheck.None
        return if (AppVersion.isNewer(version, installed)) StoreCheck.Available(version) else StoreCheck.None
    }

    override val installs: Flow<InstallStep> = emptyFlow()

    override suspend fun start(): Boolean = withContext(Dispatchers.Main) { bridge?.present(AppStoreLookup.APP_ID) ?: false }

    // App Store가 설치하며 앱을 다시 켬
    override fun restart() = Unit

    override fun openStorePage() {
        bridge?.present(AppStoreLookup.APP_ID)
    }

    private companion object {
        const val DEFAULT_COUNTRY = "us"
        const val TIMEOUT_MILLIS = 10_000L
    }
}
