package org.sjhstudio.diary.feature.plus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

data class PlusUiState(val products: List<StoreProduct> = emptyList(), val loading: Boolean = true, val busy: Boolean = false) {
    fun price(id: ProductId): String? = products.firstOrNull { it.id == id }?.price
}

enum class PlusMessage { Failed, Restored, RestoreNone }

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
            val products = purchases.products()
            _state.update { it.copy(products = products, loading = false) }
        }
    }

    fun buy(id: ProductId, onDone: () -> Unit = {}) {
        if (_state.value.busy) return
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            val result = purchases.buy(id)
            _state.update { it.copy(busy = false) }
            when (result) {
                PurchaseResult.DONE -> onDone()
                PurchaseResult.FAILED -> _messages.send(PlusMessage.Failed)
                PurchaseResult.CANCELLED -> Unit
            }
        }
    }

    fun restore() {
        if (_state.value.busy) return
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            val result = purchases.restore()
            _state.update { it.copy(busy = false) }
            _messages.send(if (result == PurchaseResult.DONE) PlusMessage.Restored else PlusMessage.RestoreNone)
        }
    }
}
