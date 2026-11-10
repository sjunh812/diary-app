package org.sjhstudio.diary.feature.plus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.sjhstudio.diary.core.data.PlanRepository
import org.sjhstudio.diary.core.data.PurchaseRepository
import org.sjhstudio.diary.core.model.Plan
import org.sjhstudio.diary.core.model.ProductId
import org.sjhstudio.diary.core.model.PurchaseResult
import org.sjhstudio.diary.core.model.StoreProduct
import org.sjhstudio.diary.core.ui.launchSafely
import org.sjhstudio.diary.core.ui.stateInSafely

data class PlusUiState(
    val products: List<StoreProduct> = emptyList(),
    val loading: Boolean = true,
    val busy: Boolean = false,
    /** 방금 결제가 실패함. 광고 제거 시트는 토스트가 없는 화면에서도 열려 시트가 직접 보임. */
    val buyFailed: Boolean = false,
    /** 방금 구매 복원의 결과. 찾지 못했거나 실패했을 때만 있고, 시트가 직접 보임. */
    val restoreNotice: PlusMessage? = null
) {
    fun price(id: ProductId): String? = products.firstOrNull { it.id == id }?.price
}

enum class PlusMessage { Failed, Pending, Restored, RestoreNone, RestoreFailed }

/**
 * 결제 화면과 시트의 상태.
 * 취소는 아무 말 없이 그대로 돌아옴.
 */
class PlusViewModel(plans: PlanRepository, private val purchases: PurchaseRepository) : ViewModel() {
    val plan: StateFlow<Plan> = plans.plan.stateInSafely(viewModelScope, SharingStarted.Eagerly, plans.plan.value)
    val manageUrl: String = purchases.manageUrl

    private val _state = MutableStateFlow(PlusUiState())
    val state: StateFlow<PlusUiState> = _state.asStateFlow()

    private val _messages = Channel<PlusMessage>(Channel.BUFFERED)
    val messages: Flow<PlusMessage> = _messages.receiveAsFlow()

    // 결제가 끝남(이미 가진 것 포함). 화면 콜백을 쥐지 않고 이벤트로 보내 화면이 다시 만들어져도 받음
    private val _purchased = Channel<ProductId>(Channel.BUFFERED)
    val purchased: Flow<ProductId> = _purchased.receiveAsFlow()

    private var loading: Job? = null

    init {
        load()
    }

    /**
     * 가격을 받음.
     * 받는 중이면 스토어를 다시 부르지 않음.
     * 다시 받기에 실패해도 받아 둔 가격은 그대로 둠.
     */
    fun load() {
        if (loading?.isActive == true) return
        loading =
            viewModelScope.launchSafely {
                _state.update { it.copy(loading = true) }
                // 스토어가 예외를 던져도 불러오는 중에 멈추지 않고 다시 시도할 수 있게 둠
                val products =
                    try {
                        purchases.products()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        emptyList()
                    }
                _state.update { current -> current.copy(products = products.ifEmpty { current.products }, loading = false) }
            }
    }

    fun buy(id: ProductId) {
        if (_state.value.busy) return
        // 이미 가진 혜택을 두 번 결제하지 않게 스토어로 보내지 않음
        if (owns(id)) {
            _purchased.trySend(id)
            return
        }
        viewModelScope.launchSafely {
            val result = attempt { purchases.buy(id) }
            _state.update { it.copy(buyFailed = result == PurchaseResult.FAILED, restoreNotice = null) }
            when (result) {
                PurchaseResult.DONE -> _purchased.send(id)
                PurchaseResult.FAILED -> _messages.send(PlusMessage.Failed)
                PurchaseResult.PENDING -> _messages.send(PlusMessage.Pending)
                PurchaseResult.CANCELLED, PurchaseResult.NOTHING -> Unit
            }
        }
    }

    /** 시트를 닫음. 다시 열 때 지난 안내가 남아 있지 않게 지움. */
    fun failureShown() = _state.update { it.copy(buyFailed = false, restoreNotice = null) }

    private fun owns(id: ProductId): Boolean = when (id) {
        ProductId.AD_FREE -> plan.value != Plan.FREE
        ProductId.PLUS_YEARLY -> plan.value == Plan.PLUS
    }

    /** 스토어가 예외를 던져도 실패로 돌려 버튼이 눌린 채로 멈추지 않게 함. */
    private suspend fun attempt(call: suspend () -> PurchaseResult): PurchaseResult {
        _state.update { it.copy(busy = true) }
        return try {
            call()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            PurchaseResult.FAILED
        } finally {
            _state.update { it.copy(busy = false) }
        }
    }

    fun restore() {
        if (_state.value.busy) return
        viewModelScope.launchSafely {
            val message =
                when (attempt { purchases.restore() }) {
                    PurchaseResult.DONE -> PlusMessage.Restored
                    PurchaseResult.FAILED -> PlusMessage.RestoreFailed
                    PurchaseResult.CANCELLED, PurchaseResult.NOTHING, PurchaseResult.PENDING -> PlusMessage.RestoreNone
                }
            _state.update { it.copy(buyFailed = false, restoreNotice = message.takeUnless { message == PlusMessage.Restored }) }
            _messages.send(message)
        }
    }
}
