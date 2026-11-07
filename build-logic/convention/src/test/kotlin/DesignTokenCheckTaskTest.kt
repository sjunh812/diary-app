import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder

// 디자인 토큰 검사(스펙 001 T079): 금지한 꼴이 든 화면 코드는 실패하고, 디자인 시스템·미리보기·주석은 통과한다.
class DesignTokenCheckTaskTest {
    private val root: File = createTempDirectory("design-token-check").toFile()

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
        val task = project.tasks.getByName("designTokenCheck") as DesignTokenCheckTask
        // Gradle이 작업을 돌릴 때는 출력 폴더를 먼저 만든다.
        // 작업을 바로 부르는 시험이라 직접 만든다.
        task.report.get().asFile.parentFile.mkdirs()
        val result = runCatching { task.check() }
        return result to task.report.get().asFile.readText()
    }

    @Test
    fun each_forbidden_pattern_fails() {
        write(
            "shared/feature/home/src/commonMain/kotlin/HomeScreen.kt",
            """
            val a = Color(0xFF112233)
            val b = Modifier.padding(12.dp)
            val c = 14.sp
            val d = RoundedCornerShape(8)
            val e = MaterialTheme.colorScheme.primary
            val f = MaterialTheme.typography.bodyLarge
            val g = Text("안녕")
            """
        )

        val (result, report) = check()

        assertTrue(result.exceptionOrNull() is GradleException)
        assertEquals(7, report.lines().count { it.isNotBlank() }, report)
        assertTrue("HomeScreen.kt:1: 색은 DiaryTheme 색 토큰으로 쓴다" in report, report)
    }

    @Test
    fun tokens_comments_design_system_previews_and_tests_pass() {
        write(
            "shared/feature/home/src/commonMain/kotlin/HomeScreen.kt",
            """
            // 12.dp는 주석이라 괜찮다
            val a = Modifier.padding(DiarySpacing.x4)
            val b = size.fontSizeSp.sp
            val c = DiaryTheme.colors.canvas
            val d = stringResource(Res.string.title)
            """
        )
        write("shared/feature/home/src/commonMain/kotlin/preview/HomePreviews.kt", "val a = 12.dp")
        write("shared/feature/home/src/commonMain/kotlin/HomeScreenPreview.kt", "val a = Color(0xFF000000)")
        write("shared/app/src/iosTest/kotlin/AppScaffoldUiTest.kt", "val a = 400.dp")
        write("shared/feature/home/src/commonTest/kotlin/HomeTest.kt", "val a = Text(\"견본\")")
        write("shared/core/designsystem/src/commonMain/kotlin/Theme.kt", "val canvas = Color(0xFFFFFFFF)")

        val (result, report) = check()

        assertTrue(result.isSuccess, report)
        assertEquals("", report)
    }

    @Test
    fun build_outputs_are_skipped() {
        write("shared/app/build/generated/Generated.kt", "val a = 12.dp")

        val (result, _) = check()

        assertTrue(result.isSuccess)
        assertFailsWith<GradleException> {
            write("shared/app/src/commonMain/kotlin/App.kt", "val a = 12.dp")
            check().first.getOrThrow()
        }
    }
}
