import org.gradle.api.tasks.CacheableTask

// 화면 모델이 실패로 앱을 끄지 않는지 본다(2026-11-08 사용자 결정: 앱 꺼짐은 절대 없어야 한다).
// 예상되는 실패는 저장소가 결과로 돌려주고, 화면 모델은 실패를 받아 주는 `launchSafely`·`launchWrite`·`stateInSafely`만 쓴다.
@CacheableTask
abstract class CoroutineSafetyCheckTask : SourcePatternCheckTask() {
    override val checkName = "코루틴 안전"

    override val rules =
        listOf(
            PatternRule(Regex("""viewModelScope\.launch\b(?!Safely|Write)"""), "화면 모델은 launchSafely나 launchWrite로 시작한다"),
            PatternRule(Regex("""\.stateIn\(\s*viewModelScope"""), "화면 모델의 흐름은 stateInSafely로 담는다")
        )
}
