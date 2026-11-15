package org.sjhstudio.diary.core.data

import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import okio.IOException
import org.sjhstudio.diary.core.datastore.PromptPrefs
import org.sjhstudio.diary.core.datastore.PromptStore
import org.sjhstudio.diary.core.model.currentDate
import org.sjhstudio.diary.core.model.orElseOnFailure

/**
 * 스토어 리뷰 창. Android는 Play 앱 안 리뷰, iOS는 StoreKit
 * 창을 띄울지와 별점 결과는 스토어가 정하고 앱은 모름
 */
fun interface StoreReview {
    suspend fun request()
}

/**
 * 의견 카드와 리뷰 창을 언제 보일지 정하는 곳(스펙 007)
 * 일기 내용은 읽지 않고 새로 쓴 편 수만 봄
 * 기록을 읽거나 쓰지 못하면 덜 묻는 쪽으로 판단
 */
class PromptRepository(
    private val store: PromptStore,
    private val writtenSince: (Instant) -> Flow<Int>,
    private val review: StoreReview,
    private val clock: Clock = Clock.System,
    private val timeZone: () -> TimeZone = { TimeZone.currentSystemDefault() }
) {
    /** 첫 사용일. 이 기능이 든 버전을 처음 연 때 한 번만 적음 */
    suspend fun start() = quietly(Unit) { store.firstUse(clock.now()) }

    /** 공간이 차 닫음을 못 적어도 이 프로세스 안에서는 카드가 다시 보이지 않게 함(광고의 하루 한 번과 같은 방식) */
    private val doneInProcess = MutableStateFlow(false)

    /** 홈 맨 위 의견 카드. 백업 카드와의 순서는 화면이 정함(FR-014) */
    @OptIn(ExperimentalCoroutinesApi::class)
    val feedbackCard: Flow<Boolean> =
        combine(store.prefs, doneInProcess) { prefs, done -> prefs.takeUnless { done } }
            .flatMapLatest { prefs ->
                val first = prefs?.firstUseAt ?: return@flatMapLatest flowOf(false)
                // 날짜 조건도 새 일기를 쓸 때마다 다시 봄. 3일 전에 구독해도 그 뒤 쓴 일기로 카드가 뜸
                writtenSince(first).map { feedbackMayShow(prefs, first) && it >= FEEDBACK_ENTRIES }
            }
            .catch { if (it is IOException) emit(false) else throw it }
            .distinctUntilChanged()

    private fun feedbackMayShow(prefs: PromptPrefs, first: Instant) =
        !prefs.feedbackDone && prefs.reviewAskedDay != today() && passed(first, FEEDBACK_DAYS)

    /** 카드 기록과 리뷰 요청 판단이 엇갈리지 않게 한 번에 하나 */
    private val turn = Mutex()

    /** 카드가 화면에 나옴. 같은 날 리뷰 창을 막음 */
    suspend fun feedbackShown() = quietly(Unit) { turn.withLock { store.feedbackShown(today()) } }

    /** 카드 닫기. 다시 보이지 않음 */
    suspend fun feedbackClosed() = feedbackDone()

    /** 의견 설문을 엶. 카드든 편지든 그 뒤로 카드를 보이지 않음 */
    suspend fun surveyOpened() = feedbackDone()

    private suspend fun feedbackDone() {
        doneInProcess.value = true
        quietly(Unit) { store.feedbackDone() }
    }

    /**
     * 저장하고 목록으로 돌아왔는데 전면 광고 차례가 아니었을 때 부름
     * 의견 카드를 닫거나 설문을 연 뒤, 그날이 아닐 때만
     * 한 번만 요청. 요청한 날을 먼저 적어 앱이 꺼져도 다시 묻지 않음
     */
    suspend fun askReviewIfDue() = quietly(Unit) {
        if (takeReviewTurn()) review.request()
    }

    /** 요청할 차례면 그날을 적고 참. 리뷰 창은 잠금 밖에서 띄움 */
    private suspend fun takeReviewTurn(): Boolean = turn.withLock {
        val prefs = store.current()
        val first = prefs.firstUseAt ?: return@withLock false
        // 의견 카드가 남아 있는 동안은 묻지 않음. 카드 조건(3일·3편)이 리뷰 조건 안에 들어 있어 카드가 먼저
        val cardOpen = !prefs.feedbackDone && !doneInProcess.value
        if (cardOpen || prefs.reviewAskedDay != null || writtenSince(first).first() < REVIEW_ENTRIES) return@withLock false
        // 편 수를 기다리는 사이 자정이 지날 수 있어 날짜는 그 뒤에 읽음
        val today = today()
        if (prefs.feedbackShownDay == today || !passed(first, REVIEW_DAYS)) return@withLock false
        store.reviewAsked(today)
        true
    }

    /** 첫 사용일부터 [days]일이 지났는지. 시각이 뒤로 가면 지나지 않은 것으로 봄 */
    private fun passed(first: Instant, days: Int): Boolean {
        val zone = timeZone()
        return currentDate(clock, zone) >= first.toLocalDateTime(zone).date.plus(days, DateTimeUnit.DAY)
    }

    private fun today(): String = currentDate(clock, timeZone()).toString()

    /** 곁일. 실패로 앱이 꺼지거나 저장 흐름이 막히면 안 됨 */
    private suspend fun <T> quietly(failed: T, block: suspend () -> T): T = orElseOnFailure(failed) { block() }

    private companion object {
        const val FEEDBACK_DAYS = 3
        const val FEEDBACK_ENTRIES = 3
        const val REVIEW_DAYS = 7
        const val REVIEW_ENTRIES = 5
    }
}
