package org.sjhstudio.diary.buildlogic

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.int(alias: String): Int = findVersion(alias).get().requiredVersion.toInt()

internal fun VersionCatalog.library(alias: String): Provider<MinimalExternalModuleDependency> = findLibrary(alias).get()

private fun VersionCatalog.string(alias: String): String = findVersion(alias).get().requiredVersion

/** 코틀린과 자바가 카탈로그의 `jvmTarget` 하나를 본다. 둘이 갈리면 어디서 어긋났는지 찾기 어렵다. */
internal val VersionCatalog.jvmTarget: JvmTarget
    get() = JvmTarget.fromTarget(string("jvmTarget"))

internal val VersionCatalog.javaVersion: JavaVersion
    get() = JavaVersion.toVersion(string("jvmTarget"))

/** `:shared:core:designsystem` → `org.sjhstudio.diary.core.designsystem` */
internal val Project.diaryNamespace: String
    get() = "org.sjhstudio.diary." + path.removePrefix(":shared:").removePrefix(":").replace(':', '.')
