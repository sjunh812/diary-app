// 레거시(1.7.x) 기록 이전(docs/migration.md). 레거시 형식을 다 다룬 뒤 통째로 지울 수 있게 한 모듈에 모은다.
plugins {
    alias(libs.plugins.diary.kmp.library)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.shared.core.model)
            implementation(projects.shared.core.database)
            implementation(projects.shared.core.datastore)
            implementation(projects.shared.core.platform)
            // 레거시 DB는 Room이 아니라 번들 드라이버의 날 SQL로 읽는다(research R5).
            implementation(libs.androidx.sqlite.bundled)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            // 본문을 서식 없는 글로 되돌리는 함수(toPlainText)를 일기 핵심과 같이 쓴다.
            implementation(projects.shared.core.data)
            implementation(libs.okio)
        }
        commonTest.dependencies {
            implementation(projects.shared.core.testing)
        }
    }
}
