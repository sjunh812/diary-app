package org.sjhstudio.diary.app.review

import android.content.Context
import com.google.android.play.core.ktx.requestReview
import com.google.android.play.core.review.ReviewManagerFactory
import org.sjhstudio.diary.app.CurrentActivity
import org.sjhstudio.diary.core.data.StoreReview

/**
 * Play 앱 안 리뷰 창
 * 띄울지와 결과는 Play가 정하고 알려주지 않음. 실패해도 알리지 않고 흐름을 이음(Play 지침)
 * 정보는 잠깐만 유효해 차례일 때만 받고, 마지막 판단([request]의 stillDue)은 받은 뒤에 함
 */
class PlayStoreReview(context: Context, private val current: CurrentActivity) : StoreReview {
    private val manager = ReviewManagerFactory.create(context.applicationContext)

    override suspend fun request(stillDue: suspend () -> Boolean): Boolean {
        val info = manager.requestReview()
        // 판단하는 사이 화면이 다시 만들어지거나 뒤로 갈 수 있어 판단 뒤에 잡음
        val activity = if (stillDue()) current.value else null
        if (activity == null) return false
        // 띄우기를 시작하면 바로 돌려줌. 창이 닫힐 때까지 기다리면 그사이 꺼질 때 기록이 빠짐
        manager.launchReviewFlow(activity, info)
        return true
    }
}
