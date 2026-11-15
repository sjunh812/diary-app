package org.sjhstudio.diary.core.data

import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
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
import kotlinx.coroutines.withContext
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
 * 창을 실제로 보일지와 별점 결과는 스토어가 정하고 앱은 모름. 횟수 한도도 스토어가 둠
 */
fun interface StoreReview {
    /** 띄우기 바로 앞에 [stillDue]로 다시 보고 참일 때만 띄움. 띄우기를 시작했으면 참 */
    suspend fun request(stillDue: suspend () -> Boolean): Boolean
}

/**
 * 의견 카드와 리뷰 창을 언제 보일지 정하는 곳(스펙 007)
 * 일기 내용은 읽지 않고 새로 쓴 편 수만 봄
 * 기록을 읽지 못하면 묻지 않음. 리뷰 요청일을 못 적으면 그 프로세스 안에서만 다시 묻지 않음
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

    /**
     * 이 프로세스에서 리뷰를 물은 날. 띄우기 직전 Mutex 안에서 잡음
     * 기기 기록 전(창이 떠 있는 동안, 공간이 차 못 적은 때)에도 다시 묻지 않음
     * 같은 날 의견 카드도 이 값으로 미룸
     */
    private val askedDayInProcess = MutableStateFlow<String?>(null)

    /** 홈 맨 위 의견 카드. 백업 카드와의 순서는 화면이 정함(FR-014) */
    @OptIn(ExperimentalCoroutinesApi::class)
    val feedbackCard: Flow<Boolean> =
        combine(store.prefs, doneInProcess, askedDayInProcess) { prefs, done, askedDay ->
            prefs.takeUnless { done }?.let { it to askedDay }
        }
            .flatMapLatest { state ->
                val (prefs, askedDay) = state ?: return@flatMapLatest flowOf(false)
                val first = prefs.firstUseAt ?: return@flatMapLatest flowOf(false)
                // 날짜 조건도 새 일기를 쓸 때마다 다시 봄. 3일 전에 구독해도 그 뒤 쓴 일기로 카드가 뜸
                writtenSince(first).map { feedbackMayShow(prefs, askedDay, first) && it >= FEEDBACK_ENTRIES }
            }
            .catch { if (it is IOException) emit(false) else throw it }
            .distinctUntilChanged()

    // 리뷰를 먼저 물은 날(백업 카드가 맨 위를 차지한 동안)은 카드를 다음 날로 미룸
    private fun feedbackMayShow(prefs: PromptPrefs, askedDay: String?, first: Instant) =
        !prefs.feedbackDone && today() !in setOf(prefs.reviewAskedDay, askedDay) && passed(first, FEEDBACK_DAYS)

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
     * 보인 의견 카드가 남아 있지 않고, 카드가 보인 날이 아닐 때만
     * 기기 기록은 창을 띄운 뒤. 그사이 앱이 꺼져 한 번 더 물어도 스토어 횟수 한도가 거름(Apple 샘플·Play 지침)
     */
    suspend fun askReviewIfDue() = quietly(Unit) {
        // 차례가 아니면 스토어에 정보를 묻지 않음
        if (turn.withLock { reviewDueDay() } == null) return@quietly
        // 겹쳐 부른 다른 요청이 잡은 날을 풀지 않게, 이 요청이 잡은 날만 다룸
        var claimed: String? = null
        val shown =
            review.request(stillDue = {
                // 정보를 받는 사이 카드가 나왔을 수 있어 띄우기 직전 다시 보고 그날을 잡음
                quietly(false) {
                    turn.withLock {
                        claimed = reviewDueDay()?.also { askedDayInProcess.value = it }
                        claimed != null
                    }
                }
            })
        val day = claimed ?: return@quietly
        if (shown) {
            // 띄운 뒤 화면을 옮겨 거둬져도 기록은 끝까지
            withContext(NonCancellable) { store.reviewAsked(day) }
        } else {
            // 판단 뒤 화면이 없어 못 띄움. 다음에 다시 물음
            askedDayInProcess.value = null
        }
    }

    /** 요청할 차례면 그날. 잠금 안에서 부름 */
    private suspend fun reviewDueDay(): String? {
        val prefs = store.current()
        val first = prefs.firstUseAt
        // 보인 의견 카드가 남아 있는 동안은 묻지 않음. 한 번도 못 본 카드(백업 카드가 맨 위)로는 막지 않음
        val cardOpen = prefs.feedbackShownDay != null && !prefs.feedbackDone && !doneInProcess.value
        val blocked = cardOpen || prefs.reviewAskedDay != null || askedDayInProcess.value != null
        if (first == null || blocked || writtenSince(first).first() < REVIEW_ENTRIES) return null
        // 편 수를 기다리는 사이 자정이 지날 수 있어 날짜는 그 뒤에 읽음
        return today().takeUnless { prefs.feedbackShownDay == it || !passed(first, REVIEW_DAYS) }
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
