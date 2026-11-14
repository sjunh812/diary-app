package org.sjhstudio.diary.core.data.diary

/**
 * 첫 화면, 드문 글자 찾기, 달력 한 달에 주는 시간(밀리초)
 * Android는 SC-003의 1초 그대로 재고, 최적화하지 않은 iOS 시험 빌드는 끝나는지만 봄
 */
internal expect val QUERY_BUDGET_MILLIS: Long

/**
 * 1만 편이 모두 걸리는 찾기에 주는 시간(밀리초)
 * 플랫폼마다 시험 빌드 속도가 크게 달라 따로 둠
 */
internal expect val WORST_QUERY_BUDGET_MILLIS: Long
