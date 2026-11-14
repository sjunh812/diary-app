rootProject.name = "diary"

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google {
            content {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

// 레거시 app/(1.7.4)은 이 빌드에 넣지 않는다.
// 이전 코드가 레거시 형식을 다 다룰 때까지 읽기용으로 폴더만 남긴다(plan.md Structure Decision).
include(":composeApp")

// 앱 시작과 목록 넘기기를 재고 Baseline Profile을 만든다. 앱에 들어가지 않는다.
include(":benchmarks")

// 고를 때 받는 일기 글꼴. 글꼴마다 자산만 든 Play 필요할 때 설치 모듈이다(research R17). iOS도 같은 파일을 쓴다.
include(":fonts:gowun_batang", ":fonts:nanum_jeomggol", ":fonts:sejong_geulggot")

// 시작 관문과 화면 전환 뼈대. 안드로이드는 composeApp이 감싸고, iOS는 이 모듈을 Shared 프레임워크로 받는다.
include(":shared:app")

include(":shared:core:model")
include(":shared:core:database")
include(":shared:core:drive")
include(":shared:core:backup")
include(":shared:core:datastore")
include(":shared:core:data")
include(":shared:core:migration")
include(":shared:core:designsystem")
include(":shared:core:ui")
include(":shared:core:platform")
include(":shared:core:testing")

include(":shared:feature:home")
include(":shared:feature:write")
include(":shared:feature:calendar")
include(":shared:feature:search")
include(":shared:feature:insight")
include(":shared:feature:trash")
include(":shared:feature:lock")
include(":shared:feature:migration")
include(":shared:feature:settings")
include(":shared:feature:backup")
include(":shared:feature:welcome")
include(":shared:feature:plus")
