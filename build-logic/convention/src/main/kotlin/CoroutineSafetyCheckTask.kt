import org.gradle.api.tasks.CacheableTask

// 화면 모델이 실패로 앱을 끄지 않는지 검사(DECISIONS.md "앱 꺼짐을 구조로 막는다")
// 예상되는 실패는 저장소가 결과로 돌려주고, 화면 모델은 실패를 받는 `launchSafely`·`launchWrite`·`stateInSafely`만 씀
@CacheableTask
abstract class CoroutineSafetyCheckTask : SourcePatternCheckTask() {
    override val checkName = "코루틴 안전"

    override val rules =
        listOf(
            PatternRule(Regex("""viewModelScope\.launch\b(?!Safely|Write)"""), "화면 모델은 launchSafely나 launchWrite로 시작한다"),
            // 줄 단위라 `stateIn(`에서 줄을 바꾼 것도 잡음
            PatternRule(Regex("""\.stateIn\((\s*viewModelScope|\s*$)"""), "화면 모델의 흐름은 stateInSafely로 담는다")
        )
}
