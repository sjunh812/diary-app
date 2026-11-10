import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder

// 코루틴 안전 검사: 맨 launch와 stateIn은 실패하고, 안전한 함수와 시험·주석은 통과한다.
class CoroutineSafetyCheckTaskTest {
    private val root: File = createTempDirectory("coroutine-safety-check").toFile()

    @AfterTest
    fun tearDown() {
        root.deleteRecursively()
    }

    private fun write(path: String, text: String) = File(root, path).apply {
        parentFile.mkdirs()
        writeText(text.trimIndent())
    }

    private fun check(): Pair<Result<Unit>, String> {
        val project = ProjectBuilder.builder().withProjectDir(root).build()
        project.pluginManager.apply(RootConventionPlugin::class.java)
        val task = project.tasks.getByName("coroutineSafetyCheck") as CoroutineSafetyCheckTask
        task.report.get().asFile.parentFile.mkdirs()
        val result = runCatching { task.check() }
        return result to task.report.get().asFile.readText()
    }

    @Test
    fun bare_launch_and_state_in_fail() {
        write(
            "shared/feature/home/src/commonMain/kotlin/HomeViewModel.kt",
            """
            fun a() = viewModelScope.launch { load() }
            val b = flow.stateIn(viewModelScope, SharingStarted.Eagerly, 0)
            val c = flow.stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                0
            )
            """
        )

        val (result, report) = check()

        assertTrue(result.exceptionOrNull() is GradleException)
        assertEquals(3, report.lines().count { it.isNotBlank() }, report)
    }

    @Test
    fun safe_helpers_tests_and_comments_pass() {
        write(
            "shared/feature/home/src/commonMain/kotlin/HomeViewModel.kt",
            """
            // viewModelScope.launch 는 쓰지 않는다
            fun a() = viewModelScope.launchSafely { load() }
            fun b() = viewModelScope.launchWrite { save() }
            val c = flow.stateInSafely(viewModelScope, SharingStarted.Eagerly, 0)
            """
        )
        write("shared/feature/home/src/commonTest/kotlin/HomeViewModelTest.kt", "fun t() = viewModelScope.launch { }")

        val (result, report) = check()

        assertTrue(result.isSuccess, report)
    }
}
