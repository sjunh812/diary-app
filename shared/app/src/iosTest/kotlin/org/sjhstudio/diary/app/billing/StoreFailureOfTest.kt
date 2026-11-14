package org.sjhstudio.diary.app.billing

import com.revenuecat.purchases.kmp.models.PurchasesError
import com.revenuecat.purchases.kmp.models.PurchasesErrorCode
import com.revenuecat.purchases.kmp.models.PurchasesException
import com.revenuecat.purchases.kmp.models.PurchasesTransactionException
import kotlin.test.Test
import kotlin.test.assertEquals
import org.sjhstudio.diary.core.data.StoreFailure

// RevenueCat 오류가 결제 판단(StoreBilling)이 아는 까닭으로 맞게 바뀌는지 봄
// 까닭이 틀리면 취소가 실패 안내로 뜨거나, 이미 산 사람이 다시 결제됨
class StoreFailureOfTest {
    private fun error(code: PurchasesErrorCode) = PurchasesError(code, null)

    private fun transaction(code: PurchasesErrorCode, cancelled: Boolean = false) =
        storeFailureOf(PurchasesTransactionException(error(code), userCancelled = cancelled)).reason

    @Test
    fun closing_the_purchase_sheet_is_a_cancel() {
        assertEquals(StoreFailure.Reason.CANCELLED, transaction(PurchasesErrorCode.UnknownError, cancelled = true))
    }

    @Test
    fun cancel_code_without_the_flag_is_still_a_cancel() {
        assertEquals(StoreFailure.Reason.CANCELLED, transaction(PurchasesErrorCode.PurchaseCancelledError))
    }

    @Test
    fun waiting_for_approval_is_pending() {
        assertEquals(StoreFailure.Reason.PENDING, transaction(PurchasesErrorCode.PaymentPendingError))
    }

    @Test
    fun buying_again_after_reinstall_is_already_owned() {
        assertEquals(StoreFailure.Reason.ALREADY_OWNED, transaction(PurchasesErrorCode.ProductAlreadyPurchasedError))
    }

    @Test
    fun network_and_other_errors_are_failures() {
        listOf(PurchasesErrorCode.NetworkError, PurchasesErrorCode.StoreProblemError, PurchasesErrorCode.UnknownError).forEach { code ->
            assertEquals(StoreFailure.Reason.OTHER, storeFailureOf(PurchasesException(error(code))).reason)
        }
    }
}
