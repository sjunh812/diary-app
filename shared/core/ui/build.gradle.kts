// 창 폭 등급, 공통 상태 화면, 미리보기 도구
plugins {
    alias(libs.plugins.diary.kmp.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.compottie)
            api(projects.shared.core.designsystem)
            // 일기 본문 글꼴과 글자 크기(DiaryBodyText)가 설정의 글꼴 ID와 크기 단계를 읽음
            api(projects.shared.core.model)
            // 일기 메뉴(EntryMenuWork)가 즐겨찾기·휴지통·공유를 저장소에 맡김
            implementation(projects.shared.core.data)
            // 날짜 형식(DiaryDateFormat) 공개 API가 날짜 형을 씀
            api(libs.kotlinx.datetime)
            // 일기 사진 칸
            // 앱 저장소 파일만 읽음(네트워크 없음)
            implementation(libs.coil.compose)
            implementation(libs.okio)
            // 맥락 팁 상태는 앱 틀이 Koin에 둠
            // 미리보기와 시험에는 없어도 됨
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            // 앱이 돌아올 때 오늘을 다시 읽음(rememberToday)
            implementation(libs.jb.lifecycle.runtime.compose)
        }
        androidMain.dependencies {
            // 알림 허용을 묻는 시스템 창(NotificationAccess)
            implementation(libs.androidx.activity.compose)
        }
    }
}

compose.resources {
    publicResClass = false
    packageOfResClass = "org.sjhstudio.diary.core.ui.resources"
    generateResClass = always
}
