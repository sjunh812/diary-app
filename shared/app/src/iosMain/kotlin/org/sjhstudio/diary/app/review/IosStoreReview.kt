package org.sjhstudio.diary.app.review

import org.sjhstudio.diary.core.data.StoreReview

/** Swift가 구현하는 리뷰 창 다리(StoreReviewBridge.swift). 화면 스레드로 넘겨 띄움 */
interface IosStoreReviewBridge {
    fun request()
}

class IosStoreReview(private val bridge: IosStoreReviewBridge) : StoreReview {
    override suspend fun request(claim: suspend () -> Boolean) {
        if (claim()) bridge.request()
    }
}
