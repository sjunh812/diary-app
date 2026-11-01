// 드라이브 확인용 빌드(스펙 003 T009, T010).
// 새 앱 뼈대가 생기기 전에 같은 패키지(org.sjhstudio.diary)로 드라이브 파일이 보이는지만 본다.
// 확인이 끝나면 이 코드는 스펙 003의 드라이브 연결 코드로 옮기고 지운다.
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "drive-probe"
include(":app")
