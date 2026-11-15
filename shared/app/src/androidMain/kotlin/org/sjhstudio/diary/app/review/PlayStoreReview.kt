package org.sjhstudio.diary.app.review

import android.content.Context
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview
import com.google.android.play.core.review.ReviewManagerFactory
import org.sjhstudio.diary.app.CurrentActivity
import org.sjhstudio.diary.core.data.StoreReview

/**
 * Play 앱 안 리뷰 창
 * 띄울지와 결과는 Play가 정하고 알려 주지 않음. 실패해도 알리지 않고 흐름을 이음(Play 지침)
 * 정보는 띄울 게 정해진 뒤에만 받음. 잠깐만 유효함
 */
class PlayStoreReview(context: Context, private val current: CurrentActivity) : StoreReview {
    private val manager = ReviewManagerFactory.create(context.applicationContext)

    override suspend fun request() {
        val info = manager.requestReview()
        // 받는 사이 화면이 뒤로 갔으면 띄우지 않음
        val activity = current.value ?: return
        manager.launchReview(activity, info)
    }
}
