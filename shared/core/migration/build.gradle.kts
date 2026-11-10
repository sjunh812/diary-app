// 레거시(1.7.x) 기록 이전(docs/migration.md)
// 레거시 형식을 다 다룬 뒤 통째로 지우려고 한 모듈에 모음
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
            // 레거시 DB는 Room 대신 번들 드라이버의 날 SQL로 읽음(research R5)
            implementation(libs.androidx.sqlite.bundled)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            // 옛 잠금을 이을 때 LockRepository 씀
            implementation(projects.shared.core.data)
            implementation(libs.okio)
        }
        commonTest.dependencies {
            implementation(projects.shared.core.testing)
        }
    }
}
