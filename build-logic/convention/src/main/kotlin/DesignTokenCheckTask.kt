import org.gradle.api.tasks.CacheableTask

// 화면 코드가 디자인 시스템 토큰과 문자열 리소스만 쓰는지 본다(FR-028, FR-030, research R18).
// 디자인 시스템 모듈과 미리보기 파일은 루트 등록에서 뺀다.
@CacheableTask
abstract class DesignTokenCheckTask : SourcePatternCheckTask() {
    override val checkName = "디자인 토큰"

    override val rules =
        listOf(
            PatternRule(Regex("""Color\(0x"""), "색은 DiaryTheme 색 토큰으로 쓴다"),
            PatternRule(Regex("""(?<![\w.])\d+(\.\d+)?f?\.(dp|sp)\b"""), "크기는 간격·모서리·글자 토큰으로 쓴다"),
            PatternRule(Regex("""RoundedCornerShape\(\s*\d"""), "모서리는 DiaryRadius 토큰으로 쓴다"),
            PatternRule(Regex("""MaterialTheme\.colorScheme"""), "MaterialTheme 대신 DiaryTheme 색을 쓴다"),
            PatternRule(Regex("""MaterialTheme\.typography"""), "MaterialTheme 대신 DiaryTheme 글자를 쓴다"),
            PatternRule(Regex("""\bText\(\s*""""), "화면 문구는 문자열 리소스에 둔다")
        )
}
