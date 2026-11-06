package org.sjhstudio.diary.app.ads

import com.google.android.gms.ads.nativead.NativeAdOptions
import kotlin.test.Test
import kotlin.test.assertEquals

class AdMobOptionsTest {
    /** 앱의 ×가 오른쪽 위에 있어서, AdChoices가 그 자리로 가면 ×에 가린다(AdMob 정책). */
    @Test
    fun adChoicesStayOutOfTheCloseButtonsCorner() {
        assertEquals(NativeAdOptions.ADCHOICES_BOTTOM_RIGHT, nativeAdOptions().adChoicesPlacement)
    }
}
