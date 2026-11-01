package org.sjhstudio.diary

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import org.sjhstudio.diary.app.DiaryApp

// 잠금 화면의 생체 인증 창(BiometricPrompt)이 FragmentActivity를 요구한다.
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { DiaryApp() }
    }
}
