package org.sjhstudio.diary.app.review

import org.sjhstudio.diary.core.data.StoreReview

/** Swift가 구현하는 리뷰 창 다리(StoreReviewBridge.swift). 화면 스레드에서 부름 */
interface IosStoreReviewBridge {
    /** 앞에 나와 있는 창을 찾아 요청했으면 참. 실제로 보일지는 iOS가 정함 */
    fun request(): Boolean
}

class IosStoreReview(private val bridge: IosStoreReviewBridge) : StoreReview {
    override suspend fun request(stillDue: suspend () -> Boolean): Boolean = stillDue() && bridge.request()
}
