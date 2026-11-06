package org.sjhstudio.diary.feature.plus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
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

data class PlusUiState(
    val products: List<StoreProduct> = emptyList(),
    val loading: Boolean = true,
    val busy: Boolean = false,
    /** 방금 결제가 실패했다. 광고 제거 시트는 토스트가 없는 화면에서도 열려서 시트가 직접 보인다. */
    val buyFailed: Boolean = false
) {
    fun price(id: ProductId): String? = products.firstOrNull { it.id == id }?.price
}

enum class PlusMessage { Failed, Restored, RestoreNone, RestoreFailed }

/** 결제 화면과 시트의 상태. 취소는 아무 말 없이 그대로 돌아온다. */
class PlusViewModel(plans: PlanRepository, private val purchases: PurchaseRepository) : ViewModel() {
    val plan: StateFlow<Plan> = plans.plan.stateIn(viewModelScope, SharingStarted.Eagerly, plans.plan.value)
    val manageUrl: String = purchases.manageUrl

    private val _state = MutableStateFlow(PlusUiState())
    val state: StateFlow<PlusUiState> = _state.asStateFlow()

    private val _messages = Channel<PlusMessage>(Channel.BUFFERED)
    val messages: Flow<PlusMessage> = _messages.receiveAsFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            // 스토어가 예외를 던져도 불러오는 중에 멈추지 않고 다시 시도할 수 있게 둔다.
            val products =
                try {
                    purchases.products()
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    emptyList()
                }
            _state.update { it.copy(products = products, loading = false) }
        }
    }

    fun buy(id: ProductId, onDone: () -> Unit = {}) {
        if (_state.value.busy) return
        // 이미 가진 혜택을 두 번 결제하지 않게 스토어로 보내지 않는다.
        if (owns(id)) {
            onDone()
            return
        }
        viewModelScope.launch {
            val result = attempt { purchases.buy(id) }
            _state.update { it.copy(buyFailed = result == PurchaseResult.FAILED) }
            when (result) {
                PurchaseResult.DONE -> onDone()
                PurchaseResult.FAILED -> _messages.send(PlusMessage.Failed)
                PurchaseResult.CANCELLED, PurchaseResult.NOTHING -> Unit
            }
        }
    }

    fun failureShown() = _state.update { it.copy(buyFailed = false) }

    private fun owns(id: ProductId): Boolean = when (id) {
        ProductId.AD_FREE -> plan.value != Plan.FREE
        ProductId.PLUS_YEARLY -> plan.value == Plan.PLUS
    }

    /** 스토어가 예외를 던져도 실패로 돌려 버튼이 눌린 채로 멈추지 않게 한다. */
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
        viewModelScope.launch {
            val message =
                when (attempt { purchases.restore() }) {
                    PurchaseResult.DONE -> PlusMessage.Restored
                    PurchaseResult.FAILED -> PlusMessage.RestoreFailed
                    PurchaseResult.CANCELLED, PurchaseResult.NOTHING -> PlusMessage.RestoreNone
                }
            _messages.send(message)
        }
    }
}
