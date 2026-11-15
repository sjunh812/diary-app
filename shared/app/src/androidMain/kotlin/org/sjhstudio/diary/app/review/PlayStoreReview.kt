package org.sjhstudio.diary.app.review

import android.content.Context
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview
import com.google.android.play.core.review.ReviewManagerFactory
import org.sjhstudio.diary.app.CurrentActivity
import org.sjhstudio.diary.core.data.StoreReview

/**
 * Play 앱 안 리뷰 창
 * 띄울지와 결과는 Play가 정하고 알려주지 않음. 실패해도 알리지 않고 흐름을 이음(Play 지침)
 * 정보는 잠깐만 유효해 차례일 때만 받고, 마지막 판단([claim])은 받은 뒤에 함
 */
class PlayStoreReview(context: Context, private val current: CurrentActivity) : StoreReview {
    private val manager = ReviewManagerFactory.create(context.applicationContext)

    override suspend fun request(claim: suspend () -> Boolean) {
        val info = manager.requestReview()
        // 받는 사이 화면이 뒤로 갔으면 묻지 않은 것으로 남김
        if (current.value == null || !claim()) return
        // 기록하는 사이 화면이 다시 만들어질 수 있어 띄우기 직전에 다시 잡음
        val activity = current.value ?: return
        manager.launchReview(activity, info)
    }
}
