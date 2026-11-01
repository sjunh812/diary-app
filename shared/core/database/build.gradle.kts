// 새 DB(Room 3 + 번들 SQLite, research R4). 스키마 JSON은 schemas/에 커밋한다.
plugins {
    alias(libs.plugins.diary.room)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.shared.core.model)
            api(libs.kotlinx.coroutines.core)
            implementation(libs.androidx.room3.paging)
        }
    }
}
