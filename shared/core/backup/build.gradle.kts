// 백업과 복원 규칙. 드라이브를 몰라도 대역으로 시험할 수 있게 드라이브 호출과 나눈다(스펙 003 plan.md).
plugins {
    alias(libs.plugins.diary.kmp.library)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.shared.core.model)
            api(projects.shared.core.drive)
            api(projects.shared.core.data)
            api(projects.shared.core.datastore)
            api(projects.shared.core.database)
            implementation(projects.shared.core.platform)
            // 예전 앱 백업(note.db)을 이전과 같은 규칙으로 읽는다.
            implementation(projects.shared.core.migration)
            implementation(libs.okio)
            // 스냅샷 사본을 Room 없이 직접 연다.
            implementation(libs.androidx.sqlite.bundled)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
        }
        commonTest.dependencies {
            implementation(projects.shared.core.testing)
            implementation(libs.okio.fakefilesystem)
        }
    }
}
