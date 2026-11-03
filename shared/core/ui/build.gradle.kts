// 창 폭 등급, 공통 상태 화면, 미리보기 도구.
plugins {
    alias(libs.plugins.diary.kmp.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.shared.core.designsystem)
            // 일기 본문 글꼴과 글자 크기(DiaryBodyText)가 설정의 글꼴 ID와 크기 단계를 읽는다.
            api(projects.shared.core.model)
            api(libs.compose.material3.adaptive)
            // 날짜 형식(DiaryDateFormat)이 공개 API에 날짜 형을 쓴다.
            api(libs.kotlinx.datetime)
            // 일기 사진 칸. 앱 저장소 파일만 읽는다(네트워크 없음).
            implementation(libs.coil.compose)
            implementation(libs.okio)
        }
    }
}

compose.resources {
    publicResClass = false
    packageOfResClass = "org.sjhstudio.diary.core.ui.resources"
    generateResClass = always
}
