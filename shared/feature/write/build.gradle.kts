// 쓰기와 수정 화면
plugins {
    alias(libs.plugins.diary.kmp.feature)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.datetime)
            // 사용 통계와 시계
            implementation(projects.shared.core.platform)
            // 서식 편집기와 사진 고르기
            implementation(libs.rich.editor)
            implementation(libs.filekit.dialogs.compose)
            implementation(libs.compose.ui.backhandler)
        }
        androidMain.dependencies {
            // 현재 위치를 가져올 때 위치 권한 창을 띄움
            implementation(libs.androidx.activity.compose)
        }
    }
}
